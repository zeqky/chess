repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":core"))
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    implementation("io.github.zeqky:fount-api:1.0.1")
    implementation("io.github.monun:invfx-api:3.3.2")
    implementation("io.github.monun:heartbeat-coroutines:0.0.5")

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
}

tasks {
    register<Jar>("paperJar") {
        archiveVersion.set("")
        archiveBaseName.set("Chess")
        (listOf(project, project(":core"))).forEach {
            from(it.sourceSets["main"].output)
        }
        doLast {
            val plugins = File(rootDir, ".server/plugins")
            copy {
                from(archiveFile)
                into(plugins)
            }
        }
    }
}