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
import com.google.devsite.components.Documentation
import com.google.devsite.components.Link
import com.google.devsite.components.PackageIndex
import com.google.devsite.components.SummaryList
import com.google.devsite.components.TwoPaneSummaryItem
import com.google.devsite.components.impl.DefaultClassIndex
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultDocumentation
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.impl.DefaultPackageIndex
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.pages.RootPageNode

/** Converts documentables into components for the root metadata (class/package index). */
internal class RootDocumentableConverter(
    private val root: RootPageNode,
    private val pathProvider: FilePathProvider
) {
    /** @return the root component for the class index page */
    fun classesPage(): DevsitePage {
        val allClasses = root.explodedChildren
            .filterIsInstance<ClasslikePageNode>()
            .filterNot { it.documentable is DEnumEntry }
            .map { it.documentable as DClasslike }
        val alphabetizedClasses = allClasses.groupBy(::categorizeClasslikes)
        val componentClasses = alphabetizedClasses.mapValues { (_, nodes) ->
            DefaultSummaryList(
                SummaryList.Params(
                    header = null,
                    nodes.map(::summaryForClass)
                )
            )
        }

        return DefaultDevsitePage(
            DevsitePage.Params(
                "Class Index",
                DefaultClassIndex(ClassIndex.Params(pathProvider.packages, componentClasses))
            )
        )
    }

    /** @return the root component for the package index page */
    fun packagesPage(): DevsitePage {
        val packages = root.children
            .filterIsInstance<PackagePageNode>()
            .map { it.documentable as DPackage }
        val componentPackages = DefaultSummaryList(
            SummaryList.Params(
                header = null,
                packages.map(::summaryForPackage)
            )
        )

        return DefaultDevsitePage(
            DevsitePage.Params(
                "Package Index",
                DefaultPackageIndex(PackageIndex.Params(pathProvider.classes, componentPackages))
            )
        )
    }

    /** Groups class-like types into buckets of their first letter. */
    private fun categorizeClasslikes(classlike: DClasslike): Char {
        val name = classlike.dri.classNames!!
        return name.first().toUpperCase()
    }

    private fun summaryForClass(classlike: DClasslike): DefaultTwoPaneSummaryItem {
        val packageName = classlike.dri.packageName!!
        val name = classlike.dri.classNames!!

        return DefaultTwoPaneSummaryItem(
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

    private fun summaryForPackage(packageDoc: DPackage): DefaultTwoPaneSummaryItem {
        return DefaultTwoPaneSummaryItem(
            TwoPaneSummaryItem.Params(
                title = DefaultLink(
                    Link.Params(
                        name = packageDoc.name,
                        url = pathProvider.forType(packageDoc.name, "package-summary")
                    )
                ),
                description = DefaultDocumentation(
                    Documentation.Params(
                        tags = packageDoc.tags(),
                        summary = true
                    )
                )
            )
        )
    }
}
