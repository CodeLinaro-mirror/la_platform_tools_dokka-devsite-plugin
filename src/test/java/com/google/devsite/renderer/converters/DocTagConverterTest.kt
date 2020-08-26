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
import com.google.devsite.components.Description
import com.google.devsite.components.Link
import com.google.devsite.components.Raw
import com.google.devsite.components.SummaryList
import com.google.devsite.components.TableTitle
import com.google.devsite.components.TwoPaneSummaryItem
import com.google.devsite.components.impl.UndocumentedSymbolDescription
import com.google.devsite.components.testing.NoopContextFreeComponent
import com.google.devsite.renderer.Language
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
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val description = converter.summaryDescription(root.doc())

            assertThat(description.javaClass)
                .isAssignableTo(UndocumentedSymbolDescription::class.java)
        }
    }

    @Test
    fun `Basic summary description has correct flags`() {
        val source = """
            |/** Hello World! */
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val description = converter.summaryDescription(root.doc())

            assertThat(description.data.summary).isTrue()
            assertThat(description.data.deprecation).isNull()
        }
    }

    @Ignore // TODO(b/166333285): support deprecation
    @Test
    fun `Deprecated summary description has correct flags`() {
        val source = """
            |@Deprecated("Bye")
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val description = converter.summaryDescription(root.doc())

            assertThat(description.data.summary).isTrue()
            assertThat(description.data.deprecation).isEqualTo("Bye")
        }
    }

    @Test
    fun `Full documentation has description`() {
        val source = """
            |/** Hello World! */
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val documentation = converter.metadata(root.doc())
            assertThat(documentation).hasSize(1)

            val description = documentation.single() as Description
            assertThat(description.data.summary).isFalse()
        }
    }

    @Test
    fun `Full documentation has params`() {
        val source = """
            |/** @param a blah */
            |fun foo(a: Int)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val documentation = converter.metadata(root.doc())
            val paramSummary = documentation.last() as SummaryList
            val header = paramSummary.data.header as TableTitle
            val params = paramSummary.data.items.map { it as TwoPaneSummaryItem }

            assertThat(header.data.title).isEqualTo("Parameters")
            assertThat(params).hasSize(1)
            assertThat((params.single().data.title as Raw).data.text).isEqualTo("a")
        }
    }

    @Test
    fun `Full documentation has receiver param`() {
        val source = """
            |/** @receiver blah */
            |fun Int.foo()
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val documentation = converter.metadata(root.doc())
            val paramSummary = documentation.last() as SummaryList
            val header = paramSummary.data.header as TableTitle
            val params = paramSummary.data.items.map { it as TwoPaneSummaryItem }

            assertThat(header.data.title).isEqualTo("Parameters")
            assertThat(params).hasSize(1)
            assertThat((params.single().data.title as Raw).data.text).isEqualTo("receiver")
        }
    }

    @Test
    fun `Full documentation has return type`() {
        val source = """
            |/** @return blah */
            |fun foo() = Unit
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val documentation =
                converter.metadata(root.doc(), returnType = NoopContextFreeComponent)
            val paramSummary = documentation.last() as SummaryList
            val header = paramSummary.data.header as TableTitle
            val returns = paramSummary.data.items.map { it as TwoPaneSummaryItem }

            assertThat(header.data.title).isEqualTo("Returns")
            assertThat(returns).hasSize(1)
            assertThat(returns.single().data.title).isSameInstanceAs(NoopContextFreeComponent)
        }
    }

    @Test
    fun `Full documentation has thrown exceptions`() {
        val source = """
            |/** @throws IllegalStateException blah */
            |fun foo()
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val documentation = converter.metadata(root.doc())
            val paramSummary = documentation.last() as SummaryList
            val header = paramSummary.data.header as TableTitle
            val params = paramSummary.data.items.map { it as TwoPaneSummaryItem }

            assertThat(header.data.title).isEqualTo("Throws")
            assertThat(params).hasSize(1)
            assertThat((params.single().data.title as Raw).data.text)
                .isEqualTo("IllegalStateException")
        }
    }

    @Test
    fun `Full documentation has see alsos`() {
        val source = """
            |/** @see String blah */
            |fun foo()
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val documentation = converter.metadata(root.doc())
            val paramSummary = documentation.last() as SummaryList
            val header = paramSummary.data.header as TableTitle
            val params = paramSummary.data.items.map { it as TwoPaneSummaryItem }

            assertThat(header.data.title).isEqualTo("See also")
            assertThat(params).hasSize(1)
            assertThat((params.single().data.title as Link).data.name).isEqualTo("String")
        }
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
