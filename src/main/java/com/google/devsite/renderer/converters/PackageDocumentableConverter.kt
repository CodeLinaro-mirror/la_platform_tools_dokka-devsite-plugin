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

import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultPackageSummary
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.PackageSummary
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties

/** Converts documentables into components for the package summary page. */
internal class PackageDocumentableConverter(
    private val displayLanguage: Language,
    private val doc: DPackage,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder
) {
    private val javadocConverter = DocTagConverter(displayLanguage, pathProvider, docsHolder)
    private val functionConverter =
        FunctionDocumentableConverter(displayLanguage, pathProvider, javadocConverter)
    private val propertyConverter =
        PropertyDocumentableConverter(displayLanguage, pathProvider, javadocConverter)

    /** @return the root component for the package summary page */
    suspend fun summaryPage(): DevsitePage = coroutineScope {
        val interfaces = async { docsToSummary(docsHolder.interfacesFor(doc)) }
        val classes = async { docsToSummary(docsHolder.classesFor(doc, displayLanguage)) }
        val enums = async { docsToSummary(docsHolder.enumsFor(doc)) }
        val exceptions = async { docsToSummary(docsHolder.exceptionsFor(doc)) }
        val annotations = async { docsToSummary(docsHolder.annotationsFor(doc)) }
        val typeAliases = async { docsToSummary(docsHolder.typeAliasesFor(doc)) }

        val topLevelConstantsSummary = async { propertiesToSummary(topLevelConstants()) }
        val topLevelPropertiesSummary = async { propertiesToSummary(topLevelProperties()) }
        val topLevelFunctionsSummary = async { functionsToSummary(topLevelFunctions()) }
        val extensionPropertiesSummary = async { propertiesToSummary(extensionProperties()) }
        val extensionFunctionsSummary = async { functionsToSummary(extensionFunctions()) }

        val topLevelConstants = async { propertiesToDetail(topLevelConstants()) }
        val topLevelProperties = async { propertiesToDetail(topLevelProperties()) }
        val topLevelFunctions = async { functionsToDetail(topLevelFunctions()) }
        val extensionProperties = async { propertiesToDetail(extensionProperties()) }
        val extensionFunctions = async { functionsToDetail(extensionFunctions()) }

        DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage,
                path = pathProvider.relative.forReference(doc.dri).url,
                bookPath = pathProvider.book,
                title = doc.name,
                content = DefaultPackageSummary(
                    PackageSummary.Params(
                        displayLanguage,
                        description = javadocConverter.metadata(doc),
                        interfaces = interfaces.await(),
                        classes = classes.await(),
                        enums = enums.await(),
                        exceptions = exceptions.await(),
                        annotations = annotations.await(),
                        typeAliases = typeAliases.await(),
                        topLevelConstantsSummary = topLevelConstantsSummary.await(),
                        topLevelPropertiesSummary = topLevelPropertiesSummary.await(),
                        topLevelFunctionsSummary = topLevelFunctionsSummary.await(),
                        extensionPropertiesSummary = extensionPropertiesSummary.await(),
                        extensionFunctionsSummary = extensionFunctionsSummary.await(),
                        topLevelConstants = topLevelConstants.await(),
                        topLevelProperties = topLevelProperties.await(),
                        topLevelFunctions = topLevelFunctions.await(),
                        extensionProperties = extensionProperties.await(),
                        extensionFunctions = extensionFunctions.await()
                    )
                )
            )
        )
    }

    private fun docsToSummary(classlikes: List<Documentable>): SummaryList {
        val components = classlikes.map { classlike ->
            val annotations = (classlike as? WithExtraProperties<*>)?.annotations().orEmpty()
            DefaultTwoPaneSummaryItem(
                TwoPaneSummaryItem.Params(
                    title = pathProvider.linkForReference(classlike.dri),
                    description = javadocConverter.summaryDescription(classlike, annotations)
                )
            )
        }

        return DefaultSummaryList(
            SummaryList.Params(
                items = components
            )
        )
    }

    private fun functionsToSummary(functions: List<DFunction>): SummaryList {
        val modifierHints = ModifierHints(displayLanguage, isSummary = true)
        val components = functions.map {
            functionConverter.summary(it, modifierHints)
        }

        return DefaultSummaryList(
            SummaryList.Params(
                items = components
            )
        )
    }

    private fun functionsToDetail(functions: List<DFunction>): List<SymbolDetail> {
        val modifierHints = ModifierHints(displayLanguage, isSummary = false)
        return functions.map {
            functionConverter.detail(it, modifierHints)
        }
    }

    private fun propertiesToSummary(properties: List<DProperty>): SummaryList {
        val modifierHints = ModifierHints(displayLanguage, isSummary = true)
        val components = properties.map {
            propertyConverter.summary(it, modifierHints)
        }

        return DefaultSummaryList(
            SummaryList.Params(
                items = components
            )
        )
    }

    private fun propertiesToDetail(properties: List<DProperty>): List<SymbolDetail> {
        val modifierHints = ModifierHints(displayLanguage, isSummary = false)
        return properties.map {
            propertyConverter.detail(it, modifierHints)
        }
    }

    private fun topLevelConstants() = doc.properties
        .filter { isConstant(it.modifiers()) }
        .sortedBy { it.name }

    private fun topLevelProperties() = doc.properties
        .filterNot { isConstant(it.modifiers()) }
        .filter { it.receiver == null }
        .sortedBy { it.name }

    private fun topLevelFunctions() = doc.functions
        .filter { it.receiver == null }
        .sortedBy { it.name }

    private fun extensionProperties() = doc.properties
        .filterNot { it.receiver == null }
        .sortedBy { it.name }

    private fun extensionFunctions() = doc.functions
        .filterNot { it.receiver == null }
        .sortedBy { it.name }
}
