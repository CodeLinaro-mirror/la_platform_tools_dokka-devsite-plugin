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
        validateDirectory("simple")
    }

    @Test
    fun `Validate simple top-level functions`() {
        validateDirectory("topLevelFunctions")
    }

    @Test
    fun `Validate inner classes`() {
        validateDirectory("innerClasses")
    }

    // Cannot be migrated to validatePrebuilts because we don't yet support KMP
    @Test
    fun `Validate prod AndroidX compose lib`() {
        validateDirectory("compose", sampleLocations = listOf("samples"))
    }

    // We can certainly remove this test once the migration to validatePrebuilts is complete
    @Test
    fun `Validate AndroidX fragment sources and prebuilts generate identical docs`() {
        // By sharing the same path, this test validates against the same goldens the next test does
        validateDirectory("fragment", sampleLocations = listOf("samples"))
    }

    @Test
    fun `Validate prod AndroidX fragment prebuilts`() {
        validatePrebuilts(
            testName = "fragment",
            artifactNames = listOf("fragment"),
            samples = true
        )
    }

    @Test
    fun `Validate prod AndroidX lifecycle prebuilts`() {
        // lifecycle-common-java8 and lifecycle-extensions no longer exist
        validatePrebuilts(
            testName = "lifecycle",
            artifactNames = listOf(
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
                "lifecycle-viewmodel-savedstate"
            )
        )
    }

    // Cannot be migrated to validatePrebuilts because we don't yet support KMP
    @Test
    fun `Validate prod AndroidX paging lib`() {
        validateDirectory(
            path = "paging",
            sampleLocations = listOf("samples"),
            includeFiles = listOf("metadata.md")
        )
    }

    // Cannot be migrated to validatePrebuilts because we don't yet support KMP
    @Test
    fun `Validate prod AndroidX collections-ktx lib`() {
        validateDirectory("collections-ktx")
    }

    @Ignore // Does not work; KMP problems; collection-jvm doesn't label source jar properly
    @Test
    fun `Validate prod AndroidX collections prebuilts`() {
        validatePrebuilts(
            testName = "collections",
            artifactNames = listOf("collection", "collection-jvm", "collection-ktx"),
        )
    }

    @Test
    fun `Validate @sample`() {
        validateDirectory(
            "sampleAnnotation",
            sampleLocations = listOf("samples")
        )
    }

    @Test
    fun `Validate complicated Platform files`() {
        validateDirectory("complicatedPlatform")
    }

    @Test
    fun `Validate inheritance tests`() {
        validateDirectory("inheritance")
    }

    @Test // A non-hidden package is necessary because of an upstream explicit !! after filtering
    fun `Validate package-leve @hide`() {
        validateDirectory("hidden")
    }

    @Test // Currently only checks the links for enums resolve
    fun `Validate linking`() { // CURRENT STATUS: BROKEN: LinkerClass.html enums aren't linked
        validateDirectory("linking")
    }

    @Test
    fun `Validate versioned directory support`() {
        validateDirectory("simpleVersioned", versionedTenant = "tools/gradle-api/7.0")
    }

    @Test
    fun `Validate @RestrictTo`() {
        validateDirectory("restrictTo")
    }

    @Test
    fun `Validate @JvmMultifile`() {
        validateDirectory("multifile")
    }

    @Test
    fun `Validate getters setters and modifiers`() {
        validateDirectory("getterSetterModifier")
    }

    @Test
    fun `Validate @JvmMultifileClass`() {
        validateDirectory("multifile")
    }

    @Test
    fun `Validate companion-static interop`() {
        validateDirectory("companionStatic")
    }
}
