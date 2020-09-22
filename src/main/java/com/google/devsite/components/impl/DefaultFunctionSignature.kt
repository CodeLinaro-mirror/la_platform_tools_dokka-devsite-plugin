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

import com.google.devsite.components.symbols.FunctionSignature
import kotlinx.html.Entities
import kotlinx.html.FlowContent
import kotlinx.html.br

/** Default implementation of a function signature. */
internal class DefaultFunctionSignature(
    override val data: FunctionSignature.Params
) : FunctionSignature {
    override fun render(html: FlowContent) = html.run {
        if (data.receiver != null) {
            data.receiver.render(this)
            +"."
        }
        val shouldBreak = shouldBreak()

        data.name.render(this)
        +"("
        if (shouldBreak) br()
        for (parameter in data.parameters) {
            if (shouldBreak) repeat(4) { +Entities.nbsp }
            parameter.render(this)

            if (parameter !== data.parameters.last()) {
                +","
                if (shouldBreak) br() else +Entities.nbsp
            }
        }
        if (shouldBreak) br()
        +")"
    }

    /** Uses the estimated function size to guess if it will overflow. */
    private fun shouldBreak(): Boolean {
        val nameSize = data.name.length()
        val allParams = listOfNotNull(data.receiver) + data.parameters
        val paramSize = allParams.sumBy { it.length() + 2 }

        val totalSize = nameSize + paramSize
        return totalSize >= 70
    }
}
