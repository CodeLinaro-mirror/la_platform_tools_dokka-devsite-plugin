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
import com.google.devsite.testing.classpathFromFile
import java.io.File
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
import org.junit.Test
import testApi.testRunner.SourceSetsBuilder
import testApi.testRunner.TestDokkaConfigurationBuilder

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
            artifactNames =
                listOf(
                    "animation",
                    "animation-core",
                    "animation-graphics",
                    "foundation",
                    "foundation-layout",
                    "material3",
                    "material3-window-size-class",
                    "runtime",
                    "ui",
                    "ui-geometry",
                    "ui-graphics",
                    "ui-text",
                    "ui-unit",
                    "ui-util",
                    "ui-tooling",
                    "ui-tooling-preview",
                    "ui-test",
                    "ui-test-junit4",
                ),
            samples = true,
        )
    }

    private var squashAndroid = true

    /** For when a test uses source outside of `./testData/` */
    override fun TestDokkaConfigurationBuilder.makeSourcesets(
        sources: List<File>,
        samplesLocations: List<String>,
        externalLinks: List<ExternalDocumentationLinkImpl>,
    ) = multiPlatformSourceSets(sources, samplesLocations, externalLinks, squashAndroid)
}

fun TestDokkaConfigurationBuilder.multiPlatformSourceSets(
    rawSources: List<File>,
    samplesLocations: List<String>,
    externalLinks: List<ExternalDocumentationLinkImpl>,
    squashAndroid: Boolean,
) {
    var sources = rawSources
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
        documentedVisibilities =
            setOf(
                DokkaConfiguration.Visibility.PUBLIC,
                DokkaConfiguration.Visibility.PROTECTED,
            )
        this.analysisPlatform = analysisPlatform
        this.dependentSourceSets = ssDependencies
    }
    /**
     * Called on a list of directories, returns the merged list of, for each directory:
     * - If the directory contains **Main folders (e.g. jvmMain), returns the list of subfolders
     *   whose names include the passed identifier.
     * - If the directory doesn't contain **Main folders, the directory is presumably not KMP. Thus,
     *   returns the same superfolder directory IFF the identifier is 'android', else nothing.
     *
     * All output folders should be of the filepath form e.g. androidx/package/subpackage/class.kt.
     * Input KMP folders should be of the form e.g. source/commonMain/androidx/collection/Foo.kt
     * Input non-KMP folders should be of the form e.g. source/dokkatest/simple/GenericInterface.kt
     * The precise path is only truly important for java files, which parsing can be fragile.
     * Existing integration tests and androidx unzippedXXXSources folders follow this pattern.
     */
    fun List<File>.filterForPlatform(identifier: String, ignoreCase: Boolean = true) =
        filter { it.isDirectory }
            .flatMap {
                val mainSourceSetFolders = it.listFiles()!!.filter { it.path.endsWith("Main") }
                // This allows androidx and android.support non-KMP sources as inputs
                if (mainSourceSetFolders.isEmpty() && identifier == "android") listOf(it)
                // standard KMP case for *Main sourceSets.
                else mainSourceSetFolders.filter { it.name.contains(identifier, ignoreCase) }
            }

    val sourceFolders =
        listOf("jvm", "android", "native", "js")
            .associateWith { sources.filterForPlatform(it) }
            .toMutableMap()
    // Aggravatingly, this filter sorts 'nonJvmMain' as a 'jvm' sourceSet. Explicitly fix.
    val nonJvmSources = sources.filterForPlatform("nonJvm").toSet()
    sourceFolders["jvm"] = sourceFolders["jvm"]!! - nonJvmSources
    sourceFolders["android"] = sourceFolders["android"]!! - nonJvmSources
    sourceFolders["native"] = sourceFolders["native"]!! + nonJvmSources
    sourceFolders["js"] = sourceFolders["js"]!! + nonJvmSources

    if (squashAndroid) {
        sourceFolders["jvm"] = sourceFolders["jvm"]!! + sourceFolders["android"]!!
        sourceFolders.remove("android")
    } else {
        throw RuntimeException(
            "Due to upstream squashing, not squashing android into jvm isn't currently supported.",
        )
    }

    return sourceSets {
        val common =
            createSourceSet("common", sources.filterForPlatform("commonMain", ignoreCase = false))
        val dependOnCommon = setOf(common.value.sourceSetID)
        sourceFolders
            .filter { it.value.isNotEmpty() }
            .map { createSourceSet(it.key, it.value, dependOnCommon) }
    }
}
