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

import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.pages.PackageSummary
import com.google.devsite.components.table.SummaryList
import com.google.devsite.renderer.Language
import kotlinx.html.FlowContent
import kotlinx.html.h2

/** Default implementation of the package summary page. */
internal class DefaultPackageSummary(
    override val data: PackageSummary.Params
) : PackageSummary {
    override fun render(html: FlowContent) = html.run {
        for (detail in data.description) {
            detail.render(this)
        }

        renderSummary(data.interfaces, "Interfaces")
        renderSummary(data.classes, "Classes")
        renderSummary(data.enums, "Enums")
        renderSummary(data.exceptions, "Exceptions")
        renderSummary(data.annotations, "Annotations")

        if (data.displayLanguage == Language.KOTLIN) {
            renderSummary(data.typeAliases, "Type aliases")

            renderSummary(data.topLevelConstantsSummary, "Constants summary")
            renderSummary(data.topLevelPropertiesSummary, "Top-level properties summary")
            renderSummary(data.topLevelFunctionsSummary, "Top-level functions summary")
            renderSummary(data.extensionPropertiesSummary, "Extension properties summary")
            renderSummary(data.extensionFunctionsSummary, "Extension functions summary")

            renderDetails(data.topLevelConstants, "Constants")
            renderDetails(data.topLevelProperties, "Top-level properties")
            renderDetails(data.topLevelFunctions, "Top-level functions")
            renderDetails(data.extensionProperties, "Extension properties")
            renderDetails(data.extensionFunctions, "Extension functions")
        }
    }

    private fun FlowContent.renderSummary(summary: SummaryList, title: String) {
        if (summary.hasContent()) {
            h2 { +title }
        }
        summary.render(this)
    }

    private fun FlowContent.renderDetails(details: List<ContextFreeComponent>, title: String) {
        if (details.isNotEmpty()) {
            h2 { +title }
        }

        for (detail in details) {
            detail.render(this)
        }
    }
}
