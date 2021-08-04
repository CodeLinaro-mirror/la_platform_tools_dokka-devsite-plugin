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
import kotlinx.html.FlowContent
import kotlinx.html.h2
import kotlinx.html.hr
import kotlinx.html.p

/** Default implementation of class-like pages. */
internal class DefaultClasslike(
    override val data: Classlike.Params
) : Classlike {
    override fun render(into: FlowContent) = into.run {
        p {
            data.signature.render(this)
        }
        data.hierarchy.render(this)
        data.relatedSymbols.render(this)
        data.description.render(into, separator = null, header = { hr() })

        val allSummarySections =
            data.symbolTypes.map { it.first }.filter { it.hasContent() } +
            data.inheritedTypes.filter { it.hasContent() }
        allSummarySections.render(into, separator = null, header = { h2 { +"Summary" } })

        for (symbolType in data.symbolTypes.map { it.second }) {
            symbolType.symbols.render(into, separator = null, header = { h2 { +symbolType.title } })
        }
    }
}
