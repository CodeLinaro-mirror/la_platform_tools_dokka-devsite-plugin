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

package com.google.devsite.renderer.impl

import com.google.devsite.components.impl.DefaultRedirectPage
import com.google.devsite.components.pages.RedirectPage
import com.google.devsite.components.symbols.Platform
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.AnnotationDocumentableConverter
import com.google.devsite.renderer.converters.DocTagConverter
import com.google.devsite.renderer.converters.EnumValueDocumentableConverter
import com.google.devsite.renderer.converters.FunctionDocumentableConverter
import com.google.devsite.renderer.converters.KmpClasslikeConverter
import com.google.devsite.renderer.converters.KmpPackageConverter
import com.google.devsite.renderer.converters.MetadataConverter
import com.google.devsite.renderer.converters.NonKmpClasslikeConverter
import com.google.devsite.renderer.converters.NonKmpPackageConverter
import com.google.devsite.renderer.converters.ParameterDocumentableConverter
import com.google.devsite.renderer.converters.PropertyDocumentableConverter
import com.google.devsite.renderer.converters.isSynthetic
import com.google.devsite.renderer.converters.name
import com.google.devsite.renderer.converters.packageName
import com.google.devsite.renderer.impl.paths.DIR_INDEX_NAME
import com.google.devsite.renderer.impl.paths.FilePathProvider
import com.google.devsite.renderer.impl.paths.PACKAGE_SUMMARY_NAME
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.Platform.jvm
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty

/** Renders docs for a single package, including the summary and each symbol. */
internal class PackageRenderer(
    private val outputWriter: OutputWriter,
    private val pathProvider: FilePathProvider,
    private val displayLanguage: Language,
    private val docsHolder: DocumentablesHolder,
    private val functionConverter: FunctionDocumentableConverter,
    private val propertyConverter: PropertyDocumentableConverter,
    private val enumConverter: EnumValueDocumentableConverter,
    private val javadocConverter: DocTagConverter,
    private val paramConverter: ParameterDocumentableConverter,
    private val annotationConverter: AnnotationDocumentableConverter,
    private val metadataConverter: MetadataConverter
) {
    /** Writes the home page. Is a redirect page with no content. */
    suspend fun writeIndex(dPackage: DPackage) {
        val redirectUrl = pathProvider.forType(dPackage.name, PACKAGE_SUMMARY_NAME)
        val redirectComponent = DefaultRedirectPage(RedirectPage.Params(redirectUrl))
        val index = createHTML().html {
            redirectComponent.render(this)
        }

        outputWriter.write(
            pathProvider.forType(dPackage.name, DIR_INDEX_NAME),
            index,
            ""
        )
    }

    suspend fun writePackageSummary(dPackage: DPackage) {
        val converter = if (dPackage.isKMP()) {
            KmpPackageConverter(
                displayLanguage,
                dPackage,
                pathProvider,
                docsHolder,
                functionConverter,
                propertyConverter,
                javadocConverter,
                paramConverter,
                dPackage.getPlatforms()
            )
        } else
            NonKmpPackageConverter(
                displayLanguage,
                dPackage,
                pathProvider,
                docsHolder,
                functionConverter,
                propertyConverter,
                javadocConverter,
                paramConverter
            )
        val page = converter.summaryPage()
        val packageSummary = createHTML().html {
            page.render(this)
        }

        outputWriter.write(
            pathProvider.forType(dPackage.name, PACKAGE_SUMMARY_NAME),
            packageSummary,
            ""
        )
    }

    suspend fun writeClasslike(
        dPackage: DPackage,
        classlikeDoc: DClasslike,
        classExtensionFunctions: List<DFunction>,
        classExtensionProperties: List<DProperty>
    ) {
        if (classlikeDoc.isSynthetic && displayLanguage == Language.KOTLIN) {
            return
        }
        val converter = if (dPackage.isKMP()) {
            KmpClasslikeConverter(
                displayLanguage,
                classlikeDoc,
                pathProvider,
                docsHolder,
                functionConverter,
                propertyConverter,
                enumConverter,
                javadocConverter,
                paramConverter,
                annotationConverter,
                metadataConverter,
                classExtensionFunctions,
                classExtensionProperties,
                dPackage.getPlatforms()
            )
        } else
            NonKmpClasslikeConverter(
                displayLanguage,
                classlikeDoc,
                pathProvider,
                docsHolder,
                functionConverter,
                propertyConverter,
                enumConverter,
                javadocConverter,
                paramConverter,
                annotationConverter,
                metadataConverter,
                classExtensionFunctions,
                classExtensionProperties
            )
        val page = converter.classlike()
        val classlike = createHTML().html {
            page.render(this)
        }

        outputWriter.write(
            pathProvider.forType(classlikeDoc.packageName(), classlikeDoc.name()),
            classlike,
            ""
        )
    }
    // Note: this cannot distinguish java-only, android-only, and non-KMP libraries.
    private fun DPackage.isKMP() = displayLanguage == Language.KOTLIN &&
        (sourceSets.size > 1 || sourceSets.single().analysisPlatform != jvm)
}
private fun DPackage.getPlatforms() =
    sourceSets.map { Platform.from(it.analysisPlatform) }.toSet().sorted()
