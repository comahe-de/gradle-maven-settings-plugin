import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.21"
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
    `maven-publish`
    `kotlin-dsl`
}

group = "de.comahe.gradle.plugin"
version = "0.2.1"

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

val mavenVersion = "3.8.9"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.apache.maven:maven-settings:$mavenVersion")
    implementation("org.apache.maven:maven-settings-builder:$mavenVersion")
    implementation("org.apache.maven:maven-model-builder:$mavenVersion")
    implementation("org.apache.maven:maven-model:$mavenVersion")
    implementation("org.apache.maven:maven-core:$mavenVersion")
    implementation("org.sonatype.plexus:plexus-cipher:1.7")
    implementation("org.sonatype.plexus:plexus-sec-dispatcher:1.4")
    // replacee the transitive dependency with a newer version
    implementation("com.google.guava:guava:33.4.8-jre")

    compileOnly(gradleApi())
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")


    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-library:2.2")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}


tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    compileTestKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
}

gradlePlugin {
    website.set("https://comahe-de.github.io/gradle-maven-settings-plugin")
    vcsUrl.set("https://github.com/comahe-de/gradle-maven-settings-plugin")
    plugins {
        create("maven-settings") {
            id = "de.comahe.maven-settings"
            implementationClass = "de.comahe.gradle.plugin.maven.settings.MavenSettingsPlugin"
            displayName = "maven-settings"
            description =
                "Gradle plugin for exposing/reading Maven settings file configuration to Gradle project. " +
                        "Can be applied to 'Project' and 'Settings'"
            tags = listOf("settings", "maven")

        }
    }
}


java {
    withSourcesJar()
    withJavadocJar()
}

