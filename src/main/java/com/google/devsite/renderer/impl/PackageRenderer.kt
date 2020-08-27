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

import com.google.devsite.components.RedirectPage
import com.google.devsite.components.impl.DefaultRedirectPage
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.PackageDocumentableConverter
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.PackagePageNode

/** Renders docs for a single package, including the summary and each symbol. */
internal class PackageRenderer(
    private val outputWriter: OutputWriter,
    private val pathProvider: FilePathProvider,
    private val displayLanguage: Language
) {
    /** Writes the home page. */
    suspend fun writeIndex(packagePage: PackagePageNode) {
        val redirectComponent = DefaultRedirectPage(RedirectPage.Params("package-summary.html"))
        val index = createHTML().html {
            redirectComponent.render(this)
        }

        outputWriter.write(
            pathProvider.forType(packagePage.name, "index"),
            index,
            ""
        )
    }

    suspend fun writePackageSummary(packagePage: PackagePageNode) {
        val converter = PackageDocumentableConverter(displayLanguage, packagePage, pathProvider)
        val page = converter.summaryPage()
        val packageSummary = createHTML().html {
            page.render(this)
        }

        outputWriter.write(
            pathProvider.forType(packagePage.name, "package-summary"),
            packageSummary,
            ""
        )
    }

    suspend fun writeClass(clazz: ClasslikePageNode) {
        // TODO
    }
}
