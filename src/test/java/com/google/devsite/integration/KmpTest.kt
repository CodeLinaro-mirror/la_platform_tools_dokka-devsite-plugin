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

import com.google.devsite.capitalize
import com.google.devsite.testing.IntegrationTestBase
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
import org.junit.Test
import testApi.testRunner.SourceSetsBuilder
import testApi.testRunner.TestDokkaConfigurationBuilder
import java.io.File
import kotlin.test.Ignore

class KmpTest : IntegrationTestBase() {
    @Test
    fun `Simple KMP classes test`() {
        squashAndroid = true
        validateDirectory("simple-kmp")
    }

    @Test
    fun `Single-platform KMP package test`() {
        squashAndroid = true
        validateDirectory("singlePlatformKMP")
    }

    @Test
    fun `Validate prod AndroidX collections prebuilts`() {
        validatePrebuilts(
            testName = "collections",
            artifactNames = listOf("collection"),
        )
    }

    @Ignore("b/326147716")
    @Test
    fun `Validate prod AndroidX datastore-core prebuilts`() {
        squashAndroid = true
        validatePrebuilts(
            testName = "datastore-kmp",
            artifactNames = listOf("datastore-core"),
            samples = true,
        )
    }

    @Test
    fun `Validate prod AndroidX annotations prebuilts`() {
        squashAndroid = true
        validatePrebuilts(
            testName = "annotation-kmp",
            artifactNames = listOf("annotation"),
        )
    }

    @Test
    fun `Validate prod AndroidX compose prebuilts`() {
        validatePrebuilts(
            testName = "compose",
            artifactNames = listOf(
                "animation", "animation-core", "animation-graphics",
                "foundation", "foundation-layout",
                "material3", "material3-window-size-class",
                "runtime",
                "ui", "ui-geometry", "ui-graphics", "ui-text", "ui-unit", "ui-util",
                "ui-tooling", "ui-tooling-preview",
                "ui-test", "ui-test-junit4",
            ),
            samples = true,
        )
    }

    private var squashAndroid = true

    /** For when a test uses source outside of `./testData/` */
    override fun TestDokkaConfigurationBuilder.makeSourcesets(
        sources: List<File>,
        samplesLocations: List<String>,
        includeFiles: List<String>,
        externalLinks: List<ExternalDocumentationLinkImpl>,
    ) {
        fun SourceSetsBuilder.createSourceSet(
            name: String,
            sourcesOfPlatform: List<File>,
            ssDependencies: Set<DokkaSourceSetID> = emptySet(),
            displayName: String = name.capitalize(),
            analysisPlatform: String = name,
        ) = sourceSet {
            this.name = name
            this.displayName = displayName
            samples = if ("ommon" in name) samplesLocations else emptyList()
            sourceRoots = sourcesOfPlatform.map { it.absolutePath }
            classpath = classpathFromFile("testData/classpath.txt")
            externalDocumentationLinks = externalLinks
            documentedVisibilities = setOf(
                DokkaConfiguration.Visibility.PUBLIC,
                DokkaConfiguration.Visibility.PROTECTED,
            )
            this.analysisPlatform = analysisPlatform
            this.dependentSourceSets = ssDependencies
        }
        fun List<File>.filterForPlatform(identifier: String) = filter { it.isDirectory }.flatMap {
            it.listFiles()?.filter { identifier.lowercase() in it.name.lowercase() } ?: emptyList()
        }

        val sourceFolders = listOf("jvm", "android", "native", "js")
            .associateWith { sources.filterForPlatform(it) }.toMutableMap()

        if (squashAndroid) {
            sourceFolders["jvm"] = sourceFolders["jvm"]!! + sourceFolders["android"]!!
            sourceFolders.remove("android")
        } else {
            throw RuntimeException(
                "Due to upstream squashing, not squashing android into jvm isn't currently " +
                    "supported.",
            )
        }

        return sourceSets {
            val common = createSourceSet("common", sources.filterForPlatform("common"))
            val dependOnCommon = setOf(common.value.sourceSetID)
            sourceFolders.filter { it.value.isNotEmpty() }.map {
                createSourceSet(it.key, it.value, dependOnCommon)
            }
        }
    }
}
