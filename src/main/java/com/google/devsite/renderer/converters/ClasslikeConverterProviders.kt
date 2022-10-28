/*
 * Copyright 2022 The Android Open Source Project
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

import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DProperty

internal class NonKmpClasslikeConverter(
    displayLanguage: Language,
    classlike: DClasslike,
    pathProvider: FilePathProvider,
    docsHolder: DocumentablesHolder,
    classExtensionFunctions: List<DFunction> = emptyList(),
    classExtensionProperties: List<DProperty> = emptyList()
) : ClasslikeDocumentableConverter(
    displayLanguage,
    classlike,
    pathProvider,
    docsHolder,
    classExtensionFunctions,
    classExtensionProperties
) {
    private val functionConverter =
        FunctionDocumentableConverter(displayLanguage, pathProvider, javadocConverter)
    private val propertyConverter =
        PropertyDocumentableConverter(displayLanguage, pathProvider, javadocConverter)
    override val functionToSummaryConverter = functionConverter::summary
    override val functionToDetailConverter = functionConverter::detail
    override val propertyToSummaryConverter = propertyConverter::summary
    override val propertyToDetailConverter = propertyConverter::detail
    override val constructorToSummaryConverter = functionConverter::summaryForConstructor
    override val constructorToDetailConverter = functionConverter::detailForConstructor
}

internal abstract class KmpClasslikeConverter(
    displayLanguage: Language,
    classlike: DClasslike,
    pathProvider: FilePathProvider,
    docsHolder: DocumentablesHolder,
    classExtensionFunctions: List<DFunction> = emptyList(),
    classExtensionProperties: List<DProperty> = emptyList()
) : ClasslikeDocumentableConverter(
    displayLanguage,
    classlike,
    pathProvider,
    docsHolder,
    classExtensionFunctions,
    classExtensionProperties
)
