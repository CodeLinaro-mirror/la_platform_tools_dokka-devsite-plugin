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
import com.google.devsite.components.symbols.TypeParameterComponent
import com.google.devsite.renderer.Language
import kotlinx.html.Entities.nbsp
import kotlinx.html.FlowContent

/** Default implementation of a function or class type parameter. */
internal class DefaultTypeParameterComponent(
    override val data: TypeParameterComponent.Params
) : TypeParameterComponent {
    init {
        validate()
    }

    override fun render(into: FlowContent) = render(into, true)

    /**
     * When rendering a single type param, e.g. in the left column of the parameters table, wrap <>s
     * When rendering a list of type params, group all within a single <>. Handled in List.render()
     */
    override fun render(into: FlowContent, angleBrackets: Boolean) = into.run {
        if (angleBrackets) { +"<" }
        data.annotationComponents.render(
            into, ShouldBreak.NO, separator = "", terminator = { +nbsp })
        when (data.displayLanguage) {
            Language.JAVA -> {
                +data.name
                // TODO: handle "implements"
                data.projections.render(into, ShouldBreak.NO,
                    header = { +nbsp; +"extends"; +nbsp; })
            }
            Language.KOTLIN -> {
                +data.name
                data.modifiers.render(into)
                // TODO: handle in/out
                data.projections.render(into, ShouldBreak.NO, header = { +nbsp; +":"; +nbsp })
            }
        }
        if (angleBrackets) { +">" }
    }

    override fun validate() {
        require(data.name.isNotEmpty()) {
            "Type parameters must have names."
        }
    }
}
