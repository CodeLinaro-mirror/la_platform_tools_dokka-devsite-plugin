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
import com.google.devsite.components.symbols.TypeSummary.Params
import com.google.devsite.components.testing.NoopTypeProjectionComponent
import com.google.devsite.renderer.converters.Modifiers
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultTypeSummaryTest {
    @Test
    fun `Simple type renders correctly`() {
        val component = DefaultTypeSummary(
            Params(
                type = NoopTypeProjectionComponent("Unit")
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>Unit</div>
            """.trim()
        )
    }

    @Test
    fun `Type with modifiers renders correctly`() {
        val component = DefaultTypeSummary(
            Params(
                modifiers = Modifiers("open", "suspend"),
                type = NoopTypeProjectionComponent("Unit")
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>open&nbsp;suspend Unit</div>
            """.trim()
        )
    }
}
