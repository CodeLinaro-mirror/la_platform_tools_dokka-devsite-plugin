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

package com.google.devsite.util

import com.google.devsite.renderer.converters.allAnnotations
import com.google.devsite.renderer.converters.functionSignatureComparator
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.driOrNull
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DRIExtraContainer
import org.jetbrains.dokka.links.DRIExtraProperty
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.properties.ExtraProperty

/**
 * An [ExtraProperty] for a [DPackage] containing lists of compose-specific [DFunctionGroup]s which
 * are treated differently than other top-level functions.
 *
 * These special functions are [composables] (see [isComposable]) and modifiers (see [isModifier]).
 */
internal class ComposeProperties(
    initialComposableList: List<DFunctionGroup>,
    initialModifierList: List<DFunctionGroup>,
) : ExtraProperty<DPackage> {
    /**
     * A list of the composables defined in the package (see [isComposable]).
     *
     * This list is sorted in name order.
     */
    val composables = initialComposableList.sortedBy { it.name }

    /**
     * A list of the modifiers defined in the package (see [isModifier]).
     *
     * This list is sorted in name order.
     */
    val modifiers = initialModifierList.sortedBy { it.name }

    /** Key for finding this [ExtraProperty]. */
    object PropertyKey : ExtraProperty.Key<DPackage, ComposeProperties>

    /** Key identifying this [ExtraProperty]. */
    override val key: ExtraProperty.Key<DPackage, *> = PropertyKey

    companion object {
        private val COMPOSABLE_DRI =
            DRI(packageName = "androidx.compose.runtime", classNames = "Composable")
        private val MODIFIER_TYPE_REFERENCE =
            TypeConstructor("androidx.compose.ui.Modifier", params = emptyList())

        /** The [DFunctionGroup.type] property for a composable. */
        const val COMPOSABLE_TYPE = "composable"

        /** The [DFunctionGroup.type] property for a modifier. */
        const val MODIFIER_TYPE = "modifier"

        /**
         * Returns whether the [dFunction] is a composable function, which means it is top-level and
         * annotated with `@Composable` (in any source set).
         */
        fun isComposable(dFunction: DFunction): Boolean {
            return dFunction.isTopLevel() &&
                dFunction.allAnnotations().any { it.dri == COMPOSABLE_DRI }
        }

        /**
         * Returns whether the [dFunction] is a modifier function, which means it is top-level and
         * an extension on `Modifier`.
         */
        fun isModifier(dFunction: DFunction): Boolean {
            return dFunction.isTopLevel() &&
                dFunction.receiver?.dri?.callable?.receiver == MODIFIER_TYPE_REFERENCE
        }

        /** Whether the function is top-level (not defined within a class). */
        private fun DFunction.isTopLevel(): Boolean = dri.classNames == null

        /**
         * If the [dFunction] is part of a function group (see [isComposable] and [isModifier]),
         * returns the [DRI] of that function group. Otherwise, returns null.
         */
        fun driForFunctionGroup(dFunction: DFunction): DRI? {
            val type =
                when {
                    isComposable(dFunction) -> COMPOSABLE_TYPE
                    isModifier(dFunction) -> MODIFIER_TYPE
                    else -> return null
                }
            return DRI(
                packageName = dFunction.dri.packageName,
                classNames = "${dFunction.name}.$type",
                extra = FunctionGroupDriExtra.extraContainer,
            )
        }
    }

    /** A group of top-level functions from the same package with the same name. */
    class DFunctionGroup(initialFunctionList: List<DFunction>, val type: String) : Documentable() {
        /** The functions in this group, sorted by signature. */
        val functions =
            initialFunctionList.sortedWith(
                // First group any extension functions by receiver
                // type (non-extension functions will appear first).
                compareBy<DFunction> { it.receiver?.type?.driOrNull?.toString() }
                    // Then sort by signature.
                    .then(functionSignatureComparator)
            )

        /** The name shared by all functions in this group. */
        override val name: String =
            functions
                .map { it.name }
                .toSet()
                .let { names ->
                    names.singleOrNull()
                        ?: error(
                            "All functions in a DFunctionGroup must have the same name (found: $names)"
                        )
                }

        /** The qualified package name which these functions are defined in. */
        val packageName: String =
            functions
                .map { it.dri.packageName }
                .toSet()
                .let { packages ->
                    packages.singleOrNull()
                        ?: error(
                            "All functions in a DFunctionGroup must have the same package (found: $packages)"
                        )
                }

        /**
         * An identifier for the function group. [FunctionGroupDriExtra] is used to mark it as
         * standing for a function group.
         */
        override val dri =
            DRI(
                packageName = packageName,
                classNames = "$name.$type",
                extra = FunctionGroupDriExtra.extraContainer,
            )

        override val children: List<Documentable>
            get() = functions

        /**
         * The documentation for the first function in the group. This should be used to create
         * summary text, which only uses the first sentence of the documentation, which is typically
         * similar between the functions in a group.
         */
        override val documentation: SourceSetDependent<DocumentationNode>
            get() = functions.first().documentation

        /**
         * Source sets which this function group exists in: an aggregation of source sets for each
         * function of the group.
         */
        override val sourceSets by lazy { functions.flatMap { it.sourceSets }.toSet() }

        /**
         * [Documentable.expectPresentInSet] is the source set which contains the `expect`
         * declaration for the documentable, if one exists.
         *
         * If all [functions] in the group are expect/actuals and the expects are all in the same
         * source set, returns that source set. Otherwise, returns null.
         */
        override val expectPresentInSet: DokkaConfiguration.DokkaSourceSet? by lazy {
            functions.map { it.expectPresentInSet }.toSet().singleOrNull()
        }
    }

    /** A [DRIExtraProperty] which labels the [DRI] as representing a function group. */
    object FunctionGroupDriExtra : DRIExtraProperty<FunctionGroupDriExtra>() {
        /** The encoded [DRI.extra] to use for a function group DRI. */
        val extraContainer =
            DRIExtraContainer().also { it[FunctionGroupDriExtra] = FunctionGroupDriExtra }.encode()
    }
}

/** Returns the list of composables in the package (empty if there are none). */
internal fun DPackage.composables(): List<ComposeProperties.DFunctionGroup> {
    return extra[ComposeProperties.PropertyKey]?.composables ?: emptyList()
}

/** Returns the list of modifiers in the package (empty if there are none). */
internal fun DPackage.composeModifiers(): List<ComposeProperties.DFunctionGroup> {
    return extra[ComposeProperties.PropertyKey]?.modifiers ?: emptyList()
}

/** Returns whether the package has any composables or modifiers. */
fun DPackage.hasComposeProperties(): Boolean = extra[ComposeProperties.PropertyKey] != null

/**
 * Returns whether the DRI is for a [ComposeProperties.DFunctionGroup], that is, is has a
 * [ComposeProperties.FunctionGroupDriExtra].
 */
fun DRI.isForFunctionGroup(): Boolean =
    DRIExtraContainer(extra)[ComposeProperties.FunctionGroupDriExtra] != null

/** Returns whether the function is part of a composable or modifier function group. */
fun DFunction.isInDFunctionGroup(): Boolean {
    return ComposeProperties.isComposable(this) || ComposeProperties.isModifier(this)
}
