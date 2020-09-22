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

import com.google.devsite.components.table.SummaryList
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.thead
import kotlinx.html.tr

/** Default implementation of the table view. */
internal class DefaultSummaryList(
    override val data: SummaryList.Params
) : SummaryList {
    override fun render(html: FlowContent) = html.run {
        if (!hasContent()) return

        div("devsite-table-wrapper") {
            table("responsive") {
                if (data.header != null) {
                    thead {
                        tr {
                            data.header.render(this)
                        }
                    }
                }

                tbody {
                    for (item in data.items) {
                        tr {
                            item.render(this)
                        }
                    }
                }
            }
        }
    }

    override fun hasContent() = data.items.isNotEmpty()
}
