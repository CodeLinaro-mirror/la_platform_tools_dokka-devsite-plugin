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

import com.google.devsite.renderer.impl.MetadataRenderer
import com.google.devsite.renderer.impl.PackageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.renderers.Renderer

internal class DevsiteRenderer(
    private val rootFileRenderer: MetadataRenderer,
    private val packageRenderer: PackageRenderer
) : Renderer {
    override fun render(root: RootPageNode) {
        runBlocking(Dispatchers.Default) {
            writeRootMetadata(root)
            for (packagePage in root.children.filterIsInstance<PackagePageNode>()) {
                writePackage(packagePage)
            }
        }
    }

    private suspend fun writeRootMetadata(root: RootPageNode) = coroutineScope {
        launch { rootFileRenderer.writePackageList(root) }
        launch { rootFileRenderer.writeRootIndex() }
        launch { rootFileRenderer.writePackages(root) }
        launch { rootFileRenderer.writeClasses(root) }
        launch { rootFileRenderer.writeToc(root) }
    }

    private suspend fun writePackage(packagePage: PackagePageNode) = coroutineScope {
        launch { packageRenderer.writePackageSummary(packagePage) }
        for (clazz in packagePage.children.filterIsInstance<ClasslikePageNode>()) {
            launch { packageRenderer.writeClass(clazz) }
        }
    }
}
