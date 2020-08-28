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
import com.google.devsite.components.DevsitePage
import com.google.devsite.components.FunctionSummary
import com.google.devsite.components.PackageSummary
import com.google.devsite.components.SummaryList
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.content
import com.google.devsite.renderer.converters.testing.functionSummary
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
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
        val page = listOf(
            """
                |/src/main/kotlin/androidx/example/A.kt
                |package hello.i.am.a.packagez
                |
                |class A
            """.trimMargin()
        ).render().page()

        assertThat(page.data.title).isEqualTo("hello.i.am.a.packagez")
    }

    @Test
    fun `Package summary creates components with correct path`() {
        val page = listOf(
            """
                |/src/main/kotlin/androidx/example/A.kt
                |package hello.i.am.a.packagez
                |
                |class A
            """.trimMargin()
        ).render().page()

        assertThat(page.data.path).isEqualTo("hello/i/am/a/packagez/package-summary.html")
    }

    @Test
    fun `Package summary creates components with correct book path`() {
        val page = """
            |class Foo
        """.render().page()

        assertPath(page.data.bookPath, "androidx/_book.yaml")
    }

    @Test
    fun `Package summary creates components for interfaces`() {
        val page = """
            |interface ImAnInterface
        """.render().page()

        val summary = page.content<PackageSummary>()
        val interfacz = summary.data.interfaces.item()

        assertThat(interfacz.link().name).isEqualTo("ImAnInterface")
        assertPath(interfacz.link().url, "androidx/example/ImAnInterface.html")
    }

    @Test
    fun `Package summary creates components for classes`() {
        val page = """
            |class ImAClass
        """.render().page()

        val summary = page.content<PackageSummary>()
        val clazz = summary.data.classes.item()

        assertThat(clazz.link().name).isEqualTo("ImAClass")
        assertPath(clazz.link().url, "androidx/example/ImAClass.html")
    }

    @Test
    fun `Package summary creates components for nested classes`() {
        val page = """
            |class Outer { class Inner }
        """.render().page()

        val summary = page.content<PackageSummary>()
        val inner = summary.data.classes.items(2).last()

        assertThat(inner.link().name).isEqualTo("Outer.Inner")
        assertPath(inner.link().url, "androidx/example/Outer.Inner.html")
    }

    @Test
    fun `Package summary creates components for enums`() {
        val page = """
            |enum class ImAnEnum
        """.render().page()

        val summary = page.content<PackageSummary>()
        val enum = summary.data.enums.item()

        assertThat(enum.link().name).isEqualTo("ImAnEnum")
        assertPath(enum.link().url, "androidx/example/ImAnEnum.html")
    }

    @Test
    fun `Package summary creates components for exceptions`() {
        val page = """
            |class ImAnException : RuntimeException()
        """.render().page()

        val summary = page.content<PackageSummary>()
        val exception = summary.data.exceptions.item()

        assertThat(exception.link().name).isEqualTo("ImAnException")
        assertPath(exception.link().url, "androidx/example/ImAnException.html")
    }

    @Test
    fun `Package summary creates components for annotations`() {
        val page = """
            |annotation class ImAnAnnotation
        """.render().page()

        val summary = page.content<PackageSummary>()
        val annotation = summary.data.annotations.item()

        assertThat(annotation.link().name).isEqualTo("ImAnAnnotation")
        assertPath(annotation.link().url, "androidx/example/ImAnAnnotation.html")
    }

    @Test
    fun `Package summary creates components for top-level functions`() {
        val page = """
            |fun foo()
        """.render().page()

        val summary = page.content<PackageSummary>()
        val topLevels = summary.data.topLevelFunctionsSummary
        val extensions = summary.data.extensionFunctionsSummary

        assertThat(topLevels.items()).hasSize(1)
        assertThat(extensions.items()).isEmpty()
        assertThat(topLevels.function().name()).isEqualTo("foo")
    }

    @Test
    fun `Package summary creates components for extension functions`() {
        val page = """
            |fun String.foo()
        """.render().page()

        val summary = page.content<PackageSummary>()
        val topLevels = summary.data.topLevelFunctionsSummary
        val extensions = summary.data.extensionFunctionsSummary

        assertThat(topLevels.items()).isEmpty()
        assertThat(extensions.items()).hasSize(1)
        assertThat(extensions.function().name()).isEqualTo("foo")
    }

    private fun RootPageNode.page(): DevsitePage {
        val packagePage = children.filterIsInstance<PackagePageNode>().single()
        val converter = PackageDocumentableConverter(language, packagePage, pathProvider())
        return runBlocking { converter.summaryPage() }
    }

    private fun SummaryList.function(): FunctionSummary = item().functionSummary()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
