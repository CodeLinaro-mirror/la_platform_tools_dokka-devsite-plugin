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

package com.google.devsite.components.testing

import com.google.devsite.components.length
import com.google.devsite.components.nobr
import com.google.devsite.components.render
import com.google.devsite.components.symbols.LambdaTypeProjectionComponent
import kotlinx.html.FlowContent

internal class NoopLambdaTypeProjectionComponent(
    private val params: List<String> = emptyList(),
    private val type: String,
    private val receiver: String? = null,
) : LambdaTypeProjectionComponent {
    override val data: LambdaTypeProjectionComponent.Params
        get() = throw NotImplementedError()

    override fun render(into: FlowContent) =
        into.run {
            if (receiver != null) +"$receiver."
            +"("
            params.render(into, nbsp = false)
            +") "
            nobr { +"->" }
            +" "
            +type
        }

    override fun length() = receiver.length + params.length() + type.length + "() -> ".length

    override fun simpleTypeString(): String {
        val receiverString = receiver?.let { "$it." } ?: ""
        return receiverString + "(" + params.joinToString(", ") + ") ->" + type
    }
}
