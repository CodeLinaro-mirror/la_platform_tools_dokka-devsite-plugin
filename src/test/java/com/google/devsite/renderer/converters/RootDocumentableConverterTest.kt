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
import com.google.devsite.components.ClassIndex
import com.google.devsite.components.Link
import com.google.devsite.components.PackageIndex
import com.google.devsite.components.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class RootDocumentableConverterTest(
    private val language: Language
) : ConverterTestBase(language) {
    @Test
    fun `Class index creates components with correct page title`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val components = converter.classesPage()

            assertThat(components.data.title).isEqualTo("Class Index")
        }
    }

    @Test
    fun `Class index creates components with correct packages link`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassIndex
            assertPath(classIndex.data.packagesUrl, "androidx/packages.html")
        }
    }

    @Test
    fun `Class index creates components for single class`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassIndex
            assertThat(classIndex.data.alphabetizedClasses).hasSize(1)

            val (letter, summary) = classIndex.data.alphabetizedClasses.entries.single()
            assertThat(letter).isEqualTo('F')
            assertThat(summary.data.items).hasSize(1)

            val clazz = (summary.data.items.single() as TwoPaneSummaryItem).data.title as Link
            assertThat(clazz.data.name).isEqualTo("Foo")
            assertPath(clazz.data.url, "androidx/example/Foo.html")
        }
    }

    @Test
    fun `Class index creates components for nested class`() {
        val source = """
            |class Outer { class Inner }
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassIndex
            val summaries = classIndex.data.alphabetizedClasses
            assertThat(summaries).hasSize(1)

            val (letter, summary) = classIndex.data.alphabetizedClasses.entries.single()
            assertThat(letter).isEqualTo('O')
            assertThat(summary.data.items).hasSize(2)

            val outer = (summary.data.items.first() as TwoPaneSummaryItem).data.title as Link
            assertThat(outer.data.name).isEqualTo("Outer")
            assertPath(outer.data.url, "androidx/example/Outer.html")

            val inner = (summary.data.items.last() as TwoPaneSummaryItem).data.title as Link
            assertThat(inner.data.name).isEqualTo("Outer.Inner")
            assertPath(inner.data.url, "androidx/example/Outer.Inner.html")
        }
    }

    @Test
    fun `Class index creates components for enum`() {
        val source = """
            |enum class Choice { A, B }
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassIndex
            val summaries = classIndex.data.alphabetizedClasses
            assertThat(summaries).hasSize(1)

            val (_, summary) = classIndex.data.alphabetizedClasses.entries.single()

            val enum = (summary.data.items.single() as TwoPaneSummaryItem).data.title as Link
            assertThat(enum.data.name).isEqualTo("Choice")
            assertPath(enum.data.url, "androidx/example/Choice.html")
        }
    }

    @Test
    fun `Class index creates components for multiple classes starting with same letter`() {
        val source = """
            |class Fo
            |class Foo
            |class Fooo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassIndex
            assertThat(classIndex.data.alphabetizedClasses).hasSize(1)

            val (letter, summary) = classIndex.data.alphabetizedClasses.entries.single()
            assertThat(letter).isEqualTo('F')
            assertThat(summary.data.items).hasSize(3)

            val expectedClasses = listOf("Fo", "Foo", "Fooo")
            val classes = summary.data.items.map { (it as TwoPaneSummaryItem).data.title as Link }
            for ((i, clazz) in classes.withIndex()) {
                val expectedName = expectedClasses[i]

                assertThat(clazz.data.name).isEqualTo(expectedName)
                assertPath(clazz.data.url, "androidx/example/$expectedName.html")
            }
        }
    }

    @Test
    fun `Class index creates components for multiple classes starting with different letters`() {
        val source = """
            |class Foo
            |class Bar
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassIndex
            assertThat(classIndex.data.alphabetizedClasses).hasSize(2)

            val (fooLetter, fooSummary) = classIndex.data.alphabetizedClasses.entries.first()
            assertThat(fooLetter).isEqualTo('F')
            assertThat(fooSummary.data.items).hasSize(1)

            val fooClazz = (fooSummary.data.items.single() as TwoPaneSummaryItem).data.title as Link
            assertThat(fooClazz.data.name).isEqualTo("Foo")
            assertPath(fooClazz.data.url, "androidx/example/Foo.html")

            val (barLetter, barSummary) = classIndex.data.alphabetizedClasses.entries.last()
            assertThat(barLetter).isEqualTo('B')
            assertThat(barSummary.data.items).hasSize(1)

            val barClazz = (barSummary.data.items.single() as TwoPaneSummaryItem).data.title as Link
            assertThat(barClazz.data.name).isEqualTo("Bar")
            assertPath(barClazz.data.url, "androidx/example/Bar.html")
        }
    }

    @Test
    fun `Package index creates components with correct page title`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val components = converter.packagesPage()

            assertThat(components.data.title).isEqualTo("Package Index")
        }
    }

    @Test
    fun `Package index creates components with correct classes link`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val components = converter.packagesPage()

            val packageIndex = components.data.content as PackageIndex
            assertPath(packageIndex.data.classesUrl, "androidx/classes.html")
        }
    }

    @Test
    fun `Package index creates components for single package`() {
        val sourceFiles = listOf(
            """
                |/src/main/kotlin/androidx/example/Test.kt
                |package androidx.example
                |
                |class Foo
            """.trimMargin()
        )

        testWithRootPageNode(sourceFiles) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val components = converter.packagesPage()

            val packageIndex = components.data.content as PackageIndex
            val packages = packageIndex.data.packages.data.items
            assertThat(packages).hasSize(1)

            val packageLink = (packages.single() as TwoPaneSummaryItem).data.title as Link
            assertThat(packageLink.data.name).isEqualTo("androidx.example")
            assertPath(packageLink.data.url, "androidx/example/package-summary.html")
        }
    }

    @Test
    fun `Package index creates components for multiple packages`() {
        val sourceFiles = listOf(
            """
                |/src/main/kotlin/androidx/example/A.kt
                |package a
                |
                |class A
            """.trimMargin(),
            """
                |/src/main/kotlin/androidx/example/B.kt
                |package b
                |
                |class B
            """.trimMargin(),
            """
                |/src/main/kotlin/androidx/example/C.kt
                |package c
                |
                |class C
            """.trimMargin()
        )

        testWithRootPageNode(sourceFiles) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val components = converter.packagesPage()

            val packageIndex = components.data.content as PackageIndex
            val packages = packageIndex.data.packages.data.items
            assertThat(packages).hasSize(3)

            val expectedPackages = listOf("a", "b", "c")
            for ((i, packageItem) in packages.withIndex()) {
                val packageLink = (packageItem as TwoPaneSummaryItem).data.title as Link

                assertThat(packageLink.data.name).isEqualTo(expectedPackages[i])
                assertPath(packageLink.data.url, "${expectedPackages[i]}/package-summary.html")
            }
        }
    }

    @Test
    fun `Toc creates components with correct metadata links`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val toc = runBlocking { converter.tocPage() }

            assertPath(toc.data.classesUrl, "androidx/classes.html")
            assertPath(toc.data.packagesUrl, "androidx/packages.html")
        }
    }

    @Test
    fun `Toc creates components with correct package link`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val toc = runBlocking { converter.tocPage() }
            val tocPackage = toc.data.packages.single()

            assertThat(tocPackage.data.name).isEqualTo("androidx.example")
            assertPath(tocPackage.data.packageUrl, "androidx/example/package-summary.html")
        }
    }

    @Test
    fun `Toc creates components with correct class link`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val toc = runBlocking { converter.tocPage() }
            val tocPackage = toc.data.packages.single()
            val clazz = tocPackage.data.classes.single()

            assertThat(clazz.name).isEqualTo("Foo")
            assertPath(clazz.url, "androidx/example/Foo.html")
        }
    }

    @Test
    fun `Toc creates components with correct inner class link`() {
        val source = """
            |class Outer { class Inner }
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(language, root, pathProvider())

            val toc = runBlocking { converter.tocPage() }
            val tocPackage = toc.data.packages.single()
            val inner = tocPackage.data.classes.last()

            assertThat(inner.name).isEqualTo("Outer.Inner")
            assertPath(inner.url, "androidx/example/Outer.Inner.html")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
