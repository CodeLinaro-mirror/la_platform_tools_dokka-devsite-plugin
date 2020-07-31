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

import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.Assert.assertEquals
import utils.TestOutputWriterPlugin
import java.io.File

/**
 * Full integration tests of source to html generation.
 *
 * Html output results can be found in testData/
 */
open class DackkaTest : AbstractCoreTest() {
    /**
     * Reads sources and outputs from a directory, and validates based on them.
     *
     * Sources are located at testData/$path/source
     * outputs are located at testData/$path/docs
     */
    fun verifyDirectory(path: String) {
        val baseDir = "testData/$path"
        val sourceDir = "$baseDir/source"

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    val sources = File(sourceDir).absoluteFile
                    check(sources.isDirectory) { "$sources does not exist or is not a directory" }
                    sourceRoots = listOf(sources.absolutePath)
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()
        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                verifyOutput(writerPlugin, "$baseDir/docs")
            }
        }
    }

    /** Confirms that the given output writer's output matches the contents of the given directory. */
    private fun verifyOutput(writerPlugin: TestOutputWriterPlugin, outputPath: String) {
        val outputDirectory = File(outputPath).absolutePath
        val generatedFiles = writerPlugin.writer.contents.filter {
            it.key.endsWith(".html")
        }

        val dumpedFile = File("build/docs/$outputPath")
        dump(writerPlugin, dumpedFile.absolutePath)

        for ((fileName, generatedContent) in generatedFiles) {
            val expectedFile = File(outputDirectory, fileName)
            val expectedText = if (expectedFile.exists()) {
                expectedFile.readText()
            } else {
                ""
            }

            if (expectedText.trim() != generatedContent.trim()) {
                val message = """
                    |Unexpected output in $fileName.
                    |To update the expected output to match the current output, run this command:
                    |
                    |  rm -rf $outputDirectory && cp -r ${dumpedFile.absolutePath} $outputDirectory
                    |
                    |Difference in outputs:
                """.trimMargin()
                assertEquals(message, expectedText, generatedContent)
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
