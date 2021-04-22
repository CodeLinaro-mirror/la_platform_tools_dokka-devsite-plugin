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

import com.google.devsite.components.ShouldBreak
import com.google.devsite.components.render
import com.google.devsite.components.symbols.Parameter
import com.google.devsite.renderer.Language
import kotlinx.html.Entities
import kotlinx.html.FlowContent
import kotlinx.html.HTMLTag
import kotlinx.html.unsafe

/** Default implementation of a function parameter. */
internal class DefaultParameter(
    override val data: Parameter.Params
) : Parameter {
    init {
        validate()
    }

    override fun render(into: FlowContent) = into.run {
        data.annotations.render(into, ShouldBreak.MAYBE, separator = "", terminator = { +" " })

        when (data.displayLanguage) {
            Language.JAVA -> {
                data.primary.render(into)
                if (data.name.isNotEmpty()) {
                    +Entities.nbsp
                    +data.name
                }
            }
            Language.KOTLIN -> {
                if (data.name.isNotEmpty()) {
                    +data.name
                    +":"
                    +Entities.nbsp
                }

                data.lambdaModifiers.render(into, terminator = { +Entities.nbsp })

                if (data.receiver != null) {
                    data.receiver.render(into)
                    +"."
                }

                if (data.isLambda) {
                    data.lambdaParams.render(into, ShouldBreak.MAYBE, brackets = "()")
                    +" "
                    nobr { +"->" }
                    +" "
                }

                data.modifiers.render(into, terminator = { +Entities.nbsp })

                data.primary.render(this)

                if (data.defaultValue != null) {
                    +" = ${data.defaultValue}"
                }
            }
        }
    }

    override fun validate() {
        require(!data.isLambda || data.displayLanguage != Language.JAVA) {
            "Lambda functions shouldn't be documented in Java."
        }
        require(data.isLambda || data.receiver == null) {
            "Parameter receivers don't make sense outside a lambda."
        }
        require(data.isLambda || data.lambdaModifiers.isEmpty()) {
            "Lambda modifiers don't make sense outside a lambda."
        }
        require(data.isLambda || data.lambdaParams.isEmpty()) {
            "Lambda params don't make sense outside a lambda."
        }
    }
}

/**
 * Recreates the functionality of non-standard <nobr> tag, used to prevent browser from inserting
 * line breaks in the given content to render.
 */
private fun FlowContent.nobr(render: HTMLTag.() -> Unit) {
    if (this !is HTMLTag) {
        return
    }
    unsafe { +"<span style=\"white-space: nowrap;\">" }
    render()
    unsafe { +"</span>" }
}
