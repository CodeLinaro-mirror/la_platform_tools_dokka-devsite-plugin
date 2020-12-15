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
import com.google.devsite.components.symbols.Parameter
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.asType
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DParameter
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ParameterDocumentableConverterTest(
    private val language: Language
) : ConverterTestBase(language) {
    @Test
    fun `Parameter has correct type`() {
        val param = """
            |fun foo(a: String) = Unit
        """.render().param()

        val paramType = param.data.primary.asType()

        assertThat(paramType.link().name).isEqualTo("String")

        javaOnly {
            assertThat(paramType.link().url).isEqualTo("/reference/java/lang/String.html")
        }

        kotlinOnly {
            assertThat(paramType.link().url).isEqualTo("/reference/kotlin/kotlin/String.html")
        }
    }

    @Test
    fun `Parameter has correct type for Any`() {
        val param = """
            |fun foo(a: Any) = Unit
        """.render().param()

        val paramType = param.data.primary.asType()

        javaOnly {
            assertThat(paramType.link().name).isEqualTo("Object")
            assertThat(paramType.link().url).isEqualTo("/reference/java/lang/Object.html")
        }

        kotlinOnly {
            assertThat(paramType.link().name).isEqualTo("Any")
            assertThat(paramType.link().url).isEqualTo("/reference/kotlin/kotlin/Any.html")
        }
    }

    @Test
    fun `Parameter understands annotation`() {
        val param = """
            |annotation class Hello
            |fun foo(@Hello a: List<Int>)
        """.render().param()

        assertThat(param.data.annotations).isNotEmpty()
    }

    @Test
    fun `Parameter understands generics`() {
        val param = """
            |fun foo(a: List<String>)
        """.render().param()

        val paramType = param.data.primary.asType()
        val generic = paramType.data.generics.item()

        assertThat(generic.link().name).isEqualTo("String")
    }

    @Test
    fun `Parameter understands nested generics`() {
        val param = """
            |fun foo(a: List<Set<String>>)
        """.render().param()

        val paramType = param.data.primary.asType()
        val generic = paramType.data.generics.item().asType()
        val nestedGeneric = generic.data.generics.item()

        assertThat(nestedGeneric.link().name).isEqualTo("String")
    }

    @Test
    fun `Parameter understands * generics`() {
        val param = """
            |fun foo(a: List<*>)
        """.render().param()

        val paramType = param.data.primary.asType()
        val generic = paramType.data.generics.item()

        javaOnly { assertThat(generic.link().name).isEqualTo("?") }
        kotlinOnly { assertThat(generic.link().name).isEqualTo("*") }
    }

    @Test
    fun `Parameter understands lambda generics`() {
        val param = """
            |fun foo(a: List<() -> String>)
        """.render().param()

        val generic = param.data.primary.asType().data.generics.item()
        val lambdaParam = (generic as Parameter).data

        javaOnly {
            assertNoLambdaStuff(lambdaParam)

            val primary = lambdaParam.primary.asType().data
            assertThat(primary.type.data.name).isEqualTo("Function0")
            assertThat(primary.generics.item().link().name).isEqualTo("String")
        }
        kotlinOnly {
            assertThat(lambdaParam.isLambda).isTrue()
            assertThat(lambdaParam.receiver).isNull()
            assertThat(lambdaParam.lambdaModifiers).isEmpty()
            assertThat(lambdaParam.lambdaParams).isEmpty()
            assertThat(lambdaParam.primary.link().name).isEqualTo("String")
        }
    }

    @Test // TODO: upstream has problems with generic java parameters?
    fun `Parameter understands variance generics`() {
        val paramK = """
            |fun foo(a: Map<in String, out Double>)
        """.render().param()
        val paramJ = """
            |public void foo(Map<? super String, ? extends Double> a)
        """.render(java = true).param()

        for (param in listOf(paramK/*, paramJ*/)) {
            val paramType = param.data.primary.asType()
            val generic = paramType.data.generics.items(2)

            // TODO(b/166530498): support variance
            assertThat(generic.first().link().name).isEqualTo("String")
            assertThat(generic.last().link().name).isEqualTo("Double")
        }
    }

    @Test
    fun `Parameter understands inline generics`() {
        val param = """
            |fun <T> foo(a: T)
        """.render().param()

        val paramType = param.data.primary.asType()

        assertThat(paramType.link().name).isEqualTo("T")
        assertThat(paramType.link().url).isEmpty()
    }

    @Test
    fun `Parameter understands factory lambda`() {
        val param = """
            |fun foo(a: () -> Unit)
        """.render().param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.asType().data
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
    fun `Parameter understands suspend lambda`() {
        val param = """
            |fun foo(a: suspend () -> Unit)
        """.render().param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.asType().data
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

    @Test
    fun `Parameter understands suspend lambda with receiver`() {
        val param = """
            |fun foo(a: suspend Float.() -> Unit)
        """.render().param().data

        javaOnly {
            assertNoLambdaStuff(param)
            assertThat(param.primary.link().name).isEqualTo("SuspendFunction1")
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
    fun `Parameter understands suspend lambda with params`() {
        val param = """
            |fun foo(a: suspend (Float) -> Unit)
        """.render().param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.asType().data
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
    fun `Parameter understands parameterized lambda`() {
        val param = """
            |fun foo(a: (String) -> Unit)
        """.render().param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.asType().data
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
    fun `Parameter understands lambda with receiver`() {
        val param = """
            |fun foo(a: Float.() -> Unit)
        """.render().param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.asType().data
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
    fun `Parameter understands lambda with difficult generic combination`() {
        val param = """
            |fun foo(block: Int.(Map<String, Int>, Double) -> Collection<Float>)
        """.render().param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.primary.asType().data
            assertThat(primary.type.data.name).isEqualTo("Function3")
            assertThat(primary.generics).hasSize(4)
            assertThat(primary.generics[0].link().name).isEqualTo("Integer")
            assertThat(primary.generics[1].link().name).isEqualTo("Map")
            assertThat(primary.generics[2].link().name).isEqualTo("Double")
            assertThat(primary.generics[3].link().name).isEqualTo("Collection")

            val mapGenerics = primary.generics[1].asType().data.generics.items(2)
            assertThat(mapGenerics.first().link().name).isEqualTo("String")
            assertThat(mapGenerics.last().link().name).isEqualTo("Integer")

            val collectionGeneric = primary.generics[3].asType().data.generics.item()
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
            assertThat(param.primary.asType().link().name).isEqualTo("Collection")

            val mapGenerics =
                param.lambdaParams.first().asType().data.generics.items(2)
            assertThat(mapGenerics.first().link().name).isEqualTo("String")
            assertThat(mapGenerics.last().link().name).isEqualTo("Int")

            val collectionGeneric = param.primary.asType().data.generics.item()
            assertThat(collectionGeneric.link().name).isEqualTo("Float")
        }
    }

    @Test
    fun `Parameter includes primitive default value`() {
        val param = """
            |fun foo(stuff: String = "stuff")
        """.render().param().data

        javaOnly { assertThat(param.defaultValue).isNull() }
        kotlinOnly { assertThat(param.defaultValue).isEqualTo("\"stuff\"") }
    }

    @Test
    fun `Parameter includes default value`() {
        val param = """
            |fun foo(stuff: List<String> = listOf("a", "b", "c"))
        """.render().param().data

        javaOnly { assertThat(param.defaultValue).isNull() }
        kotlinOnly { assertThat(param.defaultValue).isEqualTo("""listOf("a", "b", "c")""") }
    }

    @Test
    fun `Parameter excludes default value in summary`() {
        val param = """
            |fun foo(stuff: List<String> = listOf("a", "b", "c"))
        """.render().param(forSummary = true).data

        assertThat(param.defaultValue).isNull()
    }

    @Suppress("unused") // TODO: upstream dokka doesn't parse annotations on parameters, b/175612102
    @Test
    fun `Parameter summaries include annotations in 4x Kotlin and Java`() {
        val paramK = """
            |fun foo(@Hello foo: Int) {}
        """.render().param(forSummary = true).data
        val paramJ = """
            |public void foo(@Hello int foo) {}
        """.render(java = true).param(forSummary = true).data

        for (param in listOf(paramK/*, paramJ*/)) {
            assertThat(param.annotations).isNotEmpty()
        }
    }

    @Suppress("unused") // TODO: upstream dokka doesn't parse annotations on parameters, b/175612102
    @Test
    fun `Parameter summaries include nullability information in 4x Kotlin and Java`() {
        val paramK = """
            |fun foo(foo: Int?) {}
        """.render().param(forSummary = true)
        val paramJ = """
            |public static void foo(@Nullable Integer foo) {}
        """.render(java = true).param(forSummary = true)

        for (param in listOf(paramK/*, paramJ*/)) {
            kotlinOnly {
                assertThat(param.data.annotations).isEmpty()
                assertThat(param.nullable).isTrue()
            }
            javaOnly {
                assertThat(param.data.annotations).isNotEmpty()
                // Without this assertion, we end up with ? in as-java documentation
                assertThat(param.data.primary.asType().data.nullable).isFalse()
            }
        }
    }

    @Test
    fun `Nullable primitive type is upgraded in Java`() {
        val param = """
            |fun foo(foo: Int?)
        """.render().param().data

        val typeName = param.primary.asType().link().name

        javaOnly { assertThat(typeName).isEqualTo("Integer") }
        kotlinOnly { assertThat(typeName).isEqualTo("Int") }
    }

    @Test
    fun `Primitive type in generic is upgraded in Java`() {
        val param = """
            |fun foo(foo: List<Int>)
        """.render().param().data

        val generic = param.primary.asType().data.generics.item()

        javaOnly { assertThat(generic.link().name).isEqualTo("Integer") }
        kotlinOnly { assertThat(generic.link().name).isEqualTo("Int") }
    }

    @Test
    fun `Unit isn't changed in Java when used as a parameter`() {
        val param = """
            |fun foo(foo: Unit)
        """.render().param().data

        assertThat(param.primary.asType().link().name).isEqualTo("Unit")
    }

    @Test
    fun `Primative type from Java code has correct type in Java and Kotlin`() {
        val paramTypeJ = """
            |public void foo(int a) {}
        """.render(java = true).param()
        val paramTypeK = """
            |fun foo(a: Int) = Unit
        """.render().param()

        for (paramType in listOf(paramTypeJ, paramTypeK)) {
            javaOnly {
                assertThat(paramType.link().name).isEqualTo("int")
                assertThat(paramType.link().url).isEmpty()
            }

            kotlinOnly {
                assertThat(paramType.link().name).isEqualTo("Int")
                assertThat(paramType.link().url).isEqualTo("/reference/kotlin/kotlin/Int.html")
            }
        }
    }

    @Test
    fun `Primitive array is mapped in Java`() {
        val param = """
            |fun foo(foo: IntArray)
        """.render().param().data

        val typeName = param.primary.asType().link().name

        javaOnly { assertThat(typeName).isEqualTo("int[]") }
        kotlinOnly { assertThat(typeName).isEqualTo("IntArray") }
    }

    @Test
    fun `Vararg modifier appears for param`() {
        val param = """
            |fun foo(vararg stuff: Int) = Unit
        """.render().param()

        assertThat(param.data.name).isEqualTo("stuff")
        javaOnly { assertThat(param.data.modifiers).isEmpty() }
        kotlinOnly { assertThat(param.data.modifiers.last()).isEqualTo("vararg") }
    }

    @Test
    fun `Crossline modifier appears for param`() {
        val param = """
            |fun foo(crossinline stuff: () -> Unit) = Unit
        """.render().param()

        assertThat(param.data.name).isEqualTo("stuff")
        javaOnly { assertThat(param.data.modifiers).isEmpty() }
        kotlinOnly { assertThat(param.data.modifiers.last()).isEqualTo("crossinline") }
    }

    @Test
    fun `higher order param name appears for param`() {
        val param = """
            |fun foo(block: (factory: () -> String) -> Int) = Unit
        """.render().param().data

        assertThat(param.name).isEqualTo("block")
        javaOnly {
            assertNoLambdaStuff(param)
        }
        kotlinOnly {
            assertThat(param.lambdaParams).hasSize(1)
            val lambdaParam = param.lambdaParams.single() as Parameter
            assertThat(lambdaParam.data.name).isEqualTo("factory")
        }
    }

    private fun DModule.param(forSummary: Boolean = false): Parameter {
        val converter = ParameterDocumentableConverter(language, pathProvider())
        return converter.componentForParameter(parameterDoc(), forSummary)
    }

    private fun DModule.parameterDoc(): DParameter = function()!!.parameters.single()

    private fun assertNoLambdaStuff(data: Parameter.Params) {
        assertThat(data.isLambda).isFalse()
        assertThat(data.receiver).isNull()
        assertThat(data.lambdaParams).isEmpty()
        assertThat(data.lambdaModifiers).isEmpty()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
