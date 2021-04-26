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
import com.google.devsite.components.Link
import com.google.devsite.components.symbols.Parameter
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolDetail.SymbolType.PROPERTY
import com.google.devsite.components.symbols.SymbolSignature
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.SymbolType
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.asType
import com.google.devsite.renderer.converters.testing.summary
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DModule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PropertyDocumentableConverterTest(
    private val language: Language
) : ConverterTestBase(language) {
    @Test
    fun `Property summary component creates return type link`() {
        val summary = """
            |class A
            |val foo: A
        """.render().summary()

        val returnType = summary.returnSummary().type

        assertThat(returnType.link().name).isEqualTo("A")
        assertPath(returnType.link().url, "androidx/example/A.html")
    }

    @Test
    fun `Property summary component creates signature with name`() {
        val summary = """
            |val iAmACoolProperty
        """.render().summary()

        val property = summary.summary()

        assertThat(property.name()).isEqualTo("iAmACoolProperty")
    }

    @Test
    fun `Property summaries include nullability information in 4x Kotlin and Java`() {
        val paramK = ("""
            |val foo: Int? = null
        """.render().summary().data.title as TypeSummary).data.type
        val paramJ = ("""
            |@Nullable public Integer foo = null
        """.render(java = true).summary().data.title as TypeSummary).data.type
        val paramJ2 = ("""
            |@Nullable
            |public Integer foo = null
        """.render(java = true).summary().data.title as TypeSummary).data.type

        for (param in listOf(paramK, paramJ, paramJ2)) {
            javaOnly { assertThat(param.data.annotations).isNotEmpty() }
            kotlinOnly {
                assertThat(param.data.annotations).isEmpty()
                assertThat(param.nullable).isTrue()
            }
        }
    }

    @Test
    fun `Property details include nullability information in 4x Kotlin and Java`() {
        val detailsK = """
            |val foo: Int? = null
        """.render().detail().data
        val detailsJ = """
            |@Nullable public Integer foo = null
        """.render(java = true).detail().data
        val detailsJ2 = """
            |@Nullable
            |public Integer foo = null
        """.render(java = true).detail().data

        for (details in listOf(detailsK, detailsJ, detailsJ2)) {
            kotlinOnly {
                assertThat(details.annotations).isEmpty()
                assertThat(details.returnType.data.primary.asType().data.nullable).isTrue()
            }
            javaOnly { assertThat(details.annotations).isNotEmpty() }
        }
    }

    @Test
    fun `Property summary component contains @property documentation`() {
        val summary = """
            |/** @property foo some_documentation */
            |val foo
        """.render().summary()

        val property = summary.summary()

        assertThat(property.data.description.text()).isEqualTo("some_documentation")
    }

    @Test
    fun `Property summary component has correct relative link`() {
        val summary = """
            |val <T : Number> List<T>.foo
        """.render().summary()

        val property = summary.summary()
        val signature = property.data.signature

        assertPath(
            signature.data.name.data.url,
            "androidx/example/package-summary.html#(kotlin.collections.List).foo()"
        )
    }

    @Test
    fun `Property detail component has correct name`() {
        val detail = """
            |val foo
        """.render().detail()

        assertThat(detail.data.name).isEqualTo("foo")
    }

    @Test
    fun `Property detail component is marked as property type`() {
        val detail = """
            |val foo
        """.render().detail()

        assertThat(detail.data.symbolType).isEqualTo(PROPERTY)
    }

    @Test
    fun `Property detail component creates return type link`() {
        val detail = """
            |class A
            |val foo: A
        """.render().detail()

        val returnType = detail.data.returnType

        assertThat(returnType.link().name).isEqualTo("A")
        assertPath(returnType.link().url, "androidx/example/A.html")
    }

    @Test
    fun `Property detail component has annotations`() {
        val detail = """
            |annotation class Hello
            |@Hello val foo: String
        """.render().detail()

        assertThat(detail.data.annotations).isNotEmpty()
    }

    @Test
    fun `Property detail component has nullability information`() {
        val detail = """
            |val foo: String? = null
        """.render().detail()

        javaOnly { assertThat(detail.data.annotations).isNotEmpty() }
        kotlinOnly {
            assertThat(detail.data.annotations).isEmpty()
            assertThat(detail.data.returnType.asType().data.nullable).isTrue()
        }
    }

    @Test
    fun `Property detail component has correct anchors`() {
        val detail = """
            |val <T : Number> List<T>.foo
        """.render().detail()

        assertThat(detail.data.anchors).containsExactly(
            "(kotlin.collections.List).foo()",
            "(kotlin.collections.List).getFoo()",
            "(kotlin.collections.List).setFoo()",
            "-kotlin.collections.List-.getFoo--",
            "-kotlin.collections.List-.setFoo--"
        )
    }

    private fun DModule.summary(
        hints: ModifierHints = ModifierHints(language)
    ): TwoPaneSummaryItem {
        val holder = runBlocking { DocumentablesHolder(this@summary, this) }
        val docConverter = DocTagConverter(language, pathProvider(), holder)
        val converter = PropertyDocumentableConverter(language, pathProvider(), docConverter)
        return converter.summary(property()!!, hints)
    }

    private fun DModule.detail(
        hints: ModifierHints = ModifierHints(language)
    ): SymbolDetail {
        val holder = runBlocking { DocumentablesHolder(this@detail, this) }
        val docConverter = DocTagConverter(language, pathProvider(), holder)
        val converter = PropertyDocumentableConverter(language, pathProvider(), docConverter)
        return converter.detail(property()!!, hints)
    }

    private fun DModule.signature(
        hints: ModifierHints = ModifierHints(language)
    ): SymbolSignature {
        val holder = runBlocking { DocumentablesHolder(this@signature, this) }
        val docConverter = DocTagConverter(language, pathProvider(), holder)
        val converter = PropertyDocumentableConverter(language, pathProvider(), docConverter)
        return converter.summary(property()!!, hints).signature()
    }

    private fun Parameter.link(): Link.Params = (data.primary as SymbolType).link()
    private fun SymbolType.link(): Link.Params = data.type.data
    private fun TwoPaneSummaryItem.returnSummary(): TypeSummary.Params =
        (data.title as TypeSummary).data
    private fun TwoPaneSummaryItem.signature() = (data.description as SymbolSummary).data.signature

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
