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
import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.Description
import com.google.devsite.components.Raw
import com.google.devsite.components.SummaryList
import com.google.devsite.components.impl.UndocumentedSymbolDescription
import com.google.devsite.components.testing.NoopContextFreeComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.title
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class DocTagConverterTest(
    private val language: Language
) : ConverterTestBase(language) {
    @Test
    fun `Empty description isn't documented`() {
        val description = """
            |class Foo
        """.render().description()

        assertThat(description.javaClass)
            .isAssignableTo(UndocumentedSymbolDescription::class.java)
    }

    @Test
    fun `Basic summary description has correct flags`() {
        val description = """
            |/** Hello World! */
            |class Foo
        """.render().description()

        assertThat(description.data.summary).isTrue()
        assertThat(description.data.deprecation).isNull()
    }

    @Ignore // TODO(b/166333285): support deprecation
    @Test
    fun `Deprecated summary description has correct flags`() {
        val description = """
            |@Deprecated("Bye")
            |class Foo
        """.render().description()

        assertThat(description.data.summary).isTrue()
        assertThat(description.data.deprecation).isEqualTo("Bye")
    }

    @Test
    fun `Full documentation has description`() {
        val documentation = """
            |/** Hello World! */
            |class Foo
        """.render().documentation()

        val description = documentation.item() as Description

        assertThat(description.data.summary).isFalse()
    }

    @Test
    fun `Full documentation has params`() {
        val documentation = """
            |/** @param a blah */
            |fun foo(a: Int)
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item().data.title as Raw

        assertThat(paramSummary.title()).isEqualTo("Parameters")
        assertThat(paramText.data.text).isEqualTo("a")
    }

    @Test
    fun `Full documentation has receiver param`() {
        val documentation = """
            |/** @receiver blah */
            |fun Int.foo()
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item().data.title as Raw

        assertThat(paramSummary.title()).isEqualTo("Parameters")
        assertThat(paramText.data.text).isEqualTo("receiver")
    }

    @Test
    fun `Full documentation has return type`() {
        val documentation = """
            |/** @return blah */
            |fun foo() = Unit
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val returns = paramSummary.item()

        assertThat(paramSummary.title()).isEqualTo("Returns")
        assertThat(returns.data.title).isSameInstanceAs(NoopContextFreeComponent)
    }

    @Test
    fun `Full documentation has thrown exceptions`() {
        val documentation = """
            |/** @throws IllegalStateException blah */
            |fun foo()
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item().data.title as Raw

        assertThat(paramSummary.title()).isEqualTo("Throws")
        assertThat(paramText.data.text).isEqualTo("IllegalStateException")
    }

    @Test
    fun `Full documentation has see alsos`() {
        val documentation = """
            |/** @see String blah */
            |fun foo()
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertThat(paramSummary.title()).isEqualTo("See also")
        assertThat(paramText.link().name).isEqualTo("String")
    }

    @Test
    fun `Full documentation sorts tags in pre-defined order`() {
        val documentation = """
            |/**
            | * @see String
            | * @param a
            | * @return
            | */
            |fun foo(a: String)
        """.render().documentation()

        val returnSummary = documentation[1] as SummaryList
        val paramSummary = documentation[2] as SummaryList
        val seeSummary = documentation[3] as SummaryList

        assertThat(returnSummary.title()).isEqualTo("Returns")
        assertThat(paramSummary.title()).isEqualTo("Parameters")
        assertThat(seeSummary.title()).isEqualTo("See also")
    }

    @Test
    fun `Full documentation sorts params in given order`() {
        val expected = listOf("a", "b", "c")
        val documentation = """
            |/**
            | * @param b
            | * @param c
            | * @param a
            | */
            |fun foo(a: String, b: String, c: String)
        """.render().documentation(paramNames = expected)

        val paramSummary = documentation.last() as SummaryList
        val params = paramSummary.items(3)

        for ((i, param) in params.withIndex()) {
            assertThat((param.data.title as Raw).data.text).isEqualTo(expected[i])
        }
    }

    private fun RootPageNode.description(): Description {
        val converter = DocTagConverter(language, pathProvider())
        return converter.summaryDescription(doc())
    }

    private fun RootPageNode.documentation(
        paramNames: List<String> = emptyList()
    ): List<ContextFreeComponent> {
        val converter = DocTagConverter(language, pathProvider())
        return converter.metadata(
            doc(),
            returnType = NoopContextFreeComponent,
            paramNames = paramNames
        )
    }

    private fun RootPageNode.doc(): Documentable {
        val packageDoc = children
            .filterIsInstance<PackagePageNode>().single()
            .documentable as DPackage

        return packageDoc.classlikes.singleOrNull() ?: packageDoc.functions.single()
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
