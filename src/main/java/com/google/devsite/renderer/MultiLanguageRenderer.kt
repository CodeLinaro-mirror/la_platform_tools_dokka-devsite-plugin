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
import com.google.devsite.renderer.impl.paths.DefaultExternalDokkaLocationProvider
import com.google.devsite.renderer.impl.paths.ExternalDokkaLocationProvider
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

    override fun render(root: RootPageNode) {
        val module = (root as ModulePageNode).documentable as DModule
        val locationProvider = DefaultExternalDokkaLocationProvider(
            dokkaLocationProvider = DokkaLocationProvider(root, context)
        )

        runBlocking(Dispatchers.Default) {
            val holder = DocumentablesHolder(module, this, context)
            val classGraph = holder.classGraph()
            val documentablesGraph = holder.documentablesGraph()
            launch { renderJava(holder, locationProvider, classGraph, documentablesGraph) }
            launch { renderKotlin(holder, locationProvider, classGraph, documentablesGraph) }
        }
    }

    private suspend fun renderJava(
        holder: DocumentablesHolder,
        locationProvider: ExternalDokkaLocationProvider,
        classGraph: ClassGraph,
        documentablesGraph: DocumentablesGraph
    ) {
        val language = Language.JAVA
        val filePaths = DacJavaFilePathProvider(tenant, locationProvider, classGraph,
            documentablesGraph)
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
        val filePaths = DacKotlinFilePathProvider(tenant, locationProvider, classGraph,
            documentablesGraph)
        DevsiteRenderer(
            MetadataRenderer(outputWriter, filePaths, language, holder),
            PackageRenderer(outputWriter, filePaths, language, holder),
            holder
        ).render()
    }
}
