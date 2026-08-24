/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.devsite.integration

import com.google.devsite.testing.IntegrationTestBase
import org.junit.Test

class KmpTest : IntegrationTestBase() {
    @Test
    fun `Simple KMP classes test`() {
        validate("simple-kmp")
    }

    @Test
    fun `Single-platform KMP package test`() {
        validate("singlePlatformKMP", projectPath = "commonMain")
    }

    @Test
    fun `Validate prod AndroidX collections prebuilts`() {
        validate("collections", projectPath = "androidx", useAndroidxBaseSourceLink = true)
    }

    @Test
    fun `Validate prod AndroidX annotations prebuilts`() {
        validate("annotation-kmp", projectPath = "androidx", useAndroidxBaseSourceLink = true)
    }

    @Test
    fun `Validate prod AndroidX compose prebuilts`() {
        validate(
            "compose",
            projectPath = "androidx",
            useAndroidxBaseSourceLink = true,
            javaDocsDirectory = null,
        )
    }

    @Test
    fun `Validate KMP samples`() {
        validate("sampleKmp")
    }
}
