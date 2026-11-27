plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.fabric.loom)
}

group = "me.yin.comprehension"
version = "1.0.0-SNAPSHOT"

val minecraftModJarName = "Comprehension-${project.name}"
val minecraftModRelocationPrefix = "me.yin.comprehension.libs"

kotlin {
    jvmToolchain(25)
}

loom {
    runs {
        named("client") {
            programArgs(
                "--width", providers.gradleProperty("clientWidth").getOrElse("1600"),
                "--height", providers.gradleProperty("clientHeight").getOrElse("900"),
                "--username", providers.gradleProperty("clientUsername").getOrElse("Agent")
            )
        }
    }
}

repositories {
    mavenCentral()
}

val shadowBundle = configurations.create("shadowBundle") {
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx")
    exclude(group = "org.jetbrains", module = "annotations")
    exclude(group = "org.slf4j")
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.language.kotlin)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.status.pages)
    shadowBundle(libs.ktor.server.core)
    shadowBundle(libs.ktor.server.cio)
    shadowBundle(libs.ktor.server.status.pages)
}

tasks.processResources {
    val minecraftModVersion = project.version.toString()
    inputs.property("version", minecraftModVersion)

    filesMatching("fabric.mod.json") {
        expand("version" to minecraftModVersion)
    }
}

tasks.jar {
    archiveFileName.set("$minecraftModJarName.jar")
}

tasks.shadowJar {
    configurations.set(listOf(shadowBundle))
    archiveFileName.set("$minecraftModJarName-shadow.jar")
    mergeServiceFiles()
    relocate("io.ktor", "$minecraftModRelocationPrefix.io.ktor")
    relocate("com.typesafe.config", "$minecraftModRelocationPrefix.com.typesafe.config")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
