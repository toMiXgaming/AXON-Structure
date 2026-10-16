plugins {
    java
}

group = "pl.axon"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    // Opcjonalnie: regiony struktur i odstęp od działek AxonPlot.
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12") {
        exclude(group = "org.bukkit")
        exclude(group = "io.papermc.paper")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

// Generator plików struktur (.axs). Nie trafia do jara pluginu.
val generator: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output
}

tasks.register<JavaExec>("generujStruktury") {
    group = "axon"
    description = "Zapisuje struktury zdefiniowane w kodzie do build/struktury/*.axs"
    classpath = generator.runtimeClasspath
    mainClass.set("pl.axon.structure.generator.StructureExporter")
    args(layout.buildDirectory.dir("struktury").get().asFile.path)
}
