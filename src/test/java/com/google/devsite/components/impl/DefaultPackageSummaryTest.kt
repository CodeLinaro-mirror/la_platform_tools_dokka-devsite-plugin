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
import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.PackageSummary.Params
import com.google.devsite.components.SummaryList
import com.google.devsite.components.testing.NoopContextFreeComponent
import com.google.devsite.components.testing.NoopSummaryList
import com.google.devsite.renderer.Language
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultPackageSummaryTest {
    @Test
    fun `Package summary with only interfaces renders correctly`() {
        val component = createPackageSummary(interfaces = NoopSummaryList(shown = true))

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
        val component = createPackageSummary(classes = NoopSummaryList(shown = true))

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
        val component = createPackageSummary(enums = NoopSummaryList(shown = true))

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
        val component = createPackageSummary(exceptions = NoopSummaryList(shown = true))

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
        val component = createPackageSummary(annotations = NoopSummaryList(shown = true))

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
    fun `Package summary with only type aliases renders correctly`() {
        val component = createPackageSummary(
            displayLanguage = Language.KOTLIN,
            typeAliases = NoopSummaryList(shown = true)
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Type aliases</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with all class-likes renders correctly`() {
        val component = createPackageSummary(
            interfaces = NoopSummaryList(),
            classes = NoopSummaryList(),
            enums = NoopSummaryList(),
            exceptions = NoopSummaryList(),
            annotations = NoopSummaryList(),
            typeAliases = NoopSummaryList(),
            topLevelConstantsSummary = NoopSummaryList(),
            topLevelPropertiesSummary = NoopSummaryList(),
            topLevelFunctionsSummary = NoopSummaryList(),
            extensionPropertiesSummary = NoopSummaryList(),
            extensionFunctionsSummary = NoopSummaryList(),
            topLevelConstants = listOf(NoopContextFreeComponent),
            topLevelProperties = listOf(NoopContextFreeComponent),
            topLevelFunctions = listOf(NoopContextFreeComponent),
            extensionProperties = listOf(NoopContextFreeComponent),
            extensionFunctions = listOf(NoopContextFreeComponent)
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
    fun `Package summary with Kotlin top-level constants renders correctly`() {
        val component = createPackageSummary(
            displayLanguage = Language.KOTLIN,
            topLevelConstantsSummary = NoopSummaryList(shown = true),
            topLevelConstants = listOf(NoopContextFreeComponent)
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Constants summary</h2>
  <div>noop</div>
  <h2>Constants</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with Kotlin top-level properties renders correctly`() {
        val component = createPackageSummary(
            displayLanguage = Language.KOTLIN,
            topLevelPropertiesSummary = NoopSummaryList(shown = true),
            topLevelProperties = listOf(NoopContextFreeComponent)
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Top-level properties summary</h2>
  <div>noop</div>
  <h2>Top-level properties</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with Kotlin top-level functions renders correctly`() {
        val component = createPackageSummary(
            displayLanguage = Language.KOTLIN,
            topLevelFunctionsSummary = NoopSummaryList(shown = true),
            topLevelFunctions = listOf(NoopContextFreeComponent)
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
  <h2>Top-level functions</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with Kotlin extension functions renders correctly`() {
        val component = createPackageSummary(
            displayLanguage = Language.KOTLIN,
            extensionFunctionsSummary = NoopSummaryList(shown = true),
            extensionFunctions = listOf(NoopContextFreeComponent)
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
  <h2>Extension functions</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with Kotlin extension properties renders correctly`() {
        val component = createPackageSummary(
            displayLanguage = Language.KOTLIN,
            extensionPropertiesSummary = NoopSummaryList(shown = true),
            extensionProperties = listOf(NoopContextFreeComponent)
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Extension properties summary</h2>
  <div>noop</div>
  <h2>Extension properties</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with all Kotlin bits renders correctly`() {
        val component = createPackageSummary(
            displayLanguage = Language.KOTLIN,
            topLevelConstantsSummary = NoopSummaryList(shown = true),
            topLevelPropertiesSummary = NoopSummaryList(shown = true),
            topLevelFunctionsSummary = NoopSummaryList(shown = true),
            extensionPropertiesSummary = NoopSummaryList(shown = true),
            extensionFunctionsSummary = NoopSummaryList(shown = true),
            topLevelConstants = listOf(NoopContextFreeComponent),
            topLevelProperties = listOf(NoopContextFreeComponent),
            topLevelFunctions = listOf(NoopContextFreeComponent),
            extensionProperties = listOf(NoopContextFreeComponent),
            extensionFunctions = listOf(NoopContextFreeComponent)
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <h2>Constants summary</h2>
  <div>noop</div>
  <h2>Top-level properties summary</h2>
  <div>noop</div>
  <h2>Top-level functions summary</h2>
  <div>noop</div>
  <h2>Extension properties summary</h2>
  <div>noop</div>
  <h2>Extension functions summary</h2>
  <div>noop</div>
  <h2>Constants</h2>
  <div>noop</div>
  <h2>Top-level properties</h2>
  <div>noop</div>
  <h2>Top-level functions</h2>
  <div>noop</div>
  <h2>Extension properties</h2>
  <div>noop</div>
  <h2>Extension functions</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    private fun createPackageSummary(
        displayLanguage: Language = Language.JAVA,
        interfaces: SummaryList = NoopSummaryList(shown = false),
        classes: SummaryList = NoopSummaryList(shown = false),
        enums: SummaryList = NoopSummaryList(shown = false),
        exceptions: SummaryList = NoopSummaryList(shown = false),
        annotations: SummaryList = NoopSummaryList(shown = false),
        typeAliases: SummaryList = NoopSummaryList(shown = false),
        topLevelConstantsSummary: SummaryList = NoopSummaryList(shown = false),
        topLevelPropertiesSummary: SummaryList = NoopSummaryList(shown = false),
        topLevelFunctionsSummary: SummaryList = NoopSummaryList(shown = false),
        extensionPropertiesSummary: SummaryList = NoopSummaryList(shown = false),
        extensionFunctionsSummary: SummaryList = NoopSummaryList(shown = false),
        topLevelConstants: List<ContextFreeComponent> = emptyList(),
        topLevelProperties: List<ContextFreeComponent> = emptyList(),
        topLevelFunctions: List<ContextFreeComponent> = emptyList(),
        extensionProperties: List<ContextFreeComponent> = emptyList(),
        extensionFunctions: List<ContextFreeComponent> = emptyList()
    ) = DefaultPackageSummary(
        Params(
            displayLanguage = displayLanguage,
            interfaces = interfaces,
            classes = classes,
            enums = enums,
            exceptions = exceptions,
            annotations = annotations,
            typeAliases = typeAliases,
            topLevelConstantsSummary = topLevelConstantsSummary,
            topLevelPropertiesSummary = topLevelPropertiesSummary,
            topLevelFunctionsSummary = topLevelFunctionsSummary,
            extensionPropertiesSummary = extensionPropertiesSummary,
            extensionFunctionsSummary = extensionFunctionsSummary,
            topLevelConstants = topLevelConstants,
            topLevelProperties = topLevelProperties,
            topLevelFunctions = topLevelFunctions,
            extensionProperties = extensionProperties,
            extensionFunctions = extensionFunctions
        )
    )
}
