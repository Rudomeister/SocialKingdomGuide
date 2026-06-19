plugins {
    java
}

group = "no.socialkingdom"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filesMatching(listOf("plugin.yml", "config.yml")) {
        expand(
            "version" to project.version,
            "name" to project.name,
        )
    }
}
