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
import com.google.devsite.components.Parameter.Params
import com.google.devsite.components.testing.NoopLink
import com.google.devsite.components.testing.NoopParameterType
import com.google.devsite.renderer.Language
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test
import kotlin.test.assertFailsWith

class DefaultParameterTest {
    @Test
    fun `Simple Kotlin parameter renders correctly`() {
        val component = DefaultParameter(
            Params(
                name = "number",
                primary = NoopParameterType("Int"),
                language = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div><span class="identifier">number</span><span class="symbol">:</span>&nbsp;Int</div>
            """.trim()
        )
    }

    @Test
    fun `Simple Java parameter renders correctly`() {
        val component = DefaultParameter(
            Params(
                name = "number",
                primary = NoopParameterType("int"),
                language = Language.JAVA
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>int&nbsp;<span class="identifier">number</span></div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin parameter with annotations renders correctly`() {
        val component = DefaultParameter(
            Params(
                name = "number",
                primary = NoopParameterType("Int"),
                annotations = listOf(NoopLink("@Really"), NoopLink("@Special")),
                language = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>@Really&nbsp;@Special&nbsp;<span class="identifier">number</span><span class="symbol">:</span>&nbsp;Int</div>
            """.trim()
        )
    }

    @Test
    fun `Java parameter with annotations renders correctly`() {
        val component = DefaultParameter(
            Params(
                name = "number",
                primary = NoopParameterType("int"),
                annotations = listOf(NoopLink("@Really"), NoopLink("@Special")),
                language = Language.JAVA
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>@Really&nbsp;@Special&nbsp;int&nbsp;<span class="identifier">number</span></div>
            """.trim()
        )
    }

    @Test
    fun `Java parameter with receiver is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DefaultParameter(
                Params(
                    name = "number",
                    receiver = NoopParameterType("int"),
                    primary = NoopParameterType("int"),
                    language = Language.JAVA
                )
            )
        }
    }

    @Test
    fun `Java parameter with lambda params is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DefaultParameter(
                Params(
                    name = "number",
                    lambdaParams = listOf(NoopParameterType("int")),
                    primary = NoopParameterType("int"),
                    language = Language.JAVA
                )
            )
        }
    }

    @Test
    fun `Kotlin parameter with receiver renders correctly`() {
        val component = DefaultParameter(
            Params(
                name = "number",
                receiver = NoopParameterType("Int"),
                primary = NoopParameterType("Int"),
                language = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div><span class="identifier">number</span><span class="symbol">:</span>&nbsp;Int<span class="symbol">.</span><span class="symbol">(</span><span class="symbol">) &rarr; </span>Int</div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin parameter with lambda params renders correctly`() {
        val component = DefaultParameter(
            Params(
                name = "number",
                lambdaParams = listOf(NoopParameterType("Int"), NoopParameterType("String")),
                primary = NoopParameterType("Int"),
                language = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div><span class="identifier">number</span><span class="symbol">:</span>&nbsp;<span class="symbol">(</span>Int,&nbsp;String<span class="symbol">) &rarr; </span>Int</div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin parameter with both receiver and lambda params renders correctly`() {
        val component = DefaultParameter(
            Params(
                name = "number",
                receiver = NoopParameterType("Boolean"),
                lambdaParams = listOf(NoopParameterType("String")),
                primary = NoopParameterType("Int"),
                language = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div><span class="identifier">number</span><span class="symbol">:</span>&nbsp;Boolean<span class="symbol">.</span><span class="symbol">(</span>String<span class="symbol">) &rarr; </span>Int</div>
            """.trim()
        )
    }
}
