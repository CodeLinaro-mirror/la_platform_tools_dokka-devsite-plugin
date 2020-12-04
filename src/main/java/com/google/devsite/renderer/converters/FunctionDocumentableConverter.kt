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
import com.google.devsite.components.impl.DefaultSingleColumnSummaryItem
import com.google.devsite.components.impl.DefaultSymbolDetail
import com.google.devsite.components.impl.DefaultSymbolSummary
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.components.impl.DefaultTypeSummary
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.SingleColumnSummaryItem
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.DFunction

/** Converts documentable functions into function components. */
internal class FunctionDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val javadocConverter: DocTagConverter
) {
    private val paramConverter = ParameterDocumentableConverter(displayLanguage, pathProvider)

    /** @return the function summary component */
    fun summary(function: DFunction, hints: ModifierHints): TwoPaneSummaryItem {
        val annotations = function.annotations()
        return DefaultTwoPaneSummaryItem(
            TwoPaneSummaryItem.Params(
                title = DefaultTypeSummary(
                    TypeSummary.Params(
                        modifiers = function.modifiers().modifiersFor(hints),
                        type = paramConverter.componentForProjection(
                            function.type,
                            annotations,
                            isReturnType = true
                        )
                    )
                ),
                description = DefaultSymbolSummary(
                    SymbolSummary.Params(
                        signature = function.signature(isSummary = true),
                        description = javadocConverter.summaryDescription(function, annotations)
                    )
                )
            )
        )
    }

    /** @return the constructor summary component */
    fun summaryForConstructor(function: DFunction): SingleColumnSummaryItem {
        return DefaultSingleColumnSummaryItem(
            SingleColumnSummaryItem.Params(
                DefaultSymbolSummary(
                    SymbolSummary.Params(
                        signature = function.signature(isSummary = true),
                        description = javadocConverter.summaryDescription(function)
                    )
                )
            )
        )
    }

    /** @return the function detail component */
    fun detail(function: DFunction, hints: ModifierHints): SymbolDetail {
        return detail(function, hints, SymbolDetail.SymbolType.FUNCTION)
    }

    /** @return the constructor detail component */
    fun detailForConstructor(function: DFunction, hints: ModifierHints): SymbolDetail {
        return detail(function, hints, SymbolDetail.SymbolType.CONSTRUCTOR)
    }

    /** @return the symbol detail component */
    private fun detail(
        function: DFunction,
        hints: ModifierHints,
        type: SymbolDetail.SymbolType
    ): SymbolDetail {
        val annotations = function.annotations()
        val returnType =
            paramConverter.componentForProjection(function.type, annotations, isReturnType = true)

        return DefaultSymbolDetail(
            SymbolDetail.Params(
                displayLanguage = displayLanguage,
                name = function.name,
                anchors = generateCompatAnchors(function),
                annotations = annotations.annotationComponents(
                    pathProvider,
                    displayLanguage,
                    function.type.isNullable()
                ),
                modifiers = function.modifiers().modifiersFor(hints),
                returnType = returnType,
                symbolType = type,
                signature = function.signature(isSummary = false),
                metadata = javadocConverter.metadata(
                    documentable = function,
                    returnType = returnType,
                    paramNames = listOf("receiver") + function.parameters.map { it.name!! },
                    annotations = annotations
                )
            )
        )
    }

    private fun DFunction.signature(isSummary: Boolean): FunctionSignature {
        val receiver = receiver?.let { paramConverter.componentForParameter(it, isSummary) }
        val parameters = parameters.map { paramConverter.componentForParameter(it, isSummary) }
        val typeParameters = this.generics.map {
            paramConverter.componentForTypeParameter(it, isSummary) }

        return DefaultFunctionSignature(
            FunctionSignature.Params(
                name = pathProvider.linkForReference(dri),
                receiver = when (displayLanguage) {
                    Language.JAVA -> null
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
     * - `foo(int,int)`
     * - `foo(int, int)`
     * - `foo-int-int-`
     */
    private fun generateCompatAnchors(function: DFunction): LinkedHashSet<String> {
        val callable = function.dri.callable!!
        return linkedSetOf(
            callable.anchor(),
            callable.anchor(separator = ", "),
            callable.anchor("-", "-", "-")
        )
    }
}
