plugins {
    kotlin("jvm") version "1.5.31"
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.16.0"
    `maven-publish`
    `kotlin-dsl`
}

group = "de.comahe.gradle.plugin"
version = "0.1.0"

repositories {
    mavenCentral()
    jcenter()
    maven { setUrl("https://jitpack.io") }
}

val mavenVersion = "3.8.1"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.apache.maven:maven-settings:$mavenVersion")
    implementation("org.apache.maven:maven-settings-builder:$mavenVersion")
    implementation("org.apache.maven:maven-model-builder:$mavenVersion")
    implementation("org.apache.maven:maven-model:$mavenVersion")
    implementation("org.apache.maven:maven-core:$mavenVersion")
    implementation("org.sonatype.plexus:plexus-cipher:1.7")
    implementation("org.sonatype.plexus:plexus-sec-dispatcher:1.4")

    compileOnly(gradleApi())
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")


    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-library:2.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}


tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

gradlePlugin {
    plugins {
        create("maven-settings") {
            id = "de.comahe.maven-settings"
            implementationClass = "de.comahe.gradle.plugin.maven.settings.MavenSettingsPlugin"
            displayName = "maven-settings"
            description =
                "Gradle plugin for exposing/reading Maven settings file configuration to Gradle project. " +
                        "Can be applied to 'Project' and 'Settings'"

        }
    }
}

pluginBundle {
    website = "https://comahe-de.github.io/gradle-maven-settings-plugin"
    vcsUrl = "https://github.com/comahe-de/gradle-maven-settings-plugin"
    tags = listOf("settings", "maven")
}

java {
    withSourcesJar()
    withJavadocJar()
}

