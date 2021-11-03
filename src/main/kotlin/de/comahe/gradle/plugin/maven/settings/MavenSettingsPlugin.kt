package de.comahe.gradle.plugin.maven.settings

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.PluginAware
import org.gradle.api.publish.PublishingExtension


@Suppress("unused")
class MavenSettingsPlugin : Plugin<PluginAware> {

    override fun apply(pluginAware: PluginAware) {
        when (pluginAware) {
            is Project -> handleProject(pluginAware)
            is Settings -> handleSettings(pluginAware)
            else -> throw IllegalAccessException("The type '${pluginAware.javaClass}' is not supported by the 'maven-settings-plugin'")
        }
    }

    private fun handleProject(project: Project) {
        val extension = project.extensions.create(
            MAVEN_SETTINGS_EXTENSION_NAME,
            MavenSettingsPluginExtension::class.java, project.properties
        )

        project.afterEvaluate {
            val handler = MavenSettingsHandler(project.logger, extension)

            handler.activateProfiles(
                project.projectDir,
                project.extensions.findByType(ExtraPropertiesExtension::class.java),
                project.repositories
            )
            handler.registerMirrors(project.repositories)

            handler.applyRepoCredentials(project.repositories)
            handler.applyRepoCredentials(project.extensions.findByType(PublishingExtension::class.java)?.repositories)
        }

    }


    private fun handleSettings(settings: Settings) {

        val extension = settings.extensions.create(
            MAVEN_SETTINGS_EXTENSION_NAME,
            MavenSettingsPluginExtension::class.java, settings.extensions.extraProperties.properties
        )

        settings.gradle.settingsEvaluated {
            val handler = MavenSettingsHandler(Logging.getLogger(Project::class.java), extension)

            handler.activateProfiles(
                settings.rootDir,
                settings.extensions.findByType(ExtraPropertiesExtension::class.java),
                settings.pluginManagement.repositories
            )
            handler.registerMirrors(settings.pluginManagement.repositories)

            handler.applyRepoCredentials(settings.pluginManagement.repositories)
            handler.applyRepoCredentials(settings.extensions.findByType(PublishingExtension::class.java)?.repositories)
        }
    }
}