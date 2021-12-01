/*
 * Copyright 2020 The Android Open Source Project
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
import org.junit.Ignore
import org.junit.Test

/**
 * Full integration tests of source to html generation.
 */
class BasicTest : IntegrationTestBase() {
    @Test
    fun `Validate simple classes`() {
        verifyDirectory("simple")
    }

    @Test
    fun `Validate simple top-level functions`() {
        verifyDirectory("topLevelFunctions")
    }

    @Test
    fun `Validate inner classes`() {
        verifyDirectory("innerClasses")
    }

    @Test
    fun `Validate prod AndroidX compose lib`() {
        verifyDirectory("compose", sampleLocations = listOf("samples"))
    }

    @Test
    fun `Validate prod AndroidX fragment lib`() {
        verifyDirectory("fragment", sampleLocations = listOf("samples"))
    }

    @Test
    fun `Validate prod AndroidX paging lib`() {
        verifyDirectory("paging",
            sampleLocations = listOf("samples"),
            includeFiles = listOf("metadata.md"))
    }

    @Test
    fun `Validate prod AndroidX collections-ktx lib`() {
        verifyDirectory("collections-ktx")
    }

    @Test
    fun `Validate @sample`() {
        verifyDirectory("sampleAnnotation", sampleLocations = listOf("samples"))
    }

    @Test
    fun `Validate complicated Platform files`() {
        verifyDirectory("complicatedPlatform")
    }

    @Ignore // go/dokka-upstream-bug/2187 Inherited function type resolution is flaky
    @Test
    fun `Validate inheritance tests`() {
        verifyDirectory("inheritance")
    }

    @Test
    fun `Validate package-leve @hide`() {
        verifyDirectory("hidden")
    }

    @Test // Currently only checks the links for enums resolve
    fun `Validate linking`() { // CURRENT STATUS: BROKEN: LinkerClass.html enums aren't linked
        verifyDirectory("linking")
    }

    @Test
    fun `Validate versioned directory support`() {
        verifyDirectory("simpleVersioned", versionedTenant = "tools/gradle-api/7.0")
    }
}
