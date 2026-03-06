/*
 * Copyright 2026 The Android Open Source Project
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

import com.google.devsite.components.pages.FunctionGroupPage
import com.google.devsite.components.render
import kotlinx.html.FlowContent
import kotlinx.html.h2

/** Default implementation of [FunctionGroupPage]. */
internal data class DefaultFunctionGroupPage(override val data: FunctionGroupPage.Params) :
    FunctionGroupPage {
    override fun render(into: FlowContent) =
        into.run {
            // Optional  KMP header
            data.header?.render(into)

            // Summary table section
            h2 { +"Functions summary" }
            data.summary.render(this)

            // Detail section for each function
            data.detail.render(this, separator = null, header = { h2 { +"Functions" } })
        }
}
