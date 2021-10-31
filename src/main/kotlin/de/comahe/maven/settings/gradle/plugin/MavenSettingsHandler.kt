package de.comahe.maven.settings.gradle.plugin

import de.comahe.maven.settings.LocalMavenSettingsLoader
import org.apache.maven.model.path.DefaultPathTranslator
import org.apache.maven.model.profile.DefaultProfileActivationContext
import org.apache.maven.model.profile.DefaultProfileSelector
import org.apache.maven.model.profile.activation.FileProfileActivator
import org.apache.maven.model.profile.activation.JdkVersionProfileActivator
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator
import org.apache.maven.model.profile.activation.ProfileActivator
import org.apache.maven.model.profile.activation.PropertyProfileActivator
import org.apache.maven.settings.Mirror
import org.apache.maven.settings.Server
import org.apache.maven.settings.Settings
import org.apache.maven.settings.SettingsUtils
import org.apache.maven.settings.building.SettingsBuildingException
import org.gradle.api.GradleScriptException
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtraPropertiesExtension
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URI


internal class MavenSettingsHandler(
    private val logger: Logger,
    private val extension: MavenSettingsPluginExtension
) {
    private val settings: Settings =
        try {
            LocalMavenSettingsLoader(extension.userSettingsFile).loadSettings()
        } catch (e: SettingsBuildingException) {
            throw GradleScriptException("Unable to read local Maven settings.", e)
        }

    fun activateProfiles(
        projectDirectory: File,
        extraPropertiesExtension: ExtraPropertiesExtension?,
        repositories: RepositoryHandler?
    ) {
        val profileSelector = DefaultProfileSelector()
        val activationContext = DefaultProfileActivationContext()
        val profileActivators = listOf<ProfileActivator>(
            JdkVersionProfileActivator(),
            OperatingSystemProfileActivator(),
            PropertyProfileActivator(),
            FileProfileActivator().setPathTranslator(DefaultPathTranslator())
        )

        profileActivators.forEach {
            profileSelector.addProfileActivator(it)
        }

        activationContext.activeProfileIds =
            extension.activeProfiles.toList() + settings.activeProfiles
        logger.info("Maven-Settings - Active profiles: {}", activationContext.activeProfileIds)
        activationContext.projectDirectory = projectDirectory
        activationContext.setSystemProperties(System.getProperties())
        if (extension.exportGradleProps) {
            activationContext.userProperties =
                extension.projectProperties.mapValues { value -> value.toString() }
        }
        @Suppress("MoveLambdaOutsideParentheses")
        val profiles = profileSelector.getActiveProfiles(
            settings.profiles.map { SettingsUtils.convertFromSettingsProfile(it) },
            activationContext,
            { /* nothing */ }
        )

        if (extraPropertiesExtension != null) {
            profiles.forEach { profile ->
                profile.properties.forEach { entry ->
                    extraPropertiesExtension.set(entry.key.toString(), entry.value.toString())
                }
            }
        }

        if (repositories != null) {
            profiles.forEach { profile ->
                profile.repositories.forEach Loop@{ repositoryMaven ->

                    //already defined?

                    repositories.forEach { repository ->
                        if (repository is MavenArtifactRepository
                            && repository.name == repositoryMaven.id
                        )
                            return@Loop
                    }
                    logger.info(
                        "Maven-Settings - Adding Maven repository - id: {} - url: {}",
                        repositoryMaven.id, repositoryMaven.url
                    )
                    repositories.maven {
                        name = repositoryMaven.id
                        url = URI(repositoryMaven.url)
                    }
                }
            }
        }
    }


    fun registerMirrors(repositories: RepositoryHandler) {
        val globalMirror = settings.mirrors.find { it.mirrorOf.split(',').contains("*") }
        if (globalMirror != null) {
            logger.info(
                "Maven-Settings - Found global mirror in settings.xml. Replacing Maven repositories with mirror " +
                        "located at ${globalMirror.url}"
            )
            createMirrorRepository(repositories, globalMirror)
            return
        }

        val externalMirror = settings.mirrors.find {
            it.mirrorOf.split(',').contains("external:*")
        }
        if (externalMirror != null) {
            logger.info(
                "Maven-Settings - Found external mirror in settings.xml. Replacing non-local Maven repositories " +
                        "with mirror located at ${externalMirror.url}"
            )
            createMirrorRepository(repositories, externalMirror) { repo ->
                val host = InetAddress.getByName(repo.url.host)
                // only match repositories not on localhost and not file based
                return@createMirrorRepository repo.url.scheme != "file"
                        && !(host.isAnyLocalAddress
                        || host.isLoopbackAddress
                        || NetworkInterface.getByInetAddress(host) != null)
            }
            return
        }

        val centralMirror = settings.mirrors.find {
            it.mirrorOf.split(',').contains("central")
        }
        if (centralMirror != null) {
            logger.info(
                "Maven-Settings - Found central mirror in settings.xml. Replacing Maven Central repository with " +
                        "mirror located at ${centralMirror.url}"
            )
            createMirrorRepository(repositories, centralMirror) { repo ->
                ArtifactRepositoryContainer.MAVEN_CENTRAL_URL.startsWith(repo.url.toString())
            }
        }
    }


    private fun createMirrorRepository(
        repositories: RepositoryHandler,
        mirror: Mirror
    ) {
        createMirrorRepository(repositories, mirror) { true }
    }

    private fun createMirrorRepository(
        repositories: RepositoryHandler,
        mirror: Mirror,
        predicate: (MavenArtifactRepository) -> Boolean
    ) {
        var mirrorFound = false
        val excludedRepositoryNames = mirror.mirrorOf.split(',').filter {
            it.startsWith("!")
        }.map { it.substring(1) }

        repositories.all {
            if (this is MavenArtifactRepository && name != ArtifactRepositoryContainer.DEFAULT_MAVEN_LOCAL_REPO_NAME
                && url != URI.create(mirror.url)
                && predicate(this)
                && !excludedRepositoryNames.contains(name)
            ) {
                repositories.remove(this)
                mirrorFound = true
            }
        }

        if (mirrorFound) {
            val server = settings.getServer(mirror.id)
            repositories.maven {
                name = mirror.name ?: mirror.id
                url = URI.create(mirror.url)
                applyCredentials(server, this)
            }
        }
    }


    fun applyRepoCredentials(repositories: RepositoryHandler?) {
        repositories?.all {
            if (this is MavenArtifactRepository) {
                settings.servers.forEach { server ->
                    if (name == server.id) {
                        applyCredentials(server, this)
                    }
                }
            }
        }
    }

    private fun applyCredentials(server: Server?, repo: MavenArtifactRepository) {
        if (server == null || server.username == null || server.password == null)
            return
        logger.info(
            "Maven-Settings - Setting credentials for repository - id: {} - url: {}",
            repo.name, repo.url
        )
        repo.credentials {
            username = server.username
            password = server.password
        }
    }

}