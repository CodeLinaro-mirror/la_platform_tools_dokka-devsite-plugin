/*
 * Copyright 2026 The Android Open Source Project
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

package com.google.devsite.transformers

import com.google.devsite.util.ComposeProperties
import com.google.devsite.util.ComposeProperties.Companion.COMPOSABLE_TYPE
import com.google.devsite.util.ComposeProperties.Companion.MODIFIER_TYPE
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer

/**
 * A transformer which finds composable and modifier functions in a [DPackage] and puts them in a
 * [ComposeProperties] extra.
 *
 * Only transforms the packages if [enabled] is true.
 */
class ComposeTransformer(val enabled: Boolean) : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule {
        return if (enabled) {
            original.copy(packages = original.packages.map { it.transform() })
        } else {
            original
        }
    }

    /**
     * Finds the composables and modifiers in the package and updates the [DPackage] to add an extra
     * [ComposeProperties] if there are any.
     */
    private fun DPackage.transform(): DPackage {
        // Find the composables and modifiers in the package.
        val (composableFunctions, modifierAndOtherFunctions) =
            functions.partition { ComposeProperties.isComposable(it) }
        val modifierFunctions =
            modifierAndOtherFunctions.filter { ComposeProperties.isModifier(it) }

        // Group overloads together so that each group has a unique name.
        val composableGroups = composableFunctions.groupBy { it.name }
        val modifierGroups = modifierFunctions.groupBy { it.name }

        // Create a DFunctionGroup for each group of same-named functions.
        val composables =
            composableGroups.map { (_, composableFunctions) ->
                ComposeProperties.DFunctionGroup(composableFunctions, COMPOSABLE_TYPE)
            }
        val modifiers =
            modifierGroups.map { (_, modifierFunctions) ->
                ComposeProperties.DFunctionGroup(modifierFunctions, MODIFIER_TYPE)
            }

        // If there are any composables or modifiers, attach them to the DPackage.
        return if (composables.isNotEmpty() || modifiers.isNotEmpty()) {
            val composeProperties = ComposeProperties(composables, modifiers)
            val extraProperties = extra + composeProperties
            copy(extra = extraProperties)
        } else {
            this
        }
    }
}
