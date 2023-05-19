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

import com.google.devsite.DevsiteConfiguration
import com.google.devsite.renderer.converters.AnnotationDocumentableConverter
import com.google.devsite.renderer.converters.DocTagConverter
import com.google.devsite.renderer.converters.EnumValueDocumentableConverter
import com.google.devsite.renderer.converters.FunctionDocumentableConverter
import com.google.devsite.renderer.converters.MetadataConverter
import com.google.devsite.renderer.converters.ParameterDocumentableConverter
import com.google.devsite.renderer.converters.PropertyDocumentableConverter
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.MetadataRenderer
import com.google.devsite.renderer.impl.PackageRenderer
import com.google.devsite.renderer.impl.paths.DefaultExternalDokkaLocationProvider
import com.google.devsite.renderer.impl.paths.DevsiteFilePathProvider
import com.google.devsite.renderer.impl.paths.ExternalDokkaLocationProvider
import com.google.devsite.util.JsonLibraryMetadata
import com.google.devsite.util.LibraryMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.base.translators.descriptors.ExternalDocumentablesProvider
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.renderers.Renderer

/** Composite renderer which outputs multiple languages (i.e. Java + Kotlin) */
internal class MultiLanguageRenderer(
    private val context: DokkaContext,
    private val outputWriter: OutputWriter,
    private val externalDocumentablesProvider: ExternalDocumentablesProvider,
    private val devsiteConfiguration: DevsiteConfiguration
) : Renderer {

    override fun render(root: RootPageNode) {
        val module = (root as ModulePageNode).documentables.single() as DModule
        val locationProvider = DefaultExternalDokkaLocationProvider(
            dokkaLocationProvider = DokkaLocationProvider(root, context)
        )

        runBlocking(Dispatchers.Default) {
            val libraryMetadataArray = JsonLibraryMetadata.getMetadataFromFile(
                devsiteConfiguration.libraryMetadataFilename.orEmpty()
            )
            val fileMetadataMap = LibraryMetadata.convertJsonMetadataToFileMap(libraryMetadataArray)
            val jHolder = DocumentablesHolder(
                displayLanguage = Language.JAVA,
                module = module,
                scope = this,
                context = context,
                externalDocumentablesProvider = externalDocumentablesProvider,
                excludedPackages = devsiteConfiguration.computedExcludedPackagesForJava,
                fileMetadataMap = fileMetadataMap,
                baseSourceLink = devsiteConfiguration.baseSourceLink,
                annotationsNotToDisplay = devsiteConfiguration.allAnnotationsNotToDisplayJava
            )
            val kHolder = DocumentablesHolder(
                displayLanguage = Language.KOTLIN,
                module = module,
                scope = this,
                context = context,
                externalDocumentablesProvider = externalDocumentablesProvider,
                excludedPackages = devsiteConfiguration.computedExcludedPackagesForKotlin,
                fileMetadataMap = fileMetadataMap,
                baseSourceLink = devsiteConfiguration.baseSourceLink,
                annotationsNotToDisplay = devsiteConfiguration.allAnnotationsNotToDisplayKotlin
            )

            launch {
                renderLanguage(
                    Language.JAVA, devsiteConfiguration.javaDocsPath,
                    jHolder, locationProvider, devsiteConfiguration.includedHeadTagsPathJava
                )
            }
            launch {
                renderLanguage(
                    Language.KOTLIN, devsiteConfiguration.kotlinDocsPath,
                    kHolder, locationProvider, devsiteConfiguration.includedHeadTagsPathKotlin
                )
            }
        }
    }

    private suspend fun renderLanguage(
        language: Language,
        languageDocsPath: String?,
        holder: DocumentablesHolder,
        locationProvider: ExternalDokkaLocationProvider,
        includedHeadTagsPath: String?,
    ) {
        if (languageDocsPath == null) return

        val classGraph = holder.classGraph()
        val documentablesGraph = holder.documentablesGraph()

        val filePaths = DevsiteFilePathProvider(
            language,
            devsiteConfiguration.docRootPath,
            languageDocsPath,
            devsiteConfiguration.projectPath,
            includedHeadTagsPath,
            locationProvider,
            classGraph,
            documentablesGraph
        )

        val metadataConverter = MetadataConverter(holder)
        val annotationConverter = AnnotationDocumentableConverter(language, filePaths, holder)
        val paramConverter =
            ParameterDocumentableConverter(language, filePaths, annotationConverter)
        val javadocConverter =
            DocTagConverter(language, filePaths, holder, paramConverter, annotationConverter)
        val functionConverter = FunctionDocumentableConverter(
            language, filePaths, javadocConverter, paramConverter,
            annotationConverter, metadataConverter
        )
        val propertyConverter = PropertyDocumentableConverter(
            language, filePaths, javadocConverter, paramConverter,
            annotationConverter, metadataConverter
        )
        val enumConverter = EnumValueDocumentableConverter(
            language, filePaths, javadocConverter, paramConverter, annotationConverter
        )

        DevsiteRenderer(
            MetadataRenderer(outputWriter, filePaths, language, holder, javadocConverter),
            PackageRenderer(
                outputWriter,
                filePaths,
                language,
                holder,
                functionConverter,
                propertyConverter,
                enumConverter,
                javadocConverter,
                paramConverter,
                annotationConverter,
                metadataConverter
            ),
            holder,
            devsiteConfiguration,
        ).render()
    }
}
