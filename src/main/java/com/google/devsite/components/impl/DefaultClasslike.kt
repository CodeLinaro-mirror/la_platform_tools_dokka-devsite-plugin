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

import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.render
import com.google.devsite.joinMaybePrefix
import kotlinx.html.FlowContent
import kotlinx.html.h2
import kotlinx.html.hr
import kotlinx.html.p
import kotlinx.html.pre

/** Default implementation of class-like pages. */
internal data class DefaultClasslike(
    override val data: Classlike.Params
) : Classlike {
    override fun render(into: FlowContent) = into.run {
        p {
            pre {
                data.signature.render(this)
            }
        }
        data.hierarchy.render(this)
        data.relatedSymbols.render(this)
        data.description.render(into, separator = null, header = { hr() })
        // The ordering logic for these summaries is in Classlike.kt
        allVisibleSummaries.render(into, separator = null, header = { h2 { +"Summary" } })

        for (symbolType in allDetailsSections.filter { it.symbols.isNotEmpty() }) {
            symbolType.symbols.render(into, separator = null, header = { h2 { +symbolType.title } })
        }
    }

    override fun toString() = data.signature.toString() + " " +
        data.hierarchy + data.relatedSymbols +
        data.description.joinToString() +
        inheritedSummarySections.filter { it.hasContent() } +
        allSummarySections.filter { it.hasContent() }.joinMaybePrefix(prefix = "Summaries") +
        allDetailsSections.filter { it.symbols.isNotEmpty() }.joinMaybePrefix(prefix = "Details")
}
