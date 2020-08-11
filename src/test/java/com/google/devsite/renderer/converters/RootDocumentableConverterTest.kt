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
import com.google.devsite.components.ClassesIndex
import com.google.devsite.components.Type
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
    fun `Classes index creates components with correct page title`() {
        val source = """
            |class Foo
        """.trimIndent()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(root, pathProvider())

            val components = converter.classesPage()

            assertThat(components.data.title).isEqualTo("Class Index")
        }
    }

    @Test
    fun `Classes index creates components with correct packages link`() {
        val source = """
            |class Foo
        """.trimIndent()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassesIndex
            when (language) {
                Language.JAVA -> assertThat(classIndex.data.packagesUrl)
                    .isEqualTo("/reference/androidx/packages.html")
                Language.KOTLIN -> assertThat(classIndex.data.packagesUrl)
                    .isEqualTo("/reference/kotlin/androidx/packages.html")
            }
        }
    }

    @Test
    fun `Classes index creates components for single class`() {
        val source = """
            |class Foo
        """.trimIndent()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassesIndex
            assertThat(classIndex.data.alphabetizedClasses).hasSize(1)

            val (letter, summary) = classIndex.data.alphabetizedClasses.entries.single()
            assertThat(letter).isEqualTo('F')
            assertThat(summary.data.items).hasSize(1)

            val clazz = summary.data.items.single().data.title as Type
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
    fun `Classes index creates components for multiple classes starting with same letter`() {
        val source = """
            |class Fo
            |class Foo
            |class Fooo
        """.trimIndent()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassesIndex
            assertThat(classIndex.data.alphabetizedClasses).hasSize(1)

            val (letter, summary) = classIndex.data.alphabetizedClasses.entries.single()
            assertThat(letter).isEqualTo('F')
            assertThat(summary.data.items).hasSize(3)

            val expectedClasses = listOf("Fo", "Foo", "Fooo")
            val classes = summary.data.items.map { it.data.title as Type }
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
    fun `Classes index creates components for multiple classes starting with different letters`() {
        val source = """
            |class Foo
            |class Bar
        """.trimIndent()

        testWithRootPageNode(source) { root ->
            val converter = RootDocumentableConverter(root, pathProvider())

            val components = converter.classesPage()

            val classIndex = components.data.content as ClassesIndex
            assertThat(classIndex.data.alphabetizedClasses).hasSize(2)

            val (fooLetter, fooSummary) = classIndex.data.alphabetizedClasses.entries.first()
            assertThat(fooLetter).isEqualTo('F')
            assertThat(fooSummary.data.items).hasSize(1)

            val fooClazz = fooSummary.data.items.single().data.title as Type
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

            val barClazz = barSummary.data.items.single().data.title as Type
            assertThat(barClazz.data.name).isEqualTo("Bar")
            when (language) {
                Language.JAVA -> assertThat(barClazz.data.url)
                    .isEqualTo("/reference/androidx/example/Bar.html")
                Language.KOTLIN -> assertThat(barClazz.data.url)
                    .isEqualTo("/reference/kotlin/androidx/example/Bar.html")
            }
        }
    }

    private fun pathProvider() = when (language) {
        Language.JAVA -> DacJavaFilePathProvider("androidx")
        Language.KOTLIN -> DacKotlinFilePathProvider("androidx")
    }

    // TODO(asaveau): remove in favor of the real language enum, I don't know where that will be ATM
    enum class Language { JAVA, KOTLIN }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
