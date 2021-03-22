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
import com.google.devsite.renderer.impl.paths.ExternalDokkaLocationProvider
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.renderers.Renderer

import java.io.File
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal abstract class ConverterTestBase(
    private val language: Language = Language.JAVA
) : BaseAbstractTest() {
    protected fun List<String>.render(): DModule = testWithRootPageNode(this)

    protected fun String.render(
        java: Boolean = false,
        fileUseAnnotation: String = ""
    ): DModule = if (java) {
        testJavaWithRootPageNode(trimMargin())
    } else {
        testWithRootPageNode(trimMargin(), fileUseAnnotation)
    }

    protected fun DModule.classlike() = packages.single().classlikes
        .firstOrNull { it.name !in listOf("Nullable", "NonNull") }

    protected fun DModule.explicitClasslike(name: String? = null) =
        packages.single().classlikes.singleOrNull { it.name == name }
            ?: classlike()?.classlikes?.singleOrNull { it.name == name }
            ?: classlike()

    protected fun DModule.function(name: String? = null) =
        packages.single().functions.singleOrNull { it.name == name && !it.dri.isFromBaseClass() }
            ?: classlike()?.functions?.singleOrNull { it.name == name && !it.dri.isFromBaseClass() }
            ?: packages.single().functions.singleOrNull { !it.dri.isFromBaseClass() }
            ?: classlike()?.functions?.singleOrNull { !it.dri.isFromBaseClass() }

    protected fun DModule.constructor() = constructors().single()

    protected fun DModule.constructors() = (classlike() as DClass).constructors

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

    protected fun pathProvider(
        externalLocationProvider: ExternalDokkaLocationProvider? = null
    ) = when (language) {
        Language.JAVA -> DacJavaFilePathProvider("androidx", externalLocationProvider)
        Language.KOTLIN -> DacKotlinFilePathProvider("androidx", externalLocationProvider)
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
        val externalLinks = mapOf(
            "coroutines" to "https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core",
            "android" to "https://developer.android.com/reference",
            "guava" to "https://guava.dev/releases/18.0/api/docs/package-list",
            "kotlin" to "https://kotlinlang.org/api/latest/jvm/stdlib/"
        ).map {
            ExternalDocumentationLink(
                url = URL(it.value),
                packageListUrl = File("testData").toPath()
                    .resolve("package-lists/${it.key}/package-list").toUri().toURL()
            )
        }
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main")
                    classpath = listOfNotNull(jvmStdlibPath, commonStdlibPath)
                    externalDocumentationLinks = externalLinks
                }
            }
            offlineMode = true
        }
        suspendCoroutine { cont ->
            testInline(
                sourceFiles.joinToString("\n\n"),
                configuration,
                pluginOverrides = listOf(NoopPlugin)
            ) {
                renderingStage = { node: RootPageNode, _: DokkaContext ->
                    val module = (node as ModulePageNode).documentable as DModule
                    cont.resume(module)
                }
            }
        }
    }

    private fun testWithRootPageNode(sourceCode: String, fileUseAnnotation: String): DModule {
        val source = """
            |/src/main/kotlin/androidx/example/Test.kt
            |$fileUseAnnotation
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
            |@Target({METHOD, PARAMETER, FIELD, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE})
            |public @interface Nullable {
            |}
            |@Target({METHOD, PARAMETER, FIELD, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE})
            |public @interface NonNull {
            |}
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
