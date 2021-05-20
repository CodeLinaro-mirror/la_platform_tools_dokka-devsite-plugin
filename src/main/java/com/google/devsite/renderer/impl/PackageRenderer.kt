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

import com.google.devsite.components.impl.DefaultRedirectPage
import com.google.devsite.components.pages.RedirectPage
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.ClasslikeDocumentableConverter
import com.google.devsite.renderer.converters.PackageDocumentableConverter
import com.google.devsite.renderer.converters.isSynthetic
import com.google.devsite.renderer.converters.name
import com.google.devsite.renderer.converters.packageName
import com.google.devsite.renderer.impl.paths.DIR_INDEX_NAME
import com.google.devsite.renderer.impl.paths.FilePathProvider
import com.google.devsite.renderer.impl.paths.PACKAGE_SUMMARY_FILE
import com.google.devsite.renderer.impl.paths.PACKAGE_SUMMARY_NAME
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage

/** Renders docs for a single package, including the summary and each symbol. */
internal class PackageRenderer(
    private val outputWriter: OutputWriter,
    private val pathProvider: FilePathProvider,
    private val displayLanguage: Language,
    private val docsHolder: DocumentablesHolder
) {
    /** Writes the home page. */
    suspend fun writeIndex(packageDoc: DPackage) {
        val redirectComponent = DefaultRedirectPage(RedirectPage.Params(PACKAGE_SUMMARY_FILE))
        val index = createHTML().html {
            redirectComponent.render(this)
        }

        outputWriter.write(
            pathProvider.forType(packageDoc.name, DIR_INDEX_NAME),
            index,
            ""
        )
    }

    suspend fun writePackageSummary(packageDoc: DPackage) {
        val converter =
            PackageDocumentableConverter(displayLanguage, packageDoc, pathProvider, docsHolder)
        val page = converter.summaryPage()
        val packageSummary = createHTML().html {
            page.render(this)
        }

        outputWriter.write(
            pathProvider.forType(packageDoc.name, PACKAGE_SUMMARY_NAME),
            packageSummary,
            ""
        )
    }

    suspend fun writeClasslike(classlikeDoc: DClasslike, classExtensionFunctions: List<DFunction>) {
        if (classlikeDoc.isSynthetic && displayLanguage == Language.KOTLIN) {
            return
        }
        val converter =
            ClasslikeDocumentableConverter(
                displayLanguage,
                classlikeDoc,
                pathProvider,
                docsHolder,
                classExtensionFunctions
            )
        val page = converter.classlike()
        val classlike = createHTML().html {
            page.render(this)
        }

        outputWriter.write(
            pathProvider.forType(classlikeDoc.packageName(), classlikeDoc.name()),
            classlike,
            ""
        )
    }
}
