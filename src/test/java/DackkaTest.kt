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
     * reads sources and outputs from a directory, and validates based on them
     *
     * Sources are located at testData/$path/source
     * outputs are located at testData/$path/docs
     */
    fun verifyDirectory(path: String) {
        val baseDir = "testData/" + path
        val sourceDir = baseDir + "/source"

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf(File(sourceDir).absolutePath)
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()
        testFromData(
                configuration,
                pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                verifyOutput(writerPlugin, baseDir + "/docs")
            }
        }
    }

    // validates the given source against the outputs in the given directory
    fun verifyInline(testName: String, source: String) {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
                source,
                configuration,
                pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                verifyOutput(writerPlugin, "testData/$testName")
            }
        }
    }

    // confirms that the given output writer's output matches the contents of the given directory
    fun verifyOutput(writerPlugin: TestOutputWriterPlugin, outputPath: String) {
        val outputDirectory = File(outputPath).absolutePath
        val generatedFiles = writerPlugin.writer.contents.filter {
            it.key.endsWith(".html")
        }
        val dumpedFile = File("build/docs/" + outputPath)
        dump(writerPlugin, dumpedFile.absolutePath)
        generatedFiles.forEach { (fileName, generatedContent) ->
            val expectedFile = File(outputDirectory, fileName)
            val expectedText = if (expectedFile.exists()) {
                expectedFile.readText()
            } else {
                ""
            }
            if (expectedText != generatedContent) {
                val message = "Unexpected output in " + fileName + ".\n" +
                    "To update the expected output to match the current output, run this " +
                    "command:\n\n" +
                    "  rm -rf $outputDirectory && " +
                    "cp -r ${dumpedFile.absolutePath} $outputDirectory\n\n" +
                    "Difference in outputs:\n"
                assertEquals(message, expectedText, generatedContent)
            }
        }
    }

    // exports the output of writerPlugin to outputPath
    fun dump(writerPlugin: TestOutputWriterPlugin, outputPath: String) {
        val outputDirectory = File(outputPath)
        outputDirectory.deleteRecursively()
        val generatedFiles = writerPlugin.writer.contents.filter {
            it.key.endsWith(".html")
        }.forEach { (fileName, fileContent) ->
            val expectedFile = File(outputDirectory, fileName)
            expectedFile.parentFile.mkdirs()
            expectedFile.writeText(fileContent)
        }
    }
}
