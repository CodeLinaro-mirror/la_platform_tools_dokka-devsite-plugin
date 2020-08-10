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
import com.google.devsite.components.SummaryItem.Params
import com.google.devsite.components.testing.PlainTextOutput
import kotlinx.html.stream.createHTML
import kotlinx.html.tbody
import org.junit.Test

class DefaultSummaryItemTest {
    @Test
    fun `Empty item renders correctly`() {
        val component = DefaultSummaryItem(Params(PlainTextOutput(""), PlainTextOutput("")))

        val output = createHTML().tbody {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<tbody>
  <tr>
    <td></td>
    <td></td>
  </tr>
</tbody>
            """.trim()
        )
    }

    @Test
    fun `Simple item renders correctly`() {
        val component =
            DefaultSummaryItem(Params(PlainTextOutput("Title"), PlainTextOutput("Description")))

        val b = createHTML().tbody {
            component.render(this)
        }.trim()

        // language=html
        assertThat(b).isEqualTo(
            """
<tbody>
  <tr>
    <td>Title</td>
    <td>Description</td>
  </tr>
</tbody>
            """.trim()
        )
    }
}
