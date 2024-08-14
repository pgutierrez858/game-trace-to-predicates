plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "dev.pablogutierrez"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

sourceSets {
    main {
        java {
            srcDir("src/main/kotlin")
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    // https://mvnrepository.com/artifact/com.opencsv/opencsv
    implementation(files("libs/rabinizer3.1.jar"))
    // https://mvnrepository.com/artifact/org.moeaframework/moeaframework
    implementation("org.moeaframework:moeaframework:4.2")
    // https://mvnrepository.com/artifact/com.googlecode.json-simple/json-simple
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
    implementation("com.github.haifengl:smile-kotlin:3.1.1")
    // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-api-kotlin
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
    // implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    // implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    // implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.23.1")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.slf4j:slf4j-simple:2.0.16")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}