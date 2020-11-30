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

import com.google.devsite.components.symbols.TypeParameter
import com.google.devsite.renderer.Language
import kotlinx.html.Entities
import kotlinx.html.FlowContent

/** Default implementation of a function parameter. */
internal class DefaultTypeParameter(
    override val data: TypeParameter.Params
) : TypeParameter {
    init {
        validate()
    }

    override fun render(html: FlowContent) = html.run {
        +"<"
        for (annotation in data.annotations) {
            annotation.render(this)
            +Entities.nbsp
        }
        when (data.displayLanguage) {
            Language.JAVA -> {
                +data.name
                if (data.projections.isNotEmpty()) {
                    +Entities.nbsp
                    // TODO: handle "implements"
                    +"extends"
                    for (projection in data.projections.dropLast(1)) {
                        +Entities.nbsp
                        projection.render(this)
                        +","
                    }
                    +Entities.nbsp
                    data.projections.last().render(this)
                }
            }
            Language.KOTLIN -> {
                +data.name

                for (modifier in data.modifiers) {
                    +modifier
                    +Entities.nbsp
                }

                if (data.projections.isNotEmpty()) {
                    +Entities.nbsp
                    +":"
                    // TODO: handle in/out
                    for (projection in data.projections.dropLast(1)) {
                        +Entities.nbsp
                        projection.render(this)
                        +","
                    }
                    +Entities.nbsp
                    data.projections.last().render(this)
                }
            }
        }
        +">"
    }

    override fun validate() {
        require(data.name.isNotEmpty()) {
            "Type parameters must have names."
        }
    }
}
