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
import java.io.ByteArrayOutputStream
import java.io.PrintStream
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

    @Suppress("unused") // TODO: fix deprecated class details b/183420241
    @Test
    fun `Deprecated class summary and detail description flags correct in 4x Kotlin and Java`() {
        val codeK = """
            |/**
            | * class_description
            | */
            |@Deprecated("Bye")
            |class Foo
        """.render()
        val summarykK = codeK.description()
        val detailsK = codeK.documentation()
        val codeJ = """
            |/**
            | * class_description
            | * @deprecated Bye
            | */
            |@Deprecated
            |public class Foo {}
        """.render(java = true)
        val summaryJ = codeJ.description { this.clazz() }
        val detailsJ = codeJ.documentation(doc = { this.clazz() })
        for (summary in listOf(summarykK, summaryJ)) {
            assertThat(summary.data.summary).isTrue()
            assertThat(summary.text()).isEqualTo("Bye")
            assertThat(summary.data.deprecation).isEqualTo("This class is deprecated.")
        }
        /* TODO: fix deprecated class details b/183420241
        for (details in listOf(detailsK, detailsJ)) {
            val detail = (details.single() as Description)
            assertThat(detail.data.summary).isFalse()
            assertThat(detail.text()).isEqualTo("Bye")
            assertThat(detail.data.deprecation).isEqualTo("This class is deprecated.")
        }*/
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
    fun `Property parameter docs propagate correctly`() {
        // Property parameters are kotlin-exclusive
        val module = """
            |/**
            | * Class docs
            | * @property bar AtPropertyParameter docs
            | * @param bar AtParameterProperty docs
            | */
            |class Foo(val bar: String)
        """.render()

        val propDoc = module.documentation({ this.property()!! }).single() as Description
        val classDoc = module.documentation({ this.clazz() }).single() as Description
        val constructorDoc = module.documentation({ this.constructor() })
        val conParamDoc = module.documentation({ this.constructor().parameters.single() }).single()

        // An odd propagation system, but it seems to work out to properly document everything?
        assertThat((propDoc).text()).isEqualTo("AtPropertyParameter docs")
        assertThat((classDoc).text()).isEqualTo("Class docs")
        assertThat(constructorDoc.size).isEqualTo(2)
        assertThat(constructorDoc.first()).isInstanceOf(UndocumentedSymbolDescription::class.java)
        assertThat((constructorDoc.last() as SummaryList).title()).isEqualTo("Parameters")
        assertThat((constructorDoc.last() as SummaryList).single().name()).isEqualTo("bar")
        assertThat((constructorDoc.last() as SummaryList).single().description().text())
            .isEqualTo("AtParameterProperty docs")
        assertThat((conParamDoc as Description).text()).isEqualTo("AtPropertyParameter docs")
    }

    @Test
    fun `@constructor docs are applied`() {
        val withAnnotation = """
            |/**
            | * The amount by which the text is shifted up or down from current the baseline.
            | * @constructor Primary constructor docs
            | */
            |class BaselineShift(val multiplier: Float) {
            |   /** Secondary constructor docs */
            |   constructor(multiplier: Int) : this(multiplier)
            |
            |}
        """.render()

        val constructorDoc1 = withAnnotation.documentation({ this.constructors().first() })
            .single() as Description
        val constructorDoc2 = withAnnotation.documentation({ this.constructors().last() })
            .single() as Description

        assertThat(constructorDoc1.text()).isEqualTo("Secondary constructor docs")
        assertThat(constructorDoc2.text()).isEqualTo("Primary constructor docs")
    }

    @Test
    fun `Class property parameters can be documented with @property on the class`() {
        val module = """
            |/**
            | * Hello World!
            | * @param baz Buzzbuzzbuzz
            | * @property bar A vary bary name
            | */
            |class Foo(val bar: String) {
            |
            |}
        """.render()

        val propertyDoc = module.documentation({ this.property("bar")!! })
        // Upstream dokka does not propagates @param documentation on property parameters to the
        // property. We think this is what we want.
        assertThat((propertyDoc.single() as Description).text()).isEqualTo("A vary bary name")
        // Upstream dokka does not propagate @property documentation on property parameters to the
        // constructor. We think this is what we want.
        assertFails { val constructorDoc = module.documentation({ this.constructor() }) }
    }

    @Test // TODO(b/182457595): fix img tags in javadoc
    fun `Full documentation has img tag in 4x Kotlin and Java`() {
        val documentationK = """
            |/**
            | ![Alt text](/path/to/img.jpg)
            |*/
            |fun foo(a: Int)
        """.render().documentation()
        val documentationJ = """
            |/**
            | * <img src="/path/to/img.jpg" alt="Alt text"/>
            | */
            |public fun foo(Integer a)
        """.render(java = true).documentation()

        for (documentation in listOf(/*documentationJ,*/ documentationK)) {
            val description = documentation.last() as DefaultDescription
            val img = description.data.components.first().children.item() as Img

            assertThat(img.params["href"]).isEqualTo("/path/to/img.jpg")
            assertThat(img.params["alt"]).isEqualTo("Alt text")
        }
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
        """.render().documentation(doc = {
            this.packages.single()
                .properties.single()
        }).single() as Description

        assertThat(description.text()).isEqualTo("a barber")
    }

    @Test
    fun `Full class documentation has properties`() {
        val description = """
            |class Foo {
            |   /** @property bar a barber */
            |   val bar: String = "barbarbar
            |}
        """.render().documentation(doc = {
            this.packages.single().classlikes.single()
                .properties.single()
        }).single() as Description

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
        val standardOut = System.out
        val outputStreamCaptor = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStreamCaptor))
        """
        |/**
        | * @param NOT_A_REAL_PARAM aaaaaa
        | */
        |class Foo { }
        """.render().documentation() // for type params and property params
        val expected = "WARNING: unable to find what is referred to by" +
            "\n\t@param NOT_A_REAL_PARAM" +
            "\nin DClass Foo" +
            "\nDid you make a typo? Are you trying to refer to something not visible to users?"
        assertThat(outputStreamCaptor.toString()).contains(expected)
        System.setOut(standardOut)
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
        val standardOut = System.out
        val outputStreamCaptor = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStreamCaptor))
        """
        |/**
        | * @property NOT_A_REAL_PROPERTY aaaaaa
        | */
        |class Foo(NOT_A_REAL_PROPERTY: String) { }
        """.render().documentation()
        var expected = "WARNING: unable to find what is referred to by" +
            "\n\t@property NOT_A_REAL_PROPERTY" +
            "\nin DClass Foo" +
            "\nDid you make a typo? Are you trying to refer to something not visible to users?"
        assertThat(outputStreamCaptor.toString()).contains(expected)
        System.setOut(PrintStream(outputStreamCaptor))
        """
        |/**
        | * @property NO_PROPERTIES_HERE aaaaaa
        | */
        |class Foo() { }
        """.render().documentation()
        expected = "WARNING: unable to find what is referred to by" +
            "\n\t@property NO_PROPERTIES_HERE" +
            "\nin DClass Foo" +
            "\nDid you make a typo? Are you trying to refer to something not visible to users?"
        assertThat(outputStreamCaptor.toString()).contains(expected)
        assertFails {
            """
            |class Foo() {
            |   /**
            |    * @property NO_PROPERTIES_HERE aaaaaa
            |    */
            |   fun doAThing()
            |}
            """.render().documentation() // can't @property on a function
        }
        assertFails {
            """
            |/**
            | * @property NO_PROPERTIES_HERE aaaaaa
            | */
            |fun doAThing()
            """.render().documentation() // can't @property on a function
        }
        assertFails {
            """
            |/** @property a
            |val b
            |val a
            """.render().documentation() // @property must be on correct property
        }
        System.setOut(standardOut)
    }

    @Test
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
            | * Called after the current PagedList has been updated.
            | *
            | * @param previousList The previous list, may be null.
            | * @param currentList The new current list, may be null.
            | */
            |public void onCurrentListChanged(
            |            @Nullable List<T> previousList,
            |            @Nullable List<T> currentList) {}
        """.render(java = true).documentation()
        for (documentation in listOf(documentationK, documentationJ)) {
            val paramTable = documentation.first { (it as? SummaryList)?.title() == "Parameters" }
                as SummaryList
            assertThat(paramTable.size()).isEqualTo(2)
            val param0 = paramTable.items().first()
            val param0Left = (param0.data.title as Parameter)
            val param1 = paramTable.items().last()
            val param1Left = (param1.data.title as Parameter)

            assertThat(param0.name()).isEqualTo("previousList")
            assertThat(param0.description().text()).isEqualTo("The previous list, may be null.")
            assertThat(param1.name()).isEqualTo("currentList")
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
                "loaded item, returning items of a potentially new type."
        )
        assertThat(param1Left.data.name).isEqualTo("ToValue")
        assertThat(param1Left.projectionName()).isEqualTo("String")
        assertThat(param1.description().text()).isEqualTo("Type of items produced by the " +
                "new DataSource, from the passed function."
        )

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
            assertThat("thefirst" in documentation.text()).isFalse()
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
        val paramDocText = (module.documentation(doc = {
            this.function()!!.parameters.single { it.name == "parent" }
        }).first() as Description).text()

        assertThat("placedin" in paramDocText).isFalse()
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
        val functionDesc = doc.first() as DefaultDescription
        assertThat(functionDesc.data.deprecation).isNotNull()
        // Checking the root for LastLine is somewhat testing Dokka
        // but this was broken in a previous version
        assertThat(functionDesc.text()).contains("LastLine")
        assertThat(functionDesc.text()).contains("Instead of using")
        assertThat(functionDesc.data.components.single().children.size).isEqualTo(7)
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
            .data.components.first().children.first() as Text)

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
            .data.components.first().children.first() as Text)

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

    @Test
    fun `@sample annotation in kotlin fails if the target samples doesn't exist`() {
        val documentation = """
            |/**
            | * a very foo description
            | *
            | * @sample foo.samples.fooSample
            | */
            |fun foo(a: String, b: String, c: String)
        """.trimIndent()
        assertFails { documentation.render().documentation() }
    }

    @Test
    fun `Verify that several real code samples don't give warnings`() {
        val standardOut = System.out
        val outputStreamCaptor = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStreamCaptor))
        val module = """
            |/**
            | * DSL for constructing a new [DynamicGraphNavigator.DynamicNavGraph]
            | *
            | * @param provider [NavigatorProvider] to use.
            | * @param id NavGraph id.
            | * @param startDestination Id start destination in the graph
            | */
            |@NavDestinationDsl
            |public class DynamicNavGraphBuilder(
            |    provider: NavigatorProvider,
            |    @IdRes id: Int,
            |    @IdRes private var startDestination: Int
            |) {}
            |
            |    /**
            |     * ParcelableArrayType is used for [NavArgument]s which hold arrays of Parcelables.
            |     *
            |     * Null values are supported.
            |     * Default values in Navigation XML files are not supported.
            |     *
            |     * @param type the type of Parcelable component class of the array
            |     */
            |    public class ParcelableArrayType<D : Parcelable>(type: Class<D>) : NavType<Array<D>?>(true) {
            |        /**
            |         * Constructs a NavType that supports arrays of a given Parcelable type.
            |         */
            |        init {
            |            require(Parcelable::class.java.isAssignableFrom(type)) {
            |                "            type            | does not implement Parcelable."
            |            }
            |            val arrayType: Class<Array<D>> = try {
            |                @Suppress("UNCHECKED_CAST")
            |                Class.forName("[L            |type.name            |;") as Class<Array<D>>
            |            } catch (e: ClassNotFoundException) {
            |                throw RuntimeException(e) // should never happen
            |            }
            |            this.arrayType = arrayType
            |        }
            | }
        """.trimIndent().render()
        val holder = runBlocking { DocumentablesHolder(module, this) }
        val classConverter1 = ClasslikeDocumentableConverter(language,
            module.explicitClasslike("DynamicNavGraphBuilder")!!, pathProvider(), holder)
        val documentedClass1 = runBlocking { classConverter1.classlike() }
        val classConverter2 = ClasslikeDocumentableConverter(language,
            module.explicitClasslike("ParcelableArrayType")!!, pathProvider(), holder)
        val documentedClass2 = runBlocking { classConverter2.classlike() }
        assertThat(outputStreamCaptor.toString()).doesNotContain("WARNING")
        System.setOut(standardOut)
    }

    private fun DModule.description(doc: DModule.() -> Documentable = ::smartDoc): Description {
        val holder = runBlocking { DocumentablesHolder(this@description, this) }
        val converter = DocTagConverter(language, pathProvider(), holder)
        val annotations = (this.doc() as? WithExtraProperties<*>)?.annotations().orEmpty()
        return converter.summaryDescription(this.doc(), annotations)
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
