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

/** Converts various inputs to output file paths. */
internal interface FilePathProvider {

    /** The DokkaLocationProvider that is used to provide locations of external documentation */
    val locationProvider: ExternalDokkaLocationProvider?

    /** Get this provider with only relative paths. */
    val relative: FilePathProvider

    /** The raw list of packages in plain text format. */
    val packageList: String

    /** The HTML list of packages for human consumption. */
    val packages: String

    /** The HTML list of classes for human consumption. */
    val classes: String

    /** The global index file that encompasses all packages. */
    val rootIndex: String

    /** The _toc.yaml file, responsible for pointing to the paths of each index.html file in the doc tree. */
    val toc: String

    /** The _book.yaml file, responsible for the sidebar nav. */
    val book: String

    /** @return the path of a class-like type */
    fun forType(packageName: String, name: String): String
}
