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
import org.junit.Test

class LibraryMetadataTest {

    @Test
    fun `Verify link name and url`() {
        val metadata = LibraryMetadata(
            groupId = "androidx.a",
            artifactId = "b",
            releaseNotesUrl = "https://d.android.com/release"
        )

        assertThat(metadata.link.data.name).isEqualTo("androidx.a:b")
        assertThat(metadata.link.data.url).isEqualTo("https://d.android.com/release")
    }

    @Test
    fun `convertJsonMetadataToFileMap properly parses JSON library metadata`() {
        val jsonMetadata1 = JsonLibraryMetadata(
            groupId = "androidx.a",
            artifactId = "foo",
            releaseNotesUrl = "https://d.android.com/release/a",
            jarContents = listOf(
                "androidx/a/a.kt",
                "androidx/a/b.java"
            )
        )
        val jsonMetadata2 = JsonLibraryMetadata(
            groupId = "androidx.b",
            artifactId = "bar",
            releaseNotesUrl = "https://d.android.com/release/b",
            jarContents = listOf(
                "androidx/b/a.kt",
                "androidx/b/b.java"
            )
        )
        val mapping = LibraryMetadata.convertJsonMetadataToFileMap(
            listOf(jsonMetadata1, jsonMetadata2)
        )

        val fileMetadataA = mapping["androidx/a/a.kt"]!!
        assertThat(fileMetadataA.groupId).isEqualTo("androidx.a")
        assertThat(fileMetadataA.artifactId).isEqualTo("foo")

        val fileMetadataB = mapping["androidx/b/b.java"]!!
        assertThat(fileMetadataB.groupId).isEqualTo("androidx.b")
        assertThat(fileMetadataB.artifactId).isEqualTo("bar")
    }

    @Test
    fun `convertJsonMetadataToFileMap should only process Kotlin and Java files`() {
        val jsonMetadata = JsonLibraryMetadata(
            groupId = "androidx.a",
            artifactId = "b",
            releaseNotesUrl = "https://d.android.com/release",
            jarContents = listOf(
                "META-INF",
                "foo.bar",
                "androidx/library/",
                "androidx/library/a.kt",
                "androidx/library/b.java"
            )
        )
        val mapping = LibraryMetadata.convertJsonMetadataToFileMap(listOf(jsonMetadata))

        assertThat(mapping["androidx/library/a.kt"]).isNotNull()
        assertThat(mapping["androidx/library/b.java"]).isNotNull()

        assertThat(mapping["META-INF"]).isNull()
        assertThat(mapping["foo.bar"]).isNull()
        assertThat(mapping["androidx/library/"]).isNull()
    }
}
