/*
 * Copyright 2026 The Android Open Source Project
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
    kotlin("multiplatform")
    id("dackka-test-plugin")
}

kotlin {
    jvm()

    sourceSets { commonMain { kotlin.setSrcDirs(listOf("source/commonMain")) } }
}

// The stdlib-common jar is needed to resolve dependencies in commonMain, but doesn't end up in the
// dependencies for any particular compilation.
dependencies { testClasspath("org.jetbrains.kotlin:kotlin-stdlib-common:1.8.21") }
