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

package com.google.devsite.renderer.converters

import com.google.common.truth.Truth.assertThat
import com.google.devsite.components.FunctionSummary
import com.google.devsite.components.Link
import com.google.devsite.components.PackageSummary
import com.google.devsite.components.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PackageDocumentableConverterTest(
    private val language: Language
) : ConverterTestBase(language) {
    @Test
    fun `Package summary creates components with correct page title`() {
        val sourceFiles = listOf(
            """
                |/src/main/kotlin/androidx/example/A.kt
                |package hello.i.am.a.packagez
                |
                |class A
            """.trimMargin()
        )

        testWithRootPageNode(sourceFiles) { root ->
            val converter =
                PackageDocumentableConverter(language, root.packagePage(), pathProvider())

            val components = runBlocking { converter.summaryPage() }

            assertThat(components.data.title).isEqualTo("hello.i.am.a.packagez")
        }
    }

    @Test
    fun `Package summary creates components for interfaces`() {
        val source = """
            |interface ImAnInterface
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter =
                PackageDocumentableConverter(language, root.packagePage(), pathProvider())

            val components = runBlocking { converter.summaryPage() }
            val packageComponent = components.data.content as PackageSummary
            val interfaceComponent =
                packageComponent.data.interfaces.data.items.single() as TwoPaneSummaryItem
            val titleComponent = interfaceComponent.data.title as Link

            assertThat(titleComponent.data.name).isEqualTo("ImAnInterface")
            assertPath(titleComponent.data.url, "androidx/example/ImAnInterface.html")
        }
    }

    @Test
    fun `Package summary creates components for classes`() {
        val source = """
            |class ImAClass
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter =
                PackageDocumentableConverter(language, root.packagePage(), pathProvider())

            val components = runBlocking { converter.summaryPage() }
            val packageComponent = components.data.content as PackageSummary
            val classComponent =
                packageComponent.data.classes.data.items.single() as TwoPaneSummaryItem
            val titleComponent = classComponent.data.title as Link

            assertThat(titleComponent.data.name).isEqualTo("ImAClass")
            assertPath(titleComponent.data.url, "androidx/example/ImAClass.html")
        }
    }

    @Test
    fun `Package summary creates components for enums`() {
        val source = """
            |enum class ImAnEnum
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter =
                PackageDocumentableConverter(language, root.packagePage(), pathProvider())

            val components = runBlocking { converter.summaryPage() }
            val packageComponent = components.data.content as PackageSummary
            val enumComponent =
                packageComponent.data.enums.data.items.single() as TwoPaneSummaryItem
            val titleComponent = enumComponent.data.title as Link

            assertThat(titleComponent.data.name).isEqualTo("ImAnEnum")
            assertPath(titleComponent.data.url, "androidx/example/ImAnEnum.html")
        }
    }

    @Test
    fun `Package summary creates components for exceptions`() {
        val source = """
            |class ImAnException : RuntimeException()
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter =
                PackageDocumentableConverter(language, root.packagePage(), pathProvider())

            val components = runBlocking { converter.summaryPage() }
            val packageComponent = components.data.content as PackageSummary
            val exceptionComponent =
                packageComponent.data.exceptions.data.items.single() as TwoPaneSummaryItem
            val titleComponent = exceptionComponent.data.title as Link

            assertThat(titleComponent.data.name).isEqualTo("ImAnException")
            assertPath(titleComponent.data.url, "androidx/example/ImAnException.html")
        }
    }

    @Test
    fun `Package summary creates components for annotations`() {
        val source = """
            |annotation class ImAnAnnotation
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter =
                PackageDocumentableConverter(language, root.packagePage(), pathProvider())

            val components = runBlocking { converter.summaryPage() }
            val packageComponent = components.data.content as PackageSummary
            val annotationComponent =
                packageComponent.data.annotations.data.items.single() as TwoPaneSummaryItem
            val titleComponent = annotationComponent.data.title as Link

            assertThat(titleComponent.data.name).isEqualTo("ImAnAnnotation")
            assertPath(titleComponent.data.url, "androidx/example/ImAnAnnotation.html")
        }
    }

    @Test
    fun `Package summary creates components for top-level functions`() {
        val source = """
            |fun foo()
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter =
                PackageDocumentableConverter(language, root.packagePage(), pathProvider())

            val components = runBlocking { converter.summaryPage() }
            val packageComponent = components.data.content as PackageSummary
            assertThat(packageComponent.data.topLevelFunctionsSummary.data.items).hasSize(1)
            assertThat(packageComponent.data.extensionFunctionsSummary.data.items).isEmpty()

            val functions = packageComponent.data.topLevelFunctionsSummary
            val functionComponent = functions.data.items.single() as FunctionSummary
            val titleComponent = functionComponent.data.signature.data.name

            assertThat(titleComponent.data.name).isEqualTo("foo")
        }
    }

    @Test
    fun `Package summary creates components for extension functions`() {
        val source = """
            |fun String.foo()
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter =
                PackageDocumentableConverter(language, root.packagePage(), pathProvider())

            val components = runBlocking { converter.summaryPage() }
            val packageComponent = components.data.content as PackageSummary
            assertThat(packageComponent.data.topLevelFunctionsSummary.data.items).isEmpty()
            assertThat(packageComponent.data.extensionFunctionsSummary.data.items).hasSize(1)

            val functions = packageComponent.data.extensionFunctionsSummary
            val functionComponent = functions.data.items.single() as FunctionSummary
            val titleComponent = functionComponent.data.signature.data.name

            assertThat(titleComponent.data.name).isEqualTo("foo")
        }
    }

    private fun RootPageNode.packagePage() = children.filterIsInstance<PackagePageNode>().single()

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
