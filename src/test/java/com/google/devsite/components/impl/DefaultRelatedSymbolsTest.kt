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
import com.google.devsite.components.table.RelatedSymbols.Params
import com.google.devsite.components.testing.NoopLink
import com.google.devsite.components.testing.NoopSummaryList
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultRelatedSymbolsTest {
    @Test
    fun `Empty related symbols renders correctly`() {
        val component =
            DefaultRelatedSymbols(
                Params(
                    directSubclasses = emptyList(),
                    directSummary = NoopSummaryList(show = false),
                    indirectSubclasses = emptyList(),
                    indirectSummary = NoopSummaryList(show = false),
                )
            )

        val output = createHTML().div { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<div></div>
            """
                    .trim()
            )
    }

    @Test
    fun `Related symbols with direct subclasses renders correctly`() {
        val component =
            DefaultRelatedSymbols(
                Params(
                    directSubclasses = listOf(NoopLink("abc")),
                    directSummary = NoopSummaryList(),
                    indirectSubclasses = emptyList(),
                    indirectSummary = NoopSummaryList(show = false),
                )
            )

        val output = createHTML().div { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<div>
  <div class="devsite-table-wrapper"><devsite-expandable><span class="expand-control jd-sumtable-subclasses">Known direct subclasses
      <div class="showalways" id="subclasses-direct">abc</div>
    </span>
    <div id="subclasses-direct-summary">
      <div>noop</div>
    </div>
</devsite-expandable>  </div>
</div>
            """
                    .trim()
            )
    }

    @Test
    fun `Related symbols with indirect subclasses renders correctly`() {
        val component =
            DefaultRelatedSymbols(
                Params(
                    directSubclasses = emptyList(),
                    directSummary = NoopSummaryList(show = false),
                    indirectSubclasses = listOf(NoopLink("abc")),
                    indirectSummary = NoopSummaryList(),
                )
            )

        val output = createHTML().div { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<div>
  <div class="devsite-table-wrapper"><devsite-expandable><span class="expand-control jd-sumtable-subclasses">Known indirect subclasses
      <div class="showalways" id="subclasses-indirect">abc</div>
    </span>
    <div id="subclasses-indirect-summary">
      <div>noop</div>
    </div>
</devsite-expandable>  </div>
</div>
            """
                    .trim()
            )
    }
}
