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
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.render
import kotlinx.html.Entities
import kotlinx.html.FlowContent
import kotlinx.html.br
import kotlinx.html.span
import kotlinx.html.unsafe

/** Default implementation of a function signature. */
internal class DefaultFunctionSignature(
    override val data: FunctionSignature.Params
) : FunctionSignature {
    override fun render(into: FlowContent) = into.run {
        data.typeParameters.render(this)
        if (data.typeParameters.isNotEmpty()) +" "

        if (data.receiver != null) {
            data.receiver.render(this)
            +"."
        }
        val shouldBreak = shouldBreak()

        if (data.isDeprecated) {

            // Bug in kotlinx: <del> tag adds a new line before and after using it
            // https://github.com/Kotlin/kotlinx.html/issues/113
            // Manually declare <del> instead
            span {
                unsafe { +"<del>" }
                data.name.render(into)
                unsafe { +"</del>" }
            }
        } else {
            data.name.render(this)
        }
        data.parameters.render(into, shouldBreak)
    }

    /** Uses the estimated function size to guess if it will overflow. */
    private fun shouldBreak(): Boolean {
        val nameSize = data.name.length()
        val allParams = data.typeParameters + listOfNotNull(data.receiver) + data.parameters
        val paramSize = allParams.sumBy { it.length() + 2 }

        val totalSize = nameSize + paramSize
        return totalSize >= 70
    }
}

internal fun List<ContextFreeComponent>.render(
    into: FlowContent,
    shouldBreak: Boolean = false,
    brackets: String = "()"
) = into.run {
    +brackets[0].toString()
    if (shouldBreak) br()
    for (parameter in this@render) {
        if (shouldBreak) repeat(4) { +Entities.nbsp }
        parameter.render(this)

        if (parameter !== last()) {
            +","
            if (shouldBreak) br() else +Entities.nbsp
        }
    }
    if (shouldBreak) br()
    +brackets[1].toString()
}
