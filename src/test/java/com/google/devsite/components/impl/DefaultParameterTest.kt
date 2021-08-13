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
import com.google.devsite.components.symbols.Parameter.Params
import com.google.devsite.components.testing.NoopAnnotationComponent
import com.google.devsite.components.testing.NoopSymbolType
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
                isLambda = false,
                name = "number",
                type = NoopSymbolType("Int"),
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>number:&nbsp;Int</div>
            """.trim()
        )
    }

    @Test
    fun `Simple Java parameter renders correctly`() {
        val component = DefaultParameter(
            Params(
                isLambda = false,
                name = "number",
                type = NoopSymbolType("int"),
                displayLanguage = Language.JAVA
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>int&nbsp;number</div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin parameter without name renders correctly`() {
        val component = DefaultParameter(
            Params(
                isLambda = false,
                name = "",
                type = NoopSymbolType("Int"),
                displayLanguage = Language.KOTLIN
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
    fun `Java parameter without name renders correctly`() {
        val component = DefaultParameter(
            Params(
                isLambda = false,
                name = "",
                type = NoopSymbolType("int"),
                displayLanguage = Language.JAVA
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>int</div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin parameter with annotations renders correctly`() {
        val component = DefaultParameter(
            Params(
                isLambda = false,
                name = "number",
                type = NoopSymbolType("Int"),
                annotationComponents = listOf(
                    NoopAnnotationComponent("@Really"),
                    NoopAnnotationComponent("@Special")
                ),
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>@Really @Special number:&nbsp;Int</div>
            """.trim()
        )
    }

    @Test
    fun `Java parameter with annotations renders correctly`() {
        val component = DefaultParameter(
            Params(
                isLambda = false,
                name = "number",
                type = NoopSymbolType("int"),
                annotationComponents = listOf(
                    NoopAnnotationComponent("@Really"),
                    NoopAnnotationComponent("@Special")
                ),
                displayLanguage = Language.JAVA
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>@Really @Special int&nbsp;number</div>
            """.trim()
        )
    }

    @Test
    fun `Java parameter with lambda is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DefaultParameter(
                Params(
                    isLambda = true,
                    name = "number",
                    type = NoopSymbolType("int"),
                    displayLanguage = Language.JAVA
                )
            )
        }
    }

    @Test
    fun `Standard parameter with receiver is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DefaultParameter(
                Params(
                    isLambda = false,
                    name = "number",
                    receiver = NoopSymbolType("int"),
                    type = NoopSymbolType("int"),
                    displayLanguage = Language.KOTLIN
                )
            )
        }
    }

    @Test
    fun `Standard parameter with lambda modifiers is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DefaultParameter(
                Params(
                    isLambda = false,
                    name = "number",
                    lambdaModifiers = listOf("suspend"),
                    type = NoopSymbolType("int"),
                    displayLanguage = Language.KOTLIN
                )
            )
        }
    }

    @Test
    fun `Standard parameter with lambda params is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DefaultParameter(
                Params(
                    isLambda = false,
                    name = "number",
                    lambdaParams = listOf(NoopSymbolType("int")),
                    type = NoopSymbolType("int"),
                    displayLanguage = Language.KOTLIN
                )
            )
        }
    }

    @Test
    fun `Kotlin parameter with factory lambda renders correctly`() {
        val component = DefaultParameter(
            Params(
                isLambda = true,
                name = "block",
                type = NoopSymbolType("Unit"),
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>block:&nbsp;() <span style="white-space: nowrap;">-&gt;</span> Unit</div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin parameter with receiver renders correctly`() {
        val component = DefaultParameter(
            Params(
                isLambda = true,
                name = "number",
                receiver = NoopSymbolType("Int"),
                type = NoopSymbolType("Int"),
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>number:&nbsp;Int.() <span style="white-space: nowrap;">-&gt;</span> Int</div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin parameter with lambda params renders correctly`() {
        val component = DefaultParameter(
            Params(
                isLambda = true,
                name = "number",
                lambdaParams = listOf(NoopSymbolType("Int"), NoopSymbolType("String")),
                type = NoopSymbolType("Int"),
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>number:&nbsp;(Int, String) <span style="white-space: nowrap;">-&gt;</span> Int</div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin parameter with both receiver and lambda params renders correctly`() {
        val component = DefaultParameter(
            Params(
                isLambda = true,
                name = "number",
                receiver = NoopSymbolType("Boolean"),
                lambdaParams = listOf(NoopSymbolType("String")),
                type = NoopSymbolType("Int"),
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>number:&nbsp;Boolean.(String) <span style="white-space: nowrap;">-&gt;</span> Int</div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin parameter with lambda modifiers renders correctly`() {
        val component = DefaultParameter(
            Params(
                isLambda = true,
                name = "number",
                lambdaParams = listOf(NoopSymbolType("String")),
                lambdaModifiers = listOf("suspend"),
                type = NoopSymbolType("Int"),
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>number:&nbsp;suspend&nbsp;(String) <span style="white-space: nowrap;">-&gt;</span> Int</div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin parameter with default value renders correctly`() {
        val component = DefaultParameter(
            Params(
                displayLanguage = Language.KOTLIN,
                isLambda = false,
                name = "number",
                type = NoopSymbolType("Int"),
                defaultValue = "5"
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>number:&nbsp;Int = 5</div>
            """.trim()
        )
    }
}
