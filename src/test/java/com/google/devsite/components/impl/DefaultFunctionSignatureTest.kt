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
import com.google.devsite.components.testing.NoopLink
import com.google.devsite.components.testing.NoopParameter
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
                parameters = listOf(NoopParameter("String foo"), NoopParameter("int bar"))
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
                    NoopParameter("String foo"),
                    NoopParameter("int bar")
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
                receiver = NoopParameter("String")
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
                    NoopParameter("String foo", forceBreak = true),
                    NoopParameter("int bar", forceBreak = true)
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
}
