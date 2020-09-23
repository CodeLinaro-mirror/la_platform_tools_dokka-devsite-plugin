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
import com.google.devsite.components.table.ClassHierarchy.Params
import com.google.devsite.components.testing.NoopLink
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultClassHierarchyTest {
    @Test
    fun `Empty class hierarchy renders correctly`() {
        val component = DefaultClassHierarchy(Params(emptyList()))

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body></body>
            """.trim()
        )
    }

    @Test
    fun `Class hierarchy with one parent renders correctly`() {
        val component = DefaultClassHierarchy(Params(listOf(NoopLink("a"))))

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <div class="devsite-table-wrapper">
    <table class="jd-inheritance-table">
      <tbody>
        <tr>
          <td colspan="1">a</td>
        </tr>
      </tbody>
    </table>
  </div>
</body>
            """.trim()
        )
    }

    @Test
    fun `Class hierarchy with two parents renders correctly`() {
        val component = DefaultClassHierarchy(Params(listOf(NoopLink("a"), NoopLink("b"))))

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <div class="devsite-table-wrapper">
    <table class="jd-inheritance-table">
      <tbody>
        <tr>
          <td colspan="2">a</td>
        </tr>
        <tr>
          <td class="jd-inheritance-space">&nbsp;&nbsp;&nbsp;↳</td>
          <td colspan="1">b</td>
        </tr>
      </tbody>
    </table>
  </div>
</body>
            """.trim()
        )
    }

    @Test
    fun `Class hierarchy with many parents renders correctly`() {
        val component = DefaultClassHierarchy(
            Params(
                listOf(
                    NoopLink("a"),
                    NoopLink("b"),
                    NoopLink("c"),
                    NoopLink("d")
                )
            )
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <div class="devsite-table-wrapper">
    <table class="jd-inheritance-table">
      <tbody>
        <tr>
          <td colspan="4">a</td>
        </tr>
        <tr>
          <td class="jd-inheritance-space">&nbsp;&nbsp;&nbsp;↳</td>
          <td colspan="3">b</td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td class="jd-inheritance-space">&nbsp;&nbsp;&nbsp;↳</td>
          <td colspan="2">c</td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
          <td class="jd-inheritance-space">&nbsp;&nbsp;&nbsp;↳</td>
          <td colspan="1">d</td>
        </tr>
      </tbody>
    </table>
  </div>
</body>
            """.trim()
        )
    }
}
