plugins {
    java
}

group = "dev.dungeonfog"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // "+" = coge el ultimo build disponible de 26.3. Si ya sabes el build exacto, ponlo (ej: 26.3.build.12-stable)
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
}

tasks.withType<JavaCompile> {
    options.release.set(25)
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveFileName.set("DungeonFog-${project.version}.jar")
}
