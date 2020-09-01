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

import com.google.devsite.components.ClassIndex
import com.google.devsite.components.DevsitePage
import com.google.devsite.components.PackageIndex
import com.google.devsite.components.SummaryList
import com.google.devsite.components.TableOfContents
import com.google.devsite.components.TocPackage
import com.google.devsite.components.TwoPaneSummaryItem
import com.google.devsite.components.impl.DefaultClassIndex
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultPackageIndex
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableOfContents
import com.google.devsite.components.impl.DefaultTocPackage
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DTypeAlias

/** Converts documentables into components for the root metadata (class/package index). */
internal class RootDocumentableConverter(
    private val displayLanguage: Language,
    private val module: DModule,
    private val pathProvider: FilePathProvider
) {
    private val javadocConverter = DocTagConverter(displayLanguage, pathProvider)

    /** @return the root component for the class index page */
    fun classesPage(): DevsitePage {
        val allClasses = module.packages.flatMap { it.classlikes() }.sortedBy { it.name() }
        val alphabetizedClasses = allClasses.groupBy(::categorizeClasslikes)
        val componentClasses = alphabetizedClasses.mapValues { (_, nodes) ->
            DefaultSummaryList(
                SummaryList.Params(
                    items = nodes.map(::summaryForClass)
                )
            )
        }

        return DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage,
                path = pathProvider.relative.classes,
                bookPath = pathProvider.book,
                title = "Class Index",
                content = DefaultClassIndex(
                    ClassIndex.Params(
                        pathProvider.packages,
                        componentClasses
                    )
                )
            )
        )
    }

    /** @return the root component for the package index page */
    fun packagesPage(): DevsitePage {
        val packages = module.sortedPackages()
        val componentPackages = DefaultSummaryList(
            SummaryList.Params(
                items = packages.map(::summaryForPackage)
            )
        )

        return DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage,
                path = pathProvider.relative.packages,
                bookPath = pathProvider.book,
                title = "Package Index",
                content = DefaultPackageIndex(
                    PackageIndex.Params(
                        pathProvider.classes,
                        componentPackages
                    )
                )
            )
        )
    }

    /** @return the Devsite _toc.yaml */
    suspend fun tocPage(): TableOfContents {
        val packageComponents = module.sortedPackages().map { packageDoc ->
            coroutineScope {
                packageForTocAsync(packageDoc)
            }
        }.awaitAll()

        return DefaultTableOfContents(
            TableOfContents.Params(
                classesUrl = pathProvider.classes,
                packagesUrl = pathProvider.packages,
                packages = packageComponents
            )
        )
    }

    /** Groups class-like types into buckets of their first letter. */
    private fun categorizeClasslikes(classlike: DClasslike): Char {
        return classlike.name().first().toUpperCase()
    }

    private fun summaryForClass(classlike: DClasslike): DefaultTwoPaneSummaryItem {
        return DefaultTwoPaneSummaryItem(
            TwoPaneSummaryItem.Params(
                title = pathProvider.linkForReference(classlike.dri),
                description = javadocConverter.summaryDescription(classlike)
            )
        )
    }

    private fun summaryForPackage(packageDoc: DPackage): DefaultTwoPaneSummaryItem {
        return DefaultTwoPaneSummaryItem(
            TwoPaneSummaryItem.Params(
                title = pathProvider.linkForReference(packageDoc.dri),
                description = javadocConverter.summaryDescription(packageDoc)
            )
        )
    }

    private fun CoroutineScope.packageForTocAsync(
        packageDoc: DPackage
    ): Deferred<DefaultTocPackage> = async {
        val interfaces = async { packageDoc.interfaces().map(::typeForToc) }
        val classes = async { packageDoc.classes().map(::typeForToc) }
        val enums = async { packageDoc.enums().map(::typeForToc) }
        val exceptions = async { packageDoc.exceptions().map(::typeForToc) }
        val annotations = async { packageDoc.annotations().map(::typeForToc) }
        val typeAliases = async { packageDoc.typeAliases().map(::typeForToc) }

        DefaultTocPackage(
            TocPackage.Params(
                name = packageDoc.name,
                packageUrl = pathProvider.forReference(packageDoc.dri).url,
                interfaces = interfaces.await(),
                classes = classes.await(),
                enums = enums.await(),
                exceptions = exceptions.await(),
                annotations = annotations.await(),
                typeAliases = typeAliases.await()
            )
        )
    }

    private fun typeForToc(classlike: DClasslike): TocPackage.Type {
        val url = pathProvider.forReference(classlike.dri).url
        return TocPackage.Type(classlike.name(), url)
    }

    private fun typeForToc(typeAlias: DTypeAlias): TocPackage.Type {
        val url = pathProvider.forReference(typeAlias.dri).url
        return TocPackage.Type(typeAlias.name, url)
    }
}
