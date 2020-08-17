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

import com.google.devsite.components.ParameterType
import kotlinx.html.Entities
import kotlinx.html.FlowContent
import kotlinx.html.span

/** Default implementation of a function parameter type. */
internal class DefaultParameterType(
    override val data: ParameterType.Params
) : ParameterType {
    override fun render(html: FlowContent) = html.run {
        data.type.render(this)
        if (data.generics.isNotEmpty()) {
            span("symbol") { +"<" }
            for (generic in data.generics) {
                generic.render(this)
                if (generic !== data.generics.last()) {
                    +","
                    +Entities.nbsp
                }
            }
            span("symbol") { +">" }
        }
    }
}
