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
import org.junit.Assert.assertThrows
import org.junit.Test

/** Full integration tests of source to html generation. */
class BasicTest : IntegrationTestBase() {
    @Test
    fun `Validate simple classes`() {
        validate("simple")
    }

    @Test
    fun `Validate simple top-level functions`() {
        validate("topLevelFunctions")
    }

    @Test
    fun `Validate inner classes`() {
        validate("innerClasses")
    }

    @Test
    fun `Validate prod AndroidX fragment prebuilts`() {
        validatePrebuilts(
            testName = "fragment",
            artifactNames = listOf("fragment"),
            samples = true,
            versionMetadata = true,
        )
    }

    @Test
    fun `Validate prod AndroidX lifecycle prebuilts`() {
        // lifecycle-common-java8 and lifecycle-extensions no longer exist
        validatePrebuilts(
            testName = "lifecycle",
            artifactNames =
                listOf(
                    "lifecycle-common",
                    "lifecycle-compiler",
                    "lifecycle-livedata",
                    "lifecycle-livedata-core",
                    "lifecycle-livedata-core-ktx",
                    "lifecycle-livedata-ktx",
                    "lifecycle-process",
                    "lifecycle-reactivestreams",
                    "lifecycle-reactivestreams-ktx",
                    "lifecycle-runtime",
                    "lifecycle-runtime-ktx",
                    "lifecycle-runtime-testing",
                    "lifecycle-service",
                    "lifecycle-viewmodel",
                    "lifecycle-viewmodel-ktx",
                    "lifecycle-viewmodel-savedstate",
                ),
        )
    }

    @Test
    fun `Validate AndroidX paging prebuilts`() {
        validatePrebuilts(
            testName = "paging",
            artifactNames =
                listOf(
                    "paging-common",
                    "paging-common-ktx",
                    "paging-runtime",
                    "paging-runtime-ktx",
                    "paging-rxjava2",
                    "paging-rxjava2-ktx",
                    "paging-rxjava3",
                    "paging-guava",
                    // Either don't compile testData/paging/source or dackka applies the compose
                    // plugin
                    // "paging-compose"
                ),
            samples = true,
        )
    }

    @Test
    fun `Validate @sample`() {
        validateDirectory("sampleAnnotation", sampleLocations = listOf("samples"))
    }

    @Test
    fun `Validate complicated Platform files`() {
        validate("complicatedPlatform")
    }

    @Test
    fun `Validate inheritance tests`() {
        validate("inheritance")
    }

    @Test // A non-hidden package is necessary because of an upstream explicit !! after filtering
    fun `Validate package-level @hide and custom hide annotations`() {
        validate("hidden", hidingAnnotations = listOf("dokkatest.nothidden.CustomHideAnnotation"))
    }

    @Test // Currently only checks the links for enums resolve
    fun `Validate linking`() { // CURRENT STATUS: BROKEN: LinkerClass.html enums aren't linked
        validate("linking")
    }

    @Test
    fun `Validate non-standard path arguments`() {
        validate(
            path = "differentDocPath",
            docRootPath = "reference/tools/gradle-api/7.0",
            projectPath = "",
            kotlinDocsDirectory = "",
            javaDocsDirectory = null,
            includedHeadTagsPathKotlin = null,
        )
    }

    @Test
    fun `Validate @RestrictTo`() {
        validate("restrictTo")
    }

    @Test
    fun `Validate getters setters and modifiers`() {
        validate("getterSetterModifier")
    }

    @Test
    fun `Validate @JvmMultifileClass`() {
        validate("multifile", projectPath = "multifile")
    }

    @Test
    fun `Validate companion-static interop`() {
        validate("companionStatic")
    }

    @Test
    fun `Validate default hidden parents`() {
        validate("hiddenParents")
    }

    @Test
    fun `Validate hidden parents with included symbols`() {
        validate("hiddenParents-include", includeHiddenParentSymbols = true)
    }

    @Test
    fun `Validate @VisibleForTesting`() {
        validate("visibleForTesting")
    }

    @Test
    fun `Validate that Java and Kotlin paths cannot have the same value`() {
        assertThrows(IllegalStateException::class.java) {
            validate("simple", javaDocsDirectory = null, kotlinDocsDirectory = null)
        }
        assertThrows(IllegalStateException::class.java) {
            validate("simple", javaDocsDirectory = "", kotlinDocsDirectory = "")
        }
    }
}
