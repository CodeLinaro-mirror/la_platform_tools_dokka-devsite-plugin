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
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.Parameter
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolDetail.SymbolType
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.SingleColumnSummaryItem
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.asType
import com.google.devsite.renderer.converters.testing.functionSummary
import com.google.devsite.renderer.converters.testing.generics
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.projectionName
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DModule
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class FunctionDocumentableConverterTest(
    private val language: Language
) : ConverterTestBase(language) {

    @Test
    fun `Top level function summary component has correct default modifiers`() {
        val summary = """
            |fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).isEmpty() }
    }

    @Test
    fun `Function summary component ignores public modifier`() {
        val summary = """
            |public fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).isEmpty() }
        javaOnly { assertThat(returnz.modifiers).containsExactly("final") }
    }

    @Test
    fun `Function summary component has suspend modifier`() {
        val summary = """
            |suspend fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).containsExactly("suspend") }
    }

    @Test
    fun `Function summary component has inline modifier`() {
        val summary = """
            |inline fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).containsExactly("inline") }
    }

    @Ignore // TODO(b/165112358): foo doesn't show up in the dokka model
    @Test
    fun `Function summary component in abstract class has protected modifier`() {
        val summary = """
            |abstract class Foo {
            |    protected open fun foo() = Unit
            |}
        """.render().summary()

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("protected")
    }

    @Test
    fun `Function summary component in abstract class has abstract modifier`() {
        val summary = """
            |abstract class Foo {
            |    abstract fun foo()
            |}
        """.render().summary()

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("abstract")
    }

    @Test
    fun `Function summary component in class has open modifiers`() {
        val summary = """
            |class Foo {
            |    open fun foo() = Unit
            |}
        """.render().summary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).containsExactly("open") }
        javaOnly { assertThat(returnz.modifiers).isEmpty() }
    }

    @Test
    fun `Function summary component in interface has abstract modifiers`() {
        val summary = """
            |interface Foo {
            |    fun foo()
            |}
        """.render().summary(ModifierHints(language, isInterface = true))

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).isEmpty() }
        javaOnly { assertThat(returnz.modifiers).containsExactly("abstract") }
    }

    @Test
    fun `Function summary component creates return type link`() {
        val summary = """
            |class A
            |fun foo(): A
        """.render().summary()

        val returnType = summary.returnSummary().type

        assertThat(returnType.link().name).isEqualTo("A")
        assertPath(returnType.link().url, "androidx/example/A.html")
    }

    @Test
    fun `Function summary component creates void return type link`() {
        val summary = """
            |fun foo() = Unit
        """.render().summary()

        val returnType = summary.returnSummary().type

        javaOnly {
            assertThat(returnType.link().name).isEqualTo("void")
            assertThat(returnType.link().url).isEmpty()
        }
        kotlinOnly {
            assertThat(returnType.link().name).isEqualTo("Unit")
            assertPath(returnType.link().url, "kotlin/Unit.html")
        }
    }

    @Test
    fun `Function summary component handles constructors`() {
        val summary = """
            |class MyClass
        """.render().summaryForConstructor()

        val constructor = summary.data.description as SymbolSummary

        assertThat(constructor.name()).isEqualTo("MyClass")
    }

    @Test
    fun `Function summary component creates return type generics`() {
        val summary = """
            |fun foo(): Map<String, List<Long>>
        """.render().summary()

        val returnz = summary.returnSummary()
        val generics = returnz.type.asType().data.generics.items(2)
        val nestedGenerics = generics.last().asType().data.generics.item()

        assertThat(generics.first().link().name).isEqualTo("String")
        assertThat(generics.last().link().name).isEqualTo("List")
        assertThat(nestedGenerics.link().name).isEqualTo("Long")
    }

    @Test
    fun `Function summary component creates signature with name`() {
        val summary = """
            |fun iAmACoolFunction()
        """.render().summary()

        val function = summary.functionSummary()

        assertThat(function.name()).isEqualTo("iAmACoolFunction")
    }

    @Test
    fun `Function summary component creates extension receiver`() {
        val summary = """
            |fun String.foo()
        """.render().summary()

        val function = summary.functionSummary()
        val signature = function.signature()

        javaOnly {
            assertThat(signature.receiver).isNull()
            val param = signature.parameters.item()
            assertNoLambdaStuff(param.data)

            val type = param.data.primary
            assertThat(type.link().name).isEqualTo("String")
            assertThat(type.link().url).contains("java")
            assertThat(param.data.name).isEqualTo("receiver")
        }

        kotlinOnly {
            assertThat(signature.receiver).isNotNull()
            val param = signature.receiver!!
            assertNoLambdaStuff(param.data)

            val type = param.data.primary
            assertThat(type.link().name).isEqualTo("String")
            assertThat(type.link().url).contains("kotlin")
            assertThat(param.data.name).isEmpty()
        }
    }

    @Test
    fun `Function summary component creates extension receiver for proper type`() {
        val summary = """
            |fun Any.foo()
        """.render().summary()

        val function = summary.functionSummary()
        val signature = function.signature()

        javaOnly {
            assertThat(signature.receiver).isNull()
            val param = signature.parameters.item()
            assertNoLambdaStuff(param.data)

            val type = param.data.primary
            assertThat(type.link().name).isEqualTo("Object")
            assertThat(type.link().url).contains("java")
            assertThat(param.data.name).isEqualTo("receiver")
        }

        kotlinOnly {
            assertThat(signature.receiver).isNotNull()
            val param = signature.receiver!!
            assertNoLambdaStuff(param.data)

            val type = param.data.primary
            assertThat(type.link().name).isEqualTo("Any")
            assertThat(type.link().url).contains("kotlin")
            assertThat(param.data.name).isEmpty()
        }
    }

    @Test
    fun `Function summary component creates params`() {
        val summary = """
            |fun foo(a: String)
        """.render().summary()

        val function = summary.functionSummary()
        val param = function.param()
        val paramType = param.data.primary

        assertNoLambdaStuff(param.data)
        assertThat(param.data.name).isEqualTo("a")
        assertThat(paramType.link().name).isEqualTo("String")
    }

    @Ignore // TODO(b/168270546): figure out inline generics
    @Test
    fun `Function summary component creates inline generics`() {
        val summary = """
            |fun <T> foo() = Unit
        """.render().summary()
    }

    @Ignore // TODO(b/168270546): figure out inline generics
    @Test
    fun `Function summary component creates inline generics extending class`() {
        val summary = """
            |fun <T: Number> foo() = Unit
        """.render().summary()
    }

    @Ignore // TODO(b/168270546): figure out inline generics
    @Test
    fun `Function summary component creates multiple inline generics`() {
        val summary = """
            |fun <T, U, V> foo() = Unit
        """.render().summary()
    }

    @Test
    fun `Function signature component creates multiple inline generics`() {
        val inlineGenerics = """
            |fun <T: Number, U: List<String>, V: T> foo() = Unit
        """.render().summary().functionSummary().signature().typeParameters

        assertThat(inlineGenerics.map { it.data.name }).isEqualTo(listOf("T", "U", "V"))
        assertThat(inlineGenerics[0].projectionName()).isEqualTo("Number")
        assertThat(inlineGenerics[1].projectionName()).isEqualTo("List")
        val generics = (inlineGenerics[1].data.projections.single() as Parameter).generics()
        assertThat((generics.single() as Parameter).link().name).isEqualTo("String")
        assertThat(inlineGenerics[2].projectionName()).isEqualTo("T")
    }

    @Test
    fun `Function summary component has correct relative link`() {
        val summary = """
            |fun <T : Number> List<String>.foo(t: T, a: Map<String, Int>, block: String.(Float) -> Double) = Unit
        """.render().summary()

        val function = summary.functionSummary()
        val signature = function.data.signature

        assertPath(
            signature.data.name.data.url,
            "androidx/example/package-summary.html#" +
                "foo(kotlin.collections.List,kotlin.Number,kotlin.collections.Map,kotlin.Function2)"
        )
    }

    @Test
    fun `Function summary component understands Java primitives`() {
        val summary = """
            |public void foo(
            |    boolean a, int b, double c, float d, short e, long f, char g, byte h) {}
        """.render(java = true).summary()

        val function = summary.functionSummary()
        val returnType = summary.returnSummary().type.link()
        val signature = function.signature()

        javaOnly {
            assertThat(returnType.name).isEqualTo("void")

            val expected =
                listOf("boolean", "int", "double", "float", "short", "long", "char", "byte")
            for ((i, param) in signature.parameters.withIndex()) {
                assertThat(param.data.primary.link().name).isEqualTo(expected[i])
            }
        }
        kotlinOnly {
            assertThat(returnType.name).isEqualTo("Unit")

            val expected =
                listOf("Boolean", "Int", "Double", "Float", "Short", "Long", "Char", "Byte")
            for ((i, param) in signature.parameters.withIndex()) {
                assertThat(param.data.primary.link().name).isEqualTo(expected[i])
            }
        }
    }

    @Test
    fun `Function summary component understands Java object`() {
        val summary = """
            |public Object foo() {}
        """.render(java = true).summary()

        val returnType = summary.returnSummary().type.link()

        javaOnly { assertThat(returnType.name).isEqualTo("Object") }
        kotlinOnly { assertThat(returnType.name).isEqualTo("Any") }
    }

    @Test
    fun `Function detail component has correct name`() {
        val detail = """
            |fun foo()
        """.render().detail()

        assertThat(detail.data.name).isEqualTo("foo")
    }

    @Test
    fun `Top level function detail component has correct default modifiers`() {
        val detail = """
            |fun foo() = Unit
        """.render().detail()

        javaOnly { assertThat(detail.data.modifiers).containsExactly("public", "final") }
        kotlinOnly { assertThat(detail.data.modifiers).isEmpty() }
    }

    @Test
    fun `Top level function detail component has annotations`() {
        val detail = """
            |annotation class Hello
            |@Hello fun foo() = Unit
        """.render().detail()

        assertThat(detail.data.annotations).isNotEmpty()
    }

    @Test
    fun `Function detail has nullability information in 4x Kotlin and Java`() {
        val detailK = """
            |fun foo(): Int? {}
        """.render().detail()
        val detailJ = """
            |public @Nullable Integer foo() {}
        """.render(java = true).detail()
        val detailJ2 = """
            |@Nullable
            |public Integer foo() {}
        """.render(java = true).detail()

        for (detail in listOf(detailK, detailJ, detailJ2)) {
            javaOnly { assertThat(detail.data.annotations).isNotEmpty() }
            kotlinOnly {
                assertThat(detail.data.annotations).isEmpty()
                assertThat(detail.data.returnType.nullable).isTrue()
            }
        }
    }

    @Test
    fun `Function summary has nullability information in 4x Kotlin and Java`() {
        val summaryK = """
            |fun foo(): Int? {}
        """.render().summary().returnSummary().type
        val summaryJ = """
            |public @Nullable Integer foo() {}
        """.render(java = true).summary().returnSummary().type
        val summaryJ2 = """
            |@Nullable
            |public Integer foo() {}
        """.render(java = true).summary().returnSummary().type

        for (summary in listOf(summaryK, summaryJ, summaryJ2)) {
            javaOnly { assertThat(summary.data.annotations).isNotEmpty() }
            kotlinOnly {
                assertThat(summary.data.annotations).isEmpty()
                assertThat(summary.nullable).isTrue()
            }
        }
    }

    @Test
    fun `Function detail component is marked as function type`() {
        val detail = """
            |fun foo()
        """.render().detail()

        assertThat(detail.data.symbolType).isEqualTo(SymbolType.FUNCTION)
    }

    @Test
    fun `Function detail component creates void return type link`() {
        val detail = """
            |fun foo() = Unit
        """.render().detail()

        val returnType = detail.data.returnType

        javaOnly {
            assertThat(returnType.link().name).isEqualTo("void")
            assertThat(returnType.link().url).isEmpty()
        }
        kotlinOnly {
            assertThat(returnType.link().name).isEqualTo("Unit")
            assertPath(returnType.link().url, "kotlin/Unit.html")
        }
    }

    @Test
    fun `Function detail component has correct anchors`() {
        val detail = """
            |fun <T : Number> List<String>.foo(t: T, a: Map<String, Int>, block: String.(Float) -> Double) = Unit
        """.render().detail()

        assertThat(detail.data.anchors).containsExactly(
            "foo(kotlin.collections.List,kotlin.Number,kotlin.collections.Map,kotlin.Function2)",
            "foo(kotlin.collections.List, kotlin.Number, kotlin.collections.Map, kotlin.Function2)",
            "foo-kotlin.collections.List-kotlin.Number-kotlin.collections.Map-kotlin.Function2-"
        )
    }

    private fun assertNoLambdaStuff(data: Parameter.Params) {
        assertThat(data.isLambda).isFalse()
        assertThat(data.receiver).isNull()
        assertThat(data.lambdaParams).isEmpty()
        assertThat(data.lambdaModifiers).isEmpty()
    }

    private fun DModule.summary(
        hints: ModifierHints = ModifierHints(language)
    ): TwoPaneSummaryItem {
        val holder = runBlocking { DocumentablesHolder(this@summary, this) }
        val docConverter = DocTagConverter(language, pathProvider(), holder)
        val converter = FunctionDocumentableConverter(language, pathProvider(), docConverter)
        return converter.summary(function()!!, hints.copy(isSummary = true))
    }

    private fun DModule.summaryForConstructor(): SingleColumnSummaryItem {
        val holder = runBlocking { DocumentablesHolder(this@summaryForConstructor, this) }
        val docConverter = DocTagConverter(language, pathProvider(), holder)
        val converter = FunctionDocumentableConverter(language, pathProvider(), docConverter)
        return converter.summaryForConstructor(cstructor())
    }

    private fun DModule.detail(
        hints: ModifierHints = ModifierHints(language)
    ): SymbolDetail {
        val holder = runBlocking { DocumentablesHolder(this@detail, this) }
        val docConverter = DocTagConverter(language, pathProvider(), holder)
        val converter = FunctionDocumentableConverter(language, pathProvider(), docConverter)
        return converter.detail(function()!!, hints)
    }

    private fun DModule.cstructor() = (classlike() as DClass).constructors.single()

    private fun Parameter.link(): Link.Params = data.primary.link()

    private fun SymbolSummary.signature(): FunctionSignature.Params =
        (data.signature as FunctionSignature).data

    private fun SymbolSummary.param(): Parameter = signature().parameters.item()

    private fun TwoPaneSummaryItem.returnSummary(): TypeSummary.Params =
        (data.title as TypeSummary).data

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
