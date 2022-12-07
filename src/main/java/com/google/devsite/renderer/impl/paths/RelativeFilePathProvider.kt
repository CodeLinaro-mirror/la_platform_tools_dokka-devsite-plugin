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

package com.google.devsite.renderer.impl.paths

import com.google.devsite.renderer.impl.ClassGraph
import com.google.devsite.renderer.impl.DocumentablesGraph
import com.google.devsite.renderer.impl.paths.FilePathProvider.Companion.joinPaths

/** Creates relative file paths that have no knowledge of the containing website. */
internal class RelativeFilePathProvider(
    projectPath: String,
    override val locationProvider: ExternalDokkaLocationProvider? = null,
    override val classGraph: ClassGraph,
    override val documentablesGraph: DocumentablesGraph
) : FilePathProvider {
    override val relative = this

    override val packageList = joinPaths(projectPath, MACHINE_PACKAGE_LIST_FILE)

    override val packages = joinPaths(projectPath, PACKAGE_INDEX_FILE)

    override val classes = joinPaths(projectPath, CLASS_INDEX_FILE)

    override val rootIndex = joinPaths(projectPath, DIR_INDEX_FILE)

    override val toc = joinPaths(projectPath, TOC_FILE)

    override val book = joinPaths(projectPath, BOOK_FILE)

    override fun forType(packageName: String, name: String): String {
        val packageAsPath = packageName.replace(".", "/")
        return joinPaths(packageAsPath, "$name.html")
    }

    override val ANY get() = throw RuntimeException("Not associated with a language")
}
