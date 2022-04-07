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
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.Link
import com.google.devsite.components.Raw
import com.google.devsite.components.impl.UndocumentedSymbolDescriptionComponent
import com.google.devsite.components.symbols.LambdaTypeProjectionComponent
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.TypeParameterComponent
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.testing.NoopContextFreeComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.description
import com.google.devsite.renderer.converters.testing.generics
import com.google.devsite.renderer.converters.testing.isAtNonNull
import com.google.devsite.renderer.converters.testing.isAtNullable
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.projectionName
import com.google.devsite.renderer.converters.testing.single
import com.google.devsite.renderer.converters.testing.size
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.renderer.converters.testing.title
import com.google.devsite.renderer.converters.testing.typeAnnotations
import com.google.devsite.renderer.converters.testing.typeName
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.CodeBlock
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.Img
import org.jetbrains.dokka.model.doc.Pre
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertFails

@RunWith(Parameterized::class)
internal class DocTagConverterTest(
    private val displayLanguage: Language
) : ConverterTestBase(displayLanguage) {
    @Test
    fun `Empty description isn't documented`() {
        val description = """
            |class Foo
        """.render().description()

        assertThat(description).isInstanceOf(UndocumentedSymbolDescriptionComponent::class.java)
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
    fun `h2 and h3 work in description`() {
        val moduleK = """
            |/** Hello World!
            | *
            | * <h2>Second level</h2>
            | * <h3>Third level</h3>
            | */
            |class Foo
        """.render()

        val detailK = moduleK.documentation(doc = { this.clazz() }).item() as DescriptionComponent
        assertThat(detailK.render()).isEqualTo(
            """
<body>
  <p>Hello World!</p>
  <p><h2>Second level</h2><h3>Third level</h3></p>
</body>
            """.trim()
        )

        val moduleJ = """
            |/** Hello World!
            | *
            | * <h2>Second level</h2>
            | * <h3>Third level</h3>
            | */
            |public class Foo {}
        """.render(java = true)

        val detailJ = moduleJ.documentation(doc = { this.clazz() }).item() as DescriptionComponent
        assertThat(detailJ.render()).isEqualTo(
            """
<body>
  <p>Hello World! </p>
  <h2>Second level</h2>
  <h3>Third level</h3>
</body>
            """.trim()
        )
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

    /**
     * This test largely serves as a regression test for https://github.com/Kotlin/dokka/issues/1939
     */
    @Test
    fun `Full summary and description work with multiline in 4x Kotlin and Java`() {
        val moduleK = """
            |/**
            | * Hello World! Docs with period issue, e.g.&nbsp;this/e.g. this.
            | *
            | * A second line of desc. A third line of desc.
            | */
            |class Foo
        """.render()
        val moduleJ = """
            |/**
            | * Hello World! Docs with period issue, e.g.&nbsp;this/e.g. this.
            | *
            | * A second line of desc. A third line of desc.
            | */
            |public class Foo() {}
        """.render(java = true)

        for (module in listOf(moduleJ, moduleK)) {
            val summary = module.description(doc = { this.clazz() })
            assertThat(summary.data.summary).isTrue()
            assertThat(summary.render()).isEqualTo(
                """
<body>
  <p>Hello World! Docs with period issue, e.g. this/e.g. this.</p>
</body>
                """.trim()
            )

            val detail = module.documentation(doc = { this.clazz() }).item() as DescriptionComponent
            assertThat(detail.data.summary).isFalse()
            val separator = if (module == moduleK) "</p>\n  <p>" else " "
            assertThat(detail.render()).isEqualTo(
                """
<body>
  <p>Hello World! Docs with period issue, e.g. this/e.g. this.${separator}A second line of desc. A third line of desc.</p>
</body>
            """.trim()
            )

            val dComponents = detail.data.components
            val sComponents = summary.data.components
            for (components in listOf(dComponents, sComponents)) {
                val size = if (module == moduleJ) 1 else 2
                assertThat(components.size).isEqualTo(size)
            }
        }
    }

    @Test // b/192714584
    fun `th tag can be nested in a tr tag (with a thead tag)`() {
        val module = """
            |/**
            | * This is a table with a thead tag.
            | * <table>
            | *     <thead>
            | *         <tr>
            | *             <th>FooHead1</th>
            | *             <th>BarHead1</th>
            | *         </tr>
            | *     </thead>
            | *     <tr>
            | *         <td>FooCell1</td>
            | *         <td>BarCell1</td>
            | *     </tr>
            | * </table>
            | */
            |public class Foo() {}
        """.render(java = true)

        val detail = module.documentation(doc = { this.clazz() }).item() as DescriptionComponent
        assertThat(detail.render()).isEqualTo(
            """
<body>
  <p>This is a table with a thead tag. </p>
  <table>
    <thead>
      <tr>
        <th>FooHead1</th>
        <th>BarHead1</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>FooCell1</td>
        <td>BarCell1</td>
      </tr>
    </tbody>
  </table>
</body>
            """.trim()
        )
    }

    @Test // b/192714584
    fun `th tag can be nested in a tr tag (without a thead tag)`() {
        val module = """
            |/**
            | * This is a table without a thead tag.
            | * <table>
            | *     <tr>
            | *         <th>FooHead2</th>
            | *         <th>BarHead2</th>
            | *     </tr>
            | *     <tr>
            | *         <td>FooCell2</td>
            | *         <td>BarCell2</td>
            | *     </tr>
            | * </table>
            | */
            |public class Foo() {}
        """.render(java = true)

        val detail = module.documentation(doc = { this.clazz() }).item() as DescriptionComponent
        assertThat(detail.render()).isEqualTo(
            """
<body>
  <p>This is a table without a thead tag. </p>
  <table>
    <tbody>
      <tr>
        <th>FooHead2</th>
        <th>BarHead2</th>
      </tr>
      <tr>
        <td>FooCell2</td>
        <td>BarCell2</td>
      </tr>
    </tbody>
  </table>
</body>
            """.trim()
        )
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
            |public class Foo<Bar extends String, Baz> extends java.util.List<Bar> {
            |}
        """.render(java = true).documentation(doc = { this.clazz() })

        for (documentation in listOf(documentationK, documentationJ)) {
            val classParams = documentation.first {
                (it as? SummaryList)?.title() == "Parameters"
            } as SummaryList

            assertThat(classParams.size()).isEqualTo(2)
            val barParam = classParams.items().first().data
            val bazParam = classParams.items().last().data
            val barTypeParam = barParam.title as TypeParameterComponent
            assertThat(barTypeParam.data.name).isEqualTo("Bar")
            assertThat(barTypeParam.projectionName()).isEqualTo("String")
            assertThat((barParam.description as DescriptionComponent).text())
                .isEqualTo("A type of bar")
            val bazTypeParam = bazParam.title as TypeParameterComponent
            assertThat(bazTypeParam.data.name).isEqualTo("Baz")
            assertThat((bazParam.description as DescriptionComponent).text())
                .isEqualTo("Bazzy baz")
        }
    }

    @Test // NOTE: upstream dokka does not support @param <Baz> documentation style in kotlin
    fun `Function type parameters can be documented with @param with or without angle brackets`() {
        val documentationK = """
            |/**
            | * Hello World!
            | * @param Bar A type of bar
            | * @param Baz Bazzy baz
            | * @blamaram Wam wham
            | */
            |fun <Bar: String, Baz> foo(): List<Bar>
        """.render().documentation(doc = { this.function("foo")!! })
        val documentationJ = """
            |/**
            | * Hello World!
            | * @param Bar A type of bar
            | * @param <Baz> Bazzy baz
            | */
            |public <Bar extends String, Baz> java.util.List<Bar> foo() {
            |}
        """.render(java = true).documentation(doc = { this.function("foo")!! })

        for (documentation in listOf(documentationK, documentationJ)) {
            val params = documentation.first {
                (it as? SummaryList)?.title() == "Parameters"
            } as SummaryList

            assertThat(params.size()).isEqualTo(2)
            val barParam = params.items().first().data
            val bazParam = params.items().last().data
            val barTypeParam = barParam.title as TypeParameterComponent
            assertThat(barTypeParam.data.name).isEqualTo("Bar")
            assertThat(barTypeParam.projectionName()).isEqualTo("String")
            assertThat(
                (barParam.description as DescriptionComponent)
                    .text()
            ).isEqualTo("A type of bar")
            val bazTypeParam = bazParam.title as TypeParameterComponent
            assertThat(bazTypeParam.data.name).isEqualTo("Baz")
            assertThat(
                (bazParam.description as DescriptionComponent)
                    .text()
            ).isEqualTo("Bazzy baz")
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

        val propDoc = module.documentation({ this.property()!! }).single() as DescriptionComponent
        val classDoc = module.documentation({ this.clazz() }).single() as DescriptionComponent
        val constructorDoc = module.documentation({ this.constructor() })
        val conParamDoc = module.documentation({ this.constructor().parameters.single() }).single()

        // An odd propagation system, but it seems to work out to properly document everything?
        assertThat((propDoc).text()).isEqualTo("AtPropertyParameter docs")
        assertThat((classDoc).text()).isEqualTo("Class docs")
        assertThat(constructorDoc.size).isEqualTo(2)
        assertThat(constructorDoc.first())
            .isInstanceOf(UndocumentedSymbolDescriptionComponent::class.java)
        assertThat((constructorDoc.last() as SummaryList).title()).isEqualTo("Parameters")
        assertThat((constructorDoc.last() as SummaryList).single().name()).isEqualTo("bar")
        assertThat((constructorDoc.last() as SummaryList).single().description().text())
            .isEqualTo("AtParameterProperty docs")
        assertThat((conParamDoc as DescriptionComponent).text())
            .isEqualTo("AtPropertyParameter docs")
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
            .single() as DescriptionComponent
        val constructorDoc2 = withAnnotation.documentation({ this.constructors().last() })
            .single() as DescriptionComponent

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
        assertThat((propertyDoc.single() as DescriptionComponent).text())
            .isEqualTo("A vary bary name")
        // Upstream dokka does not propagate @property documentation on property parameters to the
        // constructor. We think this is what we want.
        assertFails { val constructorDoc = module.documentation({ this.constructor() }) }
    }

    @Test
    fun `Property parameter can be @suppress-ed as property only`() {
        val module = """
            |/**
            | * @param context context_docs
            | */
            |public open class NavController(
            |    /** @suppress */
            |    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            |    public val context: Context
            |) {
        """.render()
        assertThat(module.properties()).isEmpty()
        assertThat(module.constructor().parameters.single().name).isEqualTo("context")
        val constructorParams = module.documentation({ this.constructor() })[1] as SummaryList
        assertThat(constructorParams.title()).isEqualTo("Parameters")
        assertThat(constructorParams.item().name()).isEqualTo("context")
        assertThat(constructorParams.item().description().text()).isEqualTo("context_docs")
    }

    @Test
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

        for (documentation in listOf(documentationJ, documentationK)) {
            val description = documentation.last() as DescriptionComponent
            val img = description.data.components.first().children.item() as Img

            assertThat(img.params["href"]).isEqualTo("/path/to/img.jpg")
            assertThat(img.params["alt"]).isEqualTo("Alt text")
        }
    }

    @Test // Developers sometimes use white space to line up documentation
    fun `Parameter documentation works and ignores whitespace`() {
        val documentationK = """
            |/**
            | * @param a blah
            | * @param b        bblargh
            | */
            |fun foo(a: Int, b: String)
        """.render().documentation()
        val documentationJ = """
            |/**
            | * @param a blah
            | * @param b        bblargh
            | */
            |public void foo(Int a, String b)
        """.render(java = true).documentation()

        for (documentation in listOf(documentationK, documentationJ)) {
            val paramSummary = documentation.last() as SummaryList
            assertThat(paramSummary.items().size).isEqualTo(2)
            val param1 = paramSummary.items().first().data.title as ParameterComponent
            val param2 = paramSummary.items().last().data.title as ParameterComponent

            assertThat(paramSummary.title()).isEqualTo("Parameters")
            assertThat(param1.data.name).isEqualTo("a")
            assertThat(param2.data.name).isEqualTo("b")
        }
    }

    @Ignore // Upstream dokka does not support inheriting docs from hidden components
    @Test
    fun `Sealed class constructor docs inherit`() {
        val rendered = """
            |/**
            | * @param foo seele_foo_docs
            | */
            |sealed class Seele(foo: String) {
            |   /**
            |    * seele_constructor_docs
            |    */
            |   constructor {}
            |}
            |class Ba: Seele
        """.render()
        // sealed classes have hidden constructors
        assertThat((rendered.explicitClasslike("Seele") as DClass).constructors).isEmpty()

        val constructorDoc = rendered.documentation(
            { (this.explicitClasslike("Ba")!! as DClass).constructors.single() })
        val conParamDoc = rendered.documentation(
            { (this.explicitClasslike("Ba")!! as DClass).constructors.single().parameters.single() }
        ).single()

        assertThat(constructorDoc.size).isEqualTo(2)
        assertThat((constructorDoc.first() as DescriptionComponent).text())
            .isEqualTo("seele_constructor_docs")
        assertThat((constructorDoc.last() as SummaryList).title()).isEqualTo("Parameters")
        assertThat((constructorDoc.last() as SummaryList).single().name()).isEqualTo("foo")
        assertThat((constructorDoc.last() as SummaryList).single().description().text())
            .isEqualTo("seele_foo_docs")
        assertThat((conParamDoc as DescriptionComponent).text()).isEqualTo("seele_foo_docs")
    }

    @Test
    fun `Full documentation has properties`() {
        val description = """
            |/** @property bar a barber */
            |val bar: String = "barbarbar
        """.render().documentation(doc = {
            this.packages.single()
                .properties.single()
        }).single() as DescriptionComponent

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
        }).single() as DescriptionComponent

        assertThat(description.text()).isEqualTo("a barber")
    }

    @Test
    fun `@param throws exception or prints warning for invalid parameter`() {
        val exception = assertFails {
            """
            |/**
            | * @param NOT_A_REAL_PARAM aaaaaa
            | */
            |fun foo()
            """.render().documentation()
        }
        assertThat(exception.localizedMessage).contains("with contents: aaaaaa")
        val standardOut = System.out
        val outputStreamCaptor = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStreamCaptor))
        """
        |/**
        | * @param NOT_A_REAL_PARAM aaaaaa
        | */
        |class Foo { }
        """.render().documentation() // for type params and property params
        val expected = "WARN: Unable to find what is referred to by" +
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
        var expected = "WARN: Unable to find what is referred to by" +
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
        expected = "WARN: Unable to find what is referred to by" +
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
            |/** @property a */
            |val b
            |val a
            """.render().documentation() // @property must be on correct property
        }
        System.setOut(standardOut)
    }

    @Test // REGRESSION: go/dokka-upstream-bug/2388
    fun `Full documentation parameters table has types with nullability annotations`() {
        if (displayLanguage == Language.KOTLIN) return
        val documentationK = """
            |/**
            | * @param T a type
            | */
            |interface PagedListListener<T : Any> {
            |    /**
            |     * Called after the current PagedList has been updated.
            |     *
            |     * @param previousList The previous list of Ts, may be null.
            |     * @param currentList The new current list of nullable Ts, may not be null.
            |     */
            |    fun onCurrentListChanged(
            |       @Suppress("DEPRECATION") previousList: List<T>?,
            |       @Suppress("DEPRECATION") currentList: List<T?>
            |    )
            |}
        """.render().documentation()
        val documentationJ = """
            |/**
            | * Called after the current PagedList has been updated.
            | *
            | * @param previousList The previous list of Ts, may be null.
            | * @param currentList The new current list of nullable Ts, may not be null.
            | */
            |public <T> void onCurrentListChanged(
            |            @Nullable java.util.List<@NonNull T> previousList,
            |            @NonNull java.util.List<@Nullable T> currentList) {}
        """.render(java = true).documentation()
        for (documentation in listOf(documentationK, documentationJ)) {
            val paramTable = documentation.first { (it as? SummaryList)?.title() == "Parameters" }
                as SummaryList
            assertThat(paramTable.size()).isEqualTo(2)
            val param0 = paramTable.items().first()
            val param0Left = (param0.data.title as ParameterComponent)
            val param0Generic = param0Left.data.type.data.generics.single()
            val param1 = paramTable.items().last()
            val param1Left = (param1.data.title as ParameterComponent)
            val param1Generic = param1Left.data.type.data.generics.single()

            assertThat(param0.name()).isEqualTo("previousList")
            assertThat(param0.description().text())
                .isEqualTo("The previous list of Ts, may be null.")
            assertThat(param0Left.typeName()).isEqualTo("List")
            assertThat(param0Generic.name()).isEqualTo("T")
            assertThat(param1.name()).isEqualTo("currentList")
            assertThat(param1.description().text())
                .isEqualTo("The new current list of nullable Ts, may not be null.")
            assertThat(param0Left.typeName()).isEqualTo("List")
            assertThat(param1Generic.name()).isEqualTo("T")

            javaOnly {
                if (documentation == documentationJ) {
                    assertThat(param0Left.typeAnnotations().single().isAtNullable).isTrue()
                    // assertThat(param1Generic.data.annotationComponents.single().isAtNullable).isTrue()
                } else {
                    assertThat(param0Left.typeAnnotations()).isEmpty()
                    assertThat(param1Generic.data.annotationComponents.isEmpty())
                }
                // This is also the upstream bug; T should be @NonNull from both source languages
                if (documentation == documentationK) assertThat(
                    param0Generic.data.annotationComponents.single().isAtNonNull
                ).isTrue()
                assertThat(param0Left.data.annotationComponents).isEmpty()
                assertThat(param1Left.data.annotationComponents).isEmpty()
                assertThat(param1Left.typeAnnotations().single().isAtNonNull).isTrue()
            }
            kotlinOnly {
                assertThat(param0Left.data.type.nullable).isEqualTo(true)
                assertThat(param0Generic.nullable).isEqualTo(false)
                assertThat(param1Left.data.type.nullable).isEqualTo(false)
                assertThat(param0Generic.nullable).isEqualTo(true)
            }
        }
    }

    @Test
    fun `Parameter detection works on nested named lambda types`() {
        val documentation = """
            |/**
            | * @param funA a fun
            | * @param funB be fun
            | * @param unt where units go
            | * @param foo a list of foos
            | */
            |fun mega(funA: (funB: ((unt: Unit, foo: List<Boolean>) -> String) -> Int) -> (Double))
        """.render().documentation()
        val paramTable = documentation.first {
            (it as? SummaryList)?.title() == "Parameters"
        } as SummaryList
        val funADocs = paramTable.items().single { it.name() == "funA" }.description().text()
        assertThat(funADocs).isEqualTo("a fun")
        val funBDocs = paramTable.items().single { it.name() == "funB" }.description().text()
        assertThat(funBDocs).isEqualTo("be fun")
        val untDocs = paramTable.items().single { it.name() == "unt" }.description().text()
        assertThat(untDocs).isEqualTo("where units go")
        val fooDocs = paramTable.items().single { it.name() == "foo" }.description().text()
        assertThat(fooDocs).isEqualTo("a list of foos")
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
            (it as? SummaryList)?.title() == "Parameters"
        } as SummaryList

        // We don't want Value and/or Key to appear listed as parameters for map().
        assertThat(paramTable.size()).isEqualTo(2)

        val param0 = paramTable.items().first()
        val param0Left = param0.data.title as ParameterComponent
        val param1 = paramTable.items().last()
        val param1Left = param1.data.title as TypeParameterComponent

        assertThat(param0Left.data.name).isEqualTo("function")
        javaOnly {
            assertThat(param0Left.data.type.link().name).isEqualTo("Function1")
            val param0LambdaTypes = param0Left.generics().map { it.link().name }
            assertThat(param0LambdaTypes).isEqualTo(listOf("Value", "ToValue"))
        }
        kotlinOnly {
            val lambdaSymbol = param0Left.data.type as LambdaTypeProjectionComponent
            assertThat(lambdaSymbol.data.receiver).isNull()
            assertThat(lambdaSymbol.data.lambdaModifiers).isEmpty()
            // The evaluation type of the lambda is ToValue
            assertThat(lambdaSymbol.link().name).isEqualTo("ToValue")
            val param0LambdaArgumentType = lambdaSymbol.data.lambdaParams
                .map { it.data.type.link().name }
            assertThat(param0LambdaArgumentType).isEqualTo(listOf("Value"))
        }
        assertThat(param0.description().text()).isEqualTo(
            "Function that runs on each " +
                "loaded item, returning items of a potentially new type."
        )
        assertThat(param1Left.data.name).isEqualTo("ToValue")
        assertThat(param1Left.projectionName()).isEqualTo("String")
        assertThat(param1.description().text()).isEqualTo(
            "Type of items produced by the " +
                "new DataSource, from the passed function."
        )

        val seeAlsoTable = documentation.first {
            (it as? SummaryList)?.title() == "See also"
        } as SummaryList
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
            (it as? SummaryList)?.title() == "See also"
        } as SummaryList
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
        """.render().documentation().first() as DescriptionComponent
        val documentationJ = """
            |/**
            | * This is a multi-line documentation string. There is no space at the end of the
            | * first line, but "the first" with no space should not appear in the final documentation.
            | */
            | public String foo()
        """.render(java = true).documentation().first() as DescriptionComponent

        for (documentation in listOf(documentationK, documentationJ)) {
            assertThat(documentation.text()).doesNotContain("thefirst")
        }
    }

    @Test
    fun `Test several odd link choices`() {
        val documentation = """
            |/**
            | * Does bar on [this]
            | */
            |fun Foo.bar() {}
        """.render().documentation().first() as DescriptionComponent
        val docChildren = documentation.data.components.single().children
        assertThat(docChildren.size).isEqualTo(2)
        val link = (docChildren.last() as DocumentationLink)
        assertThat((link.children.single() as Text).body).isEqualTo("this")
        assertThat(link.dri.packageName).isEqualTo("androidx.example")
        assertThat(link.dri.classNames).isEqualTo(null)
        assertThat((link.dri.callable as Callable).name).isEqualTo("<this>")
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
        val paramDocText = (
            module.documentation(doc = {
                this.function()!!.parameters.single { it.name == "parent" }
            }).first() as DescriptionComponent
            ).text()

        assertThat(paramDocText).doesNotContain("placedin")
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
        val functionDesc = doc.first() as DescriptionComponent
        assertThat(functionDesc.data.deprecation).isNotNull()
        // Checking the root for LastLine is somewhat testing Dokka
        // but this was broken in a previous version
        assertThat(functionDesc.text()).contains("LastLine")
        assertThat(functionDesc.text()).contains("Instead of using")
        assertThat(functionDesc.data.components.single().children.size).isEqualTo(7)
    }

    @Test // NOTE: render-to-text puts two spaces where the <pre> was. Is this correct?
    fun `Test code blocks with @literals and nesting`() {
        val documentation = """
            |/**
            | * Below is a sample of a simple database.
            | * <pre>
            | * // File: Song.java
            | * {@literal @}Entity
            | * public class Song {
            | */
            |public void foo(){}
        """.render(java = true)
        val doc = documentation.documentation()
        val functionDesc = doc.first() as DescriptionComponent
        assertThat(functionDesc.text()).isEqualTo(
            "Below is a sample of a simple database." +
                "  // File: Song.java\n @ Entity\npublic class Song {"
        )
        assertThat(functionDesc.data.components.last()).isInstanceOf(Pre::class.java)
    }

    @Test
    fun `Full documentation has receiver param`() {
        val documentation = """
            |/** @receiver blah */
            |fun Int.foo()
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramParam = (paramSummary.item().data.title as ParameterComponent).data

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
        val throwsRight = (
            (throwsSummary.item().data.description as DescriptionComponent)
                .data.components.first().children.first() as Text
            )

        assertThat(throwsLeft.data.text).contains("IllegalStateException")
        assertThat(throwsRight.body).isEqualTo("if it fails")
    }

    @Test
    fun `Full Java documentation has throws tag`() {
        val documentation = """
            |/** @throws IllegalStateException if it fails */
            |public void foo() {}
        """.render(java = true).documentation()

        val throwsSummary = documentation.first { (it as? SummaryList)?.title() == "Throws" }
            as SummaryList
        val throwsLeft = throwsSummary.item().data.title as Raw
        val throwsRight = (
            (throwsSummary.item().data.description as DescriptionComponent)
                .data.components.first().children.first() as Text
            )

        assertThat(throwsLeft.data.text).isEqualTo("java.lang.IllegalStateException")
        assertThat(throwsRight.body).isEqualTo("if it fails")
    }

    @Ignore // b/203691421
    @Test
    fun `Full Java documentation has checked exceptions`() {
        val documentation = """
            |/** IllegalStateException if it fails */
            |public void foo() throws IllegalStateException {}
        """.render(java = true).documentation()

        val throwsSummary = documentation.first { (it as? SummaryList)?.title() == "Throws" }
            as SummaryList
        val throwsLeft = throwsSummary.item().data.title as Raw
        val throwsRight = (
            (throwsSummary.item().data.description as DescriptionComponent)
                .data.components.first().children.first() as Text
            )

        assertThat(throwsLeft.data.text).isEqualTo("java.lang.IllegalStateException")
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
        assertThat(paramText.description().text()).isEqualTo("blah")
    }

    @Test
    fun `See also parses external link`() {
        val documentation = """
            |/** @see String */
            |class Foo
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertThat(paramText.link().url)
            .isEqualTo("https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html")
        assertThat(paramText.description().text()).isEmpty()
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
        assertThat(paramText.description().text()).isEmpty()
    }

    @Ignore // Our package-list is incomplete. b/225929725
    @Test
    fun `See also parses link with Kotlin style function`() {
        val documentation = """
            |/**
            | * @see String.isEmpty
            | * @see String.lastIndex
            | */
            |class Foo
        """.render().documentation()

        val paramSummary = documentation.last() as SummaryList
        val (paramText1, paramText2) = paramSummary.items(2).toList()

        // TODO(b/167437580): figure out how to reliably parse links
        assertThat(paramText1.link().url).contains("isEmpty")
        assertThat(paramText1.description().text()).isEmpty()
        assertThat(paramText2.link().url).contains("lastIndex")
        assertThat(paramText2.description().text()).isEmpty()
    }

    @Test
    fun `See also parses link with Java style function`() {
        val documentation = """
            |/** @see String#isEmpty() */
            |public void foo() {}
        """.render(java = true).documentation()

        val paramSummary = documentation.last() as SummaryList
        val paramText = paramSummary.item()

        assertThat(paramText.link().url)
            .isEqualTo("https://developer.android.com/reference/java/lang/String.html#isEmpty()")
        assertThat(paramText.description().text()).isEmpty()
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

        assertThat(paramText.link().url).isEqualTo(
            "https://developer.android.com/reference/java/lang/String.html"
        )
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
            assertThat((param.data.title as ParameterComponent).data.name).isEqualTo(expected[i])
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
        """.render()
        val (holder, pathProvider) = holderAndProvider(module)
        val classConverter1 = ClasslikeDocumentableConverter(
            displayLanguage,
            module.explicitClasslike("DynamicNavGraphBuilder")!!,
            pathProvider,
            holder
        )
        val documentedClass1 = runBlocking { classConverter1.classlike() }
        val classConverter2 = ClasslikeDocumentableConverter(
            displayLanguage,
            module.explicitClasslike("ParcelableArrayType")!!,
            pathProvider,
            holder
        )
        val documentedClass2 = runBlocking { classConverter2.classlike() }
        assertThat(outputStreamCaptor.toString()).doesNotContain("WARNING")
        System.setOut(standardOut)
    }

    @Test
    fun `Test @return on property parameters`() {
        val module = """
            |public class Foo private constructor(
            |   /**
            |    * The arguments used for this entry
            |    * @return The arguments used when this entry was created
            |    *
            |    */
            |   @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            |   public var arguments: List<String>? = null
            |) {}
        """.render()
        val documentation = module.documentation({ this.property("arguments")!! })
        val description = (documentation.first() as DescriptionComponent).text()
        assertThat(description).isEqualTo("The arguments used for this entry")
        val table = (documentation.last() as SummaryList)
        assertThat(table.title()).isEqualTo("Returns")
        val tableEntry = table.item().description().text()
        assertThat(tableEntry).isEqualTo("The arguments used when this entry was created")
    }

    @Test // Note: this tests upstream behavior.
    fun `Test spacing around line wraps including inside tags`() {
        val documentation = """
            |/**
            | * blah blah blah blah {@link #longFunctionName(java.lang.Object, java.lang.String, int,
            | * java.lang.Integer) LongContainingClassName#longFunctionName(Object, String, int,
            | * Integer)}
            | */
            |public void longFunctionName(Object arg1, String arg2, int arg3, Integer arg4) {}
            |
        """.render(java = true).documentation({ this.function("longFunctionName")!! })
        val components = (documentation.single() as DescriptionComponent)
            .data.components.single().children
        val link = components[1] as DocumentationLink
        assertThat(link.text()).doesNotContain("int,Integer") // should be a space here
    }

    @Test
    fun `from ExoPlayer2, @link split across lines works`() {
        val documentation = """
        |public @interface Mega {
        |   /**
        |    * Reason why playback is suppressed even though {@link #getPlayWhenReady()} is {@code true}. One
        |    * of {@link #PLAYBACK_SUPPRESSION_REASON_NONE} or {@link
        |    * #PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS}.
        |    */
        |   @Documented
        |   @Retention(RetentionPolicy.SOURCE)
        |   @IntDef({
        |       PLAYBACK_SUPPRESSION_REASON_NONE,
        |       PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
        |   })
        |   @interface PlaybackSuppressionReason {}
        |   /** Playback is not suppressed. */
        |   int PLAYBACK_SUPPRESSION_REASON_NONE = 0;
        |   /** Playback is suppressed due to transient audio focus loss. */
        |   int PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS = 1;
        |
        |   public void longFunctionName(Object arg1, String arg2, int arg3, Integer arg4)
        |}
        """.render(java = true)
            .documentation({ this.explicitClasslike("PlaybackSuppressionReason") })
        val components = (documentation.single() as DescriptionComponent)
            .data.components.single().children
        val link1 = components[5] as DocumentationLink
        val link2 = components[7] as DocumentationLink

        // assertThat(link1.dri.packageName).isEqualTo("androidx.example")
        // assertThat(link2.dri.packageName).isEqualTo("androidx.example")
        assertThat(link1.dri.classNames).isEqualTo("Test.Mega")
        assertThat(link2.dri.classNames).isEqualTo("Test.Mega")
        assertThat(link1.dri.callable!!.name).isEqualTo("PLAYBACK_SUPPRESSION_REASON_NONE")
        assertThat(link2.dri.callable!!.name)
            .isEqualTo("PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS")

        assertThat(link1.text()).isEqualTo("PLAYBACK_SUPPRESSION_REASON_NONE")
        assertThat(link2.text())
            .isEqualTo("PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS")
    }

    @Test
    fun `From Platform, java-concurrent-future, pre{@code formatting test`() {
        val documentation = """
            /**
             * A {@code Future} represents the result of an asynchronous
             * computation.  Methods are provided to check if the computation is
             * complete, to wait for its completion, and to retrieve the result of
             * the computation.  The result can only be retrieved using method
             * {@code get} when the computation has completed, blocking if
             * necessary until it is ready.  Cancellation is performed by the
             * {@code cancel} method.  Additional methods are provided to
             * determine if the task completed normally or was cancelled. Once a
             * computation has completed, the computation cannot be cancelled.
             * If you would like to use a {@code Future} for the sake
             * of cancellability but not provide a usable result, you can
             * declare types of the form {@code Future<?>} and
             * return {@code null} as a result of the underlying task.
             *
             * <p><b>Sample Usage</b> (Note that the following classes are all
             * made-up.)
             *
             * <pre> {@code
             * interface ArchiveSearcher { String search(String target); }
             * class App {
             *   ExecutorService executor = ...
             *   ArchiveSearcher searcher = ...
             *   void showSearch(String target) throws InterruptedException {
             *     Callable<String> task = () -> searcher.search(target);
             *     Future<String> future = executor.submit(task);
             *     displayOtherThings(); // do other things while searching
             *     try {
             *       displayText(future.get()); // use future
             *     } catch (ExecutionException ex) { cleanup(); return; }
             *   }
             * }}</pre>
             *
             * The {@link FutureTask} class is an implementation of {@code Future} that
             * implements {@code Runnable}, and so may be executed by an {@code Executor}.
             * For example, the above construction with {@code submit} could be replaced by:
             * <pre> {@code
             * FutureTask<String> future = new FutureTask<>(task);
             * executor.execute(future);}</pre>
             *
             * <p>Memory consistency effects: Actions taken by the asynchronous computation
             * <a href="package-summary.html#MemoryVisibility"> <i>happen-before</i></a>
             * actions following the corresponding {@code Future.get()} in another thread.
             *
             * @see FutureTask
             * @see Executor
             * @since 1.5
             * @author Doug Lea
             * @param <V> The result type returned by this Future's {@code get} method
             */
            public interface Future<V>
        """.trimIndent().render(java = true).documentation({ this.explicitClasslike("Future") })
        val description = documentation[0] as DescriptionComponent
        val firstCodeBlock = (description.data.components[2] as CodeBlock).text()
        assertThat(firstCodeBlock).doesNotContain("&lt;")
        assertThat(firstCodeBlock).doesNotContain("<code>")
    }

    private fun DModule.description(doc: DModule.() -> Documentable = ::smartDoc):
        DescriptionComponent {
        val (holder, pathProvider) = holderAndProvider(this)
        val converter = DocTagConverter(displayLanguage, pathProvider, holder)
        val annotations = (this.doc() as? WithExtraProperties<*>)?.annotations().orEmpty()
        return converter.summaryDescription(this.doc(), annotations)
    }

    private fun DModule.documentation(
        doc: DModule.() -> Documentable = ::smartDoc,
        paramNames: List<String> = emptyList()
    ): List<ContextFreeComponent> {
        val (holder, pathProvider) = holderAndProvider(this)
        val converter = DocTagConverter(displayLanguage, pathProvider, holder)
        return converter.metadata(
            doc(),
            returnType = NoopContextFreeComponent,
            paramNames = paramNames
        )
    }

    private fun DModule.clazz(name: String? = null): DClass {
        val topClass = name?.let { this.explicitClasslike(name) } ?: this.classlike()!!
        if (topClass.classlikes.isNotEmpty()) return topClass.classlikes.single() as DClass
        return topClass as DClass
    }

    /** In case you aren't explicit, our best guess at what you want docs for. */
    private fun smartDoc(module: DModule): Documentable {
        return module.function() ?: module.classlike()!!
    }

    private fun ContextFreeComponent.render() = createHTML().body {
        this@render.render(this)
    }.trim()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
