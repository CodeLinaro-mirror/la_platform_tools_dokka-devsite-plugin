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
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.PackageSummary
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.content
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.summary
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DModule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PackageDocumentableConverterTest(
    private val displayLanguage: Language
) : ConverterTestBase(displayLanguage) {
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
    fun `Package summary creates components with sorted interfaces`() {
        val page = """
            |interface B
            |interface A
        """.render().page()

        val summary = page.content<PackageSummary>()
        val interfaces = summary.data.interfaces.items(2)

        assertThat(interfaces.first().link().name).isEqualTo("A")
        assertThat(interfaces.last().link().name).isEqualTo("B")
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
    fun `Package summary creates components with sorted classes`() {
        val page = """
            |class B
            |class A
        """.render().page()

        val summary = page.content<PackageSummary>()
        val classes = summary.data.classes.items(2)

        assertThat(classes.first().link().name).isEqualTo("A")
        assertThat(classes.last().link().name).isEqualTo("B")
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
    fun `Package summary creates components with sorted enums`() {
        val page = """
            |enum class B
            |enum class A
        """.render().page()

        val summary = page.content<PackageSummary>()
        val enums = summary.data.enums.items(2)

        assertThat(enums.first().link().name).isEqualTo("A")
        assertThat(enums.last().link().name).isEqualTo("B")
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
    fun `Package summary creates components with sorted exceptions`() {
        val page = """
            |class B : RuntimeException()
            |class A : RuntimeException()
        """.render().page()

        val summary = page.content<PackageSummary>()
        val classes = summary.data.classes.items(0)
        val exceptions = summary.data.exceptions.items(2)

        assertThat(exceptions.first().link().name).isEqualTo("A")
        assertThat(exceptions.last().link().name).isEqualTo("B")
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
    fun `Package summary creates components with sorted annotations`() {
        val page = """
            |annotation class B
            |annotation class A
        """.render().page()

        val summary = page.content<PackageSummary>()
        val classes = summary.data.annotations.items(2)

        assertThat(classes.first().link().name).isEqualTo("A")
        assertThat(classes.last().link().name).isEqualTo("B")
    }

    @Test
    fun `Package summary creates components for type aliases`() {
        val page = """
            |typealias ImATypeAlias = String
        """.render().page()

        val summary = page.content<PackageSummary>()
        val annotation = summary.data.typeAliases.item()

        assertThat(annotation.link().name).isEqualTo("ImATypeAlias")
        assertPath(annotation.link().url, "androidx/example/ImATypeAlias.html")
    }

    @Test
    fun `Package summary creates components with sorted type aliases`() {
        val page = """
            |typealias B = String
            |typealias A = String
        """.render().page()

        val summary = page.content<PackageSummary>()
        val classes = summary.data.typeAliases.items(2)

        assertThat(classes.first().link().name).isEqualTo("A")
        assertThat(classes.last().link().name).isEqualTo("B")
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
        assertThat(topLevels.item().summary().name()).isEqualTo("foo")
    }

    @Test
    fun `Package summary creates synthetic classes for top-level functions in Java`() {
        val page = """
            |fun foo()
        """.render().page()

        val summary = page.content<PackageSummary>()
        val classes = summary.data.classes.items()

        javaOnly {
            assertThat(classes).hasSize(1)
            val className = classes.item().link().name
            assertThat(className).isEqualTo("TestKt")
        }
        kotlinOnly {
            assertThat(classes).hasSize(0)
        }
    }

    @Test
    fun `Synthetic classes for top-level functions in Java use @JvmName`() {
        val page = """
            |@JvmName("bar")
            |fun foo()
            |
            |@JvmName("aardvark")
            |fun baz()
            |
            |fun apple()
        """.render()

        javaOnly {
            val classes = runBlocking {
                val holder = DocumentablesHolder(page, this)
                holder.classesFor(page.packages.last(), displayLanguage)
            }
            // testing first and last here is also asserting the alphabetical sort, after jvmname
            val names = classes.last().functions.map { it.name }
            assertThat(names).isEqualTo(listOf("aardvark", "apple", "bar"))
        }
    }

    @Test
    fun `Synthetic classes for top-level functions use file JvmName appropriately`() {
        val page = """
        |
        |fun foo()
        |
        """.render(fileUseAnnotation = "@file:JvmName(\"PagingRx\")")

        javaOnly {
            val classes = runBlocking {
                val holder = DocumentablesHolder(page, this)
                holder.classesFor(page.packages.last(), displayLanguage)
            }
            assertThat(classes.last().name).isEqualTo("PagingRx")
            assertThat(classes.last().dri.classNames).isEqualTo("PagingRx")

            assertThat(classes.last().functions.last().name).isEqualTo("foo")
            assertThat(classes.last().functions.last().dri.classNames).isEqualTo("PagingRx")
        }

        kotlinOnly {
            val classes = runBlocking {
                val holder = DocumentablesHolder(page, this)
                holder.classesFor(page.packages.last(), displayLanguage)
            }
            assertThat(classes).isEmpty()
        }
    }

    @Test
    fun `Package summary creates components with sorted top-level functions`() {
        val page = """
            |fun b() = Unit
            |fun a() = Unit
        """.render().page()

        val summary = page.content<PackageSummary>()
        val topLevels = summary.data.topLevelFunctionsSummary.items(2)

        assertThat(topLevels.first().summary().name()).isEqualTo("a")
        assertThat(topLevels.last().summary().name()).isEqualTo("b")
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
        assertThat(extensions.item().summary().name()).isEqualTo("foo")
    }

    @Test
    fun `Package summary creates components with sorted extension functions`() {
        val page = """
            |fun String.b() = Unit
            |fun String.a() = Unit
        """.render().page()

        val summary = page.content<PackageSummary>()
        val extensions = summary.data.extensionFunctionsSummary.items(2)

        assertThat(extensions.first().summary().name()).isEqualTo("a")
        assertThat(extensions.last().summary().name()).isEqualTo("b")
    }

    @Test
    fun `Companion objects are documented in Java but not Kotlin because they're inlined`() {
        val packageSummary = """
            |class Foo {
            |    companion object FooCompanion
            |}
            |class Bar {
            |    companion object
            |}
        """.render().page().content<PackageSummary>()

        assertThat("FooCompanion" in packageSummary.data.classes.items().map { it.name() })

        kotlinOnly {
            assertThat("Companion" !in packageSummary.data.classes.items().map { it.name() })
        }
        javaOnly {
            assertThat("Companion" in packageSummary.data.classes.items().map { it.name() })
        }
    }

    private fun DModule.page(): DevsitePage {
        val (holder, pathProvider) = holderAndProvider(this)
        val converter =
            PackageDocumentableConverter(
                displayLanguage,
                packages.single(),
                pathProvider,
                holder
            )
        return runBlocking { converter.summaryPage() }
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
