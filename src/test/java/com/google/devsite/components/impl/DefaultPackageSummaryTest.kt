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
import com.google.devsite.components.PackageSummary.Params
import com.google.devsite.components.testing.NoopSummaryList
import com.google.devsite.renderer.Language
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultPackageSummaryTest {
    @Test
    fun `Package summary with only interfaces renders correctly`() {
        val component = DefaultPackageSummary(
            Params(
                displayLanguage = Language.JAVA,
                interfaces = NoopSummaryList(shown = true),
                classes = NoopSummaryList(shown = false),
                enums = NoopSummaryList(shown = false),
                exceptions = NoopSummaryList(shown = false),
                annotations = NoopSummaryList(shown = false),
                topLevelFunctionsSummary = NoopSummaryList(shown = false),
                extensionFunctionsSummary = NoopSummaryList(shown = false)
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Interfaces</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with only classes renders correctly`() {
        val component = DefaultPackageSummary(
            Params(
                displayLanguage = Language.JAVA,
                interfaces = NoopSummaryList(shown = false),
                classes = NoopSummaryList(shown = true),
                enums = NoopSummaryList(shown = false),
                exceptions = NoopSummaryList(shown = false),
                annotations = NoopSummaryList(shown = false),
                topLevelFunctionsSummary = NoopSummaryList(shown = false),
                extensionFunctionsSummary = NoopSummaryList(shown = false)
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Classes</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with only enums renders correctly`() {
        val component = DefaultPackageSummary(
            Params(
                displayLanguage = Language.JAVA,
                interfaces = NoopSummaryList(shown = false),
                classes = NoopSummaryList(shown = false),
                enums = NoopSummaryList(shown = true),
                exceptions = NoopSummaryList(shown = false),
                annotations = NoopSummaryList(shown = false),
                topLevelFunctionsSummary = NoopSummaryList(shown = false),
                extensionFunctionsSummary = NoopSummaryList(shown = false)
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Enums</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with only exceptions renders correctly`() {
        val component = DefaultPackageSummary(
            Params(
                displayLanguage = Language.JAVA,
                interfaces = NoopSummaryList(shown = false),
                classes = NoopSummaryList(shown = false),
                enums = NoopSummaryList(shown = false),
                exceptions = NoopSummaryList(shown = true),
                annotations = NoopSummaryList(shown = false),
                topLevelFunctionsSummary = NoopSummaryList(shown = false),
                extensionFunctionsSummary = NoopSummaryList(shown = false)
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Exceptions</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with only annotations renders correctly`() {
        val component = DefaultPackageSummary(
            Params(
                displayLanguage = Language.JAVA,
                interfaces = NoopSummaryList(shown = false),
                classes = NoopSummaryList(shown = false),
                enums = NoopSummaryList(shown = false),
                exceptions = NoopSummaryList(shown = false),
                annotations = NoopSummaryList(shown = true),
                topLevelFunctionsSummary = NoopSummaryList(shown = false),
                extensionFunctionsSummary = NoopSummaryList(shown = false)
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Annotations</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with all class-likes renders correctly`() {
        val component = DefaultPackageSummary(
            Params(
                displayLanguage = Language.JAVA,
                interfaces = NoopSummaryList(),
                classes = NoopSummaryList(),
                enums = NoopSummaryList(),
                exceptions = NoopSummaryList(),
                annotations = NoopSummaryList(),
                topLevelFunctionsSummary = NoopSummaryList(),
                extensionFunctionsSummary = NoopSummaryList()
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Interfaces</h2>
  <div>noop</div>
  <h2>Classes</h2>
  <div>noop</div>
  <h2>Enums</h2>
  <div>noop</div>
  <h2>Exceptions</h2>
  <div>noop</div>
  <h2>Annotations</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with Kotlin top-level functions renders correctly`() {
        val component = DefaultPackageSummary(
            Params(
                displayLanguage = Language.KOTLIN,
                interfaces = NoopSummaryList(shown = false),
                classes = NoopSummaryList(shown = false),
                enums = NoopSummaryList(shown = false),
                exceptions = NoopSummaryList(shown = false),
                annotations = NoopSummaryList(shown = false),
                topLevelFunctionsSummary = NoopSummaryList(shown = true),
                extensionFunctionsSummary = NoopSummaryList(shown = false)
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Top-level functions summary</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with Kotlin extension functions renders correctly`() {
        val component = DefaultPackageSummary(
            Params(
                displayLanguage = Language.KOTLIN,
                interfaces = NoopSummaryList(shown = false),
                classes = NoopSummaryList(shown = false),
                enums = NoopSummaryList(shown = false),
                exceptions = NoopSummaryList(shown = false),
                annotations = NoopSummaryList(shown = false),
                topLevelFunctionsSummary = NoopSummaryList(shown = false),
                extensionFunctionsSummary = NoopSummaryList(shown = true)
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Extension functions summary</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }
}
