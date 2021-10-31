/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.comahe.maven.settings.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import java.io.File


internal const val MAVEN_SETTINGS_EXTENSION_NAME = "mavenSettings"

open class MavenSettingsPluginExtension internal constructor(internal val projectProperties: Map<String, *>) {
    /**
     * Name of settings file to use. String is evaluated using [org.gradle.api.Project.file].
     * Defaults to $USER_HOME/.m2/settings.xml.
     */
    var userSettingsFileName: String = projectProperties.getOrDefault(
        "$MAVEN_SETTINGS_EXTENSION_NAME.userSettingsFileName",
        System.getProperty("user.home") + "/.m2/settings.xml"
    ).toString()

    /**
     * List of profile ids to treat as active.
     */
    var activeProfiles: Collection<String> =
        projectProperties.getOrDefault("$MAVEN_SETTINGS_EXTENSION_NAME.userSettingsFileName", "")
            .toString().split(',').map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Flag indicating whether or not Gradle project properties should be exported for the purposes of settings file
     * property interpolation and profile activation. Defaults to true.
     */
    var exportGradleProps = true
    val userSettingsFile: File
        get() = File(userSettingsFileName)
}


/**
 * Configures the [MavenSettingsPluginExtension] for this project.
 *
 * Executes the given configuration block against the [MavenSettingsPluginExtension] for this
 * project.
 *
 * @param configuration the configuration block.
 */
fun Project.mavenSettings(configuration: MavenSettingsPluginExtension.() -> Unit) =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure(
        MAVEN_SETTINGS_EXTENSION_NAME,
        configuration
    )

/**
 * Configures the [MavenSettingsPluginExtension] for this project.
 *
 * Executes the given configuration block against the [MavenSettingsPluginExtension] for this
 * project settings.
 *
 * @param configuration the configuration block.
 */
fun Settings.mavenSettings(configuration: MavenSettingsPluginExtension.() -> Unit) =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure(
        MAVEN_SETTINGS_EXTENSION_NAME,
        configuration
    )