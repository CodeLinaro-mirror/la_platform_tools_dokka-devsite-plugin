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
import com.google.devsite.DocsSummaryList
import com.google.devsite.LinkDescriptionSummaryList
import com.google.devsite.WithDescriptionList
import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.Link
import com.google.devsite.components.impl.UndocumentedSymbolDescriptionComponent
import com.google.devsite.components.symbols.LambdaTypeProjectionComponent
import com.google.devsite.components.symbols.TypeParameterComponent
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.components.testing.NoopTypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.generics
import com.google.devsite.renderer.converters.testing.isAtNonNull
import com.google.devsite.renderer.converters.testing.isAtNullable
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.projectionName
import com.google.devsite.renderer.converters.testing.single
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.renderer.converters.testing.typeAnnotations
import com.google.devsite.renderer.converters.testing.typeName
import com.google.devsite.testing.ConverterTestBase
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertFails
import kotlinx.coroutines.runBlocking
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.childrenOfType
import org.jetbrains.dokka.model.doc.A
import org.jetbrains.dokka.model.doc.CodeBlock
import org.jetbrains.dokka.model.doc.CodeInline
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.Img
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Pre
import org.jetbrains.dokka.model.doc.Text
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("UNCHECKED_CAST")
@RunWith(Parameterized::class)
internal class DocTagConverterTest(private val displayLanguage: Language) :
    ConverterTestBase(displayLanguage) {
    @Test
    fun `Empty description isn't documented`() {
        val description =
            """
            |class Foo
        """
                .render()
                .description()

        assertThat(description).isInstanceOf(UndocumentedSymbolDescriptionComponent::class.java)
    }

    @Test
    fun `Basic summary description has correct flags`() {
        val description =
            """
            |/** Hello World! */
            |class Foo
        """
                .render()
                .description()

        assertThat(description.data.summary).isTrue()
        assertThat(description.data.deprecation).isNull()
    }

    @Test
    fun `h2 and h3 work in description`() {
        val moduleK =
            """
            |/** Hello World!
            | *
            | * <h2>Second level</h2>
            | * <h3>Third level</h3>
            | */
            |class Foo
        """
                .render()

        val detailK = moduleK.documentation(doc = { this.clazz() }).item() as DescriptionComponent
        assertThat(detailK.render())
            .isEqualTo(
                """
<body>
  <p>Hello World!</p>
  <p><h2>Second level</h2> <h3>Third level</h3></p>
</body>
            """
                    .trim()
            )

        val moduleJ =
            """
            |/** Hello World!
            | *
            | * <h2>Second level</h2>
            | * <h3>Third level</h3>
            | */
            |public class Foo {}
        """
                .render(java = true)

        val detailJ = moduleJ.documentation(doc = { this.clazz() }).item() as DescriptionComponent
        assertThat(detailJ.render())
            .isEqualTo(
                """
<body>
  <p>Hello World! </p>
  <h2>Second level</h2>
  <h3>Third level</h3>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Deprecated class summary and detail description flags correct in 4x Kotlin and Java`() {
        val codeK =
            """
            |/**
            | * class_description
            | */
            |@Deprecated("Bye")
            |class Foo
        """
                .render()
        val summaryK = codeK.description()
        val detailsK = codeK.documentation()

        val codeJ =
            """
            |/**
            | * class_description
            | * @deprecated Bye
            | */
            |@Deprecated
            |public class Foo {}
        """
                .render(java = true)
        val summaryJ = codeJ.description { this.clazz() }
        val detailsJ = codeJ.documentation(doc = { this.clazz() })

        for (summary in listOf(summaryK, summaryJ)) {
            assertThat(summary.data.summary).isTrue()
            assertThat(summary.text()).isEqualTo("Bye")
            assertThat(summary.data.deprecation).isEqualTo("This class is deprecated.")
        }

        for (details in listOf(detailsK, detailsJ)) {
            val detail = (details.first() as DescriptionComponent)
            assertThat(detail.data.summary).isFalse()
            assertThat(detail.text()).isEqualTo("Bye")
            assertThat(detail.data.deprecation).isEqualTo("This class is deprecated.")
        }
    }

    /**
     * This test largely serves as a regression test for https://github.com/Kotlin/dokka/issues/1939
     */
    @Test
    fun `Full summary and description work with multiline in 4x Kotlin and Java`() {
        val moduleK =
            """
            |/**
            | * Hello World! Docs with period issue, e.g.&nbsp;this/e.g. this.
            | *
            | * A second line of desc. A third line of desc.
            | */
            |class Foo
        """
                .render()
        val moduleJ =
            """
            |/**
            | * Hello World! Docs with period issue, e.g.&nbsp;this/e.g. this.
            | *
            | * A second line of desc. A third line of desc.
            | */
            |public class Foo() {}
        """
                .render(java = true)

        for (module in listOf(moduleJ, moduleK)) {
            val summary = module.description(doc = { this.clazz() })
            assertThat(summary.data.summary).isTrue()
            assertThat(summary.render())
                .isEqualTo(
                    """
<body>
  <p>Hello World! Docs with period issue, e.g. this/e.g. this.</p>
</body>
                """
                        .trim()
                )

            val detail = module.documentation(doc = { this.clazz() }).item() as DescriptionComponent
            assertThat(detail.data.summary).isFalse()
            val separator = if (module == moduleK) "</p>\n  <p>" else " "
            assertThat(detail.render())
                .isEqualTo(
                    """
<body>
  <p>Hello World! Docs with period issue, e.g. this/e.g. this.${separator}A second line of desc. A third line of desc.</p>
</body>
            """
                        .trim()
                )

            val dComponents = detail.data.components
            val sComponents = summary.data.components
            for (components in listOf(dComponents, sComponents)) {
                val size = if (module == moduleJ) 1 else 2
                assertThat(components.size).isEqualTo(size)
            }
        }
    }

    /** In particular, this is testing "i.e. UppercaseLetter". */
    @Test
    fun `More tests for ie based on androidx biometric`() {
        val module =
            """
            |public class BiometricManager {
            |   public interface Authenticators {
            |       /**
            |        * The non-biometric credential used to secure the device (i.e. PIN, pattern, or password).
            |        * This should typically only be used in combination with a biometric auth type, such as
            |        * {@link #BIOMETRIC_WEAK}.
            |        */
            |       int DEVICE_CREDENTIAL = 1 << 15;
            |   }
            |}
        """
                .render(java = true)

        val summary = module.description(doc = { this.property(name = "DEVICE_CREDENTIAL")!! })
        assertThat(summary.data.summary).isTrue()
        assertThat(summary.render())
            .isEqualTo(
                """
<body>
  <p>The non-biometric credential used to secure the device (i.e. PIN, pattern, or password).</p>
</body>
            """
                    .trim()
            )

        val detail =
            module.documentation(doc = { this.property(name = "DEVICE_CREDENTIAL")!! }).item()
                as DescriptionComponent
        assertThat(detail.data.summary).isFalse()
        assertThat(detail.render())
            .isEqualTo(
                """
<body>
  <p>The non-biometric credential used to secure the device (i.e. PIN, pattern, or password). This should typically only be used in combination with a biometric auth type, such as BIOMETRIC_WEAK.</p>
</body>
        """
                    .trim()
            )

        val dComponents = detail.data.components
        val sComponents = summary.data.components
        for (components in listOf(dComponents, sComponents)) {
            assertThat(components.size).isEqualTo(1)
        }
    }

    @Test // b/192714584
    fun `th tag can be nested in a tr tag (with a thead tag)`() {
        val module =
            """
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
        """
                .render(java = true)

        val detail = module.documentation(doc = { this.clazz() }).item() as DescriptionComponent
        assertThat(detail.render())
            .isEqualTo(
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
            """
                    .trim()
            )
    }

    @Test // b/192714584
    fun `th tag can be nested in a tr tag (without a thead tag)`() {
        val module =
            """
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
        """
                .render(java = true)

        val detail = module.documentation(doc = { this.clazz() }).item() as DescriptionComponent
        assertThat(detail.render())
            .isEqualTo(
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
            """
                    .trim()
            )
    }

    @Test // NOTE: upstream dokka does not support @param <Baz> documentation style in kotlin
    fun `Class type parameters can be documented with @param with or without angle brackets`() {
        val documentationK =
            """
            |/**
            | * Hello World!
            | * @param Bar A type of bar
            | * @param Baz Bazzy baz
            | */
            |class Foo<Bar: String, Baz>: List<Bar>
        """
                .render()
                .documentation(doc = { this.clazz() })
        val documentationJ =
            """
            |/**
            | * Hello World!
            | * @param Bar A type of bar
            | * @param <Baz> Bazzy baz
            | */
            |public class Foo<Bar extends String, Baz> extends java.util.List<Bar> {
            |}
        """
                .render(java = true)
                .documentation(doc = { this.clazz() })

        for (documentation in listOf(documentationK, documentationJ)) {
            val classParams = documentation.paramList()

            assertThat(classParams.size).isEqualTo(2)
            val barParam = classParams.items().first().data
            val bazParam = classParams.items().last().data
            val barTypeParam = barParam.title as TypeParameterComponent
            assertThat(barTypeParam.data.name).isEqualTo("Bar")
            assertThat(barTypeParam.projectionName()).isEqualTo("String")
            assertThat(barParam.description.text()).isEqualTo("A type of bar")
            val bazTypeParam = bazParam.title as TypeParameterComponent
            assertThat(bazTypeParam.data.name).isEqualTo("Baz")
            assertThat(bazParam.description.text()).isEqualTo("Bazzy baz")
        }
    }

    @Test // NOTE: upstream dokka does not support @param <Baz> documentation style in kotlin
    fun `Function type parameters can be documented with @param with or without angle brackets`() {
        val documentationK =
            """
            |/**
            | * Hello World!
            | * @param Bar A type of bar
            | * @param Baz Bazzy baz
            | * @blamaram Wam wham
            | */
            |fun <Bar: String, Baz> foo(): List<Bar>
        """
                .render()
                .documentation(doc = { this.function("foo")!! })
        val documentationJ =
            """
            |/**
            | * Hello World!
            | * @param Bar A type of bar
            | * @param <Baz> Bazzy baz
            | */
            |public <Bar extends String, Baz> java.util.List<Bar> foo() {
            |}
        """
                .render(java = true)
                .documentation(doc = { this.function("foo")!! })

        for (documentation in listOf(documentationK, documentationJ)) {
            val params = documentation.paramList()

            assertThat(params.size).isEqualTo(2)
            val barParam = params.items().first().data
            val bazParam = params.items().last().data
            val barTypeParam = barParam.title as TypeParameterComponent
            assertThat(barTypeParam.data.name).isEqualTo("Bar")
            assertThat(barTypeParam.projectionName()).isEqualTo("String")
            assertThat(barParam.description.text()).isEqualTo("A type of bar")
            val bazTypeParam = bazParam.title as TypeParameterComponent
            assertThat(bazTypeParam.data.name).isEqualTo("Baz")
            assertThat(bazParam.description.text()).isEqualTo("Bazzy baz")
        }
    }

    @Test // b/398214231
    fun `Can add @throws to primary constructor`() {
        val module =
            """
            |/**
            | * Class docs
            | * @throws RuntimeException if construction fails
            | */
            |class Foo()
        """
                .render()
        val classDoc = module.documentation({ this.clazz() }) // as DescriptionComponent
        val constructorDoc = module.documentation({ this.constructor() })
        assertThat(classDoc.size).isEqualTo(2)
        assertThat((classDoc[1] as DocsSummaryList).data.header.toString()).isEqualTo("Throws")
        // TODO(b/398214231) thrown exception is not propagated. Unclear if this is correct; if so,
        // there should be some way to document a primary constructor
        // assertThat(constructorDoc.size).isEqualTo(2)
        // assertThat((classDoc[1] as DocsSummaryList).data.header.toString()).isEqualTo("Throws")
    }

    @Test
    fun `Property parameter docs propagate correctly`() {
        // Property parameters are kotlin-exclusive
        val module =
            """
            |/**
            | * Class docs
            | * @property bar AtPropertyParameter docs
            | * @param bar AtParameterProperty docs
            | */
            |class Foo(val bar: String)
        """
                .render()

        val propDoc = module.documentation({ this.property()!! }).single() as DescriptionComponent
        val classDoc = module.documentation({ this.clazz() }).single() as DescriptionComponent
        val constructorDoc = module.documentation({ this.constructor() })
        val conParamDoc =
            module
                .documentation({ this.constructor().parameters.single() }, isFromJava = false)
                .single()

        // An odd propagation system, but it seems to work out to properly document everything?
        assertThat((propDoc).text()).isEqualTo("AtPropertyParameter docs")
        assertThat((classDoc).text()).isEqualTo("Class docs")
        assertThat(constructorDoc.size).isEqualTo(2)
        assertThat(constructorDoc.first())
            .isInstanceOf(UndocumentedSymbolDescriptionComponent::class.java)
        val paramsList = constructorDoc.paramList()
        assertThat(paramsList.single().name()).isEqualTo("bar")
        assertThat(paramsList.single().data.description.text())
            .isEqualTo("AtParameterProperty docs")
        assertThat((conParamDoc as DescriptionComponent).text())
            .isEqualTo("AtPropertyParameter docs")
    }

    @Test
    fun `@constructor docs are applied`() {
        val constructorModule =
            """
            |/**
            | * The amount by which the text is shifted up or down from current the baseline.
            | * @constructor Primary constructor docs
            | */
            |class BaselineShift(val multiplier: Float) {
            |   /** Secondary constructor docs */
            |   constructor(multiplier: Int) : this(multiplier)
            |
            |}
        """
                .render()

        val constructorDocs =
            constructorModule.constructors().map {
                (constructorModule.documentation({ it }).single() as DescriptionComponent).text()
            }
        assertThat(constructorDocs)
            .containsExactly("Secondary constructor docs", "Primary constructor docs")
    }

    @Test
    fun `@constructor docs are applied on annotation class`() {
        // Note that annotations cannot have secondary constructors
        val constructorModule =
            """
            |/**
            | * The amount by which the text is shifted up or down from current the baseline.
            | * @constructor Primary constructor docs
            | */
            |annotation class BaselineShift(val multiplier: Float)
        """
                .render()

        val constructorDoc =
            constructorModule.documentation({ this.constructors().single() }).single()
                as DescriptionComponent

        assertThat(constructorDoc.text()).isEqualTo("Primary constructor docs")
    }

    @Test
    fun `Class property parameters can be documented with @property on the class`() {
        val module =
            """
            |/**
            | * Hello World!
            | * @param baz Buzzbuzzbuzz
            | * @property bar A vary bary name
            | */
            |class Foo(val bar: String) {
            |
            |}
        """
                .render()

        val propertyDoc = module.documentation({ this.property("bar")!! })
        // Upstream dokka does not propagates @param documentation on property parameters to the
        // property. We think this is what we want.
        assertThat((propertyDoc.single() as DescriptionComponent).text())
            .isEqualTo("A vary bary name")
        // Upstream dokka does not propagate @property documentation on property parameters to the
        // constructor. We think this is what we want.
        assertFails {
            val constructorDoc = module.documentation({ this.constructor() })
        }
    }

    @Test
    fun `Property parameter can be @suppress-ed as property only`() {
        val module =
            """
            |/**
            | * @param context context_docs
            | */
            |public open class NavController(
            |    /** @suppress */
            |    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            |    public val context: Context
            |) {
        """
                .render()
        assertThat(module.properties()).isEmpty()
        assertThat(module.constructor().parameters.single().name).isEqualTo("context")
        val constructorParams = module.documentation({ this.constructor() }).paramList()
        assertThat(constructorParams.item().name()).isEqualTo("context")
        assertThat(constructorParams.item().data.description.text()).isEqualTo("context_docs")
    }

    @Test
    fun `Full documentation has img tag in 4x Kotlin and Java`() {
        val documentationK =
            """
            |/**
            | ![Alt text](/path/to/img.jpg)
            |*/
            |fun foo(a: Int)
        """
                .render()
                .documentation()
        val documentationJ =
            """
            |/**
            | * <img src="/path/to/img.jpg" alt="Alt text"/>
            | */
            |public void foo(Integer a)
        """
                .render(java = true)
                .documentation()

        for (documentation in listOf(documentationJ, documentationK)) {
            val description = documentation.last() as DescriptionComponent
            val img = description.data.components.first().children.item() as Img

            assertThat(img.params["href"]).isEqualTo("/path/to/img.jpg")
            assertThat(img.params["alt"]).isEqualTo("Alt text")
        }
    }

    @Test // Developers sometimes use white space to line up documentation
    fun `Parameter documentation works and ignores whitespace`() {
        val documentationK =
            """
            |/**
            | * @param a blah
            | * @param b        bblargh
            | */
            |fun foo(a: Int, b: String)
        """
                .render()
                .documentation()
        val documentationJ =
            """
            |/**
            | * @param a blah
            | * @param b        bblargh
            | */
            |public void foo(Int a, String b)
        """
                .render(java = true)
                .documentation()

        for (documentation in listOf(documentationK, documentationJ)) {
            val paramSummary = documentation.paramList()
            assertThat(paramSummary.items().size).isEqualTo(2)
            val param1 = paramSummary.items().first().data.title
            val param2 = paramSummary.items().last().data.title

            assertThat(param1.data.name).isEqualTo("a")
            assertThat(param2.data.name).isEqualTo("b")
        }
    }

    @Ignore // Upstream dokka does not support inheriting docs from hidden components
    @Test
    fun `Sealed class constructor docs inherit`() {
        val rendered =
            """
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
        """
                .render()
        // sealed classes have hidden constructors
        assertThat((rendered.explicitClasslike("Seele") as DClass).constructors).isEmpty()

        val constructorDoc =
            rendered.documentation({
                (this.explicitClasslike("Ba") as DClass).constructors.single()
            })
        val conParamDoc =
            rendered
                .documentation({
                    (this.explicitClasslike("Ba") as DClass)
                        .constructors
                        .single()
                        .parameters
                        .single()
                })
                .single()

        assertThat(constructorDoc.size).isEqualTo(2)
        assertThat((constructorDoc.first() as DescriptionComponent).text())
            .isEqualTo("seele_constructor_docs")
        val paramsList = constructorDoc.paramList()
        assertThat(paramsList.single().name()).isEqualTo("foo")
        assertThat(paramsList.single().data.description.text()).isEqualTo("seele_foo_docs")
        assertThat((conParamDoc as DescriptionComponent).text()).isEqualTo("seele_foo_docs")
    }

    @Test
    fun `Full documentation has properties`() {
        val description =
            """
            |/** @property bar a barber */
            |val bar: String = "barbarbar
        """
                .render()
                .documentation(doc = { this.packages.single().properties.single() })
                .single() as DescriptionComponent

        assertThat(description.text()).isEqualTo("a barber")
    }

    @Test
    fun `Full class documentation has properties`() {
        val description =
            """
            |class Foo {
            |   /** @property bar a barber */
            |   val bar: String = "barbarbar
            |}
        """
                .render()
                .documentation(
                    doc = { this.packages.single().classlikes.single().properties.single() }
                )
                .single() as DescriptionComponent

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
            """
                .render()
                .documentation()
        }
        assertThat(exception.localizedMessage)
            .isEqualTo(
                "Exception thrown while handling Param tags [Param(root=" +
                    "CustomDocTag(children=[P(children=[Text(body=aaaaaa, children=[], params={})], " +
                    "params={})], params={}, name=MARKDOWN_FILE), name=NOT_A_REAL_PARAM, " +
                    "address=null)]."
            )
        assertThat(exception.cause!!.localizedMessage)
            .isEqualTo(
                "Unable to find what is referred to by \"@param NOT_A_REAL_PARAM\" in " +
                    "DFunction foo, with contents: aaaaaa"
            )
        val standardOut = System.out
        val outputStreamCaptor = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStreamCaptor))
        """
        |/**
        | * @param NOT_A_REAL_PARAM aaaaaa
        | */
        |class Foo { }
        """
            .render()
            .documentation() // for type params and property params
        // val expected = "src/main/kotlin/androidx/example/Test.kt:4 Unable to find reference " +
        val expected =
            "Unable to find reference " + // b/327166311
                "@param NOT_A_REAL_PARAM in DClass Foo. Are you trying to refer to something not " +
                "visible to users?\n"
        assertThat(outputStreamCaptor.toString()).endsWith(expected)
        System.setOut(standardOut)
        assertFails { // for @param in the wrong place
            """
            |/**
            | * @param NOT_A_REAL_PARAM aaaaaa
            | */
            |val foo = "bbb"
            """
                .render()
                .documentation()
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
        """
            .render()
            .documentation()
        // var expected = "src/main/kotlin/androidx/example/Test.kt:4*/ Unable to find reference " +
        var expected =
            "Unable to find reference " + // b/327166311
                "@property NOT_A_REAL_PROPERTY in DClass Foo\n"
        assertThat(outputStreamCaptor.toString()).endsWith(expected)
        System.setOut(PrintStream(outputStreamCaptor))
        """
        |/**
        | * @property NO_PROPERTIES_HERE aaaaaa
        | */
        |class Foo() { }
        """
            .render()
            .documentation()
        // expected = "src/main/kotlin/androidx/example/Test.kt:4*/ Unable to find reference " +
        expected =
            "Unable to find reference " + // 327166311
                "@property NO_PROPERTIES_HERE in DClass Foo"
        assertThat(outputStreamCaptor.toString()).contains(expected)
        assertFails {
            """
            |class Foo() {
            |   /**
            |    * @property NO_PROPERTIES_HERE aaaaaa
            |    */
            |   fun doAThing()
            |}
            """
                .render()
                .documentation() // can't @property on a function
        }
        assertFails {
            """
            |/**
            | * @property NO_PROPERTIES_HERE aaaaaa
            | */
            |fun doAThing()
            """
                .render()
                .documentation() // can't @property on a function
        }
        assertFails {
            """
            |/** @property a */
            |val b
            |val a
            """
                .render()
                .documentation() // @property must be on correct property
        }
        System.setOut(standardOut)
    }

    @Test // REGRESSION: go/dokka-upstream-bug/2388
    fun `Full documentation parameters table has types with nullability annotations`() {
        if (displayLanguage == Language.KOTLIN) return
        val documentationK =
            """
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
        """
                .render()
                .documentation()
        val documentationJ =
            """
            |/**
            | * Called after the current PagedList has been updated.
            | *
            | * @param previousList The previous list of Ts, may be null.
            | * @param currentList The new current list of nullable Ts, may not be null.
            | */
            |public <T> void onCurrentListChanged(
            |            @Nullable java.util.List<@NonNull T> previousList,
            |            @NonNull java.util.List<@Nullable T> currentList) {}
        """
                .render(java = true)
                .documentation()
        for (documentation in listOf(documentationK, documentationJ)) {
            val paramTable = documentation.paramList()
            assertThat(paramTable.size).isEqualTo(2)
            val param0 = paramTable.items().first()
            val param0Left = param0.data.title
            val param0Generic = param0Left.data.type.data.generics.single()
            val param1 = paramTable.items().last()
            val param1Left = param1.data.title
            val param1Generic = param1Left.data.type.data.generics.single()

            assertThat(param0.name()).isEqualTo("previousList")
            assertThat(param0.data.description.text())
                .isEqualTo("The previous list of Ts, may be null.")
            assertThat(param0Left.typeName()).isEqualTo("List")
            assertThat(param0Generic.name()).isEqualTo("T")
            assertThat(param1.name()).isEqualTo("currentList")
            assertThat(param1.data.description.text())
                .isEqualTo("The new current list of nullable Ts, may not be null.")
            assertThat(param0Left.typeName()).isEqualTo("List")
            assertThat(param1Generic.name()).isEqualTo("T")

            javaOnly {
                if (documentation == documentationJ) {
                    assertThat(param0Left.typeAnnotations().single().isAtNullable).isTrue()
                    // assertThat(param1Generic.annotations.single().isAtNullable).isTrue()
                } else {
                    assertThat(param0Left.typeAnnotations()).isEmpty()
                    assertThat(param1Generic.annotations).isEmpty()
                }
                // This is also the upstream bug; T should be @NonNull from both source languages
                if (documentation == documentationK) {
                    assertThat(param0Generic.annotations.single().isAtNonNull).isTrue()
                }
                assertThat(param0Left.annotations).isEmpty()
                assertThat(param1Left.annotations).isEmpty()
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
        val documentation =
            """
            |/**
            | * @param funA a fun
            | * @param funB be fun
            | * @param unt where units go
            | * @param foo a list of foos
            | */
            |fun mega(funA: (funB: ((unt: Unit, foo: List<Boolean>) -> String) -> Int) -> (Double))
        """
                .render()
                .documentation()
        val paramTable = documentation.paramList()
        val funADocs = paramTable.items().single { it.name() == "funA" }.data.description.text()
        assertThat(funADocs).isEqualTo("a fun")
        val funBDocs = paramTable.items().single { it.name() == "funB" }.data.description.text()
        assertThat(funBDocs).isEqualTo("be fun")
        val untDocs = paramTable.items().single { it.name() == "unt" }.data.description.text()
        assertThat(untDocs).isEqualTo("where units go")
        val fooDocs = paramTable.items().single { it.name() == "foo" }.data.description.text()
        assertThat(fooDocs).isEqualTo("a list of foos")
    }

    @Test
    fun `Parameter documentation works on generic and lambda types`() {
        val documentation =
            """
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
            |     */
            |    open fun <ToValue : String> map(function: (Value) -> ToValue): Factory<Key, ToValue>
            |}
        """
                .render()
                .documentation()
        val paramTable = documentation.paramList()

        // We don't want Value and/or Key to appear listed as parameters for map().
        assertThat(paramTable.size).isEqualTo(2)

        val param0 = paramTable.items().first()
        val param0Left = param0.data.title
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
            val param0LambdaArgumentType =
                lambdaSymbol.data.lambdaParams.map { it.data.type.link().name }
            assertThat(param0LambdaArgumentType).isEqualTo(listOf("Value"))
        }
        assertThat(param0.data.description.text())
            .isEqualTo(
                "Function that runs on each " +
                    "loaded item, returning items of a potentially new type."
            )
        assertThat(param1Left.data.name).isEqualTo("ToValue")
        assertThat(param1Left.projectionName()).isEqualTo("String")
        assertThat(param1.data.description.text())
            .isEqualTo(
                "Type of items produced by the " + "new DataSource, from the passed function."
            )
    }

    @Test
    fun `Copied from PagingData`() {
        val documentation =
            """
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
        """
                .render()
                .documentation()
        val seeAlsoTable =
            documentation.first { (it as? DocsSummaryList)?.title() == "See also" }
                as DocsSummaryList
        assertThat((seeAlsoTable.single().data.title as Link).data.name).isEqualTo("filter")
    }

    @Test // https://github.com/Kotlin/dokka/issues/3658; b/342557694
    fun `Annotation in code block isn't eaten by parser, Test from androidx-media`() {
        val documentationJ =
            """
            |/**
            | * <p>In a Java class:
            | *
            | * <pre>{@code
            | * import androidx.annotation.OptIn;
            | * import androidx.media3.common.util.UnstableApi;
            | * ...
            | * @OptIn(markerClass = UnstableApi.class)
            | * private void methodUsingUnstableApis() { ... }
            | * }</pre>
            | */
            |public static void foo() {}
            """
                .trimIndent()
                .render(java = true)
                .documentation()
        val documentationK =
            """
            |/**
            | * <p>In a Java class:
            | *
            | * ```
            | * import androidx.annotation.OptIn;
            | * import androidx.media3.common.util.UnstableApi;
            | * ...
            | * @OptIn(markerClass = UnstableApi.class)
            | * private void methodUsingUnstableApis() { ... }
            | * ```
            | */
            |fun foo() {}
            """
                .trimIndent()
                .render()
                .documentation()
        for (doc in listOf(/*documentationJ, */ documentationK)) {
            assertThat(doc.toString()).contains("@OptIn")
        }
    }

    @Test
    fun `a href works in javadoc but not kdoc`() {
        val documentationJ =
            """
            /**
             * <a href="https://google.com">google</a>
             * <a href="https://m3.material.io/components/time-pickers/overview" class="external"
             * target="_blank">Material Design time picker</a>.
             */
            public static void foo() {}
        """
                .render(java = true)
                .documentation()
        val documentationK =
            """
            /**
             * [google](https://google.com)
             * [Material Design time picker](https://m3.material.io/components/time-pickers/overview)
             * <a href="https://google.com">google</a>
             * <a href="https://m3.material.io/components/time-pickers/overview" class="external"
             * target="_blank">Material Design time picker</a>.
             */
            public fun foo(): Unit {}
        """
                .render()
                .documentation()
        for (documentation in listOf(documentationJ, documentationK)) {
            val fooDesc = (documentation.single() as DescriptionComponent).data.components.single()
            // documentationK has a " " element between the first two links, for some reason.
            val fooLinks = fooDesc.childrenOfType<A>()
            // the `<a href`s in kdoc do not become A's/Links.
            assertThat(fooLinks).hasSize(2)
            assertThat(fooLinks[0])
                .isEqualTo(
                    A(
                        children = listOf(Text("google")),
                        params = mapOf("href" to "https://google.com"),
                    )
                )
            assertThat(fooLinks[1])
                .isEqualTo(
                    A(
                        children = listOf(Text("Material Design time picker")),
                        params =
                            mapOf(
                                "href" to "https://m3.material.io/components/time-pickers/overview"
                            ),
                    )
                )
        }
    }

    @Ignore // go/dokka-upstream-bug/2665
    @Test
    fun `Copied from draganddrop`() {
        val documentationK =
            """
            | /**
            | * <p><b>Note:</b> This class requires Android API level 24 or higher.
            | *
            | * @see <a href="https://developer.android.com/guide/topics/ui/drag-drop">Drag and drop</a>
            | * @see <a href="https://developer.android.com/guide/topics/large-screens/multi-window-support#dnd">
            | *      Multi-window support</a>
            | */
            |public final class DropHelper { }
        """
                .render()
                .documentation()
        val seeAlsoTable =
            documentationK.first { (it as? DocsSummaryList)?.title() == "See also" }
                as DocsSummaryList
        val (see1, see2) = seeAlsoTable.items(2)
        assertThat(see1.link().name).isEqualTo("Drag and drop")
        assertThat(see1.link().url)
            .isEqualTo("https://developer.android.com/guide/topics/ui/drag-drop")
        assertThat(see2.link().name).isEqualTo("Multi-window support")
        assertThat(see2.link().url)
            .isEqualTo(
                "https://developer.android.com/guide/topics/large-screens/multi-window-support#dnd"
            )
    }

    @Test
    fun `Documentation spacing works over multiple lines`() {
        val documentationK =
            """
            |/**
            | * This is a multi-line documentation string. There is no space at the end of the
            | * first line, but "the first" with no space should not appear in the final documentation.
            | */
            | fun foo(): String
        """
                .render()
                .documentation()
                .first() as DescriptionComponent
        val documentationJ =
            """
            |/**
            | * This is a multi-line documentation string. There is no space at the end of the
            | * first line, but "the first" with no space should not appear in the final documentation.
            | */
            | public String foo()
        """
                .render(java = true)
                .documentation()
                .first() as DescriptionComponent

        for (documentation in listOf(documentationK, documentationJ)) {
            assertThat(documentation.text()).doesNotContain("thefirst")
        }
    }

    @Test
    fun `Test several odd link choices`() {
        val documentation =
            """
            |class Foo
            |/**
            | * Does bar on [this]
            | */
            |fun Foo.bar() {}
        """
                .render()
                .documentation()
                .single { (it as DescriptionComponent).text().contains("Does bar on") }
        val docChildren = (documentation as DescriptionComponent).data.components.single().children
        assertThat(docChildren.size).isEqualTo(2)
        val link = (docChildren.last() as DocumentationLink)
        assertThat((link.children.single() as Text).body).isEqualTo("this")
        assertThat(link.dri.packageName).isEqualTo("androidx.example")
        assertThat(link.dri.classNames).isEqualTo("Foo")
        assertThat(link.dri.callable).isNull()
    }

    @Test
    fun `Test line break inside img tag`() {
        val module =
            """
            |/**
            | * Line above image
            | * ![Assist chip
            | * image](https://developer.android.com/images/reference/androidx/compose/material3/assist-chip.png)
            | *
            | */
            |fun Foo.bar() {}
            ||class Foo(
            |    /**
            |     * Line above image
            |     * ![Assist chip
            |     * image](https://developer.android.com/images/reference/androidx/compose/material3/assist-chip.png)
            |     *
            |     */
            |    val baz: String
        """
                .render()

        val documentationBar =
            (module.documentation().first() as DescriptionComponent).data.components.first()
        assertThat(documentationBar.children[0].text()).isEqualTo("Line above image ")
        val barImgTag = documentationBar.children[1] as Img
        assertThat(barImgTag.params)
            .containsExactly(
                "href",
                "https://developer.android.com/images/reference/androidx/compose/material3/assist-chip.png",
                "alt",
                "Assist chip\nimage",
            )

        val documentationBaz =
            (module.documentation({ this.property("baz")!! }).first() as DescriptionComponent)
                .data
                .components
                .first()
        assertThat(documentationBaz.children[0].text()).isEqualTo("Line above image ")
        val bazImgTag = documentationBar.children[1] as Img
        assertThat(bazImgTag.params)
            .containsExactly(
                "href",
                "https://developer.android.com/images/reference/androidx/compose/material3/assist-chip.png",
                "alt",
                "Assist chip\nimage",
            )
    }

    @Test
    fun `Test doc comment on property parameter`() {
        val module =
            """
            |/**
            | * comments on class Foo
            | * @param bar doc comment on bar as a parameter.
            | */
            |class Foo(
            |    /**
            |     * Inline doc comment on bar
            |     */
            |    public var bar: String
            |) {}
        """
                .render()
        val propertyDoc =
            module.documentation({ this.property("bar")!! }).single() as DescriptionComponent
        assertThat(propertyDoc.toString()).doesNotContain("doc comment on bar as a parameter.")
        assertThat(propertyDoc.toString()).contains("Inline doc comment on bar")
        val constructorDoc = module.documentation({ this.constructor() }).last() as DocsSummaryList
        assertThat(constructorDoc.toString()).contains("doc comment on bar as a parameter.")
        assertThat(constructorDoc.toString()).doesNotContain("Inline doc comment on bar")
        val classDoc = module.documentation().last() as DescriptionComponent
        assertThat(classDoc.toString()).doesNotContain("doc comment on bar as a parameter.")
        assertThat(classDoc.toString()).doesNotContain("Inline doc comment on bar")
    }

    @Test
    fun `See tag with URL`() {
        val documentationK =
            """
            |/**
            | * @see <a href=https://google.com>google</a>
            | */
            |fun foo() {}
        """
                .render()
                .documentation()
        val documentationJ =
            """
            |/**
            | * @see <a href=https://google.com>google</a>
            | */
            |public void foo() {}
        """
                .render(java = true)
                .documentation()
        for (documentation in listOf(documentationJ, documentationK)) {
            val seeAlso = (documentation.last() as LinkDescriptionSummaryList).single()
            val link = seeAlso.data.title as Link
            assertThat(link.data.url).isEqualTo("https://google.com")
        }
    }

    @Test
    fun `See tag with URL and additional text`() {
        val documentationK =
            """
            |/**
            | * @see
            | * <a href=https://google.com>google</a>
            | *
            | * Some additional text here.
            | */
            |fun foo() {}
        """
                .render()
                .documentation()
        val documentationJ =
            """
            |/**
            | * @see
            | * <a href=https://google.com>google</a>
            | *
            | * Some additional text here.
            | */
            |public void foo() {}
        """
                .render(java = true)
                .documentation()
        for (documentation in listOf(documentationJ, documentationK)) {
            val seeAlso = (documentation.last() as LinkDescriptionSummaryList).single()
            val link = seeAlso.data.title as Link
            assertThat(link.data.url).isEqualTo("https://google.com")
            val description = seeAlso.data.description
            assertThat(description.text()).contains("Some additional text here.")
        }
    }

    @Test
    fun `Multiline doc from fragment, with formatting`() {
        val module =
            """
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
        """
                .render(java = true)
        val paramDocText =
            (module
                    .documentation(
                        doc = { this.function()!!.parameters.single { it.name == "parent" } },
                        isFromJava = true,
                    )
                    .first() as DescriptionComponent)
                .text()

        assertThat(paramDocText).doesNotContain("placedin")
    }

    @Test
    fun `@deprecated description works over multiple lines`() {
        val documentation =
            """
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
        """
                .render(java = true)
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
        val documentation =
            """
            |/**
            | * Below is a sample of a simple database.
            | * <pre>
            | * // File: Song.java
            | * {@literal @}Entity
            | * public class Song {
            | */
            |public void foo(){}
        """
                .render(java = true)
        val doc = documentation.documentation()
        val functionDesc = doc.first() as DescriptionComponent
        assertThat(functionDesc.text())
            .isEqualTo(
                "Below is a sample of a simple database." +
                    "  // File: Song.java\n @ Entity\npublic class Song {"
            )
        assertThat(functionDesc.data.components.last()).isInstanceOf(Pre::class.java)
    }

    @Test
    fun `Full documentation has receiver param`() {
        val documentation =
            """
            |/** @receiver blah */
            |fun Int.foo()
        """
                .render()
                .documentation()

        val paramSummary = documentation.paramList()
        val paramParam = paramSummary.item().data.title.data

        javaOnly { assertThat(paramParam.name).isEqualTo("receiver") }
        kotlinOnly { assertThat(paramParam.name).isEqualTo("") }
    }

    @Test
    fun `Full documentation has return type`() {
        val documentation =
            """
            |/** @return blah */
            |fun foo() = Unit
        """
                .render()
                .documentation()

        val paramSummary = documentation.last() as WithDescriptionList<TypeProjectionComponent>
        val returns = paramSummary.item()

        assertThat(paramSummary.title()).isEqualTo("Returns")
        assertThat(returns.data.title is NoopTypeProjectionComponent).isTrue()
    }

    @Test
    fun `Full Kotlin documentation has thrown exceptions`() {
        val documentationK =
            """
            |/** @throws IllegalStateException if it fails */
            |fun foo()
        """
                .render()
                .documentation()
        val documentationJ =
            """
            |/** @throws IllegalStateException if it fails */
            |public void foo() {}
        """
                .render(java = true)
                .documentation()

        for (documentation in listOf(documentationJ, documentationK)) {
            val expectedDRI =
                if (documentation == documentationJ) {
                    "java.lang.IllegalStateException"
                } else {
                    "IllegalStateException"
                }

            val expectedURL =
                if (documentation == documentationJ) {
                    "https://developer.android.com" +
                        "/reference/java/lang/IllegalStateException.html"
                } else
                    "https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/" +
                        "-illegal-state-exception/index.html"

            val throwsSummary =
                documentation.first { (it as? DocsSummaryList)?.title() == "Throws" }
                    as DocsSummaryList
            val throwsTypeAsParam = throwsSummary.item().data.title
            val throwsTypeAsLink = throwsTypeAsParam.data.type.data.type
            val throwsDescription = throwsSummary.item().data.description
            val throwsDescText =
                throwsDescription.data.components.single().children.single() as Text

            assertThat(throwsTypeAsParam.data.name).isEqualTo("")
            assertThat(throwsTypeAsLink.data.name).contains(expectedDRI)
            assertThat(throwsTypeAsLink.data.url).isEqualTo(expectedURL)
            assertThat(throwsDescText.body).isEqualTo("if it fails")
        }
    }

    @Test
    fun `Bad throws tags generate warnings but not errors`() {
        val standardOut = System.out
        val outputStreamCaptor = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStreamCaptor))
        fun DModule.throwsTable() =
            documentation().first { (it as? DocsSummaryList)?.title() == "Throws" }
                as DocsSummaryList
        val moduleJ1 =
            """
            |/**
            | * @throws {@link IllegalStateException} if I try to linkify
            | */
            |public void foo() {}
        """
                .render(java = true)
        val moduleJ2 =
            """
            |/**
            | * @throws an IllegalStateException if my syntax is bad
            | */
            |public void foo() {}
        """
                .render(java = true)
        val moduleJ3 =
            """
            |/**
            | * @throws IOException if file operations fail
            | */
            |public void foo() {}
        """
                .render(java = true)
        val moduleK1 =
            """
            |/**
            | * @throws an IllegalStateException if my syntax is bad
            | */
            |fun foo()
        """
                .render()
        val moduleK2 =
            """
            |/**
            | * @throws [IllegalStateException] but this one is actually fine, it turns out
            | */
            |fun foo()
        """
                .render()
        val moduleK3 =
            """
            |/**
            | * @throws IOException if file operations fail
            | */
            |fun foo()
        """
                .render()

        val exception1 = assertFails { moduleJ1.throwsTable().item() }
        outputStreamCaptor.reset()
        val exception2 = assertFails { moduleJ2.throwsTable().item() }
        outputStreamCaptor.reset()
        val exception3 = assertFails { moduleK1.throwsTable().item() }
        outputStreamCaptor.reset()
        val throwsBad4 = moduleJ3.throwsTable().item()
        assertThat(outputStreamCaptor.toString())
            .contains("The general fix for these is to fully qualify the exception name")
        outputStreamCaptor.reset()
        val throwsBad5 = moduleK3.throwsTable().item()
        assertThat(outputStreamCaptor.toString())
            .contains("The general fix for these is to fully qualify the exception name")
        outputStreamCaptor.reset()
        val throwsFine1 = moduleK2.throwsTable().item()
        assertThat(outputStreamCaptor.toString()).isEmpty()

        for (exception in listOf(exception1, exception2, exception3)) {
            assertThat(exception.localizedMessage)
                .contains(
                    "Exception thrown while handling Throws tags " +
                        "[Throws(root=CustomDocTag(children=[P(children=[Text(body="
                )
        }
        assertThat(exception1.localizedMessage)
            .contains(
                "if I try to linkify, children=[], params={})], params={})], params={}, name=MARKDOWN" +
                    "_FILE), name={@link IllegalStateException}, exceptionAddress=null)]."
            )
        assertThat(exception2.localizedMessage)
            .contains(
                "IllegalStateException if my syntax is bad, children=[], params={})], params={})], " +
                    "params={}, name=MARKDOWN_FILE), name=an, exceptionAddress=null)]."
            )
        assertThat(exception3.localizedMessage)
            .contains(
                "IllegalStateException if my syntax is bad, children=[], params={})], params={})], " +
                    "params={}, name=MARKDOWN_FILE), name=an, exceptionAddress=null)]."
            )
        for (throws in listOf(throwsBad4, throwsBad5)) {
            assertThat(throws.name()).isEqualTo("")
            assertThat(throws.data.title.typeName()).isEqualTo("IOException")
            assertThat(throws.data.title.link().url).isEqualTo("")
        }

        assertThat(throwsBad4.data.description.text()).isEqualTo("if file operations fail")
        assertThat(throwsBad5.data.description.text()).isEqualTo("if file operations fail")

        assertThat(throwsFine1.data.description.text())
            .isEqualTo("but this one is actually fine, it turns out")
        assertThat(throwsFine1.name()).isEqualTo("")
        assertThat(throwsFine1.data.title.typeName()).isEqualTo("IllegalStateException")
        assertThat(throwsFine1.data.title.link().url)
            .isEqualTo(
                "https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/" +
                    "-illegal-state-exception/index.html"
            )

        System.setOut(standardOut)
    }

    @Test
    fun `Full Java documentation has checked exceptions`() {
        val documentation =
            """
            |/** IllegalStateException if it fails */
            |public void foo() throws IllegalStateException {}
        """
                .render(java = true)
                .documentation()

        val expectedDRI = "java.lang.IllegalStateException"
        val expectedURL =
            "https://developer.android.com/reference/java/lang/" + "IllegalStateException.html"

        val throwsSummary =
            documentation.first { (it as? DocsSummaryList)?.title() == "Throws" } as DocsSummaryList
        val throwsTypeAsParam = throwsSummary.item().data.title
        val throwsTypeAsLink = throwsTypeAsParam.data.type.data.type
        val throwsDescription = throwsSummary.item().data.description

        assertThat(throwsTypeAsParam.data.name).isEqualTo("")
        assertThat(throwsTypeAsLink.data.name).contains(expectedDRI)
        assertThat(throwsTypeAsLink.data.url).isEqualTo(expectedURL)
        assertThat(throwsDescription.data.components).isEmpty()
    }

    @Test
    fun `Full documentation has see alsos`() {
        val documentation =
            """
            |/** @see String blah */
            |fun foo()
        """
                .render()
                .documentation()

        val paramSummary = documentation.last() as LinkDescriptionSummaryList
        val paramText = paramSummary.item()

        assertThat(paramSummary.title()).isEqualTo("See also")
        assertThat(paramText.link().name).isEqualTo("String")
        assertThat(paramText.data.description.text()).isEqualTo("blah")
    }

    @Test
    fun `See also parses external link`() {
        val documentation =
            """
            |/** @see String */
            |class Foo
        """
                .render()
                .documentation()

        val paramSummary = documentation.last() as LinkDescriptionSummaryList
        val paramText = paramSummary.item()

        assertThat(paramText.link().url)
            .isEqualTo("https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html")
        assertThat(paramText.data.description.text()).isEmpty()
    }

    @Test
    fun `See also parses internal link`() {
        val documentation =
            """
            |/** @see Bar */
            |class Foo { class Bar }
        """
                .render()
                .documentation()

        val paramSummary = documentation.last() as LinkDescriptionSummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "androidx/example/Foo.Bar.html")
        assertThat(paramText.data.description.text()).isEmpty()
    }

    @Ignore // Our package-list is incomplete. b/225929725
    @Test
    fun `See also parses link with Kotlin style function`() {
        val documentation =
            """
            |/**
            | * @see String.isEmpty
            | * @see String.lastIndex
            | */
            |class Foo
        """
                .render()
                .documentation()

        val paramSummary = documentation.last() as DocsSummaryList
        val (paramText1, paramText2) = paramSummary.items(2).toList()

        // TODO(b/167437580): figure out how to reliably parse links
        assertThat(paramText1.link().url).contains("isEmpty")
        assertThat(paramText1.data.description.text()).isEmpty()
        assertThat(paramText2.link().url).contains("lastIndex")
        assertThat(paramText2.data.description.text()).isEmpty()
    }

    @Test
    fun `See also parses link with Java style function`() {
        val documentation =
            """
            |/** @see String#isEmpty() */
            |public void foo() {}
        """
                .render(java = true)
                .documentation()

        val paramSummary = documentation.last() as LinkDescriptionSummaryList
        val paramText = paramSummary.item()

        assertThat(paramText.link().url)
            .isEqualTo("https://developer.android.com/reference/java/lang/String.html#isEmpty()")
        assertThat(paramText.data.description.text()).isEmpty()
    }

    @Test
    fun `See also parses link with unresolved function`() {
        val documentation =
            """
            |/** @see com.example.foo.Foo#bar() */
            |public void foo() {}
        """
                .render(java = true)
                .documentation()

        val paramSummary = documentation.last() as LinkDescriptionSummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "com/example/foo/Foo.html#bar()")
    }

    @Test
    fun `See also parses link with class`() {
        val documentation =
            """
            |/** @see String */
            |public void foo() {}
        """
                .render(java = true)
                .documentation()

        val paramSummary = documentation.last() as LinkDescriptionSummaryList
        val paramText = paramSummary.item()

        assertThat(paramText.link().url)
            .isEqualTo("https://developer.android.com/reference/java/lang/String.html")
    }

    @Test
    fun `See also parses link with unresolved class`() {
        val documentation =
            """
            |/** @see com.example.foo.Foo */
            |public void foo() {}
        """
                .render(java = true)
                .documentation()

        val paramSummary = documentation.last() as LinkDescriptionSummaryList
        val paramText = paramSummary.item()

        assertPath(paramText.link().url, "com/example/foo/Foo.html")
    }

    @Test
    fun `Full documentation sorts tags in pre-defined order`() {
        val documentation =
            """
            |/**
            | * @see String
            | * @param a
            | * @return
            | */
            |fun foo(a: String)
        """
                .render()
                .documentation()

        val returnSummary = documentation[1] as DocsSummaryList
        val paramSummary = documentation[2] as DocsSummaryList
        val seeSummary = documentation[3] as DocsSummaryList

        assertThat(returnSummary.title()).isEqualTo("Returns")
        assertThat(paramSummary.title()).isEqualTo("Parameters")
        assertThat(seeSummary.title()).isEqualTo("See also")
    }

    @Test
    fun `Full documentation sorts params in given order`() {
        val expected = listOf("a", "b", "c")
        val documentation =
            """
            |/**
            | * @param b
            | * @param c
            | * @param a
            | */
            |fun foo(a: String, b: String, c: String)
        """
                .render()
                .documentation(paramNames = expected)

        val paramSummary = documentation.last() as DocsSummaryList
        val params = paramSummary.items(3)

        for ((i, param) in params.withIndex()) {
            assertThat(param.data.title.data.name).isEqualTo(expected[i])
        }
    }

    @Test
    fun `Param with same name as function is correctly documented`() {
        val expected = listOf("foo", "bar")
        val documentation =
            """
        |/**
        | * @param foo a parameter with the same name as the function
        | * @param bar a parameter with a different name
        | */
        |fun foo(foo: String, bar: String)
    """
                .render()
                .documentation(paramNames = expected)

        val paramSummary = documentation.last() as DocsSummaryList
        val params = paramSummary.items(2)

        for ((i, param) in params.withIndex()) {
            assertThat(param.data.title.data.name).isEqualTo(expected[i])
        }
    }

    @Test
    fun `Warning on missing @param documentation`() {
        val standardOut = System.out
        val outputStreamCaptor = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStreamCaptor))
        """
            |/**
            | * @param b
            | * @param c
            | */
            |fun foo(a: String, b: String, c: String)
        """
            .render()
            .documentation()

        // val expected = "src/main/kotlin/androidx/example/Test.kt:5 Missing @param tag for " +
        val expected =
            "Missing @param tag for " + // b/327166311
                "parameter `a` in DFunction foo\n"
        assertThat(outputStreamCaptor.toString()).endsWith(expected)
        System.setOut(standardOut)
    }

    @Test
    fun `@sample annotation in kotlin fails if the target samples doesn't exist`() {
        val documentation =
            """
            |/**
            | * a very foo description
            | *
            | * @sample foo.samples.fooSample
            | */
            |fun foo(a: String, b: String, c: String)
            """
                .trimIndent()
        assertFails { documentation.render().documentation() }
    }

    @Test
    fun `Verify that several real code samples don't give warnings`() {
        val standardOut = System.out
        val outputStreamCaptor = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStreamCaptor))
        val module =
            """
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
        """
                .render()
        val converterHolder = ConverterHolder(this@DocTagConverterTest, module)
        runBlocking {
            converterHolder
                .NonKmpClasslikeConverter(module.explicitClasslike("DynamicNavGraphBuilder"))
                .classlike()
        }
        runBlocking {
            converterHolder
                .NonKmpClasslikeConverter(module.explicitClasslike("ParcelableArrayType"))
                .classlike()
        }
        val filteredOutput =
            outputStreamCaptor
                .toString()
                .split("\n")
                .filterNot { it.startsWith("WARN: Couldn't resolve link for") }
                .joinToString("\n")
        assertThat(filteredOutput).doesNotContain("WARN")
        System.setOut(standardOut)
    }

    @Test
    fun `Test @return on property parameters`() {
        val module =
            """
            |public class Foo private constructor(
            |   /**
            |    * The arguments used for this entry
            |    * @return The arguments used when this entry was created
            |    *
            |    */
            |   @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            |   public var arguments: List<String>? = null
            |) {}
        """
                .render()
        val documentation = module.documentation({ this.property("arguments")!! })
        val description = (documentation.first() as DescriptionComponent).text()
        assertThat(description).isEqualTo("The arguments used for this entry")
        val table = (documentation.last() as DocsSummaryList)
        assertThat(table.title()).isEqualTo("Returns")
        val tableEntry = table.item().data.description.text()
        assertThat(tableEntry).isEqualTo("The arguments used when this entry was created")
    }

    @Test // Note: this tests upstream behavior.
    fun `Test spacing around line wraps including inside tags`() {
        val documentation =
            """
            |/**
            | * blah blah blah blah {@link #longFunctionName(java.lang.Object, java.lang.String, int,
            | * java.lang.Integer) LongContainingClassName#longFunctionName(Object, String, int,
            | * Integer)}
            | */
            |public void longFunctionName(Object arg1, String arg2, int arg3, Integer arg4) {}
            |
        """
                .render(java = true)
                .documentation({ this.function("longFunctionName")!! })
        val components =
            (documentation.single() as DescriptionComponent).data.components.single().children
        val link = components[1] as DocumentationLink
        assertThat(link.text()).doesNotContain("int,Integer") // should be a space here
    }

    @Test
    fun `from ExoPlayer2, @link split across lines works`() {
        val documentation =
            """
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
        """
                .render(java = true)
                .documentation({ this.explicitClasslike("PlaybackSuppressionReason") })
        val components =
            (documentation.single() as DescriptionComponent).data.components.single().children
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
        assertThat(link2.text()).isEqualTo("PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS")
    }

    @Test
    fun `From Platform, java-concurrent-future, pre{@code formatting test`() {
        val documentation =
            """
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
             * @since 1.5
             * @author Doug Lea
             * @param <V> The result type returned by this Future's {@code get} method
             */
            public interface Future<V>
            """
                .trimIndent()
                .render(java = true)
                .documentation({ this.explicitClasslike("Future") })
        val description = documentation[0] as DescriptionComponent
        val firstCodeBlock = (description.data.components[2] as CodeBlock).text()
        assertThat(firstCodeBlock).doesNotContain("&lt;")
        assertThat(firstCodeBlock).doesNotContain("<code>")
        val theLink = (description.data.components[15] as P).children[1] as A
        assertThat(theLink.params["href"]).isEqualTo("package-summary.html#MemoryVisibility")
    }

    @Ignore // b/278255321; go/dokka-upstream-bug/2949
    @Test
    fun `Test @value tag`() {
        val module =
            """
            |/** An at-value tag: {@value VALUE_1}. */
            |public enum Foo { VALUE_1, VALUE_2 }
        """
                .render(java = true)
        val documentation = module.documentation({ this.explicitClasslike("Foo") })
        val description = (documentation.first() as DescriptionComponent).text()
        assertThat(description).doesNotContain("{@value")
    }

    @Ignore("b/396171398, go/dokka-upstream-bug/4048")
    @Test
    fun `Test @inheritDoc and @code`() {
        val module =
            """
            |/** {@code inherited code} */
            |public class ParentClass {}
            |/** {@inheritDoc} */
            |public class ChildClass extends ParentClass {}
            """
                .render(java = true)
        val parentDocumentation = module.documentation({ explicitClasslike("ParentClass") })
        val parentCode =
            (parentDocumentation.first() as DescriptionComponent)
                .data
                .components
                .flatMap { it.childrenOfType<CodeInline>() }
                .singleOrNull()
        assertThat(parentCode).isNotNull()
        assertThat(parentCode!!.text()).isEqualTo("inherited code")

        // Test fails here: there is no CodeInline, and the text is "inherited code}"
        val childDocumentation = module.documentation({ explicitClasslike("ChildClass") })
        val childCode =
            (childDocumentation.first() as DescriptionComponent)
                .data
                .components
                .flatMap { it.childrenOfType<CodeInline>() }
                .singleOrNull()
        assertThat(childCode).isNotNull()
        assertThat(childCode!!.text()).isEqualTo("inherited code")
    }

    private fun DModule.description(
        doc: DModule.() -> Documentable = ::smartDoc
    ): DescriptionComponent {
        val converterHolder = ConverterHolder(this@DocTagConverterTest, this)
        val annotations =
            this.doc().annotations(getExpectOrCommonSourceSet()).deprecationAnnotation()
        return converterHolder.javadocConverter.summaryDescription(this.doc(), annotations)
    }

    private fun DModule.documentation(
        doc: DModule.() -> Documentable = ::smartDoc,
        paramNames: List<String> = emptyList(),
        isFromJava: Boolean? = null,
    ): List<ContextFreeComponent> {
        val converterHolder = ConverterHolder(this@DocTagConverterTest, this)
        val doc = doc()
        return if (doc is WithSources) {
            converterHolder.javadocConverter.metadata(
                documentable = doc,
                returnType = NoopTypeProjectionComponent(""),
                paramNames = paramNames,
            )
        } else
            converterHolder.javadocConverter.metadata(
                documentable = doc,
                returnType = NoopTypeProjectionComponent(""),
                paramNames = paramNames,
                isFromJava = isFromJava!!,
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

    private fun ContextFreeComponent.render() =
        createHTML().body { this@render.render(this) }.trim()

    private fun List<ContextFreeComponent>.paramList() =
        first { (it as? DocsSummaryList)?.title() == "Parameters" } as DocsSummaryList

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Language.JAVA), arrayOf(Language.KOTLIN))
    }
}
