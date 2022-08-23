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
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.Link
import com.google.devsite.components.pages.PackageSummary.Params
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.SummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.components.testing.NoopDescriptionComponent
import com.google.devsite.components.testing.NoopSummaryList
import com.google.devsite.components.testing.NoopSymbolDetail
import com.google.devsite.components.testing.NoopTwoPaneTypeSummaryItem
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
                classes = defaultSummaryListOf(
                    NoopTwoPaneTypeSummaryItem as TwoPaneSummaryItem<Link, DescriptionComponent>
                ),
                objects = defaultSummaryListOf(
                    NoopTwoPaneTypeSummaryItem as TwoPaneSummaryItem<Link, DescriptionComponent>
                ),
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
      <tbody class="list">
        <tr><noop/></tr>
      </tbody>
    </table>
  </div>
  <h2>Objects</h2>
  <div class="devsite-table-wrapper">
    <table class="responsive">
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
            topLevelConstants = listOf(NoopSymbolDetail),
            topLevelProperties = listOf(NoopSymbolDetail),
            topLevelFunctions = listOf(NoopSymbolDetail),
            extensionProperties = listOf(NoopSymbolDetail),
            extensionFunctions = listOf(NoopSymbolDetail)
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
            topLevelConstants = listOf(NoopSymbolDetail)
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
            topLevelProperties = listOf(NoopSymbolDetail)
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
            topLevelFunctions = listOf(NoopSymbolDetail)
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
            extensionFunctions = listOf(NoopSymbolDetail)
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
            extensionProperties = listOf(NoopSymbolDetail)
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
            topLevelConstants = listOf(NoopSymbolDetail),
            topLevelProperties = listOf(NoopSymbolDetail),
            topLevelFunctions = listOf(NoopSymbolDetail),
            extensionProperties = listOf(NoopSymbolDetail),
            extensionFunctions = listOf(NoopSymbolDetail)
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
        interfaces: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>> =
            NoopSummaryList(show = false),
        classes: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>> =
            NoopSummaryList(show = false),
        enums: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>> =
            NoopSummaryList(show = false),
        objects: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>> =
            NoopSummaryList(show = false),
        exceptions: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>> =
            NoopSummaryList(show = false),
        annotations: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>> =
            NoopSummaryList(show = false),
        typeAliases: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>> =
            NoopSummaryList(show = false),
        topLevelConstantsSummary: SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>> =
            NoopSummaryList(show = false),
        topLevelPropertiesSummary: SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>> =
            NoopSummaryList(show = false),
        topLevelFunctionsSummary: SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>> =
            NoopSummaryList(show = false),
        extensionPropertiesSummary: SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>> =
            NoopSummaryList(show = false),
        extensionFunctionsSummary: SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>> =
            NoopSummaryList(show = false),
        topLevelConstants: List<SymbolDetail> = emptyList(),
        topLevelProperties: List<SymbolDetail> = emptyList(),
        topLevelFunctions: List<SymbolDetail> = emptyList(),
        extensionProperties: List<SymbolDetail> = emptyList(),
        extensionFunctions: List<SymbolDetail> = emptyList()
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
