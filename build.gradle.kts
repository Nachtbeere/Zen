import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "net.nachtbeere.minecraft"
val pluginGroup = group
version = "0.5.1-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/public/") }
    maven { url = uri("https://dl.bintray.com/ichbinjoe/public/") }
}

val shadowImplementation by configurations.creating
configurations["compileOnly"].extendsFrom(shadowImplementation)
configurations["testImplementation"].extendsFrom(shadowImplementation)

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    compileOnly("com.destroystokyo.paper:paper-api:1.15.2-R0.1-SNAPSHOT")
    compileOnly("com.vexsoftware:nuvotifier-universal:2.6.0")
    compileOnly("net.luckperms", "api", "5.2")
    implementation("org.ktorm", "ktorm-core", "3.3.0")
    implementation("com.zaxxer", "HikariCP", "3.4.5")
    implementation("org.xerial", "sqlite-jdbc", "3.34.0")
    implementation("mysql", "mysql-connector-java", "8.0.21")
    implementation("io.github.microutils", "kotlin-logging", "1.12.0")
    testImplementation(kotlin("test-junit"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

//    val shadowJarTask = named<ShadowJar>("shadowJar")
//
//    shadowJarTask {
//        dependencies {
//            include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
//            include(dependency("org.jetbrains.kotlin:kotlin-reflect"))
//            include(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm"))
//            include(dependency("com.zaxxer:HikariCP"))
//            include(dependency("org.xerial:sqlite-jdbc"))
//            include(dependency("mysql:mysql-connector-java"))
//            include(dependency("org.jetbrains.exposed:exposed-core"))
//            include(dependency("org.jetbrains.exposed:exposed-dao"))
//            include(dependency("org.jetbrains.exposed:exposed-jdbc"))
//            include(dependency("org.jetbrains.exposed:exposed-java-time"))
//        }
//        relocate("com.zaxxer.hikari", "$pluginGroup.libs.hikari")
//        relocate("org.jetbrains.exposed", "$pluginGroup.libs.exposed")
//        relocate("org.sqlite", "$pluginGroup.libs.sqlite")
//        relocate("com.mysql", "$pluginGroup.libs.mysql")
//        mergeServiceFiles()
//    }
}