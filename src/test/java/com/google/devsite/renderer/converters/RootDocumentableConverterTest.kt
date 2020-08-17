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
import com.google.devsite.renderer.impl.paths.DacJavaFilePathProvider
import com.google.devsite.renderer.impl.paths.DacKotlinFilePathProvider
import com.google.devsite.testing.ConverterTestBase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RootDocumentableConverterTest(
    private val language: Language
) : ConverterTestBase() {
    @Test
    fun `Class index creates components with correct page title`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(root, pathProvider())

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
            val converter = RootDocumentableConverter(root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassIndex
            when (language) {
                Language.JAVA -> assertThat(classIndex.data.packagesUrl)
                    .isEqualTo("/reference/androidx/packages.html")
                Language.KOTLIN -> assertThat(classIndex.data.packagesUrl)
                    .isEqualTo("/reference/kotlin/androidx/packages.html")
            }
        }
    }

    @Test
    fun `Class index creates components for single class`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassIndex
            assertThat(classIndex.data.alphabetizedClasses).hasSize(1)

            val (letter, summary) = classIndex.data.alphabetizedClasses.entries.single()
            assertThat(letter).isEqualTo('F')
            assertThat(summary.data.items).hasSize(1)

            val clazz = (summary.data.items.single() as TwoPaneSummaryItem).data.title as Link
            assertThat(clazz.data.name).isEqualTo("Foo")
            when (language) {
                Language.JAVA -> assertThat(clazz.data.url)
                    .isEqualTo("/reference/androidx/example/Foo.html")
                Language.KOTLIN -> assertThat(clazz.data.url)
                    .isEqualTo("/reference/kotlin/androidx/example/Foo.html")
            }
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
            val converter = RootDocumentableConverter(root, pathProvider())

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
                when (language) {
                    Language.JAVA -> assertThat(clazz.data.url)
                        .isEqualTo("/reference/androidx/example/$expectedName.html")
                    Language.KOTLIN -> assertThat(clazz.data.url)
                        .isEqualTo("/reference/kotlin/androidx/example/$expectedName.html")
                }
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
            val converter = RootDocumentableConverter(root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassIndex
            assertThat(classIndex.data.alphabetizedClasses).hasSize(2)

            val (fooLetter, fooSummary) = classIndex.data.alphabetizedClasses.entries.first()
            assertThat(fooLetter).isEqualTo('F')
            assertThat(fooSummary.data.items).hasSize(1)

            val fooClazz = (fooSummary.data.items.single() as TwoPaneSummaryItem).data.title as Link
            assertThat(fooClazz.data.name).isEqualTo("Foo")
            when (language) {
                Language.JAVA -> assertThat(fooClazz.data.url)
                    .isEqualTo("/reference/androidx/example/Foo.html")
                Language.KOTLIN -> assertThat(fooClazz.data.url)
                    .isEqualTo("/reference/kotlin/androidx/example/Foo.html")
            }

            val (barLetter, barSummary) = classIndex.data.alphabetizedClasses.entries.last()
            assertThat(barLetter).isEqualTo('B')
            assertThat(barSummary.data.items).hasSize(1)

            val barClazz = (barSummary.data.items.single() as TwoPaneSummaryItem).data.title as Link
            assertThat(barClazz.data.name).isEqualTo("Bar")
            when (language) {
                Language.JAVA -> assertThat(barClazz.data.url)
                    .isEqualTo("/reference/androidx/example/Bar.html")
                Language.KOTLIN -> assertThat(barClazz.data.url)
                    .isEqualTo("/reference/kotlin/androidx/example/Bar.html")
            }
        }
    }

    @Test
    fun `Package index creates components with correct page title`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(root, pathProvider())

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
            val converter = RootDocumentableConverter(root, pathProvider())

            val components = converter.packagesPage()

            val packageIndex = components.data.content as PackageIndex
            when (language) {
                Language.JAVA -> assertThat(packageIndex.data.classesUrl)
                    .isEqualTo("/reference/androidx/classes.html")
                Language.KOTLIN -> assertThat(packageIndex.data.classesUrl)
                    .isEqualTo("/reference/kotlin/androidx/classes.html")
            }
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
            val converter = RootDocumentableConverter(root, pathProvider())

            val components = converter.packagesPage()

            val packageIndex = components.data.content as PackageIndex
            val packages = packageIndex.data.packages.data.items
            assertThat(packages).hasSize(1)

            val packageLink = (packages.single() as TwoPaneSummaryItem).data.title as Link
            assertThat(packageLink.data.name).isEqualTo("androidx.example")
            when (language) {
                Language.JAVA -> assertThat(packageLink.data.url)
                    .isEqualTo("/reference/androidx/example/package-summary.html")
                Language.KOTLIN -> assertThat(packageLink.data.url)
                    .isEqualTo("/reference/kotlin/androidx/example/package-summary.html")
            }
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
            val converter = RootDocumentableConverter(root, pathProvider())

            val components = converter.packagesPage()

            val packageIndex = components.data.content as PackageIndex
            val packages = packageIndex.data.packages.data.items
            assertThat(packages).hasSize(3)

            val expectedPackages = listOf("a", "b", "c")
            for ((i, packageItem) in packages.withIndex()) {
                val packageLink = (packageItem as TwoPaneSummaryItem).data.title as Link

                assertThat(packageLink.data.name).isEqualTo(expectedPackages[i])
                when (language) {
                    Language.JAVA -> assertThat(packageLink.data.url)
                        .isEqualTo("/reference/${expectedPackages[i]}/package-summary.html")
                    Language.KOTLIN -> assertThat(packageLink.data.url)
                        .isEqualTo("/reference/kotlin/${expectedPackages[i]}/package-summary.html")
                }
            }
        }
    }

    private fun pathProvider() = when (language) {
        Language.JAVA -> DacJavaFilePathProvider("androidx")
        Language.KOTLIN -> DacKotlinFilePathProvider("androidx")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
