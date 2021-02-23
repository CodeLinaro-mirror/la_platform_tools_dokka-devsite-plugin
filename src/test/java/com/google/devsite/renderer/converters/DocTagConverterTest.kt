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
import com.google.devsite.components.Link
import com.google.devsite.components.Raw
import com.google.devsite.components.impl.DefaultDescription
import com.google.devsite.components.impl.UndocumentedSymbolDescription
import com.google.devsite.components.symbols.Parameter
import com.google.devsite.components.symbols.TypeParameter
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.testing.NoopContextFreeComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.asType
import com.google.devsite.renderer.converters.testing.description
import com.google.devsite.renderer.converters.testing.generics
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.projectionName
import com.google.devsite.renderer.converters.testing.single
import com.google.devsite.renderer.converters.testing.size
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.renderer.converters.testing.title
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.Img
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertFails

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

    @Test // NOTE: upstream dokka does not support @param <Baz> documentation style in kotlin
    fun `Class type parameters can be documented with @param with or without angle brackets`() {
        val documentationK = """
            |/**
            | * Hello World!
            | * @param Bar A type of bar
            | * @param Baz Bazzy baz
            | */
            |class <Bar: String, Baz> Foo: List<Bar>
        """.render().documentation(doc = { this.clazz() })
        val documentationJ = """
            |/**
            | * Hello World!
            | * @param Bar A type of bar
            | * @param <Baz> Bazzy baz
            | */
            |public class Foo<Bar extends String, Baz> extends List<Bar> {
            |}
        """.render(java = true).documentation(doc = { this.clazz() })

        for (documentation in listOf(documentationK, documentationJ)) {
            val classParams = documentation.first {
                (it as? SummaryList)?.title() == "Parameters" } as SummaryList

            assertThat(classParams.size()).isEqualTo(2)
            val barParam = classParams.items().first().data
            val bazParam = classParams.items().last().data
            val barTypeParam = barParam.title as TypeParameter
            assertThat(barTypeParam.data.name).isEqualTo("Bar")
            assertThat(barTypeParam.projectionName()).isEqualTo("String")
            assertThat((barParam.description as Description).text()).isEqualTo("A type of bar")
            val bazTypeParam = bazParam.title as TypeParameter
            assertThat(bazTypeParam.data.name).isEqualTo("Baz")
            assertThat((bazParam.description as Description).text()).isEqualTo("Bazzy baz")
        }
    }

    @Test
    fun `Class property parameters can be documented with @param on the class`() {
        val module = """
            |/**
            | * Hello World!
            | * @param bar A vary bary name
            | */
            |class Foo(val bar: String) {
            |
            |}
        """.render()

        val propertyDoc = module.documentation(doc = { this.packages.single()
            .classlikes.single().properties.single() }).single() as Description
        val constructorDoc = module.documentation(doc = { (this.packages.single()
            .classlikes.single() as DClass).constructors.single() }).last() as SummaryList

        assertThat(propertyDoc.text()).isEqualTo("A vary bary name")

        assertThat(constructorDoc.title()).isEqualTo("Parameters")
        assertThat(constructorDoc.single().name()).isEqualTo("bar")
        assertThat(constructorDoc.single().description().text()).isEqualTo("A vary bary name")
    }

    @Test
    fun `Interface property parameters can be documented with @property on the class`() {
        val module = """
            |/**
            | * Hello World!
            | * @property bar A vary bary name
            | */
            |interface Foo(val bar: String)
        """.render()

        val propertyDoc = module.documentation(doc = { this.packages.single()
            .classlikes.single().properties.single() }).single() as Description
        // Upstream dokka does not propagate @property documentation on property parameters to the
        // constructor. This may or may not be what we want.
        assertFails {
            val constructorDoc = module.documentation(doc = { (this.packages.single()
                .classlikes.single() as DClass).constructors.single() }).single() as SummaryList
        }
        assertThat(propertyDoc.text()).isEqualTo("A vary bary name")
    }

    @Test
    fun `@constructor is ignored`() {
        val withAnnotation = """
            |/**
            | * The amount by which the text is shifted up or down from current the baseline.
            | * @constructor
            | */
            |class BaselineShift(val multiplier: Float) {}
        """.render()

        val withoutAnnotation = """
            |/**
            | * The amount by which the text is shifted up or down from current the baseline.
            | */
            |class BaselineShift(val multiplier: Float) {}
        """.render()

        // TODO(b/180525239) Implement @constructor and fix this test

        val withAnnotationDoc = withAnnotation.documentation(doc = { this.packages.single()
            .classlikes.single() }).single() as Description

        val withoutAnnotationDoc = withoutAnnotation.documentation(doc = { this.packages.single()
            .classlikes.single() }).single() as Description

        assertThat(withoutAnnotationDoc.text()).isEqualTo(withAnnotationDoc.text())
    }

    @Test
    fun `Class property parameters can be documented with @property on the class`() {
        val module = """
            |/**
            | * Hello World!
            | * @property bar A vary bary name
            | */
            |class Foo(val bar: String) {
            |
            |}
        """.render()

        val propertyDoc = module.documentation(doc = { this.packages.single()
            .classlikes.single().properties.single() }).single() as Description
        // Upstream dokka does not propagate @property documentation on property parameters to the
        // constructor. This may or may not be what we want.
        assertFails {
            val constructorDoc = module.documentation(doc = { (this.packages.single()
                    .classlikes.single() as DClass).constructors.single() }).single() as SummaryList
        }
        assertThat(propertyDoc.text()).isEqualTo("A vary bary name")
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
        val paramParam = paramSummary.item().data.title as Parameter

        assertThat(paramSummary.title()).isEqualTo("Parameters")
        assertThat(paramParam.data.name).isEqualTo("a")
    }

    @Test
    fun `Full documentation has properties`() {
        val description = """
            |/** @property bar a barber */
            |val bar: String = "barbarbar
        """.render().documentation(doc = { this.packages.single()
            .properties.single() }).single() as Description

        assertThat(description.text()).isEqualTo("a barber")
    }

    @Test
    fun `Full class documentation has properties`() {
        val description = """
            |class Foo {
            |   /** @property bar a barber */
            |   val bar: String = "barbarbar
            |}
        """.render().documentation(doc = { this.packages.single().classlikes.single()
            .properties.single() }).single() as Description

        assertThat(description.text()).isEqualTo("a barber")
    }

    @Test
    fun `@param throws exception for invalid parameter`() {
        assertFails {
            """
            |/**
            | * @param NOT_A_REAL_PARAM aaaaaa
            | */
            |fun foo()
            """.render().documentation()
        }
        assertFails { // for type params and property params
            """
            |/**
            | * @param NOT_A_REAL_PARAM aaaaaa
            | */
            |class Foo { }
            """.render().documentation()
        }
        assertFails { // for @param in the wrong place
            """
            |/**
            | * @param NOT_A_REAL_PARAM aaaaaa
            | */
            |val foo = "bbb"
            """.render().documentation()
        }
    }

    @Test
    fun `@property throws exception for invalid property`() {
        assertFails { // @property on a parameter that is not a property
            """
            |/**
            | * @property NOT_A_REAL_PROPERTY aaaaaa
            | */
            |class Foo(NOT_A_REAL_PROPERTY: String) { }
            """.render().documentation()
        }
        assertFails { // there is no corresponding property
            """
            |/**
            | * @property NO_PROPERTIES_HERE aaaaaa
            | */
            |class Foo() { }
            """.render().documentation()
        }
        assertFails { // can't @property on a function
            """
            |class Foo() {
            |   /**
            |    * @property NO_PROPERTIES_HERE aaaaaa
            |    */
            |   fun doAThing()
            |}
            """.render().documentation()
        }
        assertFails { // can't @property on a function
            """
            |/**
            | * @property NO_PROPERTIES_HERE aaaaaa
            | */
            |fun doAThing()
            """.render().documentation()
        }
        assertFails { // @property must be on correct property
            """
            |/** @property a
            |val b
            |val a
            """.render().documentation()
        }
    }

    @Test // TODO: java parameters do not have annotations upstream: b/175612102
    fun `Full documentation parameters table has types`() {
        val documentationK = """
            |/**
            | * @param T a type
            | */
            |    interface PagedListListener<T : Any> {
            |/**
            | * Called after the current PagedList has been updated.
            | *
            | * @param previousList The previous list, may be null.
            | * @param currentList The new current list, may be null.
            | */
            |fun onCurrentListChanged(
            |    @Suppress("DEPRECATION") previousList: List<T>?,
            |    @Suppress("DEPRECATION") currentList: List<T>?
            |)
            |}
        """.render().documentation()
        val documentationJ = """
            |/**
            | * @param T a type
            | */
            |    interface PagedListListener<T extends Object> {
            |/**
            | * Called after the current PagedList has been updated.
            | *
            | * @param previousList The previous list, may be null.
            | * @param currentList The new current list, may be null.
            | */
            |void onCurrentListChanged(
            |    @Suppress("DEPRECATION") @Nullable List<T> previousList,
            |    @Suppress("DEPRECATION") @Nullable List<T> currentList
            |)
            |}
        """.render(java = true).documentation()
        for (documentation in listOf(documentationK/*, documentationJ*/)) {
            val paramTable = documentation.first { (it as? SummaryList)?.title() == "Parameters" }
                as SummaryList
            assertThat(paramTable.size()).isEqualTo(2)
            val param0 = paramTable.items().first()
            val param0Left = (param0.data.title as Parameter)
            val param1 = paramTable.items().last()
            val param1Left = (param1.data.title as Parameter)

            assertThat(param0.name()).isEqualTo("previousList")
            assertThat(param0Left.link().name).isEqualTo("List")
            assertThat(param0Left.generics().single().link().name).isEqualTo("T")
            assertThat(param0.description().text()).isEqualTo("The previous list, may be null.")
            assertThat(param1.name()).isEqualTo("currentList")
            assertThat(param1Left.link().name).isEqualTo("List")
            assertThat(param1Left.generics().single().link().name).isEqualTo("T")
            assertThat(param1.description().text()).isEqualTo("The new current list, may be null.")
            javaOnly {
                assertThat(param0Left.data.annotations.single().link().name).isEqualTo("Nullable")
                assertThat(param1Left.data.annotations.single().link().name).isEqualTo("Nullable")
            }
            kotlinOnly {
                assertThat(param0Left.data.primary.asType().data.nullable).isEqualTo(true)
                assertThat(param1Left.data.primary.asType().data.nullable).isEqualTo(true)
            }
        }
    }

    @Test
    fun `Parameter documentation works on generic and lambda types`() {
        val documentation = """
            |abstract class Factory<Key : Any, Value : Any> {
            |    /**
            |     * Applies the given function to each value emitted by DataSources produced by this Factory.
            |     *
            |     * Same as mapByPage, but operates on individual items.
            |     *
            |     * @param function Function that runs on each loaded item, returning items of a potentially
            |     * new type.
            |     * @param ToValue Type of items produced by the new DataSource, from the passed function.
            |     * @return A new [Factory], which transforms items using the given function.
            |     *
            |     * @see mapByPage
            |     * @see DataSource.map
            |     * @see DataSource.mapByPage
            |     */
            |    open fun <ToValue : String> map(function: (Value) -> ToValue): Factory<Key, ToValue>
            |}
        """.render().documentation()
        val paramTable = documentation.first {
            (it as? SummaryList)?.title() == "Parameters" } as SummaryList

        // We don't want Value and/or Key to appear listed as parameters for map().
        assertThat(paramTable.size()).isEqualTo(2)

        val param0 = paramTable.items().first()
        val param0Left = param0.data.title as Parameter
        val param1 = paramTable.items().last()
        val param1Left = param1.data.title as TypeParameter

        assertThat(param0Left.data.name).isEqualTo("function")
        javaOnly {
            assertThat(param0Left.link().name).isEqualTo("Function1")
            val param0LambdaTypes = param0Left.generics().map { it.link().name }
            assertThat(param0LambdaTypes).isEqualTo(listOf("Value", "ToValue"))
        }
        kotlinOnly {
            assertThat(param0Left.data.isLambda).isTrue()
            assertThat(param0Left.data.receiver).isNull()
            assertThat(param0Left.data.lambdaModifiers).isEmpty()
            // The evaluation type of the lambda is ToValue
            assertThat(param0Left.link().name).isEqualTo("ToValue")
            val param0LambdaArgumentType = param0Left.data.lambdaParams.map { it.link().name }
            assertThat(param0LambdaArgumentType).isEqualTo(listOf("Value"))
        }
        assertThat(param0.description().text()).isEqualTo("Function that runs on each " +
            "loaded item, returning items of a potentially new type.")
        assertThat(param1Left.data.name).isEqualTo("ToValue")
        assertThat(param1Left.projectionName()).isEqualTo("String")
        assertThat(param1.description().text()).isEqualTo("Type of items produced by the " +
            "new DataSource, from the passed function.")

        val seeAlsoTable = documentation.first {
            (it as? SummaryList)?.title() == "See also" } as SummaryList
        assertThat(seeAlsoTable.size()).isEqualTo(3)
        assertThat(seeAlsoTable.items().map { (it.data.title as Link).data.name })
            .isEqualTo(listOf("mapByPage", "DataSource.map", "DataSource.mapByPage"))
}

    @Test
    fun `Copied from PagingData`() {
        val documentation = """
                |/**
                | * Returns a [PagingData] containing only elements matching the given [predicate]
                | *
                | * @see filter
                | */
                |@JvmName("filter")
                |@CheckResult
                |fun filterSync(predicate: (T) -> Boolean): PagingData<T> = transform { event ->
                |    event.filter { predicate(it) }
                |}
        """.render().documentation()
        val seeAlsoTable = documentation.first {
            (it as? SummaryList)?.title() == "See also" } as SummaryList
        assertThat((seeAlsoTable.single().data.title as Link).data.name).isEqualTo("filter")
    }

    @Test
    fun `Documentation spacing works over multiple lines`() {
        val documentationK = """
            |/**
            | * This is a multi-line documentation string. There is no space at the end of the
            | * first line, but "the first" with no space should not appear in the final documentation.
            | */
            | fun foo(): String
        """.render().documentation().first() as Description
        val documentationJ = """
            |/**
            | * This is a multi-line documentation string. There is no space at the end of the
            | * first line, but "the first" with no space should not appear in the final documentation.
            | */
            | public String foo()
        """.render(java = true).documentation().first() as Description

        for (documentation in listOf(documentationK, documentationJ)) {
            assertThat("thefirst" in documentation.data.root.toString()).isFalse()
        }
    }

    @Test
    fun `Multiline doc from fragment, with formatting`() {
        val module = """
    |/**
    | * Instantiates a Fragment's view.
    | *
    | * @param parent The parent that the created view will be placed
    | * in; <em>note that this may be null</em>.
    | * @param name Tag name to be inflated.
    | * @param context The context the view is being created in.
    | * @param attrs Inflation attributes as specified in XML file.
    | *
    | * @return view the newly created view
    | */
    |@Nullable
    |public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context,
    |                         @NonNull AttributeSet attrs) {
    |    return mHost.mFragmentManager.getLayoutInflaterFactory()
    |            .onCreateView(parent, name, context, attrs);
    |}
        """.render(java = true)
        val paramDoc = (module.documentation(doc = {
            this.function()!!.parameters.single { it.name == "parent" }
        }).first() as Description).data.root

        assertThat("placedin" in paramDoc.toString()).isFalse()
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
        val doc = documentation.documentation()
        val function = doc.first() as DefaultDescription
        assertThat(function.data.deprecation).isNotNull()
        // Checking the root for LastLine is somewhat testing Dokka
        // but this was broken in a previous version
        assertThat(function.data.root.toString()).contains("LastLine")
        assertThat(function.data.root.toString()).contains("Instead of using")
        assertThat(function.data.root.children.single().children.size).isEqualTo(7)
    }

    @Test
    fun `Full documentation has receiver param`() {
        val documentation = """
            |/** @receiver blah */
            |fun Int.foo()
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramParam = (paramSummary.item().data.title as Parameter).data

        assertThat(paramSummary.title()).isEqualTo("Parameters")
        javaOnly {
            assertThat(paramParam.name).isEqualTo("receiver")
        }
        kotlinOnly {
            assertThat(paramParam.name).isEqualTo("")
        }
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
    fun `Full Kotlin documentation has thrown exceptions`() {
        val documentation = """
            |/** @throws IllegalStateException if it fails */
            |fun foo()
        """.render().documentation()

        val throwsSummary = documentation.first { (it as? SummaryList)?.title() == "Throws" }
            as SummaryList
        val throwsLeft = throwsSummary.item().data.title as Raw
        val throwsRight = ((throwsSummary.item().data.description as Description)
            .data.root.children.first().children.first() as Text)

        assertThat(throwsLeft.data.text).contains("IllegalStateException")
        assertThat(throwsRight.body).isEqualTo("if it fails")
    }

    @Test
    fun `Full Java documentation has thrown exceptions`() {
        val documentation = """
            |/** @throws IllegalStateException if it fails */
            |public void foo() {}
        """.render(java = true).documentation()

        val throwsSummary = documentation.first { (it as? SummaryList)?.title() == "Throws" }
            as SummaryList
        val throwsLeft = throwsSummary.item().data.title as Raw
        val throwsRight = ((throwsSummary.item().data.description as Description)
            .data.root.children.first().children.first() as Text)

        assertThat(throwsLeft.data.text).isEqualTo("java.lang.IllegalStateException")
        assertThat(throwsRight.body).isEqualTo("if it fails")
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
        assertThat(paramText.link().url).contains("isEmpty")
    }

    @Test
    fun `See also parses link with Java style function`() {
        val documentation = """
            |/** @see String#isEmpty() */
            |public void foo() {}
        """.render(java = true).documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "java/lang/String.html#isEmpty()")
    }

    @Test
    fun `See also parses link with unresolved function`() {
        val documentation = """
            |/** @see com.example.foo.Foo#bar() */
            |public void foo() {}
        """.render(java = true).documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "com/example/foo/Foo.html#bar()")
    }

    @Test
    fun `See also parses link with class`() {
        val documentation = """
            |/** @see String */
            |public void foo() {}
        """.render(java = true).documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "java/lang/String.html")
    }

    @Test
    fun `See also parses link with unresolved class`() {
        val documentation = """
            |/** @see com.example.foo.Foo */
            |public void foo() {}
        """.render(java = true).documentation()

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
            assertThat((param.data.title as Parameter).data.name).isEqualTo(expected[i])
        }
    }

    private fun DModule.description(): Description {
        val holder = runBlocking { DocumentablesHolder(this@description, this) }
        val converter = DocTagConverter(language, pathProvider(), holder)
        val doc = smartDoc(this)
        val annotations = (doc as? WithExtraProperties<*>)?.annotations().orEmpty()
        return converter.summaryDescription(doc, annotations)
    }

    private fun DModule.documentation(
        doc: DModule.() -> Documentable = ::smartDoc,
        paramNames: List<String> = emptyList()
    ): List<ContextFreeComponent> {
        val holder = runBlocking { DocumentablesHolder(this@documentation, this) }
        val converter = DocTagConverter(language, pathProvider(), holder)
        return converter.metadata(
            doc(),
            returnType = NoopContextFreeComponent,
            paramNames = paramNames
        )
    }

    private fun DModule.clazz(): DClass {
        val topClass = this.classlike()!!
        if (topClass.classlikes.isNotEmpty()) return topClass.classlikes.single() as DClass
        return topClass as DClass
    }

    private fun smartDoc(module: DModule): Documentable {
        return module.function() ?: module.classlike()!!
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
