import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.3.0-RC"
    id("com.gradleup.shadow") version "9.3.0"
    idea
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    implementation("io.github.zeqky:fount-api:1.0.1")

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
}

tasks {
    register<Jar>("paperJar") {
        archiveVersion.set("")
        archiveBaseName.set("Chess")
        from(sourceSets["main"].output)
        doLast {
            val plugins = File(rootDir, ".server/plugins")
            copy {
                from(archiveFile)
                into(plugins)
            }
        }
    }
}