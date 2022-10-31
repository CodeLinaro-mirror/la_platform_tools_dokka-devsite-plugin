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
import com.google.devsite.TypeSummaryItem
import com.google.devsite.components.Link
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.isAtNonNull
import com.google.devsite.renderer.converters.testing.isAtNullable
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.testing.ConverterTestBase
import kotlinx.html.stream.createHTML
import kotlinx.html.tr
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DProperty
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PropertyDocumentableConverterTest(
    private val displayLanguage: Language
) : ConverterTestBase(displayLanguage) {

    override var defaultHints = ModifierHints(
        displayLanguage,
        isSummary = false,
        type = DProperty::class.java,
        containingType = DClass::class.java,
        isFromJava = false // There's no great way to do this. Currently only affects `const` inject
    )

    @Test
    fun `Property summary component creates return type link`() {
        val summary = """
            |class A
            |val foo: A
        """.render().summary()

        val returnType = summary.data.title.data.type

        assertThat(returnType.link().name).isEqualTo("A")
        assertPath(returnType.link().url, "androidx/example/A.html")
    }

    @Test
    fun `Property summary component creates signature with name`() {
        val summary = """
            |val iAmACoolProperty
        """.render().summary()

        val property = summary.data.description

        assertThat(property.name()).isEqualTo("iAmACoolProperty")
    }

    @Test
    fun `Property summary and detail include nullability information in 4x Kotlin and Java`() {
        val moduleJ = """
        |@Nullable
        |public String nulla;
        |public String platform;
        |@NonNull
        |public String nonnaBefore;
        |public @NonNull String nonnaClose;
        """.render(java = true)
        val moduleK = """
        |val nulla: String? = null // nullability annotations in Kotlin are errors.
        |val nonna: String = "foo"
        """.render()
        fun DModule.sOrD(summary: Boolean, propertyName: String): TypeProjectionComponent =
            if (summary) summary(propertyName).data.title.data.type
            else detail(propertyName).data.returnType
        for (isSummary in listOf(true, false)) {
            for (whichProp in listOf("nonna", "nulla", "nonnaBefore", "nonnaClose", "platform")) {
                val typeJ = if (whichProp == "nonna") null else moduleJ.sOrD(isSummary, whichProp)
                val typeK = if (whichProp.length != 5) null else moduleK.sOrD(isSummary, whichProp)
                for (aType in listOfNotNull(typeJ, typeK)) {
                    assertThat(aType.nullable).isEqualTo(whichProp in "nulla, platform")
                    val annotations = aType.annotations
                    assertThat(
                        annotations.singleOrNull()?.name?.let {
                            it in NULLABILITY_ANNOTATION_NAMES
                        }
                    )
                    kotlinOnly {
                        // We've decided to hide all nullability annotations as-kotlin even if they
                        // are present in Kotlin source, because they should not be in kotlin source
                        assertThat(annotations).isEmpty()
                    }
                    javaOnly {
                        assertThat(annotations.any { it.isAtNonNull })
                            .isEqualTo("nonna" in whichProp)
                        assertThat(annotations.any { it.isAtNullable })
                            .isEqualTo(whichProp == "nulla" && aType == typeJ)
                    }
                }
            }
        }
    }

    @Test
    fun `Property summary component contains @property documentation`() {
        val summary = """
            |/** @property foo some_documentation */
            |val foo
        """.render().summary()

        val property = summary.data.description

        assertThat(property.data.description.text()).isEqualTo("some_documentation")
    }

    @Test
    fun `Property summary component has correct relative link`() {
        val summary = """
            |val <T : Number> List<T>.foo
        """.render().summary()

        val property = summary.data.description
        val signature = property.data.signature

        assertPath(
            signature.data.name.data.url,
            "androidx/example/package-summary.html#(kotlin.collections.List).foo()"
        )
    }

    @Test
    fun `Property detail component has correct name`() {
        val detail = """
            |val foo
        """.render().detail()

        assertThat(detail.data.name).isEqualTo("foo")
    }

    @Test
    fun `Property detail component is marked as property type`() {
        val detail = """
            |val foo
        """.render().detail()

        assertThat(detail.data.symbolKind).isEqualTo(SymbolDetail.SymbolKind.READ_ONLY_PROPERTY)
    }

    @Test
    fun `Property detail component creates return type link`() {
        val detail = """
            |class A
            |val foo: A
        """.render().detail()

        val returnType = detail.data.returnType

        assertThat(returnType.link().name).isEqualTo("A")
        assertPath(returnType.link().url, "androidx/example/A.html")
    }

    @Test
    fun `Property detail component has annotations`() {
        val detail = """
            |annotation class Hello
            |@Hello val foo: String
        """.render().detail()

        assertThat(detail.data.annotationComponents).isNotEmpty()
    }

    @Test
    fun `Overall nullability of array types is handled properly in 4x Java and Kotlin`() {
        val moduleK = """
            |val foo: IntArray?
            |val oof: IntArray
            |val bar: Array<String>?
            |val rab: Array<String>
        """.render()
        val moduleJ = """
            |public int[] foo;
            |public @NonNull int[] oof;
            |public String[] bar;
            |public @NonNull String[] rab;
        """.render(java = true)
        for (module in listOf(moduleJ, moduleK)) {
            val fooType = module.detail("foo").data.returnType
            val oofType = module.detail("oof").data.returnType
            val barType = module.detail("bar").data.returnType
            val rabType = module.detail("rab").data.returnType
            assertThat(fooType.data.annotationComponents).isEmpty()
            assertThat(fooType.nullable).isTrue()
            assertThat(barType.data.annotationComponents).isEmpty()
            assertThat(barType.nullable).isTrue()

            assertThat(oofType.nullable).isFalse()
            assertThat(rabType.nullable).isFalse()
            javaOnly {
                assertThat(oofType.data.annotationComponents.single().isAtNonNull).isTrue()
                assertThat(rabType.data.annotationComponents.single().isAtNonNull).isTrue()
            }
            kotlinOnly {
                assertThat(oofType.data.annotationComponents).isEmpty()
                assertThat(rabType.data.annotationComponents).isEmpty()
            }
        }
    }

    @Test
    fun `Property detail component has correct anchors`() {
        val detail = """
            |val <T : Number> List<T>.foo
        """.render().detail()

        assertThat(detail.data.anchors).containsExactly(
            "(kotlin.collections.List).foo()",
            "(kotlin.collections.List).getFoo()",
            "(kotlin.collections.List).setFoo()",
            "-kotlin.collections.List-.getFoo--",
            "-kotlin.collections.List-.setFoo--"
        )
    }

    @Test
    fun `Property summary and detail for Kotlin top-level property include annotations`() {
        val module = """
            |annotation class ExperimentalComposeApi
            |@ExperimentalComposeApi
            |val String.numbah: Int = 5
        """.render()
        fun DModule.sOrDAnnotations(summary: Boolean, propertyName: String) =
            if (summary)
                summary(propertyName).data.description.data.annotationComponents
            else detail(propertyName).data.annotationComponents
        for (isSummary in listOf(true, false)) {
            val annotations = module.sOrDAnnotations(isSummary, "numbah")
            assertThat(annotations.single().name).isEqualTo("ExperimentalComposeApi")
        }
    }

    @Test
    fun `Constant modifier translates properly between Kotlin and Java`() {
        val detailJ = """
            public static final Integer FOO = 5
        """.render(java = true)
            .detail("FOO", hints = defaultHints.copy(isFromJava = true))
        val detailK = """
            public const val FOO: Int = 5
        """.render().detail("FOO")
        val detailKNo = """
            |object aarg {
            |    @JvmStatic public val FOO: Int = 5
            |}
        """.render().detail("FOO")
        for (detail in listOf(detailJ, detailK, detailKNo)) {
            javaOnly {
                assertThat(detail.data.modifiers).isEqualTo(listOf("public", "static", "final"))
            }
            kotlinOnly {
                val expected = if (detail == detailKNo) emptyList() else listOf("const")
                assertThat(detail.data.modifiers).isEqualTo(expected)
            }
        }
    }

    @Test
    fun `Constant properties have values`() {
        val intConstantJ = """
            public static final int FOO = 5;
        """.render(java = true).signature("FOO").data
        assertThat(intConstantJ.constantValue).isNotNull()
        assertThat(intConstantJ.constantValue).isEqualTo("5")

        val intConstantK = """
            public const val FOO: Int = 5
        """.render().signature("FOO").data
        assertThat(intConstantK.constantValue).isNotNull()
        assertThat(intConstantK.constantValue).isEqualTo("5")

        // TODO(b/237691687): currently only primitive value constants can be displayed
        // dependent on go/dokka-upstream-bug/2008
        /*
        val stringConstantJ = """
            public static final int BAR = "Hi";
        """.render(java = true).signature("BAR").data
        assertThat(stringConstantJ.constantValue).isNotNull()
        assertThat(stringConstantJ.constantValue).isEqualTo("Hi")

        val stringConstantK = """
            public const val BAR: String = "Hi"
        """.render().signature("BAR").data
        assertThat(stringConstantK.constantValue).isNotNull()
        assertThat(stringConstantK.constantValue).isEqualTo("Hi")
        */
    }

    @Test
    fun `External link resolution through ExternalDocumentationLinks doesn't give Oracle`() {
        val propertySummary = """
            public static Object foo = null
        """.render(java = true).summary()
        val html = createHTML().tr {
            propertySummary.render(this)
        }.trim()
        val htmlParts = html.split("\\s+".toRegex())
        val link = htmlParts.first { it.startsWith("href") }.split(">")[0]
        javaOnly {
            assertThat(link).isEqualTo(
                "href=\"https://developer.android.com/reference/java/lang/Object.html\""
            )
        }
        kotlinOnly {
            assertThat(link).isEqualTo(
                "href=\"https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html\""
            )
        }
    }

    /** Note: the desired behavior on `var protected set` is unclear */
    @Test
    fun `Val-Var verification`() {
        val module = """
            |public class Test {
            |   public val fullClassVal = 0
            |   public var fullClassVar = 1
            |   public var internalSetClassVar: Int = 2
            |       internal set
            |   public var protectedSetClassVar: Int = 3
            |       protected set
            |}
            |public val fullTopVal = 0
            |public var fullTopVar = 1
            |public var internalSetTopVar: Int = 2
            |    internal set
            |
        """.render()
        fun kindOf(name: String) = module.detail(name).data.symbolKind
        val fullClassVal = kindOf("fullClassVal")
        val fullClassVar = kindOf("fullClassVar")
        val internalSetClassVar = kindOf("internalSetClassVar")
        val protectedSetClassVar = kindOf("protectedSetClassVar")
        val fullTopVal = kindOf("fullTopVal")
        val fullTopVar = kindOf("fullTopVar")
        val internalSetTopVar = kindOf("internalSetTopVar")

        val vals = listOf(
            fullClassVal, internalSetClassVar, protectedSetClassVar,
            fullTopVal, internalSetTopVar
        )
        val vars = listOf(fullClassVar, fullTopVar)

        for (shouldBeAVal in vals)
            assertThat(shouldBeAVal == SymbolDetail.SymbolKind.READ_ONLY_PROPERTY)
        for (shouldBeAVar in vars)
            assertThat(shouldBeAVar == SymbolDetail.SymbolKind.PROPERTY)
    }

    private fun DModule.summary(
        name: String = "foo",
        hints: ModifierHints = defaultHints
    ): TypeSummaryItem<PropertySignature> {
        val (holder, pathProvider) = holderAndProvider(this)
        val docConverter = DocTagConverter(displayLanguage, pathProvider, holder)
        val converter = PropertyDocumentableConverter(
            displayLanguage,
            pathProvider,
            docConverter
        )
        return converter.summary(property(name)!!, hints)
    }

    private fun DModule.detail(
        name: String = "foo",
        hints: ModifierHints = defaultHints
    ): SymbolDetail<PropertySignature> {
        val (holder, pathProvider) = holderAndProvider(this)
        val docConverter = DocTagConverter(displayLanguage, pathProvider, holder)
        val converter = PropertyDocumentableConverter(
            displayLanguage,
            pathProvider,
            docConverter
        )
        return converter.detail(property(name)!!, hints)
    }

    private fun DModule.signature(
        name: String = "foo",
        hints: ModifierHints = defaultHints
    ): PropertySignature {
        val (holder, pathProvider) = holderAndProvider(this)
        val docConverter = DocTagConverter(displayLanguage, pathProvider, holder)
        val converter = PropertyDocumentableConverter(
            displayLanguage,
            pathProvider,
            docConverter
        )
        return converter.summary(property(name)!!, hints).data.description.data.signature
    }

    private fun TypeProjectionComponent.link(): Link.Params = data.type.data

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
