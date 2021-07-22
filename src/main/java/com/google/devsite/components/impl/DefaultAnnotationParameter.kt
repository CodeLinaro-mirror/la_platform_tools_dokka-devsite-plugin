/*
 * Copyright 2021 The Android Open Source Project
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
import com.google.devsite.components.symbols.AnnotationValueAnnotationParameter
import com.google.devsite.components.symbols.ArrayValueAnnotationParameter
import com.google.devsite.components.symbols.NamedValueAnnotationParameter
import kotlinx.html.Entities
import kotlinx.html.FlowContent

internal class DefaultNamedValueAnnotationParameter(
    override val data: NamedValueAnnotationParameter.Params
) : NamedValueAnnotationParameter {
    override fun render(into: FlowContent) = into.run {
        data.name.render(into)
        +data.value
    }
}

internal class DefaultAnnotationValueAnnotationParameter(
    override val data: AnnotationValueAnnotationParameter.Params
) : AnnotationValueAnnotationParameter {
    override fun render(into: FlowContent) = into.run {
        data.name.render(into)
        data.annotationValue.render(into)
    }
}

internal class DefaultArrayValueAnnotationParameter(
    override val data: ArrayValueAnnotationParameter.Params
) : ArrayValueAnnotationParameter {
    override fun render(into: FlowContent) = into.run {
        data.name.render(into)
        data.innerAnnotationParameters.render(into, brackets = "[]", shouldBreak = ShouldBreak.NO)
    }
}

private fun String?.render(into: FlowContent) = into.run {
    if (this@render != null) {
        +this@render
        +Entities.nbsp
        +"="
        +Entities.nbsp
    }
}
