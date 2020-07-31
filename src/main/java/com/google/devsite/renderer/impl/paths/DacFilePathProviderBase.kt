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
internal abstract class DacFilePathProviderBase(tenant: String) : FilePathProvider {
    private val rootPath = "reference/$tenant"

    override val packageList = "$rootPath/package-list"

    override val packages = "$rootPath/packages.html"

    override val classes = "$rootPath/classes.html"

    override val rootIndex = "$rootPath/index.html"
}
