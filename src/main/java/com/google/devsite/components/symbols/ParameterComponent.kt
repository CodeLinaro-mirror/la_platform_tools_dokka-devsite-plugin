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

package com.google.devsite.components.symbols

import com.google.devsite.renderer.Language

/** Represents a function or method parameter. */
internal interface ParameterComponent : SymbolBase {
    val data: Params

    override fun length(): Int {
        var result = data.name.length

        result += data.defaultValue?.length ?: 0
        result += data.annotationComponents.sumBy { it.length() }
        result += data.modifiers.sumBy { it.length }
        result += data.type.length()

        return result
    }

    class Params(
        val displayLanguage: Language,
        val name: String,
        val modifiers: List<String> = emptyList(),
        val type: TypeProjectionComponent,
        val annotationComponents: List<AnnotationComponent> = emptyList(),
        val defaultValue: String? = null
    )

    val nullable: Boolean
        get() = data.type.nullable || data.annotationComponents.any { it.name == "Nullable" }
}
