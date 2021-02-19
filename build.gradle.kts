plugins {
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

group = "net.nachtbeere.minecraft"
version = "0.4-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/public/") }
    maven { url = uri("https://dl.bintray.com/ichbinjoe/public/") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.destroystokyo.paper:paper-api:1.15.2-R0.1-SNAPSHOT")
    implementation("com.vexsoftware:nuvotifier-universal:2.6.0")
    implementation("org.jetbrains.exposed", "exposed-core", "0.28.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.28.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.28.1")
    implementation("org.jetbrains.exposed", "exposed-java-time", "0.28.1")
    implementation("com.zaxxer", "HikariCP", "3.4.5")
    implementation("org.xerial", "sqlite-jdbc", "3.32.3.2")
    implementation("mysql", "mysql-connector-java", "8.0.21")
    implementation("net.luckperms", "api", "5.2")
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
}