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
import com.google.devsite.components.symbols.LambdaTypeProjectionComponent
import com.google.devsite.components.symbols.ParameterComponent.Params
import com.google.devsite.components.testing.NoopAnnotationComponent
import com.google.devsite.components.testing.NoopLambdaTypeProjectionComponent
import com.google.devsite.components.testing.NoopLink
import com.google.devsite.components.testing.NoopParameterComponent
import com.google.devsite.components.testing.NoopTypeProjectionComponent
import com.google.devsite.renderer.Language
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultParameterComponentTest {
    @Test
    fun `Simple Kotlin parameter renders correctly`() {
        val component = DefaultParameterComponent(
            Params(
                name = "number",
                type = NoopTypeProjectionComponent("Int"),
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
        val component = DefaultParameterComponent(
            Params(
                name = "number",
                type = NoopTypeProjectionComponent("int"),
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
        val component = DefaultParameterComponent(
            Params(
                name = "",
                type = NoopTypeProjectionComponent("Int"),
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
    fun `Vararg Kotlin parameter renders correctly`() {
        val component = DefaultParameterComponent(
            Params(
                name = "number",
                modifiers = listOf("vararg"),
                type = NoopTypeProjectionComponent("Int"),
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>vararg&nbsp;number:&nbsp;Int</div>
            """.trim()
        )
    }

    @Test
    fun `Java parameter without name renders correctly`() {
        val component = DefaultParameterComponent(
            Params(
                name = "",
                type = NoopTypeProjectionComponent("int"),
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
        val component = DefaultParameterComponent(
            Params(
                name = "number",
                type = NoopTypeProjectionComponent("Int"),
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
        val component = DefaultParameterComponent(
            Params(
                name = "number",
                type = NoopTypeProjectionComponent("int"),
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
    fun `Kotlin parameter with factory lambda renders correctly`() {
        val component = DefaultParameterComponent(
            Params(
                name = "block",
                type = NoopLambdaTypeProjectionComponent(type = "Unit"),
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
        val component = DefaultParameterComponent(
            Params(
                name = "number",
                type = NoopLambdaTypeProjectionComponent(receiver = "Int", type = "Int"),
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
        val component = DefaultParameterComponent(
            Params(
                name = "number",
                type = DefaultLambdaTypeProjectionComponent(LambdaTypeProjectionComponent.Params(
                    type = NoopLink("Int"),
                    displayLanguage = Language.KOTLIN,
                    lambdaParams = listOf(
                        NoopParameterComponent("Int"),
                        NoopParameterComponent("String")
                    )
                )),
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
        val component = DefaultParameterComponent(
            Params(
                name = "number",
                type = DefaultLambdaTypeProjectionComponent(LambdaTypeProjectionComponent.Params(
                    type = NoopLink("Int"),
                    receiver = NoopTypeProjectionComponent("Boolean"),
                    displayLanguage = Language.KOTLIN,
                    lambdaParams = listOf(NoopParameterComponent("String"))
                )),
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
        val component = DefaultParameterComponent(
            Params(
                name = "number",
                type = DefaultLambdaTypeProjectionComponent(LambdaTypeProjectionComponent.Params(
                    type = NoopLink("Int"),
                    displayLanguage = Language.KOTLIN,
                    lambdaParams = listOf(NoopParameterComponent("String")),
                    lambdaModifiers = listOf("suspend")
                )),
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
        val component = DefaultParameterComponent(
            Params(
                displayLanguage = Language.KOTLIN,
                name = "number",
                type = NoopTypeProjectionComponent("Int"),
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
