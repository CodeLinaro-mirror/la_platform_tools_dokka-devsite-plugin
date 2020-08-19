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

import com.google.devsite.components.Parameter
import com.google.devsite.renderer.Language
import kotlinx.html.Entities
import kotlinx.html.FlowContent
import kotlinx.html.span
import kotlinx.html.unsafe

/** Default implementation of a function parameter. */
internal class DefaultParameter(
    override val data: Parameter.Params
) : Parameter {
    private val isLambda = data.receiver != null || data.lambdaParams.isNotEmpty()

    init {
        require(data.language != Language.JAVA || !isLambda) {
            "Lambda functions shouldn't be documented in Java."
        }
    }

    override fun render(html: FlowContent) = html.run {
        for (annotation in data.annotations) {
            annotation.render(this)
            +Entities.nbsp
        }

        when (data.language) {
            Language.JAVA -> {
                data.primary.render(this)
                +Entities.nbsp
                span("identifier") { +data.name }
            }
            Language.KOTLIN -> {
                span("identifier") { +data.name }
                span("symbol") { +":" }
                +Entities.nbsp

                for (modifier in data.lambdaModifiers) {
                    +modifier
                    +Entities.nbsp
                }

                if (data.receiver != null) {
                    data.receiver.render(this)
                    span("symbol") { +"." }
                }

                if (isLambda) span("symbol") { +"(" }
                for (type in data.lambdaParams) {
                    type.render(this)
                    if (type !== data.lambdaParams.last()) {
                        +","
                        +Entities.nbsp
                    }
                }
                if (isLambda) {
                    span("symbol") {
                        +") "
                        unsafe { +"&rarr;" }
                        +" "
                    }
                }

                data.primary.render(this)
            }
        }
    }
}
