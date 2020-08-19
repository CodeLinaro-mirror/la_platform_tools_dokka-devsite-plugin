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
import com.google.devsite.components.ParameterType.Params
import com.google.devsite.components.testing.NoopLink
import com.google.devsite.components.testing.NoopParameterType
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultParameterTypeTest {
    @Test
    fun `Simple parameter type renders correctly`() {
        val component = DefaultParameterType(
            Params(
                type = NoopLink("Int")
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>Int</div>
            """.trim()
        )
    }

    @Test
    fun `Parameter type with one generic renders correctly`() {
        val component = DefaultParameterType(
            Params(
                type = NoopLink("List"),
                generics = listOf(NoopParameterType("String"))
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>List<span class="symbol">&lt;</span>String<span class="symbol">&gt;</span></div>
            """.trim()
        )
    }

    @Test
    fun `Parameter type with multiple generics renders correctly`() {
        val component = DefaultParameterType(
            Params(
                type = NoopLink("Map"),
                generics = listOf(NoopParameterType("String"), NoopParameterType("Int"))
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>Map<span class="symbol">&lt;</span>String,&nbsp;Int<span class="symbol">&gt;</span></div>
            """.trim()
        )
    }
}
