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
import com.google.devsite.util.LibraryMetadata
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Ignore
import org.junit.Test

internal class DefaultLibraryMetadataComponentTest {

    private val libraryMetadata = LibraryMetadata(
        groupId = "testGroup",
        artifactId = "testArtifactId",
        releaseNotesUrl = "https://d.android.com",
        jarContents = emptyList()
    )

    // TODO: update and re-enable test when implementing b/243175565
    @Ignore("b/243175565")
    @Test
    fun `Library metadata renders correctly`() {
        val component = DefaultLibraryMetadataComponent(libraryMetadata)

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <div><a href="https://d.android.com">testGroup:testArtifactId</a></div>
</body>
            """.trim()
        )
    }
}
