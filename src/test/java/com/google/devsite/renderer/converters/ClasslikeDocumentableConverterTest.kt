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
import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.table.SingleColumnSummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.content
import com.google.devsite.renderer.converters.testing.functionSummary
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.title
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DModule
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ClasslikeDocumentableConverterTest(
    private val language: Language
) : ConverterTestBase(language) {
    @Test
    fun `Classlike creates components with correct title`() {
        val page = """
            |class Foo
        """.render().page()

        assertThat(page.data.title).isEqualTo("Foo")
    }

    @Test
    fun `Classlike creates components with correct path`() {
        val page = """
            |class Foo
        """.render().page()

        assertThat(page.data.path).isEqualTo("androidx/example/Foo.html")
    }

    @Test
    fun `Classlike creates components with correct book path`() {
        val page = """
            |class Foo
        """.render().page()

        assertPath(page.data.bookPath, "androidx/_book.yaml")
    }

    @Test
    fun `Empty classlike has no symbols`() {
        val page = """
            |interface Foo
        """.render().page()

        val classlike = page.content<Classlike>()

        for ((summary, symbol) in classlike.data.symbolTypes) {
            assertThat(summary.hasContent()).isFalse()
            assertThat(symbol.symbols).isEmpty()
        }
    }

    @Test
    fun `Public function gets documented`() {
        val page = """
            |class Foo {
            |    fun foo() = Unit
            |}
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsFor("Public functions", "Public methods")

        assertThat(summary.item().functionSummary().name()).isEqualTo("foo")
    }

    @Ignore // TODO(b/165112358): foo doesn't show up in the dokka model
    @Test
    fun `Protected function gets documented`() {
        val page = """
            |abstract class Foo {
            |    protected open fun foo() = Unit
            |}
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsFor("Protected functions", "Protected methods")

        assertThat(summary.item().functionSummary().name()).isEqualTo("foo")
    }

    @Test
    fun `Public property gets documented`() {
        val page = """
            |class Foo {
            |    val foo = Unit
            |}
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsFor("Public properties", "Public fields")

        assertThat(summary.item().functionSummary().name()).isEqualTo("foo")
    }

    @Ignore // TODO(b/165112358): foo doesn't show up in the dokka model
    @Test
    fun `Protected property gets documented`() {
        val page = """
            |abstract class Foo {
            |    protected open val foo = Unit
            |}
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsFor("Protected properties", "Protected fields")

        assertThat(summary.item().functionSummary().name()).isEqualTo("foo")
    }

    @Test
    fun `Public constructor gets documented`() {
        val page = """
            |class Foo
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsFor("Public constructors")

        assertThat(summary.constructor().name()).isEqualTo("Foo")
    }

    @Ignore // TODO(b/165112358): foo doesn't show up in the dokka model
    @Test
    fun `Protected constructor gets documented`() {
        val page = """
            |open class Foo protected constructor()
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsFor("Protected constructors")

        assertThat(summary.constructor().name()).isEqualTo("Foo")
    }

    @Test
    fun `Nested type gets documented`() {
        val page = """
            |class Foo {
            |    class Bar
            |}
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsFor("Nested types")

        assertThat(summary.item().link().name).isEqualTo("Foo.Bar")
    }

    @Test
    fun `Direct subclasses are found`() {
        val page = """
            |abstract class Foo
            |open class C : B
            |open class B : Foo
            |open class A : Foo
        """.render().page()

        val classlike = page.content<Classlike>()
        val subclasses = classlike.data.relatedSymbols.data.directSubclasses.items(2)

        assertThat(subclasses.first().data.name).isEqualTo("A")
        assertThat(subclasses.last().data.name).isEqualTo("B")
    }

    @Test
    fun `Indirect subclasses are found`() {
        val page = """
            |abstract class Foo
            |open class C : Foo
            |open class B : C
            |open class A : B
        """.render().page()

        val classlike = page.content<Classlike>()
        val subclasses = classlike.data.relatedSymbols.data.indirectSubclasses.items(2)

        assertThat(subclasses.first().data.name).isEqualTo("A")
        assertThat(subclasses.last().data.name).isEqualTo("B")
    }

    @Test
    fun `Class with root object as parent does not have hierarchy`() {
        val page = """
            |class Foo
        """.render().page()

        val classlike = page.content<Classlike>()
        val parents = classlike.data.hierarchy.data.parents

        assertThat(parents).isEmpty()
    }

    @Test
    fun `Class with single parent has hierarchy with root object, parent, and itself`() {
        val page = """
            |abstract class Parent
            |class Foo : Parent
        """.render().page()

        val classlike = page.content<Classlike>()
        val parents = classlike.data.hierarchy.data.parents.items(3).toList()

        javaOnly { assertThat(parents[0].data.name).isEqualTo("Object") }
        kotlinOnly { assertThat(parents[0].data.name).isEqualTo("Any") }
        assertThat(parents[1].data.name).isEqualTo("Parent")
        assertThat(parents[2].data.name).isEqualTo("Foo")
    }

    @Test
    fun `Class with multiple parents has hierarchy with root object and parents`() {
        val page = """
            |abstract class A
            |abstract class B : A
            |abstract class C : B
            |class Foo : C
        """.render().page()

        val classlike = page.content<Classlike>()
        val parents = classlike.data.hierarchy.data.parents.items(5).toList()

        javaOnly { assertThat(parents[0].data.name).isEqualTo("Object") }
        kotlinOnly { assertThat(parents[0].data.name).isEqualTo("Any") }
        assertThat(parents[1].data.name).isEqualTo("A")
        assertThat(parents[2].data.name).isEqualTo("B")
        assertThat(parents[3].data.name).isEqualTo("C")
        assertThat(parents[4].data.name).isEqualTo("Foo")
    }

    @Test
    fun `Class signature appears with extends and implements`() {
        val page = """
            |interface A
            |abstract class B
            |class Foo : A, B
        """.render().page()

        val classlike = page.content<Classlike>()
        assertThat(classlike.data.signature.data.type).isEqualTo("class")
        assertThat(classlike.data.signature.data.extends.single().data.name).isEqualTo("B")
        assertThat(classlike.data.signature.data.implements.single().data.name).isEqualTo("A")
    }

    private fun DModule.page(): DevsitePage {
        val classlike = packages.single().classlikes.single { it.name() == "Foo" }
        val holder = runBlocking { DocumentablesHolder(this@page, this) }
        val converter = ClasslikeDocumentableConverter(language, classlike, pathProvider(), holder)
        return runBlocking { converter.classlike() }
    }

    private fun Classlike.symbolsFor(
        vararg types: String
    ) = data.symbolTypes.single { (summary, _) ->
        summary.title() in types
    }

    private fun SummaryList.constructor() =
        (data.items.item() as SingleColumnSummaryItem).data.description as SymbolSummary

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
