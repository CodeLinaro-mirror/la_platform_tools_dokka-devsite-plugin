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

import com.google.devsite.components.symbols.LibraryMetadataComponent
import com.google.devsite.util.LibraryMetadata
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.id

/** Default implementation of a LibraryMetadata. */
internal data class DefaultLibraryMetadataComponent(
    override val data: LibraryMetadata
) : LibraryMetadataComponent {

    override fun render(into: FlowContent): Unit = into.run {
        // CSS ids are declared in internal codebase (cl/475581680)
        div {
            id = "metadata-info-block"
            div {
                id = "maven-coordinates"
                +"Artifact: "
                data.link.render(this)
            }
        }
    }

    override fun toString() = "Metadata: Release Notes URL: " + data.link
}
