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

/** Creates relative file paths that have no knowledge of the containing website. */
internal class RelativeFilePathProvider(tenant: String) : FilePathProvider {
    override val relative = this

    override val packageList = "$tenant/$MACHINE_PACKAGE_LIST_FILE"

    override val packages = "$tenant/$PACKAGE_INDEX_FILE"

    override val classes = "$tenant/$CLASS_INDEX_FILE"

    override val rootIndex = "$tenant/$DIR_INDEX_FILE"

    override val toc = "$tenant/$TOC_FILE"

    override val book = "$tenant/_book.yaml"

    override fun forType(packageName: String, name: String): String {
        val packageAsPath = packageName.replace(".", "/")
        return "$packageAsPath/$name.html"
    }
}
