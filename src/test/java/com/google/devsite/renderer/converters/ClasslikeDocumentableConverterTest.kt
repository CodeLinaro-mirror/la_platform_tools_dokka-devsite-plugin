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
import com.google.devsite.capitalize
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.SingleColumnSummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.content
import com.google.devsite.renderer.converters.testing.enumValues
import com.google.devsite.renderer.converters.testing.from
import com.google.devsite.renderer.converters.testing.inheritedFields
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.nestedTypes
import com.google.devsite.renderer.converters.testing.projectionName
import com.google.devsite.renderer.converters.testing.size
import com.google.devsite.renderer.converters.testing.summary
import com.google.devsite.renderer.converters.testing.summaryItemsFor
import com.google.devsite.renderer.converters.testing.symbolsFor
import com.google.devsite.renderer.converters.testing.symbolsForConstructors
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.renderer.converters.testing.title
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.JavaObject
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertFails

@RunWith(Parameterized::class)
internal class ClasslikeDocumentableConverterTest(
    private val displayLanguage: Language
) : ConverterTestBase(displayLanguage) {
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
        val summary = classlike.methodSummaryItems()

        assertThat(summary.single().name()).isEqualTo("foo")
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

        val propertiesSummary = documentation.content<Classlike>().propertySummaryItems()
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
        val summary = classlike.methodSummaryItems()

        javaOnly {
            assertThat(summary.size).isEqualTo(1)
        }
        kotlinOnly {
            assertThat(summary.size).isEqualTo(2)
        }
    }

    @Test
    fun `Protected function gets documented`() {
        val page = """
            |abstract class Foo {
            |    protected open fun foo() = Unit
            |}
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsFor(protectedMethodsTitle(displayLanguage))

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
        val summary = classlike.propertySummaryItems()

        assertThat(summary.single().summary().name()).isEqualTo("foo")
    }

    @Test
    fun `Protected property gets documented`() {
        val page = """
            |abstract class Foo {
            |    protected open val foo = Unit
            |}
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsFor(protectedPropertiesTitle(displayLanguage))

        assertThat(summary.item().summary().name()).isEqualTo("foo")
    }

    @Test
    fun `Public constructor gets documented`() {
        val page = """
            |class Foo
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsForConstructors()

        assertThat(summary.constructor().name()).isEqualTo("Foo")
    }

    @Test
    fun `Function summary component hides DeprecationLevel HIDDEN`() {
        val module = """
            |import kotlin.DeprecationLevel.HIDDEN
            |
            |class Visible {
            |   public val visible = "v"
            |   @Deprecated("No show!", level = HIDDEN)
            |   public val invisible = "i"
            |
            |   public fun show() = 7
            |   @Deprecated("No show!", level = HIDDEN)
            |   public fun noShow() = 5
            |}
            |
            |@Deprecated("No show!", level = HIDDEN)
            |class Nope
        """.render()

        assertThat(module.packages.single().classlikes.map { it.name }).containsExactly("Visible")
        val visible = module.page("Visible").content<Classlike>()

        assertThat(visible.methodSummaryItems().map { it.name() }).containsExactly("show")
        assertThat(visible.propertySummaryItems().map { it.name() }).containsExactly("visible")
    }

    @Test
    fun `Public constructor does not have @NonNull in 4x Kotlin and Java`() {
        val constructorsK = """
        |class Foo {
        |   constructor() {}
        |}
        """.render().page().content<Classlike>().symbolsForConstructors()
        val constructorsJ = """
        |public class Foo {
        |   public Foo() {}
        |}
        """.render(java = true).page().content<Classlike>().symbolsForConstructors()

        for (constructor in listOf(constructorsJ, constructorsK)) {
            // Constructor summaries are SingleColumnSummaryItems containing SymbolSummaries
            // They cannot have annotations, enforced by design.

            val detail = constructor.second.symbols.single() as SymbolDetail
            val returnAnnotations = detail.data.returnType.annotations
            val annotations = detail.data.annotationComponents
            val signature = detail.data.signature as FunctionSignature
            assertThat(returnAnnotations.isEmpty())
            assertThat(annotations).isEmpty()
            assertThat(signature.data.receiver).isNull()
        }
    }

    @Test
    fun `Protected constructor gets documented`() {
        val page = """
            |open class Foo protected constructor()
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.symbolsForConstructors(public = false, protected = true)

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
        val (summary) = classlike.nestedTypes()
        assertThat(summary.item().link().name).isEqualTo("Foo.Bar")
    }

    @Test
    fun `Companion objects are documented in Java but not Kotlin because they're inlined`() {
        val page = """
            |class Foo {
            |    companion object Bar
            |}
        """.render().page()

        val classlike = page.content<Classlike>()
        val (summary) = classlike.nestedTypes()
        kotlinOnly {
            assertThat(summary.items()).hasSize(0)
        }
        javaOnly {
            assertThat(summary.item().link().name).isEqualTo("Foo.Bar")
        }
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

        javaOnly { assertThat(parents[0].data.name).isEqualTo("java.lang.Object") }
        kotlinOnly { assertThat(parents[0].data.name).isEqualTo("kotlin.Any") }
        assertThat(parents[1].data.name).isEqualTo("androidx.example.Parent")
        assertThat(parents[2].data.name).isEqualTo("androidx.example.Foo")
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

        javaOnly { assertThat(parents[0].data.name).isEqualTo("java.lang.Object") }
        kotlinOnly { assertThat(parents[0].data.name).isEqualTo("kotlin.Any") }
        assertThat(parents[1].data.name).isEqualTo("androidx.example.A")
        assertThat(parents[2].data.name).isEqualTo("androidx.example.B")
        assertThat(parents[3].data.name).isEqualTo("androidx.example.C")
        assertThat(parents[4].data.name).isEqualTo("androidx.example.Foo")
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

    @Test
    fun `Class signature appears with implements for external types in 4x`() {
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
            |public class JavaArgsLazy implements Lazy {}
        """.renderJava(imports = listOf("kotlin.LazyKt.Lazy")).page(name = "JavaArgsLazy")
        val signatureK = pageExternalK.content<Classlike>().data.signature
        val signatureJ = pageExternalJ.content<Classlike>().data.signature
        assertThat(
            signatureK.data.implements.map { it.data.name }
        ).isEqualTo(listOf("Lazy"))
        assertThat(
            signatureJ.data.implements.map { it.data.name }
        ).isEqualTo(listOf("Lazy"))
    }

    // This test also validates that only direct superclasses / interfaces are included because
    // AbstractList extends AbstractCollection which implements various interfaces (Iterable etc).
    @Test
    fun `Class signature appears with extends for external types in 4x`() {
        val pageExternalK = """
            |import java.util.AbstractList
            |public class MyList() : AbstractList<Int>()
        """.render().page(name = "MyList")
        val signatureK = pageExternalK.content<Classlike>().data.signature
        val pageExternalJ = """
            |public class MyList extends AbstractList<String> {}
        """.renderJava(imports = listOf("java.util.*")).page(name = "MyList")
        val signatureJ = pageExternalJ.content<Classlike>().data.signature
        for (signature in listOf(signatureJ, signatureK)) {
            assertThat(
                signature.data.extends.map { it.data.name }
            ).isEqualTo(listOf("AbstractList"))
            assertThat(signature.data.implements).isEmpty()
        }
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
        assertThat(page.symbolsForConstructors().first.size).isEqualTo(0)
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
            val description = (classlike.data.description.first() as DescriptionComponent)

            val (enumSummary, enumDetails) = classlike.enumValues()
            val enumTable = enumSummary.data.items
            val enumOne = enumTable[0].data
            val enumTwo = enumTable[1].data
            val enumThree = enumTable[2].data

            assertThat(signature.type).isEqualTo("enum")
            assertThat(description.text()).isEqualTo("class level docs")

            assertThat(enumOne.title.data.name).contains("APPEND")
            assertThat((enumOne.description).text()).contains("Load at the end.")
            assertThat(enumTwo.title.data.name).contains("PREPEND")
            assertThat((enumTwo.description).text()).contains("Load at the start")
            assertThat(enumThree.title.data.name).contains("REFRESH")
            assertThat((enumThree.description).text()).contains("result of invalidation")

            val enumName = enumDetails.symbols[0]
            val returnType = enumName.data.returnType.link()
            assertThat(returnType.name).endsWith("AnEnumType")
        }
    }

    @Test
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
            val fooDoit = fooClass.methodSummaryItems().single()
            val fooDoitDocs = fooDoit.summary().data.description.text()
            val barDoit = barClass.methodSummaryItems().single()
            val barDoitDocs = barDoit.summary().data.description.text()
            val bazDoit = bazClass.methodSummaryItems().single()
            val bazDoitDocs = bazDoit.summary().data.description.text()

            assertThat(fooDoitDocs).isEqualTo("dew it")
            assertThat(barDoitDocs).isEqualTo("dew it")
            assertThat(bazDoitDocs).isEqualTo("overriding function docs")

            if (pages == pagesK) {
                // overriding class docs is maybe something we want in kotlin, but is not jdoc spec
                val fooDescription = (fooClass.data.description.first() as DescriptionComponent)
                val barDescription = (barClass.data.description.first() as DescriptionComponent)
                val bazDescription = (bazClass.data.description.first() as DescriptionComponent)
                assertThat(fooDescription.text()).isEqualTo("docs for foo")
                // assertThat(barDescription.text()).isEqualTo("docs for foo")
                assertThat(bazDescription.text()).isEqualTo("overriding docs for baz")

                // overriding properties is kotlin-only, and working
                val fooDemocracy = fooClass.propertySummaryItems().single().summary()
                assertThat(fooDemocracy.data.description.text()).isEqualTo("thunderous applause")
                val barDemocracy = barClass.propertySummaryItems().single().summary()
                assertThat(barDemocracy.data.description.text()).isEqualTo("thunderous applause")
                val bazDemocracy = bazClass.propertySummaryItems().single().summary()
                assertThat(bazDemocracy.data.description.text()).isEqualTo("KotOR")

                // Using {@inheritDoc} in kotlin is wrong
                val mazClass = pages.page("maz").content<Classlike>()
                val mazDoit = mazClass.methodSummaryItems().single()
                val mazDoitDocs = mazDoit.summary().data.description.text()
                assertThat(mazDoitDocs).isNotEqualTo("dew it")
            }
        }
    }

    @Suppress("UNCHECKED_CAST") // TODO: add tests once @constructor doc inheritance is implemented
    @Test // TODO: patch upstream dokka to implement kotlin documentation inheritance b/184361891
    fun `Property parameter documentation inherits properly`() {
        val pages = """
            |/**
            | * @param param1 param1_docs
            | * @property property1 property1_docs
            | */
            |class Supclaz(val param1: String, val property1: Int) {}
            |/**
            | * @param param2 param2_docs
            | * @property property2 property2_docs
            | */
            |interface Interfaz(val param2: String, val property2: Int) {}
            |/**
            | * @param param3 param3_docs
            | * @property property3 property3_docs
            | */
            |sealed class Sealclaz(internal val param3: String, protected val property3: Int) {}
            |class Foo(param1: String, property1: Int, param2: String, property2: Int): Supclaz(param1, property1), Interfaz(param2, property2)
            |/**
            | * @param param1 override_param1_docs
            | * @param param2 override_param2_docs
            | * @property property1 override_property1_docs
            | * @property property2 override_property2_docs
            | */
            |class Baz(override val param1: String, override val property1: Int, param2: String, override val property2: Int): Supclaz(param1, property1), Interfaz(param2, property2)
            |class Bar(override val param3: String, override val property3: Int): Sealclaz(param3, property3)
        """.render()
        val fooClass = pages.page("Foo").content<Classlike>()
        val fromSupclaz = fooClass.inheritedFields!!.from("androidx.example.Supclaz")!!.value
        val fromInterfaz = fooClass.inheritedFields!!.from("androidx.example.Interfaz")!!.value
        val pparam1docs = fromSupclaz.items().single { it.name() == "param1" }.data.description
        val pparam2docs = fromInterfaz.items().single { it.name() == "param2" }.data.description
        val prop1docs = fromSupclaz.items().single { it.name() == "property1" }.data.description
        val prop2docs = fromInterfaz.items().single { it.name() == "property2" }.data.description

        assertThat(fooClass.symbolsFor(publicPropertiesTitle(displayLanguage)).first.hasContent())
            .isFalse()

        assertThat(pparam1docs.text()).isEqualTo("param1_docs")
        assertThat(prop1docs.text()).isEqualTo("property1_docs")
        assertThat(pparam2docs.text()).isEqualTo("param2_docs")
        assertThat(prop2docs.text()).isEqualTo("property2_docs")
        /* Constructors don't magically inherit and merge @params from parents' constructors
        val constructorDetails = fooClass.symbolsForConstructors().second.symbols.single()
        val ctrDocsParamTable = constructorDetails.data.metadata[1] as DocsSummaryList
        val param1docs = ctrDocsParamTable.items().single { it.name() == "param1" }.data.description
        val param2docs = ctrDocsParamTable.items().single { it.name() == "param2" }.data.description
        assertThat(param1docs.text()).isEqualTo("param1_docs")
        assertThat(param2docs.text()).isEqualTo("param2_docs")
        */

        val bazClass = pages.page("Baz").content<Classlike>()
        val zpparam1docs = bazClass.propertySymbol("param1")!!.data.description
        val zprop1docs = bazClass.propertySymbol("property1")!!.data.description
        val zprop2docs = bazClass.propertySymbol("property2")!!.data.description

        assertThat(zprop1docs.text()).isEqualTo("override_property1_docs")
        assertThat(zprop2docs.text()).isEqualTo("override_property2_docs")

        // This param explicitly has "override val" so shows up as a property
        assertThat(zpparam1docs.text()).isEqualTo("override_param1_docs")
        // the "Inherited Propeties" section doesn't contain overriding documentation
        val zFromInterfaz = fooClass.inheritedFields!!.from("androidx.example.Interfaz")!!.value
        val zpparam2docs = zFromInterfaz.items().single { it.name() == "param2" }.data.description
        assertThat(zpparam2docs.text()).isEqualTo("param2_docs")

        val zConstructorDetails = bazClass.symbolsForConstructors().second.symbols.single()
        val zctrDocsParamTabl = zConstructorDetails.data.metadata[1] as DocsSummaryList
        val zparam1doc = zctrDocsParamTabl.items().single { it.name() == "param1" }.data.description
        val zparam2doc = zctrDocsParamTabl.items().single { it.name() == "param2" }.data.description
        assertThat(zparam1doc.text()).isEqualTo("override_param1_docs")
        assertThat(zparam2doc.text()).isEqualTo("override_param2_docs")

        val barClass = pages.page("Bar").content<Classlike>()
        // TODO: patch upstream dokka to support inheriting documentation on hidden components
        // val pparam3docs = barClass.propertySymbol("param3").data.description
        // assertThat(pparam3docs.text()).isEqualTo("param3_docs")

        val prop3docs = barClass.propertySymbol("property3")!!.data.description
        assertThat(prop3docs.text()).isEqualTo("property3_docs")
        val barSymbols = barClass.data.symbolTypes
        assertThat(barSymbols.filter { it.first.hasContent() }).hasSize(2)
        val barProt = barSymbols.single { it.title() == protectedPropertiesTitle(displayLanguage) }
        assertThat(barProt.first.data.items.single().data.description).isEqualTo(prop3docs)

        /* Constructors don't inherit docs
        val bConstructorDetails = fooClass.symbolsForConstructors().second.symbols.single()
        val bctrDocsParamTabl = bConstructorDetails.data.metadata[1] as DocsSummaryList
        val bparam3doc = bctrDocsParamTabl.items().single { it.name() == "param3" }.data.description
        assertThat(bparam3doc.text()).isEqualTo("param3_docs")
        */
    }

    @Test
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
            val doit = page.content<Classlike>().methodSummaryItems().single()
            assertThat(doit.summary().data.description.text()).isEqualTo("dew it")
        }
    }

    @Test
    fun `Inherited methods are sorted by name and arity`() {
        val childClass = """
            |class Parent() {
            |    fun b(input: Int, zinput2: Int) {}
            |    fun c() {}
            |    fun a() {}
            |    fun b(input: Int, input2: Int) {}
            |    fun b() {}
            |    fun b(input: Int) {}
            |}
            |class Child() : Parent {}
        """.render().page("Child").content<Classlike>()
        val inheritedSummary = childClass.data.inheritedTypes.single().data.inheritedSymbolSummaries
        val inheritedMethods = inheritedSummary.entries.single().value.data.items
        val inheritedMethodSignatures = inheritedMethods.map {
            (it.data.description as SymbolSummary).data.signature as FunctionSignature
        }
        assertThat(inheritedMethodSignatures.map { it.data.name.data.name }).isEqualTo(
            listOf("a", "b", "b", "b", "b", "c")
        )
        assertThat(inheritedMethodSignatures.map { it.data.parameters.size }).isEqualTo(
            listOf(0, 0, 1, 2, 2, 0)
        )
        val paramNames = inheritedMethodSignatures.map {
            it.data.parameters.map { it.data.name }
        }
        assertThat(paramNames).isEqualTo(
            listOf(
                listOf(),
                listOf(),
                listOf("input"),
                listOf("input", "input2"),
                listOf("input", "zinput2"),
                listOf()
            )
        )
    }

    @Test
    fun `Inherited properties are not lost`() {
        val page = """
            |open class Parent {
            |    val b: Int = 8
            |    val a: String = "9"
            |}
            |class Child: Parent()
        """.render().page("Child").content<Classlike>()

        val category = page.data.inheritedTypes.single().data.inheritedSymbolSummaries
        val names = category.values.single().items().map { it.name() }

        assertThat(names).containsExactly("a", "b").inOrder()
    }

    @Test
    fun `Different categories of symbols inherited from different classes works`() {
        val page = """
            |open class GrandParent {
            |    val grandC: Int = 18
            |    fun grandA(): String = "9"
            |    fun grandB(): {}
            |}
            |open class Parent: GrandParent {
            |    val parentB: Int = 8
            |    val parentA: String = "9"
            |    fun parentC(): {}
            |}
            |class Child: Parent()
        """.render().page("Child").content<Classlike>()

        val categoriesNames = page.data.inheritedTypes.map { it.data.header.data.title }
        kotlinOnly {
            assertThat(categoriesNames)
                .containsExactly("Inherited functions", "Inherited properties").inOrder()
        }
        javaOnly {
            assertThat(categoriesNames)
                .containsExactly("Inherited methods", "Inherited fields").inOrder()
        }

        val functions = page.data.inheritedTypes.first().data.inheritedSymbolSummaries
            .mapKeys { it.key.data.name }
            .mapValues { (_, list) -> list.items().map { it.name() } }
        assertThat(functions).containsExactly(
            "androidx.example.GrandParent", listOf("grandA", "grandB"),
            "androidx.example.Parent", listOf("parentC")
        )

        val properties = page.data.inheritedTypes.last().data.inheritedSymbolSummaries
            .mapKeys { it.key.data.name }
            .mapValues { (_, list) -> list.items().map { it.name() } }
        assertThat(properties).containsExactly(
            "androidx.example.GrandParent", listOf("grandC"),
            "androidx.example.Parent", listOf("parentA", "parentB")
        )
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
                    "Public companion functions",
                    "Protected companion functions",
                    "Public companion properties",
                    "Protected companion properties",
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

    @Test
    fun `classlike companion functions are included in Kotlin and nested static in Java`() {
        val module = """
            |class Foo {
            |  companion object {
            |    fun bar() = Unit
            |    protected fun baz() = Unit
            |  }
            |}
        """.render()

        val classlike = module.page("Foo").content<Classlike>()

        kotlinOnly {
            assertThat(classlike.symbolsFor(publicCompanionFunctionsTitle()).first.item().name())
                .isEqualTo("bar")
            assertThat(classlike.symbolsFor(protectedCompanionFunctionsTitle()).first.item().name())
                .isEqualTo("baz")
        }
        javaOnly {
            assertThat(classlike.noSectionFor(publicCompanionFunctionsTitle())).isTrue()
            assertThat(classlike.noSectionFor(publicCompanionPropertiesTitle())).isTrue()
            assertThat(classlike.nestedTypes().first.item().name()).isEqualTo("Foo.Companion")
            val companionClasslike = module.page { this.companionFor("Foo") }.content<Classlike>()
            assertThat(companionClasslike.methodSummaryItems()).hasSize(2)
            val barMethod = companionClasslike.methodSymbol("bar")!!
            val bazMethod = companionClasslike.methodSymbol("baz")!!
            assertThat(barMethod.data.title.data.modifiers).isEqualTo(listOf("static", "final"))
            assertThat(bazMethod.data.title.data.modifiers).isEqualTo(listOf("static", "final"))
        }
    }

    @Test
    fun `classlike companion properties are included in Kotlin and nested static in Java`() {
        val module = """
            |class Foo {
            |  companion object {
            |    val bar: List<String> = emptyList()
            |    protected val baz: Int = 1
            |  }
            |}
        """.render()

        val classlike = module.page("Foo").content<Classlike>()

        kotlinOnly {
            assertThat(classlike.symbolsFor(publicCompanionPropertiesTitle()).first.item().name())
                .isEqualTo("bar")
            assertThat(
                classlike.symbolsFor(protectedCompanionPropertiesTitle()).first.item().name()
            ).isEqualTo("baz")
        }
        javaOnly {
            assertThat(classlike.noSectionFor(publicCompanionFunctionsTitle())).isTrue()
            assertThat(classlike.noSectionFor(publicCompanionPropertiesTitle())).isTrue()
            assertThat(classlike.nestedTypes().first.item().name()).isEqualTo("Foo.Companion")
            val companionClasslike = module.page { this.companionFor("Foo") }.content<Classlike>()
            assertThat(companionClasslike.propertySummaryItems()).hasSize(2)
            val barProp = companionClasslike.propertySymbol("bar")!!
            val bazProp = companionClasslike.propertySymbol("baz")!!
            assertThat(barProp.data.title.data.modifiers).isEqualTo(listOf("static", "final"))
            assertThat(bazProp.data.title.data.modifiers).isEqualTo(listOf("static", "final"))
        }
    }

    @Test
    fun `Named top-level objects have pages and treat members properly in 4x Java and Kotlin`() {
        val moduleK = """
            |object Foo {
            |  fun bar() = Unit
            |  const val baz = "baz"
            |}
        """.render()
        val classlikeK = moduleK.page("Foo").content<Classlike>()

        val classlikeJ = """
            |public static class Foo {
            |  public static final void bar() {}
            |  public static final String baz = "baz"
            |}
        """.render(java = true).page("Foo").content<Classlike>()

        for (classlike in listOf(classlikeJ, classlikeK)) {
            assertThat(classlikeK.noSymbolsFor(publicCompanionFunctionsTitle())).isTrue()
            assertThat(classlikeK.noSymbolsFor(publicCompanionPropertiesTitle())).isTrue()
            assertThat(classlike.methodSummaryItems()).hasSize(1)
            assertThat(classlike.symbolsFor(publicPropertiesTitle(displayLanguage)).first.size)
                .isEqualTo(0)
            assertThat(classlike.symbolsFor(constantsTitle()).first.size).isEqualTo(1)

            val barMethod = classlike.methodSymbol("bar")!!
            val barModifiers = barMethod.data.title.data.modifiers
            val bazConst = classlike.symbolsFor(constantsTitle()).first.items()
                .single { it.name() == "baz" }
            kotlinOnly {
                if (classlike == classlikeK) assertThat(barModifiers).isEmpty()
                else assertThat(barModifiers).isEqualTo(listOf("java-static"))
                assertThat(bazConst.data.title.data.modifiers).isEqualTo(listOf("const"))
            }
            javaOnly {
                assertThat(barModifiers).isEqualTo(listOf("static", "final"))
                assertThat(bazConst.data.title.data.modifiers).isEqualTo(listOf("static", "final"))
            }
        }
    }

    @Test
    fun `Static and companion functions are treated correctly in both languages`() {
        val moduleK = """
            |class Foo {
            |  companion object {
            |    val baz: Int = 1
            |    fun bar() = Unit
            |  }
            |}
        """.render()
        val classlikeK = moduleK.page().content<Classlike>()

        val classlikeJ = """
            |public class Foo {
            |  public static void foo() {}
            |  public static String bar = "bar"
            |}
        """.render(java = true).page().content<Classlike>()

        val companionClassK = moduleK.page("Companion")

        val (kotlinNestedTypeSummary) = classlikeK.nestedTypes()

        val staticJavaMethod = classlikeJ.methodDetailsItems().single().data
        val staticJavaField = classlikeJ.propertyDetailsItems().single().data

        // Companion class is included in both modules
        assertThat(companionClassK).isNotNull()

        // can see java static methods in both languages
        assertThat(staticJavaMethod.name).isEqualTo("foo")
        assertThat(staticJavaField.name).isEqualTo("bar")

        // can find kotlin companion method in both languages
        kotlinOnly {
            // nested companion object is not documented because it is inlined
            assertThat(kotlinNestedTypeSummary.items()).hasSize(0)
            val companionFunctions = classlikeK.summaryItemsFor(publicCompanionFunctionsTitle())
            val companionProperties = classlikeK.summaryItemsFor(publicCompanionPropertiesTitle())
            assertThat(companionFunctions).hasSize(1)
            assertThat(companionProperties).hasSize(1)
        }
        javaOnly {
            // nested companion object is documented but companion functions are not inlined
            assertThat(kotlinNestedTypeSummary.items()).hasSize(1)
            assertThat(classlikeK.noSymbolsFor(publicCompanionFunctionsTitle())).isTrue()
            assertThat(classlikeK.noSymbolsFor(publicCompanionPropertiesTitle())).isTrue()
            assertThat(staticJavaMethod.modifiers).contains("static")
            assertThat(staticJavaField.modifiers).contains("static")
        }
    }

    @Ignore("Un-ignore test (and update as necessary) when b/190600426 is resolved")
    @Suppress("UNUSED_VARIABLE")
    @Test
    fun `static getters in companion objects can be renamed in java`() {
        val page = """
            |class Foo {
            |   companion object {
            |       @JvmStatic
            |       @get:JvmName("computeBar")
            |       val bar = 8
            |   }
            |}
        """.render().page("Foo").content<Classlike>()

        val methods = page.methodSummaryItems()
//        val staticMethods = methods.filter { it.modifiers().contains("static") }
//
//        javaOnly {
//            assertThat(methods.names()).containsExactly("computeBar")
//            assertThat(staticMethods).hasSize(1)
//        }

        kotlinOnly {
            val companionProperties =
                page.symbolsFor(companionPropertiesTitle()).first.items()
            val companionFunctions =
                page.symbolsFor(companionFunctionsTitle()).first.items()

            assertThat(companionProperties).hasSize(1)
            assertThat(companionFunctions).isEmpty()
        }
    }

    @Test
    fun `Extension functions are included on Java and Kotlin pages`() {
        val src = """
            |class Foo {
            |}
            |fun Foo.bar() = Unit
            |fun Foo.baz() = Unit
        """
        val classlike = src.render().page().content<Classlike>()
        val extFunctions = classlike.symbolsFor("Extension functions")
        assertThat(extFunctions.first.data.items).hasSize(2)
    }

    @Test
    fun `Extension functions are ordered by the package they come from`() {
        val src = listOf(
            """
                |/src/main/kotlin/androidx/example/Foo.kt
                |package foo
                |class Foo {
                |}
            """,
            """
                |/src/main/kotlin/androidx/example/Second.kt
                |package second
                |
                |import foo.Foo
                |
                |fun Foo.baz() = Unit
            """,
            """
                |/src/main/kotlin/androidx/example/First.kt
                |package first
                |
                |import foo.Foo
                |
                |fun Foo.zab() = Unit
            """
        )
        val classlike = src.render().page().content<Classlike>()
        val extFunctions = classlike.symbolsFor("Extension functions")
        val extFunctionClasses = extFunctions.second.symbols.map {
            (it as? SymbolDetail)?.data?.extFunctionClass
        }
        assertThat(extFunctionClasses).isEqualTo(listOf("FirstKt", "SecondKt"))
    }

    @Test
    fun `Extension functions respect @JvmName for packages`() {
        val src = listOf(
            """
                |/src/main/kotlin/androidx/example/Foo.kt
                |package foo
                |class Foo {
                |}
            """,
            """
                |/src/main/kotlin/androidx/example/Second.kt
                |
                |@file:JvmName("SecondJvm")
                |package second
                |
                |import foo.Foo
                |
                |fun Foo.baz() = Unit
            """,
            """
                |/src/main/kotlin/androidx/example/First.kt
                |
                |@file:JvmName("FirstJvm")
                |package first
                |
                |
                |import foo.Foo
                |
                |fun Foo.zab() = Unit
            """
        )
        val classlike = src.render().page().content<Classlike>()
        val extFunctions = classlike.symbolsFor("Extension functions")
        val extFunctionClasses = extFunctions.second.symbols.map {
            (it as? SymbolDetail)?.data?.extFunctionClass
        }
        assertThat(extFunctionClasses).isEqualTo(listOf("FirstJvm", "SecondJvm"))
    }

    @Ignore // TODO: b/195529157
    @Test
    fun `Annotation types with no parameters have no default constructors`() {
        // parameterless annotations are invoked as `@NonNull` not `@NonNull()`
        val documentationJ = """
        |public @interface Mega {
        |   /**
        |    * Reason why playback is suppressed even though {@link #getPlayWhenReady()} is {@code true}. One
        |    * of {@link #PLAYBACK_SUPPRESSION_REASON_NONE} or {@link
        |    * #PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS}.
        |    */
        |   @Documented
        |   @Retention(RetentionPolicy.SOURCE)
        |   @interface PlaybackSuppressionReason {}
        |}
        """.render(java = true).page("PlaybackSuppressionReason").content<Classlike>()
        val documentationK = """
        |public annotation class Mega {
        |   /**
        |    * Reason why playback is suppressed even though {@link #getPlayWhenReady()} is {@code true}. One
        |    * of {@link #PLAYBACK_SUPPRESSION_REASON_NONE} or {@link
        |    * #PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS}.
        |    */
        |   annotation class PlaybackSuppressionReason {}
        |}
        """.render().page("PlaybackSuppressionReason").content<Classlike>()

        for (documentation in listOf(documentationJ, documentationK)) {
            val constructors = documentation.symbolsForConstructors()
            assertThat(constructors.first.size).isEqualTo(0)
        }
    }

    @Test
    fun `Java getters and setters are documented`() {
        val page = """
            |public final class Foo {
            |
            |  public int a;
            |  public int c; // intentional mismatch of field name / getter name
            |  private int d;
            |  protected int e;
            |
            |  public int getA() {
            |    return a;
            |  }
            |
            |  public void setA(int a) {
            |    this.a = a;
            |  }
            |
            |  public int getB() {
            |    return c;
            |  }
            |
            |  public void setB(int b) {
            |    c = b;
            |  }
            |
            |  private int getD() {
            |    return d;
            |  }
            |
            |  private void setD(int d) {
            |    this.d = d;
            |  }
            |
            |  protected int getE() {
            |    return e;
            |  }
            |
            |  protected void setE(int e) {
            |    this.e = e;
            |  }
            |}
        """.render(java = true).page()

        val classlike = page.content<Classlike>()
        val publicMethodSymbols = classlike.symbolsFor(publicMethodsTitle(displayLanguage))
        val protectedMethodSymbols = classlike.symbolsFor(protectedMethodsTitle(displayLanguage))
        val publicMethodNames = publicMethodSymbols.second.symbols.map {
            (it as SymbolDetail).data.name
        }
        val protectedMethodNames = protectedMethodSymbols.second.symbols.map {
            (it as SymbolDetail).data.name
        }

        kotlinOnly {
            // in Kotlin, we don't need to show getA / setA because property access is preferred.
            assertThat(publicMethodNames).isEqualTo(listOf("getB", "setB"))
            assertThat(protectedMethodNames).isEmpty()
        }
        javaOnly {
            assertThat(publicMethodNames).isEqualTo(listOf("getA", "getB", "setA", "setB"))
            // TODO(b/165112358): 'e' doesn't show up in the dokka model
            // assertThat(protectedMethodNames).isEqualTo(listOf("getE", "setE"))
        }
    }

    @Test
    fun `Java source with public getter and private setter is documented correctly`() {
        val page = """
            |public final class Foo {
            |
            |  public int a;
            |
            |  public int getA() {
            |    return a;
            |  }
            |
            |  private void setA(int a) {
            |    this.a = a;
            |  }
            |}
        """.render(java = true).page()

        val classlike = page.content<Classlike>()
        val publicMethodSymbols = classlike.methodDetailsItems()
        val publicMethodNames = publicMethodSymbols.map { it.data.name }

        kotlinOnly {
            assertThat(publicMethodNames).isEmpty()
        }
        javaOnly {
            assertThat(publicMethodNames).isEqualTo(listOf("getA"))
        }
    }

    @Test
    fun `Kotlin generated getters and setters are not documented`() {
        val page = """
            |data class Foo(val a: Int, var b: Int) {
            |    var c: Int
            |       get() = 0
            |       set(c: Int): Unit
            |}
        """.render().page()

        val classlike = page.content<Classlike>()
        val methodSymbols = classlike.methodDetailsItems()
        val methodNames = methodSymbols.map { it.data.name }

        assertThat(methodNames).isEmpty()
    }

    @Test
    fun `Default no-arg constructors are autogenerated`() {
        val emptyTestClass = """
            public class Foo {}
        """.trimIndent()

        for (isJava in listOf(true, false)) {
            val classlike = emptyTestClass.render(java = isJava).page("Foo").content<Classlike>()
            val constructorList = classlike.symbolsForConstructors().second.symbols
            assertThat(constructorList.size).isEqualTo(1)
            assertThat((constructorList.single() as SymbolDetail).data.name).isEqualTo("Foo")
        }
    }

    @Test
    fun `Default no-arg constructors are not autogenerated for annotations`() {
        val classlikeJ = """
            public @interface Foo {}
        """.trimIndent().render(java = true).page("Foo").content<Classlike>()
        val classlikeK = """
            public annotation class Foo {}
        """.trimIndent().render(java = false).page("Foo").content<Classlike>()

        for (classlike in listOf(classlikeJ/*, classlikeK*/)) { // TODO: b/195529157
            val constructorList = classlike.symbolsForConstructors().second.symbols
            assertThat(constructorList).isEmpty()
        }
    }

    @Test // Interfaces "extend" other interfaces, while classes "implement" interfaces
    fun `Interface extending another interface uses correct keyword`() {
        val signatureJ = """
            public interface Foo {}
            public interface Bar extends Foo {}
        """.trimIndent().render(java = true).page("Bar").content<Classlike>().data.signature
        val signatureK = """
            public interface Foo {}
            public interface Bar : Foo {}
        """.trimIndent().render(java = false).page("Bar").content<Classlike>().data.signature

        assertThat(signatureJ.data.extends).isEmpty()
        assertThat(signatureK.data.extends).isEmpty()
        assertThat(signatureJ.data.implements.single().data.name).isEqualTo("Test.Foo")
        assertThat(signatureK.data.implements.single().data.name).isEqualTo("Foo")
        // Now route to DefaultClassSignatureTest.`Interfaces extend other interfaces`()
    }

    @Test // Kotlin.enum.valueOf isn't in the descriptor tree, despite being callable: b/235992590
    fun `Enum valueOf return type is synthetic`() {
        val enumDModuleK = """
            |enum class Foo { BAR, BAZ }
        """.render()
        val enumDModuleJ = """
            |public enum Foo { BAR, BAZ }
        """.render(java = true)
        for (enumDModule in listOf(/*enumDModuleK, */enumDModuleJ)) {
            // Test upstream behavior: only one enumJ.valueOf exists on the enum & it returns a Foo
            val dFunctions = enumDModule.explicitClasslike("Foo").functions
            val valueOfDFunctions = dFunctions.filter { it.name == "valueOf" }
            assertThat(valueOfDFunctions.size).isEqualTo(1)
            val valueOfDFunctionReturnType = valueOfDFunctions.single().type
            assertThat(valueOfDFunctionReturnType is JavaObject).isFalse()
            assertThat((valueOfDFunctionReturnType as GenericTypeConstructor).dri.classNames)
                .isEqualTo("Test.Foo")
            // Verify the final result in dackka is correct, and that valueOf is marked inherited.
            val enumClass = enumDModule.page("Foo").content<Classlike>()
            val publicFuns = enumClass.methodSummaryItems()
            val valueOfMethod = publicFuns.single { "valueOf" == it.name() }
            val valueOfReturnType = (valueOfMethod.data.title as TypeSummary).data.type
            assertThat(valueOfReturnType.name()).isEqualTo("Test.Foo")
        }
    }

    private fun DModule.page(name: String = "Foo"): DevsitePage {
        val classlike = explicitClasslike(name)
        val (holder, pathProvider) = holderAndProvider(this)
        val extFunctionMap = runBlocking { holder.extensionFunctionMap() }
        val converter = ClasslikeDocumentableConverter(
            displayLanguage,
            classlike,
            pathProvider,
            holder,
            extFunctionMap.getOrDefault(name, emptyList())
        )
        return runBlocking { converter.classlike() }
    }

    private fun DModule.page(name: DModule.() -> DClasslike): DevsitePage {
        val (holder, pathProvider) = holderAndProvider(this)
        val extFunctionMap = runBlocking { holder.extensionFunctionMap() }
        val converter = ClasslikeDocumentableConverter(
            displayLanguage,
            name(),
            pathProvider,
            holder,
            extFunctionMap.getOrDefault(name, emptyList())
        )
        return runBlocking { converter.classlike() }
    }

    private fun DModule.companionFor(name: String = "Foo") =
        explicitClasslike("Foo").classlikes.single { it.name == "Companion" }

    private fun String.possiblyAsGetter() = if (displayLanguage == Language.KOTLIN) this
    else "get" + this.capitalize()

    private fun Classlike.methodDetailsItems() = (
        symbolsFor(publicMethodsTitle(displayLanguage)).second.symbols +
            symbolsFor(protectedMethodsTitle(displayLanguage)).second.symbols
        ).map { it as SymbolDetail }

    private fun Classlike.methodSummaryItems() =
        summaryItemsFor(publicMethodsTitle(displayLanguage)) +
            summaryItemsFor(protectedMethodsTitle(displayLanguage))

    private fun Classlike.methodSymbol(name: String = "foo") =
        methodSummaryItems().singleOrNull { it.name() == name }

    private fun Classlike.propertySummaryItems() =
        summaryItemsFor(publicPropertiesTitle(displayLanguage)) +
            summaryItemsFor(protectedPropertiesTitle(displayLanguage))

    private fun Classlike.propertyDetailsItems() =
        symbolsFor(publicPropertiesTitle(displayLanguage)).second.symbols +
            symbolsFor(protectedPropertiesTitle(displayLanguage)).second.symbols

    private fun Classlike.propertySymbol(name: String = "foo") =
        propertySummaryItems().singleOrNull { it.name() == name }

    private fun SummaryList<SingleColumnSummaryItem<SymbolSummary>>.constructor() =
        data.items.item().data.description

    private fun Classlike.noSectionFor(symbolsName: String) =
        data.symbolTypes.none { it.first.title() == symbolsName }

    private fun Classlike.noSymbolsFor(symbolsName: String) = noSectionFor(symbolsName) ||
        symbolsFor(symbolsName).first.size == 0

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
