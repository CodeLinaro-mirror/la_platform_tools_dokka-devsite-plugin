/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:4.0.4")
    }
}

defaultTasks = mutableListOf("test", "jar", "shadowJar")

repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-dev")
}

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "4.0.4"
    id("application")
}

application {
    mainClassName = "com.example.Todo" // TODO: determine main class
}

val dokkaVersion = "0.11.0-dev-41"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.dokka", "dokka-core", dokkaVersion)
    implementation("org.jetbrains.dokka", "dokka-core-dependencies", dokkaVersion)
    testImplementation("junit", "junit", "4.12")
}

group = "com.google.devsite"
version = "0.0.1-alpha01" // This is appended to archiveBaseName in the ShadowJar task.
tasks.withType(ShadowJar::class.java) {
    archiveBaseName.set("dokka-devsite-plugin-full")
    archiveClassifier.set(null as String?)
    archiveVersion.set(null as String?)
    setZip64(true)
    destinationDirectory.set(project.buildDir)
}