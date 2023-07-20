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

import com.google.common.annotations.VisibleForTesting
import com.google.devsite.components.impl.DefaultMetadataComponent
import com.google.devsite.components.impl.DefaultVersionMetadataComponent
import com.google.devsite.components.symbols.MetadataComponent
import com.google.devsite.components.symbols.VersionMetadataComponent
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.util.LibraryMetadata
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.driOrNull
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
    fun getMetadataForClasslike(classlike: DClasslike): MetadataComponent {
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
        val versionMetadata = function.findMatchingVersionMetadata(releaseNotesUrl = null)

        return DefaultMetadataComponent(
            MetadataComponent.Params(
                // TODO(b/264828018): display artifact ID and source link for some functions
                libraryMetadata = null,
                sourceLinkUrl = null,
                versionMetadata = versionMetadata
            )
        )
    }

    /**
     * Creates a metadata component for the [property].
     */
    fun getMetadataForProperty(property: DProperty): MetadataComponent {
        val versionMetadata = property.findMatchingVersionMetadata(releaseNotesUrl = null)
        // TODO(b/281727318): if the property has no metadata, try finding metadata for the getter

        return DefaultMetadataComponent(
            MetadataComponent.Params(
                // TODO(b/264828018): display artifact ID and source link for some properties
                libraryMetadata = null,
                sourceLinkUrl = null,
                versionMetadata = versionMetadata
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
    private fun DClasslike.findMatchingVersionMetadata(
        releaseNotesUrl: String?
    ): VersionMetadataComponent? {
        val classVersionMetadata = docsHolder.versionMetadataMap[dri.fullName]

        return classVersionMetadata?.let {
            DefaultVersionMetadataComponent.createVersionMetadataWithBaseUrl(
                it.addedIn,
                it.deprecatedIn,
                releaseNotesUrl
            )
        }
    }

    /**
     * Query the API version metadata Map to find a [VersionMetadataComponent] that matches the
     * current function being processed and append a release URL.  Otherwise, return null.
     */
    private fun DFunction.findMatchingVersionMetadata(
        releaseNotesUrl: String?
    ): VersionMetadataComponent? {
        val classVersionMetadata = docsHolder.versionMetadataMap[dri.fullName]
        val methodVersionMetadata = classVersionMetadata?.methodVersions?.get(
            apiSinceMethodSignature(this)
        )

        return methodVersionMetadata?.let {
            DefaultVersionMetadataComponent.createVersionMetadataWithBaseUrl(
                it.addedIn,
                it.deprecatedIn,
                releaseNotesUrl
            )
        }
    }

    /**
     * Query the API version metadata map to find a [VersionMetadataComponent] that matches the
     * current property being processed and append a release URL.  Otherwise, return null.
     * Many properties will be represented in the version metadata map by their accessors, so this
     * looks for metadata of the getter if metadata can't be found for the property itself.
     */
    private fun DProperty.findMatchingVersionMetadata(
        releaseNotesUrl: String?
    ): VersionMetadataComponent? {
        // TODO(b/281727318): handle top-level properties for kotlin display (will need synthetic
        // class name
        val classVersionMetadata = docsHolder.versionMetadataMap[dri.fullName]
        val propertyVersionMetadata = classVersionMetadata?.fieldVersions?.get(name)

        return propertyVersionMetadata?.let {
            DefaultVersionMetadataComponent.createVersionMetadataWithBaseUrl(
                it.addedIn,
                it.deprecatedIn,
                releaseNotesUrl
            )
        } ?: getter?.findMatchingVersionMetadata(releaseNotesUrl)
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

    companion object {

        /**
         * Converts a method signature to a string that matches the formatting in the apiSince JSON
         */
        @VisibleForTesting
        fun apiSinceMethodSignature(function: DFunction): String {
            // The metadata uses the Java API, so use the JvmName if it exists
            val functionName = function.jvmName() ?: function.name

            val paramTypes = function.parameters.mapNotNull { param ->

                // possiblyAsJava() is needed here as Metalava generates a Java view of types
                //
                // Example: both java.lang.String and kotlin.String are represented as
                // java.lang.String
                param.type.driOrNull?.possiblyAsJava()?.let {
                    "${it.packageName}.${it.classNames}"
                }
            }.joinToString(",")

            return "$functionName($paramTypes)"
        }
    }
}

typealias SourceEntry = Map.Entry<DokkaConfiguration.DokkaSourceSet, DocumentableSource>
