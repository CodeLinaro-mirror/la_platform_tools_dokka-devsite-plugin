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

import com.google.devsite.components.Link
import com.google.devsite.components.Parameter
import com.google.devsite.components.ParameterType
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.impl.DefaultParameter
import com.google.devsite.components.impl.DefaultParameterType
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.DefaultValue
import org.jetbrains.dokka.model.FunctionModifiers
import org.jetbrains.dokka.model.JavaObject
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.PrimitiveJavaType
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.Star
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.TypeParameter
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Variance
import org.jetbrains.dokka.model.Void

/** Converts parameter and parameter-likes into their components. */
internal class ParameterDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider
) {
    /** Returns the component for a parameter. */
    fun componentForParameter(
        param: DParameter,
        isSummary: Boolean
    ): Parameter = when (displayLanguage) {
        Language.JAVA -> componentForJavaProjection(param.type, param.name ?: "receiver")
        Language.KOTLIN -> {
            val defaultValue = param.extra.allOfType<DefaultValue>().singleOrNull()?.value
                ?.takeUnless { isSummary }
            componentForKotlinProjection(param.type, param.name.orEmpty(), defaultValue)
        }
    }

    /** Returns the component for a type projection. */
    fun componentForProjection(proj: Projection): Parameter = when (displayLanguage) {
        Language.JAVA -> componentForJavaProjection(proj)
        Language.KOTLIN -> componentForKotlinProjection(proj)
    }

    private fun componentForJavaProjection(proj: Projection, name: String = ""): Parameter {
        return DefaultParameter(
            Parameter.Params(
                isLambda = false,
                name = name,
                primary = proj.toComponent(),
                // TODO(b/165104993): figure out path to implementing annotations
                annotations = emptyList(),
                displayLanguage = Language.JAVA
            )
        )
    }

    private fun componentForKotlinProjection(
        proj: Projection,
        name: String = "",
        defaultValue: String? = null
    ): Parameter {
        val isLambda = proj.isLambda()

        val receiver = proj.receiver()
        val primaryType = if (isLambda) {
            // Get the return type of the lambda
            (proj as TypeConstructor).projections.last().toComponent()
        } else {
            proj.toComponent()
        }
        val lambdaModifiers: List<String> = if (proj.isLambda(suspendOnly = true)) {
            listOf("suspend")
        } else {
            emptyList()
        }
        val lambdaParams: List<ParameterType> = if (isLambda) {
            // Always ignore the return type of the lambda since that's handled by primaryType.
            val lambdaProjections = (proj as TypeConstructor).projections.dropLast(1)
            if (receiver == null) {
                lambdaProjections.map { it.toComponent() }
            } else {
                // If the receiver is available, we also ignore the first type
                lambdaProjections.drop(1).map { it.toComponent() }
            }
        } else {
            emptyList()
        }

        return DefaultParameter(
            Parameter.Params(
                displayLanguage = Language.KOTLIN,
                isLambda = isLambda,
                name = name,
                receiver = receiver,
                lambdaModifiers = lambdaModifiers,
                lambdaParams = lambdaParams,
                primary = primaryType,
                // TODO(b/165104993): figure out path to implementing annotations
                annotations = emptyList(),
                defaultValue = defaultValue
            )
        )
    }

    /** Converts a lambda receiver projection to its type component if available. */
    private fun Projection.receiver(): ParameterType? = when (this) {
        is TypeConstructor -> if (modifier == FunctionModifiers.EXTENSION) {
            projections.first().toComponent()
        } else {
            null
        }
        is TypeParameter, is PrimitiveJavaType, is UnresolvedBound, Star, JavaObject, Void -> null
        is Nullable -> inner.receiver()
        is Variance<*> -> inner.receiver()
        else -> error("Unknown bound: $this")
    }

    /** Converts a documentable type to its type component, recursively expanding generics */
    private fun Projection.toComponent(): ParameterType {
        if (this is Variance<*>) {
            return inner.toComponent()
        }

        val generics: List<ParameterType> = when (this) {
            is TypeConstructor -> projections.map { it.toComponent() }
            is TypeParameter, is PrimitiveJavaType, is UnresolvedBound,
            Star, Void, JavaObject -> emptyList()
            is Nullable -> listOf(inner.toComponent())
            // TODO(b/166530498): support variance
            is Variance<*> -> listOf(inner.toComponent())
            else -> error("Unknown bound: $this")
        }

        return DefaultParameterType(
            ParameterType.Params(
                type = toLink(),
                generics = generics
            )
        )
    }

    /**
     * Converts a documentable type to a link component, assuming all generics have been resolved.
     */
    private fun Projection.toLink(): Link = when (this) {
        is TypeConstructor -> pathProvider.linkForReference(dri)
        is TypeParameter -> DefaultLink(
            Link.Params(
                name = name,
                url = ""
            )
        )
        Star -> DefaultLink(
            Link.Params(
                name = when (displayLanguage) {
                    Language.JAVA -> "?"
                    Language.KOTLIN -> "*"
                },
                url = ""
            )
        )
        Void -> when (displayLanguage) {
            Language.JAVA -> DefaultLink(Link.Params(name = "void", url = ""))
            Language.KOTLIN -> pathProvider.linkForReference(DRI("kotlin", "Unit"))
        }
        JavaObject -> when (displayLanguage) {
            Language.JAVA -> pathProvider.linkForReference(DRI("java.lang", "Object"))
            Language.KOTLIN -> pathProvider.linkForReference(DRI("kotlin", "Any"))
        }
        is PrimitiveJavaType -> when (displayLanguage) {
            Language.JAVA -> DefaultLink(Link.Params(name = name, url = ""))
            Language.KOTLIN -> pathProvider.linkForReference(DRI("kotlin", name.capitalize()))
        }
        is UnresolvedBound -> DefaultLink(Link.Params(name = name, url = ""))
        is Nullable -> inner.toLink()
        // TODO(b/166530498): support variance
        is Variance<*> -> inner.toLink()
        else -> error("Unknown bound: $this")
    }

    /** Determine whether or not a param is a lambda using the kotlin function type. */
    private fun Projection.isLambda(suspendOnly: Boolean = false): Boolean = when (this) {
        is TypeConstructor -> {
            val typeName = dri.classNames.orEmpty()
            val isStandardLambda = dri.packageName == "kotlin" && typeName.startsWith("Function")
            val isSuspendLambda =
                dri.packageName == "kotlin.coroutines" && typeName.startsWith("SuspendFunction")

            if (suspendOnly) {
                isSuspendLambda
            } else {
                isStandardLambda || isSuspendLambda
            }
        }
        is Nullable -> inner.isLambda()
        is Variance<*> -> inner.isLambda()
        is TypeParameter, is PrimitiveJavaType, is UnresolvedBound, Star, JavaObject, Void -> false
        else -> error("Unknown bound: $this")
    }
}
