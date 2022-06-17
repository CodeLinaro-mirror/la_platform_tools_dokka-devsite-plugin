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

import com.google.devsite.capitalize
import com.google.devsite.components.impl.DefaultPropertySignature
import com.google.devsite.components.impl.DefaultSymbolDetail
import com.google.devsite.components.impl.DefaultSymbolSummary
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.components.impl.DefaultTypeSummary
import com.google.devsite.components.symbols.AnnotationComponent
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DefaultValue

/** Converts documentable properties into property components. */
internal class PropertyDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val javadocConverter: DocTagConverter
) {
    private val paramConverter = ParameterDocumentableConverter(displayLanguage, pathProvider)

    /** @return the property summary component */
    fun summary(property: DProperty, hints: ModifierHints):
        TwoPaneSummaryItem<TypeSummary, SymbolSummary> {
        val annotations = property.annotations().filter { !it.belongsOnReturnType() }
        return DefaultTwoPaneSummaryItem(
            TwoPaneSummaryItem.Params(
                title = DefaultTypeSummary(
                    TypeSummary.Params(
                        modifiers = property.modifiers().modifiersFor(hints),
                        type = paramConverter.componentForProjection(
                            property.type,
                            property.isFromJava(),
                            property.annotations().filter { it.belongsOnReturnType() }
                        )
                    )
                ),
                description = DefaultSymbolSummary(
                    SymbolSummary.Params(
                        signature = property.signature(
                            isSummary = true,
                            annotationComponents = annotations.annotationComponents(
                                pathProvider = pathProvider,
                                displayLanguage = displayLanguage,
                                nullability = Nullability.DONT_CARE // on return type instead
                            )
                        ),
                        description = javadocConverter.summaryDescription(property, annotations)
                    )
                )
            )
        )
    }

    /** @return the property detail component */
    fun detail(property: DProperty, hints: ModifierHints): SymbolDetail {
        val (propagatedAnnotations, retainedAnnotations) = property.annotations()
            .partition { it.belongsOnReturnType() }
        val returnType = paramConverter.componentForProjection(
            property.type,
            property.isFromJava(),
            propagatedAnnotations,
            propagatedNullability = property.type
                .getNullability(displayLanguage, property.isFromJava(), propagatedAnnotations)
        )
        return DefaultSymbolDetail(
            SymbolDetail.Params(
                displayLanguage = displayLanguage,
                name = property.name,
                anchors = property.generateAnchors(),
                annotationComponents = retainedAnnotations.annotationComponents(
                    pathProvider = pathProvider,
                    displayLanguage = displayLanguage,
                    nullability = Nullability.DONT_CARE // Propagates to return type instead
                ),
                modifiers = property.modifiers().modifiersFor(hints),
                returnType = returnType,
                symbolKind = SymbolDetail.SymbolKind.PROPERTY.takeIf { property.setter != null }
                    ?: SymbolDetail.SymbolKind.READ_ONLY_PROPERTY,
                signature = property.signature(isSummary = false),
                metadata = javadocConverter.metadata(
                    documentable = property,
                    returnType = returnType,
                    paramNames = listOf("receiver"),
                    annotations = retainedAnnotations
                )
            )
        )
    }

    private fun DProperty.signature(
        isSummary: Boolean,
        annotationComponents: List<AnnotationComponent> = emptyList()
    ): PropertySignature {
        val receiver = receiver?.let {
            paramConverter.componentForParameter(
                param = it,
                isSummary = isSummary,
                isFromJava = isFromJava()
            )
        }
        val constantValue = if (isConstant(modifiers())) {
            // the value of a constant is stored as a DefaultValue, pick it out if it exists
            extra.allOfType<DefaultValue>().singleOrNull()?.value?.getValue()
        } else {
            null
        }
        return DefaultPropertySignature(
            PropertySignature.Params(
                // TODO(b/168136770): figure out path for default anchors
                name = pathProvider.linkForReference(dri),
                receiver = when (displayLanguage) {
                    Language.JAVA -> null
                    Language.KOTLIN -> receiver
                },
                annotationComponents = annotationComponents,
                constantValue = constantValue
            )
        )
    }

    /** Returns anchors for this property, including for synthetic getters and setters. */
    private fun DProperty.generateAnchors(): LinkedHashSet<String> {
        val callable = dri.callable!!

        val getterCallable = callable.copy(name = "get${callable.name.capitalize()}")
        val setterCallable = callable.copy(name = "set${callable.name.capitalize()}")

        return linkedSetOf(
            // TODO(b/168136770): figure out path for default anchors
            callable.anchor(),
            getterCallable.anchor(),
            setterCallable.anchor(),
            getterCallable.anchor(open = "-", close = "-"),
            setterCallable.anchor(open = "-", close = "-")
        )
    }
}
