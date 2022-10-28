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

import com.google.devsite.FunctionSummaryList
import com.google.devsite.PropertySummaryList
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultPackageSummary
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.PackageSummary
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.table.SummaryList
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty

/** Converts documentables into components for the package summary page. */
internal class PackageDocumentableConverter(
    private val displayLanguage: Language,
    private val dPackage: DPackage,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder
) {
    private val javadocConverter = DocTagConverter(displayLanguage, pathProvider, docsHolder)
    private val functionConverter =
        FunctionDocumentableConverter(displayLanguage, pathProvider, javadocConverter)
    private val propertyConverter =
        PropertyDocumentableConverter(displayLanguage, pathProvider, javadocConverter)

    /** @return the root component for the package summary page */
    suspend fun summaryPage(): DevsitePage<PackageSummary> = coroutineScope {
        val interfaces = async {
            javadocConverter.docsToSummary(docsHolder.interfacesFor(dPackage))
        }
        val classes = async {
            javadocConverter.docsToSummary(docsHolder.classesFor(dPackage, displayLanguage))
        }
        val enums = async { javadocConverter.docsToSummary(docsHolder.enumsFor(dPackage)) }
        val objects = async {
            javadocConverter.docsToSummary(docsHolder.objectsFor(dPackage, displayLanguage))
        }
        val exceptions = async {
            javadocConverter.docsToSummary(docsHolder.exceptionsFor(dPackage))
        }
        val annotations = async {
            javadocConverter.docsToSummary(docsHolder.annotationsFor(dPackage))
        }
        val typeAliases = async {
            javadocConverter.docsToSummary(docsHolder.typeAliasesFor(dPackage))
        }

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
                path = pathProvider.relative.forReference(dPackage.dri).url,
                bookPath = pathProvider.book,
                title = dPackage.name,
                content = DefaultPackageSummary(
                    PackageSummary.Params(
                        displayLanguage,
                        description = javadocConverter.metadata(
                            documentable = dPackage,
                            isFromJava = false // This parameter is not used in the DPackage case
                        ),
                        interfaces = interfaces.await(),
                        classes = classes.await(),
                        enums = enums.await(),
                        objects = objects.await(),
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
                ),
                metadataComponent = null
            )
        )
    }

    private fun functionsToSummary(functions: List<DFunction>): FunctionSummaryList {
        val components = functions.map {
            val modifierHints = ModifierHints(
                displayLanguage = displayLanguage,
                type = DFunction::class.java,
                containingType = DPackage::class.java,
                isFromJava = it.isFromJava(),
                isSummary = true
            )
            functionConverter.summary(it, modifierHints)
        }

        return DefaultSummaryList(
            SummaryList.Params(
                items = components
            )
        )
    }

    private fun functionsToDetail(functions: List<DFunction>):
        List<SymbolDetail<FunctionSignature>> {
        return functions.map {
            val modifierHints = ModifierHints(
                displayLanguage = displayLanguage,
                type = DFunction::class.java,
                containingType = DPackage::class.java,
                isFromJava = it.isFromJava(),
                isSummary = false
            )
            functionConverter.detail(it, modifierHints)
        }
    }

    private fun propertiesToSummary(properties: List<DProperty>): PropertySummaryList {
        val components = properties.map {
            val modifierHints = ModifierHints(
                displayLanguage = displayLanguage,
                type = DProperty::class.java,
                containingType = DPackage::class.java,
                isFromJava = it.isFromJava(),
                isSummary = true
            )
            propertyConverter.summary(it, modifierHints)
        }

        return DefaultSummaryList(
            SummaryList.Params(
                items = components
            )
        )
    }

    private fun propertiesToDetail(properties: List<DProperty>):
        List<SymbolDetail<PropertySignature>> {
        return properties.map {
            val modifierHints = ModifierHints(
                displayLanguage = displayLanguage,
                type = DProperty::class.java,
                containingType = DPackage::class.java,
                isFromJava = it.isFromJava(),
                isSummary = false
            )
            propertyConverter.detail(it, modifierHints)
        }
    }

    private fun topLevelConstants() = dPackage.properties
        .filter { isConstant(it.modifiers()) }
        .sortedBy { it.name }

    private fun topLevelProperties() = dPackage.properties
        .filterNot { isConstant(it.modifiers()) }
        .filter { it.receiver == null }
        .sortedBy { it.name }

    private fun topLevelFunctions() = dPackage.functions
        .filter { it.receiver == null }
        .sortedBy { it.name + " " + it.dri }

    private fun extensionProperties() = dPackage.properties
        .filterNot { it.receiver == null }
        .sortedBy { it.name }

    private fun extensionFunctions() = dPackage.functions
        .filterNot { it.receiver == null }
        .sortedBy { it.name + " " + it.dri }
}
