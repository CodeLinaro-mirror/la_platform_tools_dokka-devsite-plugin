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

package com.google.devsite.testing

import com.google.common.truth.Truth.assertWithMessage
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import java.io.File
import java.net.URL

/**
 * Full integration tests of source to html generation.
 *
 * Html output results can be found in testData/
 */
abstract class IntegrationTestBase : BaseAbstractTest(
    logger = TestLogger(DokkaConsoleLogger(LoggingLevel.WARN))
) {
    /**
     * Reads sources and outputs from a directory, and validates based on them.
     *
     * Sources are located at testData/$path/source
     * outputs are located at testData/$path/docs
     */
    fun verifyDirectory(
        path: String,
        sampleLocations: List<String> = emptyList(),
        includeFiles: List<String> = emptyList(),
        versionedTenant: String? = null
    ) {
        val baseDir = "testData/$path"
        val sourceDir = "$baseDir/source"

        val externalLinks = mapOf(
            "coroutines" to "https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core",
            "android" to "https://developer.android.com/reference",
            "guava" to "https://guava.dev/releases/18.0/api/docs/package-list",
            "kotlin" to "https://kotlinlang.org/api/latest/jvm/stdlib/"
        ).map {
            ExternalDocumentationLink(
                url = URL(it.value),
                packageListUrl = File("testData").toPath()
                    .resolve("package-lists/${it.key}/package-list").toUri().toURL()
            )
        }
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    val sources = File(sourceDir).absoluteFile
                    check(sources.isDirectory) { "$sources does not exist or is not a directory" }
                    sourceRoots = listOf(sources.absolutePath)
                    classpath = listOfNotNull(jvmStdlibPath, commonStdlibPath)
                    perPackageOptions += PackageOptionsImpl(
                        matchingRegex = "androidx.annotation",
                        includeNonPublic = false,
                        reportUndocumented = false,
                        skipDeprecated = false,
                        suppress = true
                    )
                    externalDocumentationLinks = externalLinks
                    samples = sampleLocations.map { "$baseDir/$it" }
                    includes = includeFiles.map { File(sources, it).absolutePath }
                }
            }
            offlineMode = true
        }

        if (versionedTenant != null) {
            System.setProperty("versionedTenant", versionedTenant)
            System.clearProperty("tenant")
        } else {
            val inferredTenant = File(sourceDir).listFiles().orEmpty()
                .singleOrNull { it.isDirectory }?.name ?: "dokkatest"
            System.setProperty("tenant", inferredTenant)
            System.clearProperty("versionedTenant")
        }

        val writerPlugin = TestOutputWriterPlugin()
        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                verifyOutput(writerPlugin, "$baseDir/docs")
            }
        }
    }

    /** Confirms that the given output writer's output matches the contents of the given directory. */
    private fun verifyOutput(writerPlugin: TestOutputWriterPlugin, outputPath: String) {
        val outputDirectory = File(outputPath).absolutePath
        val generatedFiles = writerPlugin.writer.contents

        val dumpedFile = File("build/docs/$outputPath")
        dump(writerPlugin, dumpedFile.absolutePath)

        for ((fileName, generatedContent) in generatedFiles) {
            val expectedFile = File(outputDirectory, fileName)
            val expectedText = if (expectedFile.exists()) {
                expectedFile.readText()
            } else {
                ""
            }

            if (expectedText != generatedContent) {
                val message = """
                    |Unexpected output in $fileName.
                    |To update the expected output to match the current output, run this command:
                    |
                    |  rm -rf $outputDirectory && cp -r ${dumpedFile.absolutePath} $outputDirectory
                    |
                    |Difference in outputs:
                """.trimMargin()
                assertWithMessage(message).that(generatedContent).isEqualTo(expectedText)
            }
        }
    }

    /** Exports the output of writerPlugin to outputPath. */
    private fun dump(writerPlugin: TestOutputWriterPlugin, outputPath: String) {
        val outputDirectory = File(outputPath)
        outputDirectory.deleteRecursively()
        val generatedFiles = writerPlugin.writer.contents

        for ((fileName, fileContent) in generatedFiles) {
            val expectedFile = File(outputDirectory, fileName)
            expectedFile.parentFile.mkdirs()
            expectedFile.writeText(fileContent)
        }
    }
}
