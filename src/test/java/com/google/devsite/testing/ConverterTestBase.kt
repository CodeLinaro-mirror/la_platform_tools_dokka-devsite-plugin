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

import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest

abstract class ConverterTestBase : AbstractCoreTest() {
    protected fun testWithRootPageNode(sourceFiles: List<String>, test: (RootPageNode) -> Unit) {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = sourceFiles.map { it.lineSequence().first().removePrefix("/") }
                }
            }
        }

        System.setProperty("tenant", "androidx")

        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            sourceFiles.joinToString("\n\n"),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { node, _ ->
                test(node)
            }
        }
    }

    protected fun testWithRootPageNode(sourceCode: String, test: (RootPageNode) -> Unit) {
        val source = """
            |/src/main/kotlin/androidx/example/Test.kt
            |package androidx.example
            |
            |$sourceCode
        """.trimMargin()
        testWithRootPageNode(listOf(source), test)
    }
}
