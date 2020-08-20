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

/** Directory structure tailored for d.android.com. */
internal abstract class DacFilePathProviderBase(
    tenant: String,
    pathPrefix: String? = null
) : FilePathProvider {
    private val rootDevsitePath = "/reference" + if (pathPrefix == null) "" else "/$pathPrefix"
    private val tenantPath = "$rootDevsitePath/$tenant"

    override val packageList = "$tenantPath/package-list"

    override val packages = "$tenantPath/packages.html"

    override val classes = "$tenantPath/classes.html"

    override val rootIndex = "$tenantPath/index.html"

    override val toc = "$tenantPath/_toc.yaml"

    override fun forType(packageName: String, name: String): String {
        val packageAsPath = packageName.replace(".", "/")
        return "$rootDevsitePath/$packageAsPath/$name.html"
    }
}
