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
import com.google.devsite.components.Parameter
import com.google.devsite.components.ParameterType
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
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

        val paramType = param.data.primary

        assertThat(paramType.link().name).isEqualTo("String")
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
            |fun foo(a: List<Int>)
        """.render().param()

        val paramType = param.data.primary
        val generic = paramType.data.generics.item()

        assertThat(generic.link().name).isEqualTo("Int")
    }

    @Test
    fun `Parameter understands nested generics`() {
        val param = """
            |fun foo(a: List<Set<Int>>)
        """.render().param()

        val paramType = param.data.primary
        val generic = paramType.data.generics.item()
        val nestedGeneric = generic.data.generics.item()

        assertThat(nestedGeneric.link().name).isEqualTo("Int")
    }

    @Test
    fun `Parameter understands * generics`() {
        val param = """
            |fun foo(a: List<*>)
        """.render().param()

        val paramType = param.data.primary
        val generic = paramType.data.generics.item()

        javaOnly { assertThat(generic.link().name).isEqualTo("?") }
        kotlinOnly { assertThat(generic.link().name).isEqualTo("*") }
    }

    @Test
    fun `Parameter understands lambda with difficult generic combination`() {
        val param = """
            |fun foo(block: Int.(Map<String, Int>, Double) -> Collection<Float>)
        """.render().param().data

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

    private fun DModule.param(forSummary: Boolean = false): Parameter {
        val converter = ParameterDocumentableConverter(language, pathProvider())
        return converter.componentForParameter(parameterDoc(), forSummary)
    }

    private fun DModule.parameterDoc(): DParameter {
        return packages.single().functions.single().parameters.single()
    }

    private fun ParameterType.link(): Link.Params = data.type.data

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
