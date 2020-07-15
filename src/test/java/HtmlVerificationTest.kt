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
import org.junit.Test
import utils.TestOutputWriterPlugin
import java.io.File

/**
 * Full integration test of source to html generation.
 *
 * Html output results can be found in testData/HtmlVerificationTest
 */
class HtmlVerificationTest : AbstractCoreTest() {
    @Test
    fun simpleClass() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }
        val source = """
            |/src/main/kotlin/test/Test.kt
            |package example
            |/**
            | * This is a comment
            | */
            |class Test()
        """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
                source,
                configuration,
                pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.contents.filter {
                    it.key.endsWith(".html")
                }.forEach { fileName, fileContent ->
                    val outputDirectory = File("./testData/HtmlVerificationTest/simpleClass/")
                    val expectedOutput = File(outputDirectory, fileName)
                    assertEquals(expectedOutput.readText(), fileContent)
                }
            }
        }
    }
}