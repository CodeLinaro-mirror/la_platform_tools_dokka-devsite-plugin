/*
 * Copyright 2026 The Android Open Source Project
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
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultDevsitePlatformSelector
import com.google.devsite.components.impl.DefaultFunctionGroupPage
import com.google.devsite.components.impl.DefaultReferenceObject
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.FunctionGroupPage
import com.google.devsite.components.symbols.ReferenceObject
import com.google.devsite.components.table.SummaryList
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import com.google.devsite.util.ComposeProperties
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage

/** Converter for creating [FunctionGroupPage]s from [ComposeProperties.DFunctionGroup]s. */
internal class FunctionGroupConverter(
    val functionConverter: FunctionDocumentableConverter,
    val pathProvider: FilePathProvider,
    val holder: DocumentablesHolder,
) {
    /** Creates a [FunctionGroupPage] for the [functionGroup]. */
    suspend fun page(functionGroup: ComposeProperties.DFunctionGroup): FunctionGroupPage {
        // Determine if this function group is in a KMP package, and if it is, create a platform
        // selection header for the page.
        val dPackage =
            holder.documentablesGraph()[DRI(functionGroup.packageName)] as? DPackage
                ?: error("Package not found for $functionGroup")
        val isKmp = dPackage.isKMP()
        val header =
            if (isKmp) {
                DefaultDevsitePlatformSelector(dPackage.getPlatforms())
            } else {
                null
            }

        val summaries =
            functionGroup.functions.mapNotNull { function ->
                val hints =
                    ModifierHints(
                        displayLanguage = holder.displayLanguage,
                        type = DFunction::class.java,
                        containingType = DPackage::class.java,
                        isFromJava = false,
                        isSummary = true,
                        injectStatic = function.isJavaStaticMethod(),
                        inCompanion = false,
                    )
                if (isKmp) {
                    functionConverter.summaryKmp(function, hints)
                } else {
                    functionConverter.summary(function, hints)
                }
            }
        val summaryList: FunctionSummaryList =
            DefaultSummaryList(SummaryList.Params(items = summaries))

        val details =
            functionGroup.functions.mapNotNull { function ->
                val hints =
                    ModifierHints(
                        displayLanguage = holder.displayLanguage,
                        type = DFunction::class.java,
                        containingType = DPackage::class.java,
                        isFromJava = false,
                        isSummary = false,
                        injectStatic = function.isJavaStaticMethod(),
                        inCompanion = false,
                    )
                if (isKmp) {
                    functionConverter.detailKmp(function, hints)
                } else {
                    functionConverter.detail(function, hints)
                }
            }

        return DefaultFunctionGroupPage(
            FunctionGroupPage.Params(header = header, summary = summaryList, detail = details)
        )
    }

    /** Creates a [FunctionGroupPage] from the [functionGroup] and wraps it in a [DevsitePage]. */
    suspend fun devsitePage(
        functionGroup: ComposeProperties.DFunctionGroup
    ): DevsitePage<FunctionGroupPage> {
        return DefaultDevsitePage(
            DevsitePage.Params(
                holder.displayLanguage,
                // Function groups are only generated for Kotlin display, so there is no switcher.
                pathForSwitcher = null,
                bookPath = pathProvider.book,
                title = functionGroup.name,
                content = page(functionGroup),
                // Metadata components will be generated for each function. The group may not have
                // consistent metadata.
                metadataComponent = null,
                includedHeadTagPath = pathProvider.includedHeadTagsPath,
                referenceObject =
                    DefaultReferenceObject(
                        ReferenceObject.Params(
                            name = functionGroup.name,
                            path = functionGroup.packageName,
                            // The anchor enables devsite search to link directly to the item.
                            properties =
                                functionGroup.functions.map {
                                    it.dri.callable?.anchor() ?: it.name
                                },
                            language = holder.displayLanguage,
                        )
                    ),
            )
        )
    }
}
