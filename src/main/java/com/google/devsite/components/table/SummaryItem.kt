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

package com.google.devsite.components.table

import com.google.devsite.components.ContextFreeComponent
import kotlinx.html.COL
import kotlinx.html.DIV
import kotlinx.html.TABLE
import kotlinx.html.attributesMapOf
import kotlinx.html.col
import kotlinx.html.colGroup
import kotlinx.html.table
import kotlinx.html.visit

/** Represents a summary table row. */
internal interface SummaryItem : RowComponent {
    val data: Params

    fun layout(into: DIV, contents: TABLE.() -> Unit) = into.run {
        table("responsive") {
            colGroup {
                COL(attributesMapOf("width", "40%"), consumer).visit {}
                col() // Last col should be floating-width. (This method can't set "width".)
            }
            contents()
        }
    }

    interface Params {
        val description: ContextFreeComponent
    }
}
