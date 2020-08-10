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

import com.google.devsite.components.SummaryItem
import kotlinx.html.TBODY
import kotlinx.html.td
import kotlinx.html.tr

/** Default implementation of the two-pane layout item. */
internal class DefaultSummaryItem(
    override val data: SummaryItem.Params
) : SummaryItem {
    override fun render(html: TBODY) {
        html.tr {
            td {
                data.title.render(this)
            }
            td {
                data.description.render(this)
            }
        }
    }
}
