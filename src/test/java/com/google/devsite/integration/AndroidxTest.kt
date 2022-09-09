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

import com.google.devsite.renderer.converters.failOnMissingSamples
import com.google.devsite.testing.IntegrationTestBase
import org.junit.After
import org.junit.Ignore
import org.junit.Test

class AndroidxTest : IntegrationTestBase() {
    @Ignore // Must be run manually
    @Test
    fun `Run dackka against full androidx tip-of-tree`() {
        TODO()
    }

    @Ignore // Must be run manually
    @Test
    fun `Run dackka against full AndroidX prebuilts`() {
        TODO("this would require hundreds of dependencies; not currently planned")
    }

    @Ignore // Must be run manually
    @Test
    fun `Run dackka against many AndroidX prebuilts`() {
        failOnMissingSamples = false // We do not (yet) publish samples source jars; b/149006789
        executePrebuilts(
            testName = "all",
            // A list of the first few source jars alphabetically, some of their dependencies, and
            // other projects chosen on as-available or as-useful-for-testing bases.
            artifactNames = listOf(
                "activity",
                "activity-ktx",
                "ads-identifier",
                "ads-identifier-common",
                "ads-identifier-provider",
                "annotation",
                "annotation-experimental",
                // "annotation-experimental-lint", // com.android.tools.lint is not a dependency
                "appcompat",
                "appcompat-resources",
                "appsearch",
                "appsearch-ktx",
                "appsearch-compiler",
                "appsearch-builtin-types",
                "appsearch-debug-view",
                "appsearch-platform-storage",
                "appsearch-local-storage",
                "core-common",
                "core-runtime",
                "core-testing",
                "asynclayoutinflater",
                "autofill",
                "benchmark",
                "benchmark-common",
                "benchmark-junit4",
                "benchmark-macro",
                "benchmark-macro-junit4",
                "benchmark-gradle-plugin",
                // "biometric", // KMP project
                // "biometric-ktx",
                "browser",
                "camera-camera2",
                "camera-camera2-pipe",
                "camera-camera2-pipe-testing",
                "camera-core",
                "camera-extensions",
                "camera-lifecycle",
                "camera-mlkit-vision",
                "camera-extensions",
                "camera-previewview",
                "camera-video",
                "camera-view",
                "camera-viewfinder",
                "app", // Part of Car; poorly named
                "app-aaos",
                "app-automotive",
                "app-projected",
                "app-testing",
                // "car", // obsolete artifacts
                // "car-cluster",
                // "car-moderator",
                "cardview",
                // collection is KMP
                // compose is KMP

                "fragment",

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

                "tracing",
                "tracing-ktx",
                "tracing-perfetto",
                "tracing-perfetto-binary",
                "tracing-perfetto-common",
            )
        )
    }

    @After
    fun tearDown() {
        failOnMissingSamples = true
    }
}
