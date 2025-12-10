plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    kotlin("jvm") version "1.9.24"
}

group = "com.lancontrol"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainClass.set("com.lancontrol.MainKt")
    applicationName = "LANControl"
}

javafx {
    version = "23"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.media")
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.8.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.lancontrol.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    archiveBaseName.set("LANControl")
}
