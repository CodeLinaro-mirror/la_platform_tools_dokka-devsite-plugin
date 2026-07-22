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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.truth.Truth.assertWithMessage
import com.google.devsite.DevsiteConfiguration
import com.google.devsite.defaultValidNullabilityAnnotations
import com.google.devsite.renderer.converters.isRunningInDackkasTests
import java.io.File
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaSourceSetImpl
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.toCompactJsonString
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.junit.Before
import testApi.testRunner.TestDokkaConfigurationBuilder

/**
 * Full integration tests of source to html generation.
 *
 * Html output results can be found in testData/
 */
abstract class IntegrationTestBase :
    BaseAbstractTest(TestLogger(DokkaConsoleLogger(LoggingLevel.WARN))) {
    @Before
    fun setUp() {
        isRunningInDackkasTests = true
    }

    /** For when a test uses source outside of `./testData/` */
    open fun makeExternalConfiguration(
        makeSourceSets: TestDokkaConfigurationBuilder.() -> Unit,
        docRootPath: String,
        projectPath: String,
        javaDocsPath: String?,
        kotlinDocsPath: String?,
        includedHeadTagsPathJava: String? = "_shared/_reference-head-tags.html",
        includedHeadTagsPathKotlin: String? = "_shared/_reference-head-tags.html",
        useAndroidxBaseSourceLink: Boolean = false,
        versionMetadataFilesnames: List<String>? = null,
        hidingAnnotations: List<String> = listOf("androidx.annotation.RestrictTo"),
        includeHiddenParentSymbols: Boolean = false,
        validNullabilityAnnotations: List<String> = defaultValidNullabilityAnnotations,
    ): DokkaConfigurationImpl {
        val baseSourceLink =
            if (useAndroidxBaseSourceLink) {
                "https://cs.android.com/search?q=file:%s+class:%s" +
                    "&ss=androidx/platform/frameworks/support"
            } else {
                null
            }

        return dokkaConfiguration {
            makeSourceSets()
            offlineMode = true
            pluginsConfigurations =
                mutableListOf(
                    PluginConfigurationImpl(
                        fqPluginName = "com.google.devsite.DevsitePlugin",
                        serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
                        values =
                            DevsiteConfiguration(
                                    docRootPath = docRootPath,
                                    projectPath = projectPath,
                                    excludedPackages = null,
                                    excludedPackagesForJava = null,
                                    excludedPackagesForKotlin = null,
                                    libraryMetadataFilename = null,
                                    versionMetadataFilenames = versionMetadataFilesnames,
                                    javaDocsPath = javaDocsPath,
                                    kotlinDocsPath = kotlinDocsPath,
                                    includedHeadTagsPathJava = includedHeadTagsPathJava,
                                    includedHeadTagsPathKotlin = includedHeadTagsPathKotlin,
                                    packagePrefixToRemoveInToc = null,
                                    baseSourceLink = baseSourceLink,
                                    baseFunctionSourceLink = null,
                                    basePropertySourceLink = null,
                                    // These lists are based on the AndroidX excluded annotations
                                    annotationsNotToDisplay =
                                        listOf(
                                            "androidx.compose.runtime.Stable",
                                            "androidx.compose.runtime.Immutable",
                                            "androidx.compose.runtime.ReadOnlyComposable",
                                            "androidx.annotation.OptIn",
                                            "kotlin.OptIn",
                                            "androidx.annotation.CheckResult",
                                            "kotlin.ParameterName",
                                            "kotlin.js.JsName",
                                            "java.lang.Override",
                                            "kotlin.ExtensionFunctionType",
                                        ),
                                    annotationsNotToDisplayJava = null,
                                    annotationsNotToDisplayKotlin = null,
                                    hidingAnnotations = hidingAnnotations,
                                    includeHiddenParentSymbols = includeHiddenParentSymbols,
                                    validNullabilityAnnotations = validNullabilityAnnotations,
                                )
                                .toCompactJsonString(),
                    )
                )
        }
    }

    /**
     * Runs dackka on a test project located in `testData/[path]` which uses the `DackkaTestPlugin`
     * to write a source set JSON file.
     *
     * Verifies that the docs output matches the files in the `docs` subdirectory of the test
     * project and that the logged lines match the files in `logs` subdirectory.
     */
    fun validate(
        path: String,
        docRootPath: String = "reference",
        projectPath: String = "dokkatest",
        javaDocsDirectory: String? = "",
        kotlinDocsDirectory: String? = "kotlin",
        includedHeadTagsPathJava: String? = "_shared/_reference-head-tags.html",
        includedHeadTagsPathKotlin: String? = "_shared/_reference-head-tags.html",
        hidingAnnotations: List<String> = listOf("androidx.annotation.RestrictTo"),
        includeHiddenParentSymbols: Boolean = false,
        versionMetadata: Boolean = false,
        useAndroidxBaseSourceLink: Boolean = false,
    ) {
        val outputBaseDir = "testData/$path/docs"
        val loggingDir = "testData/$path/logs"

        // The WriteSourceSets task in buildSrc creates this file for DackkaTestPlugin projects.
        val sourceSets = File("testData/$path/build/sourceSets.json")

        val versionMetadataFiles =
            if (versionMetadata) {
                val versionMetadataBaseDir = "testData/$path/versionMetadata"
                File(versionMetadataBaseDir).listFiles()?.map { it.absolutePath }
            } else {
                null
            }

        val configuration =
            makeExternalConfiguration(
                makeSourceSets = {
                    // Read source sets as JSON from the project file.
                    sourceSets {
                        addAll(
                            jacksonObjectMapper()
                                .readValue<List<DokkaSourceSetImpl>>(sourceSets)
                                .map { lazyOf(it) }
                        )
                    }
                },
                docRootPath = docRootPath,
                projectPath = projectPath,
                javaDocsPath = javaDocsDirectory,
                kotlinDocsPath = kotlinDocsDirectory,
                includedHeadTagsPathJava = includedHeadTagsPathJava,
                includedHeadTagsPathKotlin = includedHeadTagsPathKotlin,
                useAndroidxBaseSourceLink = useAndroidxBaseSourceLink,
                versionMetadataFilesnames = versionMetadataFiles,
                hidingAnnotations = hidingAnnotations,
                includeHiddenParentSymbols = includeHiddenParentSymbols,
            )

        // Find the common source root for the project. If there are multiple source sets (or a
        // single source set with multiple source roots), the longest common prefix is used.
        val sourceRootPath =
            configuration.sourceSets
                .flatMap { it.sourceRoots }
                .map { it.absolutePath }
                .reduce { currPrefix, nextPath -> currPrefix.commonPrefixWith(nextPath) }

        val writerPlugin = TestOutputWriterPlugin()
        testFromData(configuration, pluginOverrides = listOf(writerPlugin)) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                verifyOutput(writerPlugin.writer.contents, outputBaseDir)
                verifyOutput(logFiles(sourceRootPath), loggingDir)
            }
        }
    }

    /** Creates a map from a filename for a logging level to the logged messages of that level. */
    private fun logFiles(sourceDir: String): Map<String, String> {
        val absoluteSourcePath = File(sourceDir).absolutePath
        return mapOf(
                // Start with "//" to match the filepaths from the writer plugin contents
                "//warnings.txt" to cleanLogMessages(logger.warnMessages, absoluteSourcePath),
                "//error.txt" to cleanLogMessages(logger.errorMessages, absoluteSourcePath),
            )
            .filter { it.value.isNotEmpty() }
    }

    private fun cleanLogMessages(messages: List<String>, sourceDir: String): String {
        return messages.map { it.replace(sourceDir, "\$SRC_DIR") }.sorted().joinToString("\n")
    }

    /** Confirms that the given file to output map matches the contents of the given directory. */
    private fun verifyOutput(generatedFiles: Map<String, String>, outputPath: String) {
        val outputDirectory = File(outputPath).absolutePath

        val dumpedFile = File("build/docs/$outputPath")
        dump(generatedFiles, dumpedFile.absolutePath)

        for ((fileName, generatedContent) in generatedFiles) {
            val expectedFile = File(outputDirectory, fileName)
            val expectedText =
                if (expectedFile.exists()) {
                    expectedFile.readText()
                } else {
                    ""
                }

            if (expectedText != generatedContent) {
                val message =
                    """
                    |Unexpected output in $fileName.
                    |To update the expected output to match the current output, run this command:
                    |
                    |  rm -rf $outputDirectory && cp -r ${dumpedFile.absolutePath} $outputDirectory
                    |
                    |Difference in outputs:
                """
                        .trimMargin()
                assertWithMessage(message).that(generatedContent).isEqualTo(expectedText)
            }
        }
        val expectedFileList = File(outputPath).recursivelyListFiles()
        // This same "fix" happens automatically when the file is written, so it's needed to match
        val fixedGeneratedPaths = generatedFiles.keys.map { it.replace("//", "/") }
        for (eFile in expectedFileList) {
            assertWithMessage("File ${eFile.path} was expected but not generated!")
                .that(eFile.path.removePrefix(outputPath) in fixedGeneratedPaths)
                .isTrue()
        }
    }

    private fun File.recursivelyListFiles(): List<File> =
        (this.listFiles { it: File -> !it.isDirectory }?.asList() ?: emptyList()) +
            (this.listFiles { it: File -> it.isDirectory }
                ?.flatMap { it: File -> it.recursivelyListFiles() } ?: emptyList())

    /** Exports the file to output map to outputPath. */
    private fun dump(generatedFiles: Map<String, String>, outputPath: String) {
        val outputDirectory = File(outputPath)
        outputDirectory.deleteRecursively()

        for ((fileName, fileContent) in generatedFiles) {
            val expectedFile = File(outputDirectory, fileName)
            expectedFile.parentFile.mkdirs()
            expectedFile.writeText(fileContent)
        }
    }
}
