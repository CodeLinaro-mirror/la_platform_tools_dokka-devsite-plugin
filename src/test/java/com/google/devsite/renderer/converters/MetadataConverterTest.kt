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

package com.google.devsite.renderer.converters

import com.google.common.truth.Truth.assertThat
import com.google.devsite.components.symbols.MetadataComponent
import com.google.devsite.renderer.Language
import com.google.devsite.testing.ConverterTestBase
import com.google.devsite.util.ClassVersionMetadata
import com.google.devsite.util.LibraryMetadata
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class MetadataConverterTest(private val displayLanguage: Language) :
    ConverterTestBase(displayLanguage) {
    @Test
    fun `Source links are generated correctly`() {
        val metadataComponent =
            """
            |class Foo
        """
                .render()
                .metadataForClasslike(
                    baseClassSourceLink = "https://cs.android.com/search?q=file:%s+class:%s"
                )

        val link = metadataComponent.data.sourceLink
        assertThat(link).isNotNull()

        val expectedPath = "kotlin/androidx/example/Test.kt"
        val expectedClass = "androidx.example.Foo"
        val expected = "https://cs.android.com/search?q=file:$expectedPath+class:$expectedClass"
        assertThat(link!!.data.url).isEqualTo(expected)
    }

    @Test
    fun `Source links are generated correctly with no class in format string`() {
        val metadataComponent =
            """
                |class Foo
            """
                .render()
                .metadataForClasslike(
                    baseClassSourceLink = "https://cs.android.com/search?q=file:%s"
                )

        val link = metadataComponent.data.sourceLink
        assertThat(link).isNotNull()

        val expectedPath = "kotlin/androidx/example/Test.kt"
        val expected = "https://cs.android.com/search?q=file:$expectedPath"
        assertThat(link!!.data.url).isEqualTo(expected)
    }

    @Test
    fun `API version for a Class with both addedIn and deprecatedIn is generated correctly`() {
        val metadata =
            ClassVersionMetadata(
                className = "androidx.example.Foo",
                addedIn = "1.2.3",
                deprecatedIn = "2.3.4",
            )
        val metadataComponent =
            """
            |class Foo
        """
                .render()
                .metadataForClasslike(
                    versionMetadataMap = mapOf("androidx.example.Foo" to metadata)
                )

        val versionMetadata = metadataComponent.data.versionMetadata
        assertThat(versionMetadata).isNotNull()

        assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
        assertThat(versionMetadata.data.deprecatedIn?.data?.name).isEqualTo("2.3.4")
    }

    @Test
    fun `API version for a Class with only addedIn is generated correctly`() {
        val metadata =
            ClassVersionMetadata(
                className = "androidx.example.Foo",
                addedIn = "1.2.3",
                deprecatedIn = null,
            )
        val metadataComponent =
            """
            |class Foo
        """
                .render()
                .metadataForClasslike(
                    versionMetadataMap = mapOf("androidx.example.Foo" to metadata)
                )

        val versionMetadata = metadataComponent.data.versionMetadata
        assertThat(versionMetadata).isNotNull()

        assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
        assertThat(versionMetadata.data.deprecatedIn).isNull()
    }

    @Test
    fun `API version for companion is generated correctly`() {
        // Companions appear in the metadata as both a class and a field of the containing class
        val metadata =
            mapOf(
                "androidx.example.Foo" to
                    ClassVersionMetadata(
                        className = "androidx.example.Foo",
                        addedIn = "1.2.3",
                        fieldVersions =
                            mapOf(
                                "Companion" to
                                    ClassVersionMetadata.FieldVersionMetadata(
                                        fieldName = "Companion",
                                        addedIn = "1.2.3",
                                    )
                            ),
                    ),
                "androidx.example.Foo.Companion" to
                    ClassVersionMetadata(
                        className = "androidx.example.Foo.Companion",
                        addedIn = "1.2.3",
                        methodVersions =
                            mapOf(
                                "bar()" to
                                    ClassVersionMetadata.MethodVersionMetadata(
                                        methodName = "bar()",
                                        addedIn = "1.2.3",
                                    ),
                                "getFoo()" to
                                    ClassVersionMetadata.MethodVersionMetadata(
                                        methodName = "getFoo()",
                                        addedIn = "1.2.3",
                                    ),
                            ),
                    ),
            )
        val module =
            """
            |class Foo {
            |    companion object {
            |        val foo = 3
            |        fun bar(): Unit {}
            |    }
            |}
        """
                .render()

        val companion = module.classlike("Companion")!!
        val property = companion.properties.single()
        val function = companion.functions.single()

        val metadataComponents =
            listOf(
                module.metadata(companion, versionMetadataMap = metadata),
                module.metadata(property, versionMetadataMap = metadata),
                module.metadata(function, versionMetadataMap = metadata),
            )

        for (metadataComponent in metadataComponents) {
            val versionMetadata = metadataComponent.data.versionMetadata
            assertThat(versionMetadata).isNotNull()

            assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
            assertThat(versionMetadata.data.deprecatedIn).isNull()
        }
    }

    @Test
    fun `API version for @JvmName items is generated correctly`() {
        val metadata =
            mapOf(
                "androidx.example.Foo" to
                    ClassVersionMetadata(
                        className = "androidx.example.Foo",
                        addedIn = "1.2.3",
                        methodVersions =
                            mapOf(
                                "renamedMethod()" to
                                    ClassVersionMetadata.MethodVersionMetadata(
                                        methodName = "renamedMethod()",
                                        addedIn = "1.2.3",
                                    ),
                                "renamedGetter()" to
                                    ClassVersionMetadata.MethodVersionMetadata(
                                        methodName = "renamedGetter()",
                                        addedIn = "1.2.3",
                                    ),
                            ),
                    )
            )
        val module =
            """
            |class Foo {
            |    @JvmName("renamedMethod")
            |    fun originalMethod(): Unit {}
            |
            |    @get:JvmName("renamedGetter")
            |    val originalProperty = 3
            |}
        """
                .render()

        // Dackka's version of these methods won't be renamed
        val metadataComponents =
            listOf(
                module.metadataForMethod("originalMethod", versionMetadataMap = metadata),
                module.metadataForProperty("originalProperty", versionMetadataMap = metadata),
            )

        for (metadataComponent in metadataComponents) {
            val versionMetadata = metadataComponent.data.versionMetadata
            assertThat(versionMetadata).isNotNull()

            assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
            assertThat(versionMetadata.data.deprecatedIn).isNull()
        }
    }

    @Test
    fun `API version for a method is generated correctly`() {
        val metadata =
            ClassVersionMetadata(
                className = "androidx.example.Foo",
                addedIn = "1.2.3",
                methodVersions =
                    mapOf(
                        "bar()" to
                            ClassVersionMetadata.MethodVersionMetadata(
                                methodName = "bar()",
                                addedIn = "1.2.3",
                                deprecatedIn = "2.3.4",
                            )
                    ),
            )
        val metadataComponent =
            """
            |class Foo {
            |    fun bar() {}
            |}
        """
                .render()
                .metadataForMethod(
                    name = "bar",
                    versionMetadataMap = mapOf("androidx.example.Foo" to metadata),
                )

        val versionMetadata = metadataComponent.data.versionMetadata
        assertThat(versionMetadata).isNotNull()

        assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
        assertThat(versionMetadata.data.deprecatedIn?.data?.name).isEqualTo("2.3.4")
    }

    @Test
    fun `apiSinceMethodSignature formats methods to match apiSince metadata string`() {
        val functionsJ =
            """
            |public class Foo<T> {
            |    public void bar01() {}
            |    public void bar02(String param1) {}
            |    public void bar03(Object param1) {}
            |    public void bar04(T param1) {}
            |    public void bar05(int param1) {}
            |    public void bar06(String... param1) {}
            |}
        """
                .render(java = true)
                .functions()!!
        val functionsK =
            """
            |class Foo<T> {
            |    fun bar01() {}
            |    fun bar02(param1: String) {}
            |    fun bar03(param1: Any) {}
            |    fun bar04(param1: T) {}
            |    fun bar05(param1: Int) {}
            |    fun bar06(vararg param1: String) {}
            |}
        """
                .render()
                .functions()!!

        for (functions in listOf(functionsJ, functionsK)) {
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[0])).isEqualTo("bar01()")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[1]))
                .isEqualTo("bar02(java.lang.String)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[2]))
                .isEqualTo("bar03(java.lang.Object)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[3]))
                .isEqualTo("bar04(T)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[4]))
                .isEqualTo("bar05(int)")
            // TODO(b/293340652): this should work for Java too
            if (functions == functionsK) {
                assertThat(MetadataConverter.apiSinceMethodSignature(functions[5]))
                    .isEqualTo("bar06(java.lang.String...)")
            }
        }
    }

    @Test
    fun `apiSinceMethodSignature formats methods with array params to match metadata string`() {
        val functionsJ =
            """
            |public class Foo {
            |    public void bar01(String[] param1) {}
            |    public void bar02(int[] param1) {}
            |    public void bar03(Object[] param1) {}
            |    public void bar04(Integer[] param1) {}
            |    public void bar05(int[][] param1) {}
            |    public void bar06(String[][] param1) {}
            |}
        """
                .render(java = true)
                .functions()!!
        val functionsK =
            """
            |class Foo {
            |    fun bar01(param1: Array<String>) {}
            |    fun bar02(param1: IntArray) {}
            |    fun bar03(param1: Array<Any>) {}
            |    fun bar04(param1: Array<Int?>) {}
            |    fun bar05(param1: Array<IntArray>) {}
            |    fun bar06(param1: Array<Array<String>>) {}
            |}
        """
                .render()
                .functions()!!

        for (functions in listOf(functionsJ, functionsK)) {
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[0]))
                .isEqualTo("bar01(java.lang.String[])")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[1]))
                .isEqualTo("bar02(int[])")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[2]))
                .isEqualTo("bar03(java.lang.Object[])")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[3]))
                .isEqualTo("bar04(java.lang.Integer[])")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[4]))
                .isEqualTo("bar05(int[][])")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[5]))
                .isEqualTo("bar06(java.lang.String[][])")
        }
    }

    @Test
    fun `apiSinceMethodSignature formats params with generics to match metadata string`() {
        val functionsJ =
            """
            |public class Foo {
            |    public void bar01(java.util.List<String> param1) {}
            |    public void bar02(java.util.Map<String, String> param1) {}
            |    public void bar03(java.util.List<?> param1) {}
            |    public void bar04(java.util.List<? extends String> param1) {}
            |    public void bar05(java.util.List<? super String> param1) {}
            |}
        """
                .render(java = true)
                .functions()!!
        val functionsK =
            """
            |class Foo {
            |    fun bar01(param1: List<String>) {}
            |    fun bar02(param1: Map<String, String>) {}
            |    fun bar03(param1: List<*>) {}
            |    fun bar04(param1: List<out String>) {}
            |    fun bar05(param1: List<in String>) {}
            |}
        """
                .render()
                .functions()!!

        for (functions in listOf(functionsJ, functionsK)) {
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[0]))
                .isEqualTo("bar01(java.util.List<java.lang.String>)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[1]))
                .isEqualTo("bar02(java.util.Map<java.lang.String,java.lang.String>)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[2]))
                .isEqualTo("bar03(java.util.List<?>)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[3]))
                .isEqualTo("bar04(java.util.List<? extends java.lang.String>)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[4]))
                .isEqualTo("bar05(java.util.List<? super java.lang.String>)")
        }
    }

    @Test
    fun `apiSinceMethodSignature formats methods with generics to match metadata string`() {
        val functionsJ =
            """
            |public class Foo {
            |    public <E> void bar01(E param1) {}
            |    public <E extends String> void bar02(E param1) {}
            |    public <E extends java.util.List<String>> void bar03(E param1) {}
            |    public <I, O> void bar04(java.util.Map<I, O> param1) {}
            |    public <E extends String & Cloneable> void bar05(E param) {}
            |}
        """
                .render(java = true)
                .functions()!!
        val functionsK =
            """
            |class Foo {
            |    fun <E> bar01(param1: E) {}
            |    fun <E : String> bar02(param1: E) {}
            |    fun <E : List<String>> bar03(param1: E) {}
            |    fun <I, O> bar04(param1: Map<I, O>) {}
            |    fun <E> bar05(param1: E)  where E: String, E: Cloneable {}
            |}
        """
                .render()
                .functions()!!

        for (functions in listOf(functionsJ, functionsK)) {
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[0]))
                .isEqualTo("bar01<E>(E)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[1]))
                .isEqualTo("bar02<E extends java.lang.String>(E)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[2]))
                .isEqualTo("bar03<E extends java.util.List<java.lang.String>>(E)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[3]))
                .isEqualTo("bar04<I, O>(java.util.Map<I,O>)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[4]))
                .isEqualTo("bar05<E extends java.lang.String & java.lang.Cloneable>(E)")
        }
    }

    @Test
    fun `apiSinceMethodSignature formats higher-order methods to match apiSince metadata string`() {
        val functions =
            """
            |class Foo {
            |    fun bar01(param: (String) -> String) {}
            |    fun bar02(param: (Any) -> String) {}
            |    fun bar03(param: (String) -> Any) {}
            |    fun bar04(param: (String, Int) -> String) {}
            |    fun bar05(param: (String) -> Unit) {}
            |    fun bar06(param: () -> String) {}
            |    fun bar07(param: () -> Unit) {}
            |    fun bar08(param: (List<String>) -> Unit) {}
            |}
        """
                .render()
                .functions()!!

        assertThat(MetadataConverter.apiSinceMethodSignature(functions[0]))
            .isEqualTo(
                "bar01(kotlin.jvm.functions.Function1<? super java.lang.String,java.lang.String>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[1]))
            .isEqualTo("bar02(kotlin.jvm.functions.Function1<java.lang.Object,java.lang.String>)")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[2]))
            .isEqualTo("bar03(kotlin.jvm.functions.Function1<? super java.lang.String,?>)")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[3]))
            .isEqualTo(
                "bar04(kotlin.jvm.functions.Function2<? super java.lang.String," +
                    "? super java.lang.Integer,java.lang.String>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[4]))
            .isEqualTo(
                "bar05(kotlin.jvm.functions.Function1<? super java.lang.String,kotlin.Unit>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[5]))
            .isEqualTo("bar06(kotlin.jvm.functions.Function0<java.lang.String>)")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[6]))
            .isEqualTo("bar07(kotlin.jvm.functions.Function0<kotlin.Unit>)")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[7]))
            .isEqualTo(
                "bar08(kotlin.jvm.functions.Function1<" +
                    "? super java.util.List<java.lang.String>,kotlin.Unit>)"
            )
    }

    @Test
    fun `apiSinceMethodSignature formats lambda returns to match metadata string`() {
        val functions =
            """
            |class Foo {
            |   fun bar01(param: () -> String) {}
            |   fun bar02(param: () -> String?) {}
            |   fun bar03(param: () -> List<Integer>) {}
            |   fun bar04(param: () -> List<String>) {}
            |   fun bar05(param: () -> Map<String, Integer>) {}
            |   fun bar06(param: () -> Int) {}
            |   fun bar07(param: () -> IntArray) {}
            |   fun bar08(param: () -> Array<String>) {}
            |   fun bar09(param: () -> Array<String>) {}
            |   fun bar10(param: () -> ((String) -> String)) {}
            |   fun <E> bar11(param: () -> E) {}
            |}
        """
                .render()
                .functions()!!

        assertThat(MetadataConverter.apiSinceMethodSignature(functions[0]))
            .isEqualTo("bar01(kotlin.jvm.functions.Function0<java.lang.String>)")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[1]))
            .isEqualTo("bar02(kotlin.jvm.functions.Function0<java.lang.String>)")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[2]))
            .isEqualTo(
                "bar03(kotlin.jvm.functions.Function0<? extends " +
                    "java.util.List<java.lang.Integer>>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[3]))
            .isEqualTo(
                "bar04(kotlin.jvm.functions.Function0<? extends java.util.List<java.lang.String>>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[4]))
            .isEqualTo(
                "bar05(kotlin.jvm.functions.Function0<? extends " +
                    "java.util.Map<java.lang.String,java.lang.Integer>>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[5]))
            .isEqualTo("bar06(kotlin.jvm.functions.Function0<java.lang.Integer>)")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[6]))
            .isEqualTo("bar07(kotlin.jvm.functions.Function0<int[]>)")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[7]))
            .isEqualTo("bar08(kotlin.jvm.functions.Function0<java.lang.String[]>)")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[8]))
            .isEqualTo("bar09(kotlin.jvm.functions.Function0<java.lang.String[]>)")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[9]))
            .isEqualTo(
                "bar10(kotlin.jvm.functions.Function0<? extends " +
                    "kotlin.jvm.functions.Function1<? super java.lang.String,java.lang.String>>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[10]))
            .isEqualTo("bar11<E>(kotlin.jvm.functions.Function0<? extends E>)")
    }

    @Test
    fun `apiSinceMethodSignature formats suspend params to match apiSince metadata string`() {
        val functions =
            """
            |class Foo {
            |    fun bar01(param: suspend (String) -> String) {}
            |    fun bar02(param: suspend (Any) -> String) {}
            |    fun bar03(param: suspend (String) -> Any) {}
            |    fun bar04(param: suspend () -> String) {}
            |    fun bar05(param: suspend (String, Int) -> String) {}
            |    fun bar06(param: suspend (String) -> Unit) {}
            |    fun bar07(param: suspend () -> Unit) {}
            |}
        """
                .render()
                .functions()!!

        assertThat(MetadataConverter.apiSinceMethodSignature(functions[0]))
            .isEqualTo(
                "bar01(kotlin.jvm.functions.Function2<? super java.lang.String," +
                    "? super kotlin.coroutines.Continuation<? super java.lang.String>,?>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[1]))
            .isEqualTo(
                "bar02(kotlin.jvm.functions.Function2<? super java.lang.Object," +
                    "? super kotlin.coroutines.Continuation<? super java.lang.String>,?>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[2]))
            .isEqualTo(
                "bar03(kotlin.jvm.functions.Function2<? super java.lang.String," +
                    "? super kotlin.coroutines.Continuation<? super java.lang.Object>,?>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[3]))
            .isEqualTo(
                "bar04(kotlin.jvm.functions.Function1<" +
                    "? super kotlin.coroutines.Continuation<? super java.lang.String>,?>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[4]))
            .isEqualTo(
                "bar05(kotlin.jvm.functions.Function3<? super java.lang.String," +
                    "? super java.lang.Integer," +
                    "? super kotlin.coroutines.Continuation<? super java.lang.String>,?>)"
            )

        assertThat(MetadataConverter.apiSinceMethodSignature(functions[5]))
            .isEqualTo(
                "bar06(kotlin.jvm.functions.Function2<? super java.lang.String," +
                    "? super kotlin.coroutines.Continuation<? super kotlin.Unit>,?>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[6]))
            .isEqualTo(
                "bar07(kotlin.jvm.functions.Function1<" +
                    "? super kotlin.coroutines.Continuation<? super kotlin.Unit>,?>)"
            )
    }

    @Test
    fun `apiSinceMethodSignature formats suspend function to match apiSince metadata string`() {
        val functions =
            """
            |class Foo {
            |    suspend fun bar01() {}
            |    suspend fun bar02(param: String) {}
            |    suspend fun bar03(param: (String) -> String) {}
            |    suspend fun bar04(param: suspend (String) -> String) {}
            |}
        """
                .render()
                .functions()!!

        assertThat(MetadataConverter.apiSinceMethodSignature(functions[0]))
            .isEqualTo("bar01(kotlin.coroutines.Continuation<? super kotlin.Unit>)")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[1]))
            .isEqualTo(
                "bar02(java.lang.String,kotlin.coroutines.Continuation<? super kotlin.Unit>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[2]))
            .isEqualTo(
                "bar03(kotlin.jvm.functions.Function1<? super java.lang.String," +
                    "java.lang.String>,kotlin.coroutines.Continuation<? super kotlin.Unit>)"
            )
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[3]))
            .isEqualTo(
                "bar04(kotlin.jvm.functions.Function2<? super java.lang.String," +
                    "? super kotlin.coroutines.Continuation<? super java.lang.String>,?>," +
                    "kotlin.coroutines.Continuation<? super kotlin.Unit>)"
            )
    }

    @Test
    fun `API versions for synthetic classes, top-level properties, and top-level functions`() {
        // The version metadata is generated by metalava based on the Java API
        val metadataMap =
            mapOf(
                "androidx.example.TestKt" to
                    ClassVersionMetadata(
                        className = "androidx.example.TestKt",
                        addedIn = "1.2.3",
                        methodVersions =
                            mapOf(
                                "topLevelFun()" to
                                    ClassVersionMetadata.MethodVersionMetadata(
                                        methodName = "topLevelFun()",
                                        addedIn = "1.2.3",
                                    )
                            ),
                        fieldVersions =
                            mapOf(
                                "topLevelConst" to
                                    ClassVersionMetadata.FieldVersionMetadata(
                                        fieldName = "topLevelConst",
                                        addedIn = "1.2.3",
                                    )
                            ),
                    )
            )
        val module =
            """
            |fun topLevelFun(): Unit {}
            |const val topLevelConst: Int = 2
        """
                .render()

        val metadataComponents =
            mutableListOf(
                module.metadataForProperty("topLevelProperty", versionMetadataMap = metadataMap),
                module.metadataForMethod("topLevelMethod", versionMetadataMap = metadataMap),
            )
        // In Java, the property and function will exist within a synthetic class
        if (displayLanguage == Language.JAVA) {
            metadataComponents +=
                module.metadataForClasslike(name = "TestKt", versionMetadataMap = metadataMap)
        }

        for (metadataComponent in metadataComponents) {
            val versionMetadata = metadataComponent.data.versionMetadata
            assertThat(versionMetadata).isNotNull()

            assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
            assertThat(versionMetadata.data.deprecatedIn).isNull()
        }
    }

    @Test
    fun `API version for a regular property is generated correctly`() {
        // Regular properties are represented by their accessors in the metadata
        val metadata =
            mapOf(
                "androidx.example.Foo" to
                    ClassVersionMetadata(
                        className = "androidx.example.Foo",
                        addedIn = "1.0.0",
                        methodVersions =
                            mapOf(
                                "getFoo()" to
                                    ClassVersionMetadata.MethodVersionMetadata(
                                        methodName = "getFoo()",
                                        addedIn = "1.2.3",
                                        deprecatedIn = "2.3.4",
                                    )
                            ),
                    )
            )
        val module =
            """
            |class Foo {
            |    val foo = 3
            |}
        """
                .render()

        val metadataComponent = module.metadataForProperty(versionMetadataMap = metadata)
        val versionMetadata = metadataComponent.data.versionMetadata
        assertThat(versionMetadata).isNotNull()

        assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
        assertThat(versionMetadata.data.deprecatedIn?.data?.name).isEqualTo("2.3.4")
    }

    @Test
    fun `API version for a const property is generated correctly`() {
        val metadata =
            ClassVersionMetadata(
                className = "androidx.example.Foo",
                addedIn = "1.0.0",
                fieldVersions =
                    mapOf(
                        "foo" to
                            ClassVersionMetadata.FieldVersionMetadata(
                                fieldName = "foo",
                                addedIn = "1.2.3",
                                deprecatedIn = "2.3.4",
                            )
                    ),
            )
        val metadataComponent =
            """
            |class Foo {
            |    const val foo = 3
            |}
        """
                .render()
                .metadataForProperty(versionMetadataMap = mapOf("androidx.example.Foo" to metadata))

        val versionMetadata = metadataComponent.data.versionMetadata
        assertThat(versionMetadata).isNotNull()

        assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
        assertThat(versionMetadata.data.deprecatedIn?.data?.name).isEqualTo("2.3.4")
    }

    @Test
    fun `API versions for extension functions and properties are generated correctly`() {
        // Extension functions/properties will appear as functions with receivers
        val metadata =
            mapOf(
                "androidx.example.TestKt" to
                    ClassVersionMetadata(
                        className = "androidx.example.TestKt",
                        addedIn = "1.2.3",
                        methodVersions =
                            mapOf(
                                "extensionFun(androidx.example.Foo)" to
                                    ClassVersionMetadata.MethodVersionMetadata(
                                        methodName = "extensionFun(androidx.example.Foo)",
                                        addedIn = "1.2.3",
                                    ),
                                "getExtensionVal(androidx.example.Foo)" to
                                    ClassVersionMetadata.MethodVersionMetadata(
                                        methodName = "foo",
                                        addedIn = "1.2.3",
                                    ),
                                "listExtensionFun(java.util.List<java.lang.String>)" to
                                    ClassVersionMetadata.MethodVersionMetadata(
                                        methodName =
                                            "listExtensionFun(java.util.List<java.lang.String>)",
                                        addedIn = "1.2.3",
                                    ),
                            ),
                    )
            )
        val module =
            """
            |class Foo
            |
            |fun Foo.extensionFun(): Unit {}
            |val Foo.extensionVal: Int get() = 2
            |fun List<String>.listExtensionFun() {}
        """
                .render()

        val metadataComponents =
            listOf(
                module.metadataForMethod("extensionFun", versionMetadataMap = metadata),
                module.metadataForProperty("extensionVal", versionMetadataMap = metadata),
                module.metadataForMethod("listExtensionFun", versionMetadataMap = metadata),
            )

        for (metadataComponent in metadataComponents) {
            val versionMetadata = metadataComponent.data.versionMetadata
            assertThat(versionMetadata).isNotNull()

            assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
            assertThat(versionMetadata.data.deprecatedIn).isNull()
        }
    }

    @Test
    fun `Version metadata links to release notes`() {
        val libraryMetadataMap =
            mapOf(
                "kotlin/androidx/example/Test.kt" to
                    LibraryMetadata(
                        groupId = "androidx.example",
                        artifactId = "example",
                        releaseNotesUrl = "https://d.android.com/release/example",
                    )
            )
        val versionMetadataMap =
            mapOf(
                "androidx.example.Foo" to
                    ClassVersionMetadata(
                        className = "androidx.example.Foo",
                        addedIn = "1.2.3",
                        deprecatedIn = "2.3.4",
                        methodVersions =
                            mapOf(
                                "bar()" to
                                    ClassVersionMetadata.MethodVersionMetadata(
                                        methodName = "bar()",
                                        addedIn = "1.2.3",
                                        deprecatedIn = "2.3.4",
                                    )
                            ),
                        fieldVersions =
                            mapOf(
                                "foo" to
                                    ClassVersionMetadata.FieldVersionMetadata(
                                        fieldName = "foo",
                                        addedIn = "1.2.3",
                                        deprecatedIn = "2.3.4",
                                    )
                            ),
                    )
            )

        val module =
            """
            |class Foo {
            |    const val foo = 3
            |    fun bar() {}
            |}
        """
                .render()
        val metadataComponents =
            listOf(
                module.metadataForClasslike(
                    versionMetadataMap = versionMetadataMap,
                    fileMetadataMap = libraryMetadataMap,
                ),
                module.metadataForProperty(
                    versionMetadataMap = versionMetadataMap,
                    fileMetadataMap = libraryMetadataMap,
                ),
                module.metadataForMethod(
                    versionMetadataMap = versionMetadataMap,
                    fileMetadataMap = libraryMetadataMap,
                ),
            )

        for (metadataComponent in metadataComponents) {
            val versionMetadata = metadataComponent.data.versionMetadata
            assertThat(versionMetadata).isNotNull()

            val addedIn = versionMetadata!!.data.addedIn
            assertThat(addedIn?.data?.name).isEqualTo("1.2.3")
            assertThat(addedIn?.data?.url).isEqualTo("https://d.android.com/release/example#1.2.3")

            val deprecatedIn = versionMetadata.data.deprecatedIn
            assertThat(deprecatedIn?.data?.name).isEqualTo("2.3.4")
            assertThat(deprecatedIn?.data?.url)
                .isEqualTo("https://d.android.com/release/example#2.3.4")
        }
    }

    @Test
    fun `Library metadata for a top-level function and property`() {
        val libraryMetadataMap =
            mapOf(
                "kotlin/androidx/example/Test.kt" to
                    LibraryMetadata(
                        groupId = "androidx.example",
                        artifactId = "example",
                        releaseNotesUrl = "https://d.android.com/release/example",
                    )
            )

        val module =
            """
            |const val foo = 3
            |fun bar() {}
        """
                .render()
        val (function, property) =
            if (displayLanguage == Language.JAVA) {
                val syntheticClass = module.classlike("TestKt")!!
                Pair(syntheticClass.functions.single(), syntheticClass.properties.single())
            } else {
                Pair(module.function("bar")!!, module.property("foo")!!)
            }

        val metadataComponents =
            listOf(
                module.metadata(function, fileMetadataMap = libraryMetadataMap),
                module.metadata(property, fileMetadataMap = libraryMetadataMap),
            )
        for (metadataComponent in metadataComponents) {
            val libraryMetadata = metadataComponent.data.libraryMetadata
            assertThat(libraryMetadata).isNotNull()
            assertThat(libraryMetadata!!.groupId).isEqualTo("androidx.example")
            assertThat(libraryMetadata.artifactId).isEqualTo("example")
            assertThat(libraryMetadata.releaseNotesUrl)
                .isEqualTo("https://d.android.com/release/example")
        }
    }

    @Test
    fun `Library metadata for extension function and property`() {
        val libraryMetadataMap =
            mapOf(
                "kotlin/androidx/example/Test.kt" to
                    LibraryMetadata(
                        groupId = "androidx.example",
                        artifactId = "example",
                        releaseNotesUrl = "https://d.android.com/release/example",
                    )
            )

        val module =
            """
            |val String.foo get() = 0
            |fun String.bar() = Unit
        """
                .render()

        val metadataComponents =
            listOf(
                module.metadataForMethod(fileMetadataMap = libraryMetadataMap),
                module.metadataForProperty(fileMetadataMap = libraryMetadataMap),
            )
        for (metadataComponent in metadataComponents) {
            val libraryMetadata = metadataComponent.data.libraryMetadata
            assertThat(libraryMetadata).isNotNull()
            assertThat(libraryMetadata!!.groupId).isEqualTo("androidx.example")
            assertThat(libraryMetadata.artifactId).isEqualTo("example")
            assertThat(libraryMetadata.releaseNotesUrl)
                .isEqualTo("https://d.android.com/release/example")
        }
    }

    @Test
    fun `Top level function source link`() {
        val module =
            """
            |fun bar() {}
        """
                .render()
        val function =
            if (displayLanguage == Language.JAVA) {
                module.classlike("TestKt")!!.functions.single()
            } else {
                module.function("bar")!!
            }
        val metadataComponent =
            module.metadata(
                function,
                baseFunctionSourceLink = "https://cs.android.com/search?q=file:%s+function:%s",
            )
        val sourceLinkComponent = metadataComponent.data.sourceLink
        assertThat(sourceLinkComponent).isNotNull()
        assertThat(sourceLinkComponent!!.data.url)
            .isEqualTo(
                "https://cs.android.com/search?q=file:kotlin/androidx/example/Test.kt+function:bar"
            )
    }

    @Test
    fun `Top level property source link`() {
        val module =
            """
            |const val foo = 3
        """
                .render()
        val property =
            if (displayLanguage == Language.JAVA) {
                module.classlike("TestKt")!!.properties.single()
            } else {
                module.property("foo")!!
            }
        val metadataComponent =
            module.metadata(
                property,
                basePropertySourceLink = "https://cs.android.com/search?q=file:%s+symbol:%s",
            )
        val sourceLinkComponent = metadataComponent.data.sourceLink
        assertThat(sourceLinkComponent).isNotNull()
        assertThat(sourceLinkComponent!!.data.url)
            .isEqualTo(
                "https://cs.android.com/search?q=file:kotlin/androidx/example/Test.kt+symbol:foo"
            )
    }

    @Test
    fun `Top level property accessor source link`() {
        if (displayLanguage == Language.JAVA) {
            val module =
                """
                |val foo = 3
            """
                    .render()

            val accessor = module.properties()!!.gettersAndSetters().single()
            val metadataComponent =
                module.metadata(
                    accessor,
                    baseFunctionSourceLink = "https://cs.android.com/search?q=file:%s+function:%s",
                    basePropertySourceLink = "https://cs.android.com/search?q=file:%s+symbol:%s",
                )
            val sourceLinkComponent = metadataComponent.data.sourceLink
            assertThat(sourceLinkComponent).isNotNull()
            assertThat(sourceLinkComponent!!.data.url)
                .isEqualTo(
                    "https://cs.android.com/search?q=file:kotlin/androidx/example/Test.kt+symbol:foo"
                )
        }
    }

    @Test
    fun `Source link for renamed function`() {
        val module =
            """
            |@JvmName("bar")
            |fun foo() = Unit
        """
                .render()
        val renamedFunction = module.function("foo")!!.withJvmName()

        val metadataComponent =
            module.metadata(
                renamedFunction,
                baseFunctionSourceLink = "https://cs.android.com/search?q=file:%s+function:%s",
            )
        val sourceLinkComponent = metadataComponent.data.sourceLink
        assertThat(sourceLinkComponent).isNotNull()
        assertThat(sourceLinkComponent!!.data.url)
            .isEqualTo(
                "https://cs.android.com/search?q=file:kotlin/androidx/example/Test.kt+function:foo"
            )
    }

    @Test
    fun `Source link for renamed accessor`() {
        val module =
            """
            |@get:JvmName("bar")
            |val foo = 3
        """
                .render()
        val renamedFunction = module.properties()!!.gettersAndSetters().single().withJvmName()

        val metadataComponent =
            module.metadata(
                renamedFunction,
                baseFunctionSourceLink = "https://cs.android.com/search?q=file:%s+function:%s",
                basePropertySourceLink = "https://cs.android.com/search?q=file:%s+symbol:%s",
            )
        val sourceLinkComponent = metadataComponent.data.sourceLink
        assertThat(sourceLinkComponent).isNotNull()
        assertThat(sourceLinkComponent!!.data.url)
            .isEqualTo(
                "https://cs.android.com/search?q=file:kotlin/androidx/example/Test.kt+symbol:foo"
            )
    }

    @Test
    fun `Source link for function in renamed file`() {
        val module =
            """
            |fun foo() = Unit
        """
                .render(fileUseAnnotation = "@file:JvmName(\"Foo\")")
        val function = module.function("foo")!!.withJvmName()

        val metadataComponent =
            module.metadata(
                function,
                baseFunctionSourceLink = "https://cs.android.com/search?q=file:%s+function:%s",
            )
        val sourceLinkComponent = metadataComponent.data.sourceLink
        assertThat(sourceLinkComponent).isNotNull()
        assertThat(sourceLinkComponent!!.data.url)
            .isEqualTo(
                "https://cs.android.com/search?q=file:kotlin/androidx/example/Test.kt+function:foo"
            )
    }

    @Test
    fun `No source link generated for resource classes`() {
        val module =
            testWithRootPageNode(
                listOf(
                    """
                    /src/main/java/androidx/example/R.java
                    package androidx.example;
                    public final class R {
                      public static final class attr {
                        /** Public attribute */
                        public static int publicAttribute = 0;
                      }
                    }
                    """
                )
            )
        val rClass = module.classlike("R")!!
        val rClassMetadata = module.metadata(rClass)
        assertThat(rClassMetadata.data.sourceLink).isNull()

        val attrClass = rClass.classlikes.single()
        val attrClassMetadata = module.metadata(attrClass)
        assertThat(attrClassMetadata.data.sourceLink).isNull()
    }

    private fun DModule.metadataForClasslike(
        name: String = "Foo",
        baseClassSourceLink: String? = null,
        versionMetadataMap: Map<String, ClassVersionMetadata> = emptyMap(),
        fileMetadataMap: Map<String, LibraryMetadata> = emptyMap(),
    ): MetadataComponent =
        metadata(
            documentable = classlike(name)!!,
            baseClassSourceLink = baseClassSourceLink,
            versionMetadataMap = versionMetadataMap,
            fileMetadataMap = fileMetadataMap,
        )

    private fun DModule.metadataForMethod(
        name: String = "bar",
        baseFunctionSourceLink: String? = null,
        versionMetadataMap: Map<String, ClassVersionMetadata> = emptyMap(),
        fileMetadataMap: Map<String, LibraryMetadata> = emptyMap(),
    ): MetadataComponent =
        metadata(
            documentable = function(name)!!,
            baseFunctionSourceLink = baseFunctionSourceLink,
            versionMetadataMap = versionMetadataMap,
            fileMetadataMap = fileMetadataMap,
        )

    private fun DModule.metadataForProperty(
        name: String = "foo",
        basePropertySourceLink: String? = null,
        versionMetadataMap: Map<String, ClassVersionMetadata> = emptyMap(),
        fileMetadataMap: Map<String, LibraryMetadata> = emptyMap(),
    ): MetadataComponent =
        metadata(
            documentable = property(name)!!,
            basePropertySourceLink = basePropertySourceLink,
            versionMetadataMap = versionMetadataMap,
            fileMetadataMap = fileMetadataMap,
        )

    private fun DModule.metadata(
        documentable: Documentable,
        baseClassSourceLink: String? = null,
        baseFunctionSourceLink: String? = null,
        basePropertySourceLink: String? = null,
        versionMetadataMap: Map<String, ClassVersionMetadata> = emptyMap(),
        fileMetadataMap: Map<String, LibraryMetadata> = emptyMap(),
    ): MetadataComponent {
        val converterHolder =
            ConverterHolder(
                testClass = this@MetadataConverterTest,
                module = this,
                baseClassSourceLink = baseClassSourceLink,
                baseFunctionSourceLink = baseFunctionSourceLink,
                basePropertySourceLink = basePropertySourceLink,
                versionMetadataMap = versionMetadataMap,
                fileMetadataMap = fileMetadataMap,
            )

        return when (documentable) {
            is DClasslike -> converterHolder.metadataConverter.getMetadataForClasslike(documentable)
            is DFunction -> converterHolder.metadataConverter.getMetadataForFunction(documentable)
            is DProperty -> converterHolder.metadataConverter.getMetadataForProperty(documentable)
            else -> throw RuntimeException("Cannot create metadata component for $documentable")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Language.JAVA), arrayOf(Language.KOTLIN))
    }
}
