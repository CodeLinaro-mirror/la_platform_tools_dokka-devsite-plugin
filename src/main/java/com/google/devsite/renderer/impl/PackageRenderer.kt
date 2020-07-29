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
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.PackagePageNode

/** Renders docs for a single package, including the summary and each symbol. */
internal class PackageRenderer(
    private val outputWriter: OutputWriter,
    private val pathProvider: FilePathProvider
) {
    suspend fun writePackageSummary(packagePage: PackagePageNode) {
        // TODO
    }

    suspend fun writeClass(clazz: ClasslikePageNode) {
        // TODO
    }
}
