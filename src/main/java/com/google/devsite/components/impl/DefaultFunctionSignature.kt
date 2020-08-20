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

import com.google.devsite.components.FunctionSignature
import kotlinx.html.Entities
import kotlinx.html.FlowContent

/** Default implementation of a function signature. */
internal class DefaultFunctionSignature(
    override val data: FunctionSignature.Params
) : FunctionSignature {
    override fun render(html: FlowContent) = html.run {
        if (data.receiver != null) {
            data.receiver.render(this)
            +"."
        }

        data.name.render(this)
        +"("
        for (parameter in data.parameters) {
            parameter.render(this)
            if (parameter !== data.parameters.last()) {
                +","
                +Entities.nbsp
            }
        }
        +")"
    }
}
