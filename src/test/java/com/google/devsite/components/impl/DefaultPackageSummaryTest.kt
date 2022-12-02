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
import com.google.devsite.FunctionSummaryList
import com.google.devsite.LinkDescriptionSummaryList
import com.google.devsite.PropertySummaryList
import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.pages.PackageSummary.Params
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.table.SummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.testing.NoopDescriptionComponent
import com.google.devsite.components.testing.NoopSummaryList
import com.google.devsite.components.testing.NoopSymbolDetailF
import com.google.devsite.components.testing.NoopSymbolDetailP
import com.google.devsite.components.testing.NoopTableRowTypeSummaryItemLD
import com.google.devsite.renderer.Language
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultPackageSummaryTest {
    @Test
    fun `Package summary with description renders correctly`() {
        val component = createPackageSummary(
            description = listOf(NoopDescriptionComponent("Hello World!"))
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <p>Hello World!</p>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with only interfaces renders correctly`() {
        val component = createPackageSummary(interfaces = NoopSummaryList())

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
        val component = createPackageSummary(classes = NoopSummaryList())

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
        val component = createPackageSummary(enums = NoopSummaryList())

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

    private fun <T : SummaryItem> defaultSummaryListOf(vararg items: T) =
        DefaultSummaryList(SummaryList.Params(items = items.asList()))

    @Test
    fun `Package summary objects are rendered in Java and Kotlin`() {
        for (language in listOf(Language.KOTLIN, Language.JAVA)) {
            val component = createPackageSummary(
                classes = defaultSummaryListOf(NoopTableRowTypeSummaryItemLD),
                objects = defaultSummaryListOf(NoopTableRowTypeSummaryItemLD),
                displayLanguage = language
            )

            val output = createHTML().div {
                component.render(this)
            }.trim()

            val expected = if (language == Language.KOTLIN) {
                """
<div>
  <h2>Classes</h2>
  <div class="devsite-table-wrapper">
    <table class="responsive">
      <colgroup>
        <col width="40%">
        <col>
      </colgroup>
      <tbody class="list">
        <tr><noop/></tr>
      </tbody>
    </table>
  </div>
  <h2>Objects</h2>
  <div class="devsite-table-wrapper">
    <table class="responsive">
      <colgroup>
        <col width="40%">
        <col>
      </colgroup>
      <tbody class="list">
        <tr><noop/></tr>
      </tbody>
    </table>
  </div>
</div>
            """
            } else {
                """
<div>
  <h2>Classes</h2>
  <div class="devsite-table-wrapper">
    <table class="responsive">
      <colgroup>
        <col width="40%">
        <col>
      </colgroup>
      <tbody class="list">
        <tr><noop/></tr>
        <tr><noop/></tr>
      </tbody>
    </table>
  </div>
</div>
            """
            }
            // language=html
            assertThat(output).isEqualTo(expected.trim())
        }
    }

    @Test
    fun `Package summary with only exceptions renders correctly`() {
        val component = createPackageSummary(exceptions = NoopSummaryList())

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
        val component = createPackageSummary(annotations = NoopSummaryList())

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
            typeAliases = NoopSummaryList()
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
    fun `Package summary for Java with all class-likes renders correctly`() {
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
            topLevelConstants = listOf(NoopSymbolDetailP),
            topLevelProperties = listOf(NoopSymbolDetailP),
            topLevelFunctions = listOf(NoopSymbolDetailF),
            extensionProperties = listOf(NoopSymbolDetailP),
            extensionFunctions = listOf(NoopSymbolDetailF)
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
  <h2>Interfaces</h2>
  <div>noop</div>
  <h2>Classes</h2>
  <div>noop</div>
  <h2>Enums</h2>
  <div>noop</div>
  <h2>Exceptions</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Package summary with Kotlin top-level constants renders correctly`() {
        val component = createPackageSummary(
            displayLanguage = Language.KOTLIN,
            topLevelConstantsSummary = NoopSummaryList(),
            topLevelConstants = listOf(NoopSymbolDetailP)
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
            topLevelPropertiesSummary = NoopSummaryList(),
            topLevelProperties = listOf(NoopSymbolDetailP)
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
            topLevelFunctionsSummary = NoopSummaryList(),
            topLevelFunctions = listOf(NoopSymbolDetailF)
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
            extensionFunctionsSummary = NoopSummaryList(),
            extensionFunctions = listOf(NoopSymbolDetailF)
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
            extensionPropertiesSummary = NoopSummaryList(),
            extensionProperties = listOf(NoopSymbolDetailP)
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
            topLevelConstantsSummary = NoopSummaryList(),
            topLevelPropertiesSummary = NoopSummaryList(),
            topLevelFunctionsSummary = NoopSummaryList(),
            extensionPropertiesSummary = NoopSummaryList(),
            extensionFunctionsSummary = NoopSummaryList(),
            topLevelConstants = listOf(NoopSymbolDetailP),
            topLevelProperties = listOf(NoopSymbolDetailP),
            topLevelFunctions = listOf(NoopSymbolDetailF),
            extensionProperties = listOf(NoopSymbolDetailP),
            extensionFunctions = listOf(NoopSymbolDetailF)
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
  <h2>Top-level functions summary</h2>
  <div>noop</div>
  <h2>Extension functions summary</h2>
  <div>noop</div>
  <h2>Top-level properties summary</h2>
  <div>noop</div>
  <h2>Extension properties summary</h2>
  <div>noop</div>
  <h2>Constants</h2>
  <div>noop</div>
  <h2>Top-level functions</h2>
  <div>noop</div>
  <h2>Extension functions</h2>
  <div>noop</div>
  <h2>Top-level properties</h2>
  <div>noop</div>
  <h2>Extension properties</h2>
  <div>noop</div>
</div>
            """.trim()
        )
    }

    private fun createPackageSummary(
        displayLanguage: Language = Language.JAVA,
        description: List<ContextFreeComponent> = emptyList(),
        interfaces: LinkDescriptionSummaryList = NoopSummaryList(show = false),
        classes: LinkDescriptionSummaryList = NoopSummaryList(show = false),
        enums: LinkDescriptionSummaryList = NoopSummaryList(show = false),
        objects: LinkDescriptionSummaryList = NoopSummaryList(show = false),
        exceptions: LinkDescriptionSummaryList = NoopSummaryList(show = false),
        annotations: LinkDescriptionSummaryList = NoopSummaryList(show = false),
        typeAliases: LinkDescriptionSummaryList = NoopSummaryList(show = false),
        topLevelConstantsSummary: PropertySummaryList = NoopSummaryList(show = false),
        topLevelPropertiesSummary: PropertySummaryList = NoopSummaryList(show = false),
        topLevelFunctionsSummary: FunctionSummaryList = NoopSummaryList(show = false),
        extensionPropertiesSummary: PropertySummaryList = NoopSummaryList(show = false),
        extensionFunctionsSummary: FunctionSummaryList = NoopSummaryList(show = false),
        topLevelConstants: List<SymbolDetail<PropertySignature>> = emptyList(),
        topLevelProperties: List<SymbolDetail<PropertySignature>> = emptyList(),
        topLevelFunctions: List<SymbolDetail<FunctionSignature>> = emptyList(),
        extensionProperties: List<SymbolDetail<PropertySignature>> = emptyList(),
        extensionFunctions: List<SymbolDetail<FunctionSignature>> = emptyList()
    ) = DefaultPackageSummary(
        Params(
            displayLanguage = displayLanguage,
            description = description,
            interfaces = interfaces,
            classes = classes,
            enums = enums,
            objects = objects,
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
