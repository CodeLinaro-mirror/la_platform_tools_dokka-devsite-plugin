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
import com.google.devsite.components.table.SummaryList.Params
import com.google.devsite.components.testing.NoopSummaryItem
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultSummaryListTest {
    @Test
    fun `Empty summary renders correctly`() {
        val component = DefaultSummaryList(Params(items = emptyList()))

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div></div>
            """.trim()
        )
    }

    @Test
    fun `Simple summary renders correctly`() {
        val component = DefaultSummaryList(Params(items = listOf(NoopSummaryItem, NoopSummaryItem)))

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="devsite-table-wrapper">
    <table class="responsive">
      <tbody>
        <tr><noop/></tr>
        <tr><noop/></tr>
      </tbody>
    </table>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Summary with header renders correctly`() {
        val component =
            DefaultSummaryList(Params(header = NoopSummaryItem, listOf(NoopSummaryItem)))

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="devsite-table-wrapper">
    <table class="responsive">
      <thead>
        <tr><noop/></tr>
      </thead>
      <tbody>
        <tr><noop/></tr>
      </tbody>
    </table>
  </div>
</div>
            """.trim()
        )
    }
}
