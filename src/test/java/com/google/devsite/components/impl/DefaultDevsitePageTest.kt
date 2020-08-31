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
import com.google.devsite.components.DevsitePage.Params
import com.google.devsite.components.testing.NoopContextFreeComponent
import com.google.devsite.renderer.Language
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultDevsitePageTest {
    @Test
    fun `Java page renders correctly`() {
        val component = DefaultDevsitePage(
            Params(
                displayLanguage = Language.JAVA,
                path = "page.html",
                bookPath = "/reference/androidx/_book.yaml",
                title = "Page Title",
                content = NoopContextFreeComponent
            )
        )

        val output = createHTML().html {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<html devsite="true">
  <head>
    <title>Page Title</title>
{% setvar book_path %}/reference/androidx/_book.yaml{% endsetvar %}
{% include "_shared/_reference-head-tags.html" %}
  </head>
  <body>
    <h1>Page Title</h1>
{% setvar page_path %}page.html{% endsetvar %}
{% setvar can_switch %}1{% endsetvar %}
{% include "reference/_java_switcher2.md" %}
    <div>noop</div>
  </body>
</html>
            """.trim()
        )
    }

    @Test
    fun `Kotlin page renders correctly`() {
        val component = DefaultDevsitePage(
            Params(
                displayLanguage = Language.KOTLIN,
                path = "page.html",
                bookPath = "/reference/androidx/_book.yaml",
                title = "Page Title",
                content = NoopContextFreeComponent
            )
        )

        val output = createHTML().html {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<html devsite="true">
  <head>
    <title>Page Title</title>
{% setvar book_path %}/reference/androidx/_book.yaml{% endsetvar %}
{% include "_shared/_reference-head-tags.html" %}
  </head>
  <body>
    <h1>Page Title</h1>
{% setvar page_path %}page.html{% endsetvar %}
{% setvar can_switch %}1{% endsetvar %}
{% include "reference/_kotlin_switcher2.md" %}
    <div>noop</div>
  </body>
</html>
            """.trim()
        )
    }
}
