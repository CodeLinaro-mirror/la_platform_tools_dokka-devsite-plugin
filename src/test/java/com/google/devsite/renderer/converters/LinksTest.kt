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

package com.google.devsite.renderer.converters

import com.google.common.truth.Truth.assertThat
import com.google.devsite.renderer.impl.paths.ExternalDokkaLocationProvider
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.TypeConstructor
import org.junit.Test

internal class LinksTest : ConverterTestBase() {
    @Test
    fun `Root package has correct link`() {
        val dri = DRI()

        val (name, url) = pathProvider().forReference(dri)

        assertThat(name).isEqualTo("[JVM root]")
        assertPath(url, "[JVM root]/package-summary.html")
    }

    @Test
    fun `Package has correct link`() {
        val dri = DRI(packageName = "androidx.example")

        val (name, url) = pathProvider().forReference(dri)

        assertThat(name).isEqualTo("androidx.example")
        assertPath(url, "androidx/example/package-summary.html")
    }

    @Test
    fun `Class has correct link`() {
        val dri = DRI(packageName = "androidx.example", classNames = "Foo")

        val (name, url) = pathProvider().forReference(dri)

        assertThat(name).isEqualTo("Foo")
        assertPath(url, "androidx/example/Foo.html")
    }

    @Test
    fun `Top-level function has correct link`() {
        val dri = DRI(
            packageName = "androidx.example",
            callable = Callable(name = "foo", params = emptyList())
        )

        val (name, url) = pathProvider().forReference(dri)

        assertThat(name).isEqualTo("foo")
        assertPath(url, "androidx/example/package-summary.html#foo()")
    }

    @Test
    fun `Function with params has correct link`() {
        val dri = DRI(
            packageName = "androidx.example",
            callable = Callable(
                name = "foo",
                params = listOf(
                    TypeConstructor(
                        fullyQualifiedName = "kotlin.String",
                        params = emptyList()
                    ),
                    TypeConstructor(
                        fullyQualifiedName = "kotlin.Int",
                        params = emptyList()
                    )
                )
            )
        )

        val (name, url) = pathProvider().forReference(dri)

        assertThat(name).isEqualTo("foo")
        assertPath(url, "androidx/example/package-summary.html#foo(kotlin.String,kotlin.Int)")
    }

    @Test
    fun `Function with receiver and params has correct link`() {
        val dri = DRI(
            packageName = "androidx.example",
            callable = Callable(
                name = "foo",
                receiver = TypeConstructor(
                    fullyQualifiedName = "kotlin.String",
                    params = emptyList()
                ),
                params = listOf(
                    TypeConstructor(
                        fullyQualifiedName = "kotlin.Int",
                        params = emptyList()
                    )
                )
            )
        )

        val (name, url) = pathProvider().forReference(dri)

        assertThat(name).isEqualTo("foo")
        assertPath(url, "androidx/example/package-summary.html#(kotlin.String).foo(kotlin.Int)")
    }

    @Test
    fun `Type bound function has correct link`() {
        val dri = DRI(
            packageName = "androidx.example",
            classNames = "Foo",
            callable = Callable(name = "foo", params = emptyList())
        )

        val (name, url) = pathProvider().forReference(dri)

        assertThat(name).isEqualTo("foo")
        assertPath(url, "androidx/example/Foo.html#foo()")
    }

    @Test
    fun `Attempts to resolve link externally`() {
        val enternalDri = DRI(
            packageName = "external.example",
            classNames = "Foo",
            callable = Callable(name = "foo", params = emptyList())
        )

        val external = object : ExternalDokkaLocationProvider {
            override fun resolve(dri: DRI): String? {
                return when (dri.packageName!!) {
                    "external.example" -> "http://non.com/external/example/${dri.classNames}.format"
                    else -> null
                }
            }
        }

        val (_, url) = pathProvider(external).forReference(enternalDri)
        assertThat(url).isEqualTo("http://non.com/external/example/Foo.format")
    }

    @Test
    fun `Attempts to resolve link externally and falls back when it can't`() {
        val internalDri = DRI(
            packageName = "androidx.example",
            classNames = "Foo",
            callable = Callable(name = "foo", params = emptyList())
        )

        val external = object : ExternalDokkaLocationProvider {
            override fun resolve(dri: DRI): String? {
                return when (dri.packageName!!) {
                    "external.example" -> "http://non.com/external/example/${dri.classNames}.format"
                    else -> null
                }
            }
        }

        val (_, url) = pathProvider(external).forReference(internalDri)
        assertPath(url, "androidx/example/Foo.html#foo()")
    }
}
