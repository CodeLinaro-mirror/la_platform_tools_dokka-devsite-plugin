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

package com.google.devsite.renderer.impl

import com.google.devsite.components.PackageList
import com.google.devsite.components.RedirectPage
import com.google.devsite.components.impl.DefaultPackageList
import com.google.devsite.components.impl.DefaultRedirectPage
import com.google.devsite.renderer.converters.RootDocumentableConverter
import com.google.devsite.renderer.converters.annotations
import com.google.devsite.renderer.converters.classes
import com.google.devsite.renderer.converters.enums
import com.google.devsite.renderer.converters.exceptions
import com.google.devsite.renderer.converters.interfaces
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.RootPageNode

/** Renders root metadata files that provide a global overview of the entire packages surface. */
internal class MetadataRenderer(
    private val outputWriter: OutputWriter,
    private val pathProvider: FilePathProvider
) {
    /** Writes the list of packages in machine readable format. */
    suspend fun writePackageList(root: RootPageNode) {
        val component = DefaultPackageList(PackageList.Params(root.children.map { it.name }))
        val packageList = buildString {
            component.render(this)
        }

        outputWriter.write(pathProvider.packageList, packageList, "")
    }

    /** Writes the home page. */
    suspend fun writeRootIndex() {
        val redirectComponent = DefaultRedirectPage(RedirectPage.Params("classes.html"))
        val rootIndex = createHTML().html {
            redirectComponent.render(this)
        }

        outputWriter.write(pathProvider.rootIndex, rootIndex, "")
    }

    /** Writes the list of packages in human readable format. */
    suspend fun writePackages(root: RootPageNode) {
        val converter = RootDocumentableConverter(root, pathProvider)
        val packageIndex = createHTML().html {
            converter.packagesPage().render(this)
        }

        outputWriter.write(pathProvider.packages, packageIndex, "")
    }

    /** Writes the list of classes in human readable format. */
    suspend fun writeClasses(root: RootPageNode) {
        val converter = RootDocumentableConverter(root, pathProvider)
        val classIndex = createHTML().html {
            converter.classesPage().render(this)
        }

        outputWriter.write(pathProvider.classes, classIndex, "")
    }

    /** Writes the ToC for devsite consumption. */
    suspend fun writeToc(root: RootPageNode) {
        val toc = StringBuilder()

        toc.appendLine("toc:")

        toc.appendLine("- title: Class Index")
        toc.appendLine("  path: ${pathProvider.classes}")
        toc.appendLine()

        toc.appendLine("- title: Package Index")
        toc.appendLine("  path: ${pathProvider.packages}")
        toc.appendLine()

        val module = (root as ModulePageNode).documentable!! as DModule

        for (pkg in module.packages) {
            val dPackage = pkg as DPackage
            toc.appendLine("- title: ${dPackage.name}")
            toc.appendLine("  path: ${pathProvider.forType(dPackage.name, "package-summary")}")
            toc.appendLine()
            toc.appendLine("  section:")

            val classMap = LinkedHashMap<String, List<DClasslike>?>()
            classMap["Classes"] = dPackage.classes()
            classMap["Interfaces"] = dPackage.interfaces()
            classMap["Enums"] = dPackage.enums()
            classMap["Annotations"] = dPackage.annotations()
            classMap["Exceptions"] = dPackage.exceptions()

            for (type in classMap.keys) {
                val classes = classMap[type]
                if (classes.isNullOrEmpty()) {
                    continue
                }
                toc.appendLine("  - title: $type")
                toc.appendLine()
                toc.appendLine("    section:")

                for (clazz in classes) {
                    val path = pathProvider.forType(dPackage.name, clazz.name!!)
                    toc.appendLine("    - title: ${clazz.name}")
                    toc.appendLine("      path: $path")
                    toc.appendLine()
                }
            }
        }

        outputWriter.write(pathProvider.toc, toc.toString(), "")
    }
}
