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
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.testing.ConverterTestBase
import com.google.devsite.testing.createPluginsConfiguration
import com.google.devsite.testing.defaultDevsiteConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PackageDocumentableConverterTest(displayLanguage: Language) :
    ConverterTestBase(
        displayLanguage,
        createPluginsConfiguration(defaultDevsiteConfiguration.copy(applyComposeTransformer = true)),
    ) {
    @Test
    fun `Package summary creates components with correct page title`() {
        val page =
            listOf(
                    """
                    |/src/main/kotlin/androidx/example/A.kt
                    |package hello.i.am.a.packagez
                    |
                    |class A
                    """
                        .trimMargin()
                )
                .render()
                .packagePage()

        assertThat(page.data.title).isEqualTo("hello.i.am.a.packagez")
    }

    @Test
    fun `Package summary creates components with correct path`() {
        val page =
            listOf(
                    """
                    |/src/main/kotlin/androidx/example/A.kt
                    |package hello.i.am.a.packagez
                    |
                    |class A
                    """
                        .trimMargin()
                )
                .render()
                .packagePage()

        assertThat(page.data.pathForSwitcher!!)
            .isEqualTo("hello/i/am/a/packagez/package-summary.html")
    }

    @Test
    fun `Package summary creates components with correct book path`() {
        val page =
            """
            |class Foo
        """
                .render()
                .packagePage()

        assertPath(page.data.bookPath, "androidx/_book.yaml")
    }

    @Test
    fun `Package summary creates components for interfaces`() {
        val page =
            """
            |interface ImAnInterface
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val interfacz = summary.data.interfaces.item()

        assertThat(interfacz.link().name).isEqualTo("ImAnInterface")
        assertPath(interfacz.link().url, "androidx/example/ImAnInterface.html")
    }

    @Test
    fun `Package summary creates components with sorted interfaces`() {
        val page =
            """
            |interface B
            |interface A
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val interfaces = summary.data.interfaces.items(2)

        assertThat(interfaces.first().link().name).isEqualTo("A")
        assertThat(interfaces.last().link().name).isEqualTo("B")
    }

    @Test
    fun `Package summary creates components for classes`() {
        val page =
            """
            |class ImAClass
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val clazz = summary.data.classes.item()

        assertThat(clazz.link().name).isEqualTo("ImAClass")
        assertPath(clazz.link().url, "androidx/example/ImAClass.html")
    }

    @Test
    fun `Package summary creates components with sorted classes`() {
        val page =
            """
            |class B
            |class A
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val classes = summary.data.classes.items(2)

        assertThat(classes.first().link().name).isEqualTo("A")
        assertThat(classes.last().link().name).isEqualTo("B")
    }

    @Test
    fun `Package summary creates components for nested classes`() {
        val page =
            """
            |class Outer { class Inner }
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val inner = summary.data.classes.items(2).last()

        assertThat(inner.link().name).isEqualTo("Outer.Inner")
        assertPath(inner.link().url, "androidx/example/Outer.Inner.html")
    }

    @Test
    fun `Package summary creates components for enums`() {
        val page =
            """
            |enum class ImAnEnum
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val enum = summary.data.enums.item()

        assertThat(enum.link().name).isEqualTo("ImAnEnum")
        assertPath(enum.link().url, "androidx/example/ImAnEnum.html")
    }

    @Test
    fun `Package summary creates components with sorted enums`() {
        val page =
            """
            |enum class B
            |enum class A
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val enums = summary.data.enums.items(2)

        assertThat(enums.first().link().name).isEqualTo("A")
        assertThat(enums.last().link().name).isEqualTo("B")
    }

    @Test
    fun `Package summary creates components for exceptions`() {
        val page =
            """
            |class ImAnException : RuntimeException()
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val exception = summary.data.exceptions.item()

        assertThat(exception.link().name).isEqualTo("ImAnException")
        assertPath(exception.link().url, "androidx/example/ImAnException.html")
    }

    @Test
    fun `Package summary creates components with sorted exceptions`() {
        val page =
            """
            |class B : RuntimeException()
            |class A : RuntimeException()
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val classes = summary.data.classes.items(0)
        val exceptions = summary.data.exceptions.items(2)

        assertThat(exceptions.first().link().name).isEqualTo("A")
        assertThat(exceptions.last().link().name).isEqualTo("B")
    }

    @Test
    fun `Package summary creates components for annotations`() {
        val page =
            """
            |annotation class ImAnAnnotation
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val annotation = summary.data.annotations.item()

        assertThat(annotation.link().name).isEqualTo("ImAnAnnotation")
        assertPath(annotation.link().url, "androidx/example/ImAnAnnotation.html")
    }

    @Test
    fun `Package summary creates components with sorted annotations`() {
        val page =
            """
            |annotation class B
            |annotation class A
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val classes = summary.data.annotations.items(2)

        assertThat(classes.first().link().name).isEqualTo("A")
        assertThat(classes.last().link().name).isEqualTo("B")
    }

    @Test
    fun `Package summary doesn't create components for type aliases`() {
        val page =
            """
            |typealias ImATypeAlias = String
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val annotation = summary.data.typeAliases.item()

        assertThat(annotation.link().name).isEqualTo("ImATypeAlias")
        assertThat(annotation.link().url).isEmpty()
    }

    @Test
    fun `Package summary creates components with sorted type aliases`() {
        val page =
            """
            |typealias B = String
            |typealias A = String
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val classes = summary.data.typeAliases.items(2)

        assertThat(classes.first().link().name).isEqualTo("A")
        assertThat(classes.last().link().name).isEqualTo("B")
    }

    @Test
    fun `Package summary creates components for top-level functions`() {
        val page =
            """
            |fun foo()
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val topLevels = summary.data.topLevelFunctionsSummary
        val extensions = summary.data.extensionFunctionsSummary

        assertThat(topLevels.items()).hasSize(1)
        assertThat(extensions.items()).isEmpty()
        assertThat(topLevels.item().data.description.name()).isEqualTo("foo")
    }

    @Test
    fun `Package summary creates synthetic classes for top-level functions in Java`() {
        val page =
            """
            |fun foo()
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val classes = summary.data.classes.items()

        javaOnly {
            assertThat(classes).hasSize(1)
            val className = classes.item().link().name
            assertThat(className).isEqualTo("TestKt")
        }
        kotlinOnly { assertThat(classes).hasSize(0) }
    }

    @Test
    fun `Synthetic classes for top-level functions use file JvmName appropriately`() {
        val page =
            """
        |
        |fun foo()
        |
        """
                .render(fileUseAnnotation = "@file:JvmName(\"PagingRx\")")

        javaOnly {
            val classes = runBlocking {
                val holder = ConverterHolder(this@PackageDocumentableConverterTest, page).holder
                holder.classlikesToDisplayFor(page.packages.last())
            }
            assertThat(classes.last().name).isEqualTo("PagingRx")
            assertThat(classes.last().dri.classNames).isEqualTo("PagingRx")

            assertThat(classes.last().functions.last().name).isEqualTo("foo")
            assertThat(classes.last().functions.last().dri.classNames).isEqualTo("PagingRx")
        }

        kotlinOnly {
            val classes = runBlocking {
                val holder = ConverterHolder(this@PackageDocumentableConverterTest, page).holder
                holder.classlikesToDisplayFor(page.packages.last())
            }
            assertThat(classes).isEmpty()
        }
    }

    @Test
    fun `Package summary creates components with sorted top-level functions`() {
        val page =
            """
            |fun b() = Unit
            |fun a() = Unit
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val topLevels = summary.data.topLevelFunctionsSummary.items(2)

        assertThat(topLevels.first().data.description.name()).isEqualTo("a")
        assertThat(topLevels.last().data.description.name()).isEqualTo("b")
    }

    @Test
    fun `Package summary creates components for extension functions`() {
        val page =
            """
            |fun String.foo()
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val topLevels = summary.data.topLevelFunctionsSummary
        val extensions = summary.data.extensionFunctionsSummary

        assertThat(topLevels.items()).isEmpty()
        assertThat(extensions.items()).hasSize(1)
        assertThat(extensions.item().data.description.name()).isEqualTo("foo")
    }

    @Test
    fun `Package summary creates components with sorted extension functions`() {
        val page =
            """
            |fun String.b() = Unit
            |fun String.a() = Unit
        """
                .render()
                .packagePage()

        val summary = page.data.content
        val extensions = summary.data.extensionFunctionsSummary.items(2)

        assertThat(extensions.first().data.description.name()).isEqualTo("a")
        assertThat(extensions.last().data.description.name()).isEqualTo("b")
    }

    @Test
    fun `Companion objects are documented in Java but not Kotlin because they're inlined`() {
        val packageSummary =
            """
            |class Foo {
            |    companion object FooCompanion
            |}
            |class Bar {
            |    companion object
            |}
        """
                .render()
                .packagePage()
                .data
                .content

        assertThat("FooCompanion" in packageSummary.data.classes.items().map { it.name() })

        kotlinOnly {
            assertThat("Companion" !in packageSummary.data.classes.items().map { it.name() })
        }
        javaOnly {
            assertThat("Companion" in packageSummary.data.classes.items().map { it.name() })
        }
    }

    @Test
    fun `Multiple packages can be used in one test`() {
        val module =
            """
            /src/main/test/pkgA/A.kt
            package test.pkgA
            class A

            /src/main/test/pkgB/B.kt
            package test.pkgB
            class B
            """
                .trimIndent()
                .render()

        val pkgA = module.packagePage("test.pkgA")
        assertThat(pkgA.data.title).isEqualTo("test.pkgA")

        val pkgB = module.packagePage("test.pkgB")
        assertThat(pkgB.data.title).isEqualTo("test.pkgB")
    }

    @Test
    fun `Composables are included in package summary`() {
        kotlinOnly {
            val packageSummary =
                renderCompose(
                        """
                        /** TestComposableA documentation */
                        @Composable fun TestComposableA() = Unit
                        /** TestComposableB documentation */
                        @Composable fun TestComposableB() = Unit
                        """
                            .trimIndent()
                    )
                    .packagePage("com.example")
                    .data
                    .content
                    .data
            val items = packageSummary.composables.items(2)

            val composableA = items.first().data
            val composableATitle = composableA.title.data
            assertThat(composableATitle.name).isEqualTo("TestComposableA")
            assertThat(composableATitle.url)
                .isEqualTo("/reference/kotlin/com/example/TestComposableA.composable.html")
            val composableADescription = composableA.description
            assertThat(composableADescription.data.summary).isTrue()
            assertThat(composableADescription.text()).isEqualTo("TestComposableA documentation")

            val composableB = items.last().data
            val composableBTitle = composableB.title.data
            assertThat(composableBTitle.name).isEqualTo("TestComposableB")
            assertThat(composableBTitle.url)
                .isEqualTo("/reference/kotlin/com/example/TestComposableB.composable.html")
            val composableBDescription = composableB.description
            assertThat(composableBDescription.data.summary).isTrue()
            assertThat(composableBDescription.text()).isEqualTo("TestComposableB documentation")
        }
    }

    @Test
    fun `Modifier is included in package summary`() {
        kotlinOnly {
            val packageSummary =
                renderCompose(
                        """
                        /** TestModifierA documentation */
                        fun Modifier.TestModifierA() = Unit
                        /** TestModifierB documentation */
                        fun Modifier.TestModifierB() = Unit
                        """
                            .trimIndent()
                    )
                    .packagePage("com.example")
                    .data
                    .content
                    .data
            val items = packageSummary.modifiers.items(2)

            val modifierA = items.first().data
            val modifierATitle = modifierA.title.data
            assertThat(modifierATitle.name).isEqualTo("TestModifierA")
            assertThat(modifierATitle.url)
                .isEqualTo("/reference/kotlin/com/example/TestModifierA.modifier.html")
            val modifierADescription = modifierA.description
            assertThat(modifierADescription.data.summary).isTrue()
            assertThat(modifierADescription.text()).isEqualTo("TestModifierA documentation")

            val modifierB = items.last().data
            val modifierBTitle = modifierB.title.data
            assertThat(modifierBTitle.name).isEqualTo("TestModifierB")
            assertThat(modifierBTitle.url)
                .isEqualTo("/reference/kotlin/com/example/TestModifierB.modifier.html")
            val modifierBDescription = modifierB.description
            assertThat(modifierBDescription.data.summary).isTrue()
            assertThat(modifierBDescription.text()).isEqualTo("TestModifierB documentation")
        }
    }

    @Test
    fun `Correct documentation is chosen for function group with multiple elements`() {
        kotlinOnly {
            val packageSummary =
                renderCompose(
                        """
                        /** Second function documentation */
                        @Composable fun TestComposable(i: Int) = Unit
                        /** First function documentation */
                        @Composable fun TestComposable() = Unit
                        /** Third function documentation */
                        @Composable fun Int.TestComposable() = Unit
                        """
                            .trimIndent()
                    )
                    .packagePage("com.example")
                    .data
                    .content
                    .data

            val composable = packageSummary.composables.item().data
            val composableTitle = composable.title.data
            assertThat(composableTitle.name).isEqualTo("TestComposable")
            assertThat(composableTitle.url)
                .isEqualTo("/reference/kotlin/com/example/TestComposable.composable.html")

            val composableDescription = composable.description
            assertThat(composableDescription.data.summary).isTrue()
            assertThat(composableDescription.text()).isEqualTo("First function documentation")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Language.JAVA), arrayOf(Language.KOTLIN))
    }
}
