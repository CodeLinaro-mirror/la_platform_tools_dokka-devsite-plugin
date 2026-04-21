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
    alias(libs.plugins.kotlin)
    `java-gradle-plugin`
    alias(libs.plugins.ktfmt)
}

repositories { maven("../../../prebuilts/dokka-devsite-plugin") }

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(libs.kotlin.gradle)
    implementation(libs.dokka.test.api)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.module.kotlin)
}

gradlePlugin {
    plugins {
        create("dackka-test-plugin") {
            id = "dackka-test-plugin"
            implementationClass = "com.google.devsite.DackkaTestPlugin"
        }
    }
}

kotlin { compilerOptions { allWarningsAsErrors = true } }

ktfmt { kotlinLangStyle() }
