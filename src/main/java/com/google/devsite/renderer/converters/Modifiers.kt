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

package com.google.devsite.renderer.converters

import org.jetbrains.dokka.model.AdditionalModifiers
import org.jetbrains.dokka.model.WithAbstraction
import org.jetbrains.dokka.model.WithVisibility
import org.jetbrains.dokka.model.properties.WithExtraProperties

/** @return the complete list of modifiers for this type */
internal fun <T> T.modifiers(
    withVisibility: Boolean = true
): List<String> where T : WithAbstraction,
                      T : WithVisibility,
                      T : WithExtraProperties<*> {
    val visibilityModifiers = listOf(visibility.values.single().name)
        .takeIf { withVisibility }.orEmpty()
    val baseModifiers = modifier.values.map { it.name }
    val extraModifiers = extra.allOfType<AdditionalModifiers>().flatMap { modifiers ->
        modifiers.content.values.single().map { it.name }
    }

    return visibilityModifiers + extraModifiers + baseModifiers
}

/** @return true if the modifiers represent a constant symbol, false otherwise */
internal fun isConstant(modifiers: List<String>) =
    "const" in modifiers || "static" in modifiers && "final" in modifiers
