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
import com.google.devsite.components.Raw
import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.table.SingleColumnSummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.content
import com.google.devsite.renderer.converters.testing.description
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.projectionName
import com.google.devsite.renderer.converters.testing.size
import com.google.devsite.renderer.converters.testing.summary
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.renderer.converters.testing.title
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DModule
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertFails

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

        assertThat(summary.item().summary().name()).isEqualTo("foo")
    }

    @Test
    fun `@jvmName functions get documented and sorted by correct name`() {
        val page = """
            |class Foo {
            |    @JvmName("bar")
            |    fun foo() = Unit
            |
            |    @JvmName("aar")
            |    fun zoo() = Unit
            |}
        """.render().page()

        javaOnly {
            val classlike = page.content<Classlike>()
            val (summary) = classlike.symbolsFor("Public methods")
            assertThat(summary.items().first().summary().name()).isEqualTo("aar")
            assertThat(summary.items().last().summary().name()).isEqualTo("bar")
        }
        kotlinOnly {
            val classlike = page.content<Classlike>()
            val (summary) = classlike.symbolsFor("Public functions")
            assertThat(summary.items().first().summary().name()).isEqualTo("foo")
            assertThat(summary.items().last().summary().name()).isEqualTo("zoo")
        }
    }

    @Test
    fun `Properties are documented in sorted order`() {
        val expected = listOf("a", "b", "c")
        val documentation = """
            |class Foo {
            |   /** @property b b_doc */
            |   public val b: String
            |   /** @property c c_doc */
            |   public val c: String
            |   /** @property a a_doc */
            |   public val a: String
            |}
        """.render().page()

        val (propertiesSummary) = documentation.content<Classlike>()
            .symbolsFor("Public fields", "Public properties")
        val props = propertiesSummary.items(3)

        for ((i, prop) in props.withIndex()) {
            assertThat((prop.data.description as SymbolSummary).name()).isEqualTo(expected[i])
        }
    }

    @Test
    fun `@JvmSynthetic methods are not documented in java`() {
        val page = """
            |class Foo {
            |    fun foo() = Unit
            |
            |    @JvmSynthetic
            |    fun zoo() = Unit
            |}
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsFor("Public functions", "Public methods")

        javaOnly {
            assertThat(summary.items().size).isEqualTo(1)
        }
        kotlinOnly {
            assertThat(summary.items().size).isEqualTo(2)
        }
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

        assertThat(summary.item().summary().name()).isEqualTo("foo")
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

        assertThat(summary.item().summary().name()).isEqualTo("foo")
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

        assertThat(summary.item().summary().name()).isEqualTo("foo")
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
    fun `Class signature appears with extends and implements for internal types in 4x`() {
        val pageK = """
            |interface A
            |abstract class B
            |class Foo : A, B
        """.render().page("Foo")
        val pageJ = """
            |public interface A {}
            |public abstract class B {}
            |public class Foo extends Test.B implements Test.A {}
        """.render(java = true).page("Foo")

        for (page in listOf(pageJ, pageK)) {
            val prefix = if (page == pageK) "" else "Test."
            val classSignature = page.content<Classlike>().data.signature.data
            assertThat(classSignature.type).isEqualTo("class")
            assertThat(classSignature.extends.single().data.name).isEqualTo("${prefix}B")
            assertThat(classSignature.implements.single().data.name).isEqualTo("${prefix}A")
        }
    }

    @Ignore // b/170124934
    @Test
    fun `Class signature appears with extends or implements for external types in 4x`() {
        val pageExternalK = """
            |/**
            | * An implementation of [Lazy] used by [android.app.Activity.navArgs] and
            | * [androidx.fragment.app.Fragment.navArgs].
            | *
            | * [argumentProducer] is a lambda that will be called during initialization to provide
            | * arguments to construct an [Args] instance via reflection.
            | */
            |public class NavArgsLazy<Args : String>(
            |    private val navArgsClass: KClass<Args>,
            |    private val argumentProducer: () -> Bundle
            |) : Lazy<Args> {
        """.render().page(name = "NavArgsLazy")
        val pageExternalJ = """
            |public class JavaArgsLazy<Args extends String>() implements Lazy<Args> {}
        """.render(java = true).page(name = "NavArgsLazy")
    }

    @Test
    fun `Primary constructor can be @suppress-ed without hiding the class itself`() {
        // Primary constructor suppression is broken upstream
        // https://github.com/Kotlin/dokka/issues/1953
        val modulePrimary = """
            |public class BenchmarkState
            |    /** @suppress */
            |    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            |    constructor(val foo: String) {
            |}
        """.render()
        assertFails {
            val page = modulePrimary.page("BenchmarkState")
        }
        // We can convert to equivalent secondary constructor and it works
        val moduleSecondary = """
            |public class BenchmarkState {
            |    val foo: String
            |    /** @suppress */
            |    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            |    constructor(foo: String) { this.foo = foo }
            |}
        """.render()
        val page = moduleSecondary.page("BenchmarkState").content<Classlike>()
        assertThat(page.symbolsFor("Public constructors").first.size()).isEqualTo(0)
    }

    @Test
    fun `Enum class is rendered and has enum values with types in 4x Kotlin and Java`() {
        val pageK = """
            |/**
            | * class level docs
            | */
            |enum class AnEnumType {
            |    /**
            |     * content being refreshed, which can be a result of
            |     * invalidation, refresh that may contain content updates, or the initial load.
            |     */
            |    REFRESH,
            |    /**
            |     * Load at the start
            |     */
            |    PREPEND,
            |    /**
            |     * Load at the end.
            |     */
            |    APPEND
            |
            |    fun foo()
            |}
        """.render().page(name = "AnEnumType")
        val pageJ = """
            |/**
            | * class level docs
            | */
            |public enum AnEnumType {
            |    /**
            |     * content being refreshed, which can be a result of
            |     * invalidation, refresh that may contain content updates, or the initial load.
            |     */
            |    REFRESH,
            |    /**
            |     * Load at the start
            |     */
            |    PREPEND,
            |    /**
            |     * Load at the end.
            |     */
            |    APPEND
            |
            |    fun foo()
            |}
        """.render(java = true).page(name = "AnEnumType")

        for (page in listOf(pageK, pageJ)) {
            val classlike = page.content<Classlike>()
            val signature = classlike.data.signature.data
            val description = (classlike.data.description.first() as Description)

            val (enumSummary, enumDetails) = classlike.data.symbolTypes.first {
                (it.first as? SummaryList)?.title() == "Enum Values"
            }
            val enumTable = enumSummary.items(3) as List
            val enumOne = enumTable[0].data
            val enumTwo = enumTable[1].data
            val enumThree = enumTable[2].data

            assertThat(signature.type).isEqualTo("enum")
            assertThat(description.text()).isEqualTo("class level docs")

            assertThat((enumOne.title as Raw).data.text).contains("APPEND")
            assertThat((enumOne.description as Description).text())
                .contains("Load at the end.")
            assertThat((enumTwo.title as Raw).data.text).contains("PREPEND")
            assertThat((enumTwo.description as Description).text())
                .contains("Load at the start")
            assertThat((enumThree.title as Raw).data.text).contains("REFRESH")
            if (page == pageK) { // TODO: fix. Result in java has "result ofinvalidation"
                assertThat((enumThree.description as Description).text())
                    .contains("result of invalidation")
            }

            val enumName = enumDetails.symbols[0] as SymbolDetail
            val returnType = enumName.data.returnType.link()
            assertThat(returnType.name).endsWith("AnEnumType")
        }
    }

    @Test // TODO: add tests and support for kotlin inheriting from java and vice versa
    fun `Class component inherits docs from same language in 4x Kotlin and Java`() {
        val pagesK = """
            | /** docs for foo */
            |class foo() {
            |    /** dew it */
            |    fun doit() {}
            |    /** thunderous applause */
            |    open val democracy = false
            |}
            |class bar() : foo {
            |    override fun doit() {}
            |    override val democracy = true
            |}
            | /** overriding docs for baz */
            |class baz() : foo {
            |    /** overriding function docs */
            |    override fun doit() {}
            |    /** KotOR */
            |    override val democracy = true
            |}
            |class maz() : foo {
            |   /** {@inheritDoc} */
            |   override fun doit() {}
            |}        """.render()
        val pagesJ = """
            |public class foo {
            |    /** dew it */
            |    public void doit() {}
            |}
            |public class bar extends foo {
            |    /** {@inheritDoc} */
            |    @Override
            |    public void doit() {}
            |}
            |public class baz extends foo {
            |    /** overriding function docs */
            |    @Override
            |    public void doit() {}
            |}
        """.render(java = true) // Java {@inheritdoc} does not support classes & properties
        for (pages in listOf(pagesK, pagesJ)) {
            val fooClass = pages.page("foo").content<Classlike>()
            val barClass = pages.page("bar").content<Classlike>()
            val bazClass = pages.page("baz").content<Classlike>()

            // Function description inheritance does not work properly
            val fooDoit = fooClass.methodSymbols().first.items().single()
            val fooDoitDocs = fooDoit.summary().data.description.text()
            val barDoit = barClass.methodSymbols().first.items().single()
            val barDoitDocs = barDoit.summary().data.description.text()
            val bazDoit = bazClass.methodSymbols().first.items().single()
            val bazDoitDocs = bazDoit.summary().data.description.text()

            assertThat(fooDoitDocs).isEqualTo("dew it")
            assertThat(barDoitDocs).isEqualTo("dew it")
            assertThat(bazDoitDocs).isEqualTo("overriding function docs")

            if (pages == pagesK) {
                // overriding class docs is maybe something we want in kotlin, but is not jdoc spec
                val fooDescription = (fooClass.data.description.first() as Description)
                val barDescription = (barClass.data.description.first() as Description)
                val bazDescription = (bazClass.data.description.first() as Description)
                assertThat(fooDescription.text()).isEqualTo("docs for foo")
                // assertThat(barDescription.text()).isEqualTo("docs for foo")
                assertThat(bazDescription.text()).isEqualTo("overriding docs for baz")

                // overriding properties is kotlin-only, and working
                val fooDemocracy = fooClass.propertySymbols().first.items().single().summary()
                assertThat(fooDemocracy.data.description.text()).isEqualTo("thunderous applause")
                val barDemocracy = barClass.propertySymbols().first.items().single().summary()
                assertThat(barDemocracy.data.description.text()).isEqualTo("thunderous applause")
                val bazDemocracy = bazClass.propertySymbols().first.items().single().summary()
                assertThat(bazDemocracy.data.description.text()).isEqualTo("KotOR")

                // Using {@inheritDoc} in kotlin is wrong
                val mazClass = pages.page("maz").content<Classlike>()
                val mazDoit = mazClass.methodSymbols().first.items().single()
                val mazDoitDocs = mazDoit.summary().data.description.text()
                assertThat(mazDoitDocs).isNotEqualTo("dew it")
            }
        }
    }

    @Ignore // TODO: patch upstream dokka to implement kotlin documentation inheritance b/184361891
    @Test // TODO: add @constructor doc inheritance tests once that is implemented
    fun `Property parameter documentation inherits properly`() {
        val pages = """
            |/**
            | * @param param1 param1_docs
            | * @property property1 property1_docs
            | */
            |class supclaz(val param1: String, val property1: Int) {}
            |/**
            | * @param param2 param2_docs
            | * @property property2 property2_docs
            | */
            |interface interfaz(val param2: String, val property2: Int) {}
            |/**
            | * @param param3 param3_docs
            | * @property property3 property3_docs
            | */
            |sealed class sealclaz(internal val param3: String, protected val property3: Int) {}
            |class foo(param1: String, property1: Int, param2: String, property2: Int): supclaz(param1, property1), interfaz(param2, property2)
            |/**
            | * @param param1 override_param1_docs
            | * @param param2 override_param2_docs
            | * @property property1 override_property1_docs
            | * @property property2 override_property2_docs
            | */
            |class baz(param1: String, property1: Int, param2: String, property2: Int): supclaz(param1, property1), interfaz(param2, property2)
            |class bar(param3: String, property3: Int): sealclaz(param3, property3)
        """.render()
        val fooClass = pages.page("foo").content<Classlike>()
        val pparam1docs = fooClass.propertySymbol("param1").description()
        val pparam2docs = fooClass.propertySymbol("param2").description()
        val prop1docs = fooClass.propertySymbol("property1").description()
        val prop2docs = fooClass.propertySymbol("property2").description()
        val param1docs = ((fooClass.symbolsFor("Public constructors").second
            .symbols.single() as SymbolDetail).data.metadata[1] as SummaryList)
            .items().single { it.name() == "param1" }.description()
        val param2docs = ((fooClass.symbolsFor("Public constructors").second
            .symbols.single() as SymbolDetail).data.metadata[1] as SummaryList)
            .items().single { it.name() == "param2" }.description()
        assertThat(pparam1docs.text()).isEqualTo("param1_docs")
        assertThat(prop1docs.text()).isEqualTo("property1_docs")
        assertThat(pparam2docs.text()).isEqualTo("param2_docs")
        assertThat(prop2docs.text()).isEqualTo("property2_docs")
        assertThat(param1docs.text()).isEqualTo("param1_docs")
        assertThat(param2docs.text()).isEqualTo("param2_docs")
        val bazClass = pages.page("baz").content<Classlike>()
        val zpparam1docs = bazClass.propertySymbol("param1").description()
        val zpparam2docs = bazClass.propertySymbol("param2").description()
        val zprop1docs = bazClass.propertySymbol("property1").description()
        val zprop2docs = bazClass.propertySymbol("property2").description()
        val zparam1docs = ((bazClass.symbolsFor("Public constructors").second
            .symbols.single() as SymbolDetail).data.metadata[1] as SummaryList)
            .items().single { it.name() == "param1" }.description()
        val zparam2docs = ((bazClass.symbolsFor("Public constructors").second
            .symbols.single() as SymbolDetail).data.metadata[1] as SummaryList)
            .items().single { it.name() == "param2" }.description()
        assertThat(zpparam1docs.text()).isEqualTo("override_param1_docs")
        assertThat(zprop1docs.text()).isEqualTo("override_property1_docs")
        assertThat(zpparam2docs.text()).isEqualTo("override_param2_docs")
        assertThat(zprop2docs.text()).isEqualTo("override_property2_docs")
        assertThat(zparam1docs.text()).isEqualTo("override_param1_docs")
        assertThat(zparam2docs.text()).isEqualTo("override_param2_docs")
        val barClass = pages.page("bar").content<Classlike>()
        // TODO: patch upstream? dokka to support inheriting documentation on hidden components
        val pparam3docs = barClass.propertySymbol("param3").description()
        val prop3docs = barClass.propertySymbol("property3").description()
        val param3docs = ((barClass.symbolsFor("Public constructors").second
            .symbols.single() as SymbolDetail).data.metadata[1] as SummaryList)
            .items().single { it.name() == "param3" }.description()
        assertThat(pparam3docs.text()).isEqualTo("param3_docs")
        assertThat(prop3docs.text()).isEqualTo("property3_docs")
        assertThat(param3docs.text()).isEqualTo("param3_docs")
    }

    @Test // TODO: add tests and support for language-interleaving inheritance hierarchies
    fun `Level-jumping doc inheritance works in 4x Kotlin and Java`() {
        val pageK = """
            | /** docs for foo */
            |class foo() {
            |    /** dew it */
            |    fun doit() {}
            |}
            |class bar() : foo {}
            | /** overriding docs for baz */
            |class baz() : bar {
            |    override fun doit() {}
            |}
        """.render().page("baz")
        val pageJ = """
            | /** docs for foo */
            |public class foo() {
            |    /** dew it */
            |    public void doit() {}
            |}
            |public class bar() extends foo {}
            | /** overriding docs for baz */
            |public class baz() extends bar {
            |    /** {@inheritdoc} */
            |    override public void doit() {}
            |}
        """.render(java = true).page("baz")
        for (page in listOf(pageJ, pageK)) {
            val doit = page.content<Classlike>().methodSymbols().first.items().single()
            assertThat(doit.summary().data.description.text()).isEqualTo("dew it")
        }
    }

    @Test
    fun `Class component creates inline generics`() {
        val page = """
            |class Foo<T: Number, U>() {}
        """.render().page()
        val typeParams = page.content<Classlike>().data.signature.data.typeParameters
        assertThat(typeParams.first().data.name).isEqualTo("T")
        assertThat(typeParams.first().projectionName()).isEqualTo("Number")
        assertThat(typeParams.last().data.name).isEqualTo("U")
        kotlinOnly { assertThat(typeParams.last().projectionName()).isEqualTo("Any") }
        javaOnly { assertThat(typeParams.last().projectionName()).isEqualTo("Object") }
    }

    @Test
    fun `Class component creates all symbols in the correct order`() {
        val page = "class Foo {}".render().page()
        val symbolTypes = page
            .content<Classlike>()
            .data
            .symbolTypes
            .map { it.second.title }

        javaOnly {
            assertThat(symbolTypes).isEqualTo(
                listOf(
                    "Nested types",
                    "Enum Values",
                    "Constants",
                    "Public fields",
                    "Protected fields",
                    "Public constructors",
                    "Protected constructors",
                    "Public methods",
                    "Protected methods"
                )
            )
        }

        kotlinOnly {
            assertThat(symbolTypes).isEqualTo(
                listOf(
                    "Nested types",
                    "Enum Values",
                    "Constants",
                    "Public constructors",
                    "Protected constructors",
                    "Public functions",
                    "Protected functions",
                    "Public properties",
                    "Protected properties"
                )
            )
        }
    }

    private fun DModule.page(name: String = "Foo"): DevsitePage {
        val classlike = explicitClasslike(name)
        val holder = runBlocking { DocumentablesHolder(this@page, this) }
        val classGraph = runBlocking { holder.classGraph() }
        val converter = ClasslikeDocumentableConverter(
            language,
            classlike,
            pathProvider(classGraph = classGraph),
            holder)
        return runBlocking { converter.classlike() }
    }

    private fun Classlike.symbolsFor(
        vararg types: String
    ) = data.symbolTypes.single { (summary, _) ->
        summary.title() in types
    }

    private fun Classlike.methodSymbols(): Pair<SummaryList, Classlike.SymbolType> =
        symbolsFor(if (language == Language.KOTLIN) "Public functions" else "Public methods")

    private fun Classlike.methodSymbol(name: String = "foo") =
        methodSymbols().first.items().singleOrNull { it.name() == name }
            ?: methodSymbols().first.items().single()

    private fun Classlike.propertySymbols(): Pair<SummaryList, Classlike.SymbolType> =
        symbolsFor(if (language == Language.KOTLIN) "Public properties" else "Public fields")

    private fun Classlike.propertySymbol(name: String = "foo") =
        propertySymbols().first.items().singleOrNull { it.name() == name }
            ?: propertySymbols().first.items().single()

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
