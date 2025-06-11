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
import com.google.devsite.ConstructorSummaryList
import com.google.devsite.DocsSummaryList
import com.google.devsite.TypeSummaryItem
import com.google.devsite.capitalize
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSignature
import com.google.devsite.components.table.SummaryList
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.companionName
import com.google.devsite.renderer.converters.testing.description
import com.google.devsite.renderer.converters.testing.descriptionDocs
import com.google.devsite.renderer.converters.testing.from
import com.google.devsite.renderer.converters.testing.fullName
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.modifiers
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.nonInstance
import com.google.devsite.renderer.converters.testing.projectionName
import com.google.devsite.renderer.converters.testing.receiverTypeName
import com.google.devsite.renderer.converters.testing.single
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.renderer.converters.testing.title
import com.google.devsite.renderer.converters.testing.typeName
import com.google.devsite.testing.ConverterTestBase
import kotlin.test.assertFails
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.JavaObject
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ClasslikeDocumentableConverterTest(private val displayLanguage: Language) :
    ConverterTestBase(displayLanguage) {
    @Test
    fun `Classlike creates components with correct title`() {
        val page =
            """
            |class Foo
        """
                .render()
                .page()

        assertThat(page.data.title).isEqualTo("Foo")
    }

    @Test
    fun `Classlike creates components with correct path`() {
        val page =
            """
            |class Foo
        """
                .render()
                .page()

        assertThat(page.data.pathForSwitcher!!).isEqualTo("androidx/example/Foo.html")
    }

    @Test
    fun `Classlike creates components with correct book path`() {
        val page =
            """
            |class Foo
        """
                .render()
                .page()

        assertPath(page.data.bookPath, "androidx/_book.yaml")
    }

    @Test
    fun `Empty classlike has no symbols`() {
        val page =
            """
            |interface Foo
        """
                .render()
                .page()

        val classlike = page.data.content

        assertThat(
                classlike.allSummarySections.filter { it.hasContent() } +
                    classlike.inheritedSummarySections.filter { it.hasContent() }
            )
            .isEmpty()
        assertThat(classlike.allDetailsSections.filter { it.symbols.isNotEmpty() }).isEmpty()
    }

    @Test
    fun `Public function gets documented`() {
        val page =
            """
            |class Foo {
            |    fun foo() = Unit
            |}
        """
                .render()
                .page()

        val classlike = page.data.content
        val summary = classlike.data.publicFunctionsSummary

        assertThat(summary.single().name()).isEqualTo("foo")
    }

    @Test
    fun `@jvmName functions get documented and sorted by correct name`() {
        val page =
            """
            |class Foo {
            |    @JvmName("bar")
            |    fun foo() = Unit
            |
            |    @JvmName("aar")
            |    fun zoo() = Unit
            |}
        """
                .render()
                .page()

        val classlike = page.data.content
        val summary = classlike.data.publicFunctionsSummary

        val (fun1, fun2) = summary.items(2)

        javaOnly {
            assertThat(fun1.description.name()).isEqualTo("aar")
            assertThat(fun2.description.name()).isEqualTo("bar")
        }
        kotlinOnly {
            assertThat(fun1.description.name()).isEqualTo("foo")
            assertThat(fun2.description.name()).isEqualTo("zoo")
        }
    }

    @Test
    fun `Properties are documented in sorted order`() {
        val expected = listOf("a", "b", "c")
        val documentation =
            """
            |class Foo {
            |   /** @property b b_doc */
            |   @JvmField public val b: String
            |   /** @property c c_doc */
            |   @JvmField public val c: String
            |   /** @property a a_doc */
            |   @JvmField public val a: String
            |}
        """
                .render()
                .page()

        val propertiesSummary = documentation.data.content.data.publicPropertiesSummary
        val props = propertiesSummary.items(3)

        for ((i, prop) in props.withIndex()) {
            assertThat(prop.data.description.name()).isEqualTo(expected[i])
        }
    }

    @Test
    fun `@JvmSynthetic methods are not documented in java`() {
        val page =
            """
            |class Foo {
            |    fun foo() = Unit
            |
            |    @JvmSynthetic
            |    fun zoo() = Unit
            |}
        """
                .render()
                .page()

        val classlike = page.data.content
        val summary = classlike.data.publicFunctionsSummary

        javaOnly { assertThat(summary.size).isEqualTo(1) }
        kotlinOnly { assertThat(summary.size).isEqualTo(2) }
    }

    @Test
    fun `Protected function gets documented`() {
        val page =
            """
            |abstract class Foo {
            |    protected open fun foo() = Unit
            |}
        """
                .render()
                .page()

        val classlike = page.data.content
        val summary = classlike.data.protectedFunctionsSummary

        assertThat(summary.item().description.name()).isEqualTo("foo")
    }

    @Test
    fun `Public property gets documented`() {
        val page =
            """
            |class Foo {
            |    @JvmField
            |    val foo = Unit
            |}
        """
                .render()
                .page()

        val classlike = page.data.content
        val summary = classlike.data.publicPropertiesSummary

        assertThat(summary.single().description.name()).isEqualTo("foo")
    }

    @Test
    fun `Protected property gets documented`() {
        val page =
            """
            |abstract class Foo {
            |    @JvmField
            |    protected open val foo = Unit
            |}
        """
                .render()
                .page()

        val classlike = page.data.content
        val summary = classlike.data.protectedPropertiesSummary

        assertThat(summary.item().description.name()).isEqualTo("foo")
    }

    @Test
    fun `Public constructor gets documented`() {
        val page =
            """
            |class Foo
        """
                .render()
                .page()

        val classlike = page.data.content
        val summary = classlike.data.publicConstructorsSummary

        assertThat(summary.constructor().name()).isEqualTo("Foo")
    }

    @Test
    fun `Function summary component hides DeprecationLevel HIDDEN`() {
        val module =
            """
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
        """
                .render()

        assertThat(module.packages.single().classlikes.map { it.name }).containsExactly("Visible")
        val visible = module.page("Visible").data.content

        javaOnly {
            assertThat(visible.data.publicFunctionsSummary.map { it.name() })
                .containsExactly("getVisible", "show")
            assertThat(visible.data.publicPropertiesSummary).isEmpty()
        }
        kotlinOnly {
            assertThat(visible.data.publicFunctionsSummary.map { it.name() })
                .containsExactly("show")
            assertThat(visible.data.publicPropertiesSummary.map { it.name() })
                .containsExactly("visible")
        }
    }

    @Test
    fun `Public constructor does not have @NonNull in 4x Kotlin and Java`() {
        val classlikeK =
            """
        |class Foo {
        |   constructor() {}
        |}
        """
                .render()
                .page()
                .data
                .content
        val classlikeJ =
            """
        |public class Foo {
        |   public Foo() {}
        |}
        """
                .render(java = true)
                .page()
                .data
                .content

        for (classlike in listOf(classlikeJ, classlikeK)) {
            // Ctor summaries are TableRowSummaryItem<Nothing?, SymbolSummary<FunctionSignature>>
            // they don't have return types in the model at all, so those can't be annotated
            // though the constructor itself still can be, which is fine
            val summary = classlike.data.publicConstructorsSummary.single()
            val summaryAnnotations = summary.data.description.data.annotationComponents
            assertThat(summaryAnnotations).isEmpty()

            val detail = classlike.data.publicConstructorsDetails.symbols.single()
            val returnAnnotations = detail.data.returnType.annotations
            val annotations = detail.data.annotationComponents
            assertThat(returnAnnotations).isEmpty()
            assertThat(annotations).isEmpty()
            assertThat(detail.data.signature.data.receiver).isNull()
        }
    }

    @Test
    fun `Protected constructor gets documented`() {
        val page =
            """
            |open class Foo protected constructor()
        """
                .render()
                .page()

        val classlike = page.data.content
        val summary = classlike.data.protectedConstructorsSummary

        assertThat(summary.constructor().name()).isEqualTo("Foo")
    }

    @Test
    fun `Nested type gets documented`() {
        val page =
            """
            |class Foo {
            |    class Bar
            |}
        """
                .render()
                .page()

        val classlike = page.data.content
        assertThat(classlike.companionName()).isEqualTo("Foo.Bar")
    }

    @Test
    fun `Empty unnamed companions are not documented, empty named companions are`() {
        val module =
            """
            |class Foo {
            |    companion object FooCompanion
            |}
            |class Bar {
            |    companion object
            |}
        """
                .render()
        val foo = module.page("Foo").data.content
        val bar = module.page("Bar").data.content

        assertThat(foo.companionName()).isEqualTo("Foo.FooCompanion")
        assertThat(bar.data.nestedTypesSummary).isEmpty()
    }

    @Test
    fun `Companions are interesting for Java when they have un-hoisted functions or properties`() {
        val module =
            """
            |class UnhoistedProperty {
            |    companion object {
            |        // Not hoisted in Java
            |        val ordinaryVal = 0
            |        // Hoisted in Java
            |        @JvmStatic fun jvmStaticFun() = Unit
            |    }
            |}
            |class UnhoistedFunction {
            |    companion object {
            |        // Not hoisted in Java
            |        fun ordinaryFun() = Unit
            |        // Hoisted in Java
            |        @JvmStatic fun jvmStaticFun() = Unit
            |    }
            |}
            |class LateinitUnhoistedProperty {
            |    companion object {
            |        // lateinit vars are hoisted, but their accessors are not
            |        lateinit var lateinitVar: String
            |    }
            |}
            |class AllHoisted {
            |    companion object {
            |        // Hoisted in Java
            |        @JvmField val jvmFieldVal = 0
            |        @JvmStatic fun jvmStaticFun() = Unit
            |    }
            |}
        """
                .render()

        val unhoistedProperty = module.page("UnhoistedProperty").data.content
        val unhoistedFunction = module.page("UnhoistedFunction").data.content
        val lateinitUnhoistedProperty = module.page("LateinitUnhoistedProperty").data.content
        val allHoisted = module.page("AllHoisted").data.content

        javaOnly {
            assertThat(unhoistedProperty.data.nestedTypesSummary).hasSize(1)
            assertThat(unhoistedFunction.data.nestedTypesSummary).hasSize(1)
            assertThat(lateinitUnhoistedProperty.data.nestedTypesSummary).hasSize(1)
        }
        kotlinOnly {
            assertThat(unhoistedProperty.data.nestedTypesSummary).isEmpty()
            assertThat(unhoistedFunction.data.nestedTypesSummary).isEmpty()
            assertThat(lateinitUnhoistedProperty.data.nestedTypesSummary).isEmpty()
        }

        assertThat(allHoisted.data.nestedTypesSummary).isEmpty()
    }

    @Test
    fun `Direct subclasses are found`() {
        val page =
            """
            |abstract class Foo
            |open class C : B
            |open class B : Foo
            |open class A : Foo
        """
                .render()
                .page()

        val classlike = page.data.content
        val subclasses =
            classlike.data.description.data.relatedSymbols.data.directSubclasses.items(2)

        assertThat(subclasses.first().data.name).isEqualTo("A")
        assertThat(subclasses.last().data.name).isEqualTo("B")
    }

    @Test
    fun `Indirect subclasses are found`() {
        val page =
            """
            |abstract class Foo
            |open class C : Foo
            |open class B : C
            |open class A : B
        """
                .render()
                .page()

        val classlike = page.data.content
        val subclasses =
            classlike.data.description.data.relatedSymbols.data.indirectSubclasses.items(2)

        assertThat(subclasses.first().data.name).isEqualTo("A")
        assertThat(subclasses.last().data.name).isEqualTo("B")
    }

    @Test
    fun `Class with root object as parent does not have hierarchy`() {
        val page =
            """
            |class Foo
        """
                .render()
                .page()

        val classlike = page.data.content
        val parents = classlike.data.description.data.hierarchy.data.parents

        assertThat(parents).isEmpty()
    }

    @Test
    fun `Class with single parent has hierarchy with root object, parent, and itself`() {
        val page =
            """
            |abstract class Parent
            |class Foo : Parent
        """
                .render()
                .page()

        val classlike = page.data.content
        val parents = classlike.data.description.data.hierarchy.data.parents.items(3).toList()

        javaOnly { assertThat(parents[0].data.name).isEqualTo("java.lang.Object") }
        kotlinOnly { assertThat(parents[0].data.name).isEqualTo("kotlin.Any") }
        assertThat(parents[1].data.name).isEqualTo("androidx.example.Parent")
        assertThat(parents[2].data.name).isEqualTo("androidx.example.Foo")
    }

    @Test
    fun `Class with multiple parents has hierarchy with root object and parents`() {
        val page =
            """
            |abstract class A
            |abstract class B : A
            |abstract class C : B
            |class Foo : C
        """
                .render()
                .page()

        val classlike = page.data.content
        val parents = classlike.data.description.data.hierarchy.data.parents.items(5).toList()

        javaOnly { assertThat(parents[0].data.name).isEqualTo("java.lang.Object") }
        kotlinOnly { assertThat(parents[0].data.name).isEqualTo("kotlin.Any") }
        assertThat(parents[1].data.name).isEqualTo("androidx.example.A")
        assertThat(parents[2].data.name).isEqualTo("androidx.example.B")
        assertThat(parents[3].data.name).isEqualTo("androidx.example.C")
        assertThat(parents[4].data.name).isEqualTo("androidx.example.Foo")
    }

    @Test
    fun `Class signature appears with extends and implements for internal types in 4x`() {
        val pageK =
            """
            |interface A
            |abstract class B
            |class Foo : A, B
        """
                .render()
                .page("Foo")
        val pageJ =
            """
            |public interface A {}
            |public abstract class B {}
            |public class Foo extends Test.B implements Test.A {}
        """
                .render(java = true)
                .page("Foo")

        for (page in listOf(pageJ, pageK)) {
            val prefix = if (page == pageK) "" else "Test."
            val classSignature = page.data.content.data.description.data.primarySignature.data
            assertThat(classSignature.type).isEqualTo("class")
            assertThat(classSignature.extends.single().data.name).isEqualTo("${prefix}B")
            assertThat(classSignature.implements.single().data.name).isEqualTo("${prefix}A")
        }
    }

    @Ignore
    @Test
    fun `Class signature and hierarchy can contain generics in 4x`() {
        val pageK =
            """
            |class Foo : List<String>
        """
                .render()
                .page("Foo")
                .data
                .content
        val pageJ =
            """
            |public class Foo extends List<String> {}
        """
                .render(java = true)
                .page("Foo")
                .data
                .content

        for (page in listOf(pageJ, pageK)) {
            val classSignature = page.data.description.data.primarySignature.data
            assertThat(classSignature.type).isEqualTo("class")
            assertThat(classSignature.extends.single().data.name).isEqualTo("List<String>")
            val hierarchy = page.data.description.data.hierarchy.data
            assertThat(hierarchy.parents.size).isEqualTo(2)
            assertThat(hierarchy.parents.first().data.name).isEqualTo("List<String>")
        }
    }

    @Test
    fun `Class signature appears with implements for external types in 4x`() {
        val pageExternalK =
            """
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
        """
                .render()
                .page(name = "NavArgsLazy")
        val pageExternalJ =
            """
            |public class JavaArgsLazy implements Lazy {}
        """
                .renderJava(imports = listOf("kotlin.Lazy"))
                .page(name = "JavaArgsLazy")
        val signatureK = pageExternalK.data.content.data.description.data.primarySignature
        val signatureJ = pageExternalJ.data.content.data.description.data.primarySignature
        assertThat(signatureK.data.implements.map { it.data.name }).isEqualTo(listOf("Lazy"))
        assertThat(signatureJ.data.implements.map { it.data.name }).isEqualTo(listOf("Lazy"))
    }

    // This test also validates that only direct superclasses / interfaces are included because
    // AbstractList extends AbstractCollection which implements various interfaces (Iterable etc).
    @Test
    fun `Class signature appears with extends for external types in 4x`() {
        val pageExternalK =
            """
            |import java.util.AbstractList
            |public class MyList() : AbstractList<Int>()
        """
                .render()
                .page(name = "MyList")
        val signatureK = pageExternalK.data.content.data.description.data.primarySignature
        val pageExternalJ =
            """
            |public class MyList extends AbstractList<String> {}
        """
                .renderJava(imports = listOf("java.util.*"))
                .page(name = "MyList")
        val signatureJ = pageExternalJ.data.content.data.description.data.primarySignature
        for (signature in listOf(signatureJ, signatureK)) {
            assertThat(signature.data.extends.map { it.data.name })
                .isEqualTo(listOf("AbstractList"))
            assertThat(signature.data.implements).isEmpty()
        }
    }

    @Test
    fun `Primary constructor can be @suppress-ed without hiding the class itself`() {
        // Primary constructor suppression is broken upstream
        // https://github.com/Kotlin/dokka/issues/1953
        val modulePrimary =
            """
            |public class BenchmarkState
            |    /** @suppress */
            |    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            |    constructor(val foo: String) {
            |}
        """
                .render()
        assertFails {
            val page = modulePrimary.page("BenchmarkState")
        }
        // We can convert to equivalent secondary constructor and it works
        val moduleSecondary =
            """
            |public class BenchmarkState {
            |    val foo: String
            |    /** @suppress */
            |    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            |    constructor(foo: String) { this.foo = foo }
            |}
        """
                .render()
        val page = moduleSecondary.page("BenchmarkState").data.content
        assertThat(page.data.publicConstructorsSummary.size).isEqualTo(0)
    }

    @Test
    fun `Enum class is rendered and has enum values with types in 4x Kotlin and Java`() {
        val pageK =
            """
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
        """
                .render()
                .page(name = "AnEnumType")
        val pageJ =
            """
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
        """
                .render(java = true)
                .page(name = "AnEnumType")

        for (page in listOf(pageK, pageJ)) {
            val classlike = page.data.content
            val signature = classlike.data.description.data.primarySignature.data
            val description = (classlike.descriptionDocs.first() as DescriptionComponent)

            val enumSummary = classlike.data.enumValuesSummary
            val enumDetails = classlike.data.enumValuesDetails
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
    fun `Class component inherits docs from same language 4x test`() {
        val pagesJ =
            """
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
        """
                .render(java = true) // Java {@inheritdoc} does not support classes & properties
        val pagesK =
            """
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
            |}        """
                .render()

        for (pages in listOf(pagesJ, pagesK)) {
            val fooClass = pages.page("foo").data.content
            val barClass = pages.page("bar").data.content
            val bazClass = pages.page("baz").data.content

            // Function description inheritance does not work properly
            val fooDoit = fooClass.data.publicFunctionsSummary.single { it.name() == "doit" }
            val fooDoitDocs = fooDoit.description.data.description.text()
            val barDoit = barClass.data.publicFunctionsSummary.single { it.name() == "doit" }
            val barDoitDocs = barDoit.description.data.description.text()
            val bazDoit = bazClass.data.publicFunctionsSummary.single { it.name() == "doit" }
            val bazDoitDocs = bazDoit.description.data.description.text()

            assertThat(fooDoitDocs).isEqualTo("dew it")
            assertThat(barDoitDocs).isEqualTo("dew it")
            assertThat(bazDoitDocs).isEqualTo("overriding function docs")
        }

        // Tests for the Kotlin-source-only properties and class docs
        val fooClassK = pagesK.page("foo").data.content
        val barClassK = pagesK.page("bar").data.content
        val bazClassK = pagesK.page("baz").data.content

        // overriding class docs is maybe something we want in kotlin, but is not jdoc spec
        val fooDescription = (fooClassK.descriptionDocs.first() as DescriptionComponent)
        val barDescription = (barClassK.descriptionDocs.first() as DescriptionComponent)
        val bazDescription = (bazClassK.descriptionDocs.first() as DescriptionComponent)
        assertThat(fooDescription.text()).isEqualTo("docs for foo")
        // assertThat(barDescription.text()).isEqualTo("docs for foo")
        assertThat(bazDescription.text()).isEqualTo("overriding docs for baz")

        kotlinOnly {
            // overriding properties is kotlin-only, and working
            val fooDemocracy = fooClassK.data.publicPropertiesSummary.single().description
            assertThat(fooDemocracy.data.description.text()).isEqualTo("thunderous applause")
            val barDemocracy = barClassK.data.publicPropertiesSummary.single().description
            assertThat(barDemocracy.data.description.text()).isEqualTo("thunderous applause")
            val bazDemocracy = bazClassK.data.publicPropertiesSummary.single().description
            assertThat(bazDemocracy.data.description.text()).isEqualTo("KotOR")
        }
        javaOnly {
            // overriding properties is kotlin-only, and working
            val fooDemocracy =
                fooClassK.data.publicFunctionsSummary
                    .single { it.name() == "getDemocracy" }
                    .description
            assertThat(fooDemocracy.data.description.text()).isEqualTo("thunderous applause")
            val barDemocracy =
                barClassK.data.publicFunctionsSummary
                    .single { it.name() == "getDemocracy" }
                    .description
            assertThat(barDemocracy.data.description.text()).isEqualTo("thunderous applause")
            val bazDemocracy =
                bazClassK.data.publicFunctionsSummary
                    .single { it.name() == "getDemocracy" }
                    .description
            assertThat(bazDemocracy.data.description.text()).isEqualTo("KotOR")
        }

        // Using {@inheritDoc} in kotlin is wrong
        val mazClass = pagesK.page("maz").data.content
        val mazDoit = mazClass.data.publicFunctionsSummary.single()
        val mazDoitDocs = mazDoit.data.description.data.description.text()
        assertThat(mazDoitDocs).isNotEqualTo("dew it")
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Property parameter documentation from interfaces inherits properly`() {
        val pages =
            """
            |/**
            | * @param param1 param1_docs
            | * @property property1 property1_docs
            | */
            |interface Interfaz(val param1: String, val property1: Int) {}
            |class Foo(param1: String, property1: Int): Interfaz(param1, property1)
            |/**
            | * @param param1 override_param1_docs
            | * @property property1 override_property1_docs
            | */
            |class Baz(override val param1: String, override val property1: Int): Interfaz(param1, property1)
        """
                .render()

        val foo = pages.page("Foo").data.content.data
        val baz = pages.page("Baz").data.content.data

        val fooFromInterfaz =
            if (displayLanguage == Language.KOTLIN) {
                foo.inheritedProperties.from("androidx.example.Interfaz")!!.value
            } else {
                foo.inheritedFunctions.from("androidx.example.Interfaz")!!.value
            }

        val fooParam1 =
            fooFromInterfaz
                .single { it.data.description.name() == "param1".possiblyAsGetter() }
                .data
                .description
        assertThat(fooParam1.text()).isEqualTo("param1_docs")
        val fooProp1 =
            fooFromInterfaz
                .single { it.data.description.name() == "property1".possiblyAsGetter() }
                .data
                .description
        assertThat(fooProp1.text()).isEqualTo("property1_docs")

        assertThat(foo.publicPropertiesSummary.hasContent()).isFalse()
        assertThat(foo.publicFunctionsSummary.hasContent()).isFalse()

        val bazDefinitions =
            if (displayLanguage == Language.KOTLIN) {
                baz.publicPropertiesSummary
            } else {
                baz.publicFunctionsSummary
            }

        val bazParam1 =
            bazDefinitions
                .single { it.data.description.name() == "param1".possiblyAsGetter() }
                .data
                .description
        assertThat(bazParam1.text()).isEqualTo("override_param1_docs")
        val bazProp1 =
            bazDefinitions
                .single { it.data.description.name() == "property1".possiblyAsGetter() }
                .data
                .description
        assertThat(bazProp1.text()).isEqualTo("override_property1_docs")

        val bazConstructorParams =
            baz.publicConstructorsDetails.symbols.single().data.metadata[1] as DocsSummaryList
        val bazConstructorParam1 =
            bazConstructorParams.items().single { it.name() == "param1" }.data.description
        assertThat(bazConstructorParam1.text()).isEqualTo("override_param1_docs")
    }

    @Suppress("UNCHECKED_CAST") // TODO: add tests once @constructor doc inheritance is implemented
    @Test // TODO: patch upstream dokka to implement kotlin documentation inheritance b/184361891
    fun `Property parameter documentation inherits properly`() {
        val pages =
            """
            |/**
            | * @param param1 param1_docs
            | * @property property1 property1_docs
            | */
            |open class Supclaz(open val param1: String, open val property1: Int) {}
            |/**
            | * @param param2 param2_docs
            | * @property property2 property2_docs
            | */
            |sealed class Sealclaz(internal open val param2: String, protected open val property2: Int) {}
            |class Foo(param1: String, property1: Int): Supclaz(param1, property1)
            |/**
            | * @param param1 override_param1_docs
            | * @property property1 override_property1_docs
            | */
            |class Baz(override val param1: String, override val property1: Int): Supclaz(param1, property1)
            |class Bar(override val param2: String, override val property2: Int): Sealclaz(param2, property2)
        """
                .render()
        val foo = pages.page("Foo").data.content
        val baz = pages.page("Baz").data.content
        val bar = pages.page("Bar").data.content

        val fooFromSupclaz =
            if (displayLanguage == Language.KOTLIN) {
                foo.data.inheritedProperties.from("androidx.example.Supclaz")!!.value
            } else {
                foo.data.inheritedFunctions.from("androidx.example.Supclaz")!!.value
            }

        val fooParam1 =
            fooFromSupclaz
                .single { it.data.description.name() == "param1".possiblyAsGetter() }
                .data
                .description
        assertThat(fooParam1.text()).isEqualTo("param1_docs")
        val fooProp1 =
            fooFromSupclaz
                .single { it.data.description.name() == "property1".possiblyAsGetter() }
                .data
                .description
        assertThat(fooProp1.text()).isEqualTo("property1_docs")

        assertThat(foo.data.publicPropertiesSummary.hasContent()).isFalse()
        assertThat(foo.data.publicFunctionsSummary.hasContent()).isFalse()

        val bazDefinitions =
            if (displayLanguage == Language.KOTLIN) {
                baz.data.publicPropertiesSummary
            } else {
                baz.data.publicFunctionsSummary
            }

        val bazParam1 =
            bazDefinitions
                .single { it.data.description.name() == "param1".possiblyAsGetter() }
                .data
                .description
        assertThat(bazParam1.text()).isEqualTo("override_param1_docs")
        val bazProp1 =
            bazDefinitions
                .single { it.data.description.name() == "property1".possiblyAsGetter() }
                .data
                .description
        assertThat(bazProp1.text()).isEqualTo("override_property1_docs")

        val barProtectedDefinitions =
            if (displayLanguage == Language.KOTLIN) {
                bar.data.protectedPropertiesSummary
            } else {
                bar.data.protectedFunctionsSummary
            }

        assertThat(barProtectedDefinitions).hasSize(1)
        val barProp2 =
            barProtectedDefinitions
                .single { it.data.description.name() == "property2".possiblyAsGetter() }
                .data
                .description
        assertThat(barProp2.text()).isEqualTo("property2_docs")

        /* Constructors don't magically inherit and merge @params from parents' constructors
        val constructorDetails = fooClass.symbolsForConstructors().second.symbols.single()
        val ctrDocsParamTable = constructorDetails.data.metadata[1] as DocsSummaryList
        val param1docs = ctrDocsParamTable.items().single { it.name() == "param1" }.data.description
        assertThat(param1docs.text()).isEqualTo("param1_docs")
         */

        val bazConstructorParams =
            baz.data.publicConstructorsDetails.symbols.single().data.metadata[1] as DocsSummaryList
        val bazConstructorParam1 =
            bazConstructorParams.items().single { it.name() == "param1" }.data.description
        assertThat(bazConstructorParam1.text()).isEqualTo("override_param1_docs")

        // TODO: patch upstream dokka to support inheriting documentation on hidden components
        // val pparam2docs = barClass.propertySymbol("param2").data.description
        // assertThat(pparam2docs.text()).isEqualTo("param2_docs")

        val expected =
            when (displayLanguage) {
                Language.KOTLIN ->
                    listOf(bar.data.publicConstructorsSummary, bar.data.protectedPropertiesSummary)
                Language.JAVA ->
                    listOf(bar.data.publicConstructorsSummary, bar.data.protectedFunctionsSummary)
            }
        val observed =
            bar.allSummarySections.filter { it.hasContent() } +
                bar.inheritedSummarySections.filter { it.hasContent() }
        assertThat(observed).containsExactlyElementsIn(expected).inOrder()

        /* Constructors don't inherit docs
        val bConstructorDetails = fooClass.symbolsForConstructors().second.symbols.single()
        val bctrDocsParamTabl = bConstructorDetails.data.metadata[1] as DocsSummaryList
        val bparam2doc = bctrDocsParamTabl.items().single { it.name() == "param2" }.data.description
        assertThat(bparam2doc.text()).isEqualTo("param2_docs")
         */
    }

    @Test
    fun `Level-jumping doc inheritance works in 4x Kotlin and Java`() {
        val pageK =
            """
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
        """
                .render()
                .page("baz")
        val pageJ =
            """
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
        """
                .render(java = true)
                .page("baz")
        for (page in listOf(pageJ, pageK)) {
            val doit = page.data.content.data.publicFunctionsSummary.single()
            assertThat(doit.description.data.description.text()).isEqualTo("dew it")
        }
    }

    @Test
    fun `Inherited methods are sorted by name and arity`() {
        val childClass =
            """
            |class Parent() {
            |    fun b(input: Int, zinput2: Int) {}
            |    fun c() {}
            |    fun a() {}
            |    fun b(input: Int, input2: Int) {}
            |    fun b() {}
            |    fun b(input: Int) {}
            |}
            |class Child() : Parent {}
        """
                .render()
                .page("Child")
                .data
                .content
        val inheritedSummary = childClass.data.inheritedFunctions.data.inheritedSymbolSummaries
        val inheritedMethods = inheritedSummary.entries.single().value.data.items
        val inheritedMethodSignatures = inheritedMethods.map { it.data.description.data.signature }
        assertThat(inheritedMethodSignatures.map { it.data.name.data.name })
            .isEqualTo(listOf("a", "b", "b", "b", "b", "c"))
        assertThat(inheritedMethodSignatures.map { it.data.parameters.size })
            .isEqualTo(listOf(0, 0, 1, 2, 2, 0))
        val paramNames = inheritedMethodSignatures.map { it.data.parameters.map { it.data.name } }
        assertThat(paramNames)
            .isEqualTo(
                listOf(
                    listOf(),
                    listOf(),
                    listOf("input"),
                    listOf("input", "input2"),
                    listOf("input", "zinput2"),
                    listOf(),
                )
            )
    }

    @Test
    fun `Inherited properties are not lost 4x test`() {
        val moduleJ =
            """
            |public class Parent {
            |   public int b = 8;
            |   private int a = 9;
            |   public int getA() { return a; }
            |   public void setA(int newA) { a = newA; }
            |}
            |public class Child extends Parent
        """
                .render(java = true)
        val moduleK =
            """
            |class Test {
            |   open class Parent {
            |       var a: Int = 9
            |       @JvmField
            |       var b: Int = 8
            |   }
            |   class Child: Parent()
            |}
        """
                .render()

        for (module in listOf(moduleJ, moduleK)) {
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
            val childPage = module.page("Child").data.content

            // a shows up as an inherited property in kotlin, inherited accessor functions in java
            val inheritedProps = childPage.data.inheritedProperties.data.inheritedSymbolSummaries
            val inheritedFuns = childPage.data.inheritedFunctions.data.inheritedSymbolSummaries
            val inhPropNames = inheritedProps.values.single().items().map { it.name() }
            javaOnly {
                // TODO (b/241259955): Java source private fields appear unless they start with "m"
                if (module == moduleJ) {
                    assertThat(inhPropNames).containsExactly("a", "b")
                } else {
                    assertThat(inhPropNames).containsExactly("b")
                }
                val inhFunNames = inheritedFuns.values.single().items().map { it.name() }
                assertThat(inhFunNames).containsExactly("getA", "setA").inOrder()
            }
            kotlinOnly {
                assertThat(inhPropNames).containsExactly("a", "b").inOrder()
                assertThat(inheritedFuns).isEmpty()
            }
        }
    }

    @Test
    fun `Externally-inherited vars from Kotlin source`() {
        val module =
            """
            |class Test {
            |   class Child: kotlin.RuntimeException()
            |}
        """
                .render()

        // kotlin.Throwable is an `actual typealias`.
        val throwableDRI = "kotlin.Throwable"
        // add/get is not consolidated into a property in Kotlin
        val sixFuns = listOf("addSuppressed", "fillInStackTrace", "printStackTrace", "initCause")
        // "Message" becomes a "val" in Kotlin, which hides its getter. ToString is from Object.
        val printOverloads = listOf("printStackTrace", "printStackTrace")
        val expectedProps =
            listOf("cause", "message", "localizedMessage", "suppressed", "stackTrace")
        val expectedPropAccessors =
            listOf(
                "getCause",
                "getMessage",
                "getLocalizedMessage",
                "getStackTrace",
                "getSuppressed",
                "setStackTrace",
            )
        val expectedFuns = sixFuns + printOverloads

        // This is a test of the upstream dokka Documentables tree
        val dChild = module.explicitClasslike("Child")
        assertThat(dChild.properties.names()).containsExactlyElementsIn(expectedProps)
        assertThat(dChild.functions.names()).containsExactlyElementsIn(expectedFuns)

        val dMessageProp = dChild.properties.single { it.name == "message" }
        val dPrintStackTrace = dChild.functions.single { it.name == "fillInStackTrace" }
        assertThat(dMessageProp.dri.fullName).contains(throwableDRI)
        assertThat(dMessageProp.getter!!.dri.fullName).contains(throwableDRI)
        assertThat(dPrintStackTrace.dri.fullName).contains(throwableDRI)

        // This is a test of the dackka Components tree
        val childPage = module.page("Child").data.content

        val inheritedFuns = childPage.data.inheritedFunctions.from(throwableDRI)!!.value
        val funNames = inheritedFuns.items().map { it.name() }

        javaOnly {
            assertThat(funNames).containsExactlyElementsIn(expectedFuns + expectedPropAccessors)

            assertThat(childPage.data.inheritedProperties.items).isEmpty()
        }
        kotlinOnly {
            assertThat(funNames).containsExactlyElementsIn(expectedFuns)

            val inheritedProps = childPage.data.inheritedProperties.from(throwableDRI)!!.value
            val propNames = inheritedProps.items().map { it.name() }
            assertThat(propNames).containsExactlyElementsIn(expectedProps)
        }
    }

    @Test
    fun `Externally-inherited vars from Java source`() {
        val module =
            """
            |public class Child extends java.lang.RuntimeException {}
        """
                .render(java = true)

        // kotlin.Throwable is an `actual typealias`.
        // java Throwable: https://docs.oracle.com/javase/7/docs/api/java/lang/Throwable.html
        val throwableDRI = "java.lang.Throwable"
        // add/get is not consolidated into a property in Kotlin
        val sixFuns =
            listOf(
                "addSuppressed",
                "getSuppressed",
                "fillInStackTrace",
                "printStackTrace",
                "getLocalizedMessage",
                "initCause",
            )
        // "Message" becomes a "val" in Kotlin, which hides its getter. ToString is from Object.
        val missingInKotlin = listOf("getMessage", "toString")
        // TODO: figure out why stackTrace is accessors in Kotlin (but a field in Java) upstream
        // Maybe inherited accessors don't get merged into a property? Cause/stackTrace are
        // private fields upstream, as is `*final* String detailMessage`....
        val stackTraceAccessors = listOf("getStackTrace", "setStackTrace")
        val printOverloads = listOf("printStackTrace", "printStackTrace")
        val expectedProps = listOf("cause", "stackTrace")
        val expectedFuns = sixFuns + missingInKotlin
        // dackka extracts getters and setters from properties to display separately as-Java
        val bonusInDackka = listOf("getCause") + stackTraceAccessors

        // This is a test of the upstream dokka Documentables tree
        val dChild = module.explicitClasslike("Child")
        assertThat(dChild.properties.size).isEqualTo(2)
        assertThat(dChild.functions.size).isEqualTo(8)
        assertThat(dChild.properties.names()).containsExactlyElementsIn(expectedProps)
        assertThat(dChild.functions.names()).containsExactlyElementsIn(expectedFuns)

        val dCauseProp = dChild.properties.single { it.name == "cause" }
        val dStackTrace = dChild.properties.single { it.name == "stackTrace" }
        assertThat(dCauseProp.dri.fullName).contains(throwableDRI)
        assertThat(dCauseProp.getter!!.dri.fullName).contains(throwableDRI)
        assertThat(dStackTrace.dri.fullName).contains(throwableDRI)
        assertThat(dStackTrace.getter!!.dri.fullName).contains(throwableDRI)

        // This is a test of the dackka Components tree
        val childPage = module.page("Child").data.content

        val inheritedFuns = childPage.data.inheritedFunctions.from(throwableDRI)!!.value
        val funNames = inheritedFuns.items().map { it.name() }

        javaOnly {
            assertThat(funNames.size).isEqualTo(11)
            assertThat(funNames).containsExactlyElementsIn(expectedFuns + bonusInDackka)
        }
        kotlinOnly {
            assertThat(funNames.size).isEqualTo(8)
            assertThat(funNames).containsExactlyElementsIn(expectedFuns)
        }

        // TODO (b/241259955): these properties should be private and not appear in Java docs
        val inheritedProps = childPage.data.inheritedProperties.from(throwableDRI)!!.value
        val propNames = inheritedProps.items().map { it.name() }
        assertThat(propNames.size).isEqualTo(2)
        assertThat(propNames).containsExactlyElementsIn(expectedProps)
    }

    @Test
    fun `Different categories of symbols inherited from different classes works`() {
        val page =
            """
            |open class GrandParent {
            |    @JvmField val grandD: Int = 1
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
        """
                .render()
                .page("Child")
                .data
                .content

        val categoriesNames =
            listOf(page.data.inheritedFunctions.title(), page.data.inheritedProperties.title())
        kotlinOnly {
            assertThat(categoriesNames)
                .containsExactly("Inherited functions", "Inherited properties")
                .inOrder()
        }
        javaOnly {
            assertThat(categoriesNames)
                .containsExactly("Inherited methods", "Inherited fields")
                .inOrder()
        }

        val functions =
            page.data.inheritedFunctions.data.inheritedSymbolSummaries
                .mapKeys { it.key.data.name }
                .mapValues { (_, list) -> list.items().map { it.name() } }

        // Java function include generated getters and setters
        kotlinOnly {
            assertThat(functions)
                .containsExactly(
                    "androidx.example.GrandParent",
                    listOf("grandA", "grandB"),
                    "androidx.example.Parent",
                    listOf("parentC"),
                )
        }
        javaOnly {
            assertThat(functions)
                .containsExactly(
                    "androidx.example.GrandParent",
                    listOf("getGrandC", "grandA", "grandB"),
                    "androidx.example.Parent",
                    listOf("getParentA", "getParentB", "parentC"),
                )
        }

        val properties =
            page.data.inheritedProperties.data.inheritedSymbolSummaries
                .mapKeys { it.key.data.name }
                .mapValues { (_, list) -> list.items().map { it.name() } }

        // In Java, the properties are surfaced through getter functions except for @JvmFields
        kotlinOnly {
            assertThat(properties)
                .containsExactly(
                    "androidx.example.GrandParent",
                    listOf("grandC", "grandD"),
                    "androidx.example.Parent",
                    listOf("parentA", "parentB"),
                )
        }
        javaOnly {
            assertThat(properties).containsExactly("androidx.example.GrandParent", listOf("grandD"))
        }
    }

    @Test
    fun `Failed resolution exception includes line number`() {
        val message =
            assertFails {
                    """
            |/** @param foo does not exist */
            |class Foo<T: Number, U>() {}
            """
                        .render()
                        .page()
                }
                .message
        assertThat(message).contains("when handling DFunction Foo in DClass Foo")
        // assertThat(message).contains("androidx/example/Test.kt:2") b/327166311
    }

    @Test
    fun `Class component creates inline generics`() {
        val page =
            """
            |class Foo<T: Number, U>() {}
        """
                .render()
                .page()
        val typeParams =
            page.data.content.data.description.data.primarySignature.data.typeParameters
        assertThat(typeParams.first().data.name).isEqualTo("T")
        assertThat(typeParams.first().projectionName()).isEqualTo("Number")
        assertThat(typeParams.last().data.name).isEqualTo("U")
        kotlinOnly { assertThat(typeParams.last().projectionName()).isEqualTo("Any") }
        javaOnly { assertThat(typeParams.last().projectionName()).isEqualTo("Object") }
    }

    @Test
    fun `Class component creates all symbols in the correct order`() {
        val page = "class Foo {}".render().page()
        val symbolTypes = page.data.content.allDetailsSections.map { it.title }

        javaOnly {
            assertThat(symbolTypes)
                .containsExactlyElementsIn(
                    listOf(
                        // "Nested types", // there is no details section for nested types
                        "Enum Values",
                        "Constants",
                        "Public fields",
                        "Protected fields",
                        "Public constructors",
                        "Protected constructors",
                        "Public methods",
                        "Protected methods",
                        "Extension functions",
                    )
                )
                .inOrder()
        }

        kotlinOnly {
            assertThat(symbolTypes)
                .containsExactlyElementsIn(
                    listOf(
                        // "Nested types", // there is no details section for nested types
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
                        "Protected properties",
                        "Extension functions",
                        "Extension properties",
                    )
                )
                .inOrder()
        }
    }

    @Test
    fun `classlike companion functions are included in Kotlin and nested static in Java`() {
        val module =
            """
            |class Foo {
            |  companion object {
            |    fun bar() = Unit
            |    protected fun baz() = Unit
            |  }
            |}
        """
                .render()

        val classlike = module.page("Foo").data.content

        kotlinOnly {
            assertThat(classlike.data.publicCompanionFunctionsSummary.item().name())
                .isEqualTo("bar")
            assertThat(classlike.data.protectedCompanionFunctionsSummary.item().name())
                .isEqualTo("baz")
        }
        javaOnly {
            assertThat(classlike.data.publicCompanionFunctionsSummary).isEmpty()
            assertThat(classlike.data.publicCompanionPropertiesSummary).isEmpty()
            assertThat(classlike.companionName()).isEqualTo("Foo.Companion")
            val companionClasslike = module.page { this.companionFor("Foo") }.data.content
            val barMethod = companionClasslike.data.publicFunctionsSummary.single()
            assertThat(barMethod.name()).isEqualTo("bar")
            val bazMethod = companionClasslike.data.protectedFunctionsSummary.single()
            assertThat(bazMethod.name()).isEqualTo("baz")
            assertThat(barMethod.modifiers()).isEqualTo(listOf("static", "final"))
            assertThat(bazMethod.modifiers()).isEqualTo(listOf("static", "final"))
        }
    }

    @Test
    fun `classlike companion properties are included in Kotlin and nested accessors in Java`() {
        val module =
            """
            |class Foo {
            |  companion object {
            |    val bar: List<String> = emptyList()
            |    protected val baz: Int = 1
            |  }
            |}
        """
                .render()

        val classlike = module.page("Foo").data.content

        kotlinOnly {
            assertThat(classlike.data.publicCompanionPropertiesSummary.item().name())
                .isEqualTo("bar")
            assertThat(classlike.data.protectedCompanionPropertiesSummary.item().name())
                .isEqualTo("baz")
        }
        javaOnly {
            assertThat(classlike.data.publicCompanionFunctionsSummary).isEmpty()
            assertThat(classlike.data.publicCompanionPropertiesSummary).isEmpty()
            assertThat(classlike.companionName()).isEqualTo("Foo.Companion")
            val companionClasslike = module.page { this.companionFor("Foo") }.data.content
            assertThat(companionClasslike.data.publicPropertiesSummary).isEmpty()
            val barGetter = companionClasslike.data.publicFunctionsSummary.single()
            assertThat(barGetter.name()).isEqualTo("getBar")
            assertThat(companionClasslike.data.protectedPropertiesSummary).isEmpty()
            val bazGetter = companionClasslike.data.protectedFunctionsSummary.single()
            assertThat(bazGetter.name()).isEqualTo("getBaz")
            assertThat(barGetter.modifiers()).isEqualTo(listOf("static", "final"))
            assertThat(bazGetter.modifiers()).isEqualTo(listOf("static", "final"))
        }
    }

    @Test
    fun `Named top-level objects have pages and treat members properly in 4x Java and Kotlin`() {
        val moduleK =
            """
            |object Foo {
            |  fun bar() = Unit
            |  const val baz = "baz"
            |}
        """
                .render()
        val classlikeK = moduleK.page("Foo").data.content

        val moduleJ =
            """${javaHeader("Foo")}
            |public class Foo {
            |  private Foo() {}
            |  public final void bar() {}
            |  public static final String baz = "baz";
            |  public static Foo INSTANCE = new Foo();
            |}
        """
                .renderWithoutLanguageHeader()
        val classlikeJ = moduleJ.page("Foo").data.content

        for (classlike in listOf(classlikeJ, classlikeK)) {
            assertThat(classlikeK.data.publicCompanionFunctionsSummary).isEmpty()
            assertThat(classlikeK.data.publicCompanionPropertiesSummary).isEmpty()
            assertThat(classlike.data.publicFunctionsSummary.single().name()).isEqualTo("bar")
            val props = classlike.data.publicPropertiesSummary
            val consts = classlike.data.constantsSummary
            assertThat(consts.map { it.name() }).containsExactly("baz")
            if (displayLanguage == Language.KOTLIN && classlike == classlikeK) {
                assertThat(props).isEmpty()
            } else {
                assertThat(props.single().name()).isEqualTo("INSTANCE")
            }

            val barMethod = classlike.data.publicFunctionsSummary.single { it.name() == "bar" }
            val barModifiers = barMethod.modifiers()
            val bazConst = consts.single { it.name() == "baz" }
            kotlinOnly {
                assertThat(barModifiers).isEmpty()
                assertThat(bazConst.modifiers()).isEqualTo(listOf("const"))
            }
            javaOnly {
                assertThat(barModifiers).isEqualTo(listOf("final"))
                assertThat(bazConst.modifiers()).isEqualTo(listOf("static", "final"))
            }
        }
    }

    @Test
    fun `Static and companion functions are treated correctly in both languages`() {
        val moduleK =
            """
            |class Foo {
            |  companion object {
            |    val baz: Int = 1
            |    fun bar() = Unit
            |  }
            |}
        """
                .render()
        val classlikeK = moduleK.page().data.content

        val classlikeJ =
            """
            |public class Foo {
            |  public static void foo() {}
            |  public static String bar = "bar"
            |}
        """
                .render(java = true)
                .page()
                .data
                .content

        val kotlinNestedTypeSummary = classlikeK.data.nestedTypesSummary

        val staticJavaMethod = classlikeJ.data.publicFunctionsDetails.symbols.single().data
        val staticJavaField = classlikeJ.data.publicPropertiesDetails.symbols.single().data

        // can see java static methods in both languages
        assertThat(staticJavaMethod.name).isEqualTo("foo")
        assertThat(staticJavaField.name).isEqualTo("bar")

        // can find kotlin companion method in both languages
        kotlinOnly {
            // nested companion object is not documented because it is inlined
            assertThat(kotlinNestedTypeSummary).isEmpty()
            val companionFunctions = classlikeK.data.publicCompanionFunctionsSummary
            val companionProperties = classlikeK.data.publicCompanionPropertiesSummary
            assertThat(companionFunctions).hasSize(1)
            assertThat(companionProperties).hasSize(1)
        }
        javaOnly {
            val companionK = moduleK.page("Companion").data.content
            // nested companion object is documented but companion functions are not inlined
            assertThat(companionK.data.publicPropertiesDetails).isEmpty()
            assertThat(companionK.data.publicFunctionsDetails.map { it.data.name })
                .containsExactly("bar", "getBaz")
            assertThat(kotlinNestedTypeSummary).hasSize(1)
            assertThat(classlikeK.data.publicCompanionFunctionsSummary).isEmpty()
            assertThat(classlikeK.data.publicCompanionPropertiesSummary).isEmpty()
            assertThat(staticJavaMethod.modifiers).contains("static")
            assertThat(staticJavaField.modifiers).contains("static")
        }
    }

    @Test
    fun `JvmStatic is correctly handled in java`() {
        if (displayLanguage != Language.JAVA) return
        val page =
            """
            |class Foo {
            |   companion object {
            |       @JvmStatic fun bar() {}
            |       @JvmStatic val baz = 8
            |   }
            |}
        """
                .render()
                .page("Foo")
                .data
                .content

        val nestedTypes = page.data.nestedTypesSummary
        val methods = page.data.publicFunctionsSummary
        val staticMethods = methods.filter { it.modifiers().contains("static") }

        // method `bar` can be referenced both as `Foo.bar` and `Foo.Companion.bar`
        assertThat(nestedTypes).hasSize(1)
        assertThat(methods.map { it.name() }).containsExactly("bar", "getBaz")
        assertThat(staticMethods).hasSize(2)
        assertThat(page.data.publicPropertiesDetails.symbols).isEmpty()
    }

    @Test
    fun `JvmField in companion object is static field in java and unchanged in kotlin`() {
        val module =
            """
            |class Foo {
            |   companion object FooCompanion {
            |       @JvmField val BAR = 8
            |       @JvmField var BAZ = "abc"
            |   }
            |}
        """
                .render()

        val classPage = module.page("Foo").data.content
        val companionPage = module.page("FooCompanion").data.content

        val nestedTypes = classPage.data.nestedTypesSummary
        val fields = classPage.data.publicPropertiesSummary
        val staticFields = fields.filter { it.modifiers().contains("static") }

        javaOnly {
            // static fields are removed from the companion object
            assertThat(nestedTypes.size).isEqualTo(1)
            assertThat(classPage.data.publicFunctionsSummary).isEmpty()
            assertThat(fields.map { it.name() }).containsExactly("BAR", "BAZ")
            assertThat(staticFields).hasSize(2)

            // companion object exists, but has no fields
            assertThat(companionPage.data.publicFunctionsSummary).isEmpty()
            assertThat(companionPage.data.publicPropertiesSummary.nonInstance()).isEmpty()
        }

        kotlinOnly {
            val companionProperties = classPage.data.publicCompanionPropertiesSummary.items()

            // on the other hand, nothing has changed from the kotlin's point of view
            assertThat(companionProperties.map { it.name() }).containsExactly("BAR", "BAZ")
        }
    }

    @Test
    fun `Class properties correctly interop to Java`() {
        val foo =
            """
            |class Foo {
            |    var regularVar = 0
            |    @JvmField var jvmFieldVar = 0
            |    lateinit var lateinitVar: String
            |    // const vals are not allowed in this context
            |    // @JvmStatic is not allowed in this context
            |}
        """
                .render()
                .page()
                .data
                .content
                .data

        javaOnly {
            val functions = foo.publicFunctionsSummary.data.items
            assertThat(functions.map { it.name() })
                .containsExactly(
                    "getRegularVar",
                    "setRegularVar",
                    "getLateinitVar",
                    "setLateinitVar",
                )

            val properties = foo.publicPropertiesSummary.data.items
            assertThat(properties.map { it.name() }).containsExactly("jvmFieldVar", "lateinitVar")

            // Nothing should be static
            assertThat(functions.filter { it.modifiers().contains("static") }).isEmpty()
            assertThat(properties.filter { it.modifiers().contains("static") }).isEmpty()
        }
    }

    @Test
    fun `Top-level properties correctly interop to Java`() {
        val module =
            """
            |/** Some documentation **/
            |var topLevelRegularVar = 0
            |/** Some documentation **/
            |@JvmField var topLevelJvmFieldVar = 0
            |/** Some documentation **/
            |const val topLevelConstVal = 0
            |/** Some documentation **/
            |lateinit var topLevelLateinitVar: String
            ||// @JvmStatic is not allowed in this context
        """
                .render()

        javaOnly {
            val testKt = module.page("TestKt").data.content.data
            val functions = testKt.publicFunctionsSummary.data.items
            assertThat(functions.map { it.name() })
                .containsExactly(
                    "getTopLevelRegularVar",
                    "setTopLevelRegularVar",
                    "getTopLevelLateinitVar",
                    "setTopLevelLateinitVar",
                )

            val properties = testKt.publicPropertiesSummary.data.items
            assertThat(properties.map { it.name() })
                .containsExactly("topLevelJvmFieldVar", "topLevelLateinitVar")

            val consts = testKt.constantsSummary.data.items
            assertThat(consts.map { it.name() }).containsExactly("topLevelConstVal")

            assertThat(functions.filter { it.modifiers().contains("static") }).hasSize(4)
            assertThat(properties.filter { it.modifiers().contains("static") }).hasSize(2)
            assertThat(consts.filter { it.modifiers().contains("static") }).hasSize(1)

            for (item in functions + properties + consts) {
                assertThat(item.data.description.text()).isEqualTo("Some documentation")
            }
        }
    }

    @Test
    fun `Synthetic classes for top-level functions in Java use @JvmName`() {
        val module =
            """
            |@JvmName("bar")
            |fun foo()
            |
            |@JvmName("aardvark")
            |fun baz()
            |
            |fun apple()
        """
                .render()

        javaOnly {
            val testKt = module.page("TestKt").data.content.data
            // also assert the alphabetical sort, after jvmname
            val names = testKt.publicFunctionsSummary.map { it.name() }
            assertThat(names).isEqualTo(listOf("aardvark", "apple", "bar"))
        }
    }

    @Test
    fun `Object properties correctly interop to Java`() {
        val foo =
            """
            |object Foo {
            |    var regularVar = 0
            |    @JvmField var jvmFieldVar = 0
            |    lateinit var lateinitVar: String
            |    const val constVal = 0
            |    @JvmStatic var jvmStaticVar = 0
            |}
        """
                .render()
                .page()
                .data
                .content
                .data

        javaOnly {
            val functions = foo.publicFunctionsSummary
            assertThat(functions.map { it.name() })
                .containsExactly(
                    "getRegularVar",
                    "getLateinitVar",
                    "getJvmStaticVar",
                    "setRegularVar",
                    "setLateinitVar",
                    "setJvmStaticVar",
                )
            val staticFunctions =
                functions
                    .filter { it.data.title.data.modifiers.contains("static") }
                    .map { it.name() }
            assertThat(staticFunctions).containsExactly("getJvmStaticVar", "setJvmStaticVar")

            val properties = foo.publicPropertiesSummary
            assertThat(properties.map { it.name() })
                .containsExactly("jvmFieldVar", "lateinitVar", "INSTANCE")
            // All of these properties appear as static
            assertThat(properties.filter { it.data.title.data.modifiers.contains("static") })
                .hasSize(3)

            val constants = foo.constantsSummary.map { it.name() }
            assertThat(constants).containsExactly("constVal")
        }
    }

    @Test
    fun `Top level extension properties are correctly documented in Java`() {
        javaOnly {
            val testKt =
                """
                |/** Some documentation **/
                |val String.extensionProp: Int get() = length
                |// @JvmStatic is not allowed at the top level
                |// const, lateinit, and @JvmField extension properties are not allowed
            """
                    .render()
                    .page("TestKt")
                    .data
                    .content
                    .data

            // The getter should have one parameter, the receiver string
            val functions = testKt.publicFunctionsSummary.data.items
            assertThat(functions.map { it.name() }).containsExactly("getExtensionProp")
            val getExtensionProp = functions[0]
            assertThat(getExtensionProp.data.description.data.signature.data.parameters).hasSize(1)

            assertThat(testKt.publicPropertiesSummary.data.items).isEmpty()

            assertThat(getExtensionProp.modifiers()).contains("static")
            assertThat(getExtensionProp.description.text()).isEqualTo("Some documentation")
        }
    }

    @Test
    fun `Extension properties inside objects are correctly documented in Java`() {
        val foo =
            """
            |// const, lateinit, and @JvmField extension properties are not allowed
            |object Foo {
            |    val String.objExtension: Int get() = 0
            |    val String.objExtensionWithStaticGetter: Int @JvmStatic get() = 5
            |    @JvmStatic val String.objStaticExtension: Int get() = 5
            |}
        """
                .render()
                .page()
                .data
                .content
                .data

        javaOnly {
            val functions = foo.publicFunctionsSummary.data.items
            assertThat(functions.map { it.name() })
                .containsExactly(
                    "getObjExtension",
                    "getObjExtensionWithStaticGetter",
                    "getObjStaticExtension",
                )

            // The getters should have one parameter, the receiver string
            for (getter in functions) {
                assertThat(getter.data.description.data.signature.data.parameters).hasSize(1)
            }

            val statics = functions.filter { it.modifiers().contains("static") }.map { it.name() }
            assertThat(statics)
                .containsExactly("getObjExtensionWithStaticGetter", "getObjStaticExtension")

            assertThat(foo.publicPropertiesSummary.data.items.map { it.name() })
                .containsExactly("INSTANCE")
        }
    }

    @Test
    fun `lateinit property in companion object is static field in java and unchanged in kotlin`() {
        val module =
            """
            |class Foo {
            |   companion object FooCompanion {
            |       lateinit var bar: String;
            |   }
            |}
        """
                .render()

        val classPage = module.page("Foo").data.content
        val companionPage = module.page("FooCompanion").data.content

        val nestedTypes = classPage.data.nestedTypesSummary
        val fields = classPage.data.publicPropertiesSummary
        val staticFields = fields.filter { it.modifiers().contains("static") }

        val companionPageMethods = companionPage.data.publicFunctionsSummary
        val companionPageFields = companionPage.data.publicPropertiesSummary.nonInstance()

        javaOnly {
            // lateinit properties are a field of the containing class (not getters/setters)
            assertThat(nestedTypes.size).isEqualTo(1)
            assertThat(classPage.data.publicFunctionsSummary).isEmpty()
            assertThat(fields.map { it.name() }).containsExactly("bar")
            assertThat(staticFields).hasSize(1)

            // lateinit fields appear as getters/setters on the companion object
            assertThat(companionPageMethods.map { it.name() }).containsExactly("getBar", "setBar")
            assertThat(companionPageFields).isEmpty()
        }

        kotlinOnly {
            val companionProperties = classPage.data.publicCompanionPropertiesSummary.items()

            // on the other nothing has changed from the kotlin's point of view
            assertThat(companionProperties.map { it.name() }).containsExactly("bar")
        }
    }

    @Test
    fun `const property in companion object is static field in java and const in kotlin`() {
        val classlike =
            """
            |class Foo {
            |   companion object {
            |       const val MARGIN = 9
            |   }
            |}
        """
                .render()
                .page("Foo")
                .data
                .content

        val fields = classlike.data.publicPropertiesSummary

        val constantFields = classlike.data.constantsSummary
        assertThat(constantFields.map { it.name() }).containsExactly("MARGIN")

        javaOnly {
            assertThat(classlike.data.publicFunctionsSummary).isEmpty()

            assertThat(fields).isEmpty()
        }

        kotlinOnly {
            assertThat(classlike.data.publicCompanionPropertiesSummary).isEmpty()
            assertThat(classlike.data.protectedCompanionPropertiesSummary).isEmpty()
        }
    }

    @Test
    fun `static getters in companion objects can be renamed in java`() {
        val page =
            """
            |class Foo {
            |   companion object {
            |       @JvmStatic
            |       @get:JvmName("computeBar")
            |       val bar = 8
            |   }
            |}
        """
                .render()
                .page("Foo")
                .data
                .content

        val methods = page.data.publicFunctionsSummary
        val staticMethods = methods.filter { it.modifiers().contains("static") }

        javaOnly {
            assertThat(methods.map { it.name() }).containsExactly("computeBar")
            assertThat(staticMethods).hasSize(1)
        }

        kotlinOnly {
            val companionProperties = page.data.publicCompanionPropertiesSummary
            val companionFunctions = page.data.publicCompanionFunctionsSummary

            assertThat(companionProperties.items()).hasSize(1)
            assertThat(companionFunctions).isEmpty()
        }
    }

    @Test
    fun `companion objects can be named`() {
        val module =
            """
            |class Foo {
            |    companion object Named {
            |        fun bar() {}
            |    }
            |}
        """
                .render()

        val classPage = module.page("Foo").data.content
        val companionPage = module.page("Named").data.content

        val nestedTypes = classPage.data.nestedTypesSummary
        val methods = classPage.data.publicFunctionsSummary
        val companionPageMethods = companionPage.data.publicFunctionsSummary

        assertThat(classPage.companionName()).isEqualTo("Foo.Named")
        assertThat(companionPageMethods.map { it.name() }).containsExactly("bar")
        assertThat(methods).isEmpty()

        kotlinOnly {
            val companionFunctions = classPage.data.publicCompanionFunctionsSummary
            assertThat(companionFunctions.items().map { it.name() }).containsExactly("bar")
        }
    }

    @Test
    fun `companion objects can inherit`() {
        val module =
            """
            |class Companionable {
            |   fun bar() {}
            |}
            |
            |class Foo {
            |    companion object : Companionable
            |}
        """
                .render()

        val classPage = module.page("Foo").data.content
        assertThat(classPage.companionName()).isEqualTo("Foo.Companion")

        val companionPage = module.page("Companion").data.content

        val companionInheritedMethodsSummaries = companionPage.data.inheritedFunctions

        assertThat(companionInheritedMethodsSummaries.map { it.name() }).containsExactly("bar")

        kotlinOnly {
            // Inherited companion functions are not hoisted
            assertThat(classPage.data.inheritedFunctions).isEmpty()
        }
    }

    @Test
    fun `Boring companion objects do not appear in 'nested types'`() {
        val module =
            """
            |class Foo {
            |    class Bar {
            |        companion object
            |    }
            |}
        """
                .render()

        val fooPage = module.page("Foo").data.content
        val barPage = module.page("Bar").data.content
        fun Classlike.nestedTypeNames() =
            data.nestedTypesSummary.map { it.data.description.data.signature.fullName() }

        assertThat(fooPage.nestedTypeNames()).containsExactly("Foo.Bar")
        assertThat(barPage.nestedTypeNames()).isEmpty()
    }

    @Test
    fun `Companions with extension functions or properties are not boring`() {
        val module =
            """
            |class Foo {
            |    companion object {}
            |}
            |class Bar {
            |    companion object {}
            |}
            |fun Foo.Companion.extFun() = Unit
            |val Bar.Companion.extBar: Int get() = 0
        """
                .render()

        val foo = module.page("Foo").data.content.data
        val bar = module.page("Bar").data.content.data

        assertThat(foo.nestedTypesSummary).hasSize(1)
        assertThat(bar.nestedTypesSummary).hasSize(1)

        val companions = module.pages("Companion")
        val fooCompanion =
            companions.single { it.data.pathForSwitcher!!.contains("Foo") }.data.content.data
        val barCompanion =
            companions.single { it.data.pathForSwitcher!!.contains("Bar") }.data.content.data

        assertThat(fooCompanion.extensionFunctionsSummary).hasSize(1)
        // Extension properties appear as accessors in Java, properties in Kotlin
        javaOnly { assertThat(barCompanion.extensionFunctionsSummary).hasSize(1) }
        kotlinOnly { assertThat(barCompanion.extensionPropertiesSummary).hasSize(1) }
    }

    @Test // TODO: non-overridden inherited elements in companion objects are missing
    fun `companion objects that inherits still can have static forwarders`() {
        // Aka you can add the JvmStatic-ness in an override
        // NOTE: "@JvmField cannot be applied to a property that overrides another property"
        // NOTE: "@JvmField can only be applied to final property"
        // NOTE: @JvmStatic can only be applied to elements in a static context, e.g. an object,
        // not an interface like Companionable, and you can't inherit from singletons/static context
        // NOTE: "property in an interface cannot have a backing field" (no getters in interface)
        val module =
            """
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
        """
                .render()

        val classPage = module.page("Foo").data.content
        val companionPage = module.page("Companion").data.content
        assertThat(classPage.companionName()).isEqualTo("Foo.Companion")

        val classMethods = classPage.data.publicFunctionsSummary
        val classFields = classPage.data.publicPropertiesSummary
        val companionPageMethods = companionPage.data.publicFunctionsSummary
        val companionPageFields = companionPage.data.publicPropertiesSummary
        val companionInheritedProperties =
            companionPage.data.inheritedProperties
                .from("androidx.example.Companionable")
                ?.value
                ?.items() ?: emptyList()
        val companionInheritedFunctions =
            companionPage.data.inheritedFunctions
                .from("androidx.example.Companionable")
                ?.value
                ?.items() ?: emptyList()
        val inheritedDefaultProp =
            companionPage.data.inheritedProperties
                .from("androidx.example.ForDefaultProperties")
                ?.value
                ?.items() ?: emptyList()

        kotlinOnly {
            // Overridden functions
            assertThat(companionPageMethods.map { it.name() })
                .containsExactly("becomesStaticFun", "notStaticOverriddenFun")
            // Overridden vars
            val companionProperties = classPage.data.publicCompanionPropertiesSummary.items()
            val companionFunctions = classPage.data.publicCompanionFunctionsSummary.items()
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
            // These fields are not overridden but TODO staticness
            assertThat(companionInheritedProperties.map { it.name() }).isEmpty()
            assertThat(inheritedDefaultProp.map { it.name() })
                .containsExactly("notStaticNonOverriddenVar")
        }
        javaOnly {
            // Overridden functions
            assertThat(companionPageMethods.map { it.name() })
                .containsExactly(
                    "becomesStaticFun",
                    "getBecomesLateinitVar",
                    "getNotStaticOverriddenVar",
                    "notStaticOverriddenFun",
                    "setBecomesLateinitVar",
                    "setNotStaticOverriddenVar",
                )
            // Overridden vars
            assertThat(companionPageFields).isEmpty()
            assertThat(classMethods.map { it.name() }).containsExactly("becomesStaticFun")
            // lateinit companion properties are accessed through methods on the companion,
            // and as a field on the class
            assertThat(classFields.map { it.name() }).containsExactly("becomesLateinitVar")
        }
    }

    @Suppress("SuspiciousCollectionReassignment")
    @Test
    fun `Comprehensive companion function-property hoist-duplication test`() {
        val module =
            """
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
        """
                .render()

        fun <T : SymbolSignature> Iterable<TypeSummaryItem<T>>.names() = map { it.name() }
        val names =
            listOf(
                "publicConlyFun",
                "protectedConlyFun",
                "publicDuplicatedFun",
                "protectedDuplicatedFun",
                "publicHoistedField",
                "protectedHoistedField",
                "publicHoistedConst",
                "protectedHoistedConst",
            ) +
                if (displayLanguage == Language.KOTLIN) {
                    listOf(
                        "publicConlyProp",
                        "protectedConlyProp",
                        "publicDuplicatedProp",
                        "protectedDuplicatedProp",
                    )
                } else {
                    listOf(
                        "getPublicConlyProp",
                        "getProtectedConlyProp",
                        "getPublicDuplicatedProp",
                        "getProtectedDuplicatedProp",
                    )
                }

        val containerClass = module.page("TheContainer").data.content
        val companionClass = module.page("TheCompanion").data.content
        assertThat(containerClass.companionName()).isEqualTo("TheContainer.TheCompanion")
        // Pull public/protected elements that are hoisted or are in the companion
        // "CompanionONLY" conly is a misnomer right now; includes methods in both companion&parent
        var publicHoistedFuns =
            if (displayLanguage == Language.KOTLIN) {
                containerClass.data.publicCompanionFunctionsSummary.data.items
            } else containerClass.data.publicFunctionsSummary.filter { "static" in it.modifiers() }
        var protectedHoistedFuns =
            if (displayLanguage == Language.KOTLIN) {
                containerClass.data.protectedCompanionFunctionsSummary.data.items
            } else
                containerClass.data.protectedFunctionsSummary.filter { "static" in it.modifiers() }
        var publicHoistedProps =
            if (displayLanguage == Language.KOTLIN) {
                containerClass.data.publicCompanionPropertiesSummary.data.items
            } else containerClass.data.publicPropertiesSummary.filter { "static" in it.modifiers() }
        var protectedHoistedProps =
            if (displayLanguage == Language.KOTLIN) {
                containerClass.data.protectedCompanionPropertiesSummary.data.items
            } else
                containerClass.data.protectedPropertiesSummary.filter { "static" in it.modifiers() }
        var publicConlyFuns = companionClass.data.publicFunctionsSummary.data.items
        var protectedConlyFuns = companionClass.data.protectedFunctionsSummary.data.items
        var publicConlyProps = companionClass.data.publicPropertiesSummary.data.items
        var protectedConlyProps = companionClass.data.protectedPropertiesSummary.data.items
        // Now filter out elements in both hoisted and conly and put them in duplicated
        val publicDuplicatedFuns = publicConlyFuns.intersect(publicHoistedFuns.toSet())
        publicConlyFuns -= publicDuplicatedFuns
        publicHoistedFuns -= publicDuplicatedFuns
        val protectedDuplicatedFuns = protectedConlyFuns.intersect(protectedHoistedFuns.toSet())
        protectedConlyFuns -= protectedDuplicatedFuns
        protectedHoistedFuns -= protectedDuplicatedFuns
        val publicDuplicatedProps = publicConlyProps.intersect(publicHoistedProps.toSet())
        publicConlyProps -= publicDuplicatedProps
        publicHoistedProps -= publicDuplicatedProps
        val protectedDuplicatedProps = protectedConlyProps.intersect(protectedHoistedProps.toSet())
        protectedConlyProps -= protectedDuplicatedProps
        protectedHoistedProps -= protectedDuplicatedProps
        // No visibility distinction for constants: b/237083570
        // Constants are all hoisted in Java and duplicated in Kotlin
        val hoistedConstants = containerClass.data.constantsSummary
        val companionConstants = companionClass.data.constantsSummary

        javaOnly {
            // Perform asserts based on name mangling
            assertThat(publicHoistedFuns.names())
                .containsExactlyElementsIn(
                    names.filter { "ublic" in it && "Hoisted" in it && "Fun" in it }
                )
            assertThat(protectedHoistedFuns.names())
                .containsExactlyElementsIn(
                    names.filter { "rotected" in it && "Hoisted" in it && "Fun" in it }
                )
            assertThat(publicHoistedProps.names())
                .containsExactlyElementsIn(
                    names.filter {
                        "ublic" in it && "Hoisted" in it && ("Prop" in it || "Field" in it)
                    }
                )
            assertThat(protectedHoistedProps.names())
                .containsExactlyElementsIn(
                    names.filter {
                        "rotected" in it && "Hoisted" in it && ("Prop" in it || "Field" in it)
                    }
                )
            assertThat(publicConlyFuns.names())
                .containsExactlyElementsIn(
                    names.filter { "ublic" in it && "Conly" in it && ("Fun" in it || "get" in it) }
                )
            assertThat(protectedConlyFuns.names())
                .containsExactlyElementsIn(
                    names.filter {
                        "rotected" in it && "Conly" in it && ("Fun" in it || "get" in it)
                    }
                )
            // Regular properties converted to accessors in java
            assertThat(publicConlyProps.names()).isEmpty()
            assertThat(protectedConlyProps.names()).isEmpty()
            assertThat(publicDuplicatedFuns.names())
                .containsExactlyElementsIn(
                    names.filter {
                        "ublic" in it && "Duplicated" in it && ("Fun" in it || "get" in it)
                    }
                )
            assertThat(protectedDuplicatedFuns.names())
                .containsExactlyElementsIn(
                    names.filter {
                        "rotected" in it && "Duplicated" in it && ("Fun" in it || "get" in it)
                    }
                )
            assertThat(publicDuplicatedProps.names()).isEmpty()
            assertThat(protectedDuplicatedProps.names()).isEmpty()
            assertThat(hoistedConstants.names())
                .containsExactlyElementsIn(names.filter { "Const" in it })
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
            assertThat(publicDuplicatedFuns.names())
                .containsExactlyElementsIn(names.filter { "ublic" in it && "Fun" in it })
            assertThat(protectedDuplicatedFuns.names())
                .containsExactlyElementsIn(names.filter { "rotected" in it && "Fun" in it })
            assertThat(publicDuplicatedProps.names())
                .containsExactlyElementsIn(
                    names.filter { "ublic" in it && ("Prop" in it || "Field" in it) }
                )
            assertThat(protectedDuplicatedProps.names())
                .containsExactlyElementsIn(
                    names.filter { "rotected" in it && ("Prop" in it || "Field" in it) }
                )
            assertThat(hoistedConstants).containsExactlyElementsIn(companionConstants)
            assertThat(hoistedConstants.names())
                .containsExactlyElementsIn(names.filter { "Const" in it })
        }
    }

    @Test
    fun `Extension functions are included on Java and Kotlin pages`() {
        val src =
            """
            |class Foo {
            |}
            |fun Foo.bar() = Unit
            |fun Foo?.baz() = Unit
        """
        val classlike = src.render().page().data.content
        assertThat(classlike.data.extensionFunctionsSummary.data.items).hasSize(2)

        kotlinOnly {
            val (extDetail1, extDetail2) = classlike.data.extensionFunctionsDetails.symbols
            assertThat(extDetail1.data.signature.receiverTypeName()).isEqualTo("Foo")
            assertThat(extDetail2.data.signature.receiverTypeName()).isEqualTo("Foo?")
        }
    }

    @Test
    fun `Extension functions are linked correctly on both Java and Kotlin pages`() {
        val src =
            """
            |class Foo {
            |}
            |fun Foo.bar() = Unit
        """
        val classlike = src.render().page().data.content
        val extFunction = classlike.data.extensionFunctionsDetails.symbols[0]
        val url = extFunction.data.signature.data.name.data.url
        assertThat(url).endsWith("androidx/example/Foo.html#(androidx.example.Foo).bar()")
    }

    @Test
    fun `Extension functions are ordered by the package they come from`() {
        val src =
            listOf(
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
            """,
            )
        val classlike = src.render().page().data.content
        val extFunctions = classlike.data.extensionFunctionsDetails.symbols
        val extFunctionClasses = extFunctions.map { it.data.extFunctionClass }
        assertThat(extFunctionClasses).isEqualTo(listOf("FirstKt", "SecondKt"))
    }

    @Test
    fun `Extension functions respect @JvmName for packages`() {
        val src =
            listOf(
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
            """,
            )
        val classlike = src.render().page().data.content
        val extFunctions = classlike.data.extensionFunctionsDetails.symbols
        val extFunctionClasses = extFunctions.map { it.data.extFunctionClass }
        assertThat(extFunctionClasses).isEqualTo(listOf("FirstJvm", "SecondJvm"))
    }

    @Test
    fun `Extension functions do not apply to different class with same name`() {
        val src =
            listOf(
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
            """,
            )

        // Each Foo should have one extension function: bar for foo1, baz for foo2
        val pages = src.render().pages("Foo")
        assertThat(pages.size).isEqualTo(2)
        for (page in pages) {
            val classlike = page.data.content
            assertThat(classlike.data.extensionFunctionsDetails.symbols.size).isEqualTo(1)
        }
    }

    @Test
    fun `Extension functions work for inner classes`() {
        val src =
            """
            |class Foo {
            |    class Bar {
            |    }
            |}
            |fun Foo.Bar.baz() = Unit
        """
        val classlike = src.render().page("Bar").data.content
        assertThat(classlike.data.extensionFunctionsDetails.symbols).hasSize(1)
    }

    @Test
    fun `Extension properties are handled correctly as-Java and as-Kotlin`() {
        val module =
            """
            |class Foo {}
            |var Foo.bar: Int
            |  get() = 5
            |  set(value) = TODO()
        """
                .render()
        val classlikeFoo = module.page("Foo").data.content

        javaOnly {
            val classlikeKt = module.page("TestKt").data.content

            assertThat(classlikeFoo.data.extensionPropertiesDetails).isEmpty()
            assertThat(classlikeKt.data.publicPropertiesDetails).isEmpty()

            val fooExtFunctions = classlikeFoo.data.extensionFunctionsDetails.symbols.items(2)
            val ktFunctions = classlikeKt.data.publicFunctionsDetails.symbols.items(2)

            for (funPair in listOf(fooExtFunctions, ktFunctions)) {
                val (extGetterDetail, extSetterDetail) = funPair

                assertThat(extGetterDetail.data.name).isEqualTo("getBar")
                val getterSignature = extGetterDetail.data.signature
                assertThat(getterSignature.data.parameters.single().typeName()).isEqualTo("Foo")
                assertThat(getterSignature.data.parameters.single().data.name).isEqualTo("receiver")

                assertThat(extSetterDetail.data.name).isEqualTo("setBar")
                val setterSignature = extSetterDetail.data.signature
                val (param1, param2) = setterSignature.data.parameters.items(2)
                assertThat(param1.typeName()).isEqualTo("Foo")
                assertThat(param1.data.name).isEqualTo("receiver")
                assertThat(param2.typeName()).isEqualTo("int")
                assertThat(param2.data.name).isEqualTo("value")

                // When displayed as an extension function the receiver is needed, when displayed
                // as a method of TestKt it isn't.
                if (funPair == fooExtFunctions) {
                    assertThat(getterSignature.data.receiver!!.typeName()).isEqualTo("TestKt")
                    assertThat(setterSignature.data.receiver!!.typeName()).isEqualTo("TestKt")
                } else {
                    assertThat(getterSignature.data.receiver).isNull()
                    assertThat(setterSignature.data.receiver).isNull()
                }
            }
        }

        kotlinOnly {
            val packageSummary = module.packagePage().data.content
            val extPropertiesFoo =
                classlikeFoo.data.extensionPropertiesSummary to
                    classlikeFoo.data.extensionPropertiesDetails
            val extPropertiesPackage =
                packageSummary.data.extensionPropertiesSummary to
                    packageSummary.data.extensionProperties
            for (extProperties in listOf(extPropertiesFoo, extPropertiesPackage)) {
                assertThat(extProperties.first.data.items.single().name()).isEqualTo("bar")
                val extDeet = extProperties.second.single()
                assertThat(extDeet.data.name).isEqualTo("bar")
                assertThat(extDeet.data.signature.data.receiver!!.typeName()).isEqualTo("Foo")
            }
        }
    }

    @Test
    fun `Extension function defined inside classlike has correct receiver`() {
        val foo =
            """
            |object Foo {
            |    @JvmStatic
            |    fun String?.stringExtension() = Unit
            |}
        """
                .render()
                .page()
                .data
                .content
                .data

        val extFun = foo.publicFunctionsDetails.item().data
        val extSignature = extFun.signature.data
        assertThat(extFun.name).isEqualTo("stringExtension")

        javaOnly {
            assertThat(extSignature.receiver).isNull()
            val extParam = extSignature.parameters.single()
            assertThat(extParam.typeName()).isEqualTo("String")
            assertThat(extParam.nullable).isTrue()
            assertThat(extParam.data.name).isEqualTo("receiver")
        }
        kotlinOnly {
            val receiver = extSignature.receiver
            assertThat(receiver).isNotNull()
            assertThat(receiver!!.typeName()).isEqualTo("String")
            assertThat(receiver.nullable).isTrue()
            assertThat(extSignature.parameters).isEmpty()
        }
    }

    @Test
    fun `Extension property defined inside classlike has correct receiver`() {
        val foo =
            """
            |object Foo {
            |    @JvmStatic
            |    val String.stringExtension: Int
            |        get() = 0
            |}
        """
                .render()
                .page()
                .data
                .content
                .data

        javaOnly {
            val extFun = foo.publicFunctionsDetails.item().data
            val extSignature = extFun.signature.data
            assertThat(extFun.name).isEqualTo("getStringExtension")

            assertThat(extSignature.receiver).isNull()
            val extParam = extSignature.parameters.single()
            assertThat(extParam.typeName()).isEqualTo("String")
            assertThat(extParam.data.name).isEqualTo("receiver")
        }
        kotlinOnly {
            val extProp = foo.publicPropertiesDetails.item().data
            val receiver = extProp.signature.data.receiver
            assertThat(receiver).isNotNull()
            assertThat(receiver!!.typeName()).isEqualTo("String")
        }
    }

    @Ignore // TODO: b/195529157
    @Test
    fun `Annotation types with no parameters have no default constructors`() {
        // parameterless annotations are invoked as `@NonNull` not `@NonNull()`
        val documentationJ =
            """
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
        """
                .render(java = true)
                .page("PlaybackSuppressionReason")
                .data
                .content
        val documentationK =
            """
        |public annotation class Mega {
        |   /**
        |    * Reason why playback is suppressed even though {@link #getPlayWhenReady()} is {@code true}. One
        |    * of {@link #PLAYBACK_SUPPRESSION_REASON_NONE} or {@link
        |    * #PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS}.
        |    */
        |   annotation class PlaybackSuppressionReason {}
        |}
        """
                .render()
                .page("PlaybackSuppressionReason")
                .data
                .content

        for (documentation in listOf(documentationJ, documentationK)) {
            assertThat(documentation.data.publicConstructorsSummary).isEmpty()
        }
    }

    @Test
    fun `Java getters and setters are documented`() {
        val page =
            """
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
        """
                .render(java = true)
                .page()

        val classlike = page.data.content
        val publicMethodSymbols = classlike.data.publicFunctionsDetails
        val protectedMethodSymbols = classlike.data.protectedFunctionsDetails
        val publicMethodNames = publicMethodSymbols.symbols.map { it.data.name }
        val protectedMethodNames = protectedMethodSymbols.symbols.map { it.data.name }

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
        val page =
            """
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
        """
                .render(java = true)
                .page()

        val classlike = page.data.content

        val aProp = classlike.data.publicPropertiesDetails.symbols.single { it.data.name == "a" }
        assertThat(aProp.data.symbolKind).isEqualTo(SymbolDetail.SymbolKind.READ_ONLY_PROPERTY)

        val publicMethodSymbols = classlike.data.publicFunctionsDetails.symbols
        val publicMethodNames = publicMethodSymbols.map { it.data.name }
        kotlinOnly {
            assertThat(publicMethodNames).isEqualTo(listOf("getA")) // TODO: fix upstream
            // assertThat(publicMethodNames).isEmpty()
        }
        javaOnly { assertThat(publicMethodNames).isEqualTo(listOf("getA")) }
    }

    @Test
    fun `Kotlin properties are documented as getters and setters in Java`() {
        val page =
            """
            |data class Foo(val a: Int, var b: Int) {
            |    var c: Int
            |       get() = 0
            |       set(c: Int): Unit
            |}
        """
                .render()
                .page()

        val classlike = page.data.content
        val methodNames = classlike.data.publicFunctionsDetails.map { it.data.name }
        val propertyNames = classlike.data.publicPropertiesDetails.map { it.data.name }

        javaOnly {
            assertThat(methodNames).containsExactly("getA", "getB", "getC", "setB", "setC")
            assertThat(propertyNames).isEmpty()
        }
        kotlinOnly {
            assertThat(methodNames).isEmpty()
            assertThat(propertyNames).containsExactly("a", "b", "c")
        }
    }

    @Test
    fun `Docs for defined setter are correct`() {
        val fooK =
            """
            |class Foo {
            |    var currentState: Int
            |       get() = 0
            |        /**
            |         * Sets the state.
            |         * @param state new state
            |         */
            |        set(state) {}
            |}
        """
                .render()
                .page()
                .data
                .content
        val fooJ =
            """
            |public class Foo {
            |    private int currentState = 0;
            |    public int getCurrentState() { return currentState; }
            |    /**
            |     * Sets the state.
            |     * @param state new state
            |     */
            |    public void setCurrentState(int state) { currentState = state; }
            |}
        """
                .render(java = true)
                .page()
                .data
                .content

        for (foo in listOf(fooJ, fooK)) {
            javaOnly {
                val functions = foo.data.publicFunctionsDetails
                val setter = functions.single { it.data.name == "setCurrentState" }
                val setterDocs = setter.data.metadata[0] as DescriptionComponent
                assertThat(setterDocs.text()).isEqualTo("Sets the state.")
                val setterParamDocs =
                    (setter.data.metadata[1] as SummaryList<*>)[0].data.description
                        as DescriptionComponent
                assertThat(setterParamDocs.text()).isEqualTo("new state")
                val setterParam = setter.data.signature.data.parameters.single().data
                // TODO (b/268236485): the given name is ignored
                // assertThat(setterParam.name).isEqualTo("state")
            }
        }
    }

    @Test
    fun `Docs for property propagate to defined accessors`() {
        val foo =
            """
            |class Foo {
            |    /** Some documentation. */
            |    var definedAccessorsDocsOnProp: Int
            |        get() = 0
            |        set(v) {}
            |
            |    /** Some documentation. */
            |    var noDefinedAccessorsDocsOnProp = 0
            |
            |    var definedAccessorsDocsOnAccessors: Int
            |        /** Some documentation. */
            |        get() = 0
            |        /** Some documentation. */
            |        set(v) {}
            |
            |    /** Some property docs, not for accessors. */
            |    var definedAccessorsDocsOnBoth: Int
            |        /** Some documentation. */
            |        get() = 0
            |        /** Some documentation. */
            |        set(v) {}
            |}
        """
                .render()
                .page()
                .data
                .content
                .data

        javaOnly {
            val functions = foo.publicFunctionsSummary
            assertThat(functions.map { it.name() })
                .containsExactly(
                    "getDefinedAccessorsDocsOnProp",
                    "setDefinedAccessorsDocsOnProp",
                    "getNoDefinedAccessorsDocsOnProp",
                    "setNoDefinedAccessorsDocsOnProp",
                    "getDefinedAccessorsDocsOnAccessors",
                    "setDefinedAccessorsDocsOnAccessors",
                    "getDefinedAccessorsDocsOnBoth",
                    "setDefinedAccessorsDocsOnBoth",
                )
            for (function in functions) {
                assertThat(function.description.text()).isEqualTo("Some documentation.")
            }
        }
    }

    @Test
    fun `Property with @property tag is documented correctly as-Java`() {
        val foo =
            """
            |class Foo {
            |    /** @property foo This is a foo. */
            |    var foo: Int
            |        get() = 0
            |        set() {}
            |}
        """
                .render()
                .page()
                .data
                .content
                .data
        javaOnly {
            val functions = foo.publicFunctionsSummary
            assertThat(functions.map { it.name() }).containsExactly("getFoo", "setFoo")
            for (function in functions) {
                assertThat(function.description.text()).isEqualTo("This is a foo.")
            }
        }
    }

    @Test
    fun `Default no-arg constructors are autogenerated`() {
        val emptyTestClass =
            """
            public class Foo {}
            """
                .trimIndent()

        for (isJava in listOf(true, false)) {
            val classlike = emptyTestClass.render(java = isJava).page("Foo").data.content
            val constructorList = classlike.data.publicConstructorsDetails.symbols
            assertThat(constructorList.size).isEqualTo(1)
            assertThat(constructorList.single().data.name).isEqualTo("Foo")
        }
    }

    @Test
    fun `Default no-arg constructors are not autogenerated for annotations`() {
        val classlikeJ =
            """
            public @interface Foo {}
            """
                .trimIndent()
                .render(java = true)
                .page("Foo")
                .data
                .content
        val classlikeK =
            """
            public annotation class Foo {}
            """
                .trimIndent()
                .render(java = false)
                .page("Foo")
                .data
                .content

        // TODO: b/195529157 `listOf(classlikeJ, classlikeK)`
        for (classlike in listOf(classlikeJ)) {
            assertThat(classlike.data.publicConstructorsDetails).isEmpty()
        }
    }

    @Test
    fun `Default constructors are autogenerated when no explicit constructor is present`() {
        val moduleJ =
            """
            public class Foo {}
            """
                .trimIndent()
                .render(java = true)
        val classlikeJ = moduleJ.page("Foo").data.content
        val moduleK =
            """
            public class Foo {}
            """
                .trimIndent()
                .render(java = false)
        val classlikeK = moduleK.page("Foo").data.content

        // test the Documentables tree. The correct behavior would be both isNotEmpty
        for (module in listOf(moduleJ, moduleK)) {
            val classlike = module.explicitClasslike("Foo")
            assertThat((classlike as DClass).constructors).isNotEmpty()
        }

        // test the Documentables tree. The correct behavior is both isNotEmpty
        for (classlike in listOf(classlikeJ, classlikeK)) {
            assertThat(classlike.data.publicConstructorsDetails).isNotEmpty()
        }
    }

    @Test
    fun `Default constructors are not autogenerated when private-constructor pattern is used`() {
        val moduleJ =
            """
            public class Foo { private Foo() {} }
            """
                .trimIndent()
                .render(java = true)
        val classlikeJ = moduleJ.page("Foo").data.content
        val moduleK =
            """
            public class Foo private constructor() {}
            """
                .trimIndent()
                .render(java = false)
        val classlikeK = moduleK.page("Foo").data.content

        // test the Documentables tree. The correct behavior is both isEmpty
        for (module in listOf(moduleJ, moduleK)) {
            val classlike = module.explicitClasslike("Foo")
            assertThat((classlike as DClass).constructors).isEmpty()
        }

        // test our Components tree. The correct behavior is both isEmpty.
        for (classlike in listOf(classlikeJ, classlikeK)) {
            assertThat(classlike.data.publicConstructorsDetails).isEmpty()
        }
    }

    @Test // Interfaces "extend" other interfaces, while classes "implement" interfaces
    fun `Interface extending another interface uses correct keyword`() {
        val signatureJ =
            """
            public interface Foo {}
            public interface Bar extends Foo {}
            """
                .trimIndent()
                .render(java = true)
                .page("Bar")
                .data
                .content
                .data
                .description
                .data
                .primarySignature
        val signatureK =
            """
            public interface Foo {}
            public interface Bar : Foo {}
            """
                .trimIndent()
                .render(java = false)
                .page("Bar")
                .data
                .content
                .data
                .description
                .data
                .primarySignature

        assertThat(signatureJ.data.extends).isEmpty()
        assertThat(signatureK.data.extends).isEmpty()
        assertThat(signatureJ.data.implements.single().data.name).isEqualTo("Test.Foo")
        assertThat(signatureK.data.implements.single().data.name).isEqualTo("Foo")
        // Now route to DefaultClassSignatureTest.`Interfaces extend other interfaces`()
    }

    @Test
    fun `companion to inner static class 4x test`() {
        val moduleK =
            """
            |fun topLevelFun() = 5
            |object TopLevelObject {
            |   fun topObjectFun() = 5
            |}
            |class Container {
            |   companion object ContainerCompanion {
            |       fun companionFun() = 5
            |       const val hoistedField = 5
            |   }
            |}
        """
                .render()
        val sourceJ =
            javaHeader("Container") +
                """
            |public class Container {
            |   public static class ContainerCompanion {
            |      public static int companionFun() {}
            |   }
            |   public static final int hoistedField = 5
            |}
        """ +
                javaHeader("TestKt") +
                """
            |public static class TestKt {
            |   public static int topLevelFun() {}
            |}
        """ +
                javaHeader("TopLevelObject") +
                """
                |public class TopLevelObject {
                |   public static TopLevelObject INSTANCE = TopLevelObject()
                |   public int topObjectFun() {}
                |}
                """
                    .trimIndent()
        val moduleJ = sourceJ.renderWithoutLanguageHeader()

        for (module in listOf(moduleK, moduleJ)) {
            val testKt =
                if (displayLanguage == Language.JAVA || module == moduleJ) {
                    module.page("TestKt").data.content
                } else {
                    null
                }
            val packagePage =
                if (displayLanguage == Language.KOTLIN && module == moduleK) {
                    module.packagePage().data.content
                } else {
                    null
                }
            // This is top-level in Kotlin and in a Kt class in Java
            val topLevelFun =
                packagePage?.data?.topLevelFunctionsSummary?.item()
                    ?: testKt!!.data.publicFunctionsSummary.single()
            assertThat(topLevelFun.name()).isEqualTo("topLevelFun")
            javaOnly { assertThat(topLevelFun.modifiers()).contains("static") }
            if (packagePage != null) {
                assertThat(topLevelFun.urlSuffix()).isEqualTo("package-summary.html#topLevelFun()")
            } else {
                assertThat(topLevelFun.urlSuffix()).isEqualTo("TestKt.html#topLevelFun()")
            }

            // top-level object -> static inner class of synthetic Kt class
            val topObject = module.page("TopLevelObject").data.content
            val topObjectFun = topObject.data.publicFunctionsSummary.single()
            assertThat(topObjectFun.name()).isEqualTo("topObjectFun")
            // This function is not annotated with @JvmStatic, so it isn't static
            assertThat(topObjectFun.data.title.data.modifiers).doesNotContain("static")
            assertThat(topObjectFun.urlSuffix()).isEqualTo("TopLevelObject.html#topObjectFun()")

            if (testKt != null) {
                assertThat(testKt.data.nestedTypesSummary).isEmpty()
            } else
                assertThat(packagePage!!.data.objects.data.items.map { it.name() })
                    .contains("TopLevelObject")

            fun IterableSubject.containsPublicMaybeStatic() =
                if (module == moduleK) {
                    this.containsExactly("public", "static").inOrder()
                } else this.containsExactly("public").inOrder()

            val topObjectSignature = topObject.data.description.data.primarySignature
            if (displayLanguage == Language.KOTLIN && module == moduleK) {
                assertThat(topObjectSignature.data.type).isEqualTo("object")
            } else assertThat(topObjectSignature.data.type).isEqualTo("class")

            javaOnly {
                // top-level static classes don't exist in Java
                assertThat(topObject.modifiers()).containsPublicMaybeStatic()
                // top-level Kotlin objects become Java non-static classes with no constructor
                // but a static INSTANCE field that contains a static instance
                val instanceVal = topObject.data.publicPropertiesSummary.single()
                assertThat(instanceVal.name()).isEqualTo("INSTANCE")
                assertThat(instanceVal.modifiers()).contains("static")
            }

            // companion <-> inner static class
            val companionObject = module.page("ContainerCompanion").data.content
            val companionFun = companionObject.data.publicFunctionsSummary.single()
            assertThat(companionFun.name()).isEqualTo("companionFun")
            javaOnly { assertThat(companionFun.modifiers()).contains("static") }
            if (module == moduleJ || displayLanguage == Language.JAVA) {
                assertThat(companionFun.urlSuffix())
                    .isEqualTo("Container.ContainerCompanion.html#companionFun()")
            } // For Kotlin source and display only, the link is hoisted to the containing class
            else assertThat(companionFun.urlSuffix()).isEqualTo("Container.html#companionFun()")

            val companionObjectSignature = companionObject.data.description.data.primarySignature
            if (displayLanguage == Language.KOTLIN && module == moduleK) {
                assertThat(companionObjectSignature.data.type).isEqualTo("object")
            } else assertThat(companionObjectSignature.data.type).isEqualTo("class")

            javaOnly { assertThat(companionObject.modifiers()).containsPublicMaybeStatic() }

            // This is top-level in Kotlin and in a Kt class in Java
            val hoistedField = module.page("Container").data.content.data.constantsSummary.single()
            assertThat(hoistedField.name()).isEqualTo("hoistedField")
            javaOnly { assertThat(hoistedField.modifiers()).contains("static") }
            kotlinOnly { assertThat(hoistedField.modifiers()).contains("const") }
            assertThat(hoistedField.urlSuffix()).isEqualTo("Container.html#hoistedField()")
        }
    }

    @Test
    fun `Extensions of companion objects link are validly linked`() {
        val module =
            """
            |class Foo {
            |    companion object {}
            |}
            |fun Foo.Companion.extOfBoringCompanion() = Unit
            |
            |class Bar {
            |    companion object BarCompanion {}
            |}
            |fun Bar.BarCompanion.extOfInterestingCompanion() = Unit
        """
                .render()

        val fooSuffix = "(androidx.example.Foo.Companion).extOfBoringCompanion()"
        val barSuffix = "(androidx.example.Bar.BarCompanion).extOfInterestingCompanion()"

        // For Kotlin, the extensions are on the package summary page, for Java, they're on a
        // synthetic class page. Either way, the links should go to the same page as they appear.
        val (extensionFunctions, extensionPage) =
            when (displayLanguage) {
                Language.KOTLIN ->
                    Pair(
                        module.packagePage().data.content.data.extensionFunctionsSummary,
                        "package-summary.html",
                    )
                Language.JAVA ->
                    Pair(
                        module.page("TestKt").data.content.data.publicFunctionsSummary,
                        "TestKt.html",
                    )
            }
        assertThat(extensionFunctions[0].urlSuffix()).isEqualTo("$extensionPage#$fooSuffix")
        assertThat(extensionFunctions[1].urlSuffix()).isEqualTo("$extensionPage#$barSuffix")

        // In Kotlin, the extension will also show up on the companion page itself, if it exists.
        kotlinOnly {
            val barExtension =
                module.page("BarCompanion").data.content.data.extensionFunctionsSummary.single()
            val barExtensionPage = "Bar.BarCompanion.html"
            assertThat(barExtension.urlSuffix()).isEqualTo("$barExtensionPage#$barSuffix")
        }
    }

    @Test // Kotlin.enum.valueOf isn't in the descriptor tree, despite being callable: b/235992590
    fun `Enum valueOf return type is synthetic`() {
        val enumDModuleK =
            """
            |enum class Foo { BAR, BAZ }
        """
                .render()
        val enumDModuleJ =
            """
            |public enum Foo { BAR, BAZ }
        """
                .render(java = true)
        for (enumDModule in listOf(enumDModuleJ)) { // TODO: listOf(enumDModuleK, enumDModuleJ)
            // Test upstream behavior: only one enumJ.valueOf exists on the enum & it returns a Foo
            val dFunctions = enumDModule.explicitClasslike("Foo").functions
            val valueOfDFunctions = dFunctions.filter { it.name == "valueOf" }
            assertThat(valueOfDFunctions.size).isEqualTo(1)
            val valueOfDFunctionReturnType = valueOfDFunctions.single().type
            assertThat(valueOfDFunctionReturnType is JavaObject).isFalse()
            assertThat((valueOfDFunctionReturnType as GenericTypeConstructor).dri.classNames)
                .isEqualTo("Test.Foo")

            // Verify the final result in dackka is correct, and that valueOf is marked inherited.
            val enumClass = enumDModule.page("Foo").data.content
            val publicFuns = enumClass.data.publicFunctionsSummary
            val valueOfMethod = publicFuns.single { "valueOf" == it.name() }
            val valueOfReturnType = valueOfMethod.data.title.data.type
            assertThat(valueOfReturnType.name()).isEqualTo("Test.Foo")
        }
    }

    @Ignore // This does not generate a PagingRx class in either Kotlin or Java; TODO: fix
    @Test
    fun `JvmMultiFile does not break static attribution`() {
        val src =
            listOf(
                kotlinHeader(
                    name = "PagingRx",
                    fileAnnotations =
                        listOf("@file:JvmName(\"PagingRx\")", "@file:JvmMultifileClass"),
                ) +
                    """
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
                    fileAnnotations =
                        listOf("@file:JvmName(\"PagingRx\")", "@file:JvmMultifileClass"),
                ) +
                    """
                    |/**
                    | * Returns a [PagingData] containing only elements matching the given [predicate].
                    | */
                    |@JvmName("filter")
                    |@CheckResult
                    |fun <T : Any> PagingData<T>.filterAsync(
                    |    predicate: (T) -> Single<Boolean>
                    |): PagingData<T> = filter { predicate(it).await() }
                """,
            )
        val module = testWithRootPageNode(src)
    }

    @Ignore // b/232944038; go/dokka-upstream-bug/2620
    @Test
    fun `Upstream hashcode does not use sources`() {
        val moduleK =
            """
            |object Foo {
            |  fun bar() = Unit
            |  const val baz = "baz"
            |}
        """
                .render()
        val a = moduleK.children.first().children.first() as DObject
        val a2 = a.copy()
        val b = a.copy(sources = emptyMap())
        assertThat(a == a2).isTrue()
        assertThat(a.hashCode() == a2.hashCode()).isTrue()
        // These lines fail
        assertThat(a == b).isTrue()
        assertThat(a.hashCode() == b.hashCode()).isTrue()
    }

    @Ignore // Test does not work; problems mixing Java and Kotlin sources. b/282167724
    @Test
    fun `Can distinguish boxing of Java primitives used from Kotlin`() {
        val src =
            listOf(
                """/src/main/java/androidx/example/JavaParent.java
            |package androidx.example;
            |public class JavaParent {
            |   public int unboxed = 5;
            |   public Integer boxed = 5;
            |}
            |public class JavaChild extends JavaParent implements KotlinParent {}""",
                """/src/main/kotlin/androidx/example/KotlinParent.kt
            |package androidx.example
            |open interface KotlinParent""",
                """/src/main/kotlin/androidx/example/KotlinChild.kt
            |package androidx.example
            |class KotlinChild: androidx.example.JavaParent(), KotlinParent""",
            )

        val module = testWithRootPageNode(src.map { it.trimMargin() })
        val parent = module.page("Parent").data.content
        val child = module.page("Child").data.content
        assertThat(child.data.description.data.hierarchy.data.parents.single().data.name)
            .isEqualTo("Parent")
        val unboxedDirect = parent.propertySymbol("unboxed")!!.data.title.data.type
        val boxedDirect = parent.propertySymbol("boxed")!!.data.title.data.type
        val unboxedInherited = child.propertySymbol("unboxed")!!.data.title.data.type
        val boxedInherited = child.propertySymbol("boxed")!!.data.title.data.type
        val expectedBoxedType = if (displayLanguage == Language.KOTLIN) "Int" else "Integer"
        val expectedUnboxedType = if (displayLanguage == Language.KOTLIN) "Int" else "int"

        assertThat(unboxedDirect.data.type.data.name).isEqualTo(expectedUnboxedType)
        assertThat(boxedDirect.data.type.data.name).isEqualTo(expectedBoxedType)
        assertThat(unboxedInherited.data.type.data.name).isEqualTo(expectedUnboxedType)
        assertThat(boxedInherited.data.type.data.name).isEqualTo(expectedBoxedType)
    }

    @Test
    fun `Constants in companion object appear properly in page`() {
        // Note: this code is from gms dtdi
        val page =
            """
            |@Target(AnnotationTarget.TYPE)
            |annotation class ApiSurface {
            |  /** Possible values of ApiSurface. */
            |  companion object {
            |    /** The API surface was not requested or cannot be determined. */
            |    const val UNKNOWN = 0
            |    /** The API surface associated with [DtdiClient.createDevicePickerIntent]. */
            |    const val DISCOVERY = 1
            |    /**
            |     * The API surface associated with [DtdiClient.sendPayload] and
            |     * [DtdiClient.registerPayloadReceiver].
            |     */
            |    const val CONNECTIONS = 2
            |    /** The API surface associated with the higher level Sessions APIs. */
            |    const val SESSIONS = 3
            |    /** The API surface associated with waking up an application on the remote device. */
            |    const val WAKE_UP = 4
            |  }
            |}
        """
                .render()
                .page("ApiSurface")
        val constants = page.data.content.data.constantsSummary
        assertThat(constants.data.items.map { it.name() })
            .containsExactly("UNKNOWN", "DISCOVERY", "CONNECTIONS", "SESSIONS", "WAKE_UP")
    }

    @Test
    fun `Accessor for open val in open class has the correct modifiers in Java`() {
        val foo =
            """
            |open class Foo {
            |    open val foo = 0
            |}
        """
                .render()
                .page()
                .data
                .content
                .data

        javaOnly {
            val functions = foo.publicFunctionsSummary
            assertThat(functions).hasSize(1)
            val getter = functions.single()
            assertThat(getter.name()).isEqualTo("getFoo")
            // Ensure that the final-ness of the val (as in it can't be reassigned),
            // doesn't propagate to the getter, which can be overridden
            assertThat(getter.modifiers()).doesNotContain("final")
        }
    }

    @Test
    fun `Annotations on specific property targets appear correctly in the docs 4x test`() {
        val fooK =
            """
            |class Foo(
            |    @JvmField @field:Ann val annJvmField: Int,
            |    @field:Ann val annField: Int,
            |    @get:Ann val annGetter: Int,
            |    @param:Ann val annParam: Int
            |)
            |annotation class Ann
        """
                .render()
                .page()
                .data
                .content
                .data
        val fooJ =
            """
            |public class Foo {
            |    public Foo(int annJvmField, int annField, int annGetter, @Ann int annParam) {
            |        this.annJvmField = annJvmField;
            |        this.annField = annField;
            |        this.annGetter = annGetter;
            |        this.annParam = annParam;
            |    }
            |
            |    @Ann final public int annJvmField;
            |    @Ann final private int annField;
            |    final private int annGetter;
            |    final private int annParam;
            |
            |    @Ann public int getAnnGetter() { return annGetter; }
            |    public int getAnnField() { return annField; }
            |    public int getAnnParam() { return annParam; }
            |}
            |public @interface Ann {}
        """
                .render(java = true)
                .page()
                .data
                .content
                .data

        for (foo in listOf(fooK, fooJ)) {
            val annotationName =
                if (foo == fooK) {
                    "Ann"
                } else {
                    "Test.Ann"
                }

            val ctor = foo.publicConstructorsSummary.single()
            val constructorParams = ctor.data.description.data.signature.data.parameters
            assertThat(constructorParams.map { it.data.name })
                .containsExactly("annJvmField", "annField", "annGetter", "annParam")
            for (constructorParam in constructorParams) {
                val annotations = constructorParam.data.annotationComponents
                if (constructorParam.data.name == "annParam") {
                    assertThat(annotations.single().name).isEqualTo(annotationName)
                } else {
                    assertThat(annotations).isEmpty()
                }
            }

            val getters = foo.publicFunctionsSummary
            javaOnly {
                assertThat(getters.map { it.name() })
                    .containsExactly("getAnnField", "getAnnGetter", "getAnnParam")
                for (getter in getters) {
                    val annotations = getter.data.description.data.annotationComponents
                    if (getter.name() == "getAnnGetter") {
                        assertThat(annotations.single().name).isEqualTo(annotationName)
                    } else {
                        assertThat(annotations).isEmpty()
                    }
                }
            }
            kotlinOnly { assertThat(getters).isEmpty() }

            val properties = foo.publicPropertiesSummary
            // TODO (b/241259955): Java source private fields appear unless they start with "m"
            if (displayLanguage == Language.JAVA && foo == fooK) {
                assertThat(properties.map { it.name() }).containsExactly("annJvmField")
            } else {
                assertThat(properties.map { it.name() })
                    .containsExactly("annJvmField", "annField", "annGetter", "annParam")
            }
            for (property in properties) {
                val annotations = property.data.description.data.annotationComponents
                if (property.name() == "annJvmField" || property.name() == "annField") {
                    assertThat(annotations.single().name).isEqualTo(annotationName)
                } else {
                    assertThat(annotations).isEmpty()
                }
            }
        }
    }

    @Test
    fun `Getter for private static final Java field appears in the docs`() {
        val foo =
            """
            |public class Foo {
            |    private static final Foo privateWithGetter = new Foo();
            |    public static final int publicNoGetter = 0;
            |    private static final int privateNoGetter = 0;
            |
            |    private Foo() {}
            |
            |    public static Foo getPrivateWithGetter() {
            |        return privateWithGetter;
            |    }
            |}
        """
                .render(java = true)
                .page()
                .data
                .content
                .data

        // TODO (b/241259955): privateWithGetter is private, getter should exist for kotlin as well
        javaOnly {
            assertThat(foo.publicFunctionsSummary.map { it.name() })
                .containsExactly("getPrivateWithGetter")
        }

        // TODO (b/241259955): privateWithGetter is private
        assertThat(foo.publicPropertiesSummary.map { it.name() })
            .containsExactly("privateWithGetter")

        assertThat(foo.constantsSummary.map { it.name() }).containsExactly("publicNoGetter")
    }

    @Test
    fun `Can have two setters in Java`() {
        var classlike =
            """
            |public class HasTwoSettersAndNoPrivateBackingField {
            |   public String setFoo(String foo) { return "Setter One"; }
            |   public String setFoo(String foo, String anotherArg) { return "Setter Two"; }
            |}
        """
                .render(java = true)
                .page("HasTwoSettersAndNoPrivateBackingField")
                .data
                .content
        var setListenerMethods =
            classlike.data.publicFunctionsSummary.data.items.filter { it.name() == "setFoo" }
        assertThat(setListenerMethods.size).isEqualTo(2)

        classlike =
            """
            |public class HasTwoSettersAndPrivateBackingField {
            |   private String foo;
            |   public String setFoo(String foo) { return "Setter One"; }
            |   public String setFoo(String foo, String anotherArg) { return "Setter Two"; }
            |}
        """
                .render(java = true)
                .page("HasTwoSettersAndPrivateBackingField")
                .data
                .content
        setListenerMethods =
            classlike.data.publicFunctionsSummary.data.items.filter { it.name() == "setFoo" }
        assertThat(setListenerMethods.size).isEqualTo(2)
    }

    @Test
    fun `Data class with JvmField`() {
        // bar is a field/property, no accessors in Java
        val classlike =
            """
            |data class Foo (@JvmField val bar: String)
        """
                .render()
                .page("Foo")
                .data
                .content

        val ctor = classlike.data.publicConstructorsSummary.single()
        val ctorParam = ctor.data.description.data.signature.data.parameters.single()
        assertThat(ctorParam.data.name).isEqualTo("bar")

        val field = classlike.data.publicPropertiesSummary.single().data.description.data
        assertThat(field.signature.data.name.data.name).isEqualTo("bar")

        assertThat(classlike.data.publicFunctionsSummary).isEmpty()
    }

    private fun DModule.page(name: String = "Foo"): DevsitePage<Classlike> {
        val classlike = explicitClasslikes(name).single()
        return page { classlike }
    }

    private fun DModule.page(name: DModule.() -> DClasslike) = pages(listOf(name())).single()

    @JvmName("pagesForClasslikes")
    private fun DModule.pages(classlikes: List<DClasslike>): List<DevsitePage<Classlike>> =
        runBlocking {
            val converterHolder =
                ConverterHolder(this@ClasslikeDocumentableConverterTest, this@pages)
            classlikes.map { converterHolder.NonKmpClasslikeConverter(it).classlike() }
        }

    /** Note: does not return nested classlikes */
    private fun DModule.allPages() = pages(packages.single().classlikes)

    private fun DModule.pages(name: String = "Foo") = pages(explicitClasslikes(name))

    private fun DModule.pages(names: List<String>) = pages(names.map { explicitClasslike(it) })

    private fun DModule.companionFor(name: String = "Foo") =
        explicitClasslike("Foo").classlikes.single { it.name == "Companion" }

    private fun String.possiblyAsGetter() =
        if (displayLanguage == Language.KOTLIN) {
            this
        } else "get" + this.capitalize()

    private fun Classlike.propertySymbol(name: String = "foo") =
        (data.publicPropertiesSummary + data.protectedPropertiesSummary + data.inheritedProperties)
            .singleOrNull { it.name() == name }

    private fun ConstructorSummaryList.constructor() = data.items.item().data.description

    private fun <T : SymbolSignature> TypeSummaryItem<T>.urlSuffix() =
        data.description.data.signature.data.name.data.url.substringAfter("example/")

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Language.JAVA), arrayOf(Language.KOTLIN))
    }
}
