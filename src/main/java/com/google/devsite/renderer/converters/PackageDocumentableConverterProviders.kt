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

import com.google.devsite.components.impl.DefaultDevsitePlatformSelector
import com.google.devsite.components.symbols.Platform
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.DPackage

internal class NonKmpPackageConverter(
    displayLanguage: Language,
    dPackage: DPackage,
    pathProvider: FilePathProvider,
    docsHolder: DocumentablesHolder
) : PackageDocumentableConverter(
    displayLanguage,
    dPackage,
    pathProvider,
    docsHolder
) {
    override val header: DefaultDevsitePlatformSelector? = null
    private val functionConverter =
        FunctionDocumentableConverter(displayLanguage, pathProvider, javadocConverter)
    private val propertyConverter =
        PropertyDocumentableConverter(displayLanguage, pathProvider, javadocConverter)
    override val functionToSummaryConverter = functionConverter::summary
    override val functionToDetailConverter = functionConverter::detail
    override val propertyToSummaryConverter = propertyConverter::summary
    override val propertyToDetailConverter = propertyConverter::detail
    override val docsToSummary = javadocConverter::docsToSummaryDefault
}

internal class KmpPackageConverter(
    displayLanguage: Language,
    dPackage: DPackage,
    pathProvider: FilePathProvider,
    docsHolder: DocumentablesHolder
) : PackageDocumentableConverter(
    displayLanguage,
    dPackage,
    pathProvider,
    docsHolder
) {
    override val header = DefaultDevsitePlatformSelector(
        platforms = listOf(Platform.COMMON, Platform.JVM, Platform.JS, Platform.NATIVE)
    )
    private val functionConverter =
        FunctionDocumentableConverter(displayLanguage, pathProvider, javadocConverter)
    private val propertyConverter =
        PropertyDocumentableConverter(displayLanguage, pathProvider, javadocConverter)
    override val functionToSummaryConverter = functionConverter::summaryKmp
    override val functionToDetailConverter = functionConverter::detailKmp
    override val propertyToSummaryConverter = propertyConverter::summaryKmp
    override val propertyToDetailConverter = propertyConverter::detailKmp
    override val docsToSummary = javadocConverter::docsToSummaryKmp
}
