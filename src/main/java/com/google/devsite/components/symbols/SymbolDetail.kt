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

import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.EmptyModifiers
import com.google.devsite.renderer.converters.Modifiers

/** Represents a fully documented function or property. */
internal interface SymbolDetail<T : SymbolSignature> : ContextFreeComponent {
    val data: Params<T>

    data class Params<T : SymbolSignature>(
        val name: String,
        val returnType: TypeProjectionComponent,
        val symbolKind: SymbolKind,
        val signature: T,
        val anchors: LinkedHashSet<String>,
        val metadata: List<ContextFreeComponent>,
        val displayLanguage: Language,
        val modifiers: Modifiers = EmptyModifiers,
        val extFunctionClass: String? = null,
        val annotationComponents: List<AnnotationComponent> = emptyList()
    )

    /** Holds the Kotlin keywords for various symbol types. */
    enum class SymbolKind(val keyword: String) {
        READ_ONLY_PROPERTY("val"),
        PROPERTY("var"),
        FUNCTION("fun"),
        CONSTRUCTOR("")
    }
}
