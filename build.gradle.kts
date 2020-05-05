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

plugins {
    kotlin("jvm") version "1.3.71"
}

repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-dev")
}

val dokkaVersion = "0.11.0-dev-41"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.dokka", "dokka-core", dokkaVersion)
    implementation("org.jetbrains.dokka", "dokka-core-dependencies", dokkaVersion)
    testImplementation("junit", "junit", "4.12")
}
