project.version = "1.6.7"

plugins {
    kotlin("jvm") version "2.0.21"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("io.github.goooler.shadow") version "8.1.8"
    id("maven-publish")
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://maven.evokegames.gg/snapshots")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.1")

    implementation("me.tofaa.entitylib:spigot:2.4.11-SNAPSHOT")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("com.github.technicallycoded:FoliaLib:main-SNAPSHOT")

    library(kotlin("stdlib"))
    library(kotlin("reflect"))
}

kotlin {
    jvmToolchain(17)
}

tasks.compileKotlin {
    compilerOptions.javaParameters = true
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

val pluginPackage = "com.maximde.hologramlib"
tasks.shadowJar {

    archiveFileName.set("HologramLib-$version.jar")

    exclude(
        "DebugProbesKt.bin",
        "*.SF", "*.DSA", "*.RSA", "META-INF/**", "OSGI-INF/**",
        "deprecated.properties", "driver.properties", "mariadb.properties", "mozilla/public-suffix-list.txt",
        "org/slf4j/**", "org/apache/logging/slf4j/**", "org/apache/logging/log4j/**", "Log4j-*"
    )

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*"))
        exclude(dependency("org.checkerframework:.*"))
        exclude(dependency("org.jetbrains:annotations"))
        exclude(dependency("org.slf4j:.*"))
    }

    rootDir.resolve("gradle").resolve("relocations.txt").readLines().forEach {
        if (it.isNotBlank()) relocate(it, "$pluginPackage.__relocated__.$it")
    }
}

bukkit {
    version = project.version.toString()
    main = "com.maximde.hologramlib.Main"
    apiVersion = "1.19"
    author = "MaximDe"
    foliaSupported = true
    depend = listOf("packetevents")
    softDepend = listOf("ItemsAdder")
}