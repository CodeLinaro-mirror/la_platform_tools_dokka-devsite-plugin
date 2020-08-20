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

import com.google.devsite.components.Documentation
import com.google.devsite.components.FunctionSignature
import com.google.devsite.components.FunctionSummary
import com.google.devsite.components.Link
import com.google.devsite.components.Parameter
import com.google.devsite.components.ParameterType
import com.google.devsite.components.impl.DefaultDocumentation
import com.google.devsite.components.impl.DefaultFunctionSignature
import com.google.devsite.components.impl.DefaultFunctionSummary
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.impl.DefaultParameter
import com.google.devsite.components.impl.DefaultParameterType
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.AdditionalModifiers
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.FunctionModifiers
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.OtherParameter
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.TypeConstructor

/** Converts documentable functions into function components. */
internal class FunctionDocumentableConverter(
    private val language: Language,
    private val pathProvider: FilePathProvider
) {
    /** @return the function summary component */
    fun summary(function: DFunction): FunctionSummary? {
        if (!shouldDocument(function)) return null

        return DefaultFunctionSummary(
            FunctionSummary.Params(
                modifiers = function.modifiers(),
                returnType = function.type.toComponent(),
                signature = function.signature(),
                description = DefaultDocumentation(
                    Documentation.Params(
                        tags = function.tags(),
                        summary = true
                    )
                )
            )
        )
    }

    private fun shouldDocument(function: DFunction): Boolean {
        return if (language == Language.KOTLIN) {
            true
        } else {
            // We currently ignore higher order functions
            // TODO(b/165105949): document as FunctionN in synthetic classes
            function.parameters.none { it.type.isLambda() }
        }
    }

    private fun DFunction.signature(): FunctionSignature {
        val parameters = parameters.map { param ->
            val isLambda = param.type.isLambda()

            val receiver = param.type.receiver()
            val primaryType = if (isLambda) {
                // Get the return type of the lambda
                (param.type as TypeConstructor).projections.last().toComponent()
            } else {
                param.type.toComponent()
            }
            val lambdaParams: List<ParameterType> = if (isLambda) {
                // Always ignore the return type of the lambda since that's handled by primaryType.
                val lambdaProjections = (param.type as TypeConstructor).projections.dropLast(1)
                if (receiver == null) {
                    lambdaProjections.map { it.toComponent() }
                } else {
                    // If the receiver is available, we also ignore the first type
                    lambdaProjections.drop(1).map { it.toComponent() }
                }
            } else {
                emptyList()
            }

            DefaultParameter(
                Parameter.Params(
                    isLambda = isLambda,
                    name = param.name!!,
                    receiver = receiver,
                    lambdaParams = lambdaParams,
                    primary = primaryType,
                    // TODO(b/165104993): figure out path to implementing annotations
                    annotations = emptyList(),
                    language = language
                )
            )
        }

        return DefaultFunctionSignature(
            FunctionSignature.Params(
                name = relativeLink(),
                parameters = parameters
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
        is OtherParameter -> null
        is Nullable -> inner.receiver()
        else -> error("Unknown bound: $this")
    }

    /** @return the complete list of modifiers for this function */
    private fun DFunction.modifiers(): List<String> {
        val baseModifiers = modifier.values.map { it.name }
        val extraModifiers = extra.allOfType<AdditionalModifiers>().flatMap { modifiers ->
            modifiers.content.values.single().map { it.name }
        }

        return extraModifiers + baseModifiers
    }

    /** Converts a documentable type to its type component, recursively expanding generics */
    private fun Projection.toComponent(): ParameterType {
        val generics: List<ParameterType> = when (this) {
            is TypeConstructor -> projections.map { it.toComponent() }
            is OtherParameter -> emptyList()
            is Nullable -> listOf(inner.toComponent())
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
        is TypeConstructor -> {
            val packageName = dri.packageName!!
            val name = dri.classNames!!

            DefaultLink(
                Link.Params(
                    name = name,
                    url = pathProvider.forType(packageName, name)
                )
            )
        }
        is OtherParameter -> DefaultLink(
            Link.Params(
                name = name,
                url = ""
            )
        )
        is Nullable -> inner.toLink()
        else -> error("Unknown bound: $this")
    }

    /**
     * Creates a deep link to the function, assuming its detailed documentation will be present on
     * the same page this summary is being rendered to.
     */
    private fun DFunction.relativeLink(): Link {
        val fullyQualifiedProjections = parameters.map { param ->
            param.type.toFullyQualifiedSignature()
        }

        return DefaultLink(
            Link.Params(
                name = name,
                url = "#$name(${fullyQualifiedProjections.joinToString()})"
            )
        )
    }

    /** Determine whether or not a param is a lambda using the kotlin function type. */
    private fun Projection.isLambda(): Boolean = when (this) {
        is TypeConstructor -> dri.packageName == "kotlin" &&
            dri.classNames.orEmpty().startsWith("Function")
        is OtherParameter -> false
        is Nullable -> inner.isLambda()
        else -> error("Unknown bound: $this")
    }

    private fun Projection.toFullyQualifiedSignature(): String = when (this) {
        is TypeConstructor ->
            dri.packageName!! + "." + dri.classNames!!
        is OtherParameter -> name
        is Nullable -> inner.toFullyQualifiedSignature() + "?"
        else -> error("Unknown bound: $this")
    }
}
