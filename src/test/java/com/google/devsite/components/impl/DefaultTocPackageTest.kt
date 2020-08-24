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
import com.google.devsite.components.TocPackage.Params
import com.google.devsite.components.TocPackage.Type
import org.junit.Test

class DefaultTocPackageTest {
    @Test
    fun `Toc package with empty types renders correctly`() {
        val component = DefaultTocPackage(
            Params(
                name = "androidx.example",
                packageUrl = "androidx/example/package-summary"
            )
        )

        val output = buildString {
            component.render(this)
        }.trim()

        assertThat(output).isEqualTo(
            """
- title: androidx.example
  path: androidx/example/package-summary

  section:
            """.trim()
        )
    }

    @Test
    fun `Toc package with types renders correctly`() {
        val component = DefaultTocPackage(
            Params(
                name = "androidx.example",
                packageUrl = "androidx/example/package-summary",
                interfaces = listOf(Type("Interface", "link")),
                classes = listOf(Type("Class", "link")),
                enums = listOf(Type("Enum", "link")),
                exceptions = listOf(Type("Exception", "link")),
                annotations = listOf(Type("Annotation", "link"))
            )
        )

        val output = buildString {
            component.render(this)
        }.trim()

        assertThat(output).isEqualTo(
            """
- title: androidx.example
  path: androidx/example/package-summary

  section:
  - title: Interfaces

    section:
    - title: Interface
      path: link

  - title: Classes

    section:
    - title: Class
      path: link

  - title: Enums

    section:
    - title: Enum
      path: link

  - title: Exceptions

    section:
    - title: Exception
      path: link

  - title: Annotations

    section:
    - title: Annotation
      path: link
            """.trim()
        )
    }

    @Test
    fun `Toc package with multiple of one type renders correctly`() {
        val component = DefaultTocPackage(
            Params(
                name = "androidx.example",
                packageUrl = "androidx/example/package-summary",
                interfaces = listOf(
                    Type("InterfaceA", "link/a"),
                    Type("InterfaceB", "link/b")
                )
            )
        )

        val output = buildString {
            component.render(this)
        }.trim()

        assertThat(output).isEqualTo(
            """
- title: androidx.example
  path: androidx/example/package-summary

  section:
  - title: Interfaces

    section:
    - title: InterfaceA
      path: link/a
    - title: InterfaceB
      path: link/b
            """.trim()
        )
    }
}
