/*
 * Copyright 2026 The Android Open Source Project
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
import com.google.devsite.components.pages.FunctionGroupPage
import com.google.devsite.components.testing.NoopSummaryList
import com.google.devsite.components.testing.NoopSymbolDetailF
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultFunctionGroupPageTest {
    @Test
    fun `Test function group page`() {
        val component =
            DefaultFunctionGroupPage(
                FunctionGroupPage.Params(
                    summary = NoopSummaryList(),
                    detail = listOf(NoopSymbolDetailF, NoopSymbolDetailF),
                )
            )

        val output = createHTML().div { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
                <div>
                  <h2>Functions summary</h2>
                  <div>noop</div>
                  <h2>Functions</h2>
                  <div>noop</div>
                  <div>noop</div>
                </div>
                """
                    .trimIndent()
            )
    }
}
