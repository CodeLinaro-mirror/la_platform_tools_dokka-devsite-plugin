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
import com.google.devsite.components.pages.ClassIndex.Params
import com.google.devsite.components.testing.NoopSummaryList
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultClassIndexTest {
    @Test
    fun `Empty classes renders correctly`() {
        val component = DefaultClassIndex(Params("packages.html", emptyMap()))

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>These are all the API classes. See all <a href="packages.html">API packages</a>.</p>
  <p><em>This project has no classes.</em></p>
</body>
            """
                    .trim(),
            )
    }

    @Test
    fun `Single class renders correctly`() {
        val component = DefaultClassIndex(Params("packages.html", mapOf('A' to NoopSummaryList())))

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>These are all the API classes. See all <a href="packages.html">API packages</a>.</p>
  <div class="jd-letterlist"><a href="#letter_A">A</a>&nbsp;&nbsp;</div>
  <h2 id="letter_A">A</h2>
  <div>noop</div>
</body>
            """
                    .trim(),
            )
    }

    @Test
    fun `Multiple classes renders correctly`() {
        val component =
            DefaultClassIndex(
                Params(
                    "packages.html",
                    mapOf(
                        'A' to NoopSummaryList(),
                        'B' to NoopSummaryList(),
                        'Z' to NoopSummaryList()
                    ),
                ),
            )

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>These are all the API classes. See all <a href="packages.html">API packages</a>.</p>
  <div class="jd-letterlist"><a href="#letter_A">A</a>&nbsp;&nbsp;<a href="#letter_B">B</a>&nbsp;&nbsp;<a href="#letter_Z">Z</a>&nbsp;&nbsp;</div>
  <h2 id="letter_A">A</h2>
  <div>noop</div>
  <h2 id="letter_B">B</h2>
  <div>noop</div>
  <h2 id="letter_Z">Z</h2>
  <div>noop</div>
</body>
            """
                    .trim(),
            )
    }
}
