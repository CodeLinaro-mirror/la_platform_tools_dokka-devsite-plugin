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
import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.pages.Classlike.Params
import com.google.devsite.components.table.SummaryItem
import com.google.devsite.components.testing.NoopClassHierarchy
import com.google.devsite.components.testing.NoopClassSignature
import com.google.devsite.components.testing.NoopDescriptionComponent
import com.google.devsite.components.testing.NoopRelatedSymbols
import com.google.devsite.components.testing.NoopSummaryList
import com.google.devsite.components.testing.NoopSymbolDetail
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultClasslikeTest {
    @Test
    fun `Empty classlike renders correctly`() {
        val component = DefaultClasslike(
            Params(
                signature = NoopClassSignature(),
                hierarchy = NoopClassHierarchy(shown = false),
                relatedSymbols = NoopRelatedSymbols(shown = false),
                description = emptyList(),
                symbolTypes = emptyList(),
                inheritedTypes = emptyList()
            )
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>Signature</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Classlike with hierarchy renders correctly`() {
        val component = DefaultClasslike(
            Params(
                signature = NoopClassSignature(),
                hierarchy = NoopClassHierarchy(),
                relatedSymbols = NoopRelatedSymbols(shown = false),
                description = emptyList(),
                symbolTypes = emptyList(),
                inheritedTypes = emptyList()
            )
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>Signature</p>
  <div>Class hierarchy</div>
</body>
            """.trim()
        )
    }

    @Test
    fun `Classlike with related symbols renders correctly`() {
        val component = DefaultClasslike(
            Params(
                signature = NoopClassSignature(),
                hierarchy = NoopClassHierarchy(shown = false),
                relatedSymbols = NoopRelatedSymbols(),
                description = emptyList(),
                symbolTypes = emptyList(),
                inheritedTypes = emptyList()
            )
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>Signature</p>
  <div>Related symbols</div>
</body>
            """.trim()
        )
    }

    @Test
    fun `Classlike with description renders correctly`() {
        val component = DefaultClasslike(
            Params(
                signature = NoopClassSignature(),
                hierarchy = NoopClassHierarchy(shown = false),
                relatedSymbols = NoopRelatedSymbols(shown = false),
                description = listOf(NoopDescriptionComponent("Hello World!")),
                symbolTypes = emptyList(),
                inheritedTypes = emptyList()
            )
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>Signature</p>
  <hr>
  <p>Hello World!</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Classlike with symbols renders correctly`() {
        val component = DefaultClasslike(
            Params(
                signature = NoopClassSignature(),
                hierarchy = NoopClassHierarchy(shown = false),
                relatedSymbols = NoopRelatedSymbols(shown = false),
                description = emptyList(),
                symbolTypes = listOf(
                    NoopSummaryList<SummaryItem>() to Classlike.TitledList(
                        "Symbols",
                        listOf(NoopSymbolDetail)
                    )
                ),
                inheritedTypes = emptyList()
            )
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>Signature</p>
  <h2>Summary</h2>
  <div>noop</div>
  <h2>Symbols</h2>
  <div>noop</div>
</body>
            """.trim()
        )
    }
}
