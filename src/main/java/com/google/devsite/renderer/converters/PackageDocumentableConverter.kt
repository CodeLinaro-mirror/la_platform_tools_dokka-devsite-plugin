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

import com.google.devsite.components.DevsitePage
import com.google.devsite.components.FunctionDetail
import com.google.devsite.components.PackageSummary
import com.google.devsite.components.SummaryList
import com.google.devsite.components.TwoPaneSummaryItem
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultPackageSummary
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage

/** Converts documentables into components for the package summary page. */
internal class PackageDocumentableConverter(
    private val displayLanguage: Language,
    private val doc: DPackage,
    private val pathProvider: FilePathProvider
) {
    private val javadocConverter = DocTagConverter(displayLanguage, pathProvider)
    private val functionConverter =
        FunctionDocumentableConverter(displayLanguage, pathProvider, javadocConverter)

    /** @return the root component for the package summary page */
    suspend fun summaryPage(): DevsitePage = coroutineScope {
        val interfaces = async { classlikesToSummary(doc.interfaces()) }
        val classes = async { classlikesToSummary(doc.classes()) }
        val enums = async { classlikesToSummary(doc.enums()) }
        val exceptions = async { classlikesToSummary(doc.exceptions()) }
        val annotations = async { classlikesToSummary(doc.annotations()) }

        val topLevelFunctionsSummary = async { functionsToSummary(doc.topLevelFunctions()) }
        val extensionFunctionsSummary = async { functionsToSummary(doc.extensionFunctions()) }

        val topLevelFunctions = async { functionsToDetail(doc.topLevelFunctions()) }
        val extensionFunctions = async { functionsToDetail(doc.extensionFunctions()) }

        DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage,
                path = pathProvider.relative.forReference(doc.dri).url,
                bookPath = pathProvider.book,
                title = doc.name,
                content = DefaultPackageSummary(
                    PackageSummary.Params(
                        displayLanguage,
                        interfaces = interfaces.await(),
                        classes = classes.await(),
                        enums = enums.await(),
                        exceptions = exceptions.await(),
                        annotations = annotations.await(),
                        topLevelFunctionsSummary = topLevelFunctionsSummary.await(),
                        extensionFunctionsSummary = extensionFunctionsSummary.await(),
                        topLevelFunctions = topLevelFunctions.await(),
                        extensionFunctions = extensionFunctions.await()
                    )
                )
            )
        )
    }

    private fun classlikesToSummary(classlikes: List<DClasslike>): SummaryList {
        val components = classlikes.map { classlike ->
            DefaultTwoPaneSummaryItem(
                TwoPaneSummaryItem.Params(
                    title = pathProvider.linkForReference(classlike.dri),
                    description = javadocConverter.summaryDescription(classlike)
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
        val components = functions.map {
            functionConverter.summary(it)
        }

        return DefaultSummaryList(
            SummaryList.Params(
                items = components
            )
        )
    }

    private fun functionsToDetail(functions: List<DFunction>): List<FunctionDetail> {
        return functions.map {
            functionConverter.detail(it)
        }
    }
}
