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

import com.google.devsite.components.Raw
import com.google.devsite.components.impl.DefaultPropertySignature
import com.google.devsite.components.impl.DefaultRaw
import com.google.devsite.components.impl.DefaultSymbolDetail
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.GenericTypeConstructor

/** Converts documentable DEnumEntrys into EnumValue components. */
internal class EnumValueDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val javadocConverter: DocTagConverter
) {
    private val paramConverter = ParameterDocumentableConverter(displayLanguage, pathProvider)

    /** @return the enum value summary component */
    fun summary(enumValue: DEnumEntry): TwoPaneSummaryItem {
        val annotations = enumValue.annotations()
        return DefaultTwoPaneSummaryItem(
            TwoPaneSummaryItem.Params(
                title = DefaultRaw(Raw.Params(enumValue.name)),
                description = javadocConverter.summaryDescription(enumValue, annotations)
            )
        )
    }

    /** @return the enum detail component */
    fun detail(dEnum: DEnum, enumValue: DEnumEntry, hints: ModifierHints): SymbolDetail {
        val annotations = enumValue.annotations()
        val projection = paramConverter.componentForProjection(
            GenericTypeConstructor(dEnum.dri, emptyList()),
            isJavaSource = enumValue.isFromJava(),
            // While technically an ENUM_VALUE is a member of ENUM_TYPE? because you can always
            // define an enum value which is `null`, this isn't useful information
            propagatedNullability = Nullability.DONT_CARE
        )
        return DefaultSymbolDetail(
            SymbolDetail.Params(
                displayLanguage = displayLanguage,
                name = enumValue.name,
                anchors = enumValue.generateAnchors(),
                annotationComponents = annotations.annotationComponents(
                    pathProvider = pathProvider,
                    displayLanguage = displayLanguage,
                    nullability = Nullability.DONT_CARE // See above
                ),
                modifiers = enumValue.getExtraModifiers().modifiersFor(hints),
                returnType = projection,
                symbolKind = SymbolDetail.SymbolKind.READ_ONLY_PROPERTY,
                signature = enumValue.signature(),
                metadata = javadocConverter.metadata(
                    documentable = enumValue,
                    returnType = projection,
                    paramNames = listOf(),
                    annotations = annotations
                )
            )
        )
    }

    internal fun DEnumEntry.signature(): PropertySignature {
        return DefaultPropertySignature(
            PropertySignature.Params(
                // TODO(b/168136770): figure out path for default anchors
                name = pathProvider.linkForReference(dri),
                receiver = null
            )
        )
    }

    /** Returns anchors for this enum value. */
    private fun DEnumEntry.generateAnchors(): LinkedHashSet<String> {
        return linkedSetOf(
            name
        )
    }
}
