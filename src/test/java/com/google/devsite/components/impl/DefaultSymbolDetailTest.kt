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
import com.google.devsite.components.symbols.SymbolDetail.Params
import com.google.devsite.components.symbols.SymbolDetail.SymbolType
import com.google.devsite.components.testing.NoopAnnotation
import com.google.devsite.components.testing.NoopContextFreeComponent
import com.google.devsite.components.testing.NoopFunctionSignature
import com.google.devsite.components.testing.NoopParameter
import com.google.devsite.renderer.Language
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultSymbolDetailTest {
    @Test
    fun `Simple Java function renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                displayLanguage = Language.JAVA,
                name = "foo",
                anchors = linkedSetOf(),
                returnType = NoopParameter("void"),
                symbolType = SymbolType.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                metadata = emptyList()
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div>
    <h3 class="api-name">foo</h3>
    <pre class="api-signature no-pretty-print">void&nbsp;foo()</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Simple Kotlin function renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                displayLanguage = Language.KOTLIN,
                name = "foo",
                anchors = linkedSetOf(),
                returnType = NoopParameter("Unit"),
                symbolType = SymbolType.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                metadata = emptyList()
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div>
    <h3 class="api-name">foo</h3>
    <pre class="api-signature no-pretty-print">fun&nbsp;foo():&nbsp;Unit</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Simple Kotlin property renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                displayLanguage = Language.KOTLIN,
                name = "foo",
                anchors = linkedSetOf(),
                returnType = NoopParameter("Unit"),
                symbolType = SymbolType.PROPERTY,
                signature = NoopFunctionSignature("foo"),
                metadata = emptyList()
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div>
    <h3 class="api-name">foo</h3>
    <pre class="api-signature no-pretty-print">val&nbsp;foo:&nbsp;Unit</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Java function with annotations renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                displayLanguage = Language.JAVA,
                name = "foo",
                anchors = linkedSetOf(),
                annotations = listOf(NoopAnnotation("@Foo"), NoopAnnotation("@Bar")),
                returnType = NoopParameter("void"),
                symbolType = SymbolType.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                metadata = emptyList()
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div>
    <h3 class="api-name">foo</h3>
    <pre class="api-signature no-pretty-print">@Foo<br>@Bar<br>void&nbsp;foo()</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Java function with modifiers renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                displayLanguage = Language.JAVA,
                name = "foo",
                anchors = linkedSetOf(),
                modifiers = listOf("protected", "abstract"),
                returnType = NoopParameter("void"),
                symbolType = SymbolType.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                metadata = emptyList()
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div>
    <h3 class="api-name">foo</h3>
    <pre class="api-signature no-pretty-print">protected&nbsp;abstract&nbsp;void&nbsp;foo()</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin function with modifiers renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                displayLanguage = Language.KOTLIN,
                name = "foo",
                anchors = linkedSetOf(),
                modifiers = listOf("protected", "abstract"),
                returnType = NoopParameter("Unit"),
                symbolType = SymbolType.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                metadata = emptyList()
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div>
    <h3 class="api-name">foo</h3>
    <pre class="api-signature no-pretty-print">protected&nbsp;abstract&nbsp;fun&nbsp;foo():&nbsp;Unit</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Function anchors render correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                displayLanguage = Language.JAVA,
                name = "foo",
                anchors = linkedSetOf("foo(a,b)", "foo(a, b)", "foo-a-b-"),
                returnType = NoopParameter("void"),
                symbolType = SymbolType.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                metadata = emptyList()
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div><a name="foo(a, b)"></a><a name="foo-a-b-"></a>
    <h3 class="api-name" id="foo(a,b)">foo</h3>
    <pre class="api-signature no-pretty-print">void&nbsp;foo()</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Function with metadata renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                displayLanguage = Language.JAVA,
                name = "foo",
                anchors = linkedSetOf(),
                returnType = NoopParameter("void"),
                symbolType = SymbolType.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                metadata = listOf(NoopContextFreeComponent, NoopContextFreeComponent)
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div>
    <h3 class="api-name">foo</h3>
    <pre class="api-signature no-pretty-print">void&nbsp;foo()</pre>
    <div>noop</div>
    <div>noop</div>
  </div>
</div>
            """.trim()
        )
    }
}
