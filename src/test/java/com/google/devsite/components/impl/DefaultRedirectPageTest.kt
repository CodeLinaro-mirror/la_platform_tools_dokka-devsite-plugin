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
import com.google.devsite.components.pages.RedirectPage.Params
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultRedirectPageTest {
    @Test
    fun `Page renders correctly`() {
        val component = DefaultRedirectPage(Params("foo.html"))

        val output = createHTML().html {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<html>
  <head>
    <meta charset="utf-8">
    <meta content="0; url=foo.html" http-equiv="refresh">
    <meta name="robots" content="noindex">
    <link href="foo.html" rel="canonical">
    <title>Redirecting&hellip;</title>
  </head>
  <body>
    <h1>Redirecting&hellip;</h1>
<a href="foo.html">Click here if you are not redirected.</a></body>
</html>
            """.trim(),
        )
    }
}
