/*
 * Copyright 2023 The Android Open Source Project
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

import com.google.devsite.components.impl.DefaultMetadataComponent
import com.google.devsite.components.impl.DefaultVersionMetadataComponent
import com.google.devsite.components.symbols.MetadataComponent
import com.google.devsite.components.symbols.VersionMetadataComponent
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.util.LibraryMetadata
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.dokka.model.WithSources

/**
 * Creates metadata components (a section containing information such as artifact ID and source
 * links) for documentables.
 */
internal class MetadataConverter(
    private val docsHolder: DocumentablesHolder
) {
    /**
     * Creates a metadata component for the classlike.
     */
    fun getMetadataForClasslike(classlike: DClasslike): MetadataComponent? {
        val entries = classlike.getSourceEntries()
        val paths = entries?.map { it.getSourceFilePath() }
        val libraryMetadata = paths?.let { classlike.findMatchingLibraryMetadata(it) }
        val sourceUrl = paths?.let { classlike.createLinkToSource(it) }
        val versionMetadata = classlike.findMatchingVersionMetadata(
            libraryMetadata?.releaseNotesUrl
        )

        return DefaultMetadataComponent(
            MetadataComponent.Params(
                libraryMetadata = libraryMetadata,
                sourceLinkUrl = sourceUrl,
                versionMetadata = versionMetadata
            )
        )
    }

    /**
     * Creates a metadata component for the [function].
     */
    fun getMetadataForFunction(function: DFunction): MetadataComponent {
        return DefaultMetadataComponent(
            MetadataComponent.Params(
                // TODO(b/264828018): display artifact ID and source link for some functions
                libraryMetadata = null,
                sourceLinkUrl = null,
                // TODO(b/264280616): display version metadata for functions
                versionMetadata = null
            )
        )
    }

    /**
     * Creates a metadata component for the [property].
     */
    fun getMetadataForProperty(property: DProperty): MetadataComponent {
        return DefaultMetadataComponent(
            MetadataComponent.Params(
                // TODO(b/264828018): display artifact ID and source link for some properties
                libraryMetadata = null,
                sourceLinkUrl = null,
                // TODO(b/281727318): display version metadata for properties
                versionMetadata = null
            )
        )
    }

    /**
     * Iterate through the library metadata Map to find a [LibraryMetadata] that matches the
     * current class being processed.  Otherwise, return null.
     */
    private fun Documentable.findMatchingLibraryMetadata(paths: List<String>): LibraryMetadata? {
        if (paths.size > 1) {
            docsHolder.logger.warn(
                "Multiple sources exist for $name. Artifact ID metadata will not be " +
                    "displayed"
            )
            return null
        }
        val path = paths.single()

        return docsHolder.fileMetadataMap[path]
    }

    /**
     * Query the API version metadata Map to find a [VersionMetadataComponent] that matches the
     * current class being processed and append a release URL.  Otherwise, return null.
     */
    private fun Documentable.findMatchingVersionMetadata(
        releaseNotesUrl: String?
    ): VersionMetadataComponent? {
        val classVersionMetadata = docsHolder.versionMetadataMap[dri.fullName]

        return if (classVersionMetadata == null) {
            null
        } else {
            DefaultVersionMetadataComponent.createVersionMetadataWithBaseUrl(
                classVersionMetadata.addedIn,
                classVersionMetadata.deprecatedIn,
                releaseNotesUrl
            )
        }
    }

    /**
     * Finds the source entries associated with the classlike. Returns null and logs a warning
     * if there are no source entries.
     */
    private fun <T> T.getSourceEntries(): Set<SourceEntry>?
        where T : WithSources, T : Documentable {
        if (sources.isEmpty()) {
            docsHolder.logger.warn("Sources for $name is empty")
            return null
        }

        return sources.entries
    }

    /**
     * Get the source file path from the [SourceEntry] relative to the root of the source directory.
     *
     * For example - this would return "androidx/paging/compose/LazyPagingItems.kt" if the path was
     * "/location/to/root/of/source/files/androidx/paging/compose/LazyPagingItems.kt".
     */
    private fun SourceEntry.getSourceFilePath(): String {
        val sourceRoots = key.sourceRoots.map { it.toString() }
        val fullFilePath = value.path
        // Find the source root that the file path starts with, so it can be trimmed off.
        // This assumes the full file path always begins with one of the source roots.
        val relevantSourceRoot = sourceRoots.first { fullFilePath.startsWith(it) }
        val filePath = fullFilePath.substringAfter(relevantSourceRoot)
        return filePath.removePrefix("/")
    }

    /**
     * Creates a link to the source of the classlike using the base URL from the configuration.
     *
     * Returns null if there was no base source link in the configuration.
     */
    private fun Documentable.createLinkToSource(paths: List<String>): String? {
        // Reduce the list of paths to a single path by taking the common prefix of all of them.
        val path = paths.reduce { currPrefix, nextPath -> currPrefix.commonPrefixWith(nextPath) }
        return docsHolder.baseSourceLink?.format(path, dri.fullName)
    }
}

typealias SourceEntry = Map.Entry<DokkaConfiguration.DokkaSourceSet, DocumentableSource>
