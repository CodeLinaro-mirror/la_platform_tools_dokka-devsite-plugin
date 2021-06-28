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

/** Directory structure tailored for d.android.com. */
internal abstract class DacFilePathProviderBase(
    tenant: String,
    pathPrefix: String? = null,
    override val locationProvider: ExternalDokkaLocationProvider? = null,
    override val classGraph: ClassGraph,
    override val documentablesGraph: DocumentablesGraph,
    final override val relative: FilePathProvider =
        RelativeFilePathProvider(tenant, locationProvider, classGraph, documentablesGraph)
) : FilePathProvider {
    private val dacPath = "/reference" + if (pathPrefix == null) "" else "/$pathPrefix"

    override val packageList = "$dacPath/${relative.packageList}"

    override val packages = "$dacPath/${relative.packages}"

    override val classes = "$dacPath/${relative.classes}"

    override val rootIndex = "$dacPath/${relative.rootIndex}"

    override val toc = "$dacPath/${relative.toc}"

    override val book = "$dacPath/${relative.book}"

    override fun forType(packageName: String, name: String): String {
        return "$dacPath/${relative.forType(packageName, name)}"
    }
}
