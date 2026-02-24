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
import com.google.devsite.components.pages.ClassIndex
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.PackageIndex
import com.google.devsite.components.pages.TableOfContents
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.testing.ConverterTestBase
import com.google.devsite.testing.createPluginsConfiguration
import com.google.devsite.testing.defaultDevsiteConfiguration
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DModule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class RootDocumentableConverterTest(displayLanguage: Language) :
    ConverterTestBase(
        displayLanguage,
        createPluginsConfiguration(defaultDevsiteConfiguration.copy(applyComposeTransformer = true)),
    ) {
    @Test
    fun `Class index creates components with correct page title`() {
        val page =
            """
            |class Foo
        """
                .render()
                .indexPageForClasses()

        assertThat(page.data.title).isEqualTo("Class Index")
    }

    @Test
    fun `Class index creates components with correct path`() {
        val page =
            """
            |class Foo
        """
                .render()
                .indexPageForClasses()

        assertThat(page.data.pathForSwitcher!!).isEqualTo("androidx/classes.html")
    }

    @Test
    fun `Class index creates components with correct book path`() {
        val page =
            """
            |class Foo
        """
                .render()
                .indexPageForClasses()

        assertPath(page.data.bookPath, "androidx/_book.yaml")
    }

    @Test
    fun `Class index creates components with correct packages link`() {
        val page =
            """
            |class Foo
        """
                .render()
                .indexPageForClasses()

        val classIndex = page.data.content

        assertPath(classIndex.data.packagesUrl, "androidx/packages.html")
    }

    @Test
    fun `Class index creates components for single class`() {
        val page =
            """
            |class Foo
        """
                .render()
                .indexPageForClasses()

        val classIndex = page.data.content
        val (letter, summary) = classIndex.item()

        assertThat(letter).isEqualTo('F')
        assertThat(summary.item().link().name).isEqualTo("Foo")
        assertPath(summary.item().link().url, "androidx/example/Foo.html")
    }

    @Test
    fun `Class index creates components for nested class`() {
        val page =
            """
            |class Outer { class Inner }
        """
                .render()
                .indexPageForClasses()

        val classIndex = page.data.content
        val (letter, summary) = classIndex.item()
        val outer = summary.items(2).first()
        val inner = summary.items(2).last()

        assertThat(letter).isEqualTo('O')
        assertThat(outer.link().name).isEqualTo("Outer")
        assertPath(outer.link().url, "androidx/example/Outer.html")
        assertThat(inner.link().name).isEqualTo("Outer.Inner")
        assertPath(inner.link().url, "androidx/example/Outer.Inner.html")
    }

    @Test
    fun `Class index creates components for enum`() {
        val page =
            """
            |enum class Choice { A, B }
        """
                .render()
                .indexPageForClasses()

        val classIndex = page.data.content
        val (letter, summary) = classIndex.item()

        assertThat(letter).isEqualTo('C')
        assertThat(summary.item().link().name).isEqualTo("Choice")
        assertPath(summary.item().link().url, "androidx/example/Choice.html")
    }

    @Test
    fun `Class index creates components for multiple classes starting with same letter`() {
        val page =
            """
            |class Fo
            |class Foo
            |class Fooo
        """
                .render()
                .indexPageForClasses()

        val classIndex = page.data.content
        val (letter, summary) = classIndex.item()

        assertThat(letter).isEqualTo('F')

        val expectedClasses = listOf("Fo", "Foo", "Fooo")
        for ((i, clazz) in summary.items(3).withIndex()) {
            val expectedName = expectedClasses[i]

            assertThat(clazz.link().name).isEqualTo(expectedName)
            assertPath(clazz.link().url, "androidx/example/$expectedName.html")
        }
    }

    @Test
    fun `Class index creates components with sorted classes`() {
        val page =
            """
            |class AB
            |class AA
        """
                .render()
                .indexPageForClasses()

        val classIndex = page.data.content
        val classes = classIndex.item().value.items(2)

        assertThat(classes.first().link().name).isEqualTo("AA")
        assertThat(classes.last().link().name).isEqualTo("AB")
    }

    @Test
    fun `Class index creates components with sorted classes across packages`() {
        val page =
            listOf(
                    """
                    |/src/main/kotlin/androidx/example/A.kt
                    |package a
                    |
                    |class AB
                    """
                        .trimMargin(),
                    """
                    |/src/main/kotlin/androidx/example/B.kt
                    |package b
                    |
                    |class AA
                    """
                        .trimMargin(),
                )
                .render()
                .indexPageForClasses()

        val classIndex = page.data.content
        val classes = classIndex.item().value.items(2)

        assertThat(classes.first().link().name).isEqualTo("AA")
        assertThat(classes.last().link().name).isEqualTo("AB")
    }

    @Test
    fun `Class index creates components with sorted categories`() {
        val page =
            """
            |class B
            |class A
        """
                .render()
                .indexPageForClasses()

        val classIndex = page.data.content
        val classes = classIndex.items(2)

        assertThat(classes.first().key).isEqualTo('A')
        assertThat(classes.last().key).isEqualTo('B')
    }

    @Test
    fun `Class index creates components for multiple classes starting with different letters`() {
        val page =
            """
            |class Foo
            |class Bar
        """
                .render()
                .indexPageForClasses()

        val classIndex = page.data.content
        val (fooLetter, fooSummary) = classIndex.items(2).last()
        val (barLetter, barSummary) = classIndex.items(2).first()

        assertThat(fooLetter).isEqualTo('F')
        assertThat(barLetter).isEqualTo('B')
        assertThat(fooSummary.item().link().name).isEqualTo("Foo")
        assertThat(barSummary.item().link().name).isEqualTo("Bar")
        assertPath(fooSummary.item().link().url, "androidx/example/Foo.html")
        assertPath(barSummary.item().link().url, "androidx/example/Bar.html")
    }

    @Test
    fun `Package index creates components with correct page title`() {
        val page =
            """
            |class Foo
        """
                .render()
                .indexPageForPackages()

        assertThat(page.data.title).isEqualTo("Package Index")
    }

    @Test
    fun `Package index creates components with correct path`() {
        val page =
            """
            |class Foo
        """
                .render()
                .indexPageForPackages()

        assertThat(page.data.pathForSwitcher!!).isEqualTo("androidx/packages.html")
    }

    @Test
    fun `Package index creates components with correct book path`() {
        val page =
            """
            |class Foo
        """
                .render()
                .indexPageForPackages()

        assertPath(page.data.bookPath, "androidx/_book.yaml")
    }

    @Test
    fun `Package index creates components with correct classes link`() {
        val page =
            """
            |class Foo
        """
                .render()
                .indexPageForPackages()

        val packageIndex = page.data.content

        assertPath(packageIndex.data.classesUrl, "androidx/classes.html")
    }

    @Test
    fun `Package index creates components for single package`() {
        val page =
            listOf(
                    """
                    |/src/main/kotlin/androidx/example/Test.kt
                    |package androidx.example
                    |
                    |class Foo
                    """
                        .trimMargin()
                )
                .render()
                .indexPageForPackages()

        val packageIndex = page.data.content
        val packagez = packageIndex.data.packages.item()

        assertThat(packagez.link().name).isEqualTo("androidx.example")
        assertPath(packagez.link().url, "androidx/example/package-summary.html")
    }

    @Test
    fun `Package index creates components for multiple packages`() {
        val page =
            listOf(
                    """
                    |/src/main/kotlin/androidx/example/A.kt
                    |package a
                    |
                    |class A
                    """
                        .trimMargin(),
                    """
                    |/src/main/kotlin/androidx/example/B.kt
                    |package b
                    |
                    |class B
                    """
                        .trimMargin(),
                    """
                    |/src/main/kotlin/androidx/example/C.kt
                    |package c
                    |
                    |class C
                    """
                        .trimMargin(),
                )
                .render()
                .indexPageForPackages()

        val packageIndex = page.data.content
        val packages = packageIndex.data.packages.items(3)

        val expectedPackages = listOf("a", "b", "c")
        for ((i, packagez) in packages.withIndex()) {
            assertThat(packagez.link().name).isEqualTo(expectedPackages[i])
            assertPath(packagez.link().url, "${expectedPackages[i]}/package-summary.html")
        }
    }

    @Test
    fun `Package index creates components with sorted packages`() {
        val page =
            listOf(
                    """
                    |/src/main/kotlin/androidx/example/B.kt
                    |package b
                    |
                    |class B
                    """
                        .trimMargin(),
                    """
                    |/src/main/kotlin/androidx/example/a/A.kt
                    |package a
                    |
                    |class A
                    """
                        .trimMargin(),
                )
                .render()
                .indexPageForPackages()

        val packageIndex = page.data.content
        val packages = packageIndex.data.packages.items(2)

        assertThat(packages.first().link().name).isEqualTo("a")
        assertThat(packages.last().link().name).isEqualTo("b")
    }

    @Test
    fun `Toc creates components with correct metadata links`() {
        val toc =
            """
            |class Foo
        """
                .render()
                .toc()

        assertPath(toc.data.classesUrl, "androidx/classes.html")
        assertPath(toc.data.packagesUrl, "androidx/packages.html")
    }

    @Test
    fun `Toc creates components with correct package link`() {
        val toc =
            """
            |class Foo
        """
                .render()
                .toc()

        val tocPackage = toc.item()

        assertThat(tocPackage.data.name).isEqualTo("androidx.example")
        assertPath(tocPackage.data.packageUrl, "androidx/example/package-summary.html")
    }

    @Test
    fun `Toc creates components with correct class link`() {
        val toc =
            """
            |class Foo
        """
                .render()
                .toc()

        val tocPackage = toc.item()
        val clazz = tocPackage.data.classes.item()

        assertThat(clazz.name).isEqualTo("Foo")
        assertPath(clazz.url, "androidx/example/Foo.html")
    }

    @Test
    fun `Toc creates components with correct inner class link`() {
        val toc =
            """
            |class Outer { class Inner }
        """
                .render()
                .toc()

        val tocPackage = toc.item()
        val inner = tocPackage.data.classes.items(2).last()

        assertThat(inner.name).isEqualTo("Outer.Inner")
        assertPath(inner.url, "androidx/example/Outer.Inner.html")
    }

    @Test
    fun `Toc includes top level objects in Kotlin`() {
        val toc =
            """
            |object Foo {}
        """
                .render()
                .toc()

        val tocPackage = toc.item()
        javaOnly {
            // TODO(b/203678085): Objects should be accessible from top-level static inner class
            assertThat(tocPackage.data.objects).isEmpty()
        }
        kotlinOnly {
            val foo = tocPackage.data.objects.item()
            assertThat(foo.name).isEqualTo("Foo")
            assertPath(foo.url, "androidx/example/Foo.html")
        }
    }

    @Test
    fun `Toc does not include companion objects`() {
        val toc =
            """
            |class Foo {
            |    companion object {}
            |}
            |class Bar {
            |    companion object Baz {}
            |}
        """
                .render()
                .toc()

        val tocPackage = toc.item()
        kotlinOnly {
            assertThat(tocPackage.data.objects.single().name).isEqualTo("Bar.Baz")
            assertThat(tocPackage.data.classes.map { it.name }).containsExactly("Foo", "Bar")
        }
        javaOnly {
            assertThat(tocPackage.data.objects).isEmpty()
            assertThat(tocPackage.data.classes.map { it.name })
                .containsExactly("Foo", "Bar", "Bar.Baz")
        }
    }

    @Test
    fun `Toc includes composables and modifiers`() {
        kotlinOnly {
            val toc =
                renderCompose(
                        """
                        @Composable fun TestComposable() = Unit
                        fun Modifier.TestModifier() = Unit
                        """
                            .trimIndent()
                    )
                    .toc()
            // Skip the compose packages which are generated based on the stubs from `renderCompose`
            val tocPackage = toc.items(3).last()

            val composable = tocPackage.data.composables.item()
            assertThat(composable.name).isEqualTo("TestComposable")
            assertThat(composable.url)
                .isEqualTo("/reference/kotlin/com/example/TestComposable.composable.html")

            val modifier = tocPackage.data.modifiers.item()
            assertThat(modifier.name).isEqualTo("TestModifier")
            assertThat(modifier.url)
                .isEqualTo("/reference/kotlin/com/example/TestModifier.modifier.html")
        }
    }

    @Test
    fun `Toc removes package prefix if specified`() {
        val toc =
            listOf(
                    """
                    |/src/main/kotlin/androidx/example/B.kt
                    |package androidx.example.b
                    |
                    |class B
                    """
                        .trimMargin(),
                    """
                    |/src/main/kotlin/androidx/example/a/A.kt
                    |package androidx.example.a
                    |
                    |class A
                    """
                        .trimMargin(),
                )
                .render()
                .toc(packagePrefixToRemove = "androidx.example")

        val packageNames = toc.data.packages.map { it.data.name }
        assertThat(packageNames).containsExactly("a", "b")
    }

    @Test
    fun `Class summary only contains synthetic classes for Java display`() {
        val classes =
            """
            |fun foo(): Unit {}
        """
                .render()
                .indexPageForClasses()
                .data
                .content
                .data
                .alphabetizedClasses
        javaOnly {
            // T is for TestKt
            assertThat(classes).containsKey('T')
        }
        kotlinOnly { assertThat(classes).isEmpty() }
    }

    private fun DModule.rootConverter() =
        ConverterHolder(this@RootDocumentableConverterTest, this).rootDocumentableConverter

    private fun DModule.indexPageForClasses(): DevsitePage<ClassIndex> {
        return runBlocking { rootConverter().classesIndexPage() }
    }

    private fun DModule.indexPageForPackages(): DevsitePage<PackageIndex> {
        return runBlocking { rootConverter().packagesIndexPage() }
    }

    private fun DModule.toc(packagePrefixToRemove: String? = null): TableOfContents {
        return runBlocking { rootConverter().tocPage(packagePrefixToRemove) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Language.JAVA), arrayOf(Language.KOTLIN))
    }
}
