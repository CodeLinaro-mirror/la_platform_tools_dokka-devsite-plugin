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
import com.google.devsite.components.symbols.LambdaTypeProjectionComponent
import com.google.devsite.components.symbols.MappedTypeProjectionComponent
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.alternativeLink
import com.google.devsite.renderer.converters.testing.exceptNonNull
import com.google.devsite.renderer.converters.testing.fullTypeName
import com.google.devsite.renderer.converters.testing.isAtNonNull
import com.google.devsite.renderer.converters.testing.isAtNullable
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.typeAnnotations
import com.google.devsite.renderer.converters.testing.typeName
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DParameter
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ParameterDocumentableConverterTest(
    private val displayLanguage: Language
) : ConverterTestBase(displayLanguage) {

    private val intArray = if (displayLanguage == Language.JAVA) "int[]" else "IntArray"
    private val boolArray = if (displayLanguage == Language.JAVA) "boolean[]" else "BooleanArray"
    private val objArray = if (displayLanguage == Language.JAVA) "Object[]" else "Array<Any>"

    @Test
    fun `Parameter has correct type`() {
        val param = """
            |fun foo(a: String) = Unit
        """.render().param()

        val paramType = param.data.type

        assertThat(paramType.link().name).isEqualTo("String")

        javaOnly {
            assertThat(paramType.link().url)
                .isEqualTo("https://developer.android.com/reference/java/lang/String.html")
        }

        kotlinOnly {
            assertThat(paramType.link().url)
                .isEqualTo("https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html")
        }
    }

    @Test
    fun `Parameter has correct type for Any`() {
        val param = """
            |fun foo(a: Any) = Unit
        """.render().param()

        val paramType = param.data.type

        javaOnly {
            assertThat(paramType.link().name).isEqualTo("Object")
            assertThat(paramType.link().url)
                .isEqualTo("https://developer.android.com/reference/java/lang/Object.html")
        }

        kotlinOnly {
            assertThat(paramType.link().name).isEqualTo("Any")
            assertThat(paramType.link().url)
                .isEqualTo("https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html")
        }
    }

    @Test
    fun `Parameter understands annotation`() {
        val param = """
            |annotation class Hello
            |fun foo(@Hello a: List<Int>)
        """.render().param()

        assertThat(param.annotations).isNotEmpty()
    }

    @Test
    fun `Parameter understands generics`() {
        val param = """
            |fun foo(a: List<String>)
        """.render().param()

        val paramType = param.data.type
        val generic = paramType.data.generics.item()

        assertThat(generic.link().name).isEqualTo("String")
    }

    @Test
    fun `Parameter understands nested generics`() {
        val param = """
            |fun foo(a: List<Set<String>>)
        """.render().param()

        val paramType = param.data.type
        val generic = paramType.data.generics.item()
        val nestedGeneric = generic.data.generics.item()

        assertThat(nestedGeneric.link().name).isEqualTo("String")
    }

    @Test
    fun `Parameter understands * generics`() {
        val param = """
            |fun foo(a: List<*>)
        """.render().param()

        val paramType = param.data.type
        val generic = paramType.data.generics.item()

        javaOnly { assertThat(generic.link().name).isEqualTo("?") }
        kotlinOnly { assertThat(generic.link().name).isEqualTo("*") }
    }

    @Test
    fun `Parameter understands lambda generics`() {
        val param = """
            |fun foo(a: List<() -> String>)
        """.render().param()

        val generic = param.data.type.data.generics.item()

        javaOnly {
            assertThat(generic is LambdaTypeProjectionComponent).isFalse()

            assertThat(generic.data.type.data.name).isEqualTo("Function0")
            assertThat(generic.data.generics.item().link().name).isEqualTo("String")
        }
        kotlinOnly {
            val lambdaSymbol = generic as LambdaTypeProjectionComponent
            assertThat(lambdaSymbol.data.receiver).isNull()
            assertThat(lambdaSymbol.data.lambdaModifiers).isEmpty()
            assertThat(lambdaSymbol.data.lambdaParams).isEmpty()
            assertThat(lambdaSymbol.data.type.data.name).isEqualTo("String")
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
            val paramType = param.data.type
            val generic = paramType.data.generics.items(2)

            // TODO(b/166530498): support variance
            assertThat(generic.first().link().name).isEqualTo("String")
            assertThat(generic.last().link().name).isEqualTo("Double")
        }
    }

    @Test
    fun `Parameter understands inline generics`() {
        val paramK = """
            |fun <T> foo(a: T)
        """.render().param()
        val paramJ = """
            |public <T> void foo(T a)
        """.render(java = true).param()

        for (param in listOf(paramJ, paramK)) {
            val paramType = param.data.type

            assertThat(paramType.link().name).isEqualTo("T")
            assertThat(paramType.link().url).isEmpty()
        }
    }

    private val ARRAY_URI = "https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html"

    @Test
    fun `Parameter understands inline generics and does correct Kotlin-Java array translation`() {
        val moduleK = """
            |fun <T> foo(a: Array<Array<T>>): Array<Array<T>>
        """.render()
        val moduleJ = """
            |public <T> T[][] foo(T[][] a)
        """.render(java = true)

        for (module in listOf(moduleJ, moduleK)) {
            val paramType = module.param().data.type
            val returnType = module.returnType()

            javaOnly {
                assertThat(paramType.link().name).isEqualTo("T[][]")
                assertThat(returnType.link().name).isEqualTo("T[][]")
                assertThat(paramType.link().url).isEmpty()
                assertThat(returnType.link().url).isEmpty()
            }
            kotlinOnly {
                assertThat(paramType.link().name).isEqualTo("Array")
                val paramGeneric1 = paramType.data.generics.single()
                assertThat(paramGeneric1.link().name).isEqualTo("Array")
                val paramGeneric2 = paramGeneric1.data.generics.single()
                assertThat(paramGeneric2.link().name).isEqualTo("T")
                assertThat(returnType.link().name).isEqualTo("Array")
                val returnGeneric1 = returnType.data.generics.single()
                assertThat(returnGeneric1.link().name).isEqualTo("Array")
                val returnGeneric2 = returnGeneric1.data.generics.single()
                assertThat(returnGeneric2.link().name).isEqualTo("T")
                assertThat(paramType.link().url).isEqualTo(ARRAY_URI)
                assertThat(returnType.link().url).isEqualTo(ARRAY_URI)
                assertThat(paramGeneric1.link().url).isEqualTo(ARRAY_URI)
                assertThat(returnGeneric1.link().url).isEqualTo(ARRAY_URI)
                assertThat(paramGeneric2.link().url).isEmpty()
                assertThat(returnGeneric2.link().url).isEmpty()
            }
        }
    }

    @Test
    fun `Array of lambdas in Kotlin is handled properly in Java`() {
        val paramType = """
            |fun foo(a: Array<Float.(String) -> Int>): Array<(String) -> Int>
        """.render().param().data.type
        javaOnly {
            assertThat(paramType.link().name).isEqualTo("Function2(Float, String, Integer)[]")
        }
        kotlinOnly {
            assertThat(paramType.link().name).isEqualTo("Array")
            val paramGeneric = paramType.data.generics.single() as LambdaTypeProjectionComponent
            assertThat(paramGeneric.data.type.data.name).isEqualTo("Int")
            assertThat(paramGeneric.data.lambdaParams.single().data.type.name()).isEqualTo("String")
            assertThat(paramGeneric.data.receiver!!.name()).isEqualTo("Float")
        }
    }

    @Test
    fun `Nested array nullability is handled properly in Kotlin`() {
        val moduleK = """
            |fun foo(a: Array<Array<String>?>, b: Array<Array<String?>>?)
        """.render()
        val paramA = moduleK.param("a")
        val typeA = paramA.data.type
        val paramB = moduleK.param("b")
        val typeB = paramB.data.type
        kotlinOnly {
            assertThat(paramA.nullable).isFalse()
            assertThat(typeA.nullable).isFalse()
            assertThat(typeA.data.generics.single().nullable).isTrue()
            assertThat(typeA.data.generics.single().data.generics.single().nullable).isFalse()

            assertThat(paramB.nullable).isTrue()
            assertThat(typeB.nullable).isTrue()
            assertThat(typeB.data.generics.single().nullable).isFalse()
            assertThat(typeB.data.generics.single().data.generics.single().nullable).isTrue()
        }
        // This is impossible to represent in java, sadly, and this is a limitation of the language.
    }

    @Test
    fun `Array nullability is handled properly in conversion to Java`() {
        val moduleK = """
            |fun foo(
            |   nonnaInt: IntArray,
            |   nonnaObj: Array<Any>,
            |   nullaInt: IntArray?
            |   nullaArr: Array<List<Int>>?
            |)
        """.render()
        val moduleJ = """
            |public void foo(
            |   @NonNull int[] nonnaInt,
            |   @NonNull Object[] nonnaObj,
            |   @Nullable int[] nullaInt,
            |   @Nullable List<Integer>[] nullaArr
            |)
        """.render(java = true)
        for (module in listOf(moduleJ, moduleK)) {
            val nonnaInt = module.param("nonnaInt")
            val nonnaObj = module.param("nonnaObj")
            val nullaInt = module.param("nullaInt")
            val nullaArr = module.param("nullaArr")

            val nullas = listOf(nullaInt, nullaArr)
            val nonnas = listOf(nonnaInt, nonnaObj)
            val ints = listOf(nullaInt, nullaInt)

            val expectedNonNull =
                if (module == moduleJ) Nullability.JAVA_ANNOTATED_NOT_NULL
                else Nullability.KOTLIN_DEFAULT
            val expectedNullable =
                if (module == moduleJ) Nullability.JAVA_ANNOTATED_NULLABLE
                else Nullability.KOTLIN_NULLABLE

            nonnas.forEach { assertThat(it.nullable).isFalse() }
            nullas.forEach { assertThat(it.nullable).isTrue() }
            nonnas.forEach { assertThat(it.data.type.data.nullability).isEqualTo(expectedNonNull) }
            nullas.forEach { assertThat(it.data.type.data.nullability).isEqualTo(expectedNullable) }

            val expectedArrList = when (module to displayLanguage) {
                moduleJ to Language.JAVA -> "List<Integer>[]"
                moduleK to Language.JAVA -> "List[]"
                moduleJ to Language.KOTLIN -> "Array<List<Integer>>"
                moduleK to Language.KOTLIN -> "Array<List>"
                else -> throw RuntimeException("Impossible! Perhaps the archives are incomplete.")
            }
            ints.forEach { assertThat(it.typeName()).isEqualTo(intArray) }
            assertThat(nonnaObj.fullTypeName()).isEqualTo(objArray)
            assertThat(nullaArr.fullTypeName()).isEqualTo(expectedArrList)
        }
    }

    @Test
    fun `Nullability on parameters is rendered correctly in 4x Kotlin and Java`() {
        val functionK = """
            |fun foo(a: String, b: String?) // Cannot write platform types `String!` in source code
        """.render()
        val functionJ = """
            |public void foo(@NonNull String a, @Nullable String b, String c)
        """.render(java = true)
        for (function in listOf(functionK, functionJ)) {
            val paramA = function.param("a")
            val paramB = function.param("b")
            assertThat(paramA.nullable).isFalse()
            assertThat(paramB.nullable).isTrue()
            if (function == functionJ) {
                val paramC = function.param("c")
                assertThat(paramC.nullable).isTrue()
                assertThat(paramC.annotations).isEmpty()
            }
            kotlinOnly { // Kotlin uses ?, !, and default-nonnull instead of annotations
                assertThat(paramA.annotations).isEmpty()
                assertThat(paramB.annotations).isEmpty()
            }
            javaOnly {
                assertThat(paramA.typeAnnotations().single().isAtNonNull).isTrue()
                if (function == functionJ) {
                    assertThat(paramB.typeAnnotations().single().isAtNullable).isTrue()
                } else {
                    assertThat(paramB.typeAnnotations()).isEmpty()
                }
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
        assertThat(paramA.annotations).isEmpty()
        // Unit can be nullable, but that information is basically never useful
        assertThat(paramB.annotations).isEmpty()
        // Nothing is always null, but @Nullable is not useful
        assertThat(paramC.annotations).isEmpty()
    }

    @Test
    fun `Parameter understands factory lambda`() {
        val lambdaParam = """
            |fun foo(a: () -> Unit)
        """.render().param().data

        val primary = lambdaParam.type

        javaOnly {
            assertThat(primary is LambdaTypeProjectionComponent).isFalse()

            assertThat(primary.name()).isEqualTo("Function0")
            assertThat(primary.data.generics.item().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(primary is LambdaTypeProjectionComponent).isTrue()
            assertThat((primary as LambdaTypeProjectionComponent).data.receiver).isNull()
            assertThat(primary.data.lambdaModifiers).isEmpty()
            assertThat(primary.data.lambdaParams).isEmpty()
            assertThat(primary.link().name).isEqualTo("Unit")
        }
    }

    @Test
    fun `Parameter understands suspend lambda`() {
        val lambdaParam = """
            |fun foo(a: suspend () -> Unit)
        """.render().param().data

        javaOnly {
            assertThat(lambdaParam.type is LambdaTypeProjectionComponent).isFalse()

            val primary = lambdaParam.type.data
            assertThat(primary.type.data.name).isEqualTo("SuspendFunction0")
            assertThat(primary.generics.single().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(lambdaParam.type is LambdaTypeProjectionComponent).isTrue()
            val lambdaSymbol = (lambdaParam.type as LambdaTypeProjectionComponent).data
            assertThat(lambdaSymbol.receiver).isNull()
            assertThat(lambdaSymbol.lambdaModifiers).containsExactly("suspend")
            assertThat(lambdaSymbol.lambdaParams).isEmpty()
            assertThat(lambdaParam.type.link().name).isEqualTo("Unit")
        }
    }

    @Test
    fun `Parameter understands suspend lambda with receiver`() {
        val param = """
            |fun foo(a: suspend Float.() -> Unit)
        """.render().param().data

        val primary = param.type

        javaOnly {
            assertThat(param.type is LambdaTypeProjectionComponent).isFalse()
            assertThat(param.type.link().name).isEqualTo("SuspendFunction1")
        }
        kotlinOnly {
            assertThat(primary is LambdaTypeProjectionComponent).isTrue()
            assertThat((primary as LambdaTypeProjectionComponent).data.receiver).isNotNull()
            assertThat(primary.data.receiver!!.link().name).isEqualTo("Float")
            assertThat(primary.data.lambdaModifiers).containsExactly("suspend")
            assertThat(primary.data.lambdaParams).isEmpty()
        }
    }

    @Test
    fun `Parameter understands suspend lambda with params`() {
        val param = """
            |fun foo(a: suspend (Float) -> Unit)
        """.render().param().data

        val primary = param.type

        javaOnly {
            assertThat(param.type is LambdaTypeProjectionComponent).isFalse()

            assertThat(primary.data.type.data.name).isEqualTo("SuspendFunction1")
            assertThat(primary.data.generics.first().link().name).isEqualTo("Float")
            assertThat(primary.data.generics.last().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(primary is LambdaTypeProjectionComponent).isTrue()
            assertThat((primary as LambdaTypeProjectionComponent).data.receiver)
                .isNull()
            assertThat(primary.data.lambdaModifiers).containsExactly("suspend")
            assertThat(primary.data.lambdaParams).hasSize(1)
            assertThat(primary.data.lambdaParams.single().data.type.link().name).isEqualTo("Float")
        }
    }

    @Test
    fun `Parameter understands parameterized lambda`() {
        val param = """
            |fun foo(a: (String) -> Unit)
        """.render().param().data

        val primary = param.type

        javaOnly {
            assertThat(param.type is LambdaTypeProjectionComponent).isFalse()

            assertThat(primary.data.type.data.name).isEqualTo("Function1")
            assertThat(primary.data.generics.first().link().name).isEqualTo("String")
            assertThat(primary.data.generics.last().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(primary is LambdaTypeProjectionComponent).isTrue()
            assertThat((primary as LambdaTypeProjectionComponent).data.receiver).isNull()
            assertThat(primary.data.lambdaModifiers).isEmpty()
            assertThat(primary.data.lambdaParams).hasSize(1)
            assertThat(primary.data.lambdaParams.single().typeName()).isEqualTo("String")
        }
    }

    @Test
    fun `Parameter understands lambda with receiver`() {
        val param = """
            |fun foo(a: Float.() -> Unit)
        """.render().param().data

        val primary = param.type

        javaOnly {
            assertThat(primary is LambdaTypeProjectionComponent).isFalse()

            assertThat(primary.data.type.data.name).isEqualTo("Function1")
            assertThat(primary.data.generics.first().link().name).isEqualTo("Float")
            assertThat(primary.data.generics.last().link().name).isEqualTo("Unit")
        }
        kotlinOnly {
            assertThat(primary is LambdaTypeProjectionComponent).isTrue()
            assertThat((primary as LambdaTypeProjectionComponent).data.receiver).isNotNull()
            assertThat(primary.data.receiver!!.link().name).isEqualTo("Float")
            assertThat(primary.data.lambdaModifiers).isEmpty()
            assertThat(primary.data.lambdaParams).isEmpty()
        }
    }

    @Test
    fun `Parameter understands lambda with difficult generic combination`() {
        val param = """
            |fun foo(block: Int.(Map<String, Int>, Double) -> Collection<Float>)
        """.render().param().data

        val primary = param.type

        javaOnly {
            assertThat(param.type is LambdaTypeProjectionComponent).isFalse()

            assertThat(primary.data.type.data.name).isEqualTo("Function3")
            assertThat(primary.data.generics).hasSize(4)
            assertThat(primary.data.generics[0].link().name).isEqualTo("Integer")
            assertThat(primary.data.generics[1].link().name).isEqualTo("Map")
            assertThat(primary.data.generics[2].link().name).isEqualTo("Double")
            assertThat(primary.data.generics[3].link().name).isEqualTo("Collection")

            val mapGenerics = primary.data.generics[1].data.generics.items(2)
            assertThat(mapGenerics.first().link().name).isEqualTo("String")
            assertThat(mapGenerics.last().link().name).isEqualTo("Integer")

            val collectionGeneric = primary.data.generics[3].data.generics.item()
            assertThat(collectionGeneric.link().name).isEqualTo("Float")
        }
        kotlinOnly {
            assertThat(primary is LambdaTypeProjectionComponent).isTrue()
            assertThat((primary as LambdaTypeProjectionComponent).data.receiver).isNotNull()
            assertThat(primary.data.receiver!!.link().name).isEqualTo("Int")
            assertThat(primary.data.lambdaModifiers).isEmpty()
            assertThat(primary.data.lambdaParams).hasSize(2)
            assertThat(primary.data.lambdaParams.first().link().name).isEqualTo("Map")
            assertThat(primary.data.lambdaParams.last().link().name).isEqualTo("Double")
            assertThat(param.type.link().name).isEqualTo("Collection")

            val mapGenerics = primary.data.lambdaParams.first().data.type
                .data.generics.items(2)
            assertThat(mapGenerics.first().link().name).isEqualTo("String")
            assertThat(mapGenerics.last().link().name).isEqualTo("Int")

            val collectionGeneric = primary.data.generics.item()
            assertThat(collectionGeneric.link().name).isEqualTo("Float")
        }
    }

    @Test
    fun `Parameter understands nullable lambda`() {
        val nullableLambdaParam = """
                |fun foo(a: (() -> Unit)?)
            """.render().param().data
        val nonNullLambdaParam = """
                |fun foo(a: (() -> Unit))
            """.render().param().data
        for (lambdaParam in listOf(nullableLambdaParam, nonNullLambdaParam)) {
            javaOnly {
                assertThat(lambdaParam.type is LambdaTypeProjectionComponent).isFalse()

                val lambdaType = lambdaParam.type
                // assertThat(lambdaType.data.generics.single().nullable).isFalse() // DONT_CARE
                assertThat(lambdaType.data.generics.single().annotations).isEmpty()
                assertThat(lambdaParam.annotationComponents).isEmpty()
                if (lambdaParam == nullableLambdaParam) {
                    assertThat(lambdaType.nullable).isTrue()
                    assertThat(lambdaType.annotations).isEmpty()
                } else {
                    assertThat(lambdaType.nullable).isFalse()
                    assertThat(lambdaType.annotations.single().isAtNonNull).isTrue()
                }
            }
            kotlinOnly {
                assertThat(lambdaParam.type is LambdaTypeProjectionComponent).isTrue()
                val lambdaSymbol = (lambdaParam.type as LambdaTypeProjectionComponent)
                assertThat(lambdaSymbol.nullable).isEqualTo(lambdaParam == nullableLambdaParam)
            }
        }
    }

    @Test
    fun `Parameter understands awful compose lambda`() {
        val lambdaParam = """
            |@ExperimentalAnimationApi
            |@Composable
            |fun <S> AnimatedContent(
            |    targetState: S,
            |    modifier: Modifier = Modifier,
            |    transitionSpec: List<S>.() -> String,
            |    contentAlignment: Alignment = Alignment.TopStart,
            |    content: @Composable() AnimatedVisibilityScope.(targetState: S) -> Unit
            |)
            |public class <T> AnimatedContentScope<T>
            |public class ContentTransform
        """.render().param("transitionSpec").data

        javaOnly {
            assertThat(lambdaParam.type is LambdaTypeProjectionComponent).isFalse()

            val lambdaType = lambdaParam.type.data
            assertThat(lambdaParam.annotationComponents).isEmpty()
            assertThat(lambdaType.type.data.name).isEqualTo("Function1")
            assertThat(lambdaType.annotationComponents.map { it.name })
                .isEqualTo(listOf("ExtensionFunctionType", "NonNull"))
            assertThat(lambdaType.generics.size).isEqualTo(2)
            val generic0 = lambdaType.generics.first()
            val generic1 = lambdaType.generics.last()

            assertThat(generic0.nullable).isFalse()
            assertThat(generic0.data.type.data.name).isEqualTo("List")
            assertThat(generic0.annotations.single().isAtNonNull).isTrue()
            val generic0generic = generic0.data.generics.single()
            assertThat(generic0generic.nullable).isFalse()
            assertThat(generic0generic.data.type.data.name).isEqualTo("S")
            assertThat(generic0generic.annotations.single().isAtNonNull).isTrue()
            assertThat(generic0generic.data.generics).isEmpty()

            assertThat(generic1.nullable).isFalse()
            assertThat(generic1.data.type.data.name).isEqualTo("String")
            assertThat(generic1.annotations.single().isAtNonNull).isTrue()
            assertThat(generic1.data.generics).isEmpty()
        }
        kotlinOnly {
            assertThat(lambdaParam.name).isEqualTo("transitionSpec")
            assertThat(lambdaParam.annotationComponents).isEmpty()
            assertThat(lambdaParam.type is LambdaTypeProjectionComponent).isTrue()
            val lambdaSymbol = (lambdaParam.type as LambdaTypeProjectionComponent)
            assertThat(lambdaSymbol.data.type.data.name).isEqualTo("String")
            assertThat(lambdaSymbol.nullable).isFalse()
            assertThat(lambdaSymbol.annotations).isEmpty()
            assertThat(lambdaSymbol.data.lambdaModifiers).isEmpty()
            assertThat(lambdaSymbol.data.lambdaParams).isEmpty()
            assertThat(lambdaSymbol.data.receiver!!.nullable).isFalse()
            assertThat(lambdaSymbol.data.receiver!!.data.type.data.name).isEqualTo("List")
            assertThat(lambdaSymbol.data.receiver!!.annotations).isEmpty()
            val lambdaReceiverGeneric = lambdaSymbol.data.receiver!!.data.generics.single()
            assertThat(lambdaReceiverGeneric.nullable).isFalse()
            assertThat(lambdaReceiverGeneric.data.type.data.name).isEqualTo("S")
            assertThat(lambdaReceiverGeneric.annotations).isEmpty()
            assertThat(lambdaReceiverGeneric.data.generics).isEmpty()
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
        """.render().param(forSummary = true).annotations
        val annotJ = """
            |@Target({PARAMETER})
            |public @interface Stuff {
            |}
            |public void foo(@Stuff int javaFoo) {};
        """.render(java = true).param(forSummary = true).annotations

        for (annot in listOf(annotK, annotJ)) {
            assertThat(annot.exceptNonNull().single().data.type.data.name).contains("Stuff")
        }
    }

    @Test
    fun `Parameter summaries and details include nullability information in 4x Kotlin and Java`() {
        for (isSummary in listOf(true, false)) {
            val paramK = """
                |fun foo(foo: Int?) {}
            """.render().param(forSummary = isSummary)
            val paramJ = """
                |public static void foo(@Nullable Integer foo) {}
            """.render(java = true).param(forSummary = isSummary)
            val paramJ2 = """
                |public static void foo(Integer foo) {}
            """.render(java = true).param(forSummary = isSummary)

            for (param in listOf(paramK, paramJ, paramJ2)) {

                kotlinOnly {
                    assertThat(param.annotations).isEmpty()
                    assertThat(param.nullable).isTrue()
                }
                javaOnly {
                    when (param) {
                        paramJ -> assertThat(param.typeAnnotations().single().isAtNullable).isTrue()
                        paramK, paramJ2 -> assertThat(param.typeAnnotations()).isEmpty()
                    }
                }
            }
        }
    }

    @Test
    fun `Nullable primitive type is upgraded in Java`() {
        val param = """
            |fun foo(foo: Int?)
        """.render().param().data

        val typeName = param.type.link().name

        javaOnly { assertThat(typeName).isEqualTo("Integer") }
        kotlinOnly { assertThat(typeName).isEqualTo("Int") }
    }

    @Test
    fun `Primitive type in generic is upgraded in Java`() {
        val param = """
            |fun foo(foo: List<Int>)
        """.render().param().data

        val generic = param.type.data.generics.item()

        javaOnly { assertThat(generic.link().name).isEqualTo("Integer") }
        kotlinOnly { assertThat(generic.link().name).isEqualTo("Int") }
    }

    @Test
    fun `Unit isn't changed in Java when used as a parameter`() {
        val param = """
            |fun foo(foo: Unit)
        """.render().param().data

        assertThat(param.type.link().name).isEqualTo("Unit")
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
                assertThat(paramType.link().url).isEqualTo(
                    "https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html"
                )
            }
        }
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

    @Ignore // Kotlin does not support modifiers on lambda params
    @Test
    fun `Vararg modifier appears for lambda param`() {
        val param = """
            |fun foo(vararg lambdas: (vararg lambdaparams: String -> Unit))) = Unit
        """.render().param()

        assertThat(param.data.name).isEqualTo("lambdas")
        javaOnly {
            assertThat(param.data.modifiers).isEmpty()
        }
        kotlinOnly {
            assertThat(param.data.modifiers.last()).isEqualTo("vararg")
            val lambdaParam =
                (param.data.type as LambdaTypeProjectionComponent).data.lambdaParams.single()
            assertThat(lambdaParam.data.modifiers).isEqualTo("vararg")
        }
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

    @Test // Note: annotations on named lambda parameters *in front of the name* aren't picked up
    fun `higher order param name appears for param`() {
        val param = """
            |annotation class Size
            |annotation class Res
            |annotation class Annotation
            |annotation class Something
            |/** @param factory a String factory lambda */
            |fun foo(block: (@Size @Annotation factory: @Res @Something () -> String) -> Int) = Unit
        """.render().param().data

        assertThat(param.name).isEqualTo("block")
        javaOnly {
            assertThat(param.type is LambdaTypeProjectionComponent).isFalse()
        }
        kotlinOnly {
            val lambdaParam = (param.type as LambdaTypeProjectionComponent)
                .data.lambdaParams.single()
            assertThat(lambdaParam.data.name).isEqualTo("factory")
            assertThat(lambdaParam.typeName()).isEqualTo("String")

            // We have decided to suppress the ParameterName annotation for now
            val lambdaParamAnnotation = lambdaParam.annotations.single()
            assertThat(lambdaParamAnnotation.data.type.data.name).isEqualTo("Something")

            // We de-deuplicate and assort annotations on lambda parameter parameter names/types
            // Just like we do on functions/return types
            assertThat(lambdaParam.typeAnnotations().single().data.type.data.name).isEqualTo("Res")
        }
    }

    @Test
    fun `params that are type aliases appear as aliases`() {
        val param = """
            |typealias MyString = String
            |fun foo(name: MyString) = Unit
        """.render().param().data

        assertThat(param.name).isEqualTo("name")
        val typeName = param.type.link().name
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
            val paramType = param.data.type

            assertThat(paramType.link().name).isEqualTo("String")

            javaOnly {
                assertThat(paramType.link().url)
                    .isEqualTo("https://developer.android.com/reference/java/lang/String.html")
            }

            kotlinOnly {
                assertThat(paramType.link().url).isEqualTo(
                    "https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html"
                )
            }
        }
    }

    @Test
    fun `Primitive array is mapped in Java`() {
        val param = """
            |fun foo(foo: IntArray)
        """.render().param().data

        val typeName = param.type.link().name

        javaOnly { assertThat(typeName).isEqualTo("int[]") }
        kotlinOnly { assertThat(typeName).isEqualTo("IntArray") }
    }

    @Test
    fun `int array in java has correct nullability in Kotlin and vice versa`() {
        val nullableTypeK = """
            |fun foo(a: IntArray?) = Unit
        """.render().param().data.type
        val nullableTypeJ = """
            |public void foo(@Nullable int[] a) {}
        """.render(java = true).param().data.type
        val nonnullTypeK = """
            |fun foo(a: IntArray) = Unit
        """.render().param().data.type
        val nonnullTypeJ = """
            |public void foo(@NonNull int[] a) {}
        """.render(java = true).param().data.type

        val variantJ = listOf(nullableTypeJ, nonnullTypeJ)
        val variantK = listOf(nullableTypeK, nonnullTypeK)

        for ((nullableType, nonnullType) in listOf(variantJ, variantK)) {
            assertThat(nullableType.nullable).isTrue()
            assertThat(nonnullType.nullable).isFalse()
            javaOnly {
                assertThat(nullableType.name()).isEqualTo("int[]")
                assertThat(nonnullType.name()).isEqualTo("int[]")
                // We explicitly do not inject @Nullable. See Nullability.renderAsJavaAnnotation()
                if (nullableType == nullableTypeK) {
                    assertThat(nullableType.annotations).isEmpty()
                } else {
                    assertThat(nullableType.annotations.single().isAtNullable).isTrue()
                }
                assertThat(nonnullType.annotations.single().isAtNonNull).isTrue()
            }
            kotlinOnly {
                assertThat(nullableType.name()).isEqualTo("IntArray")
                assertThat(nonnullType.name()).isEqualTo("IntArray")
                assertThat(nonnullType.annotations).isEmpty()
                assertThat(nullableType.annotations).isEmpty()
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
            val typeName = paramType.type.link().name

            assertThat(typeName).isEqualTo(intArray)
        }
        for (paramType in listOf(booleanParamTypeJ, booleanParamTypeK)) {
            val typeName = paramType.type.link().name

            assertThat(typeName).isEqualTo(boolArray)
        }
    }

    @Test // TODO: sync with Jetbrains about what should be happening here
    fun `Kotlin arrays are translated only when necessary`() {
        val paramType = """
             |fun foo(a: Array<Int>)
        """.render().param().data.type

        // These values are translated, but maybe shouldn't be. It appears to happen upstream before
        // any  of our translation code because Array<Int> is treated as IntArray, and in Java we
        // translate IntArray to int[]. Documenting the current behavior with this test case.
        javaOnly {
            assertThat(paramType.link().name).isEqualTo("int[]") // Should probably be `Integer[]`
        }
        kotlinOnly {
            assertThat(paramType.link().name).isEqualTo("IntArray") // should probably be Array<Int>
        }
    }

    @Test
    fun `Nested arrays are translated correctly`() {
        val paramTypeK = """
             |fun foo(a: Array<IntArray>)
        """.render().param().data.type
        val paramTypeJ = """
            |public void foo(int[][] a) {}
        """.render(java = true).param().data.type

        listOf(paramTypeJ, paramTypeK).forEach { paramType ->
            javaOnly {
                assertThat(paramType.link().name).isEqualTo("int[][]")
                assertThat(paramType.data.generics).isEmpty()
            }
            kotlinOnly {
                val generic = paramType.data.generics.singleOrNull()?.link()?.name
                assertThat(paramType.link().name).isEqualTo("Array")
                assertThat(generic).isEqualTo("IntArray")
            }
        }
    }

    @Test
    fun `Deeply nested arrays are translated correctly`() {
        val paramTypeK = """
             |fun foo(a: Array<Array<IntArray>>)
        """.render().param().data.type
        val paramTypeJ = """
            |public void foo(int[][][] a) {}
        """.render(java = true).param().data.type

        listOf(paramTypeJ, paramTypeK).forEach { paramType ->
            javaOnly {
                assertThat(paramType.link().name).isEqualTo("int[][][]")
            }
            kotlinOnly {
                val generic = paramType.data.generics.singleOrNull()
                assertThat(paramType.link().name).isEqualTo("Array")
                assertThat(generic?.link()?.name).isEqualTo("Array")
                assertThat(
                    generic?.data?.generics?.singleOrNull()?.link()?.name
                ).isEqualTo("IntArray")
            }
        }
    }

    @Test
    fun `Java arrays with non-primitive members are translated correctly`() {
        val paramType = """
            |public void foo(Integer[] a) {}
        """.render(java = true).param().data.type
        val nestedParamType = """
            |public void foo(Integer[][] a) {}
        """.render(java = true).param().data.type

        javaOnly {
            assertThat(paramType.link().name).isEqualTo("Integer[]")
            assertThat(nestedParamType.link().name).isEqualTo("Integer[][]")
        }
        kotlinOnly {
            val generic = paramType.data.generics.single()
            assertThat(paramType.link().name).isEqualTo("Array")
            assertThat(generic.link().name).isEqualTo("Int")

            val nestedGeneric = nestedParamType.data.generics.single()
            assertThat(nestedParamType.link().name).isEqualTo("Array")
            assertThat(nestedGeneric.link().name).isEqualTo("Array")
        }
    }

    @Test // go/kotlin-upstream-bug/2207
    fun `Type-target annotations work on unresolved return types`() {
        val paramString = """
            |annotation class Squark
            |fun foo(): @Squark String? = null
        """.render().returnType()
        val paramUnresolved = """
            |annotation class Squark
            |fun foo(): @Squark Unresolved? = null
        """.render().returnType()

        assertThat(paramString.data.type.data.name).isEqualTo("String")
        assertThat(paramUnresolved.data.type.data.name).isEqualTo("<Error class: unknown class>")

        for (param in listOf(paramString/*, paramUnresolved*/)) {
            assertThat(param.annotations.size).isEqualTo(1)
            assertThat(param.nullable).isTrue()
            assertThat(param.annotations.first().name).isEqualTo("Squark")
        }
    }

    @Test
    fun `Nullability is correct on implicit return type`() {
        val returnFoo = """
            |fun foo() = ""
        """.render().returnType("foo")
        val returnCompanion = """
            |companion object {
            |    fun static() = "jvm"
            |}
        """.render().returnType("static")
        val returnBar = """
            |fun bar() = if (1 == 2) "bbb" else null
        """.render().returnType("bar")

        assertThat(returnFoo.nullable).isFalse()
        assertThat(returnCompanion.nullable).isFalse()
        assertThat(returnBar.nullable).isTrue()
        javaOnly {
            assertThat(returnFoo.annotations.single().isAtNonNull).isTrue()
            assertThat(returnCompanion.annotations.single().isAtNonNull).isTrue()
        }
        kotlinOnly {
            assertThat(returnFoo.annotations).isEmpty()
            assertThat(returnCompanion.annotations).isEmpty()
        }
    }

    @Test // TODO: implement. Handle generic Star/wrapping Covariance/Contravariance
    fun `Type projections are handled`() {
        val module = """
            |fun foo(retrieveString: List<out String>, storeString: List<in String>, nope: List<*>)
        """.render()
        val typeOut = module.param("retrieveString").data.type
        val typeIn = module.param("storeString").data.type
        val typeStar = module.param("nope").data.type
    }

    @Test
    fun `Collection types are mapped from Java to Kotlin`() {
        val module = """
            |public void foo(java.util.List<String> list, java.util.Map.Entry<String, Int> entry) {}
        """.render(java = true)

        kotlinOnly {
            val list = module.param("list").data.type
            assertThat(list).isInstanceOf(MappedTypeProjectionComponent::class.java)
            assertThat(list.link().name).isEqualTo("List")
            assertThat(list.link().url).isEqualTo(
                "https://kotlinlang.org/api/latest/" +
                    "jvm/stdlib/kotlin.collections/-list/index.html"
            )
            assertThat(list.alternativeLink()?.url).isEqualTo(
                "https://kotlinlang.org/api/latest/" +
                    "jvm/stdlib/kotlin.collections/-mutable-list/index.html"
            )

            val entry = module.param("entry").data.type
            assertThat(entry).isInstanceOf(MappedTypeProjectionComponent::class.java)
            assertThat(entry.link().name).isEqualTo("Map.Entry")
            assertThat(entry.link().url).isEqualTo(
                "https://kotlinlang.org/api/latest/" +
                    "jvm/stdlib/kotlin.collections/-map/-entry/index.html"
            )
            assertThat(entry.alternativeLink()?.url).isEqualTo(
                "https://kotlinlang.org/api/latest" +
                    "/jvm/stdlib/kotlin.collections/-mutable-map/-mutable-entry/index.html"
            )
        }

        javaOnly {
            val list = module.param("list").data.type
            assertThat(list).isNotInstanceOf(MappedTypeProjectionComponent::class.java)
            assertThat(list.link().name).isEqualTo("List")
            assertThat(list.link().url).isEqualTo(
                "https://developer.android.com/reference/java/util/List.html"
            )

            val entry = module.param("entry").data.type
            assertThat(entry).isNotInstanceOf(MappedTypeProjectionComponent::class.java)
            assertThat(entry.link().name).isEqualTo("Map.Entry")
            assertThat(entry.link().url).isEqualTo(
                "https://developer.android.com/reference/java/util/Map.Entry.html"
            )
        }
    }

    @Test
    fun `Collection type mapping test from CoordinatorLayout`() {
        val returnType = """
            |@NonNull
            |public List<View> getDependents(@NonNull View children) {
            |    return Collections.<View>emptyList();
            |}
        """.renderJava(imports = listOf("import java.util.List"))
            .returnType(functionName = "getDependents")
        kotlinOnly {
            assertThat(returnType).isInstanceOf(MappedTypeProjectionComponent::class.java)
            assertThat(returnType.link().name).isEqualTo("List")
            assertThat(returnType.link().url).isEqualTo(
                "https://kotlinlang.org/api/latest/" +
                    "jvm/stdlib/kotlin.collections/-list/index.html"
            )
            assertThat(returnType.alternativeLink()?.url).isEqualTo(
                "https://kotlinlang.org/api/latest/" +
                    "jvm/stdlib/kotlin.collections/-mutable-list/index.html"
            )
        }
    }

    @Test
    fun `Kotlin collection types are mapped only in Java`() {
        val module = """
            |fun foo(list: java.util.List<String>) {}
        """.render()

        val list = module.param("list").data.type
        assertThat(list).isNotInstanceOf(MappedTypeProjectionComponent::class.java)

        javaOnly {
            assertThat(list.link().url)
                .isEqualTo("https://developer.android.com/reference/java/util/List.html")
        }
    }

    @Test
    fun `Nullable kotlin primitive converted to nullable java boxed primitive`() {
        val paramType = """
            |fun aFunction(foo: Int?)
        """.render().param("foo").data.type
        assertThat(paramType.nullable).isTrue()

        javaOnly {
            assertThat(paramType.name()).isEqualTo("Integer")
            "java/lang/Integer" in paramType.data.type.data.url
        }
        kotlinOnly {
            assertThat(paramType.name()).isEqualTo("Int")
            "kotlin/kotlin/Int" in paramType.data.type.data.url
        }
    }

    private fun DModule.param(name: String = "foo", forSummary: Boolean = false):
        ParameterComponent {
        val (_, pathProvider) = holderAndProvider(this)
        val converter = ParameterDocumentableConverter(
            displayLanguage,
            pathProvider
        )
        return converter.componentForParameter(
            param = parameterDoc(name),
            isSummary = forSummary,
            isFromJava = function()!!.isFromJava(),
            parent = function()!!
        )
    }

    private fun DModule.returnType(functionName: String = "foo"): TypeProjectionComponent {
        val (_, pathProvider) = holderAndProvider(this)
        val converter = ParameterDocumentableConverter(
            displayLanguage,
            pathProvider
        )
        return converter.componentForProjection(
            projection = function(functionName)!!.type,
            // Propagate ALL annotations _for display in the summary_, b/197321617
            propagatedAnnotations = emptyList(),
            isReturnType = true,
            isJavaSource = function(functionName)!!.isFromJava(),
            sourceSet = getExpectOrCommonSourceSet()
        )
    }

    private fun DModule.parameterDoc(name: String = "foo"): DParameter =
        function()!!.parameters.singleOrNull { it.name == name } ?: function()!!.parameters.single()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
