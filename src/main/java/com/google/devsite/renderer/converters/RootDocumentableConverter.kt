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
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DTypeAlias
import org.jetbrains.dokka.model.properties.WithExtraProperties

/** Converts documentables into components for the root metadata (class/package index). */
internal class RootDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder
) {
    private val javadocConverter = DocTagConverter(displayLanguage, pathProvider)

    /** @return the root component for the class index page */
    suspend fun classesPage(): DevsitePage {
        val allClasses = docsHolder.allClasslikes()
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
    suspend fun packagesPage(): DevsitePage {
        val packages = docsHolder.packages()
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
        val packageComponents = docsHolder.packages().map { packageDoc ->
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
        val annotations = (classlike as? WithExtraProperties<*>)?.annotations().orEmpty()
        return DefaultTwoPaneSummaryItem(
            TwoPaneSummaryItem.Params(
                title = pathProvider.linkForReference(classlike.dri),
                description = javadocConverter.summaryDescription(classlike, annotations)
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
        val interfaces = docsHolder.interfacesFor(packageDoc).map(::typeForToc)
        val classes = docsHolder.classesFor(packageDoc).map(::typeForToc)
        val enums = docsHolder.enumsFor(packageDoc).map(::typeForToc)
        val exceptions = docsHolder.exceptionsFor(packageDoc).map(::typeForToc)
        val annotations = docsHolder.annotationsFor(packageDoc).map(::typeForToc)
        val typeAliases = docsHolder.typeAliasesFor(packageDoc).map(::typeForToc)

        DefaultTocPackage(
            TocPackage.Params(
                name = packageDoc.name,
                packageUrl = pathProvider.forReference(packageDoc.dri).url,
                interfaces = interfaces,
                classes = classes,
                enums = enums,
                exceptions = exceptions,
                annotations = annotations,
                typeAliases = typeAliases
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
