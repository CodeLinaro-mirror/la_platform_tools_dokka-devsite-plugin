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

package com.google.devsite.integration

import com.google.devsite.testing.TestOutputWriterPlugin
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

class MathjaxPluginTest : AbstractCoreTest() {

    @Ignore("Mathjax tags aren't being rendered as DevsiteRenderer is still being built out")
    @Test
    fun `Basic test`() {
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
            |
            |/**
            | * @usesMathJax
            | *
            | * \(\alpha_{out} = \alpha_{dst}\)
            | * \(C_{out} = C_{dst}\)
            | */
            |class Bar()
        """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val mathjaxElements = Jsoup
                    .parse(writerPlugin.writer.contents.getValue("reference/java/example/TestKt.html"))
                    .body()
                    .select("devsite-mathjax")
                assertEquals(1, mathjaxElements.size)
            }
        }
    }

    @Ignore("TMP while we get the new architecture set up")
    @Test
    fun noMathjaxTest() {
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
            |
            |/**
            | * Just a regular kdoc
            | */
            |class Bar
        """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val mathjaxElements = Jsoup
                    .parse(writerPlugin.writer.contents.getValue("reference/java/example/Bar.html"))
                    .body()
                    .select("devsite-mathjax")
                assertEquals(0, mathjaxElements.size)
            }
        }
    }
}
