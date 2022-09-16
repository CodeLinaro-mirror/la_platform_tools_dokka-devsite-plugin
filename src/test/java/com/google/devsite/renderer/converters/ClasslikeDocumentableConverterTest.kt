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

import com.google.common.truth.IterableSubject
import com.google.common.truth.Truth.assertThat
import com.google.devsite.capitalize
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.PackageSummary
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.SingleColumnSummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.companionName
import com.google.devsite.renderer.converters.testing.content
import com.google.devsite.renderer.converters.testing.enumValues
import com.google.devsite.renderer.converters.testing.from
import com.google.devsite.renderer.converters.testing.functionSummary
import com.google.devsite.renderer.converters.testing.inheritedFields
import com.google.devsite.renderer.converters.testing.inheritedFunctions
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.modifiers
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.nestedTypes
import com.google.devsite.renderer.converters.testing.nonInstance
import com.google.devsite.renderer.converters.testing.projectionName
import com.google.devsite.renderer.converters.testing.single
import com.google.devsite.renderer.converters.testing.summaryItemsFor
import com.google.devsite.renderer.converters.testing.symbolsFor
import com.google.devsite.renderer.converters.testing.symbolsForConstructors
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.renderer.converters.testing.title
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DObject
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
            assertThat(summary.items().first().functionSummary().name()).isEqualTo("aar")
            assertThat(summary.items().last().functionSummary().name()).isEqualTo("bar")
        }
        kotlinOnly {
            val classlike = page.content<Classlike>()
            val (summary) = classlike.symbolsFor("Public functions")
            assertThat(summary.items().first().functionSummary().name()).isEqualTo("foo")
            assertThat(summary.items().last().functionSummary().name()).isEqualTo("zoo")
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
            assertThat(prop.data.description.name()).isEqualTo(expected[i])
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

        assertThat(summary.item().functionSummary().name()).isEqualTo("foo")
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

        assertThat(summary.single().functionSummary().name()).isEqualTo("foo")
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

        assertThat(summary.item().functionSummary().name()).isEqualTo("foo")
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

            val detail = constructor.second.symbols.single()
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
        val module = """
            |class Foo {
            |    companion object FooCompanion
            |}
            |class Bar {
            |    companion object
            |}
        """.render()
        val foo = module.page("Foo").content<Classlike>()
        val bar = module.page("Bar").content<Classlike>()

        kotlinOnly {
            assertThat(foo.nestedTypes().first.items()).hasSize(1)
            assertThat(bar.nestedTypes().first.items()).isEmpty()
        }
        javaOnly {
            fun Classlike.companionName() = nestedTypes().first.item().link().name
            assertThat(foo.companionName()).isEqualTo("Foo.FooCompanion")
            assertThat(bar.companionName()).isEqualTo("Bar.Companion")
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

    @Ignore
    @Test
    fun `Class signature and hierarchy can contain generics in 4x`() {
        val pageK = """
            |class Foo : List<String>
        """.render().page("Foo").content<Classlike>()
        val pageJ = """
            |public class Foo extends List<String> {}
        """.render(java = true).page("Foo").content<Classlike>()

        for (page in listOf(pageJ, pageK)) {
            val classSignature = page.data.signature.data
            assertThat(classSignature.type).isEqualTo("class")
            assertThat(classSignature.extends.single().data.name).isEqualTo("List<String>")
            val hierarchy = page.data.hierarchy.data
            assertThat(hierarchy.parents.size).isEqualTo(2)
            assertThat(hierarchy.parents.first().data.name).isEqualTo("List<String>")
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
            val fooDoitDocs = fooDoit.functionSummary().data.description.text()
            val barDoit = barClass.methodSummaryItems().single()
            val barDoitDocs = barDoit.functionSummary().data.description.text()
            val bazDoit = bazClass.methodSummaryItems().single()
            val bazDoitDocs = bazDoit.functionSummary().data.description.text()

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
                val fooDemocracy = fooClass.propertySummaryItems().single().functionSummary()
                assertThat(fooDemocracy.data.description.text()).isEqualTo("thunderous applause")
                val barDemocracy = barClass.propertySummaryItems().single().functionSummary()
                assertThat(barDemocracy.data.description.text()).isEqualTo("thunderous applause")
                val bazDemocracy = bazClass.propertySummaryItems().single().functionSummary()
                assertThat(bazDemocracy.data.description.text()).isEqualTo("KotOR")

                // Using {@inheritDoc} in kotlin is wrong
                val mazClass = pages.page("maz").content<Classlike>()
                val mazDoit = mazClass.methodSummaryItems().single()
                val mazDoitDocs = mazDoit.functionSummary().data.description.text()
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
            assertThat(doit.functionSummary().data.description.text()).isEqualTo("dew it")
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
            it.data.description.data.signature as FunctionSignature
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
        val moduleK = """
            |class Test {
            |   open class Parent {
            |       var b: Int = 8
            |       @JvmField
            |       var a: String = "9"
            |   }
            |   class Child: Parent()
            |}
        """.render()
        val moduleJ = """
            |public class Parent {
            |   private int b = 8;
            |   public int getB() { return b; }
            |   public void setB(int newB) { b = newB; }
            |   private String a = "9";
            |   public String getA() { return a; }
            |   public void setA(String newA) { a = newA; }
            |}
            |public class Child extends Parent
        """.render(java = true)

        for (module in listOf(moduleK, moduleJ)) {
            // This is a test of upstream dokka
            val dParent = module.explicitClasslike("Parent")
            val dChild = module.explicitClasslike("Child")
            // TODO: propertyA should be private. b/241259955 go/dokka-upstream-bug/2603
            val dPropertyA = dChild.properties.single { it.name == "a" }
            val getterDri = dPropertyA.getter!!.dri

            assertThat(dPropertyA.dri.fullName).contains(dParent.dri.fullName)
            assertThat(dPropertyA.dri.fullName).isEqualTo("androidx.example.Test.Parent")
            assertThat(getterDri.fullName).contains(dParent.dri.fullName)
            assertThat(getterDri.fullName).isEqualTo("androidx.example.Test.Parent")

            // This is a test of dackka
            val childPage = module.page("Child").content<Classlike>()

            // TODO: `var b` is not `@JvmField`; should be missing. This is also b/241259955
            val inheritedProps = childPage.inheritedFields!!.data.inheritedSymbolSummaries
            val inhPropNames = inheritedProps.values.single().items().map { it.name() }
            assertThat(inhPropNames).containsExactly("a", "b").inOrder()
            if (displayLanguage == Language.JAVA && module == moduleJ) {
                val inheritedFuns = childPage.inheritedFunctions!!.data.inheritedSymbolSummaries
                val inhFunNames = inheritedFuns.values.single().items().map { it.name() }
                assertThat(inhFunNames).containsExactly("getA", "getB", "setA", "setB").inOrder()
            }
        }
    }

    @Test
    fun `Externally-inherited vars 4x language test`() {
        val moduleK = """
            |class Test {
            |   class Child: kotlin.RuntimeException()
            |}
        """.render()
        val moduleJ = """
            |public class Child extends java.lang.RuntimeException
        """.render(java = true)

        for (module in listOf(moduleK, moduleJ)) {
            // kotlin.Throwable is an `actual typealias`.
            // java Throwable: https://docs.oracle.com/javase/7/docs/api/java/lang/Throwable.html
            val throwableDRI = if (module == moduleK) "kotlin.Throwable" else "java.lang.Throwable"
            // add/get is not consolidated into a property in Kotlin
            val sixFuns = listOf(
                "addSuppressed", "getSuppressed",
                "fillInStackTrace", "printStackTrace",
                "getLocalizedMessage",
                "initCause"
            )
            // "Message" becomes a "val" in Kotlin, which hides its getter. ToString is from Object.
            val missingInKotlin = listOf("getMessage", "toString")
            // TODO: figure out why stackTrace is accessors in Kotlin (but a field in Java) upstream
            // Maybe inherited accessors don't get merged into a property? Cause/stackTrace are
            // private fields upstream, as is `*final* String detailMessage`....
            val stackTraceAccessors = listOf("getStackTrace", "setStackTrace")
            val printOverloads = listOf("printStackTrace", "printStackTrace")
            val expectedProps = listOf("cause", if (module == moduleK) "message" else "stackTrace")
            val expectedFuns = if (module == moduleK) sixFuns + printOverloads + stackTraceAccessors
            else sixFuns + missingInKotlin
            // dackka extracts getters and setters from properties to display separately as-Java
            val bonusInDackka = if (module == moduleK) emptyList()
            else listOf("getCause") + stackTraceAccessors

            // This is a test of the upstream dokka Documentables tree
            val dChild = module.explicitClasslike("Child")
            assertThat(dChild.properties.size).isEqualTo(2)
            assertThat(dChild.functions.size).isEqualTo(if (module == moduleK) 10 else 8)
            assertThat(dChild.properties.names()).containsExactlyElementsIn(expectedProps)
            assertThat(dChild.functions.names()).containsExactlyElementsIn(expectedFuns)

            if (module == moduleK) {
                val dMessageProp = dChild.properties.single { it.name == "message" }
                val dGetStackTrace = dChild.functions.single { it.name == "getStackTrace" }
                assertThat(dMessageProp.dri.fullName).contains(throwableDRI)
                assertThat(dMessageProp.getter!!.dri.fullName).contains(throwableDRI)
                assertThat(dGetStackTrace.dri.fullName).contains(throwableDRI)
            } else {
                val dCauseProp = dChild.properties.single { it.name == "cause" }
                val dStackTrace = dChild.properties.single { it.name == "stackTrace" }
                assertThat(dCauseProp.dri.fullName).contains(throwableDRI)
                assertThat(dCauseProp.getter!!.dri.fullName).contains(throwableDRI)
                assertThat(dStackTrace.dri.fullName).contains(throwableDRI)
                assertThat(dStackTrace.getter!!.dri.fullName).contains(throwableDRI)
            }

            // This is a test of the dackka Components tree
            val childPage = module.page("Child").content<Classlike>()

            val inheritedFuns = childPage.inheritedFunctions!!.from(throwableDRI)!!.value
            val inheritedProps = childPage.inheritedFields!!.from(throwableDRI)!!.value
            val funNames = inheritedFuns.items().map { it.name() }
            val propNames = inheritedProps.items().map { it.name() }

            assertThat(funNames.size).isEqualTo(if (module == moduleK) 10 else 11)
            assertThat(propNames.size).isEqualTo(2)
            assertThat(funNames).containsExactlyElementsIn(expectedFuns + bonusInDackka)
            assertThat(propNames).containsExactlyElementsIn(expectedProps)
        }
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
    fun `Failed resolution exception includes line number`() {
        val message = assertFails {
            """
            |/** @param foo does not exist */
            |class Foo<T: Number, U>() {}
            """.render().page()
        }.message
        assertThat(message).contains("androidx/example/Test.kt at line 2")
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
            assertThat(barMethod.modifiers()).isEqualTo(listOf("static", "final"))
            assertThat(bazMethod.modifiers()).isEqualTo(listOf("static", "final"))
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
            assertThat(barProp.modifiers()).isEqualTo(listOf("static", "final"))
            assertThat(bazProp.modifiers()).isEqualTo(listOf("static", "final"))
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

        val moduleJ = """${javaHeader("Foo")}
            |public class Foo {
            |  public static final void bar() {}
            |  public static final String baz = "baz"
            |  public static Foo INSTANCE = new Foo()
            |}
        """.renderWithoutLanguageHeader()
        val classlikeJ = moduleJ.page("Foo").content<Classlike>()

        for (classlike in listOf(classlikeJ, classlikeK)) {
            assertThat(classlikeK.noSymbolsFor(publicCompanionFunctionsTitle())).isTrue()
            assertThat(classlikeK.noSymbolsFor(publicCompanionPropertiesTitle())).isTrue()
            assertThat(classlike.methodSummaryItems()).hasSize(1)
            val props = classlike.summaryItemsFor(publicPropertiesTitle(displayLanguage))
            val consts = classlike.summaryItemsFor(constantsTitle())
            assertThat(consts.map { it.name() }).containsExactly("baz")
            if (displayLanguage == Language.KOTLIN && classlike == classlikeK) {
                assertThat(props).isEmpty()
            } else {
                assertThat(props.single().name()).isEqualTo("INSTANCE")
            }

            val barMethod = classlike.methodSymbol("bar")!!
            val barModifiers = barMethod.modifiers()
            val bazConst = consts.single { it.name() == "baz" }
            kotlinOnly {
                if (classlike == classlikeK) assertThat(barModifiers).isEmpty()
                else assertThat(barModifiers).isEqualTo(listOf("java-static"))
                assertThat(bazConst.modifiers()).isEqualTo(listOf("const"))
            }
            javaOnly {
                assertThat(barModifiers).isEqualTo(listOf("static", "final"))
                assertThat(bazConst.modifiers()).isEqualTo(listOf("static", "final"))
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

    @Ignore // Pending getter/setter implementation/changes landing
    @Test
    fun `JvmStatic is correctly handled in java`() {
        if (displayLanguage != Language.JAVA) return
        val page = """
            |class Foo {
            |   companion object {
            |       @JvmStatic fun bar() {}
            |       @JvmStatic val baz = 8
            |   }
            |}
        """.render().page("Foo").content<Classlike>()

        val nestedTypes = page.summaryItemsFor("Nested types")
        val methods = page.methodSummaryItems()
        val staticMethods = methods.filter { it.modifiers().contains("static") }

        // method `bar` can be referenced both as `Foo.bar` and `Foo.Companion.bar`
        assertThat(nestedTypes).hasSize(1)
        assertThat(methods.map { it.name() }).containsExactly("bar", "getBaz")
        assertThat(staticMethods).hasSize(2)
    }

    @Test
    fun `JvmField in companion object is static field in java and unchanged in kotlin`() {
        val module = """
            |class Foo {
            |   companion object {
            |       @JvmField val BAR = 8
            |       @JvmField var BAZ = "abc"
            |   }
            |}
        """.render()

        val classPage = module.page("Foo").content<Classlike>()
        val companionPage = module.page("Companion").content<Classlike>()

        val nestedTypes = classPage.nestedTypes()
        val fields = classPage.propertySummaryItems()
        val staticFields = fields.filter { it.modifiers().contains("static") }

        javaOnly {
            // static fields are removed from the companion object
            assertThat(nestedTypes.first.size).isEqualTo(1)
            assertThat(classPage.methodSummaryItems()).isEmpty()
            assertThat(fields.map { it.name() }).containsExactly("BAR", "BAZ")
            assertThat(staticFields).hasSize(2)

            // companion object exists, but has no fields
            assertThat(companionPage.methodSummaryItems()).isEmpty()
            assertThat(companionPage.propertySummaryItems().nonInstance()).isEmpty()
        }

        kotlinOnly {
            val companionProperties =
                classPage.symbolsFor(publicCompanionPropertiesTitle()).first.items()

            // on the other hand, nothing has changed from the kotlin's point of view
            assertThat(companionProperties.map { it.name() }).containsExactly("BAR", "BAZ")
        }
    }

    @Test
    fun `lateinit property in companion object is static field in java and unchanged in kotlin`() {
        val module = """
            |class Foo {
            |   companion object {
            |       lateinit var bar: String;
            |   }
            |}
        """.render()

        val classPage = module.page("Foo").content<Classlike>()
        val companionPage = module.page("Companion").content<Classlike>()

        val nestedTypes = classPage.nestedTypes()
        val fields = classPage.propertySummaryItems()
        val staticFields = fields.filter { it.modifiers().contains("static") }

        val companionPageMethods = companionPage.methodSummaryItems()
        val companionPageFields = companionPage.propertySummaryItems().nonInstance()

        javaOnly {
            // static fields are removed from the companion object
            assertThat(nestedTypes.first.size).isEqualTo(1)
            assertThat(classPage.methodSummaryItems()).isEmpty()
            assertThat(fields.map { it.name() }).containsExactly("bar")
            assertThat(staticFields).hasSize(1)

            // everything is duplicated in companion objects in Kotlin
            // assertThat(companionPageMethods.names()).containsExactly("getBar", "setBar")
            assertThat(companionPageMethods).isEmpty()
            assertThat(companionPageFields.map { it.name() }).containsExactly("bar")
        }

        kotlinOnly {
            val companionProperties =
                classPage.symbolsFor(publicCompanionPropertiesTitle()).first.items()

            // on the other nothing has changed from the kotlin's point of view
            assertThat(companionProperties.map { it.name() }).containsExactly("bar")
        }
    }

    @Test
    fun `const property in companion object is static field in java and const in kotlin`() {
        val classlike = """
            |class Foo {
            |   companion object {
            |       const val MARGIN = 9
            |   }
            |}
        """.render().page("Foo").content<Classlike>()

        val fields = classlike.propertySummaryItems()

        val constantFields = classlike.summaryItemsFor("Constants")
        assertThat(constantFields.map { it.name() }).containsExactly("MARGIN")

        javaOnly {
            assertThat(classlike.methodSummaryItems()).isEmpty()

            assertThat(fields.map { it.name() }).isEmpty()
        }

        kotlinOnly {
            val companionProperties =
                classlike.summaryItemsFor(publicCompanionPropertiesTitle()) +
                    classlike.summaryItemsFor(protectedCompanionPropertiesTitle())

            assertThat(companionProperties).isEmpty()
        }
    }

    @Ignore // Pending getter/setter implementation/changes landing
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
        val staticMethods = methods.filter { it.modifiers().contains("static") }

        javaOnly {
            assertThat(methods.map { it.name() }).containsExactly("computeBar")
            assertThat(staticMethods).hasSize(1)
        }

        kotlinOnly {
            val companionProperties =
                page.symbolsFor(publicCompanionPropertiesTitle()).first.items()
            val companionFunctions =
                page.symbolsFor(publicCompanionFunctionsTitle()).first.items()

            assertThat(companionProperties).hasSize(1)
            assertThat(companionFunctions).isEmpty()
        }
    }

    @Test
    fun `companion objects can be named`() {
        val module = """
            |class Foo {
            |    companion object Named {
            |        fun bar() {}
            |    }
            |}
        """.render()

        val classPage = module.page("Foo").content<Classlike>()
        val companionPage = module.page("Named").content<Classlike>()

        val nestedTypes = classPage.nestedTypes().first.items()
        val methods = classPage.methodSummaryItems()
        val companionPageMethods = companionPage.methodSummaryItems()

        assertThat(nestedTypes.map { it.name() }).containsExactly("Foo.Named")
        assertThat(companionPageMethods.map { it.name() }).containsExactly("bar")
        assertThat(methods).isEmpty()

        kotlinOnly {
            val companionFunctions = classPage.symbolsFor(publicCompanionFunctionsTitle()).first
            assertThat(companionFunctions.items().map { it.name() }).containsExactly("bar")
        }
    }

    @Test
    fun `companion objects can inherit`() {
        val module = """
            |class Companionable {
            |   fun bar() {}
            |}
            |
            |class Foo {
            |    companion object : Companionable
            |}
        """.render()

        val classPage = module.page("Foo").content<Classlike>()
        val nestedTypes = classPage.nestedTypes().first.items()
        assertThat(nestedTypes.map { it.name() }).containsExactly("Foo.Companion")

        val companionPage = module.page("Companion").content<Classlike>()

        val companionInheritedMethodsSummaries =
            companionPage.summaryItemsFor(inheritedMethodsTitle(displayLanguage))

        assertThat(companionInheritedMethodsSummaries.map { it.name() }).containsExactly("bar")

        kotlinOnly {
            // Inherited companion functions are not hoisted
            assertThat(classPage.noSymbolsFor(inheritedMethodsTitle(displayLanguage))).isTrue()
        }
    }

    @Test // TODO: non-overridden inherited elements in companion objects are missing
    fun `companion objects that inherits still can have static forwarders`() {
        // Aka you can add the JvmStatic-ness in an override
        // NOTE: "@JvmField cannot be applied to a property that overrides another property"
        // NOTE: "@JvmField can only be applied to final property"
        // NOTE: @JvmStatic can only be applied to elements in a static context, e.g. an object,
        // not an interface like Companionable, and you can't inherit from singletons/static context
        // NOTE: "property in an interface cannot have a backing field" (no getters in interface)
        val module = """
            |open class ForDefaultProperties {
            |   open var notStaticNonOverriddenVar = "bar"
            |}
            |interface Companionable {
            |   fun becomesStaticFun() {}
            |   var becomesLateinitVar: String
            |   fun notStaticNonOverriddenFun() {}
            |   fun notStaticOverriddenFun() {}
            |   var notStaticOverriddenVar: String
            |}
            |
            |class Foo {
            |    companion object : Companionable, ForDefaultProperties() {
            |       @JvmStatic override fun becomesStaticFun() {}
            |       override lateinit var becomesLateinitVar: String
            |       override fun notStaticOverriddenFun() {}
            |       override var notStaticOverriddenVar: String = "baz"
            |    }
            |}
        """.render()

        val classPage = module.page("Foo").content<Classlike>()
        val companionPage = module.page("Companion").content<Classlike>()
        assertThat(classPage.nestedTypes().first.items().map { it.name() })
            .containsExactly("Foo.Companion")

        val methods = classPage.methodSummaryItems()
        val fields = classPage.propertySummaryItems()
        val companionPageMethods = companionPage.methodSummaryItems()
        val companionPageFields = companionPage.propertySummaryItems()
        val companionInheritedProperties = companionPage.inheritedFields!!
            .from("androidx.example.Companionable")?.value?.items() ?: emptyList()
        val companionInheritedFunctions = companionPage.inheritedFunctions!!
            .from("androidx.example.Companionable")?.value?.items() ?: emptyList()
        val inheritedDefaultProp = companionPage.inheritedFields!!
            .from("androidx.example.ForDefaultProperties")?.value?.items() ?: emptyList()

        // Overridden functions
        assertThat(companionPageMethods.map { it.name() })
            .containsExactly("becomesStaticFun", "notStaticOverriddenFun")
        // Overridden vars

        kotlinOnly {
            val companionProperties =
                classPage.symbolsFor(publicCompanionPropertiesTitle()).first.items()
            val companionFunctions =
                classPage.symbolsFor(publicCompanionFunctionsTitle()).first.items()
            val zippedProps = companionProperties.zip(companionPageFields)
            val zippedFuns = companionFunctions.zip(companionPageMethods)
            assertThat(companionProperties.map { it.name() })
                .isEqualTo(companionPageFields.map { it.name() })
            assertThat(companionFunctions.map { it.name() })
                .isEqualTo(companionPageMethods.map { it.name() })

            assertThat(companionPageFields.map { it.name() })
                .containsExactly("notStaticOverriddenVar", "becomesLateinitVar")

            // These functions are overridden and newly made static
            assertThat(companionPageMethods.map { it.name() })
                .containsExactly("becomesStaticFun", "notStaticOverriddenFun")
            // These fields are overridden and newly made static
            assertThat(companionPageFields.map { it.name() })
                .containsExactly("becomesLateinitVar", "notStaticOverriddenVar")
            // These functions are not overridden but TODO staticness
            assertThat(companionInheritedFunctions.map { it.name() })
                .containsExactly("notStaticNonOverriddenFun")
            // These fields are not ovverridden but TODO staticness
            assertThat(companionInheritedProperties.map { it.name() }).isEmpty()
            assertThat(inheritedDefaultProp.map { it.name() })
                .containsExactly("notStaticNonOverriddenVar")
        }
        javaOnly {
            assertThat(companionPageFields.map { it.name() })
                .containsExactly("notStaticOverriddenVar", "becomesLateinitVar")
            assertThat(methods.map { it.name() }).containsExactly("becomesStaticFun")
            assertThat(fields.map { it.name() }).containsExactly("becomesLateinitVar")
        }
    }

    @Test
    fun `Comprehensive companion function-property hoist-duplication test`() {
        val module = """
            |open class TheContainer {
            |    companion object TheCompanion {
            |        fun publicConlyFun() = 1
            |        protected fun protectedConlyFun() = 2
            |        @JvmStatic fun publicDuplicatedFun() = 3
            |        @JvmStatic protected fun protectedDuplicatedFun() = 4
            |        val publicConlyProp = 5
            |        protected val protectedConlyProp = 6
            |        @JvmStatic val publicDuplicatedProp = 7
            |        @JvmStatic protected val protectedDuplicatedProp = 8
            |        @JvmField val publicHoistedField = 9
            |        @JvmField protected val protectedHoistedField = 10
            |        const val publicHoistedConst = 11
            |        protected const val protectedHoistedConst = 12
            |    }
            |}
        """.render()

        fun Iterable<TwoPaneSummaryItem<TypeSummary, SymbolSummary>>.names() = map { it.name() }
        val names = listOf(
            "publicConlyFun",
            "protectedConlyFun",
            "publicDuplicatedFun",
            "protectedDuplicatedFun",
            "publicHoistedField",
            "protectedHoistedField",
            "publicHoistedConst",
            "protectedHoistedConst"
        ) + /*if (displayLanguage == Language.KOTLIN)*/ listOf(
            "publicConlyProp",
            "protectedConlyProp",
            "publicDuplicatedProp",
            "protectedDuplicatedProp"
        ) /*else listOf(        // getter/setters generation still needs broader work b/168340963
            "getPublicConlyProp",
            "getProtectedConlyProp",
            "getPublicDuplicatedProp",
            "getProtectedDuplicatedProp"
        )*/

        val containerClass = module.page("TheContainer").content<Classlike>()
        val companionClass = module.page("TheCompanion").content<Classlike>()
        assertThat(containerClass.nestedTypes().first.single().name())
            .isEqualTo("TheContainer.TheCompanion")
        assertThat(containerClass.companionName())
            .isEqualTo("TheContainer.TheCompanion")
        // Pull public/protected elements that are hoisted or are in the companion
        // "CompanionONLY" conly is a misnomer right now; includes methods in both companion&parent
        var publicHoistedFuns = if (displayLanguage == Language.KOTLIN)
            containerClass.summaryItemsFor(publicCompanionFunctionsTitle())
        else containerClass.summaryItemsFor(publicMethodsTitle(displayLanguage))
            .filter { "static" in it.modifiers() }
        var protectedHoistedFuns = if (displayLanguage == Language.KOTLIN)
            containerClass.summaryItemsFor(protectedCompanionFunctionsTitle())
        else containerClass.summaryItemsFor(protectedMethodsTitle(displayLanguage))
            .filter { "static" in it.modifiers() }
        var publicHoistedProps = if (displayLanguage == Language.KOTLIN)
            containerClass.summaryItemsFor(publicCompanionPropertiesTitle())
        else containerClass.summaryItemsFor(publicPropertiesTitle(displayLanguage))
            .filter { "static" in it.modifiers() }
        var protectedHoistedProps = if (displayLanguage == Language.KOTLIN)
            containerClass.summaryItemsFor(protectedCompanionPropertiesTitle())
        else containerClass.summaryItemsFor(protectedPropertiesTitle(displayLanguage))
            .filter { "static" in it.modifiers() }
        var publicConlyFuns = companionClass
            .summaryItemsFor(publicMethodsTitle(displayLanguage))
        var protectedConlyFuns = companionClass
            .summaryItemsFor(protectedMethodsTitle(displayLanguage))
        var publicConlyProps = companionClass
            .summaryItemsFor(publicPropertiesTitle(displayLanguage))
        var protectedConlyProps = companionClass
            .summaryItemsFor(protectedPropertiesTitle(displayLanguage))
        // Now filter out elements in both hoisted and conly and put them in duplicated
        val publicDuplicatedFuns = publicConlyFuns.intersect(publicHoistedFuns)
        publicConlyFuns -= publicDuplicatedFuns
        publicHoistedFuns -= publicDuplicatedFuns
        val protectedDuplicatedFuns = protectedConlyFuns.intersect(protectedHoistedFuns)
        protectedConlyFuns -= protectedDuplicatedFuns
        protectedHoistedFuns -= protectedDuplicatedFuns
        val publicDuplicatedProps = publicConlyProps.intersect(publicHoistedProps)
        publicConlyProps -= publicDuplicatedProps
        publicHoistedProps -= publicDuplicatedProps
        val protectedDuplicatedProps = protectedConlyProps.intersect(protectedHoistedProps)
        protectedConlyProps -= protectedDuplicatedProps
        protectedHoistedProps -= protectedDuplicatedProps
        // No visibility distinction for constants: b/237083570
        // Constants are all hoisted in Java and duplicated in Kotlin
        val hoistedConstants = containerClass.summaryItemsFor("Constants")
        val companionConstants = companionClass.summaryItemsFor("Constants")

        javaOnly {
            // Perform asserts based on name mangling
            assertThat(publicHoistedFuns.names()).containsExactlyElementsIn(
                names.filter { "ublic" in it && "Hoisted" in it && "Fun" in it }
            )
            assertThat(protectedHoistedFuns.names()).containsExactlyElementsIn(
                names.filter { "rotected" in it && "Hoisted" in it && "Fun" in it }
            )
            assertThat(publicHoistedProps.names()).containsExactlyElementsIn(
                names.filter {
                    "ublic" in it && "Hoisted" in it &&
                        ("Prop" in it || "Field" in it)
                }
            )
            assertThat(protectedHoistedProps.names()).containsExactlyElementsIn(
                names.filter {
                    "rotected" in it && "Hoisted" in it &&
                        ("Prop" in it || "Field" in it)
                }
            )
            assertThat(publicConlyFuns.names()).containsExactlyElementsIn(
                names.filter { "ublic" in it && "Conly" in it && "Fun" in it }
            )
            assertThat(protectedConlyFuns.names()).containsExactlyElementsIn(
                names.filter { "rotected" in it && "Conly" in it && "Fun" in it }
            )
            assertThat(publicConlyProps.names()).containsExactlyElementsIn(
                names.filter { "ublic" in it && "Conly" in it && "Prop" in it }
            )
            assertThat(protectedConlyProps.names()).containsExactlyElementsIn(
                names.filter { "rotected" in it && "Conly" in it && "Prop" in it }
            )
            assertThat(publicDuplicatedFuns.names()).containsExactlyElementsIn(
                names.filter { "ublic" in it && "Duplicated" in it && "Fun" in it }
            )
            assertThat(protectedDuplicatedFuns.names()).containsExactlyElementsIn(
                names.filter { "rotected" in it && "Duplicated" in it && "Fun" in it }
            )
            assertThat(publicDuplicatedProps.names()).containsExactlyElementsIn(
                names.filter { "ublic" in it && "Duplicated" in it && "Prop" in it }
            )
            assertThat(protectedDuplicatedProps.names()).containsExactlyElementsIn(
                names.filter { "rotected" in it && "Duplicated" in it && "Prop" in it }
            )
            assertThat(hoistedConstants.names()).containsExactlyElementsIn(
                names.filter { "Const" in it }
            )
            assertThat(companionConstants).isEmpty()
        }
        // In Kotlin, everything is hoisted and there is no `static`, i.e. everything is duplicated
        kotlinOnly {
            assertThat(publicHoistedFuns).isEmpty()
            assertThat(protectedHoistedFuns).isEmpty()
            assertThat(publicHoistedProps).isEmpty()
            assertThat(protectedHoistedProps).isEmpty()
            assertThat(publicConlyFuns).isEmpty()
            assertThat(protectedConlyFuns).isEmpty()
            assertThat(publicConlyProps).isEmpty()
            assertThat(protectedConlyProps).isEmpty()
            assertThat(publicDuplicatedFuns.names()).containsExactlyElementsIn(
                names.filter { "ublic" in it && "Fun" in it }
            )
            assertThat(protectedDuplicatedFuns.names()).containsExactlyElementsIn(
                names.filter { "rotected" in it && "Fun" in it }
            )
            assertThat(publicDuplicatedProps.names()).containsExactlyElementsIn(
                names.filter { "ublic" in it && ("Prop" in it || "Field" in it) }
            )
            assertThat(protectedDuplicatedProps.names()).containsExactlyElementsIn(
                names.filter { "rotected" in it && ("Prop" in it || "Field" in it) }
            )
            assertThat(hoistedConstants).containsExactlyElementsIn(companionConstants)
            assertThat(hoistedConstants.names()).containsExactlyElementsIn(
                names.filter { "Const" in it }
            )
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
    fun `Extension functions are linked correctly on both Java and Kotlin pages`() {
        val src = """
            |class Foo {
            |}
            |fun Foo.bar() = Unit
        """
        val classlike = src.render().page().content<Classlike>()
        val extFunction = classlike.symbolsFor("Extension functions").second.symbols[0]
        val url = extFunction.data.signature.data.name.data.url
        assertThat(url).endsWith("androidx/example/Foo.html#(androidx.example.Foo).bar()")
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

    @Test
    fun `Extension functions do not apply to different class with same name`() {
        val src = listOf(
            """
                |/src/main/kotlin/androidx/example/Foo1.kt
                |package foo1
                |
                |class Foo {
                |}
            """,
            """
                |/src/main/kotlin/androidx/example/Foo1Extension.kt
                |package foo1
                |
                |fun Foo.bar() = Unit
            """,
            """
                |/src/main/kotlin/androidx/example/Foo2.kt
                |package foo2
                |
                |class Foo {
                |}
            """,
            """
                |/src/main/kotlin/androidx/example/Foo2Extension.kt
                |package foo2
                |
                |fun Foo.baz() = Unit
            """
        )

        // Each Foo should have one extension function: bar for foo1, baz for foo2
        val pages = src.render().pages()
        assertThat(pages.size).isEqualTo(2)
        for (page in pages) {
            val classlike = page.content<Classlike>()
            val extFunctions = classlike.symbolsFor("Extension functions")
            assertThat(extFunctions.second.symbols.size).isEqualTo(1)
        }
    }

    @Test
    fun `Extension functions work for inner classes`() {
        val src = """
            |class Foo {
            |    class Bar {
            |    }
            |}
            |fun Foo.Bar.baz() = Unit
        """
        val classlike = src.render().page("Bar").content<Classlike>()
        val extFunctions = classlike.symbolsFor("Extension functions")
        assertThat(extFunctions.first.data.items).hasSize(1)
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
        val publicMethodNames = publicMethodSymbols.second.symbols.map { it.data.name }
        val protectedMethodNames = protectedMethodSymbols.second.symbols.map { it.data.name }

        kotlinOnly {
            // in Kotlin, we don't need to show getA / setA because property access is preferred.
            // assertThat(publicMethodNames).isEqualTo(listOf("getB", "setB")) // TODO: fix upstream
            assertThat(publicMethodNames).isEqualTo(listOf("getA", "getB", "setA", "setB"))
            // assertThat(protectedMethodNames).isEmpty() // TODO: fix upstream
            assertThat(protectedMethodNames).isEqualTo(listOf("getE", "setE"))
        }
        javaOnly {
            assertThat(publicMethodNames).isEqualTo(listOf("getA", "getB", "setA", "setB"))
            assertThat(protectedMethodNames).isEqualTo(listOf("getE", "setE"))
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
        val aProp = classlike.propertyDetailsItems().single { it.data.name == "a" }

        kotlinOnly {
            assertThat(aProp.data.symbolKind).isEqualTo(SymbolDetail.SymbolKind.READ_ONLY_PROPERTY)
            assertThat(publicMethodNames).isEqualTo(listOf("getA")) // TODO: fix upstream
            // assertThat(publicMethodNames).isEmpty()
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
            assertThat(constructorList.single().data.name).isEqualTo("Foo")
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

    @Test
    fun `companion to inner static class 4x test`() {
        val moduleK = """
            |fun topLevelFun() = 5
            |object TopLevelObject {
            |   fun topObjectFun() = 5
            |}
            |class Container {
            |   companion object {
            |       fun companionFun() = 5
            |       const val hoistedField = 5
            |   }
            |}
        """.render()
        val sourceJ = javaHeader("Container") + """
            |public class Container {
            |   public static class Companion {
            |      public static int companionFun() {}
            |   }
            |   public static final int hoistedField = 5
            |}
        """ + javaHeader("TestKt") + """
            |public static class TestKt {
            |   public static int topLevelFun() {}
            |}
        """ + javaHeader("TopLevelObject") + """
            |public class TopLevelObject {
            |   public static TopLevelObject INSTANCE = TopLevelObject()
            |   public static int topObjectFun() {}
            |}
        """.trimIndent()
        val moduleJ = sourceJ.renderWithoutLanguageHeader()

        fun TwoPaneSummaryItem<*, SymbolSummary>.urlSuffix() =
            data.description.data.signature.data.name.data.url.substringAfter("example/")

        for (module in listOf(moduleK, moduleJ)) {
            val testKt = if (displayLanguage == Language.JAVA || module == moduleJ)
                module.page("TestKt").content<Classlike>()
            else null
            val packagePage = if (displayLanguage == Language.KOTLIN && module == moduleK)
                module.packagePage().content<PackageSummary>()
            else null
            // This is top-level in Kotlin and in a Kt class in Java
            val topLevelFun = packagePage?.data?.topLevelFunctionsSummary?.item()
                ?: testKt!!.methodSummaryItems().single()
            assertThat(topLevelFun.name()).isEqualTo("topLevelFun")
            javaOnly { assertThat(topLevelFun.modifiers()).contains("static") }
            if (packagePage != null) {
                assertThat(topLevelFun.urlSuffix()).isEqualTo("package-summary.html#topLevelFun()")
            } else {
                assertThat(topLevelFun.urlSuffix()).isEqualTo("TestKt.html#topLevelFun()")
            }

            // top-level object -> static inner class of synthetic Kt class
            val topObject = module.page("TopLevelObject").content<Classlike>()
            val topObjectFun = topObject.methodSummaryItems().single()
            assertThat(topObjectFun.name()).isEqualTo("topObjectFun")
            javaOnly { assertThat(topObjectFun.data.title.data.modifiers).contains("static") }
            assertThat(topObjectFun.urlSuffix()).isEqualTo("TopLevelObject.html#topObjectFun()")

            if (testKt != null) assertThat(testKt.nestedTypes().first.items()).isEmpty()
            else assertThat(packagePage!!.data.objects.data.items.map { it.name() })
                .contains("TopLevelObject")

            fun IterableSubject.containsPublicMaybeStatic() =
                if (module == moduleK) this.containsExactly("public", "static").inOrder()
                else this.containsExactly("public").inOrder()

            if (displayLanguage == Language.KOTLIN && module == moduleK)
                assertThat(topObject.data.signature.data.type).isEqualTo("object")
            else assertThat(topObject.data.signature.data.type).isEqualTo("class")

            javaOnly {
                // top-level static classes don't exist in Java
                assertThat(topObject.modifiers()).containsPublicMaybeStatic()
                // top-level Kotlin objects become Java non-static classes with no constructor
                // but a static INSTANCE field that contains a static instance
                val instanceVal = topObject.propertySummaryItems().single()
                assertThat(instanceVal.name()).isEqualTo("INSTANCE")
                assertThat(instanceVal.modifiers()).contains("static")
            }

            // companion <-> inner static class
            val companionObject = module.page("Companion").content<Classlike>()
            val companionFun = companionObject.methodSummaryItems().single()
            assertThat(companionFun.name()).isEqualTo("companionFun")
            javaOnly { assertThat(companionFun.modifiers()).contains("static") }
            assertThat(companionFun.urlSuffix())
                .isEqualTo("Container.Companion.html#companionFun()")

            if (displayLanguage == Language.KOTLIN && module == moduleK)
                assertThat(companionObject.data.signature.data.type).isEqualTo("object")
            else assertThat(companionObject.data.signature.data.type).isEqualTo("class")

            javaOnly {
                assertThat(companionObject.modifiers()).containsPublicMaybeStatic()
            }

            // This is top-level in Kotlin and in a Kt class in Java
            val hoistedField = module.page("Container").content<Classlike>()
                .symbolsFor(constantsTitle()).first.single()
            assertThat(hoistedField.name()).isEqualTo("hoistedField")
            javaOnly { assertThat(hoistedField.modifiers()).contains("static") }
            kotlinOnly { assertThat(hoistedField.modifiers()).contains("const") }
            if (module == moduleJ)
                assertThat(hoistedField.urlSuffix()).isEqualTo("Container.html#hoistedField()")
            else assertThat(hoistedField.urlSuffix())
                .isEqualTo("Container.Companion.html#hoistedField()")
        }
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
            assertThat(valueOfDFunctions.size).isEqualTo(0)
            // NEW: valueOf is hidden by upstream dokka's ObviousFunction filter
            // val valueOfDFunctionReturnType = valueOfDFunctions.single().type
            // assertThat(valueOfDFunctionReturnType is JavaObject).isFalse()
            // assertThat((valueOfDFunctionReturnType as GenericTypeConstructor).dri.classNames)
            //    .isEqualTo("Test.Foo")
            // Verify the final result in dackka is correct, and that valueOf is marked inherited.
            // val enumClass = enumDModule.page("Foo").content<Classlike>()
            // val publicFuns = enumClass.methodSummaryItems()
            // val valueOfMethod = publicFuns.single { "valueOf" == it.name() }
            // val valueOfReturnType = (valueOfMethod.data.title as TypeSummary).data.type
            // assertThat(valueOfReturnType.name()).isEqualTo("Test.Foo")
        }
    }

    @Ignore // This does not generate a PagingRx class in either Kotlin or Java; TODO: fix
    @Test
    fun `JvmMultiFile does not break static attribution`() {
        val src = listOf(
            kotlinHeader(
                name = "PagingRx",
                fileAnnotations = listOf(
                    "@file:JvmName(\"PagingRx\")",
                    "@file:JvmMultifileClass",
                )
            ) + """
                    |/**
                    | * An [Observable] of [PagingData], which mirrors the stream provided by [Pager.flow], but exposes
                    | * it as an [Observable].
                    | */
                    |// Both annotations are needed here see: https://youtrack.jetbrains.com/issue/KT-45227
                    |@ExperimentalCoroutinesApi
                    |val <Key : Any, Value : Any> Pager<Key, Value>.observable: Observable<PagingData<Value>>
                    |    get() = flow
                    |        .conflate()
                    |        .asObservable()
                """,
            kotlinHeader(
                name = "RxPagingData",
                fileAnnotations = listOf(
                    "@file:JvmName(\"PagingRx\")",
                    "@file:JvmMultifileClass",
                )
            ) + """
                    |/**
                    | * Returns a [PagingData] containing only elements matching the given [predicate].
                    | */
                    |@JvmName("filter")
                    |@CheckResult
                    |fun <T : Any> PagingData<T>.filterAsync(
                    |    predicate: (T) -> Single<Boolean>
                    |): PagingData<T> = filter { predicate(it).await() }
                """
        )
        val module = testWithRootPageNode(src)
    }

    @Ignore // b/232944038; go/dokka-upstream-bug/2620
    @Test
    fun `Upstream hashcode does not use sources`() {
        val moduleK = """
            |object Foo {
            |  fun bar() = Unit
            |  const val baz = "baz"
            |}
        """.render()
        val a = moduleK.children.first().children.first() as DObject
        val a2 = a.copy()
        val b = a.copy(sources = emptyMap())
        assertThat(a.equals(a2)).isTrue()
        assertThat(a.hashCode() == a2.hashCode()).isTrue()
        // These lines fail
        assertThat(a.equals(b)).isTrue()
        assertThat(a.hashCode() == b.hashCode()).isTrue()
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
            extFunctionMap.getOrDefault(classlike.dri, emptyList())
        )
        return runBlocking { converter.classlike() }
    }

    private fun DModule.page(name: DModule.() -> DClasslike): DevsitePage {
        val classlike = name()
        val (holder, pathProvider) = holderAndProvider(this)
        val extFunctionMap = runBlocking { holder.extensionFunctionMap() }
        val converter = ClasslikeDocumentableConverter(
            displayLanguage,
            classlike,
            pathProvider,
            holder,
            extFunctionMap.getOrDefault(classlike.dri, emptyList())
        )
        return runBlocking { converter.classlike() }
    }

    private fun DModule.pages(name: String = "Foo"): List<DevsitePage> {
        // Collect pages for all classlikes with a given name
        val classlikes = explicitClasslikes(name)
        val (holder, pathProvider) = holderAndProvider(this)
        val extFunctionMap = runBlocking { holder.extensionFunctionMap() }
        val converters = classlikes.map {
            ClasslikeDocumentableConverter(
                displayLanguage,
                it,
                pathProvider,
                holder,
                extFunctionMap.getOrDefault(it.dri, emptyList())
            )
        }
        return runBlocking { converters.map { it.classlike() } }
    }

    private fun DModule.companionFor(name: String = "Foo") =
        explicitClasslike("Foo").classlikes.single { it.name == "Companion" }

    private fun String.possiblyAsGetter() = if (displayLanguage == Language.KOTLIN) this
    else "get" + this.capitalize()

    private fun Classlike.methodDetailsItems() = (
        symbolsFor(publicMethodsTitle(displayLanguage)).second.symbols +
            symbolsFor(protectedMethodsTitle(displayLanguage)).second.symbols
        )

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
        data.symbolTypes.none { it.first.title() == symbolsName } &&
            data.inheritedTypes.none { it.title() == symbolsName }

    private fun Classlike.noSymbolsFor(symbolsName: String) = noSectionFor(symbolsName) ||
        summaryItemsFor(symbolsName).isEmpty()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
