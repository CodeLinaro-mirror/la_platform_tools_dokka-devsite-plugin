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
import java.io.File
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import testApi.testRunner.TestDokkaConfigurationBuilder

/** Allows testing against lots of androidx sources, e.g. for profiling purposes. */
class AndroidxTest : IntegrationTestBase() {
    private fun getAndroidxPath(): String {
        val androidxPath = File("path_to_androidx_checkout.txt").readLines().singleOrNull()?.trim()
        logger.debug("Path to androidx checkout: $androidxPath")
        assert(androidxPath != null && androidxPath.endsWith("androidx-main/frameworks/support")) {
            "In order to run this test, you must provide a (.gitignore'd) file " +
                "'path_to_androidx_checkout.txt' in the checkout root, containing a valid path to" +
                "an androidx-main checkout on the same machine. The file should not contain quotes."
        }
        return androidxPath!!
    }

    @Ignore // Must be run manually
    @Test
    fun `Run dackka against partial androidx tip-of-tree`() {
        val base = getAndroidxPath()
        executionTest(
            testName = "partialAndroidx",
            paths =
                listOf(
                    "$base/appcompat/",
                    "$base/fragment/",
                    "$base/leanback/",
                    "$base/media/",
                ),
            sampleLocations = listOf("$base/samples/", "$base/fragment/fragment-compose/samples"),
        )
    }

    @Ignore // Must be run manually
    @Test
    fun `Run dackka against androidx docs-public jvm sources`() {
        val base = getAndroidxPath()
        val docsPublic = "${getAndroidxPath()}/../../out/androidx/docs-public"
        executionTest(
            testName = "androidxDocsPublicJvm",
            paths =
                listOf(
                    "$docsPublic/build/unzippedJvmSources/",
                ),
            sampleLocations =
                listOf(
                    "$docsPublic/build/unzippedJvmSampleSources/",
                    "$docsPublic/build/unzippedDeprecatedSampleSources/",
                    "$base/samples/"
                ),
        )
    }

    @Ignore // Must be run manually
    @Test
    fun `Run dackka against androidx docs-public multiplatform sources`() {
        val base = getAndroidxPath()
        val docsPublic = "${getAndroidxPath()}/../../out/androidx/docs-public"
        executionTest(
            testName = "androidxDocsPublicMultiplatform",
            paths =
                listOf(
                    "$docsPublic/build/unzippedJvmSources/",
                    "$docsPublic/build/unzippedMultiplatformSources/",
                ),
            sampleLocations =
                listOf(
                    "$docsPublic/build/unzippedJvmSampleSources/",
                    "$docsPublic/build/unzippedDeprecatedSampleSources/",
                    "$docsPublic/build/unzippedMultiplatformSampleSources/",
                    "$base/samples/",
                ),
        )
    }

    @Ignore // Must be run manually
    @Test
    fun `Run dackka against full androidx tip-of-tree`() {
        // some projects are not intended to be documented and have a large backlog of docs issues
        val excludedPaths =
            mutableListOf(
                "lint-checks",
                "room-compiler",
                "camera-camera2-pipe-integration",
                "integration-tests", // Specifically paging
                "watchface-samples-minimal-instances",
                "watchface-samples-minimal-complications",
                "watchface-samples-minimal-style",
                "generator", // material-icons-generator
                "appsearch-builtin-types",
            )
        crawlingExecTest("fullAndroidx", getAndroidxPath(), excludedPaths, maxFolders = 200)
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
            testName = "manyAndroidxPrebuilts",
            // A list of the first few source jars alphabetically, some of their dependencies, and
            // other projects chosen on as-available or as-useful-for-testing bases.
            artifactNames =
                listOf(
                    "activity",
                    "activity-ktx",
                    "annotation",
                    "annotation-experimental",
                    // "annotation-experimental-lint", // com.android.tools.lint is not a dependency
                    "appcompat",
                    "appcompat-resources",
                    "appsearch",
                    "appsearch-ktx",
                    "appsearch-compiler",
                    // "appsearch-builtin-types", // still broken until the next release
                    "appsearch-debug-view",
                    "appsearch-platform-storage",
                    "appsearch-local-storage",
                    "core-common",
                    "core-runtime",
                    "core-testing",
                    "asynclayoutinflater",
                    "autofill",
                    "benchmark-common",
                    "benchmark-junit4",
                    "benchmark-macro",
                    "benchmark-macro-junit4",
                    "benchmark-gradle-plugin",
                    // "biometric", // KMP project
                    // "biometric-ktx",
                    "browser",
                    "camera-camera2",
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
                ),
        )
    }

    @After
    fun tearDown() {
        failOnMissingSamples = true
    }

    override fun TestDokkaConfigurationBuilder.makeSourcesets(
        sources: List<File>,
        samplesLocations: List<String>,
        includeFiles: List<String>,
        externalLinks: List<ExternalDocumentationLinkImpl>,
    ) {
        if (sources.size == 1) {
            return singlePlatformSourceSets(sources, samplesLocations, includeFiles, externalLinks)
        }
        return multiPlatformSourceSets(sources, samplesLocations, includeFiles, externalLinks, true)
    }
}
