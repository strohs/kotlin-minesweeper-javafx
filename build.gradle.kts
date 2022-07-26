import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.cliff"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.7.10"
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
}

javafx {
    modules("javafx.base", "javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("org.cliff.kotsweeper.KSweeper")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}