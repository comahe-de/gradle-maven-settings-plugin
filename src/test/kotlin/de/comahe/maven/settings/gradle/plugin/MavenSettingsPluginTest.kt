package de.comahe.maven.settings.gradle.plugin

import org.apache.maven.settings.Mirror
import org.apache.maven.settings.Profile
import org.apache.maven.settings.Server
import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.kotlin.dsl.repositories
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.Matchers.hasProperty
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URI
import java.util.Properties

class MavenSettingsPluginTest : AbstractMavenSettingsTest() {

    @Suppress("ObjectLiteralToLambda") // compiler mixes up with too many "apply" function
    @Test
    fun applyMavenSettingsPlugin() {
        project.apply(object : Action<ObjectConfigurationAction> {
            override fun execute(t: ObjectConfigurationAction) {
                t.plugin("de.comahe.maven-settings")
            }
        })



        assertTrue(project.plugins.hasPlugin(MavenSettingsPlugin::class.java))
    }

    @Test
    fun declareGlobalMirror() {
        withSettings {
            mirrors.add(Mirror().apply {
                id = "myrepo"
                mirrorOf = "*"
                url = "http://maven.foo.bar"
            })
        }

        addPluginWithSettings()

        with(project) {
            repositories {
                mavenLocal()
                jcenter()
                mavenCentral()
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(2))
        assertThat(
            project.repositories,
            hasItem(hasProperty<Any>("name", equalTo("myrepo")))
        )
        assertThat(
            project.repositories,
            hasItem(hasProperty<Any>("name", equalTo("MavenLocal")))
        )
    }

    @Test
    fun respectsMirrorExcludes() {
        withSettings {
            mirrors.add(Mirror().apply {
                id = "myrepo"
                mirrorOf = "*,!some-repo"
                url = "http://maven.foo.bar"
            })
        }

        addPluginWithSettings()

        with(project) {
            repositories {
                mavenLocal()
                mavenCentral()
                maven {
                    name = "some-repo"
                    url = URI("https://example.com")
                }
                maven {
                    name = "some-other-repo"
                    url = URI("https://example.com")
                }
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(3))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("myrepo"))))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("MavenLocal"))))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("some-repo"))))
    }

    @Test
    fun declareExternalMirrorWithFileRepo() {
        withSettings {
            mirrors.add(Mirror().apply {
                id = "myrepo"
                mirrorOf = "external:*"
                url = "http://maven.foo.bar"
            })
        }

        addPluginWithSettings()

        with(project) {
            repositories {
                mavenLocal()
                jcenter()
                mavenCentral()
                maven {
                    name = "myLocal"
                    url = File(project.buildDir, ".m2").toURI()
                }
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(3))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("myrepo"))))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("MavenLocal"))))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("myLocal"))))
    }

    @Test
    fun declareExternalMirrorWithLocalhostRepo() {
        withSettings {
            mirrors.add(Mirror().apply {
                id = "myrepo"
                mirrorOf = "external:*"
                url = "http://maven.foo.bar"
            })
        }


        addPluginWithSettings()

        with(project) {
            repositories {
                mavenLocal()
                jcenter()
                mavenCentral()
                maven {
                    name = "myLocal"
                    url = URI("http://localhost/maven")
                }
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(3))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("myrepo"))))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("MavenLocal"))))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("myLocal"))))
    }

    @Test
    fun declareMavenCentralMirror() {
        withSettings {
            mirrors.add(Mirror().apply {
                id = "myrepo"
                mirrorOf = "central"
                url = "http://maven.foo.bar"
            })
        }

        addPluginWithSettings()

        with(project) {
            repositories {
                mavenLocal()
                mavenCentral()
                maven {
                    name = "myRemote"
                    url = URI("https://maven.foobar.org/repo")
                }
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(3))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("myrepo"))))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("MavenLocal"))))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("myRemote"))))
    }

    @Test
    fun declareMavenCentralMirrorWithoutCentralRepo() {
        withSettings {
            mirrors.add(Mirror().apply {
                id = "myrepo"
                mirrorOf = "central"
                url = "http://maven.foo.bar"
            })
        }

        addPluginWithSettings()

        with(project) {
            repositories {
                mavenLocal()
                maven {
                    name = "myRemote"
                    url = URI("https://maven.foobar.org/repo")
                }
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(2))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("MavenLocal"))))
        assertThat(project.repositories, hasItem(hasProperty<Any>("name", equalTo("myRemote"))))
    }

    @Test
    fun profileActiveWithSettingsActiveProfile() {
        val props = Properties()
        props.setProperty("myprop", "true")

        withSettings {
            profiles.add(Profile().apply { id = "myprofile"; properties = props; })
            activeProfiles = listOf("myprofile")
        }

        addPluginWithSettings()

        project.evaluate()

        assertThat(project.properties, hasEntry("myprop", "true"))
    }

    @Test
    fun profileActiveWithExtensionActiveProfile() {
        val props = Properties()
        props.setProperty("myprop", "true")

        withSettings {
            profiles.add(Profile().apply { id = "myprofile"; properties = props; })
            activeProfiles = listOf("myprofile")
        }

        addPluginWithSettings()

        with(project) {
            mavenSettings {
                activeProfiles = listOf("myprofile")
            }
        }

        project.evaluate()

        assertThat(project.properties, hasEntry("myprop", "true"))
    }

    @Test
    fun credentialsAddedToNamedRepository() {
        withSettings {
            servers.add(Server().apply {
                id = "central"; username = "first.last";password = "secret"
            })
        }

        addPluginWithSettings()

        with(project) {
            repositories {
                maven {
                    name = "central"
                    url = URI("https://repo1.maven.org/maven2/")
                }
            }
        }

        project.evaluate()

        val centralRepo = project.repositories.getByName("central") as MavenArtifactRepository
        assertEquals("first.last", centralRepo.credentials.username)
        assertEquals("secret", centralRepo.credentials.password)
    }

    @Test
    fun credentialsOnlyAddedToMavenRepositories() {
        withSettings {
            servers.add(Server().apply {
                id = "flat"; username = "first.last"; password = "secret"
            })
        }

        addPluginWithSettings()

        with(project) {
            repositories {
                flatDir {
                    name = "flat"
                    dirs = setOf(File("."))
                }
            }
        }

        project.evaluate()
    }

    // ### not working with kotlin-dsl
//    @Test
//    fun credentialsAddedToPublishingRepository() {
//        withSettings {
//            servers.add(Server().apply {
//                id = "central"; username = "first.last"; password = "secret"
//            })
//        }
//
//        addPluginWithSettings()
//
//        with(project) {
//            apply(plugin = "maven-publish")
//
//            publishing {
//                repositories {
//                    maven {
//                        name "central"
//                        url "https://repo1.maven.org/maven2/"
//                    }
//                }
//            }
//        }
//
//        project.evaluate()
//
//        assertEquals("first.last", project.publishing.repositories.central.credentials.username)
//        assertEquals("secret", project.publishing.repositories.central.credentials.password)
//    }


}
