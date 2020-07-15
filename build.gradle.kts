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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

defaultTasks = mutableListOf("test", "jar", "shadowJar")

repositories {
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
    maven("../dokka-utils")
    maven("../../prebuilts/androidx/external")
}

plugins {
    kotlin("jvm") version "1.4-M3"
    id("com.github.johnrengelman.shadow") version "4.0.4"
    id("application")
}

application {
    mainClassName = "org.jetbrains.dokka.MainKt"
}

val dokkaVersion = "0.11.0-SNAPSHOT"

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.dokka:dokka-base:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-cli:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    implementation("org.jetbrains.dokka:javadoc-plugin:$dokkaVersion")

    testImplementation("junit:junit:4.12")
    testImplementation("org.jetbrains.dokka:dokka-test-api:$dokkaVersion")
    testImplementation("org.jsoup:jsoup:1.12.1")
}

group = "com.google.devsite"
version = "0.0.1-alpha01" // This is appended to archiveBaseName in the ShadowJar task.

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("dokka-devsite-plugin-full")
    archiveClassifier.set(null as String?)
    archiveVersion.set(null as String?)
    isZip64 = true
    destinationDirectory.set(project.buildDir)
}
