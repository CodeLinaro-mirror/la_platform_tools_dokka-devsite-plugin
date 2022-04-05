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

package com.google.devsite.util

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileNotFoundException
import java.io.IOException

class JsonLibraryMetadataTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    @Test
    fun `getMetadataFromFile with empty string filename`() {
        val actual = JsonLibraryMetadata.getMetadataFromFile("")
        val expected = emptyList<JsonLibraryMetadata>()
        assertThat(actual).isEqualTo(expected)
    }

    @Test(expected = FileNotFoundException::class)
    fun `getMetadataFromFile with missing file`() {
        JsonLibraryMetadata.getMetadataFromFile("NotAnActualFile.json")
    }

    @Test(expected = IOException::class)
    fun `getMetadataFromFile with unparseable json file`() {
        val json = """
[
  {
    "a": "b"
  },
  {
    "c": "d"
  }
]
        """.trimIndent()

        val file = folder.newFile("Unparseable.json")
        file.writeText(json)
        JsonLibraryMetadata.getMetadataFromFile(file.toString())
    }

    @Test
    fun `getMetadataFromFile with valid json file`() {
        val json = """
[
  {
    "library": "a",
    "groupId": "androidx.a",
    "artifactId": "a-runtime",
    "releaseNotesUrl": "https://d.android.com/a"
  },
  {
    "library": "b",
    "groupId": "androidx.b",
    "artifactId": "b-runtime",
    "releaseNotesUrl": "https://d.android.com/b"
  }
]
        """.trimIndent()

        val file = folder.newFile("LibraryMetadata.json")
        file.writeText(json)

        val metadata = JsonLibraryMetadata.getMetadataFromFile(file.toString())
        assertThat(metadata.size).isEqualTo(2)

        val libraryMetadata = metadata[0]
        assertThat(libraryMetadata.library).isEqualTo("a")
        assertThat(libraryMetadata.groupId).isEqualTo("androidx.a")
        assertThat(libraryMetadata.artifactId).isEqualTo("a-runtime")
        assertThat(libraryMetadata.releaseNotesUrl).isEqualTo("https://d.android.com/a")
    }
}
