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
import com.google.devsite.DevsiteConfiguration
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.toJsonString
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
    logger = TestLogger(DokkaConsoleLogger(LoggingLevel.DEBUG))
) {
    /** For when a test uses source outside of `./testData/` */
    open fun makeExternalConfiguration(
        sources: List<File>,
        samplesLocations: List<String>,
        includeFiles: List<String> = emptyList(),
        docRootPath: String,
        projectPath: String,
        javaDocsPath: String?,
        kotlinDocsPath: String?
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
                // TODO: improve package-list updateability b/243840381
                packageListUrl = File("testData").toPath()
                    .resolve("package-lists/${it.key}/package-list").toUri().toURL()
            )
        }
        return dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = sources.map { it.absolutePath }
                    // TODO: find a workaround to using a fixed classpath file b/243842129
                    classpath = classpathFromFile("testData/classpath.txt")
                    externalDocumentationLinks = externalLinks
                    samples = samplesLocations
                    includes = includeFiles
                    documentedVisibilities = setOf(
                        DokkaConfiguration.Visibility.PUBLIC,
                        DokkaConfiguration.Visibility.PROTECTED
                    )
                }
            }
            offlineMode = true
            pluginsConfigurations = mutableListOf(
                PluginConfigurationImpl(
                    fqPluginName = "com.google.devsite.DevsitePlugin",
                    serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
                    values = DevsiteConfiguration(
                        docRootPath = docRootPath,
                        projectPath = projectPath,
                        excludedPackages = null,
                        excludedPackagesForJava = null,
                        excludedPackagesForKotlin = null,
                        libraryMetadataFilename = null,
                        javaDocsPath = javaDocsPath,
                        kotlinDocsPath = kotlinDocsPath,
                    ).toJsonString()
                )
            )
        }
    }

    /** For when a test uses source in `./testData/` */
    fun makeInternalConfiguration(
        samplesBaseDir: String,
        sourceDir: String,
        sampleLocations: List<String> = emptyList(),
        includeFiles: List<String> = emptyList(),
        docRootPath: String,
        projectPath: String,
        javaDocsDirectory: String?,
        kotlinDocsDirectory: String?
    ): DokkaConfigurationImpl {
        val sources = File(sourceDir).absoluteFile
        return makeExternalConfiguration(
            listOf(sources),
            sampleLocations.map { "$samplesBaseDir/$it" },
            includeFiles.map { File(sourceDir, it).absolutePath },
            docRootPath,
            projectPath,
            javaDocsDirectory,
            kotlinDocsDirectory
        )
    }

    /** Executes dackka on source from an androidx checkout on the same machine. No validation. */
    fun executionTest(
        paths: List<String>,
        sampleLocations: List<String> = emptyList(),
        includeFiles: List<String> = emptyList(),
    ) {
        val configuration = makeExternalConfiguration(
            paths.map { File(it).absoluteFile },
            sampleLocations,
            includeFiles,
            docRootPath = "reference",
            projectPath = "androidx",
            javaDocsPath = "",
            kotlinDocsPath = "kotlin"
        )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin),
        ) { }
    }

    /**
     * Executes dackka on source from an androidx checkout on the same machine. No validation.
     * @param maxFolders limits the source files run against, in case of performance issues
     */
    fun crawlingExecTest(
        checkoutRoot: String,
        excludedPaths: MutableList<String> = mutableListOf(),
        maxFolders: Int = 999999
    ) {
        // We never intend to run dackka on these folders
        excludedPaths += listOf(
            "test", // "test" folders don't contain "main"; stop recursion
            "development", // A folder of scripts
            "buildSrc", // Not published or documented
            "frameworks", // basically a recursive symlink to checkout-root
            "annotation-sampled" // not published and its docs confuse the samples system
        )
        var sourceRoots = mutableListOf<File>()
        val samplesRoots = mutableSetOf<String>() // Due to symlinks, we need to de-dupe Support4
        // We want to limit unnecessary recursion work, and we can check that no main folder is more
        // than 6 folders deep from the checkout root, as folder structure ~ package structure.
        val MAX_DEPTH = 6
        var currentDirs = listOf(File(checkoutRoot))
        var nextDirs = mutableListOf<File>()
        for (i in 0..MAX_DEPTH) {
            currentDirs.forEach { parent ->
                parent.listFiles { child -> child.isDirectory }?.forEach { child ->
                    when (child.name) {
                        "main" -> sourceRoots += child
                        "samples" -> samplesRoots += child.absolutePath
                        in excludedPaths -> {}
                        else -> nextDirs += child
                    }
                }
            }
            currentDirs = nextDirs
            nextDirs = mutableListOf()
            if (sourceRoots.size > maxFolders) break
        }
        sourceRoots = sourceRoots.take(maxFolders).toMutableList()

        logger.debug("Number of main folders found: ${sourceRoots.size}")
        logger.debug("Number of samples folders found: ${samplesRoots.size}")

        val configuration = makeExternalConfiguration(
            sourceRoots,
            samplesRoots.toList(),
            emptyList(),
            docRootPath = "reference",
            projectPath = "androidx",
            javaDocsPath = "",
            kotlinDocsPath = "kotlin"
        )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin),
        ) { }
    }

    /**
     * Runs dackka on sources from a prebuilt; for verifying that there are no errors.
     *
     * Sources are unzipped from prebuilts/androidx/internal/ (grabbed via gradle dependency)
     *      into `build/explodedSources/$artifactName-$version-sources/`
     * Samples are kept locally (read from `testData/$testName/samples/`), as they are not published
     */
    fun executePrebuilts(
        testName: String,
        artifactNames: List<String>,
        samples: Boolean = false,
    ) {
        val samplesBaseDir = "testData/$testName/samples"

        val configuration = makeExternalConfiguration(
            artifactNames.map { File("build/explodedSources/$it/").absoluteFile },
            if (samples) listOf(samplesBaseDir) else emptyList(),
            docRootPath = "reference",
            projectPath = "androidx",
            javaDocsPath = "",
            kotlinDocsPath = "kotlin"
        )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin),
        ) { }
    }

    /**
     * Reads sources and outputs from a directory in `./testData/`, and validates based on them.
     *
     * Sources are located at testData/$path/source
     * outputs are located at testData/$path/docs
     */
    open fun validateDirectory(
        path: String,
        sampleLocations: List<String> = emptyList(),
        includeFiles: List<String> = emptyList(),
        docRootPath: String = "reference",
        projectPath: String? = null,
        javaDocsDirectory: String? = "",
        kotlinDocsDirectory: String? = "kotlin",
        suffix: String = "source"
    ) {
        val samplesBaseDir = "testData/$path"
        val outputBaseDir = "testData/$path/docs"
        val sourceDir = "testData/$path/$suffix"

        val inferredProjectPath = projectPath
            ?: File(sourceDir).listFiles().orEmpty().singleOrNull { it.isDirectory }?.name
            ?: "dokkatest"

        val configuration = makeInternalConfiguration(
            samplesBaseDir,
            sourceDir,
            sampleLocations,
            includeFiles,
            docRootPath,
            inferredProjectPath,
            javaDocsDirectory,
            kotlinDocsDirectory
        )

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

    /**
     * Runs dackka on sources from a prebuilt and validates against saved docs in `./testData/`.
     *
     * Sources are unzipped from prebuilts/androidx/internal/ (grabbed via gradle dependency)
     *      into `build/explodedSources/$artifactName-$version-sources/`
     * outputs are located at `testData/$testName/docs`
     * Samples are kept locally (read from `testData/$testName/samples/`), as they are not published
     */
    fun validatePrebuilts(
        testName: String,
        artifactNames: List<String>,
        samples: Boolean = false,
        includeFiles: List<String> = emptyList()
    ) {
        val outputBaseDir = "testData/$testName/docs"
        val samplesBaseDir = "testData/$testName/samples"

        val configuration = makeExternalConfiguration(
            artifactNames.map { File("build/explodedSources/$it/").absoluteFile },
            if (samples) listOf(samplesBaseDir) else emptyList(),
            includeFiles = includeFiles.map { File("testData/$testName/source", it).absolutePath },
            docRootPath = "reference",
            projectPath = "androidx",
            javaDocsPath = "",
            kotlinDocsPath = "kotlin"
        )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin),
        ) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                verifyOutput(writerPlugin, outputBaseDir)
            }
        }
    }

    protected fun classpathFromFile(file: String): List<String> =
        File(file).bufferedReader().readLines()

    /** Confirms that the given output writer's output matches the contents of the given directory. */
    protected fun verifyOutput(writerPlugin: TestOutputWriterPlugin, outputPath: String) {
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
