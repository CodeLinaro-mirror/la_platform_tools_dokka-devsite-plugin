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

import com.google.devsite.components.Link
import com.google.devsite.renderer.Language

/** Represents a symbol type such as function parameter types. */
internal interface TypeProjectionComponent : SymbolBase {
    val data: Params

    override fun length(): Int {
        val typeSize = data.type.length()
        val annotationSize = data.annotationComponents.sumOf { it.length() }
        val genericsSize = data.generics.sumOf { it.length() + 2 }

        return typeSize + annotationSize + genericsSize
    }

    open class Params(
        open val type: Link,
        open val annotationComponents: List<AnnotationComponent> = emptyList(),
        open val nullable: Boolean = false,
        open val generics: List<TypeProjectionComponent> = emptyList(),
        open val displayLanguage: Language
    )

    val nullable: Boolean
        get() = data.nullable || data.annotationComponents.any { it.name == "Nullable" }
}
