/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.devsite.components.impl

import com.google.devsite.components.Link
import com.google.devsite.components.symbols.LibraryMetadata
import kotlinx.html.FlowContent
import kotlinx.html.div

/** Default implementation of a LibraryMetadata. */
internal class DefaultLibraryMetadata(
    override val data: LibraryMetadata.Params,
    private val shown: Boolean = false
) : LibraryMetadata {

    override fun render(into: FlowContent): Unit = into.run {
        if (!shown) return

        val params = Link.Params(
            name = data.groupId + ":" + data.artifactId,
            url = data.releaseNotesUrl
        )
        val link = DefaultLink(params)

        div {
            link.render(into)
        }
    }
}
