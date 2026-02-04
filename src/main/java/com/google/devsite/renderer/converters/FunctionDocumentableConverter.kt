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

import com.google.devsite.KmpTypeSummaryItem
import com.google.devsite.TypeSummaryItem
import com.google.devsite.components.impl.DefaultFunctionSignature
import com.google.devsite.components.impl.DefaultKmpSymbolDetail
import com.google.devsite.components.impl.DefaultKmpTableRowSummaryItem
import com.google.devsite.components.impl.DefaultParameterComponent
import com.google.devsite.components.impl.DefaultPlatformComponent
import com.google.devsite.components.impl.DefaultSymbolDetail
import com.google.devsite.components.impl.DefaultSymbolSummary
import com.google.devsite.components.impl.DefaultTableRowSummaryItem
import com.google.devsite.components.impl.DefaultTypeProjectionComponent
import com.google.devsite.components.impl.DefaultTypeSummary
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.KmpSymbolDetail
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.KmpTableRowSummaryItem
import com.google.devsite.components.table.TableRowSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import java.util.Locale
import org.jetbrains.dokka.model.DFunction

/** Converts documentable functions into function components. */
internal class FunctionDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val javadocConverter: DocTagConverter,
    private val paramConverter: ParameterDocumentableConverter,
    private val annotationConverter: AnnotationDocumentableConverter,
    private val metadataConverter: MetadataConverter,
) {

    /** @return the function summary component */
    fun summary(function: DFunction, hints: ModifierHints): TypeSummaryItem<FunctionSignature>? {
        val jvmSourceSet = function.getAsJavaSourceSet() ?: return null
        val (typeAnnotations, nonTypeAnnotations) =
            function.annotations(jvmSourceSet).partition { it.belongsOnReturnType() }
        return DefaultTableRowSummaryItem(
            TableRowSummaryItem.Params(
                title =
                    DefaultTypeSummary(
                        TypeSummary.Params(
                            type =
                                paramConverter.componentForProjection(
                                    projection = function.type,
                                    // Propagate ALL annotations _for display in the summary_,
                                    // b/197321617
                                    propagatedAnnotations = typeAnnotations,
                                    isReturnType = true,
                                    isJavaSource = function.isFromJava(),
                                    sourceSet = jvmSourceSet,
                                ),
                            modifiers = function.modifiers(jvmSourceSet).modifiersFor(hints),
                        )
                    ),
                description =
                    DefaultSymbolSummary(
                        SymbolSummary.Params(
                            signature = function.signature(isSummary = true),
                            description =
                                javadocConverter.summaryDescription(
                                    function,
                                    nonTypeAnnotations.deprecationAnnotation(),
                                ),
                            annotationComponents =
                                annotationConverter.annotationComponents(
                                    annotations = nonTypeAnnotations,
                                    // Propagates to return type instead
                                    nullability = Nullability.DONT_CARE,
                                ),
                        )
                    ),
            )
        )
    }

    /** @return the function summary component */
    fun summaryKmp(
        function: DFunction,
        hints: ModifierHints,
    ): KmpTypeSummaryItem<FunctionSignature> {
        // TODO(KMP member signatures b/254493209)
        val (typeAnnotations, nonTypeAnnotations) =
            function.annotations(function.getExpectOrCommonSourceSet()).partition {
                it.belongsOnReturnType()
            }
        return DefaultKmpTableRowSummaryItem(
            KmpTableRowSummaryItem.Params(
                title =
                    DefaultTypeSummary(
                        TypeSummary.Params(
                            type =
                                paramConverter.componentForProjection(
                                    projection = function.type,
                                    // Propagate ALL annotations _for display in the summary_,
                                    // b/197321617
                                    propagatedAnnotations = typeAnnotations,
                                    isReturnType = true,
                                    isJavaSource = function.isFromJava(),
                                    sourceSet = function.getExpectOrCommonSourceSet(),
                                ),
                            // TODO(KMP, b/254493209)
                            modifiers =
                                function
                                    .modifiers(function.getExpectOrCommonSourceSet())
                                    .modifiersFor(hints),
                        )
                    ),
                description =
                    DefaultSymbolSummary(
                        SymbolSummary.Params(
                            signature = function.signature(isSummary = true),
                            description =
                                javadocConverter.summaryDescription(
                                    function,
                                    nonTypeAnnotations.deprecationAnnotation(),
                                ),
                            annotationComponents =
                                annotationConverter.annotationComponents(
                                    annotations = nonTypeAnnotations,
                                    // Propagates to return type instead
                                    nullability = Nullability.DONT_CARE,
                                ),
                        )
                    ),
                platforms = DefaultPlatformComponent(function.sourceSets),
            )
        )
    }

    /** @return the constructor summary component */
    fun summaryForConstructor(
        function: DFunction
    ): TableRowSummaryItem<Nothing?, SymbolSummary<FunctionSignature>>? {
        val jvmSourceSet = function.getAsJavaSourceSet() ?: return null
        return DefaultTableRowSummaryItem(
            TableRowSummaryItem.Params(
                title = null,
                DefaultSymbolSummary(
                    SymbolSummary.Params(
                        signature = function.signature(isSummary = true),
                        description = javadocConverter.summaryDescription(function),
                        annotationComponents =
                            annotationConverter.annotationComponents(
                                annotations = function.annotations(jvmSourceSet),
                                // Propagates to return type instead
                                nullability = Nullability.DONT_CARE,
                            ),
                    )
                ),
            )
        )
    }

    /** @return the constructor summary component */
    fun summaryForKmpConstructor(
        function: DFunction
    ): KmpTableRowSummaryItem<Nothing?, SymbolSummary<FunctionSignature>> =
        DefaultKmpTableRowSummaryItem(
            KmpTableRowSummaryItem.Params(
                title = null,
                DefaultSymbolSummary(
                    SymbolSummary.Params(
                        signature = function.signature(isSummary = true),
                        description = javadocConverter.summaryDescription(function),
                        // TODO(KMP member signatures b/254493209)
                        annotationComponents =
                            annotationConverter.annotationComponents(
                                annotations =
                                    function.annotations(function.getExpectOrCommonSourceSet()),
                                // Propagates to return type instead
                                nullability = Nullability.DONT_CARE,
                            ),
                    )
                ),
                platforms = DefaultPlatformComponent(function.sourceSets),
            )
        )

    /** @return the function detail component */
    fun detail(function: DFunction, hints: ModifierHints) =
        detail(function, hints, SymbolDetail.SymbolKind.FUNCTION)

    /** @return the function detail component */
    fun detailKmp(function: DFunction, hints: ModifierHints) =
        detailKmp(function, hints, SymbolDetail.SymbolKind.FUNCTION)

    /** @return the constructor detail component */
    fun detailForConstructor(function: DFunction, hints: ModifierHints) =
        detail(function, hints, SymbolDetail.SymbolKind.CONSTRUCTOR)

    /** @return the constructor detail component */
    fun detailForKmpConstructor(function: DFunction, hints: ModifierHints) =
        detailKmp(function, hints, SymbolDetail.SymbolKind.CONSTRUCTOR)

    /** @return the symbol detail component */
    private fun detail(
        function: DFunction,
        hints: ModifierHints,
        kind: SymbolDetail.SymbolKind,
    ): SymbolDetail<FunctionSignature>? {
        val jvmSourceSet = function.getAsJavaSourceSet() ?: return null
        val (typeAnnotations, signatureAnnotations) =
            function.annotations(jvmSourceSet).partition { it.belongsOnReturnType() }
        val returnType =
            paramConverter.componentForProjection(
                projection = function.type,
                isJavaSource = function.isFromJava(),
                propagatedAnnotations = typeAnnotations,
                isReturnType = true,
                propagatedNullability =
                    if (kind == SymbolDetail.SymbolKind.CONSTRUCTOR || function.isConstructor) {
                        Nullability.DONT_CARE
                    } else {
                        null
                    },
                sourceSet = jvmSourceSet,
            )

        // So far I've only seen this in unit tests where we use the wrong entry point into
        // FunctionDocumentableConverter, but it's possible it could happen in other ways.
        if (function.isConstructor != (kind == SymbolDetail.SymbolKind.CONSTRUCTOR)) {
            throw RuntimeException(
                "Constructor ${function.dri} is not being parsed correctly! File a bug on dackka!"
            )
        }

        return DefaultSymbolDetail(
            SymbolDetail.Params(
                name = function.name,
                returnType = returnType,
                symbolKind = kind,
                signature = function.signature(isSummary = false),
                anchors = generateCompatAnchors(function),
                metadata =
                    javadocConverter.metadata(
                        documentable = function,
                        returnType = returnType,
                        paramNames = listOf("receiver") + function.parameters.map { it.name!! },
                        deprecationAnnotation = signatureAnnotations.deprecationAnnotation(),
                    ),
                displayLanguage = displayLanguage,
                modifiers = function.modifiers(jvmSourceSet).modifiersFor(hints),
                extFunctionClass = function.receiver?.let { nameForSyntheticClass(function) },
                annotationComponents =
                    annotationConverter.annotationComponents(
                        annotations = signatureAnnotations,
                        // Nullability is on the return type instead
                        nullability = Nullability.DONT_CARE,
                    ),
                metadataComponent = metadataConverter.getMetadataForFunction(function),
            )
        )
    }

    /** @return the symbol detail component */
    private fun detailKmp(
        function: DFunction,
        hints: ModifierHints,
        kind: SymbolDetail.SymbolKind,
    ): KmpSymbolDetail<FunctionSignature> {
        // TODO(KMP member signatures b/254493209)
        val (typeAnnotations, signatureAnnotations) =
            function.annotations(function.getExpectOrCommonSourceSet()).partition {
                it.belongsOnReturnType()
            }
        val returnType =
            paramConverter.componentForProjection(
                projection = function.type,
                isJavaSource = function.isFromJava(),
                propagatedAnnotations = typeAnnotations,
                isReturnType = true,
                propagatedNullability =
                    if (kind == SymbolDetail.SymbolKind.CONSTRUCTOR || function.isConstructor) {
                        Nullability.DONT_CARE
                    } else {
                        null
                    },
                sourceSet = function.getExpectOrCommonSourceSet(),
            )

        // So far I've only seen this in unit tests where we use the wrong entry point into
        // FunctionDocumentableConverter, but it's possible it could happen in other ways.
        if (function.isConstructor != (kind == SymbolDetail.SymbolKind.CONSTRUCTOR)) {
            throw RuntimeException(
                "Constructor ${function.dri} is not being parsed correctly! File a bug on dackka!"
            )
        }

        return DefaultKmpSymbolDetail(
            KmpSymbolDetail.Params(
                name = function.name,
                returnType = returnType,
                symbolKind = kind,
                signature = function.signature(isSummary = false),
                anchors = generateCompatAnchors(function),
                metadata =
                    javadocConverter.metadata(
                        documentable = function,
                        returnType = returnType,
                        paramNames = listOf("receiver") + function.parameters.map { it.name!! },
                        deprecationAnnotation = signatureAnnotations.deprecationAnnotation(),
                    ),
                displayLanguage = displayLanguage,
                // TODO(KMP, b/254493209)
                modifiers =
                    function.modifiers(function.getExpectOrCommonSourceSet()).modifiersFor(hints),
                extFunctionClass = function.receiver?.let { nameForSyntheticClass(function) },
                annotationComponents =
                    annotationConverter.annotationComponents(
                        annotations = signatureAnnotations,
                        // Nullability is on the return type instead
                        nullability = Nullability.DONT_CARE,
                    ),
                platforms = DefaultPlatformComponent(function.sourceSets),
                metadataComponent = metadataConverter.getMetadataForFunction(function),
            )
        )
    }

    internal fun DFunction.signature(isSummary: Boolean): FunctionSignature {
        val receiver =
            receiver?.let {
                paramConverter.componentForParameter(
                    param = it,
                    isSummary = isSummary,
                    isFromJava = isFromJava(),
                    parent = this,
                )
            }
        val parameters =
            parameters.map {
                paramConverter.componentForParameter(
                    param = it,
                    isSummary = isSummary,
                    isFromJava = isFromJava(),
                    parent = this,
                )
            }
        val typeParameters =
            this.generics.map {
                paramConverter.componentForTypeParameter(param = it, isFromJava = isFromJava())
            }

        return DefaultFunctionSignature(
            FunctionSignature.Params(
                name =
                    pathProvider.linkForReference(
                        dri.possiblyConvertMappedType(displayLanguage),
                        name = this.name,
                    ),
                receiver =
                    when (displayLanguage) {
                        Language.JAVA -> receiver?.let { extFunctionClass() }
                        Language.KOTLIN -> receiver
                    },
                typeParameters = typeParameters,
                parameters =
                    when (displayLanguage) {
                        Language.JAVA -> listOfNotNull(receiver) + parameters
                        Language.KOTLIN -> parameters
                    },
                // TODO(handle sourceSet-varying deprecations b/262711247)
                isDeprecated = annotations(getExpectOrCommonSourceSet()).isDeprecated(),
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
            callable.name.lowercase(Locale.getDefault()),
        )
    }

    /**
     * Creates the parameter representing the fake containing class for Kotlin top-level functions
     * represented in a synthetic class, e.g. "SyntheticKt" in SyntheticKt.topLevelFunction(args)
     */
    private fun DFunction.extFunctionClass(): ParameterComponent {
        return DefaultParameterComponent(
            ParameterComponent.Params(
                name = "",
                type =
                    DefaultTypeProjectionComponent(
                        TypeProjectionComponent.Params(
                            type = pathProvider.linkForReference(driForSyntheticClass()),
                            nullability = Nullability.DONT_CARE,
                            displayLanguage =
                                displayLanguage, // Fake synthetic classes can't be null
                        )
                    ),
                displayLanguage = displayLanguage,
            )
        )
    }
}
