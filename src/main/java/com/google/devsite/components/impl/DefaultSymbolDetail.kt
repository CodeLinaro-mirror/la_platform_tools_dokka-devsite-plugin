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
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolDetail.SymbolType
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableTitle
import com.google.devsite.renderer.Language
import kotlinx.html.Entities
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.br
import kotlinx.html.div
import kotlinx.html.h3
import kotlinx.html.pre

/** Default implementation of a fully documented function. */
internal class DefaultSymbolDetail(
    override val data: SymbolDetail.Params
) : SymbolDetail {
    override fun render(into: FlowContent) = into.div {
        for (anchor in data.anchors.drop(1)) {
            a { attributes["name"] = anchor }
        }

        h3("api-name") {
            data.anchors.firstOrNull()?.let { attributes["id"] = it }

            +data.name
        }
        pre("api-signature no-pretty-print") {
            for (annotation in data.annotations) {
                annotation.render(this)
                br()
            }

            for (modifier in data.modifiers) {
                +modifier
                +Entities.nbsp
            }

            when (data.displayLanguage) {
                Language.JAVA -> {
                    if (data.symbolType != SymbolType.CONSTRUCTOR) {
                        data.returnType.render(this)
                        +Entities.nbsp
                    }

                    data.signature.render(this)
                }
                Language.KOTLIN -> {
                    +data.symbolType.keyword
                    if (data.symbolType != SymbolType.CONSTRUCTOR) +Entities.nbsp
                    data.signature.render(this)

                    if (data.symbolType != SymbolType.CONSTRUCTOR) {
                        +":"
                        +Entities.nbsp
                        data.returnType.render(this)
                    }
                }
            }
        }

        for (detail in data.metadata.sortedBy { descriptionSorter(it) }) {
            detail.render(this)
        }
    }
}

private fun descriptionSorter(component: ContextFreeComponent): Int {
    return when (component) {
        is SummaryList -> {
            val tableName = (component.data.header as TableTitle).data.title
            when (tableName) {
                "Parameters" -> 1
                "Returns" -> 2
                "Throws" -> 3
                "See also" -> 4
                else -> 5 // Unknown tables go at the end
            }
        } // Parameters Table > Returns Table
        else -> 0 // Preserve input order for all other cases, and put them before the tables
    }
}
