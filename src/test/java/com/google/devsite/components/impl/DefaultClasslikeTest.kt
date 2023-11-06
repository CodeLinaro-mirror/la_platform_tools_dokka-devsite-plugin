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
import com.google.devsite.components.pages.emptyTitledList
import com.google.devsite.components.symbols.ClasslikeDescription
import com.google.devsite.components.testing.NoopClassHierarchy
import com.google.devsite.components.testing.NoopClasslikeDescription
import com.google.devsite.components.testing.NoopClasslikeSignature
import com.google.devsite.components.testing.NoopRelatedSymbols
import com.google.devsite.components.testing.NoopSummaryList
import com.google.devsite.components.testing.NoopSymbolDetailF
import com.google.devsite.renderer.Language
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultClasslikeTest {
    @Test
    fun `Empty classlike renders correctly`() {
        val component = DefaultClasslike(emptyClasslikeParams)

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>header
signature
hierarchy
relatedSymbols
descriptionDocs
</body>
            """.trim(),
        )
    }

    @Test
    fun `Classlike with description renders correctly`() {
        val component = DefaultClasslike(
            emptyClasslikeParams.copy(
                description = DefaultClasslikeDescription(
                    ClasslikeDescription.Params(
                        header = null,
                        hierarchy = NoopClassHierarchy(),
                        primarySignature = NoopClasslikeSignature(),
                        relatedSymbols = NoopRelatedSymbols(),
                        descriptionDocs = emptyList(),
                    ),
                ),
            ),
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>
    <pre>Signature</pre>
  </p>
  <div>Class hierarchy</div>
  <div>Related symbols</div>
</body>
            """.trim(),
        )
    }

    @Test
    fun `Classlike with symbols renders correctly`() {
        val component = DefaultClasslike(
            emptyClasslikeParams.copy(
                publicFunctionsSummary = NoopSummaryList(),
                publicFunctionsDetails = Classlike.TitledList("Symbols", listOf(NoopSymbolDetailF)),
            ),
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>header
signature
hierarchy
relatedSymbols
descriptionDocs

  <h2>Summary</h2>
  <div>noop</div>
  <div class="list">
    <h2>Symbols</h2>
    <div>noop</div>
  </div>
</body>
            """.trim(),
        )
    }
}

internal val emptyClasslikeParams =
    Params(
        description = NoopClasslikeDescription(),
        displayLanguage = Language.KOTLIN, // We only use no-op components; this is fine
        nestedTypesSummary = emptySummaryList(),
        enumValuesSummary = emptySummaryList(),
        enumValuesDetails = emptyTitledList(),
        constantsSummary = emptySummaryList(),
        constantsDetails = emptyTitledList(),
        publicCompanionFunctionsSummary = emptySummaryList(),
        publicCompanionFunctionsDetails = emptyTitledList(),
        protectedCompanionFunctionsSummary = emptySummaryList(),
        protectedCompanionFunctionsDetails = emptyTitledList(),
        publicCompanionPropertiesSummary = emptySummaryList(),
        publicCompanionPropertiesDetails = emptyTitledList(),
        protectedCompanionPropertiesSummary = emptySummaryList(),
        protectedCompanionPropertiesDetails = emptyTitledList(),
        publicPropertiesSummary = emptySummaryList(),
        publicPropertiesDetails = emptyTitledList(),
        protectedPropertiesSummary = emptySummaryList(),
        protectedPropertiesDetails = emptyTitledList(),
        publicFunctionsSummary = emptySummaryList(),
        publicFunctionsDetails = emptyTitledList(),
        protectedFunctionsSummary = emptySummaryList(),
        protectedFunctionsDetails = emptyTitledList(),
        publicConstructorsSummary = emptySummaryList(),
        publicConstructorsDetails = emptyTitledList(),
        protectedConstructorsSummary = emptySummaryList(),
        protectedConstructorsDetails = emptyTitledList(),
        extensionFunctionsSummary = emptySummaryList(),
        extensionFunctionsDetails = emptyTitledList(),
        extensionPropertiesSummary = emptySummaryList(),
        extensionPropertiesDetails = emptyTitledList(),
        inheritedConstants = emptyInheritedSymbolsList(),
        inheritedFunctions = emptyInheritedSymbolsList(),
        inheritedProperties = emptyInheritedSymbolsList(),
    )
