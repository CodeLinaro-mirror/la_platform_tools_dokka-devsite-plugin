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

import androidx.tracing.Tracer
import com.google.devsite.DevsiteConfiguration
import com.google.devsite.renderer.converters.fullName
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.MetadataRenderer
import com.google.devsite.renderer.impl.PackageRenderer
import com.google.devsite.util.composables
import com.google.devsite.util.composeModifiers
import com.google.devsite.util.hasComposeProperties
import com.google.devsite.util.traceCoroutine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.dokka.model.DPackage

internal class DevsiteRenderer(
    private val rootFileRenderer: MetadataRenderer,
    private val packageRenderer: PackageRenderer,
    private val docsHolder: DocumentablesHolder,
    private val devsiteConfiguration: DevsiteConfiguration,
) {
    context(tracer: Tracer)
    suspend fun render() {
        tracer.traceCoroutine("writeRootMetadata") { writeRootMetadata() }

        for (dPackage in docsHolder.packages()) {
            tracer.traceCoroutine("writePackage", "package" to dPackage.packageName) {
                writePackage(dPackage)
            }
        }
    }

    private suspend fun writeRootMetadata() = coroutineScope {
        launch { rootFileRenderer.writePackageList() }
        launch { rootFileRenderer.writeRootIndex() }
        launch { rootFileRenderer.writePackages() }
        launch { rootFileRenderer.writeClasses() }
        launch { rootFileRenderer.writeComposables() }
        launch { rootFileRenderer.writeModifiers() }
        launch { rootFileRenderer.writeToc(devsiteConfiguration.packagePrefixToRemoveInToc) }
    }

    context(tracer: Tracer)
    private suspend fun writePackage(dPackage: DPackage) = coroutineScope {
        launch { packageRenderer.writeIndex(dPackage) }
        launch { packageRenderer.writePackageSummary(dPackage) }

        for (clazz in docsHolder.classlikesToDisplayFor(dPackage)) {
            launch {
                tracer.traceCoroutine("writeClasslike", "class" to clazz.dri.fullName) {
                    packageRenderer.writeClasslike(dPackage, clazz)
                }
            }
        }

        // If there are compose function groups, create pages for them for kotlin display.
        if (docsHolder.displayLanguage == Language.KOTLIN) {
            for (modifier in dPackage.composeModifiers()) {
                launch {
                    tracer.traceCoroutine(
                        "writeFunctionGroup",
                        "functionGroup" to modifier.dri.fullName,
                    ) {
                        packageRenderer.writeFunctionGroup(modifier)
                    }
                }
            }
            for (composable in dPackage.composables()) {
                launch {
                    tracer.traceCoroutine(
                        "writeFunctionGroup",
                        "functionGroup" to composable.dri.fullName,
                    ) {
                        packageRenderer.writeFunctionGroup(composable)
                    }
                }
            }
        } else if (dPackage.hasComposeProperties()) {
            // Compose is only intended to be used from kotlin.
            docsHolder.logger.warn(
                "Package ${dPackage.name} is a compose package but is included in java docs"
            )
        }
    }
}
