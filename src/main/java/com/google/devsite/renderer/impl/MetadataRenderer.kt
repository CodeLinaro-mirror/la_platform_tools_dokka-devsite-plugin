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

import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.pages.RootPageNode

/** Renders root metadata files that provide a global overview of the entire packages surface. */
internal class MetadataRenderer(
    private val outputWriter: OutputWriter,
    private val pathProvider: FilePathProvider
) {
    /** Writes the list of packages in machine readable format. */
    suspend fun writePackageList(root: RootPageNode) {
        // TODO
    }

    /** Writes the home page. */
    suspend fun writeRootIndex() {
        // TODO
    }

    /** Writes the list of packages in human readable format. */
    suspend fun writePackages(root: RootPageNode) {
        // TODO
    }

    /** Writes the list of classes in human readable format. */
    suspend fun writeClasses(root: RootPageNode) {
        // TODO
    }

    /** Writes the ToC for devsite consumption. */
    suspend fun writeToc(root: RootPageNode) {
        // TODO
    }
}
