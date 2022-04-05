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

package com.google.devsite.renderer

import com.google.devsite.renderer.impl.ClassGraph
import com.google.devsite.renderer.impl.DocumentablesGraph
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.MetadataRenderer
import com.google.devsite.renderer.impl.PackageRenderer
import com.google.devsite.renderer.impl.paths.DacJavaFilePathProvider
import com.google.devsite.renderer.impl.paths.DacKotlinFilePathProvider
import com.google.devsite.renderer.impl.paths.DacVersionedDocsFilePathProvider
import com.google.devsite.renderer.impl.paths.DefaultExternalDokkaLocationProvider
import com.google.devsite.renderer.impl.paths.ExternalDokkaLocationProvider
import com.google.devsite.util.JsonLibraryMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.renderers.Renderer

/** Composite renderer which outputs multiple languages (i.e. Java + Kotlin) */
internal class MultiLanguageRenderer(
    private val context: DokkaContext,
    private val outputWriter: OutputWriter
) : Renderer {
    private val tenant: String by lazy {
        checkNotNull(System.getenv("DEVSITE_TENANT") ?: System.getProperty("tenant")) {
            "Please specify the DEVSITE_TENANT envar. For example, if you were generating" +
                " AndroidX docs, you would set DEVSITE_TENANT=\"androidx\""
        }
    }

    private val versionedTenant: String? by lazy {
        System.getenv("DEVSITE_TENANT_VERSIONED") ?: System.getProperty("versionedTenant")
    }

    // Set of packages that Dackka will exclude for both Java and Kotlin refdoc generation
    private val excludedPackagesForBoth: Set<Regex> by lazy {
        System.getenv("DACKKA_EXCLUDED_PACKAGES")?.split(",")
            ?.map { it.toRegex() }?.toSet() ?: emptySet()
    }

    // Set of packages that Dackka will exclude for Java refdoc generation, which includes
    // packages specified in `excludedPackagesForBoth`
    private val excludedPackagesForJava: Set<Regex> by lazy {
        excludedPackagesForBoth +
            (
                System.getenv("DACKKA_EXCLUDED_PACKAGES_JAVA")?.split(",")
                    ?.map { it.toRegex() }?.toSet() ?: emptySet()
                )
    }

    // Set of packages that Dackka will exclude for Java refdoc generation, which includes
    // packages specified in `excludedPackagesForBoth`
    private val excludedPackagesForKotlin: Set<Regex> by lazy {
        excludedPackagesForBoth +
            (
                System.getenv("DACKKA_EXCLUDED_PACKAGES_KOTLIN")?.split(",")
                    ?.map { it.toRegex() }?.toSet() ?: emptySet()
                )
    }

    /**
     * Boolean to determine if library metadata (such as artifact ID) should be shown.
     *
     * This value does not do anything if "LIBRARY_METADATA_FILE" is not specified (see
     * [libraryMetadataFilename])
     */
    private val showLibraryMetadata: Boolean by lazy {
        System.getenv("SHOW_LIBRARY_METADATA") == "true"
    }

    /**
     * The location of the JSON file containing the library metadata.
     *
     * Returns an empty string if "SHOW_LIBRARY_METADATA" system variable isn't defined.
     *
     * This value does not do anything if "SHOW_LIBRARY_METADATA" is not true (see
     * [showLibraryMetadata])
     */
    private val libraryMetadataFilename: String by lazy {
        System.getenv("LIBRARY_METADATA_FILE") ?: ""
    }

    override fun render(root: RootPageNode) {
        val module = (root as ModulePageNode).documentables.single() as DModule
        val locationProvider = DefaultExternalDokkaLocationProvider(
            dokkaLocationProvider = DokkaLocationProvider(root, context)
        )

        runBlocking(Dispatchers.Default) {
            val jsonLibraryMetadataArray = JsonLibraryMetadata.getMetadataFromFile(
                libraryMetadataFilename
            )
            val jHolder = DocumentablesHolder(
                module = module,
                scope = this,
                context = context,
                excludedPackages = excludedPackagesForJava,
                showLibraryMetadata = showLibraryMetadata,
                libraryMetadata = jsonLibraryMetadataArray,
            )
            val jClassGraph = jHolder.classGraph()
            val jDocumentablesGraph = jHolder.documentablesGraph()
            val kHolder = DocumentablesHolder(
                module = module,
                scope = this,
                context = context,
                excludedPackages = excludedPackagesForKotlin,
                showLibraryMetadata = showLibraryMetadata,
                libraryMetadata = jsonLibraryMetadataArray,
            )
            val kClassGraph = kHolder.classGraph()
            val kDocumentablesGraph = kHolder.documentablesGraph()

            launch { renderJava(jHolder, locationProvider, jClassGraph, jDocumentablesGraph) }
            launch { renderKotlin(kHolder, locationProvider, kClassGraph, kDocumentablesGraph) }
        }
    }

    private suspend fun renderJava(
        holder: DocumentablesHolder,
        locationProvider: ExternalDokkaLocationProvider,
        classGraph: ClassGraph,
        documentablesGraph: DocumentablesGraph
    ) {
        if (versionedTenant != null) return
        val language = Language.JAVA
        val filePaths = DacJavaFilePathProvider(
            tenant, locationProvider, classGraph,
            documentablesGraph
        )
        DevsiteRenderer(
            MetadataRenderer(outputWriter, filePaths, language, holder),
            PackageRenderer(outputWriter, filePaths, language, holder),
            holder
        ).render()
    }

    private suspend fun renderKotlin(
        holder: DocumentablesHolder,
        locationProvider: ExternalDokkaLocationProvider,
        classGraph: ClassGraph,
        documentablesGraph: DocumentablesGraph
    ) {
        val language = Language.KOTLIN
        val filePaths = versionedTenant?.let {
            DacVersionedDocsFilePathProvider(
                it, locationProvider, classGraph, documentablesGraph
            )
        } ?: DacKotlinFilePathProvider(tenant, locationProvider, classGraph, documentablesGraph)
        DevsiteRenderer(
            MetadataRenderer(outputWriter, filePaths, language, holder),
            PackageRenderer(outputWriter, filePaths, language, holder),
            holder
        ).render()
    }
}
