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

import com.google.devsite.components.symbols.Parameter
import com.google.devsite.renderer.Language
import kotlinx.html.Entities
import kotlinx.html.FlowContent

/** Default implementation of a function parameter. */
internal class DefaultParameter(
    override val data: Parameter.Params
) : Parameter {
    init {
        validate()
    }

    override fun render(html: FlowContent) = html.run {
        for (annotation in data.annotations) {
            annotation.render(this)
            +Entities.nbsp
        }

        when (data.displayLanguage) {
            Language.JAVA -> {
                data.primary.render(this)
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

                for (modifier in data.lambdaModifiers) {
                    +modifier
                    +Entities.nbsp
                }

                if (data.receiver != null) {
                    data.receiver.render(this)
                    +"."
                }

                if (data.isLambda) +"("
                for (type in data.lambdaParams) {
                    type.render(this)
                    if (type !== data.lambdaParams.last()) {
                        +","
                        +Entities.nbsp
                    }
                }
                if (data.isLambda) {
                    +")"
                    +Entities.nbsp
                    +"->"
                    +Entities.nbsp
                }

                for (modifier in data.modifiers) {
                    +modifier
                    +Entities.nbsp
                }

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
