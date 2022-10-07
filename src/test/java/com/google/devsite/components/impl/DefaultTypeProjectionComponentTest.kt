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
import com.google.devsite.components.symbols.TypeProjectionComponent.Params
import com.google.devsite.components.testing.NoopLink
import com.google.devsite.components.testing.NoopTypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Nullability
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultTypeProjectionComponentTest {
    @Test
    fun `Simple parameter type renders correctly`() {
        val component = DefaultTypeProjectionComponent(
            Params(
                type = NoopLink("Int"),
                nullability = Nullability.KOTLIN_DEFAULT,
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
    fun `Simple nullable parameter type renders correctly`() {
        val component = DefaultTypeProjectionComponent(
            Params(
                type = NoopLink("Int"),
                nullability = Nullability.KOTLIN_NULLABLE,
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>Int?</div>
            """.trim()
        )
    }

    @Test
    fun `Simple platform parameter type renders correctly`() {
        val component = DefaultTypeProjectionComponent(
            Params(
                type = NoopLink("Int"),
                nullability = Nullability.JAVA_NOT_ANNOTATED,
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>Int!</div>
            """.trim()
        )
    }

    @Test
    fun `Parameter type with one generic renders correctly`() {
        val component = DefaultTypeProjectionComponent(
            Params(
                type = NoopLink("List"),
                nullability = Nullability.KOTLIN_DEFAULT,
                displayLanguage = Language.KOTLIN,
                generics = listOf(NoopTypeProjectionComponent("String"))
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>List&lt;String&gt;</div>
            """.trim()
        )
    }

    @Test
    fun `Nullable parameter type with one generic renders correctly`() {
        val component = DefaultTypeProjectionComponent(
            Params(
                type = NoopLink("List"),
                nullability = Nullability.KOTLIN_NULLABLE,
                displayLanguage = Language.KOTLIN,
                generics = listOf(NoopTypeProjectionComponent("String"))
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>List&lt;String&gt;?</div>
            """.trim()
        )
    }

    @Test
    fun `Parameter type with multiple generics renders correctly`() {
        val component = DefaultTypeProjectionComponent(
            Params(
                type = NoopLink("Map"),
                nullability = Nullability.KOTLIN_DEFAULT,
                displayLanguage = Language.KOTLIN,
                generics = listOf(
                    NoopTypeProjectionComponent("String"),
                    NoopTypeProjectionComponent("Int")
                )
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>Map&lt;String,&nbsp;Int&gt;</div>
            """.trim()
        )
    }
}
