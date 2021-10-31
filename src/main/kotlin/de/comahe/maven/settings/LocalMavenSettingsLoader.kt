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
package de.comahe.maven.settings

import org.apache.maven.settings.Settings
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest
import org.apache.maven.settings.building.SettingsBuildingException
import org.apache.maven.settings.building.SettingsBuildingRequest
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher
import org.sonatype.plexus.components.sec.dispatcher.SecUtil
import java.io.File

/**
 * Class used to load Maven settings.
 *
 * @author Mark Vieira
 * @author Marcel Heckel
 */
class LocalMavenSettingsLoader(private val userSettingsFile: File) {
    /**
     * Loads and merges Maven settings from global and local user configuration files. Returned
     * [Settings] object includes decrypted credentials.
     *
     * @return Effective settings
     * @throws SettingsBuildingException If the effective settings cannot be built
     */
    @Throws(SettingsBuildingException::class)
    fun loadSettings(): Settings {
        val settingsBuildingRequest: SettingsBuildingRequest = DefaultSettingsBuildingRequest()
        settingsBuildingRequest.userSettingsFile = userSettingsFile
        settingsBuildingRequest.globalSettingsFile =
            GLOBAL_SETTINGS_FILE
        settingsBuildingRequest.systemProperties = System.getProperties()
        val factory = DefaultSettingsBuilderFactory()
        val settingsBuilder = factory.newInstance()
        val settingsBuildingResult = settingsBuilder.build(settingsBuildingRequest)
        val settings = settingsBuildingResult.effectiveSettings
        decryptCredentials(settings)
        return settings
    }

    private fun decryptCredentials(settings: Settings) {
        try {
            var masterPassword: String? = null
            val cipher = DefaultPlexusCipher()
            val settingsSecurityFile = File(SETTINGS_SECURITY_FILE_LOCATION)
            var hasSettingsSecurity = false
            if (settingsSecurityFile.exists() && !settingsSecurityFile.isDirectory) {
                val settingsSecurity = SecUtil.read(SETTINGS_SECURITY_FILE_LOCATION, true)
                masterPassword = cipher.decryptDecorated(
                    settingsSecurity.master,
                    DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION
                )
                hasSettingsSecurity = true
            }
            for (server in settings.servers) {
                if (cipher.isEncryptedString(server.password)) {
                    if (hasSettingsSecurity) {
                        server.password = cipher.decryptDecorated(server.password, masterPassword)
                    } else {
                        throw RuntimeException("Maven settings contains encrypted credentials yet no settings-security.xml exists.")
                    }
                }
                if (cipher.isEncryptedString(server.passphrase)) {
                    if (hasSettingsSecurity) {
                        server.passphrase =
                            cipher.decryptDecorated(server.passphrase, masterPassword)
                    } else {
                        throw RuntimeException("Maven settings contains encrypted credentials yet no settings-security.xml exists.")
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Unable to decrypt local Maven settings credentials.", e)
        }
    }

    companion object {
        val GLOBAL_SETTINGS_FILE = File(System.getenv("M2_HOME"), "conf/settings.xml")
        val SETTINGS_SECURITY_FILE_LOCATION =
            System.getProperty("user.home") + "/.m2/settings-security.xml"
    }
}