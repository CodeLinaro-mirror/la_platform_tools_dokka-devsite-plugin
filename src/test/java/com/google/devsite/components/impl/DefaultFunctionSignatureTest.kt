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

package com.google.devsite.components.impl

import com.google.common.truth.Truth.assertThat
import com.google.devsite.components.symbols.FunctionSignature.Params
import com.google.devsite.components.symbols.TypeParameterComponent
import com.google.devsite.components.testing.NoopLink
import com.google.devsite.components.testing.NoopParameterComponent
import com.google.devsite.components.testing.NoopTypeProjectionComponent
import com.google.devsite.renderer.Language
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultFunctionSignatureTest {
    @Test
    fun `Signature with no params renders correctly`() {
        val component = DefaultFunctionSignature(
            Params(
                name = NoopLink("foo")
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>foo()</div>
            """.trim()
        )
    }

    @Test
    fun `Signature with params renders correctly`() {
        val component = DefaultFunctionSignature(
            Params(
                name = NoopLink("foo"),
                parameters = listOf(
                    NoopParameterComponent("String foo"),
                    NoopParameterComponent("int bar")
                )
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>foo(String foo,&nbsp;int bar)</div>
            """.trim()
        )
    }

    @Test
    fun `Signature with deprecation renders correctly`() {
        val component = DefaultFunctionSignature(
            Params(
                name = NoopLink("foo"),
                parameters = listOf(
                    NoopParameterComponent("String foo"),
                    NoopParameterComponent("int bar")
                ),
                isDeprecated = true
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div><span><del>foo</del></span>(String foo,&nbsp;int bar)</div>
            """.trim()
        )
    }

    @Test
    fun `Signature with receiver renders correctly`() {
        val component = DefaultFunctionSignature(
            Params(
                name = NoopLink("foo"),
                receiver = NoopParameterComponent("String")
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>String.foo()</div>
            """.trim()
        )
    }

    @Test
    fun `Signature with breaks renders correctly`() {
        val component = DefaultFunctionSignature(
            Params(
                name = NoopLink("foo"),
                parameters = listOf(
                    NoopParameterComponent("String foo", forceBreak = true),
                    NoopParameterComponent("int bar", forceBreak = true)
                )
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>foo(<br>&nbsp;&nbsp;&nbsp;&nbsp;String foo,<br>&nbsp;&nbsp;&nbsp;&nbsp;int bar<br>)</div>
            """.trim()
        )
    }

    @Test
    fun `Signature with multiple bounds renders correctly`() {
        for (language in listOf(Language.KOTLIN, Language.JAVA)) {
            val component = DefaultFunctionSignature(
                Params(
                    name = NoopLink("copyWhenGreater"),
                    parameters = listOf(
                        NoopParameterComponent("list: List<T>", forceBreak = true),
                        NoopParameterComponent("T threshold", forceBreak = true)
                    ),
                    typeParameters = listOf(
                        DefaultTypeParameterComponent(
                            TypeParameterComponent.Params(
                                displayLanguage = language,
                                name = "T",
                                projections = listOf(
                                    NoopTypeProjectionComponent("CharSequence"),
                                    NoopTypeProjectionComponent("Comparable<T>")
                                )
                            )
                        )
                    )
                )
            )

            val output = createHTML().div {
                component.render(this)
            }.trim()

            if (language == Language.KOTLIN) {
                // language=html
                assertThat(output).isEqualTo(
                    """
<div>&lt;T&nbsp;:&nbsp;CharSequence&nbsp;&amp;&nbsp;Comparable&lt;T&gt;&gt; copyWhenGreater(<br>&nbsp;&nbsp;&nbsp;&nbsp;list: List&lt;T&gt;,<br>&nbsp;&nbsp;&nbsp;&nbsp;T threshold<br>)</div>
            """.trim()
                )
            } else {
                // language=html
                assertThat(output).isEqualTo(
                    """
<div>&lt;T&nbsp;extends&nbsp;CharSequence&nbsp;&amp;&nbsp;Comparable&lt;T&gt;&gt; copyWhenGreater(<br>&nbsp;&nbsp;&nbsp;&nbsp;list: List&lt;T&gt;,<br>&nbsp;&nbsp;&nbsp;&nbsp;T threshold<br>)</div>
            """.trim()
                )
            }
        }
    }
}
