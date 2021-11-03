package de.comahe.gradle.plugin.maven.settings

import org.apache.maven.settings.Settings
import org.apache.maven.settings.io.DefaultSettingsWriter
import org.apache.maven.settings.io.SettingsWriter
import org.gradle.api.Action
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.kotlin.dsl.closureOf
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.util.ConfigureUtil
import org.junit.After
import org.junit.Before
import java.io.File

abstract class AbstractMavenSettingsTest {
    var settingsDir: File = File("build/tmp/.m2/")
    lateinit var settingsFile: File
    lateinit var project: DefaultProject

    @Before
    fun createSettingsXml() {
        settingsFile = File(settingsDir, "settings.xml")
        project = ProjectBuilder.builder().build() as DefaultProject
    }

    @After
    fun deleteSettingsXml() {
        settingsFile.delete()
    }

    fun withSettings(configureClosure: Settings.() -> Unit) {
        val settings = Settings()
        ConfigureUtil.configure(closureOf<Settings> { this.configureClosure() }, settings)
        val writer: SettingsWriter = DefaultSettingsWriter()
        writer.write(settingsFile, null, settings)
    }


    @Suppress("ObjectLiteralToLambda") // compiler mixes up with too many "apply" function
    fun addPluginWithSettings() {

        project.apply(object : Action<ObjectConfigurationAction> {
            override fun execute(t: ObjectConfigurationAction) {
                t.plugin(MavenSettingsPlugin::class.java)
            }
        })


        project.mavenSettings {
            userSettingsFileName = settingsFile.canonicalPath
        }
    }
}