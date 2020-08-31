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

import com.google.devsite.components.FunctionDetail
import com.google.devsite.components.FunctionSignature
import com.google.devsite.components.FunctionSummary
import com.google.devsite.components.Link
import com.google.devsite.components.Parameter
import com.google.devsite.components.ParameterType
import com.google.devsite.components.TwoPaneSummaryItem
import com.google.devsite.components.TypeSummary
import com.google.devsite.components.impl.DefaultFunctionDetail
import com.google.devsite.components.impl.DefaultFunctionSignature
import com.google.devsite.components.impl.DefaultFunctionSummary
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.impl.DefaultParameter
import com.google.devsite.components.impl.DefaultParameterType
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.components.impl.DefaultTypeSummary
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.AdditionalModifiers
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.FunctionModifiers
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.OtherParameter
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.Star
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.Variance

/** Converts documentable functions into function components. */
internal class FunctionDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val javadocConverter: DocTagConverter
) {
    /** @return the function summary component */
    fun summary(function: DFunction): TwoPaneSummaryItem {
        return DefaultTwoPaneSummaryItem(
            TwoPaneSummaryItem.Params(
                title = DefaultTypeSummary(
                    TypeSummary.Params(
                        modifiers = function.modifiers(),
                        type = function.type.toComponent()
                    )
                ),
                description = DefaultFunctionSummary(
                    FunctionSummary.Params(
                        signature = function.signature(),
                        description = javadocConverter.summaryDescription(function)
                    )
                )
            )
        )
    }

    /** @return the function detail component */
    fun detail(function: DFunction): FunctionDetail {
        val returnType = function.type.toComponent()
        return DefaultFunctionDetail(
            FunctionDetail.Params(
                displayLanguage = displayLanguage,
                name = function.name,
                anchors = generateCompatAnchors(function),
                modifiers = function.modifiers(),
                returnType = returnType,
                signature = function.signature(),
                metadata = javadocConverter.metadata(
                    doc = function,
                    returnType = returnType,
                    paramNames = listOf("receiver") + function.parameters.map { it.name!! }
                )
            )
        )
    }

    private fun DFunction.signature(): FunctionSignature {
        val receiver = receiver?.let(::componentForParameter)
        val parameters = parameters.map(::componentForParameter)

        return DefaultFunctionSignature(
            FunctionSignature.Params(
                name = pathProvider.linkForReference(dri),
                receiver = when (displayLanguage) {
                    Language.JAVA -> null
                    Language.KOTLIN -> receiver
                },
                parameters = when (displayLanguage) {
                    Language.JAVA -> listOfNotNull(receiver) + parameters
                    Language.KOTLIN -> parameters
                }
            )
        )
    }

    private fun componentForParameter(param: DParameter): Parameter = when (displayLanguage) {
        Language.JAVA -> componentForJavaParameter(param)
        Language.KOTLIN -> componentForKotlinParameter(param)
    }

    private fun componentForJavaParameter(param: DParameter): Parameter {
        return DefaultParameter(
            Parameter.Params(
                isLambda = false,
                name = param.name ?: "receiver",
                primary = param.type.toComponent(),
                // TODO(b/165104993): figure out path to implementing annotations
                annotations = emptyList(),
                displayLanguage = Language.JAVA
            )
        )
    }

    private fun componentForKotlinParameter(param: DParameter): Parameter {
        val isLambda = param.type.isLambda()

        val receiver = param.type.receiver()
        val primaryType = if (isLambda) {
            // Get the return type of the lambda
            (param.type as TypeConstructor).projections.last().toComponent()
        } else {
            param.type.toComponent()
        }
        val lambdaModifiers: List<String> = if (param.type.isLambda(suspendOnly = true)) {
            listOf("suspend")
        } else {
            emptyList()
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

        return DefaultParameter(
            Parameter.Params(
                isLambda = isLambda,
                name = param.name.orEmpty(),
                receiver = receiver,
                lambdaModifiers = lambdaModifiers,
                lambdaParams = lambdaParams,
                primary = primaryType,
                // TODO(b/165104993): figure out path to implementing annotations
                annotations = emptyList(),
                displayLanguage = Language.KOTLIN
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
        is OtherParameter, Star -> null
        is Nullable -> inner.receiver()
        is Variance -> inner.receiver()
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
            is OtherParameter, Star -> emptyList()
            is Nullable -> listOf(inner.toComponent())
            // TODO(b/166530498): support variance
            is Variance -> listOf(inner.toComponent())
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
        Star -> DefaultLink(
            Link.Params(
                name = when (displayLanguage) {
                    Language.JAVA -> "?"
                    Language.KOTLIN -> "*"
                },
                url = ""
            )
        )
        is Nullable -> inner.toLink()
        // TODO(b/166530498): support variance
        is Variance -> inner.toLink()
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
        is OtherParameter -> false
        is Nullable -> inner.isLambda()
        is Variance -> inner.isLambda()
        is Star -> false
        else -> error("Unknown bound: $this")
    }

    /**
     * Creates method anchors compatible with several different iterations of javadoc.
     *
     * The different types are:
     * - `foo(int,int)`
     * - `foo(int, int)`
     * - `foo-int-int-`
     */
    private fun generateCompatAnchors(function: DFunction): Set<String> {
        val callable = function.dri.callable!!
        return setOf(
            callable.anchor(),
            callable.anchor(separator = ", "),
            callable.anchor("-", "-", "-")
        )
    }

    private fun Projection.toFullyQualifiedSignature(): String = when (this) {
        is TypeConstructor -> dri.packageName!! + "." + dri.classNames!!
        is OtherParameter -> name
        is Nullable -> inner.toFullyQualifiedSignature()
        is Variance -> inner.toFullyQualifiedSignature()
        is Star -> ""
        else -> error("Unknown bound: $this")
    }
}
