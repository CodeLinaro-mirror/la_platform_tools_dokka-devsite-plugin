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
import com.google.devsite.renderer.converters.testing.exceptNonNull
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.testing.ConverterTestBase
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
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

        val paramType = param.data.type.asType()

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

        val paramType = param.data.type.asType()

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

        assertThat(param.data.annotationComponents).isNotEmpty()
    }

    @Test
    fun `Parameter understands generics`() {
        val param = """
            |fun foo(a: List<String>)
        """.render().param()

        val paramType = param.data.type.asType()
        val generic = paramType.data.generics.item()

        assertThat(generic.link().name).isEqualTo("String")
    }

    @Test
    fun `Parameter understands nested generics`() {
        val param = """
            |fun foo(a: List<Set<String>>)
        """.render().param()

        val paramType = param.data.type.asType()
        val generic = paramType.data.generics.item().asType()
        val nestedGeneric = generic.data.generics.item()

        assertThat(nestedGeneric.link().name).isEqualTo("String")
    }

    @Test
    fun `Parameter understands * generics`() {
        val param = """
            |fun foo(a: List<*>)
        """.render().param()

        val paramType = param.data.type.asType()
        val generic = paramType.data.generics.item()

        javaOnly { assertThat(generic.link().name).isEqualTo("?") }
        kotlinOnly { assertThat(generic.link().name).isEqualTo("*") }
    }

    @Test
    fun `Parameter understands lambda generics`() {
        val param = """
            |fun foo(a: List<() -> String>)
        """.render().param()

        val generic = param.data.type.asType().data.generics.item()
        val lambdaParam = (generic as Parameter).data

        javaOnly {
            assertNoLambdaStuff(lambdaParam)

            val primary = lambdaParam.type.asType().data
            assertThat(primary.type.data.name).isEqualTo("Function0")
            assertThat(primary.generics.item().link().name).isEqualTo("String")
        }
        kotlinOnly {
            assertThat(lambdaParam.isLambda).isTrue()
            assertThat(lambdaParam.receiver).isNull()
            assertThat(lambdaParam.lambdaModifiers).isEmpty()
            assertThat(lambdaParam.lambdaParams).isEmpty()
            assertThat(lambdaParam.type.link().name).isEqualTo("String")
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
            val paramType = param.data.type.asType()
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

        val paramType = param.data.type.asType()

        assertThat(paramType.link().name).isEqualTo("T")
        assertThat(paramType.link().url).isEmpty()
    }

    @Test
    fun `Nullability on parameters is rendered correctly in 4x Kotlin and Java`() {
        val functionK = """
            |fun foo(a: String, b: String?)
        """.render()
        val functionJ = """
            |public void foo(@NonNull String a, @Nullable String b)
        """.render(java = true)
        for (function in listOf(functionK, functionJ)) {
            val paramA = function.param("a")
            val paramB = function.param("b")
            assertFalse(paramA.nullable)
            assertTrue(paramB.nullable)
            kotlinOnly { // Kotlin uses ? and default-nonnull instead of annotations
                assertThat(paramA.data.annotationComponents).isEmpty()
                assertThat(paramB.data.annotationComponents).isEmpty()
            }
            javaOnly {
                if (function != functionK) { // Kotlin-as-Java *types only* don't generate @NonNull
                    assertThat(paramA.data.annotationComponents.single().name).isEqualTo("NonNull")
                }
                assertThat(paramB.data.annotationComponents.single().name).isEqualTo("Nullable")
            }
        }
    }

    @Test
    fun `Java primitive types do not have nullability injected`() {
        val function = """
            |fun foo (a: Int, b: Unit, c: Nothing)
        """.render()
        val paramA = function.param("a")
        val paramB = function.param("b")
        val paramC = function.param("c")
        assertThat(paramA.data.annotationComponents).isEmpty()
        // Unit can be nullable, but that information is basically never useful
        assertThat(paramB.data.annotationComponents).isEmpty()
        // Nothing is always null, but @Nullable is not useful
        assertThat(paramC.data.annotationComponents).isEmpty()
    }

    @Test
    fun `Parameter understands factory lambda`() {
        val param = """
            |fun foo(a: () -> Unit)
        """.render().param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.type.asType().data
            assertThat(primary.type.data.name).isEqualTo("Function0")
            assertThat(primary.generics.item().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(param.isLambda).isTrue()
            assertThat(param.receiver).isNull()
            assertThat(param.lambdaModifiers).isEmpty()
            assertThat(param.lambdaParams).isEmpty()
            assertThat(param.type.link().name).isEqualTo("Unit")
        }
    }

    @Test
    fun `Parameter understands suspend lambda`() {
        val param = """
            |fun foo(a: suspend () -> Unit)
        """.render().param().data

        javaOnly {
            assertNoLambdaStuff(param)

            val primary = param.type.asType().data
            assertThat(primary.type.data.name).isEqualTo("SuspendFunction0")
            assertThat(primary.generics.single().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(param.isLambda).isTrue()
            assertThat(param.receiver).isNull()
            assertThat(param.lambdaParams).isEmpty()
            assertThat(param.lambdaModifiers).containsExactly("suspend")
            assertThat(param.type.link().name).isEqualTo("Unit")
        }
    }

    @Test
    fun `Parameter understands suspend lambda with receiver`() {
        val param = """
            |fun foo(a: suspend Float.() -> Unit)
        """.render().param().data

        javaOnly {
            assertNoLambdaStuff(param)
            assertThat(param.type.link().name).isEqualTo("SuspendFunction1")
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

            val primary = param.type.asType().data
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

            val primary = param.type.asType().data
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

            val primary = param.type.asType().data
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

            val primary = param.type.asType().data
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
            assertThat(param.type.asType().link().name).isEqualTo("Collection")

            val mapGenerics =
                param.lambdaParams.first().asType().data.generics.items(2)
            assertThat(mapGenerics.first().link().name).isEqualTo("String")
            assertThat(mapGenerics.last().link().name).isEqualTo("Int")

            val collectionGeneric = param.type.asType().data.generics.item()
            assertThat(collectionGeneric.link().name).isEqualTo("Float")
        }
    }

    @Test
    fun `Parameter includes default string value`() {
        val param = """
            |fun foo(stuff: String = "stuff")
        """.render().param().data

        javaOnly { assertThat(param.defaultValue).isNull() }
        kotlinOnly { assertThat(param.defaultValue).isEqualTo("\"stuff\"") }
    }

    @Test
    fun `Parameter includes default float values`() {
        val module = """
            |fun foo(a: Float = 0f, b: Float = 2.7180f)
        """.render()

        val paramA = module.param("a").data
        val paramB = module.param("b").data

        javaOnly {
            assertThat(paramA.defaultValue).isNull()
            assertThat(paramB.defaultValue).isNull()
        }
        kotlinOnly {
            assertThat(paramA.defaultValue).isEqualTo("0.0f")
            assertThat(paramB.defaultValue).isEqualTo("2.718f")
        }
    }

    @Test
    fun `Parameter includes default double values`() {
        val module = """
            |fun foo(a: Double = 0.0, b: Double = 2.7180)
        """.render()

        val paramA = module.param("a").data
        val paramB = module.param("b").data

        javaOnly {
            assertThat(paramA.defaultValue).isNull()
            assertThat(paramB.defaultValue).isNull()
        }
        kotlinOnly {
            assertThat(paramA.defaultValue).isEqualTo("0.0")
            assertThat(paramB.defaultValue).isEqualTo("2.718")
        }
    }

    @Test
    fun `Parameter includes complex default values`() {
        val module = """
            |fun foo(a: List<String = listOf("a", "b", "c"))
        """.render()

        val param = module.param().data

        javaOnly {
            assertThat(param.defaultValue).isNull()
        }
        kotlinOnly {
            assertThat(param.defaultValue).isEqualTo("listOf(\"a\", \"b\", \"c\")")
        }
    }

    @Test
    fun `Parameter includes default list value`() {
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

    @Test
    fun `Parameter summaries include annotations in 4x Kotlin and Java`() {
        val annotK = """
            |annotation class Stuff
            |fun foo(@Stuff kotlinFoo: Int) {}
        """.render().param(forSummary = true).data.annotationComponents
        val annotJ = """
            |@Target({PARAMETER})
            |public @interface Stuff {
            |}
            |public void foo(@Stuff int javaFoo) {};
        """.render(java = true).param(forSummary = true).data.annotationComponents

        for (annot in listOf(annotK, annotJ)) {
            assertThat(annot.exceptNonNull().single().data.type.data.name).contains("Stuff")
        }
    }

    @Test
    fun `Parameter summaries include nullability information in 4x Kotlin and Java`() {
        val paramK = """
            |fun foo(foo: Int?) {}
        """.render().param(forSummary = true)
        val paramJ = """
            |public static void foo(@Nullable Integer foo) {}
        """.render(java = true).param(forSummary = true)

        for (param in listOf(paramK, paramJ)) {
            kotlinOnly {
                assertThat(param.data.annotationComponents).isEmpty()
                assertThat(param.nullable).isTrue()
            }
            javaOnly {
                assertThat(param.data.annotationComponents).isNotEmpty()
                // Without this assertion, we end up with ? in as-java documentation
                assertThat(param.data.type.asType().data.nullable).isFalse()
            }
        }
    }

    @Test
    fun `Nullable primitive type is upgraded in Java`() {
        val param = """
            |fun foo(foo: Int?)
        """.render().param().data

        val typeName = param.type.asType().link().name

        javaOnly { assertThat(typeName).isEqualTo("Integer") }
        kotlinOnly { assertThat(typeName).isEqualTo("Int") }
    }

    @Test
    fun `Primitive type in generic is upgraded in Java`() {
        val param = """
            |fun foo(foo: List<Int>)
        """.render().param().data

        val generic = param.type.asType().data.generics.item()

        javaOnly { assertThat(generic.link().name).isEqualTo("Integer") }
        kotlinOnly { assertThat(generic.link().name).isEqualTo("Int") }
    }

    @Test
    fun `Unit isn't changed in Java when used as a parameter`() {
        val param = """
            |fun foo(foo: Unit)
        """.render().param().data

        assertThat(param.type.asType().link().name).isEqualTo("Unit")
    }

    @Test
    fun `Primitive type from Java code has correct type in Java and Kotlin`() {
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

        val typeName = param.type.asType().link().name

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

    @Test
    fun `params that are type aliases appear as aliases`() {
        val param = """
            |typealias MyString = String
            |fun foo(name: MyString) = Unit
        """.render().param().data

        assertThat(param.name).isEqualTo("name")
        val typeName = param.type.asType().link().name
        assertThat(typeName).isEqualTo("String")
    }

    @Test
    fun `Kotlin docs for java sources use kotlin types when available`() {
        val paramK = """
            |fun foo(a: String) = Unit
        """.render().param()

        val paramJ = """
            |public void foo(String foo) {}
        """.render(java = true).param()

        for (param in listOf(paramJ, paramK)) {
            val paramType = param.data.type.asType()

            assertThat(paramType.link().name).isEqualTo("String")

            javaOnly {
                assertThat(paramType.link().url).isEqualTo("/reference/java/lang/String.html")
            }

            kotlinOnly {
                assertThat(paramType.link().url).isEqualTo("/reference/kotlin/kotlin/String.html")
            }
        }
    }

    @Test
    fun `Kotlin docs for java sources use kotlin types for arrays`() {
        val intParamTypeJ = """
            |public void foo(int[] a) {}
        """.render(java = true).param().data
        val intParamTypeK = """
             |fun foo(a: IntArray)
        """.render().param().data
        val booleanParamTypeJ = """
            |public void foo(boolean[] a) {}
        """.render(java = true).param().data
        val booleanParamTypeK = """
             |fun foo(a: BooleanArray)
        """.render().param().data

        for (paramType in listOf(intParamTypeJ, intParamTypeK)) {
            val typeName = paramType.type.asType().link().name

            javaOnly { assertThat(typeName).isEqualTo("int[]") }
            kotlinOnly { assertThat(typeName).isEqualTo("IntArray") }
        }
        for (paramType in listOf(booleanParamTypeJ, booleanParamTypeK)) {
            val typeName = paramType.type.asType().link().name

            javaOnly { assertThat(typeName).isEqualTo("boolean[]") }
            kotlinOnly { assertThat(typeName).isEqualTo("BooleanArray") }
        }
    }

    @Test
    fun `Kotlin arrays are translated only when necessary`() {
        val paramType = """
             |fun foo(a: Array<Int>)
        """.render().param().data

        val actualType = paramType.type.asType().link().name

        // These values are translated, but maybe shouldn't be. It appears to happen upstream before
        // any  of our translation code because Array<Int> is treated as IntArray, and in Java we
        // translate IntArray to int[]. Documenting the current behavior with this test case.
        javaOnly {
            assertThat(actualType).isEqualTo("int[]") // Should probably be `Integer[]`
        }
        kotlinOnly {
            assertThat(actualType).isEqualTo("IntArray") // should probably be Array<Int>
        }
    }

    @Test
    fun `Nested arrays are translated correctly`() {
        val paramTypeK = """
             |fun foo(a: Array<IntArray>)
        """.render().param().data.type.asType()
        val paramTypeJ = """
            |public void foo(int[][] a) {}
        """.render(java = true).param().data.type.asType()

        listOf(paramTypeJ, paramTypeK).forEach { paramType ->
            val actualType = paramType.link().name
            javaOnly {
                assertThat(actualType).isEqualTo("int[][]")
            }
            kotlinOnly {
                val generic = paramType.data.generics.singleOrNull()?.link()?.name
                assertThat(actualType).isEqualTo("Array")
                assertThat(generic).isEqualTo("IntArray")
            }
        }
    }

    @Test
    fun `Deeply nested arrays are translated correctly`() {
        val paramTypeK = """
             |fun foo(a: Array<Array<IntArray>>)
        """.render().param().data.type.asType()
        val paramTypeJ = """
            |public void foo(int[][][] a) {}
        """.render(java = true).param().data.type.asType()

        listOf(paramTypeJ, paramTypeK).forEach { paramType ->
            val actualType = paramType.link().name
            javaOnly {
                assertThat(actualType).isEqualTo("int[][][]")
            }
            kotlinOnly {
                val generic = paramType.data.generics.singleOrNull()
                assertThat(actualType).isEqualTo("Array")
                assertThat(generic?.link()?.name).isEqualTo("Array")
                assertThat(
                    generic?.asType()?.data?.generics?.singleOrNull()?.link()?.name
                ).isEqualTo("IntArray")
            }
        }
    }

    @Test
    fun `Java arrays with non-primitive members are translated correctly`() {
        val paramType = """
            |public void foo(Integer[] a) {}
        """.render(java = true).param().data.type.asType()
        val nestedParamType = """
            |public void foo(Integer[][] a) {}
        """.render(java = true).param().data.type.asType()

        val actualType = paramType.link().name
        val nestedActualType = nestedParamType.link().name

        javaOnly {
            assertThat(actualType).isEqualTo("Integer[]")
            assertThat(nestedActualType).isEqualTo("Integer[][]")
        }
        kotlinOnly {
            val generic = paramType.data.generics.single()
            assertThat(actualType).isEqualTo("Array")
            assertThat(generic.link().name).isEqualTo("Int")

            val nestedGeneric = nestedParamType.data.generics.single()
            assertThat(nestedActualType).isEqualTo("Array")
            assertThat(nestedGeneric.link().name).isEqualTo("Array")
        }
    }

    private fun DModule.param(name: String = "foo", forSummary: Boolean = false): Parameter {
        val classGraph = runBlocking {
            DocumentablesHolder(this@param, this).classGraph()
        }
        val converter = ParameterDocumentableConverter(
            language,
            pathProvider(classGraph = classGraph)
        )
        return converter.componentForParameter(parameterDoc(name), forSummary)
    }

    private fun DModule.parameterDoc(name: String = "foo"): DParameter =
        function()!!.parameters.singleOrNull { it.name == name } ?: function()!!.parameters.single()

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
