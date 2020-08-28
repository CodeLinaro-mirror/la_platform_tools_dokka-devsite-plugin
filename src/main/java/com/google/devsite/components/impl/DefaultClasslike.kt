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

import com.google.devsite.components.Classlike
import kotlinx.html.FlowContent
import kotlinx.html.h2
import kotlinx.html.hr
import kotlinx.html.p

/** Default implementation of class-like pages. */
internal class DefaultClasslike(
    override val data: Classlike.Params
) : Classlike {
    override fun render(html: FlowContent) = html.run {
        p { +"TODO(b/166518424) class signature" }
        p { +"TODO(b/166518951) inheritance hierarchy" }
        p { +"TODO(b/166518636) direct subclasses" }
        p { +"TODO(b/166518636) indirect subclasses" }

        hr()

        for (detail in data.description) {
            detail.render(this)
        }

        h2 {
            +"Summary"
        }

        p { +"Nested *" }
        p { +"Enum values" }
        p { +"Constants" }
        p { +"Public fields" }
        p { +"Protected fields" }
        p { +"Public constructors" }
        p { +"Protected constructors" }
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
