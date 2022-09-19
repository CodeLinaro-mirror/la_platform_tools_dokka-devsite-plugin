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

import com.google.devsite.components.impl.DefaultFunctionSignature
import com.google.devsite.components.impl.DefaultParameterComponent
import com.google.devsite.components.impl.DefaultSingleColumnSummaryItem
import com.google.devsite.components.impl.DefaultSymbolDetail
import com.google.devsite.components.impl.DefaultSymbolSummary
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.components.impl.DefaultTypeProjectionComponent
import com.google.devsite.components.impl.DefaultTypeSummary
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.SingleColumnSummaryItem
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.DFunction
import java.util.Locale

/** Converts documentable functions into function components. */
internal class FunctionDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val javadocConverter: DocTagConverter
) {
    private val paramConverter = ParameterDocumentableConverter(displayLanguage, pathProvider)

    /** @return the function summary component */
    fun summary(function: DFunction, hints: ModifierHints):
        TwoPaneSummaryItem<TypeSummary, SymbolSummary> {
        val (typeAnnotations, nonTypeAnnotations) =
            function.annotations().partition { it.belongsOnReturnType() }
        return DefaultTwoPaneSummaryItem(
            TwoPaneSummaryItem.Params(
                title = DefaultTypeSummary(
                    TypeSummary.Params(
                        modifiers = function.modifiers().modifiersFor(hints),
                        type = paramConverter.componentForProjection(
                            projection = function.type,
                            // Propagate ALL annotations _for display in the summary_, b/197321617
                            propagatedAnnotations = typeAnnotations,
                            isReturnType = true,
                            isJavaSource = function.isFromJava()
                        )
                    )
                ),
                description = DefaultSymbolSummary(
                    SymbolSummary.Params(
                        signature = function.signature(isSummary = true),
                        description = javadocConverter.summaryDescription(
                            function,
                            nonTypeAnnotations
                        ),
                        annotationComponents = nonTypeAnnotations.annotationComponents(
                            pathProvider = pathProvider,
                            displayLanguage = displayLanguage,
                            nullability = Nullability.DONT_CARE // Propagates to return type instead
                        )
                    )
                )
            )
        )
    }

    /** @return the constructor summary component */
    fun summaryForConstructor(function: DFunction): SingleColumnSummaryItem<SymbolSummary> {
        return DefaultSingleColumnSummaryItem(
            SingleColumnSummaryItem.Params(
                DefaultSymbolSummary(
                    SymbolSummary.Params(
                        signature = function.signature(isSummary = true),
                        description = javadocConverter.summaryDescription(function),
                        annotationComponents = function.annotations().annotationComponents(
                            pathProvider = pathProvider,
                            displayLanguage = displayLanguage,
                            nullability = Nullability.DONT_CARE // Propagates to return type instead
                        )
                    )
                )
            )
        )
    }

    /** @return the function detail component */
    fun detail(function: DFunction, hints: ModifierHints): SymbolDetail {
        return detail(function, hints, SymbolDetail.SymbolKind.FUNCTION)
    }

    /** @return the constructor detail component */
    fun detailForConstructor(function: DFunction, hints: ModifierHints): SymbolDetail {
        return detail(function, hints, SymbolDetail.SymbolKind.CONSTRUCTOR)
    }

    /** @return the symbol detail component */
    private fun detail(
        function: DFunction,
        hints: ModifierHints,
        kind: SymbolDetail.SymbolKind
    ): SymbolDetail {
        val (typeAnnotations, signatureAnnotations) =
            function.annotations().partition { it.belongsOnReturnType() }
        val returnType = paramConverter.componentForProjection(
            projection = function.type,
            isJavaSource = function.isFromJava(),
            propagatedAnnotations = typeAnnotations,
            isReturnType = true,
            propagatedNullability =
            if (kind == SymbolDetail.SymbolKind.CONSTRUCTOR || function.isConstructor)
                Nullability.DONT_CARE else null
        )

        // So far I've only seen this in unit tests where we use the wrong entry point into
        // FunctionDocumentableConverter, but it's possible it could happen in other ways.
        if (function.isConstructor != (kind == SymbolDetail.SymbolKind.CONSTRUCTOR)) {
            println("WARNING: constructor ${function.dri} is not being parsed correctly")
        }

        return DefaultSymbolDetail(
            SymbolDetail.Params(
                displayLanguage = displayLanguage,
                name = function.name,
                anchors = generateCompatAnchors(function),
                annotationComponents = signatureAnnotations.annotationComponents(
                    pathProvider = pathProvider,
                    displayLanguage = displayLanguage,
                    nullability = Nullability.DONT_CARE // Nullability is on the return type instead
                ),
                modifiers = function.modifiers().modifiersFor(hints),
                returnType = returnType,
                symbolKind = kind,
                signature = function.signature(isSummary = false),
                metadata = javadocConverter.metadata(
                    documentable = function,
                    returnType = returnType,
                    paramNames = listOf("receiver") + function.parameters.map { it.name!! },
                    annotations = signatureAnnotations
                ),
                extFunctionClass = function.receiver?.let { nameForSyntheticClass(function) }
            )
        )
    }

    internal fun DFunction.signature(isSummary: Boolean): FunctionSignature {
        val receiver = receiver?.let {
            paramConverter.componentForParameter(
                param = it,
                isSummary = isSummary,
                isFromJava = isFromJava(),
                parent = this
            )
        }
        val parameters = parameters.map {
            paramConverter.componentForParameter(
                param = it,
                isSummary = isSummary,
                isFromJava = isFromJava(),
                parent = this
            )
        }
        val typeParameters = this.generics.map {
            paramConverter.componentForTypeParameter(param = it, isFromJava = isFromJava())
        }

        return DefaultFunctionSignature(
            FunctionSignature.Params(
                name = pathProvider.linkForReference(
                    dri.possiblyConvertMappedType(displayLanguage),
                    name = this.name
                ),
                receiver = when (displayLanguage) {
                    Language.JAVA -> receiver?.let { extFunctionClass() }
                    Language.KOTLIN -> receiver
                },
                typeParameters = typeParameters,
                parameters = when (displayLanguage) {
                    Language.JAVA -> listOfNotNull(receiver) + parameters
                    Language.KOTLIN -> parameters
                },
                isDeprecated = annotations().isDeprecated()
            )
        )
    }

    /**
     * Creates method anchors compatible with several different iterations of javadoc in order of
     * preference.
     *
     * The different types are:
     * - `(caller).fooBar(int,int)`
     * - `(caller).fooBar(int, int)`
     * - `-caller-.fooBar-int-int-`
     * - `foobar`
     */
    private fun generateCompatAnchors(function: DFunction): LinkedHashSet<String> {
        val callable = function.dri.callable!!
        return linkedSetOf(
            callable.anchor(),
            callable.anchor(separator = ", "),
            callable.anchor("-", "-", "-"),
            callable.name.lowercase(Locale.getDefault())
        )
    }

    /**
     * Creates the parameter representing the fake containing class for Kotlin top-level functions
     * represented in a synthetic class, e.g. "SyntheticKt" in SyntheticKt.topLevelFunction(args)
     */
    private fun DFunction.extFunctionClass(): ParameterComponent {
        return DefaultParameterComponent(
            ParameterComponent.Params(
                displayLanguage = displayLanguage,
                name = "",
                type = DefaultTypeProjectionComponent(
                    TypeProjectionComponent.Params(
                        type = pathProvider.linkForReference(driForSyntheticClass()),
                        displayLanguage = displayLanguage,
                        nullability = Nullability.DONT_CARE // Fake synthetic classes can't be null
                    )
                )
            )
        )
    }
}
