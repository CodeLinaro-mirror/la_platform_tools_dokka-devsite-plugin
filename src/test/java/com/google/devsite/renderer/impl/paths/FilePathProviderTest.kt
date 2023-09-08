/*
 * Copyright 2021 The Android Open Source Project
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

package com.google.devsite.renderer.impl.paths

import com.google.common.truth.Truth.assertThat
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.ClassGraph
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.model.DModule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class FilePathProviderTest(
    private val displayLanguage: Language
) : ConverterTestBase(displayLanguage) {

    val pathProvider = pathProvider(externalLocationProvider = externalProvider)

    @Test
    fun `Root package has correct link`() {
        val dri = DRI()

        val (name, url) = pathProvider.forReference(dri)

        assertThat(name).isEqualTo("[JVM root]")
        assertPath(url, "[JVM root]/package-summary.html")
    }

    @Test
    fun `Package has correct link`() {
        val dri = DRI(packageName = "androidx.example")

        val (name, url) = pathProvider.forReference(dri)

        assertThat(name).isEqualTo("androidx.example")
        assertPath(url, "androidx/example/package-summary.html")
    }

    @Test
    fun `Class has correct link`() {
        val dri = DRI(packageName = "androidx.example", classNames = "Foo")

        val (name, url) = pathProvider.forReference(dri)

        assertThat(name).isEqualTo("Foo")
        assertPath(url, "androidx/example/Foo.html")
    }

    @Test
    fun `Top-level function has correct link`() {
        val dri = DRI(
            packageName = "androidx.example",
            callable = Callable(name = "foo", params = emptyList())
        )

        val (name, url) = pathProvider.forReference(dri)

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

        val (name, url) = pathProvider.forReference(dri)

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

        val (name, url) = pathProvider.forReference(dri)

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

        val (name, url) = pathProvider.forReference(dri)

        assertThat(name).isEqualTo("foo")
        assertPath(url, "androidx/example/Foo.html#foo()")
    }

    @Test
    fun `Enum class values have the correct link`() {
        val module = """
            class Outer {
                 enum class Inner {
                     FOO
                 }
            }
        """.trimIndent().render()
        val classGraph = classGraph(module)
        val dri = DRI(
            packageName = "androidx.example",
            classNames = "Outer.Inner.FOO",
            extra = "{\"org.jetbrains.dokka.links.EnumEntryDRIExtra\":" +
                "{\"key\":\"org.jetbrains.dokka.links.EnumEntryDRIExtra\"}}"
        )

        val (name, url) = pathProvider(classGraph = classGraph).forReference(dri)

        assertThat(name).isEqualTo("Outer.Inner.FOO")
        assertPath(url, "androidx/example/Outer.Inner.html#FOO")
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

    @Test
    fun `findInDocumentablesGraph finds top level documentable by DRI`() {
        val module = """
            |class Foo {}
        """.trimIndent().render()
        val classGraph = classGraph(module)
        val dri = DRI(
            packageName = "androidx.example",
            classNames = "Foo",
            callable = null
        )
        val actual = pathProvider(
            externalLocationProvider = null,
            classGraph = classGraph
        ).findInDocumentablesGraph(dri)?.name
        val expected = "Foo"
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `findInDocumentablesGraph finds nested documentable by DRI`() {
        val module = """
            |class Outer {
            |    class Inner {}
            |}
        """.trimIndent().render()
        val classGraph = classGraph(module)
        val dri = DRI(
            packageName = "androidx.example",
            classNames = "Outer.Inner",
            callable = null
        )
        val actual = pathProvider(
            externalLocationProvider = null,
            classGraph = classGraph
        ).findInDocumentablesGraph(dri)?.name
        val expected = "Inner"
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `findInDocumentablesGraph finds deeply nested documentable by DRI`() {
        val module = """
            |class A { class B { class C { class D {} } } }
        """.trimIndent().render()
        val classGraph = classGraph(module)
        val dri = DRI(
            packageName = "androidx.example",
            classNames = "A.B.C.D",
            callable = null
        )
        val actual = pathProvider(
            externalLocationProvider = null,
            classGraph = classGraph
        ).findInDocumentablesGraph(dri)?.name
        val expected = "D"
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Reference to hoisted companion value goes to the containing classlike page`() {
        // Check for all kinds of [DClasslike]s that implement [WithCompanion]
        val module = """
            |class FooClass {
            |    companion object {
            |        @JvmField val hoistedVal = 3
            |    }
            |}
            |annotation class FooAnnotation {
            |    companion object {
            |        @JvmField val hoistedVal = 3
            |    }
            |    }
            |}
            |interface FooInterface {
            |    companion object {
            |        @JvmField val hoistedVal = 3
            |    }
            |}
            |enum FooEnum {
            |    companion object {
            |        @JvmField val hoistedVal = 3
            |    }
            |}
        """.trimIndent().render()
        val classGraph = classGraph(module)
        for (name in listOf("FooClass", "FooAnnotation", "FooInterface", "FooEnum")) {
            val dri = DRI(
                packageName = "androidx.example",
                classNames = "$name.Companion",
                callable = Callable(name = "hoistedVal", params = emptyList())
            )
            val reference = pathProvider(
                externalLocationProvider = null,
                classGraph = classGraph
            ).forReference(dri)
            assertThat(reference.name).isEqualTo("hoistedVal")
            assertThat(reference.url.urlSuffix()).isEqualTo("$name.html#hoistedVal()")
        }
    }

    @Test
    fun `Reference to non-hoisted in Java companion function goes to the companion page in Java`() {
        val funName = "nonHoistedInJavaFun"
        val module = """
            |class Foo {
            |    companion object {
            |        fun $funName() = Unit
            |    }
            |}
        """.trimIndent().render()
        val classGraph = classGraph(module)
        val dri = DRI(
            packageName = "androidx.example",
            classNames = "Foo.Companion",
            callable = Callable(name = funName, params = emptyList())
        )
        val reference = pathProvider(
            externalLocationProvider = null,
            classGraph = classGraph
        ).forReference(dri)
        assertThat(reference.name).isEqualTo(funName)
        // All functions are hoisted in Kotlin
        val expected = when (displayLanguage) {
            Language.JAVA -> "Foo.Companion.html#$funName()"
            Language.KOTLIN -> "Foo.html#$funName()"
        }
        assertThat(reference.url.urlSuffix()).isEqualTo(expected)
    }

    @Test
    fun `Reference to hoisted function of named companion goes to the class page`() {
        val module = """
            |class Foo {
            |    companion object FooCompanion {
            |        @JvmStatic fun hoistedFun() = Unit
            |    }
            |}
        """.trimIndent().render()
        val classGraph = classGraph(module)
        val dri = DRI(
            packageName = "androidx.example",
            classNames = "Foo.FooCompanion",
            callable = Callable(name = "hoistedFun", params = emptyList())
        )
        val reference = pathProvider(
            externalLocationProvider = null,
            classGraph = classGraph
        ).forReference(dri)
        assertThat(reference.name).isEqualTo("hoistedFun")
        assertThat(reference.url.urlSuffix()).isEqualTo("Foo.html#hoistedFun()")
    }

    @Test
    fun `Reference to non-hoisted in Java companion property goes to the companion page in Java`() {
        val propertyName = "nonHoistedVal"
        val module = """
            |class Foo {
            |    companion object FooCompanion {
            |        val $propertyName = 0
            |    }
            |}
        """.trimIndent().render()
        val classGraph = classGraph(module)
        val dri = DRI(
            packageName = "androidx.example",
            classNames = "Foo.FooCompanion",
            callable = Callable(name = "nonHoistedVal", params = emptyList())
        )
        val reference = pathProvider(
            externalLocationProvider = null,
            classGraph = classGraph
        ).forReference(dri)
        assertThat(reference.name).isEqualTo(propertyName)
        // All properties are hoisted in Kotlin
        val expected = when (displayLanguage) {
            Language.JAVA -> "Foo.FooCompanion.html#$propertyName()"
            Language.KOTLIN -> "Foo.html#$propertyName()"
        }
        assertThat(reference.url.urlSuffix()).isEqualTo(expected)
    }

    @Test
    fun `No link is created for a suspend function type`() {
        val suspendFunctionDri = DRI(
            packageName = "kotlin.coroutines",
            classNames = "SuspendFunction2"
        )

        val external = object : ExternalDokkaLocationProvider {
            override fun resolve(dri: DRI): String? {
                return when (dri.packageName!!) {
                    "kotlin.coroutines" -> "http://non.empty"
                    else -> null
                }
            }
        }

        val (name, url) = pathProvider(external).forReference(suspendFunctionDri)

        assertThat(name).isEqualTo("SuspendFunction2")
        assertThat(url).isEqualTo("")
    }

    private fun classGraph(module: DModule): ClassGraph =
        runBlocking { ConverterHolder(this@FilePathProviderTest, module).classGraph }

    private fun String.urlSuffix() = substringAfter("example/")

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
