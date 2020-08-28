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
import com.google.devsite.components.Classlike
import com.google.devsite.components.testing.NoopContextFreeComponent
import com.google.devsite.components.testing.NoopDescription
import com.google.devsite.components.testing.NoopSummaryList
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultClasslikeTest {
    @Test
    fun `Empty classlike renders correctly`() {
        val component = DefaultClasslike(
            Classlike.Params(
                description = emptyList(),
                symbolTypes = emptyList()
            )
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>TODO(b/166518424) class signature</p>
  <p>TODO(b/166518951) inheritance hierarchy</p>
  <p>TODO(b/166518636) direct subclasses</p>
  <p>TODO(b/166518636) indirect subclasses</p>
  <hr>
  <h2>Summary</h2>
  <p>Nested *</p>
  <p>Enum values</p>
  <p>Constants</p>
  <p>Public fields</p>
  <p>Protected fields</p>
  <p>Public constructors</p>
  <p>Protected constructors</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Classlike with description renders correctly`() {
        val component = DefaultClasslike(
            Classlike.Params(
                description = listOf(NoopDescription("Hello World!")),
                symbolTypes = emptyList()
            )
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>TODO(b/166518424) class signature</p>
  <p>TODO(b/166518951) inheritance hierarchy</p>
  <p>TODO(b/166518636) direct subclasses</p>
  <p>TODO(b/166518636) indirect subclasses</p>
  <hr>
  <p>Hello World!</p>
  <h2>Summary</h2>
  <p>Nested *</p>
  <p>Enum values</p>
  <p>Constants</p>
  <p>Public fields</p>
  <p>Protected fields</p>
  <p>Public constructors</p>
  <p>Protected constructors</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Classlike with symbols renders correctly`() {
        val component = DefaultClasslike(
            Classlike.Params(
                description = emptyList(),
                symbolTypes = listOf(
                    NoopSummaryList() to Classlike.SymbolType(
                        "Symbols",
                        listOf(NoopContextFreeComponent)
                    )
                )
            )
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>TODO(b/166518424) class signature</p>
  <p>TODO(b/166518951) inheritance hierarchy</p>
  <p>TODO(b/166518636) direct subclasses</p>
  <p>TODO(b/166518636) indirect subclasses</p>
  <hr>
  <h2>Summary</h2>
  <p>Nested *</p>
  <p>Enum values</p>
  <p>Constants</p>
  <p>Public fields</p>
  <p>Protected fields</p>
  <p>Public constructors</p>
  <p>Protected constructors</p>
  <div>noop</div>
  <h2>Symbols</h2>
  <div>noop</div>
</body>
            """.trim()
        )
    }
}
