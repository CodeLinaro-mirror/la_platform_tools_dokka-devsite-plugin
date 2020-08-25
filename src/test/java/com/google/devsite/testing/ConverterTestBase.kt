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

import com.google.common.truth.Truth.assertThat
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.DacJavaFilePathProvider
import com.google.devsite.renderer.impl.paths.DacKotlinFilePathProvider
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest

internal abstract class ConverterTestBase(
    private val language: Language = Language.JAVA
) : AbstractCoreTest() {
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

    protected fun assertPath(actual: String, expected: String) {
        when (language) {
            Language.JAVA -> assertThat(actual).isEqualTo("/reference/$expected")
            Language.KOTLIN -> assertThat(actual).isEqualTo("/reference/kotlin/$expected")
        }
    }

    protected fun pathProvider() = when (language) {
        Language.JAVA -> DacJavaFilePathProvider("androidx")
        Language.KOTLIN -> DacKotlinFilePathProvider("androidx")
    }

    protected fun javaOnly(block: () -> Unit) {
        if (language == Language.JAVA) {
            block()
        }
    }

    protected fun kotlinOnly(block: () -> Unit) {
        if (language == Language.KOTLIN) {
            block()
        }
    }
}
