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
import com.google.devsite.components.FunctionSummary.Params
import com.google.devsite.components.testing.NoopDocumentation
import com.google.devsite.components.testing.NoopFunctionSignature
import com.google.devsite.components.testing.NoopParameterType
import kotlinx.html.stream.createHTML
import kotlinx.html.table
import kotlinx.html.tr
import org.junit.Test

class DefaultFunctionSummaryTest {
    @Test
    fun `Simple function summary renders correctly`() {
        val component = DefaultFunctionSummary(
            Params(
                returnType = NoopParameterType("Unit"),
                signature = NoopFunctionSignature("foo()"),
                description = NoopDocumentation("This method does baz.")
            )
        )

        val output = createHTML().table {
            tr { component.render(this) }
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<table>
  <tr>
    <td><code>Unit</code></td>
    <td width="100%">
      <div><code>foo()</code></div>
      <p>This method does baz.</p>
    </td>
  </tr>
</table>
            """.trim()
        )
    }

    @Test
    fun `Function summary with modifiers renders correctly`() {
        val component = DefaultFunctionSummary(
            Params(
                modifiers = listOf("open", "suspend"),
                returnType = NoopParameterType("Unit"),
                signature = NoopFunctionSignature("foo()"),
                description = NoopDocumentation("This method does baz.")
            )
        )

        val output = createHTML().table {
            tr { component.render(this) }
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<table>
  <tr>
    <td><code>open&nbsp;suspend&nbsp;Unit</code></td>
    <td width="100%">
      <div><code>foo()</code></div>
      <p>This method does baz.</p>
    </td>
  </tr>
</table>
            """.trim()
        )
    }
}
