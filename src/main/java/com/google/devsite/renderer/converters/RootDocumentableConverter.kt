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

import com.google.devsite.components.impl.DefaultClassIndex
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultPackageIndex
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableOfContents
import com.google.devsite.components.impl.DefaultTocPackage
import com.google.devsite.components.pages.ClassIndex
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.PackageIndex
import com.google.devsite.components.pages.TableOfContents
import com.google.devsite.components.symbols.TocPackage
import com.google.devsite.components.table.SummaryList
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

/** Converts documentables into components for the root metadata (class/package index). */
internal class RootDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder
) {
    private val javadocConverter = DocTagConverter(displayLanguage, pathProvider, docsHolder)

    /** @return the root component for the class index page */
    // TODO(KMP b/256171288)
    suspend fun classesIndexPage(): DevsitePage<ClassIndex> {
        val allClasses = docsHolder.allClasslikes().filterNot {
            it.shouldNotBeDisplayed(displayLanguage)
        }
        val alphabetizedClasses = allClasses.groupBy(::categorizeClasslikes)
        val componentClasses = alphabetizedClasses.mapValues { (_, nodes) ->
            DefaultSummaryList(
                SummaryList.Params(
                    items = nodes.sortedBy { it.dri.classNames + " " + it.dri }
                        .map { javadocConverter.summaryForDocumentable(it) }
                )
            )
        }

        return DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage = displayLanguage,
                path = pathProvider.relative.classes,
                bookPath = pathProvider.book,
                title = "Class Index",
                content = DefaultClassIndex(
                    ClassIndex.Params(
                        pathProvider.packages,
                        componentClasses
                    )
                ),
                metadataComponent = null
            )

        )
    }

    /** @return the root component for the package index page */
    // TODO(KMP b/256171288)
    suspend fun packagesIndexPage(): DevsitePage<PackageIndex> {
        val packages = docsHolder.packages()
        val componentPackages = DefaultSummaryList(
            SummaryList.Params(
                items = packages
                    .filter { it.name != "[root]" } // this synthetic package has broken self-links
                    .map { javadocConverter.summaryForDocumentable(it) }
            )
        )

        return DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage = displayLanguage,
                path = pathProvider.relative.packages,
                bookPath = pathProvider.book,
                title = "Package Index",
                content = DefaultPackageIndex(
                    PackageIndex.Params(
                        pathProvider.classes,
                        componentPackages
                    )
                ),
                metadataComponent = null
            )
        )
    }

    /** @return the Devsite _toc.yaml */
    suspend fun tocPage(): TableOfContents {
        val packageComponents = docsHolder.packages().map { dPackage ->
            coroutineScope {
                packageForTocAsync(dPackage)
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
        return classlike.name().first().uppercaseChar()
    }

    private fun CoroutineScope.packageForTocAsync(
        dPackage: DPackage
    ): Deferred<DefaultTocPackage> = async {
        val interfaces = docsHolder.interfacesFor(dPackage).map(::typeForToc)
        val classes = docsHolder.classesFor(dPackage, displayLanguage).map(::typeForToc)
        val enums = docsHolder.enumsFor(dPackage).map(::typeForToc)
        val exceptions = docsHolder.exceptionsFor(dPackage).map(::typeForToc)
        val annotations = docsHolder.annotationsFor(dPackage).map(::typeForToc)
        val typeAliases = docsHolder.typeAliasesFor(dPackage).map(::typeForToc)
        val objects = docsHolder.interestingObjectsFor(dPackage, displayLanguage).map(::typeForToc)

        DefaultTocPackage(
            TocPackage.Params(
                name = dPackage.name,
                packageUrl = pathProvider.forReference(dPackage.dri).url,
                interfaces = interfaces,
                classes = classes,
                enums = enums,
                exceptions = exceptions,
                annotations = annotations,
                typeAliases = typeAliases,
                objects = objects
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
