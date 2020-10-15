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
import kotlinx.html.FlowContent
import kotlinx.html.h2
import kotlinx.html.hr
import kotlinx.html.p

/** Default implementation of class-like pages. */
internal class DefaultClasslike(
    override val data: Classlike.Params
) : Classlike {
    override fun render(html: FlowContent) = html.run {
        p {
            data.signature.render(this)
        }
        data.hierarchy.render(this)
        data.relatedSymbols.render(this)

        hr()

        for (detail in data.description) {
            detail.render(this)
        }

        h2 {
            +"Summary"
        }

        for ((summary, _) in data.symbolTypes) {
            summary.render(this)
        }

        val symbolTypes = data.symbolTypes.filter { (_, symbolType) ->
            symbolType.symbols.isNotEmpty()
        }
        for ((_, symbolType) in symbolTypes) {
            h2 { +symbolType.title }

            for (symbol in symbolType.symbols) {
                symbol.render(this)
            }
        }
    }
}
