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
import com.google.devsite.components.Documentation
import com.google.devsite.components.Link
import com.google.devsite.components.PackageSummary
import com.google.devsite.components.SummaryList
import com.google.devsite.components.TwoPaneSummaryItem
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultDocumentation
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.impl.DefaultPackageSummary
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.pages.PackagePageNode

/** Converts documentables into components for the package summary page. */
internal class PackageDocumentableConverter(
    private val language: Language,
    private val packagePage: PackagePageNode,
    private val pathProvider: FilePathProvider
) {
    private val functionConverter = FunctionDocumentableConverter(language, pathProvider)

    /** @return the root component for the package summary page */
    suspend fun summaryPage(): DevsitePage = coroutineScope {
        val doc = packagePage.documentable as DPackage
        val allClasslikes = doc.explodedChildren.filterIsInstance<DClasslike>()
        val interfaces = async {
            classlikesToSummary(allClasslikes.filterIsInstance<DInterface>())
        }
        val classes = async {
            classlikesToSummary(allClasslikes.filterIsInstance<DClass>())
        }
        val enums = async {
            classlikesToSummary(allClasslikes.filterIsInstance<DEnum>())
        }
        val exceptions = async {
            // Dokka doesn't tell us when something is an exception and the supertypes only
            // include direct parents, so we make an educated guess that a class is an exception
            // type if it inherits methods from Throwable.
            classlikesToSummary(allClasslikes.filterIsInstance<DClass>().filter { clazz ->
                clazz.functions.any { function -> function.dri.classNames == "Throwable" }
            })
        }
        val annotations = async {
            classlikesToSummary(allClasslikes.filterIsInstance<DAnnotation>())
        }
        val topLevelFunctionsSummary = async {
            functionsToSummary(doc.functions.filter { it.receiver == null })
        }
        val extensionFunctionsSummary = async {
            functionsToSummary(doc.functions.filterNot { it.receiver == null })
        }

        DefaultDevsitePage(
            DevsitePage.Params(
                packagePage.name,
                DefaultPackageSummary(
                    PackageSummary.Params(
                        language,
                        interfaces = interfaces.await(),
                        classes = classes.await(),
                        enums = enums.await(),
                        exceptions = exceptions.await(),
                        annotations = annotations.await(),
                        topLevelFunctionsSummary = topLevelFunctionsSummary.await(),
                        extensionFunctionsSummary = extensionFunctionsSummary.await()
                    )
                )
            )
        )
    }

    private fun classlikesToSummary(classlikes: List<DClasslike>): SummaryList {
        val components = classlikes.map { classlike ->
            val packageName = classlike.dri.packageName!!
            val name = classlike.dri.classNames!!

            DefaultTwoPaneSummaryItem(
                TwoPaneSummaryItem.Params(
                    title = DefaultLink(
                        Link.Params(
                            name = name,
                            url = pathProvider.forType(packageName, name)
                        )
                    ),
                    description = DefaultDocumentation(
                        Documentation.Params(
                            tags = classlike.tags(),
                            summary = true
                        )
                    )
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
        val components = functions.mapNotNull {
            functionConverter.summary(it)
        }

        return DefaultSummaryList(
            SummaryList.Params(
                items = components
            )
        )
    }
}
