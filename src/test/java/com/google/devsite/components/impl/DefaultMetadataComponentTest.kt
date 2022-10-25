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

package com.google.devsite.components.impl

import com.google.common.truth.Truth.assertThat
import com.google.devsite.components.symbols.MetadataComponent
import com.google.devsite.util.LibraryMetadata
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

internal class DefaultMetadataComponentTest {

    @Test
    fun `Library metadata renders correctly`() {
        val libraryMetadata = LibraryMetadata(
            groupId = "testGroup",
            artifactId = "testArtifactId",
            releaseNotesUrl = "https://d.android.com"
        )
        val metadata = MetadataComponent.Params(
            libraryMetadata = libraryMetadata,
            sourceLinkUrl = null
        )
        val component = DefaultMetadataComponent(metadata)

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <div id="metadata-info-block">
    <div id="maven-coordinates">Artifact: <a href="https://d.android.com">testGroup:testArtifactId</a></div>
  </div>
</body>
            """.trim()
        )
    }

    @Test
    fun `Library metadata without release notes URL renders correctly`() {
        val libraryMetadata = LibraryMetadata(
            groupId = "testGroup",
            artifactId = "testArtifactId",
            releaseNotesUrl = ""
        )
        val metadata = MetadataComponent.Params(
            libraryMetadata = libraryMetadata,
            sourceLinkUrl = null
        )
        val component = DefaultMetadataComponent(metadata)

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <div id="metadata-info-block">
    <div id="maven-coordinates">Artifact: testGroup:testArtifactId</div>
  </div>
</body>
            """.trim()
        )
    }

    @Test
    fun `Link to source renders correctly`() {
        val metadata = MetadataComponent.Params(
            libraryMetadata = null,
            sourceLinkUrl = "https://cs.android.com"
        )
        val component = DefaultMetadataComponent(metadata)

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <div id="metadata-info-block">
    <div id="source-link"><a href="https://cs.android.com">View Source</a></div>
  </div>
</body>
            """.trim()
        )
    }

    @Test
    fun `Library metadata and link to source render correctly`() {
        val libraryMetadata = LibraryMetadata(
            groupId = "testGroup",
            artifactId = "testArtifactId",
            releaseNotesUrl = "https://d.android.com"
        )
        val metadata = MetadataComponent.Params(
            libraryMetadata = libraryMetadata,
            sourceLinkUrl = "https://cs.android.com"
        )
        val component = DefaultMetadataComponent(metadata)

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <div id="metadata-info-block">
    <div id="maven-coordinates">Artifact: <a href="https://d.android.com">testGroup:testArtifactId</a></div>
    <div id="source-link"><a href="https://cs.android.com">View Source</a></div>
  </div>
</body>
            """.trim()
        )
    }

    @Test
    fun `No library metadata and no link to source renders correctly`() {
        val metadata = MetadataComponent.Params(
            libraryMetadata = null,
            sourceLinkUrl = null
        )
        val component = DefaultMetadataComponent(metadata)

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <div id="metadata-info-block"></div>
</body>
            """.trim()
        )
    }
}
