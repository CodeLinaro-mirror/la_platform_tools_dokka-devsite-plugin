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
import com.google.devsite.components.ParameterBase
import com.google.devsite.components.ParameterType
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.impl.DefaultParameter
import com.google.devsite.components.impl.DefaultParameterType
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
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
        Language.JAVA -> componentForJavaProjection(
            proj = param.type,
            name = param.name ?: "receiver",
            annotations = param.annotations()
        )
        Language.KOTLIN -> {
            val defaultValue = param.extra.allOfType<DefaultValue>().singleOrNull()?.value
                ?.takeUnless { isSummary }
            componentForKotlinProjection(
                proj = param.type,
                name = param.name.orEmpty(),
                defaultValue = defaultValue,
                annotations = param.annotations()
            )
        }
    }

    /** Returns the component for a type projection. */
    fun componentForProjection(proj: Projection): Parameter = when (displayLanguage) {
        Language.JAVA -> componentForJavaProjection(proj)
        Language.KOTLIN -> componentForKotlinProjection(proj)
    }

    private fun componentForJavaProjection(
        proj: Projection,
        name: String = "",
        annotations: List<Annotations.Annotation> = emptyList()
    ): Parameter {
        return DefaultParameter(
            Parameter.Params(
                isLambda = false,
                name = name,
                primary = proj.toComponent(),
                annotations = annotations.annotationComponents(pathProvider),
                displayLanguage = Language.JAVA
            )
        )
    }

    private fun componentForKotlinProjection(
        proj: Projection,
        name: String = "",
        defaultValue: String? = null,
        annotations: List<Annotations.Annotation> = emptyList()
    ): Parameter {
        val isLambda = proj.isLambda()

        val receiver = proj.receiver()
        val primaryType = if (isLambda) {
            // Get the return type of the lambda
            componentForKotlinProjection(proj.asTypeConstructor().projections.last())
        } else {
            proj.toComponent()
        }
        val lambdaModifiers: List<String> = if (proj.isLambda(suspendOnly = true)) {
            listOf("suspend")
        } else {
            emptyList()
        }
        val lambdaParams: List<Parameter> = if (isLambda) {
            // Always ignore the return type of the lambda since that's handled by primaryType.
            val lambdaProjections = proj.asTypeConstructor().projections.dropLast(1)
            if (receiver == null) {
                lambdaProjections.map(::componentForKotlinProjection)
            } else {
                // If the receiver is available, we also ignore the first type
                lambdaProjections.drop(1).map(::componentForKotlinProjection)
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
                annotations = annotations.annotationComponents(pathProvider),
                defaultValue = defaultValue
            )
        )
    }

    /** Converts a lambda receiver projection to its type component if available. */
    private fun Projection.receiver(): Parameter? = when (this) {
        is TypeConstructor -> if (modifier == FunctionModifiers.EXTENSION) {
            componentForProjection(projections.first())
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
        if (this is Nullable) {
            return inner.toComponent()
        }

        val generics: List<ParameterBase> = when (this) {
            is TypeConstructor -> projections.map { componentForProjection(it) }
            is TypeParameter, is PrimitiveJavaType, is UnresolvedBound,
            Star, Void, JavaObject -> emptyList()
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

    /** Gets the type constructor of a *lambda param only*. */
    private fun Projection.asTypeConstructor(): TypeConstructor = when (this) {
        is Variance<*> -> inner.asTypeConstructor()
        is Nullable -> inner.asTypeConstructor()
        else -> this as TypeConstructor
    }
}
