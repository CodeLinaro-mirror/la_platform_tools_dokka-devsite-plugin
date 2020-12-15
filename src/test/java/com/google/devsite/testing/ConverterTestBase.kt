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
import com.google.devsite.DevsitePlugin
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.isFromBaseClass
import com.google.devsite.renderer.impl.paths.DacJavaFilePathProvider
import com.google.devsite.renderer.impl.paths.DacKotlinFilePathProvider
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal abstract class ConverterTestBase(
    private val language: Language = Language.JAVA
) : AbstractCoreTest() {
    protected fun List<String>.render(): DModule = testWithRootPageNode(this)

    protected fun String.render(java: Boolean = false): DModule = if (java) {
        testJavaWithRootPageNode(trimMargin())
    } else {
        testWithRootPageNode(trimMargin())
    }

    protected fun DModule.classlike() = packages.single().classlikes
        .firstOrNull { it.name != "Nullable" }

    protected fun DModule.function(name: String? = null) =
        packages.single().functions.singleOrNull { it.name == name && !it.dri.isFromBaseClass() }
            ?: classlike()?.functions?.singleOrNull { it.name == name && !it.dri.isFromBaseClass() }
            ?: packages.single().functions.singleOrNull { !it.dri.isFromBaseClass() }
            ?: classlike()?.functions?.singleOrNull { !it.dri.isFromBaseClass() }

    protected fun DModule.functions() =
        packages.single().functions.nullIfEmpty()
            ?: classlike()?.functions

    protected fun DModule.property(name: String? = null) =
        packages.single().properties.singleOrNull { it.name == name }
            ?: classlike()?.properties?.singleOrNull { it.name == name }
            ?: packages.single().properties.singleOrNull()
            ?: classlike()?.properties?.singleOrNull()

    protected fun DModule.properties() =
        packages.single().properties.nullIfEmpty()
            ?: classlike()?.properties

    private fun <E> List<E>.nullIfEmpty() = if (this.isNotEmpty()) this else null

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

    private fun testWithRootPageNode(sourceFiles: List<String>): DModule = runBlocking {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main")
                    classpath = listOfNotNull(jvmStdlibPath, commonStdlibPath)
                }
            }
        }

        suspendCoroutine { cont ->
            testInline(
                sourceFiles.joinToString("\n\n"),
                configuration,
                pluginOverrides = listOf(NoopPlugin)
            ) {
                renderingStage = { node, _ ->
                    val module = (node as ModulePageNode).documentable as DModule
                    cont.resume(module)
                }
            }
        }
    }

    private fun testWithRootPageNode(sourceCode: String): DModule {
        val source = """
            |/src/main/kotlin/androidx/example/Test.kt
            |package androidx.example
            |
            |$sourceCode
        """.trimMargin()
        return testWithRootPageNode(listOf(source))
    }

    private fun testJavaWithRootPageNode(sourceCode: String): DModule {
        val source = """
            |/src/main/java/androidx/example/Test.java
            |package androidx.example;
            |annotation class Nullable
            |public class Test {
            |$sourceCode
            |}
        """.trimMargin()
        return testWithRootPageNode(listOf(source))
    }

    object NoopPlugin : DokkaPlugin() {
        private val devsite by lazy { plugin<DevsitePlugin>() }

        val renderer by extending {
            CoreExtensions.renderer providing { NoopRenderer } override devsite.renderer
        }

        private object NoopRenderer : Renderer {
            override fun render(root: RootPageNode) = Unit
        }
    }
}
