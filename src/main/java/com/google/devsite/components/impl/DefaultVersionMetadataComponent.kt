/*
 * Copyright 2023 The Android Open Source Project
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
import com.google.devsite.components.symbols.VersionMetadataComponent
import com.google.devsite.util.JsonApiMetadata
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.id

/** Default implementation of a VersionMetadataComponent. */
internal data class DefaultVersionMetadataComponent(
    override val data: VersionMetadataComponent.Params
) : VersionMetadataComponent {

    override fun render(into: FlowContent): Unit = into.run {
        div {
            id = "version-metadata"
            data.addedIn?.let {
                div {
                    id = "added-in"
                    +"Added in "
                    it.render(into)
                }
            }
            data.deprecatedIn?.let {
                div {
                    id = "deprecated-in"
                    +"Deprecated in "
                    it.render(into)
                }
            }
        }
    }

    override fun toString() = "Version information:" +
        data.addedIn?.let { " Added in $it" }.orEmpty() +
        data.deprecatedIn?.let { " Deprecated in $it" }.orEmpty()

    companion object {
        /**
         * Creates a [DefaultVersionMetadataComponent] with the given [addedIn] and [deprecatedIn]
         * versions, with the link URLs in the form `[baseUrl]#version`.
         * If [baseUrl] is `null`, the link will have an empty string URL (rendered as plain text).
         * This is the format used for AndroidX version links.
         */
        fun createVersionMetadataWithBaseUrl(
            addedIn: String?,
            deprecatedIn: String?,
            baseUrl: String?
        ) = DefaultVersionMetadataComponent(
            VersionMetadataComponent.Params(
                addedIn = addedIn?.let { createVersionLinkFromBase(it, baseUrl) },
                deprecatedIn = deprecatedIn?.let { createVersionLinkFromBase(it, baseUrl) }
            )
        )

        /**
         * Generate mapping of each class to its API metadata
         */
        fun convertJsonApiMetadataToVersionMap(
            apiMetadataList: List<JsonApiMetadata>
        ): Map<String, Pair<String, String?>> {
            val versionMetadataMap = hashMapOf<String, Pair<String, String?>>()
            apiMetadataList.forEach { jsonApiMetadata ->
                versionMetadataMap[jsonApiMetadata.clazz] =
                    Pair(jsonApiMetadata.addedIn, jsonApiMetadata.deprecatedIn)

                // TODO: also process methods (b/264280616) and fields (b/281727318). This will
                // probably require the return to change from Pair to something more structured
            }

            return versionMetadataMap
        }

        private fun createVersionLinkFromBase(version: String, baseUrl: String?) =
            DefaultLink(
                Link.Params(
                    name = version,
                    url = baseUrl?.plus("#$version").orEmpty()
                )
            )
    }
}
