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
import java.net.URL
import kotlin.collections.singleOrNull
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaSourceSetImpl
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
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

    fun TestDokkaConfigurationBuilder.singlePlatformSourceSets(
        sources: List<File>,
        samplesLocations: List<String>,
        externalLinks: List<ExternalDocumentationLinkImpl> = emptyList(),
    ) = sourceSets {
        sourceSet {
            sourceRoots = sources.map { it.absolutePath }
            // TODO: find a workaround to using a fixed classpath file b/243842129
            classpath = classpathFromFile("testData/classpath.txt")
            externalDocumentationLinks = externalLinks
            samples = samplesLocations
            documentedVisibilities =
                setOf(DokkaConfiguration.Visibility.PUBLIC, DokkaConfiguration.Visibility.PROTECTED)
        }
    }

    open fun TestDokkaConfigurationBuilder.makeSourcesets(
        sources: List<File>,
        samplesLocations: List<String>,
        externalLinks: List<ExternalDocumentationLinkImpl> = emptyList(),
    ) = singlePlatformSourceSets(sources, samplesLocations, externalLinks)

    /**
     * Creates a function to be used to make source sets in a [TestDokkaConfigurationBuilder], based
     * on the [sources] which should be directories containing the source roots for the project and
     * [samplesLocations] which should be directories containing the samples for the project.
     */
    fun makeSourceSetsCreator(
        sources: List<File>,
        samplesLocations: List<String>,
    ): TestDokkaConfigurationBuilder.() -> Unit {
        sources.forEach { check(it.isDirectory) { "$it does not exist or is not a directory" } }
        val externalLinks =
            mapOf(
                    "coroutines" to "https://kotlinlang.org/api/kotlinx.coroutines",
                    "android" to "https://developer.android.com/reference",
                    "guava" to "https://guava.dev/releases/18.0/api/docs/package-list",
                    "kotlin" to "https://kotlinlang.org/api/core/kotlin-stdlib/",
                )
                .map {
                    ExternalDocumentationLink(
                        url = URL(it.value),
                        // TODO: improve package-list updateability b/243840381
                        packageListUrl =
                            File("testData")
                                .toPath()
                                .resolve("package-lists/${it.key}/package-list")
                                .toUri()
                                .toURL(),
                    )
                }
        return { makeSourcesets(sources, samplesLocations, externalLinks) }
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

    /** For when a test uses source in `./testData/` */
    private fun makeInternalConfiguration(
        samplesBaseDir: String,
        sourceDir: String,
        sampleLocations: List<String> = emptyList(),
        docRootPath: String,
        projectPath: String,
        javaDocsDirectory: String?,
        kotlinDocsDirectory: String?,
        includedHeadTagsPathJava: String?,
        includedHeadTagsPathKotlin: String?,
        useAndroidxBaseSourceLink: Boolean,
        hidingAnnotations: List<String>,
        includeHiddenParentSymbols: Boolean,
    ): DokkaConfigurationImpl {
        val sources = File(sourceDir).absoluteFile
        return makeExternalConfiguration(
            makeSourceSetsCreator(listOf(sources), sampleLocations.map { "$samplesBaseDir/$it" }),
            docRootPath,
            projectPath,
            javaDocsDirectory,
            kotlinDocsDirectory,
            includedHeadTagsPathJava,
            includedHeadTagsPathKotlin,
            useAndroidxBaseSourceLink,
            hidingAnnotations = hidingAnnotations,
            includeHiddenParentSymbols = includeHiddenParentSymbols,
        )
    }

    /** Executes dackka on source from an androidx checkout on the same machine. No validation. */
    fun executionTest(
        testName: String,
        paths: List<String>,
        sampleLocations: List<String> = emptyList(),
    ) {
        val sourceRoots = paths.map { File(it).absoluteFile }
        val configuration =
            makeExternalConfiguration(
                makeSourceSetsCreator(sourceRoots, sampleLocations),
                docRootPath = "reference",
                projectPath = "androidx",
                javaDocsPath = "",
                kotlinDocsPath = "kotlin",
                useAndroidxBaseSourceLink = true,
                validNullabilityAnnotations =
                    defaultValidNullabilityAnnotations +
                        "org.checkerframework.checker.nullness.qual.Nullable",
            )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(configuration, pluginOverrides = listOf(writerPlugin)) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                dump(writerPlugin.writer.contents, File("build/docs/$testName").absolutePath)
            }
        }
    }

    /**
     * Executes dackka on source from an androidx checkout on the same machine. No validation.
     *
     * @param maxFolders limits the source files run against, in case of performance issues
     */
    fun crawlingExecTest(
        testName: String,
        checkoutRoot: String,
        excludedPaths: MutableList<String> = mutableListOf(),
        maxFolders: Int = 999999,
    ) {
        // We never intend to run dackka on these folders
        excludedPaths +=
            listOf(
                "test", // "test" folders don't contain "main"; stop recursion
                "development", // A folder of scripts
                "buildSrc", // Not published or documented
                "frameworks", // basically a recursive symlink to checkout-root
                "annotation-sampled", // not published and its docs confuse the samples system
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
                parent
                    .listFiles { child -> child.isDirectory }
                    ?.forEach { child ->
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

        val configuration =
            makeExternalConfiguration(
                makeSourceSetsCreator(sourceRoots, samplesRoots.toList()),
                docRootPath = "reference",
                projectPath = "androidx",
                javaDocsPath = "",
                kotlinDocsPath = "kotlin",
                useAndroidxBaseSourceLink = true,
                validNullabilityAnnotations =
                    defaultValidNullabilityAnnotations +
                        "org.checkerframework.checker.nullness.qual.Nullable",
            )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(configuration, pluginOverrides = listOf(writerPlugin)) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                dump(writerPlugin.writer.contents, File("build/docs/$testName").absolutePath)
            }
        }
    }

    /**
     * Runs dackka on sources from a prebuilt; for verifying that there are no errors.
     *
     * Sources are unzipped from prebuilts/androidx/internal/ (grabbed via gradle dependency) into
     * `build/explodedSources/$artifactName-$version-sources/` Samples are kept locally (read from
     * `testData/$testName/samples/`), as they are not published
     */
    fun executePrebuilts(testName: String, artifactNames: List<String>, samples: Boolean = false) {
        val samplesBaseDir = "testData/$testName/samples"
        val sourceRoots = artifactNames.map { File("build/explodedSources/$it/").absoluteFile }
        val samplesRoots = if (samples) listOf(samplesBaseDir) else emptyList()

        val configuration =
            makeExternalConfiguration(
                makeSourceSetsCreator(sourceRoots, samplesRoots),
                docRootPath = "reference",
                projectPath = "androidx",
                javaDocsPath = "",
                kotlinDocsPath = "kotlin",
                useAndroidxBaseSourceLink = true,
            )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(configuration, pluginOverrides = listOf(writerPlugin)) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                dump(writerPlugin.writer.contents, File("build/docs/$testName").absolutePath)
            }
        }
    }

    /**
     * To work around a parser issue with `@sample` (see b/427708573), rewrite `@sample` tags to
     * `@author #@sample`.
     *
     * If [inPlace] is true, updates the files in [originalSourceDirPath] directly. Otherwise, the
     * files are first copied to a directory named [name] in a build subdirectory and then updated.
     *
     * Returns the path of the directory containing the updated files.
     */
    private fun rewriteSamplesTags(
        originalSourceDirPath: String,
        name: String,
        inPlace: Boolean,
    ): String {
        val originalSourceDir = File(originalSourceDirPath)

        val (sourceDir, sourceDirPath) =
            if (inPlace) {
                originalSourceDir to originalSourceDirPath
            } else {
                val updatedDirPath = "build/updatedTestSources/$name"
                val updatedDir = File(updatedDirPath)
                updatedDir.deleteRecursively()
                originalSourceDir.copyRecursively(updatedDir)
                updatedDir to updatedDirPath
            }

        for (file in sourceDir.recursivelyListFiles()) {
            if (file.extension == "kt") {
                val originalContents = file.readText()
                // Only replace `@sample` when it is used at the start of a comment line.
                val updatedContents =
                    originalContents.replace(" * @sample ", " * @author #@sample ")
                file.writeText(updatedContents)
            }
        }

        return sourceDirPath
    }

    /**
     * Reads sources and outputs from a directory in `./testData/`, and validates based on them.
     *
     * Sources are located at testData/$path/source outputs are located at testData/$path/docs
     */
    open fun validateDirectory(
        path: String,
        sampleLocations: List<String> = emptyList(),
        docRootPath: String = "reference",
        projectPath: String? = null,
        javaDocsDirectory: String? = "",
        kotlinDocsDirectory: String? = "kotlin",
        includedHeadTagsPathJava: String? = "_shared/_reference-head-tags.html",
        includedHeadTagsPathKotlin: String? = "_shared/_reference-head-tags.html",
        useAndroidxBaseSourceLink: Boolean = false,
        hidingAnnotations: List<String> = listOf("androidx.annotation.RestrictTo"),
        includeHiddenParentSymbols: Boolean = false,
    ) {
        val samplesBaseDir = "testData/$path"
        val outputBaseDir = "testData/$path/docs"
        val loggingDir = "testData/$path/logs"

        val originalSourceDir = "testData/$path/source"
        // If there are samples provided, apply the @sample tag workaround. Don't update the files
        // in place because they are checked in source files.
        val sourceDir =
            if (sampleLocations.isNotEmpty()) {
                rewriteSamplesTags(originalSourceDir, path, inPlace = false)
            } else {
                originalSourceDir
            }

        val inferredProjectPath =
            projectPath
                ?: File(sourceDir).listFiles().orEmpty().singleOrNull { it.isDirectory }?.name
                ?: "dokkatest"

        val configuration =
            makeInternalConfiguration(
                samplesBaseDir,
                sourceDir,
                sampleLocations,
                docRootPath,
                inferredProjectPath,
                javaDocsDirectory,
                kotlinDocsDirectory,
                includedHeadTagsPathJava,
                includedHeadTagsPathKotlin,
                useAndroidxBaseSourceLink,
                hidingAnnotations,
                includeHiddenParentSymbols,
            )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(configuration, pluginOverrides = listOf(writerPlugin)) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                verifyOutput(writerPlugin.writer.contents, outputBaseDir)
                verifyOutput(logFiles(sourceDir), loggingDir)
            }
        }
    }

    /**
     * Runs dackka on sources from a prebuilt and validates against saved docs in `./testData/`.
     *
     * Sources are unzipped from prebuilts/androidx/internal/ (grabbed via gradle dependency) into
     * `build/explodedSources/$artifactName-$version-sources/` outputs are located at
     * `testData/$testName/docs` Samples are kept locally (read from `testData/$testName/samples/`),
     * as they are not published
     *
     * Use useAndroidxBaseSourceLink=true to include AndroidX source links in the generated page.
     * Multiple artifact names and source links don't work well together, links end up using
     * absolute paths.
     */
    fun validatePrebuilts(
        testName: String,
        artifactNames: List<String>,
        samples: Boolean = false,
        useAndroidxBaseSourceLink: Boolean = true,
        versionMetadata: Boolean = false,
    ) {
        val outputBaseDir = "testData/$testName/docs"
        val samplesBaseDir = "testData/$testName/samples"
        val loggingDir = "testData/$testName/logs"

        val versionMetadataBaseDir = "testData/$testName/versionMetadata"
        val versionMetadataFiles =
            if (versionMetadata) {
                File(versionMetadataBaseDir).listFiles()?.map { it.absolutePath }
            } else {
                null
            }

        val explodedSourcesDir = "build/explodedSources"
        val sourceDirPaths =
            if (samples) {
                // If there are samples provided, apply the @sample tag workaround. Update the files
                // in place since they are generated as part of the build.
                artifactNames.map {
                    rewriteSamplesTags("$explodedSourcesDir/$it/", it, inPlace = true)
                }
            } else {
                artifactNames.map { "$explodedSourcesDir/$it/" }
            }
        val sourceDirs = sourceDirPaths.map { File(it).absoluteFile }
        val samplesDirs = if (samples) listOf(samplesBaseDir) else emptyList()
        val configuration =
            makeExternalConfiguration(
                makeSourceSetsCreator(sourceDirs, samplesDirs),
                docRootPath = "reference",
                projectPath = "androidx",
                javaDocsPath = "",
                kotlinDocsPath = "kotlin",
                useAndroidxBaseSourceLink = useAndroidxBaseSourceLink,
                versionMetadataFilesnames = versionMetadataFiles,
            )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(configuration, pluginOverrides = listOf(writerPlugin)) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                verifyOutput(writerPlugin.writer.contents, outputBaseDir)
                verifyOutput(logFiles(explodedSourcesDir), loggingDir)
            }
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
                "//debug.txt" to cleanLogMessages(logger.debugMessages, absoluteSourcePath),
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

fun classpathFromFile(file: String): List<String> = File(file).bufferedReader().readLines()
