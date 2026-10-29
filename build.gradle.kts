plugins {
    java
}

group = "pl.axon"
version = "0.2.0"

repositories {
    // Repo Papera ma też kopię Maven Central, więc idzie pierwsze (Central potrafi odpowiadać 429).
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
    // WorldGuard/WorldEdit tylko z repo EngineHub i bez czytania POM-ów (ich rodzice z Maven Central nie są potrzebne).
    exclusiveContent {
        forRepository {
            maven("https://maven.enginehub.org/repo/") {
                metadataSources { artifact() }
            }
        }
        filter { includeGroupByRegex("com\\.sk89q\\..*") }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    // Opcjonalnie: regiony struktur i odstęp od działek AxonPlot.
    // Bez zależności przechodnich: ich wersje fastutil gryzą się z paper-api.
    listOf(
        "com.sk89q.worldguard:worldguard-core:7.0.12",
        "com.sk89q.worldguard:worldguard-bukkit:7.0.12",
        "com.sk89q.worldedit:worldedit-core:7.3.0",
        "com.sk89q.worldedit:worldedit-bukkit:7.3.0",
    ).forEach { compileOnly(it) { isTransitive = false } }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Testy wczytują config.yml przez YamlConfiguration, więc potrzebują Paper API także w testach.
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
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
