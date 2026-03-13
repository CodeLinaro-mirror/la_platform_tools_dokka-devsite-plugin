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
import com.google.devsite.components.table.TableRowSummaryItem.Params
import com.google.devsite.components.testing.PlainTextOutput
import kotlinx.html.stream.createHTML
import kotlinx.html.tr
import org.junit.Test

class DefaultTableRowSummaryItemTest {
    @Test
    fun `Empty item renders correctly`() {
        val component = DefaultTableRowSummaryItem(Params(PlainTextOutput(""), PlainTextOutput("")))

        val output = createHTML().tr { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<tr>
  <td><code></code></td>
  <td></td>
</tr>
            """
                    .trim()
            )
    }

    @Test
    fun `Simple item renders correctly`() {
        val component =
            DefaultTableRowSummaryItem(
                Params(PlainTextOutput("Title"), PlainTextOutput("Description"))
            )

        val b = createHTML().tr { component.render(this) }.trim()

        // language=html
        assertThat(b)
            .isEqualTo(
                """
<tr>
  <td><code>Title</code></td>
  <td>Description</td>
</tr>
            """
                    .trim()
            )
    }

    @Test
    fun `Item with anchors renders correctly`() {
        val component =
            DefaultTableRowSummaryItem(
                Params(
                    title = PlainTextOutput("Title"),
                    description = PlainTextOutput("Description"),
                    anchors = setOf("#anchor1", "#anchor2"),
                )
            )
        val row = createHTML().tr { component.render(this) }.trim()

        // language=html
        assertThat(row)
            .isEqualTo(
                """
                <tr>
                  <td><a name="#anchor1"></a><a name="#anchor2"></a><code>Title</code></td>
                  <td>Description</td>
                </tr>
                """
                    .trimIndent()
            )
    }
}
