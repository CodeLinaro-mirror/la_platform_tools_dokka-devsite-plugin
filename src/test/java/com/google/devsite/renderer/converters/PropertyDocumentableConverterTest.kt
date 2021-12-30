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
import com.google.devsite.components.Link
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolDetail.SymbolKind.PROPERTY
import com.google.devsite.components.symbols.SymbolSignature
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.isAtNonNull
import com.google.devsite.renderer.converters.testing.isAtNullable
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.summary
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DModule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PropertyDocumentableConverterTest(
    private val language: Language
) : ConverterTestBase(language) {
    @Test
    fun `Property summary component creates return type link`() {
        val summary = """
            |class A
            |val foo: A
        """.render().summary()

        val returnType = summary.returnSummary().type

        assertThat(returnType.link().name).isEqualTo("A")
        assertPath(returnType.link().url, "androidx/example/A.html")
    }

    @Test
    fun `Property summary component creates signature with name`() {
        val summary = """
            |val iAmACoolProperty
        """.render().summary()

        val property = summary.summary()

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
            if (summary) (summary(propertyName).data.title as TypeSummary).data.type
            else detail(propertyName).data.returnType
        for (isSummary in listOf(true, false)) {
            for (whichProp in listOf("nonna", "nulla", "nonnaBefore", "nonnaClose", "platform")) {
                val typeJ = if (whichProp == "nonna") null else moduleJ.sOrD(isSummary, whichProp)
                val typeK = if (whichProp.length != 5) null else moduleK.sOrD(isSummary, whichProp)
                for (aType in listOfNotNull(typeJ, typeK)) {
                    assertThat(aType.nullable).isEqualTo(whichProp in "nulla, platform")
                    val annotations = aType.data.annotationComponents
                    assertThat(annotations.singleOrNull()?.name?.let {
                        it in NULLABILITY_ANNOTATION_NAMES })
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

        val property = summary.summary()

        assertThat(property.data.description.text()).isEqualTo("some_documentation")
    }

    @Test
    fun `Property summary component has correct relative link`() {
        val summary = """
            |val <T : Number> List<T>.foo
        """.render().summary()

        val property = summary.summary()
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

        assertThat(detail.data.symbolKind).isEqualTo(PROPERTY)
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
            if (summary) (summary(propertyName).data.description as SymbolSummary)
                .data.signature.data.annotationComponents
            else detail(propertyName).data.annotationComponents
        for (isSummary in listOf(true, false)) {
            val annotations = module.sOrDAnnotations(isSummary, "numbah")
            assertThat(annotations.single().name).isEqualTo("ExperimentalComposeApi")
        }
    }

    @Test
    fun `Constant modifier translates properly between Kotlin and Java`() {
        val modifiersJ = """
            public static final int FOO = 5
        """.render(java = true).detail("FOO").data.modifiers
        val modifiersK = """
            public const val FOO: Int = 5
        """.render().detail("FOO").data.modifiers
        for (modifiers in listOf(modifiersJ, modifiersK)) {
            javaOnly {
                assertThat(modifiers).isEqualTo(listOf("public", "static", "final"))
            }
            kotlinOnly {
                assertThat(modifiers).isEqualTo(listOf("const"))
            }
        }
    }

    private fun DModule.summary(
        name: String = "foo",
        hints: ModifierHints = ModifierHints(language)
    ): TwoPaneSummaryItem {
        val holder = runBlocking { DocumentablesHolder(this@summary, this) }
        val classGraph = runBlocking { holder.classGraph() }
        val docConverter = DocTagConverter(language, pathProvider(classGraph = classGraph), holder)
        val converter = PropertyDocumentableConverter(
            language,
            pathProvider(classGraph = classGraph),
            docConverter
        )
        return converter.summary(property(name)!!, hints)
    }

    private fun DModule.detail(
        name: String = "foo",
        hints: ModifierHints = ModifierHints(language)
    ): SymbolDetail {
        val holder = runBlocking { DocumentablesHolder(this@detail, this) }
        val classGraph = runBlocking { holder.classGraph() }
        val docConverter = DocTagConverter(language, pathProvider(classGraph = classGraph), holder)
        val converter = PropertyDocumentableConverter(
            language,
            pathProvider(classGraph = classGraph),
            docConverter
        )
        return converter.detail(property(name)!!, hints)
    }

    private fun DModule.signature(
        name: String = "foo",
        hints: ModifierHints = ModifierHints(language)
    ): SymbolSignature {
        val holder = runBlocking { DocumentablesHolder(this@signature, this) }
        val classGraph = runBlocking { holder.classGraph() }
        val docConverter = DocTagConverter(language, pathProvider(classGraph = classGraph), holder)
        val converter = PropertyDocumentableConverter(
            language,
            pathProvider(classGraph = classGraph),
            docConverter
        )
        return converter.summary(property(name)!!, hints).signature()
    }

    private fun TypeProjectionComponent.link(): Link.Params = data.type.data
    private fun TwoPaneSummaryItem.returnSummary(): TypeSummary.Params =
        (data.title as TypeSummary).data
    private fun TwoPaneSummaryItem.signature() = (data.description as SymbolSummary).data.signature

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
