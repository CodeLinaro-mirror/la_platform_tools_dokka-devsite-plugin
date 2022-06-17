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

package com.google.devsite.components.pages

import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.Link
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language

/** Represents the package summary page. */
internal interface PackageSummary : ContextFreeComponent {
    val data: Params

    data class Params(
        val displayLanguage: Language,
        val description: List<ContextFreeComponent>,
        val interfaces: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>>,
        val classes: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>>,
        val enums: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>>,
        val exceptions: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>>,
        val annotations: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>>,
        val typeAliases: SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>>,
        val topLevelConstantsSummary: SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>>,
        val topLevelPropertiesSummary: SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>>,
        val topLevelFunctionsSummary: SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>>,
        val extensionPropertiesSummary: SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>>,
        val extensionFunctionsSummary: SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>>,
        val topLevelConstants: List<SymbolDetail>,
        val topLevelProperties: List<SymbolDetail>,
        val topLevelFunctions: List<SymbolDetail>,
        val extensionProperties: List<SymbolDetail>,
        val extensionFunctions: List<SymbolDetail>
    )
}
