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
import com.google.devsite.components.FunctionDetail
import com.google.devsite.components.FunctionSummary
import com.google.devsite.components.Link
import com.google.devsite.components.Parameter
import com.google.devsite.components.ParameterType
import com.google.devsite.components.TwoPaneSummaryItem
import com.google.devsite.components.TypeSummary
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.functionSummary
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class FunctionDocumentableConverterTest(
    private val language: Language
) : ConverterTestBase(language) {
    private val docConverter = DocTagConverter(language, pathProvider())

    @Test
    fun `Top level function summary component has correct default modifiers`() {
        val summary = """
            |fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("final")
    }

    @Test
    fun `Function summary component ignores public modifier`() {
        val summary = """
            |public fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("final")
    }

    @Test
    fun `Function summary component has suspend modifier`() {
        val summary = """
            |suspend fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("final", "suspend")
    }

    @Test
    fun `Function summary component has inline modifier`() {
        val summary = """
            |inline fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("final", "inline")
    }

    @Ignore // TODO(b/165112358): foo doesn't show up in the dokka model
    @Test
    fun `Function summary component in abstract class has protected modifier`() {
        val summary = """
            |abstract class Foo {
            |    protected open fun foo() = Unit
            |}
        """.render().summary(fromClass = true)

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("protected")
    }

    @Test
    fun `Function summary component in abstract class has abstract modifier`() {
        val summary = """
            |abstract class Foo {
            |    abstract fun foo()
            |}
        """.render().summary(fromClass = true)

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("abstract")
    }

    @Test
    fun `Function summary component in class has open modifiers`() {
        val summary = """
            |class Foo {
            |    open fun foo() = Unit
            |}
        """.render().summary(fromClass = true)

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("open")
    }

    @Test
    fun `Function summary component in interface has abstract modifiers`() {
        val summary = """
            |interface Foo {
            |    fun foo()
            |}
        """.render().summary(fromClass = true)

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("abstract")
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
    fun `Function summary component creates return type generics`() {
        val summary = """
            |fun foo(): Map<String, List<Int>>
        """.render().summary()

        val returnz = summary.returnSummary()
        val generics = returnz.type.data.generics.items(2)
        val nestedGenerics = generics.last().data.generics.item()

        assertThat(generics.first().link().name).isEqualTo("String")
        assertThat(generics.last().link().name).isEqualTo("List")
        assertThat(nestedGenerics.link().name).isEqualTo("Int")
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
        val signature = function.data.signature

        val param = when (language) {
            Language.JAVA -> {
                assertThat(signature.data.receiver).isNull()
                signature.data.parameters.item()
            }
            Language.KOTLIN -> {
                assertThat(signature.data.receiver).isNotNull()
                signature.data.receiver!!
            }
        }

        val type = param.data.primary

        assertNoLambdaStuff(param.data)
        assertThat(type.link().name).isEqualTo("String")
        when (language) {
            Language.JAVA -> assertThat(param.data.name).isEqualTo("receiver")
            Language.KOTLIN -> assertThat(param.data.name).isEmpty()
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

    @Ignore // TODO(b/165139177): figure out correct implementation
    @Test
    fun `Function summary component handles nullable types`() {
        val summary = """
            |fun Int?.foo(a: List<String?>?)
        """.render().summary()
    }

    @Test
    fun `Function summary component creates params with generics`() {
        val summary = """
            |fun foo(a: List<Int>)
        """.render().summary()

        val function = summary.functionSummary()
        val generic = function.param().data.primary.data.generics.item()

        assertThat(generic.link().name).isEqualTo("Int")
    }

    @Ignore // TODO(asaveau): figure out inline generics
    @Test
    fun `Function summary component creates params with inline generics and generic param`() {
        val summary = """
            |fun <T> foo(a: List<T>)
        """.render().summary()
    }

    @Ignore // TODO(asaveau): figure out inline generics
    @Test
    fun `Function summary component creates params with inline generics param`() {
        val summary = """
            |fun <T> foo(a: T)
        """.render().summary()
    }

    @Ignore // TODO(asaveau): figure out inline generics
    @Test
    fun `Function summary component creates params with inline generics receiver`() {
        val summary = """
            |fun <T> T.foo()
        """.render().summary()
    }

    @Ignore // TODO(asaveau): figure out inline generics
    @Test
    fun `Function summary component creates params with inline generics return type`() {
        val summary = """
            |fun <T> foo(): T
        """.render().summary()
    }

    @Test
    fun `Function summary component creates factory lambda param`() {
        val summary = """
            |fun foo(a: () -> Unit)
        """.render().summary()

        val function = summary.functionSummary()
        val param = function.param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.data
            assertThat(primary.type.data.name).isEqualTo("Function0")
            assertThat(primary.generics.item().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(param.isLambda).isTrue()
            assertThat(param.receiver).isNull()
            assertThat(param.lambdaModifiers).isEmpty()
            assertThat(param.lambdaParams).isEmpty()
            assertThat(param.primary.link().name).isEqualTo("Unit")
        }
    }

    @Test
    fun `Function summary component creates suspend lambda param`() {
        val summary = """
            |fun foo(a: suspend () -> Unit)
        """.render().summary()

        val function = summary.functionSummary()
        val param = function.param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.data
            assertThat(primary.type.data.name).isEqualTo("SuspendFunction0")
            assertThat(primary.generics.single().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(param.isLambda).isTrue()
            assertThat(param.receiver).isNull()
            assertThat(param.lambdaParams).isEmpty()
            assertThat(param.lambdaModifiers).containsExactly("suspend")
            assertThat(param.primary.link().name).isEqualTo("Unit")
        }
    }

    @Ignore // TODO(b/165709374): dokka doesn't understand suspending lambda receivers
    @Test
    fun `Function summary component creates suspend lambda param with receiver`() {
        val summary = """
            |fun foo(a: suspend Float.() -> Unit)
        """.render().summary()

        val function = summary.functionSummary()
        val param = function.param().data
        javaOnly {
            assertNoLambdaStuff(param)
            assertThat(param.primary.link().name).isEqualTo("")
        }
        kotlinOnly {
            assertThat(param.isLambda).isTrue()
            assertThat(param.receiver).isNotNull()
            assertThat(param.receiver!!.link().name).isEqualTo("Float")
            assertThat(param.lambdaModifiers).containsExactly("suspend")
            assertThat(param.lambdaParams).isEmpty()
        }
    }

    @Test
    fun `Function summary component creates suspend lambda param with params`() {
        val summary = """
            |fun foo(a: suspend (Float) -> Unit)
        """.render().summary()

        val function = summary.functionSummary()
        val param = function.param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.data
            assertThat(primary.type.data.name).isEqualTo("SuspendFunction1")
            assertThat(primary.generics.first().link().name).isEqualTo("Float")
            assertThat(primary.generics.last().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(param.isLambda).isTrue()
            assertThat(param.receiver).isNull()
            assertThat(param.lambdaModifiers).containsExactly("suspend")
            assertThat(param.lambdaParams).hasSize(1)
            assertThat(param.lambdaParams.single().link().name).isEqualTo("Float")
        }
    }

    @Test
    fun `Function summary component creates lambda param`() {
        val summary = """
            |fun foo(a: (String) -> Unit)
        """.render().summary()

        val function = summary.functionSummary()
        val param = function.param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.data
            assertThat(primary.type.data.name).isEqualTo("Function1")
            assertThat(primary.generics.first().link().name).isEqualTo("String")
            assertThat(primary.generics.last().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(param.isLambda).isTrue()
            assertThat(param.receiver).isNull()
            assertThat(param.lambdaModifiers).isEmpty()
            assertThat(param.lambdaParams).hasSize(1)
            assertThat(param.lambdaParams.single().link().name).isEqualTo("String")
        }
    }

    @Test
    fun `Function summary component creates lambda param with receiver`() {
        val summary = """
            |fun foo(a: Float.() -> Unit)
        """.render().summary()

        val function = summary.functionSummary()
        val param = function.param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.data
            assertThat(primary.type.data.name).isEqualTo("Function1")
            assertThat(primary.generics.first().link().name).isEqualTo("Float")
            assertThat(primary.generics.last().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(param.isLambda).isTrue()
            assertThat(param.receiver).isNotNull()
            assertThat(param.receiver!!.link().name).isEqualTo("Float")
            assertThat(param.lambdaModifiers).isEmpty()
            assertThat(param.lambdaParams).isEmpty()
        }
    }

    @Test
    fun `Function summary component creates lambda param with difficult generic combination`() {
        val summary = """
            |fun foo(block: Int.(Map<String, Int>, Double) -> Collection<Float>)
        """.render().summary()

        val function = summary.functionSummary()
        val param = function.param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.data
            assertThat(primary.type.data.name).isEqualTo("Function3")
            assertThat(primary.generics).hasSize(4)
            assertThat(primary.generics[0].link().name).isEqualTo("Int")
            assertThat(primary.generics[1].link().name).isEqualTo("Map")
            assertThat(primary.generics[2].link().name).isEqualTo("Double")
            assertThat(primary.generics[3].link().name).isEqualTo("Collection")

            val mapGenerics = primary.generics[1].data.generics.items(2)
            assertThat(mapGenerics.first().link().name).isEqualTo("String")
            assertThat(mapGenerics.last().link().name).isEqualTo("Int")

            val collectionGeneric = primary.generics[3].data.generics.item()
            assertThat(collectionGeneric.link().name).isEqualTo("Float")
        }
        kotlinOnly {
            assertThat(param.isLambda).isTrue()
            assertThat(param.receiver).isNotNull()
            assertThat(param.receiver!!.link().name).isEqualTo("Int")
            assertThat(param.lambdaModifiers).isEmpty()
            assertThat(param.lambdaParams).hasSize(2)
            assertThat(param.lambdaParams.first().link().name).isEqualTo("Map")
            assertThat(param.lambdaParams.last().link().name).isEqualTo("Double")
            assertThat(param.primary.link().name).isEqualTo("Collection")

            val mapGenerics = param.lambdaParams.first().data.generics.items(2)
            assertThat(mapGenerics.first().link().name).isEqualTo("String")
            assertThat(mapGenerics.last().link().name).isEqualTo("Int")

            val collectionGeneric = param.primary.data.generics.item()
            assertThat(collectionGeneric.link().name).isEqualTo("Float")
        }
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
    fun `Function detail component has correct name`() {
        val detail = """
            |fun foo()
        """.render().detail()

        assertThat(detail.data.name).isEqualTo("foo")
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

    private fun RootPageNode.summary(fromClass: Boolean = false): TwoPaneSummaryItem {
        val converter = FunctionDocumentableConverter(language, pathProvider(), docConverter)
        return converter.summary(function(fromClass))
    }

    private fun RootPageNode.detail(fromClass: Boolean = false): FunctionDetail {
        val converter = FunctionDocumentableConverter(language, pathProvider(), docConverter)
        return converter.detail(function(fromClass))
    }

    private fun RootPageNode.function(fromClass: Boolean): DFunction {
        val packageDoc = children
            .filterIsInstance<PackagePageNode>().single()
            .documentable as DPackage

        return if (fromClass) {
            packageDoc.classlikes.single().functions.single { it.name == "foo" }
        } else {
            packageDoc.functions.single()
        }
    }

    private fun ParameterType.link(): Link.Params = data.type.data
    private fun FunctionSummary.param(): Parameter = data.signature.data.parameters.item()
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
