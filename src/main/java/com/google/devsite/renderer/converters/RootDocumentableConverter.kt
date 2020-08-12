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

import com.google.devsite.components.ClassesIndex
import com.google.devsite.components.DevsitePage
import com.google.devsite.components.Documentation
import com.google.devsite.components.PackageIndex
import com.google.devsite.components.SummaryItem
import com.google.devsite.components.SummaryList
import com.google.devsite.components.Type
import com.google.devsite.components.impl.DefaultClassesIndex
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultDocumentation
import com.google.devsite.components.impl.DefaultPackageIndex
import com.google.devsite.components.impl.DefaultSummaryItem
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultType
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.pages.RootPageNode

/** Converts documentables into components for the root metadata (class/package index). */
internal class RootDocumentableConverter(
    private val root: RootPageNode,
    private val pathProvider: FilePathProvider
) {
    /** @return the root component for the class index page. */
    fun classesPage(): DevsitePage {
        val allClasses = root.children.flatMap { it.children }.filterIsInstance<ClasslikePageNode>()
        val alphabetizedClasses = allClasses.groupBy { it.name.first().toUpperCase() }
        val componentClasses = alphabetizedClasses.mapValues { (_, nodes) ->
            DefaultSummaryList(SummaryList.Params(nodes.map(::summaryForClass)))
        }

        return DefaultDevsitePage(
            DevsitePage.Params(
                "Class Index",
                DefaultClassesIndex(ClassesIndex.Params(pathProvider.packages, componentClasses))
            )
        )
    }

    /** @return the root component for the package index page. */
    fun packagesPage(): DevsitePage {
        val packages = root.children.filterIsInstance<PackagePageNode>()
        val componentPackages = DefaultSummaryList(
            SummaryList.Params(
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

    private fun summaryForClass(clazz: ClasslikePageNode): DefaultSummaryItem {
        val doc = clazz.documentable!!
        val packageName = doc.dri.packageName!!
        return DefaultSummaryItem(
            SummaryItem.Params(
                title = DefaultType(
                    Type.Params(
                        name = clazz.name,
                        url = pathProvider.forType(packageName, clazz.name)
                    )
                ),
                description = DefaultDocumentation(
                    Documentation.Params(
                        tags = doc.tags(),
                        summary = true
                    )
                )
            )
        )
    }

    private fun summaryForPackage(packageNode: PackagePageNode): DefaultSummaryItem {
        return DefaultSummaryItem(
            SummaryItem.Params(
                title = DefaultType(
                    Type.Params(
                        name = packageNode.name,
                        url = pathProvider.forType(packageNode.name, "package-summary")
                    )
                ),
                description = DefaultDocumentation(
                    Documentation.Params(
                        tags = packageNode.documentable!!.tags(),
                        summary = true
                    )
                )
            )
        )
    }

    private fun Documentable.tags() = documentation.values.singleOrNull()?.children.orEmpty()
}
