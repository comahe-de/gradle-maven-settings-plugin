# Gradle Maven settings plugin

## Overview

This Gradle plugin provides a migration path for projects coming from a Maven ecosystem. It exposes
standard Maven configuration located in [settings files](http://maven.apache.org/settings.html) to
your Gradle project. This allows projects to continue to leverage functionality provided by Maven
such as mirrors as well use existing settings configuration to store encrypted repository
authentication credentials.

## Hint

This is a fork of https://github.com/mark-vieira/gradle-maven-settings-plugin.

It was transferred to Kotlin and dependencies were updated.

New features are

* Ability to apply the plugin in a `settings.gradle.kts` to handle plugin repositories. So that also
  plugin can be loaded from private repositories
* Repositories defined in the maven settings file are automatically added to the project.

## Usage

This plugin is hosted on
the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/de.comahe.maven-settings). To use the
plugin, add the following to your `build.gradle.kts` file.

    plugins {
      id ("de.comahe.maven-settings") version "0.0.1"
    }

To load also plugins from your private repositories you have to apply the plugin in the
`settings.gradle.kts`. There, the plugin declaration must be done after the `pluginManagement`
block, e.g.

    rootProject.name = "myProject"
    
    pluginManagement {
        repositories {
            mavenLocal()
            gradlePluginPortal()
        }
    }
    
    plugins {
        id("de.comahe.maven-settings") version "0.0.1"
    }


## Mirrors

The plugin exposes Maven-like mirror capabilities. The plugin will properly register and enforce any
mirrors defined in a `settings.xml` with `<mirrorOf>` values of `*`, `external:*` or `central`.
Existing
`repositories {...}` definitions that match these identifiers will be removed.

Exclusions are also taken into consideration. For example, a `<mirrorOf>` value of `*,!myRepo` will
replace all repositories except for the repository with the name 'myRepo'.

## Credentials

The plugin will attempt to apply credentials located in `<server>` elements to appropriate Maven
repository definitions in your build script. This is done by matching the `<id>` element in
the `settings.xml` file to the `name`
property of the repository definition.

    repositories {
        maven {
            name = "myRepo" // should match <id>myRepo</id> of appropriate <server> in settings.xml
            url = "https://intranet.foo.org/repo"
        }
    }

Server credentials are used for mirrors as well. When mirrors are added the plugin will look for
a `<server>` element with the same `<id>` and the configured credentials are used
and [decrypted](http://maven.apache.org/guides/mini/guide-encryption.html)
if necessary.

### Publishing

The plugin will also attempt to apply credentials to repositories configured using the
['maven-publish'](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.

    publishing {
        repositories {
            maven {
                name = "myRepo" // should match <id>myRepo</id> of appropriate <server> in settings.xml
                url = "https://intranet.foo.org/repo/repositories/releases"
            }
        }
    }

> **Note:** Currently only Basic Authentication using username and password is supported at this time.

## Profiles

Profiles defined in a `settings.xml` will have their properties exported to the Gradle project when
the profile is considered active. Active profiles are those listed in the `<activeProfiles>` section
of the `settings.xml`, the `activeProfiles`
property of the `mavenSettings {...}` configuration closure, or those that satisfy the given
profile's `<activation>`
criteria.

Repositories of active profiles defined in the `settings.xml` are automatically added to the project
repositories.

## Configuration

Configuration of the Maven settings plugin is done via the `mavenSettings {...}` configuration
closure. The following properties are available.

* `userSettingsFileName` - String representing the path of the file to be used as the user settings
  file. This defaults to
  `'$USER_HOME/.m2/settings.xml'`
* `activeProfiles` - List of profile ids to treat as active.
* `exportGradleProps` - Flag indicating whether or not Gradle project properties should be exported
  for the purposes of settings file property interpolation and profile activation. This defaults
  to `true`.
