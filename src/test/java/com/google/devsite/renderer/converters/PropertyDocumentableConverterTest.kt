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
import com.google.devsite.components.Link
import com.google.devsite.components.Parameter
import com.google.devsite.components.ParameterType
import com.google.devsite.components.TwoPaneSummaryItem
import com.google.devsite.components.TypeSummary
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.functionSummary
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DProperty
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

        val property = summary.functionSummary()

        assertThat(property.name()).isEqualTo("iAmACoolProperty")
    }

    @Test
    fun `Property summary component has correct relative link`() {
        val summary = """
            |val <T : Number> List<T>.foo
        """.render().summary()

        val property = summary.functionSummary()
        val signature = property.data.signature

        assertPath(
            signature.data.name.data.url,
            "androidx/example/package-summary.html#foo(kotlin.collections.List)"
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
    fun `Property detail component has correct anchors`() {
        val detail = """
            |val <T : Number> List<T>.foo
        """.render().detail()

        assertThat(detail.data.anchors).containsExactly(
            "foo(kotlin.collections.List)",
            "getFoo(kotlin.collections.List)",
            "setFoo(kotlin.collections.List)",
            "getFoo-kotlin.collections.List-",
            "setFoo-kotlin.collections.List-"
        )
    }

    private fun DModule.summary(fromClass: Boolean = false): TwoPaneSummaryItem {
        val docConverter = DocTagConverter(language, pathProvider())
        val converter = PropertyDocumentableConverter(language, pathProvider(), docConverter)
        return converter.summary(property(fromClass))
    }

    private fun DModule.detail(fromClass: Boolean = false): FunctionDetail {
        val docConverter = DocTagConverter(language, pathProvider())
        val converter = PropertyDocumentableConverter(language, pathProvider(), docConverter)
        return converter.detail(property(fromClass))
    }

    private fun DModule.property(fromClass: Boolean): DProperty {
        val packageDoc = packages.single()

        return if (fromClass) {
            packageDoc.classlikes.single().properties.single { it.name == "foo" }
        } else {
            packageDoc.properties.single()
        }
    }

    private fun Parameter.link(): Link.Params = data.primary.link()
    private fun ParameterType.link(): Link.Params = data.type.data
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
