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
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.ExternalDocumentationLink
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
    fun makeConfiguration(
        sources: List<File>,
        samplesBaseDir: String,
        samplesLocations: List<String>,
        includeFiles: List<String>
    ): DokkaConfigurationImpl {
        sources.forEach { check(it.isDirectory) { "$it does not exist or is not a directory" } }
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
        return dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = sources.map { it.absolutePath }
                    classpath = classpathFromFile("testData/classpath.txt")
                    externalDocumentationLinks = externalLinks
                    samples = samplesLocations.map { "$samplesBaseDir/$it" }
                    includes = includeFiles.map { File(sources.first(), it).absolutePath }
                    documentedVisibilities = setOf(
                        DokkaConfiguration.Visibility.PUBLIC,
                        DokkaConfiguration.Visibility.PROTECTED
                    )
                }
            }
            offlineMode = true
        }
    }

    fun makeConfiguration(
        samplesBaseDir: String,
        sourceDir: String,
        sampleLocations: List<String> = emptyList(),
        includeFiles: List<String> = emptyList()
    ): DokkaConfigurationImpl {
        val sources = File(sourceDir).absoluteFile
        return makeConfiguration(listOf(sources), samplesBaseDir, sampleLocations, includeFiles)
    }

    fun setEnvVarsForTests(inferredTenant: String, versionedTenant: String? = null) {
        if (versionedTenant != null) {
            System.setProperty("versionedTenant", versionedTenant)
            System.clearProperty("tenant")
        } else {
            System.setProperty("tenant", inferredTenant)
            System.clearProperty("versionedTenant")
        }
    }

    fun executionTest(
        vararg paths: String,
        sampleLocations: List<String> = emptyList(),
        includeFiles: List<String> = emptyList(),
        versionedTenant: String? = null
    ) {
        val samplesBaseDir = null // TODO

        val configuration = makeConfiguration(
            paths.map { File(it).absoluteFile },
            paths.reduce { a: String, b: String ->
                a.asIterable().intersect(b.asIterable()).joinToString()
            },
            sampleLocations,
            includeFiles
        )

        val inferredTenant = "androidx"
        setEnvVarsForTests(inferredTenant, versionedTenant)

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin),
        ) { }
    }

    /**
     * Reads sources and outputs from a directory, and validates based on them.
     *
     * Sources are located at testData/$path/source
     * outputs are located at testData/$path/docs
     */
    fun verifyDirectory(
        path: String,
        sampleLocs: List<String> = emptyList(),
        includeFiles: List<String> = emptyList(),
        versionedTenant: String? = null
    ) {
        val samplesBaseDir = "testData/$path"
        val outputBaseDir = "testData/$path/docs"
        val sourceDir = "testData/$path/source"

        val configuration = makeConfiguration(samplesBaseDir, sourceDir, sampleLocs, includeFiles)

        val inferredTenant = File(sourceDir).listFiles().orEmpty()
            .singleOrNull { it.isDirectory }?.name ?: "dokkatest"
        setEnvVarsForTests(inferredTenant, versionedTenant)

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                verifyOutput(writerPlugin, outputBaseDir)
            }
        }
    }

    private fun classpathFromFile(file: String): List<String> =
        File(file).bufferedReader().readLines()

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
        val expectedFileList = File(outputPath).recursivelyListFiles()
        // This same "fix" happens automatically when the file is written, so it's needed to match
        val fixedGeneratedPaths = generatedFiles.keys.map { it.replace("//", "/") }
        for (eFile in expectedFileList) {
            assertWithMessage("File ${eFile.path} was expected but not generated!")
                .that(eFile.path.removePrefix(outputPath) in fixedGeneratedPaths).isTrue()
        }
    }

    fun File.recursivelyListFiles(): List<File> =
        (this.listFiles { it: File -> !it.isDirectory }?.asList() ?: emptyList()) + (
            this.listFiles { it: File -> it.isDirectory }
                ?.flatMap { it: File -> it.recursivelyListFiles() } ?: emptyList()
            )

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
