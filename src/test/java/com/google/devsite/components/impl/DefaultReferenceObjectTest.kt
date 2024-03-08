/*
 * Copyright 2024 The Android Open Source Project
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

import com.google.common.truth.Truth
import com.google.devsite.components.symbols.ReferenceObject
import com.google.devsite.renderer.Language
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultReferenceObjectTest {
    @Test
    fun `Basic reference object renders correctly`() {
        val component = DefaultReferenceObject(
            ReferenceObject.Params(
                language = Language.JAVA,
                name = "Foo",
            ),
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        Truth.assertThat(output).isEqualTo(
            """
                <body>
                  <div itemscope="" itemtype="http://developers.google.com/ReferenceObject">
                    <meta itemprop="name" content="Foo">
                    <meta itemprop="language" content="JAVA">
                  </div>
                </body>
            """.trimIndent(),
        )
    }

    @Test
    fun `Reference object with path renders correctly`() {
        val component = DefaultReferenceObject(
            ReferenceObject.Params(
                language = Language.KOTLIN,
                name = "Foo",
                path = "test.pkg",
            ),
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        Truth.assertThat(output).isEqualTo(
            """
                <body>
                  <div itemscope="" itemtype="http://developers.google.com/ReferenceObject">
                    <meta itemprop="name" content="Foo">
                    <meta itemprop="path" content="test.pkg">
                    <meta itemprop="language" content="KOTLIN">
                  </div>
                </body>
            """.trimIndent(),
        )
    }

    @Test
    fun `Reference object with single property renders correctly`() {
        val component = DefaultReferenceObject(
            ReferenceObject.Params(
                language = Language.KOTLIN,
                name = "Foo",
                properties = listOf("foo"),
            ),
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        Truth.assertThat(output).isEqualTo(
            """
                <body>
                  <div itemscope="" itemtype="http://developers.google.com/ReferenceObject">
                    <meta itemprop="name" content="Foo">
                    <meta itemprop="property" content="foo">
                    <meta itemprop="language" content="KOTLIN">
                  </div>
                </body>
            """.trimIndent(),
        )
    }

    @Test
    fun `Reference object with multiple properties renders correctly`() {
        val component = DefaultReferenceObject(
            ReferenceObject.Params(
                language = Language.KOTLIN,
                name = "Foo",
                properties = listOf("foo", "bar", "Foo.Companion", "FOO"),
            ),
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        Truth.assertThat(output).isEqualTo(
            """
                <body>
                  <div itemscope="" itemtype="http://developers.google.com/ReferenceObject">
                    <meta itemprop="name" content="Foo">
                    <meta itemprop="property" content="foo">
                    <meta itemprop="property" content="bar">
                    <meta itemprop="property" content="Foo.Companion">
                    <meta itemprop="property" content="FOO">
                    <meta itemprop="language" content="KOTLIN">
                  </div>
                </body>
            """.trimIndent(),
        )
    }

    @Test
    fun `Reference object with path and multiple properties renders correctly`() {
        val component = DefaultReferenceObject(
            ReferenceObject.Params(
                language = Language.JAVA,
                name = "Foo",
                path = "test.pkg",
                properties = listOf("foo", "bar", "Foo.Companion", "FOO"),
            ),
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        Truth.assertThat(output).isEqualTo(
            """
                <body>
                  <div itemscope="" itemtype="http://developers.google.com/ReferenceObject">
                    <meta itemprop="name" content="Foo">
                    <meta itemprop="path" content="test.pkg">
                    <meta itemprop="property" content="foo">
                    <meta itemprop="property" content="bar">
                    <meta itemprop="property" content="Foo.Companion">
                    <meta itemprop="property" content="FOO">
                    <meta itemprop="language" content="JAVA">
                  </div>
                </body>
            """.trimIndent(),
        )
    }
}
