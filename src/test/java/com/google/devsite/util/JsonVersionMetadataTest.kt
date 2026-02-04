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

package com.google.devsite.util

import com.google.common.truth.Truth.assertThat
import java.io.FileNotFoundException
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JsonVersionMetadataTest {

    @JvmField @Rule val folder = TemporaryFolder()

    @Test
    fun `getMetadataFromFile with empty string filename`() {
        val actual = JsonVersionMetadata.getMetadataFromFile("")
        val expected = emptyList<JsonVersionMetadata>()
        assertThat(actual).isEqualTo(expected)
    }

    @Test(expected = FileNotFoundException::class)
    fun `getMetadataFromFile with missing file throws FileNotFoundException`() {
        JsonVersionMetadata.getMetadataFromFile("NotAnActualFile.json")
    }

    @Test(expected = IOException::class)
    fun `getMetadataFromFile with unparseable json file throws IOException`() {
        val json =
            """
            [
              {
                "a": "b"
              },
              {
                "c": "d"
              }
            ]
            """
                .trimIndent()

        val file = folder.newFile("Unparseable.json")
        file.writeText(json)
        JsonLibraryMetadata.getMetadataFromFile(file.toString())
    }

    @Test
    fun `getMetadataFromFile with valid json file`() {
        val json =
            """
            [
              {
                "class": "androidx.fragment.foo",
                "addedIn": "1.0.0",
                "deprecatedIn": "1.1.0",
                "methods": [
                  {
                    "method": "isFoo()",
                    "addedIn": "1.0.0",
                    "deprecatedIn": "1.1.0"
                  },
                  {
                    "method": "isBar()",
                    "addedIn": "1.0.1"
                  }
                ],
                "fields": [
                  {
                    "field": "FIELD_FOO",
                    "addedIn": "1.0.0",
                    "deprecatedIn": "1.1.0"
                  },
                  {
                    "field": "FIELD_BAR",
                    "addedIn": "1.0.1"
                  }
                ]
              },
              {
                "class": "androidx.fragment.bar",
                "addedIn": "1.2.3",
                "methods": [],
                "fields": []
              }
            ]
            """
                .trimIndent()

        val file = folder.newFile("LibraryMetadata.json")
        file.writeText(json)

        val metadata = JsonVersionMetadata.getMetadataFromFile(file.toString())
        assertThat(metadata.size).isEqualTo(2)

        val versionMetadata = metadata.first()
        assertThat(versionMetadata.clazz).isEqualTo("androidx.fragment.foo")
        assertThat(versionMetadata.addedIn).isEqualTo("1.0.0")
        assertThat(versionMetadata.deprecatedIn).isEqualTo("1.1.0")

        assertThat(versionMetadata.methods.size).isEqualTo(2)

        val methodsFoo = versionMetadata.methods[0]
        assertThat(methodsFoo.method).isEqualTo("isFoo()")
        assertThat(methodsFoo.addedIn).isEqualTo("1.0.0")
        assertThat(methodsFoo.deprecatedIn).isEqualTo("1.1.0")

        val methodsBar = versionMetadata.methods[1]
        assertThat(methodsBar.method).isEqualTo("isBar()")
        assertThat(methodsBar.addedIn).isEqualTo("1.0.1")

        assertThat(versionMetadata.fields.size).isEqualTo(2)

        val fieldsFoo = versionMetadata.fields[0]
        assertThat(fieldsFoo.field).isEqualTo("FIELD_FOO")
        assertThat(fieldsFoo.addedIn).isEqualTo("1.0.0")
        assertThat(fieldsFoo.deprecatedIn).isEqualTo("1.1.0")

        val fieldsBar = versionMetadata.fields[1]
        assertThat(fieldsBar.field).isEqualTo("FIELD_BAR")
        assertThat(fieldsBar.addedIn).isEqualTo("1.0.1")
    }

    @Test
    fun `getMetadataFromFile with valid json file with extra field`() {
        val json =
            """
            [
              {
                "class": "androidx.fragment.foo",
                "addedIn": "1.0.0",
                "extraField": "extraFoo"
              }
            ]
            """
                .trimIndent()

        val file = folder.newFile("LibraryMetadata.json")
        file.writeText(json)

        // This should not throw an exception
        JsonVersionMetadata.getMetadataFromFile(file.toString())
    }

    @Test
    fun `getMetadataFromFile with missing JSON fields returns default values`() {
        val json =
            """
            [
              {
                "class": "androidx.fragment.foo",
                "addedIn": "1.0.0"
              },
              {
                "class": "androidx.fragment.bar",
                "addedIn": "1.0.0",
                "methods": [
                  {
                    "method": "isFoo()",
                    "addedIn": "1.0.0"
                  }
                ],
                "fields": [
                  {
                    "field": "FIELD_FOO",
                    "addedIn": "1.0.0"
                  }
                ]
              }
            ]
            """
                .trimIndent()

        val file = folder.newFile("LibraryMetadata.json")
        file.writeText(json)

        val metadata = JsonVersionMetadata.getMetadataFromFile(file.toString())
        val versionMetadata = metadata.first()

        // Top level - deprecatedIn returns null by default
        assertThat(versionMetadata.deprecatedIn).isNull()

        // methods and fields return emptyList() by default
        assertThat(versionMetadata.methods).isEmpty()
        assertThat(versionMetadata.fields).isEmpty()

        // Methods - deprecatedIn returns null by default
        assertThat(metadata[1].methods.first().deprecatedIn).isNull()

        // Fields - deprecatedIn returns null by default
        assertThat(metadata[1].fields.first().deprecatedIn).isNull()
    }
}
