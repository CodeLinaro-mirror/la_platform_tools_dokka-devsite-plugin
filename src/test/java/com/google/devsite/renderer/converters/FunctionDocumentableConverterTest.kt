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
import com.google.devsite.components.FunctionSummary
import com.google.devsite.components.Parameter
import com.google.devsite.components.TypeSummary
import com.google.devsite.renderer.Language
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
    @Test
    fun `Top level function summary component has correct default modifiers`() {
        val source = """
            |fun foo() = Unit
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val type = summary.data.title as TypeSummary

            assertThat(type.data.modifiers).containsExactly("final")
        }
    }

    @Test
    fun `Function summary component ignores public modifier`() {
        val source = """
            |public fun foo() = Unit
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val type = summary.data.title as TypeSummary

            assertThat(type.data.modifiers).containsExactly("final")
        }
    }

    @Test
    fun `Function summary component has suspend modifier`() {
        val source = """
            |suspend fun foo() = Unit
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val type = summary.data.title as TypeSummary

            assertThat(type.data.modifiers).containsExactly("final", "suspend")
        }
    }

    @Test
    fun `Function summary component has inline modifier`() {
        val source = """
            |inline fun foo() = Unit
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val type = summary.data.title as TypeSummary

            assertThat(type.data.modifiers).containsExactly("final", "inline")
        }
    }

    @Ignore // TODO(b/165112358): foo doesn't show up in the dokka model
    @Test
    fun `Function summary component in abstract class has protected modifier`() {
        val source = """
            |abstract class Foo {
            |    protected open fun foo() = Unit
            |}
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function(fromClass = true))
            val type = summary.data.title as TypeSummary

            assertThat(type.data.modifiers).containsExactly("protected")
        }
    }

    @Test
    fun `Function summary component in abstract class has abstract modifier`() {
        val source = """
            |abstract class Foo {
            |    abstract fun foo()
            |}
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function(fromClass = true))
            val type = summary.data.title as TypeSummary

            assertThat(type.data.modifiers).containsExactly("abstract")
        }
    }

    @Test
    fun `Function summary component in class has open modifiers`() {
        val source = """
            |class Foo {
            |    open fun foo() = Unit
            |}
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function(fromClass = true))
            val type = summary.data.title as TypeSummary

            assertThat(type.data.modifiers).containsExactly("open")
        }
    }

    @Test
    fun `Function summary component in interface has abstract modifiers`() {
        val source = """
            |interface Foo {
            |    fun foo()
            |}
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function(fromClass = true))
            val type = summary.data.title as TypeSummary

            assertThat(type.data.modifiers).containsExactly("abstract")
        }
    }

    @Test
    fun `Function summary component creates return type link`() {
        val source = """
            |class A
            |fun foo(): A
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val type = summary.data.title as TypeSummary
            val returnType = type.data.type
            val link = returnType.data.type

            assertThat(link.data.name).isEqualTo("A")
            assertPath(link.data.url, "androidx/example/A.html")
        }
    }

    @Test
    fun `Function summary component creates return type generics`() {
        val source = """
            |fun foo(): Map<String, List<Int>>
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val type = summary.data.title as TypeSummary
            val returnType = type.data.type
            val generics = returnType.data.generics

            assertThat(generics).hasSize(2)
            assertThat(generics.first().data.type.data.name).isEqualTo("String")
            assertThat(generics.last().data.type.data.name).isEqualTo("List")

            val nestedGenerics = generics.last().data.generics
            assertThat(nestedGenerics).hasSize(1)
            assertThat(nestedGenerics.single().data.type.data.name).isEqualTo("Int")
        }
    }

    @Test
    fun `Function summary component creates signature with name`() {
        val source = """
            |fun iAmACoolFunction()
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature

            assertThat(signature.data.name.data.name).isEqualTo("iAmACoolFunction")
        }
    }

    @Test
    fun `Function summary component creates extension receiver`() {
        val source = """
            |fun String.foo()
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature

            val param = when (language) {
                Language.JAVA -> {
                    assertThat(signature.data.receiver).isNull()
                    signature.data.parameters.single()
                }
                Language.KOTLIN -> {
                    assertThat(signature.data.receiver).isNotNull()
                    signature.data.receiver!!
                }
            }

            val type = param.data.primary

            assertNoLambdaStuff(param.data)
            assertThat(type.data.type.data.name).isEqualTo("String")
            when (language) {
                Language.JAVA -> assertThat(param.data.name).isEqualTo("receiver")
                Language.KOTLIN -> assertThat(param.data.name).isEmpty()
            }
        }
    }

    @Test
    fun `Function summary component creates params`() {
        val source = """
            |fun foo(a: String)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
            val param = signature.data.parameters.single()
            val type = param.data.primary

            assertNoLambdaStuff(param.data)
            assertThat(param.data.name).isEqualTo("a")
            assertThat(type.data.type.data.name).isEqualTo("String")
        }
    }

    @Ignore // TODO(b/165139177): figure out correct implementation
    @Test
    fun `Function summary component handles nullable types`() {
        val source = """
            |fun Int?.foo(a: List<String?>?)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
        }
    }

    @Test
    fun `Function summary component creates params with generics`() {
        val source = """
            |fun foo(a: List<Int>)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
            val param = signature.data.parameters.single()
            val generics = param.data.primary.data.generics

            assertThat(generics).hasSize(1)
            assertThat(generics.single().data.type.data.name).isEqualTo("Int")
        }
    }

    @Ignore // TODO(asaveau): figure out inline generics
    @Test
    fun `Function summary component creates params with inline generics and generic param`() {
        val source = """
            |fun <T> foo(a: List<T>)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
            val param = signature.data.parameters.single()
        }
    }

    @Ignore // TODO(asaveau): figure out inline generics
    @Test
    fun `Function summary component creates params with inline generics param`() {
        val source = """
            |fun <T> foo(a: T)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
            val param = signature.data.parameters.single()
        }
    }

    @Ignore // TODO(asaveau): figure out inline generics
    @Test
    fun `Function summary component creates params with inline generics receiver`() {
        val source = """
            |fun <T> T.foo()
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
        }
    }

    @Ignore // TODO(asaveau): figure out inline generics
    @Test
    fun `Function summary component creates params with inline generics return type`() {
        val source = """
            |fun <T> foo(): T
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
        }
    }

    @Test
    fun `Function summary component creates factory lambda param`() {
        val source = """
            |fun foo(a: () -> Unit)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
            val param = signature.data.parameters.single()

            javaOnly {
                assertNoLambdaStuff(param.data)

                val primary = param.data.primary.data
                assertThat(primary.type.data.name).isEqualTo("Function0")
                assertThat(primary.generics.single().data.type.data.name).isEqualTo("Unit")
            }
            kotlinOnly {
                assertThat(param.data.isLambda).isTrue()
                assertThat(param.data.receiver).isNull()
                assertThat(param.data.lambdaModifiers).isEmpty()
                assertThat(param.data.lambdaParams).isEmpty()
                assertThat(param.data.primary.data.type.data.name).isEqualTo("Unit")
            }
        }
    }

    @Test
    fun `Function summary component creates suspend lambda param`() {
        val source = """
            |fun foo(a: suspend () -> Unit)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
            val param = signature.data.parameters.single()

            javaOnly {
                assertNoLambdaStuff(param.data)

                val primary = param.data.primary.data
                assertThat(primary.type.data.name).isEqualTo("SuspendFunction0")
                assertThat(primary.generics.single().data.type.data.name).isEqualTo("Unit")
            }
            kotlinOnly {
                assertThat(param.data.isLambda).isTrue()
                assertThat(param.data.receiver).isNull()
                assertThat(param.data.lambdaParams).isEmpty()
                assertThat(param.data.lambdaModifiers).containsExactly("suspend")
                assertThat(param.data.primary.data.type.data.name).isEqualTo("Unit")
            }
        }
    }

    @Ignore // TODO(b/165709374): dokka doesn't understand suspending lambda receivers
    @Test
    fun `Function summary component creates suspend lambda param with receiver`() {
        val source = """
            |fun foo(a: suspend Float.() -> Unit)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
            val param = signature.data.parameters.single()

            javaOnly {
                assertNoLambdaStuff(param.data)
                assertThat(param.data.primary.data.type.data.name).isEqualTo("")
            }
            kotlinOnly {
                assertThat(param.data.isLambda).isTrue()
                assertThat(param.data.receiver).isNotNull()
                assertThat(param.data.receiver!!.data.type.data.name).isEqualTo("Float")
                assertThat(param.data.lambdaModifiers).containsExactly("suspend")
                assertThat(param.data.lambdaParams).isEmpty()
            }
        }
    }

    @Test
    fun `Function summary component creates suspend lambda param with params`() {
        val source = """
            |fun foo(a: suspend (Float) -> Unit)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
            val param = signature.data.parameters.single()

            javaOnly {
                assertNoLambdaStuff(param.data)

                val primary = param.data.primary.data
                assertThat(primary.type.data.name).isEqualTo("SuspendFunction1")
                assertThat(primary.generics.first().data.type.data.name).isEqualTo("Float")
                assertThat(primary.generics.last().data.type.data.name).isEqualTo("Unit")
            }
            kotlinOnly {
                assertThat(param.data.isLambda).isTrue()
                assertThat(param.data.receiver).isNull()
                assertThat(param.data.lambdaModifiers).containsExactly("suspend")
                assertThat(param.data.lambdaParams).hasSize(1)
                assertThat(param.data.lambdaParams.single().data.type.data.name).isEqualTo("Float")
            }
        }
    }

    @Test
    fun `Function summary component creates lambda param`() {
        val source = """
            |fun foo(a: (String) -> Unit)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
            val param = signature.data.parameters.single()

            javaOnly {
                assertNoLambdaStuff(param.data)

                val primary = param.data.primary.data
                assertThat(primary.type.data.name).isEqualTo("Function1")
                assertThat(primary.generics.first().data.type.data.name).isEqualTo("String")
                assertThat(primary.generics.last().data.type.data.name).isEqualTo("Unit")
            }
            kotlinOnly {
                assertThat(param.data.isLambda).isTrue()
                assertThat(param.data.receiver).isNull()
                assertThat(param.data.lambdaModifiers).isEmpty()
                assertThat(param.data.lambdaParams).hasSize(1)
                assertThat(param.data.lambdaParams.single().data.type.data.name).isEqualTo("String")
            }
        }
    }

    @Test
    fun `Function summary component creates lambda param with receiver`() {
        val source = """
            |fun foo(a: Float.() -> Unit)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
            val param = signature.data.parameters.single()

            javaOnly {
                assertNoLambdaStuff(param.data)

                val primary = param.data.primary.data
                assertThat(primary.type.data.name).isEqualTo("Function1")
                assertThat(primary.generics.first().data.type.data.name).isEqualTo("Float")
                assertThat(primary.generics.last().data.type.data.name).isEqualTo("Unit")
            }
            kotlinOnly {
                assertThat(param.data.isLambda).isTrue()
                assertThat(param.data.receiver).isNotNull()
                assertThat(param.data.receiver!!.data.type.data.name).isEqualTo("Float")
                assertThat(param.data.lambdaModifiers).isEmpty()
                assertThat(param.data.lambdaParams).isEmpty()
            }
        }
    }

    @Test
    fun `Function summary component creates lambda param with difficult generic combination`() {
        val source = """
            |fun foo(block: Int.(Map<String, Int>, Double) -> Collection<Float>)
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature
            val param = signature.data.parameters.single()

            javaOnly {
                assertNoLambdaStuff(param.data)

                val primary = param.data.primary.data
                assertThat(primary.type.data.name).isEqualTo("Function3")
                assertThat(primary.generics).hasSize(4)
                assertThat(primary.generics[0].data.type.data.name).isEqualTo("Int")
                assertThat(primary.generics[1].data.type.data.name).isEqualTo("Map")
                assertThat(primary.generics[2].data.type.data.name).isEqualTo("Double")
                assertThat(primary.generics[3].data.type.data.name).isEqualTo("Collection")

                val mapGenerics = primary.generics[1].data.generics
                assertThat(mapGenerics).hasSize(2)
                assertThat(mapGenerics.first().data.type.data.name).isEqualTo("String")
                assertThat(mapGenerics.last().data.type.data.name).isEqualTo("Int")

                val collectionGenerics = primary.generics[3].data.generics
                assertThat(collectionGenerics).hasSize(1)
                assertThat(collectionGenerics.single().data.type.data.name).isEqualTo("Float")
            }
            kotlinOnly {
                assertThat(param.data.isLambda).isTrue()
                assertThat(param.data.receiver).isNotNull()
                assertThat(param.data.receiver!!.data.type.data.name).isEqualTo("Int")
                assertThat(param.data.lambdaModifiers).isEmpty()
                assertThat(param.data.lambdaParams).hasSize(2)
                assertThat(param.data.lambdaParams.first().data.type.data.name).isEqualTo("Map")
                assertThat(param.data.lambdaParams.last().data.type.data.name).isEqualTo("Double")
                assertThat(param.data.primary.data.type.data.name).isEqualTo("Collection")

                val mapGenerics = param.data.lambdaParams.first().data.generics
                assertThat(mapGenerics).hasSize(2)
                assertThat(mapGenerics.first().data.type.data.name).isEqualTo("String")
                assertThat(mapGenerics.last().data.type.data.name).isEqualTo("Int")

                val collectionGenerics = param.data.primary.data.generics
                assertThat(collectionGenerics).hasSize(1)
                assertThat(collectionGenerics.single().data.type.data.name).isEqualTo("Float")
            }
        }
    }

    @Test
    fun `Function summary component has correct relative link`() {
        val source = """
            |fun List<String>.foo(a: Map<String, Int>, block: String.(Float) -> Double) = Unit
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = FunctionDocumentableConverter(language, pathProvider())

            val summary = converter.summary(root.function())
            val breakdown = summary.data.description as FunctionSummary
            val signature = breakdown.data.signature

            assertThat(signature.data.name.data.url)
                .isEqualTo("#foo(kotlin.collections.List,kotlin.collections.Map,kotlin.Function2)")
        }
    }

    private fun assertNoLambdaStuff(data: Parameter.Params) {
        assertThat(data.isLambda).isFalse()
        assertThat(data.receiver).isNull()
        assertThat(data.lambdaParams).isEmpty()
        assertThat(data.lambdaModifiers).isEmpty()
    }

    private fun RootPageNode.function(fromClass: Boolean = false): DFunction {
        val packageDoc = children
            .filterIsInstance<PackagePageNode>().single()
            .documentable as DPackage

        return if (fromClass) {
            packageDoc.classlikes.single().functions.single { it.name == "foo" }
        } else {
            packageDoc.functions.single()
        }
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
