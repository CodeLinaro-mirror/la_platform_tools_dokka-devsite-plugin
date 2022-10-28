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

import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.MetadataRenderer
import com.google.devsite.renderer.impl.PackageRenderer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty

internal class DevsiteRenderer(
    private val rootFileRenderer: MetadataRenderer,
    private val packageRenderer: PackageRenderer,
    private val docsHolder: DocumentablesHolder,
    private val displayLanguage: Language
) {
    suspend fun render() {
        writeRootMetadata()

        val extensionFunctionsMapping = docsHolder.extensionFunctionMap(displayLanguage)
        val extensionPropertiesMapping = docsHolder.extensionPropertyMap()

        for (dPackage in docsHolder.packages()) {
            writePackage(dPackage, extensionFunctionsMapping, extensionPropertiesMapping)
        }
    }

    private suspend fun writeRootMetadata() = coroutineScope {
        launch { rootFileRenderer.writePackageList() }
        launch { rootFileRenderer.writeRootIndex() }
        launch { rootFileRenderer.writePackages() }
        launch { rootFileRenderer.writeClasses() }
        launch { rootFileRenderer.writeToc() }
    }

    private suspend fun writePackage(
        dPackage: DPackage,
        extensionFunctionsMapping: Map<DRI, List<DFunction>>,
        extensionPropertiesMapping: Map<DRI, List<DProperty>>
    ) = coroutineScope {
        launch { packageRenderer.writeIndex(dPackage) }
        launch { packageRenderer.writePackageSummary(dPackage) }

        for (clazz in docsHolder.classlikesFor(dPackage, displayLanguage)) {
            launch {
                packageRenderer.writeClasslike(
                    dPackage,
                    clazz,
                    extensionFunctionsMapping.getOrDefault(clazz.dri, emptyList()),
                    extensionPropertiesMapping.getOrDefault(clazz.dri, emptyList()),
                )
            }
        }
    }
}
