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
import com.google.devsite.components.impl.DefaultDescription
import com.google.devsite.components.impl.UndocumentedSymbolDescription
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.testing.NoopContextFreeComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.title
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.Img
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.properties.WithExtraProperties
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

    @Test
    fun `Deprecated summary description has correct flags`() {
        val description = """
            |@Deprecated("Bye")
            |class Foo
        """.render().description()

        assertThat(description.data.summary).isTrue()
        assertThat(description.data.deprecation).isEqualTo("This class is deprecated.")
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
    fun `Full documentation has img tag`() {
        val documentation = """
            |/**
            | ![Alt text](/path/to/img.jpg)
            |*/
            |fun foo(a: Int)
        """.render().documentation()

        val description = documentation.last() as DefaultDescription
        val img = description.data.root.children.first().children.item() as Img

        assertThat(img.params["href"]).isEqualTo("/path/to/img.jpg")
        assertThat(img.params["alt"]).isEqualTo("Alt text")
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
    fun `@deprecated description works over multiple lines`() {
        val documentation = """
            |/**
            | * Return the target fragment set by {@link #setTargetFragment}.
            | *
            | * @deprecated Instead of using a target fragment to pass results, use
            | * {@link androidx.fragment.app.FragmentManager#setFragmentResult(java.lang.String,android.os.Bundle) FragmentManager#setFragmentResult(String, Bundle)} to deliver results to
            | * {@link androidx.fragment.app.FragmentResultListener FragmentResultListener} instances registered by other fragments via
            | * {@link androidx.fragment.app.FragmentManager#setFragmentResultListener(java.lang.String,androidx.lifecycle.LifecycleOwner,androidx.fragment.app.FragmentResultListener) FragmentManager#setFragmentResultListener(String, LifecycleOwner,
            | * LastLine)}.
            | */
            | @Deprecated
            |public void foo(){}
        """.render(java = true)
        val doc = documentation.documentation(doc = ::classFunctionDoc)
        val function = doc.first() as DefaultDescription
        assertThat(function.data.deprecation).isNotNull()
        // Checking the root for LastLine is somewhat testing Dokka
        // but this was broken in a previous version
        assertThat(function.data.root.toString()).contains("LastLine")
        // TODO (b/171570474)
        assertThat(function.data.root.toString()).contains("Insteadof")
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

    @Ignore("b/170397127")
    @Test
    fun `Throws table is present and correct`() {
        val documentation = """
            |/**
            | * a normal comment
            | *
            | * @throws java.lang.IllegalStateException if the Dialog has not yet been created (before
            | * onCreateDialog ) or has been destroyed (after onDestroyView .
            | * @see toString
            | */
            |fun foo()
        """.render().documentation()

        val throwsTableSummary = documentation.first { (it as? SummaryList)?.title() == "Throws" }
            as SummaryList
        val throwsLeftColumn = throwsTableSummary.item().data.title as Raw
        val throwsRightColumnTop = throwsTableSummary.item().data.description as Description
        val throwsRightColumnText = (throwsRightColumnTop.data.root.children.first() as Text)

        assertThat(throwsTableSummary.title()).isEqualTo("Throws")
        assertThat(throwsLeftColumn.data.text).isEqualTo("java.lang.IllegalStateException")
        assertThat(throwsRightColumnText.body).isEqualTo(
            "if the Dialog has not yet been created (before onCreateDialog ) or has " +
                "been destroyed (after onDestroyView ."
        )
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
    fun `See also parses external link`() {
        val documentation = """
            |/** @see String */
            |class Foo
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "kotlin/String.html")
    }

    @Test
    fun `See also parses internal link`() {
        val documentation = """
            |/** @see Bar */
            |class Foo { class Bar }
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "androidx/example/Foo.Bar.html")
    }

    @Test
    fun `See also parses link with Kotlin style function`() {
        val documentation = """
            |/** @see String.isEmpty */
            |class Foo
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        // TODO(b/167437580): figure out how to reliably parse links
        assertThat(paramText.link().url).isEmpty()
    }

    @Test
    fun `See also parses link with Java style function`() {
        val documentation = """
            |/** @see String#isEmpty() */
            |public void foo() {}
        """.render(java = true).documentation(doc = ::classFunctionDoc)

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "java/lang/String.html#isEmpty()")
    }

    @Test
    fun `See also parses link with unresolved function`() {
        val documentation = """
            |/** @see com.example.foo.Foo#bar() */
            |public void foo() {}
        """.render(java = true).documentation(doc = ::classFunctionDoc)

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "com/example/foo/Foo.html#bar()")
    }

    @Test
    fun `See also parses link with class`() {
        val documentation = """
            |/** @see String */
            |public void foo() {}
        """.render(java = true).documentation(doc = ::classFunctionDoc)

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "java/lang/String.html")
    }

    @Test
    fun `See also parses link with unresolved class`() {
        val documentation = """
            |/** @see com.example.foo.Foo */
            |public void foo() {}
        """.render(java = true).documentation(doc = ::classFunctionDoc)

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "com/example/foo/Foo.html")
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

    private fun DModule.description(): Description {
        val converter = DocTagConverter(language, pathProvider())
        val doc = smartDoc(this)
        val annotations = (doc as? WithExtraProperties<*>)?.annotations().orEmpty()
        return converter.summaryDescription(doc, annotations)
    }

    private fun DModule.documentation(
        doc: DModule.() -> Documentable = ::smartDoc,
        paramNames: List<String> = emptyList()
    ): List<ContextFreeComponent> {
        val converter = DocTagConverter(language, pathProvider())
        return converter.metadata(
            doc(),
            returnType = NoopContextFreeComponent,
            paramNames = paramNames
        )
    }

    private fun smartDoc(module: DModule): Documentable {
        val packageDoc = module.packages.single()

        return packageDoc.classlikes.singleOrNull() ?: packageDoc.functions.single()
    }

    private fun classFunctionDoc(module: DModule): Documentable {
        return module.packages.single().classlikes.single().functions.single()
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
