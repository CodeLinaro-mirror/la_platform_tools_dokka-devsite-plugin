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

import com.google.devsite.components.FunctionSummary
import kotlinx.html.Entities
import kotlinx.html.TR
import kotlinx.html.code
import kotlinx.html.div
import kotlinx.html.td

/**
 * Default implementation of a function summary row.
 *
 * The return type and modifiers will be in the left column, while the signature and short
 * description are on the right. Annotations should not be included in the summary to save space.
 */
internal class DefaultFunctionSummary(
    override val data: FunctionSummary.Params
) : FunctionSummary {
    override fun render(html: TR) = html.run {
        td {
            code {
                for (modifier in data.modifiers) {
                    +modifier
                    +Entities.nbsp
                }

                data.returnType.render(this)
            }
        }

        td {
            attributes["width"] = "100%"

            div {
                code {
                    data.signature.render(this)
                }
            }

            data.description.render(this)
        }
    }
}
