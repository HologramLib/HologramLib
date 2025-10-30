project.version = "1.8.2"

group = "com.github.max1mde"
version = "1.8.3"

plugins {
    kotlin("jvm") version "2.0.21"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("io.github.goooler.shadow") version "8.1.8"
    id("maven-publish")
    signing
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar.get()) {
            }

            groupId = "com.maximjsx"
            artifactId = "hologramlib"
            version = project.version.toString()

            pom {
                name.set("HologramLib")
                description.set("Fancy hologram library")
                url.set("https://github.com/HologramLib/HologramLib")
                licenses {
                    license {
                        name.set("GPL-3.0 License")
                        url.set("https://opensource.org/license/gpl-3-0")
                    }
                }
                developers {
                    developer {
                        id.set("maximjsx")
                        name.set("Maxim.jsx")
                    }
                }
                scm {
                    url.set("https://github.com/HologramLib/HologramLib")
                    connection.set("scm:git:git://github.com/HologramLib/HologramLib.git")
                    developerConnection.set("scm:git:ssh://github.com/HologramLib/HologramLib.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "MavenCentral"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrhUsername") as String? ?: ""
                password = project.findProperty("ossrhPassword") as String? ?: ""
            }
        }
    }
}


signing {
    val secretKey: String? = project.findProperty("signing.secretKey") as String?
    val password: String? = project.findProperty("signing.password") as String?

    if (secretKey != null && password != null) {
        useInMemoryPgpKeys(secretKey, password)
        sign(publishing.publications["mavenJava"])
    }
}



repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven {
        name = "tcoded-releases"
        url = uri("https://repo.tcoded.com/releases")
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.10.0")
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.1")
    compileOnly("me.clip:placeholderapi:2.11.6")

    implementation("com.github.maximjsx.EntityLib:spigot:1.0.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("com.tcoded:FoliaLib:0.5.1")

    implementation("com.github.HologramLib:AddonLib:1.1.0")

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
    softDepend = listOf("ItemsAdder", "PlaceholderAPI")
    name = "HologramLib"
}