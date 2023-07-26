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
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class MetadataConverterTest(
    private val displayLanguage: Language
) : ConverterTestBase(displayLanguage) {
    @Test
    fun `Source links are generated correctly`() {
        val metadataComponent = """
            |class Foo
        """.render().metadataForClasslike(
            baseSourceLink = "https://cs.android.com/search?q=file:%s+class:%s"
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
        val metadataComponent = """
                |class Foo
            """.render().metadataForClasslike(
            baseSourceLink = "https://cs.android.com/search?q=file:%s"
        )

        val link = metadataComponent.data.sourceLink
        assertThat(link).isNotNull()

        val expectedPath = "kotlin/androidx/example/Test.kt"
        val expected = "https://cs.android.com/search?q=file:$expectedPath"
        assertThat(link!!.data.url).isEqualTo(expected)
    }

    @Test
    fun `API version for a Class with both addedIn and deprecatedIn is generated correctly`() {
        val metadata = ClassVersionMetadata(
            className = "androidx.example.Foo",
            addedIn = "1.2.3",
            deprecatedIn = "2.3.4"
        )
        val metadataComponent = """
            |class Foo
        """.render().metadataForClasslike(
            versionMetadataMap = mapOf("androidx.example.Foo" to metadata)
        )

        val versionMetadata = metadataComponent.data.versionMetadata
        assertThat(versionMetadata).isNotNull()

        assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
        assertThat(versionMetadata.data.deprecatedIn?.data?.name).isEqualTo("2.3.4")
    }

    @Test
    fun `API version for a Class with only addedIn is generated correctly`() {
        val metadata = ClassVersionMetadata(
            className = "androidx.example.Foo",
            addedIn = "1.2.3",
            deprecatedIn = null
        )
        val metadataComponent = """
            |class Foo
        """.render().metadataForClasslike(
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
        val metadata = mapOf(
            "androidx.example.Foo" to ClassVersionMetadata(
                className = "androidx.example.Foo",
                addedIn = "1.2.3",
                fieldVersions = mapOf(
                    "Companion" to ClassVersionMetadata.FieldVersionMetadata(
                        fieldName = "Companion",
                        addedIn = "1.2.3"
                    )
                )
            ),
            "androidx.example.Foo.Companion" to ClassVersionMetadata(
                className = "androidx.example.Foo.Companion",
                addedIn = "1.2.3",
                methodVersions = mapOf(
                    "bar()" to ClassVersionMetadata.MethodVersionMetadata(
                        methodName = "bar()",
                        addedIn = "1.2.3"
                    ),
                    "getFoo()" to ClassVersionMetadata.MethodVersionMetadata(
                        methodName = "getFoo()",
                        addedIn = "1.2.3"
                    ),
                )
            )
        )
        val module = """
            |class Foo {
            |    companion object {
            |        val foo = 3
            |        fun bar(): Unit {}
            |    }
            |}
        """.render()

        val companion = module.classlike("Companion")!!
        val property = companion.properties.single()
        val function = companion.functions.single()

        val metadataComponents = listOf(
            module.metadata(companion, versionMetadataMap = metadata),
            module.metadata(property, versionMetadataMap = metadata),
            module.metadata(function, versionMetadataMap = metadata)
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
        val metadata = mapOf(
            "androidx.example.Foo" to ClassVersionMetadata(
                className = "androidx.example.Foo",
                addedIn = "1.2.3",
                methodVersions = mapOf(
                    "renamedMethod()" to ClassVersionMetadata.MethodVersionMetadata(
                        methodName = "renamedMethod()",
                        addedIn = "1.2.3"
                    ),
                    "renamedGetter()" to ClassVersionMetadata.MethodVersionMetadata(
                        methodName = "renamedGetter()",
                        addedIn = "1.2.3"
                    )
                )
            )
        )
        val module = """
            |class Foo {
            |    @JvmName("renamedMethod")
            |    fun originalMethod(): Unit {}
            |
            |    @get:JvmName("renamedGetter")
            |    val originalProperty = 3
            |}
        """.render()

        // Dackka's version of these methods won't be renamed
        val metadataComponents = listOf(
            module.metadataForMethod("originalMethod", versionMetadataMap = metadata),
            module.metadataForProperty("originalProperty", versionMetadataMap = metadata)
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
        val metadata = ClassVersionMetadata(
            className = "androidx.example.Foo",
            addedIn = "1.2.3",
            methodVersions = mapOf(
                "bar()" to ClassVersionMetadata.MethodVersionMetadata(
                    methodName = "bar()",
                    addedIn = "1.2.3",
                    deprecatedIn = "2.3.4"
                )
            )
        )
        val metadataComponent = """
            |class Foo {
            |    fun bar() {}
            |}
        """.render().metadataForMethod(
            name = "bar",
            versionMetadataMap = mapOf("androidx.example.Foo" to metadata)
        )

        val versionMetadata = metadataComponent.data.versionMetadata
        assertThat(versionMetadata).isNotNull()

        assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
        assertThat(versionMetadata.data.deprecatedIn?.data?.name).isEqualTo("2.3.4")
    }

    @Test
    fun `apiSinceMethodSignature formats methods to match apiSince metadata string`() {
        val functionsJ = """
            |public class Foo<T> {
            |    public void bar01() {}
            |    public void bar02(String param1) {}
            |    public void bar03(Object param1) {}
            |    public void bar04(T param1) {}
            |    public void bar05(int param1) {}
            |}
        """.render(java = true).functions()!!
        val functionsK = """
            |class Foo<T> {
            |    fun bar01() {}
            |    fun bar02(param1: String) {}
            |    fun bar03(param1: Any) {}
            |    fun bar04(param1: T) {}
            |    fun bar05(param1: Int) {}
            |}
        """.render().functions()!!

        for (functions in listOf(functionsJ, functionsK)) {
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[0]))
                .isEqualTo("bar01()")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[1]))
                .isEqualTo("bar02(java.lang.String)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[2]))
                .isEqualTo("bar03(java.lang.Object)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[3]))
                .isEqualTo("bar04(T)")
            assertThat(MetadataConverter.apiSinceMethodSignature(functions[4]))
                .isEqualTo("bar05(int)")
        }

        // TODO (b/292023516): add more test cases
    }

    @Test
    fun `apiSinceMethodSignature formats methods with array params to match metadata string`() {
        val functionsJ = """
            |public class Foo {
            |    public void bar01(String[] param1) {}
            |    public void bar02(int[] param1) {}
            |    public void bar03(Object[] param1) {}
            |    public void bar04(Integer[] param1) {}
            |    public void bar05(int[][] param1) {}
            |    public void bar06(String[][] param1) {}
            |}
        """.render(java = true).functions()!!
        val functionsK = """
            |class Foo {
            |    fun bar01(param1: Array<String>) {}
            |    fun bar02(param1: IntArray) {}
            |    fun bar03(param1: Array<Any>) {}
            |    fun bar04(param1: Array<Int?>) {}
            |    fun bar05(param1: Array<IntArray>) {}
            |    fun bar06(param1: Array<Array<String>>) {}
            |}
        """.render().functions()!!

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
    fun `API versions for synthetic classes, top-level properties, and top-level functions`() {
        // The version metadata is generated by metalava based on the Java API
        val metadataMap = mapOf(
            "androidx.example.TestKt" to ClassVersionMetadata(
                className = "androidx.example.TestKt",
                addedIn = "1.2.3",
                methodVersions = mapOf(
                    "topLevelFun()" to ClassVersionMetadata.MethodVersionMetadata(
                        methodName = "topLevelFun()",
                        addedIn = "1.2.3",
                    )
                ),
                fieldVersions = mapOf(
                    "topLevelConst" to ClassVersionMetadata.FieldVersionMetadata(
                        fieldName = "topLevelConst",
                        addedIn = "1.2.3"
                    )
                )
            )
        )
        val module = """
            |fun topLevelFun(): Unit {}
            |const val topLevelConst: Int = 2
        """.render()

        val metadataComponents = mutableListOf(
            module.metadataForProperty("topLevelProperty", versionMetadataMap = metadataMap),
            module.metadataForMethod("topLevelMethod", versionMetadataMap = metadataMap)
        )
        // In Java, the property and function will exist within a synthetic class
        if (displayLanguage == Language.JAVA) {
            metadataComponents += module.metadataForClasslike(
                name = "TestKt",
                versionMetadataMap = metadataMap
            )
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
        val metadata = mapOf(
            "androidx.example.Foo" to ClassVersionMetadata(
                className = "androidx.example.Foo",
                addedIn = "1.0.0",
                methodVersions = mapOf(
                    "getFoo()" to ClassVersionMetadata.MethodVersionMetadata(
                        methodName = "getFoo()",
                        addedIn = "1.2.3",
                        deprecatedIn = "2.3.4"
                    )
                )
            )
        )
        val module = """
            |class Foo {
            |    val foo = 3
            |}
        """.render()

        val metadataComponent = module.metadataForProperty(versionMetadataMap = metadata)
        val versionMetadata = metadataComponent.data.versionMetadata
        assertThat(versionMetadata).isNotNull()

        assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
        assertThat(versionMetadata.data.deprecatedIn?.data?.name).isEqualTo("2.3.4")
    }

    @Test
    fun `API version for a const property is generated correctly`() {
        val metadata = ClassVersionMetadata(
            className = "androidx.example.Foo",
            addedIn = "1.0.0",
            fieldVersions = mapOf(
                "foo" to ClassVersionMetadata.FieldVersionMetadata(
                    fieldName = "foo",
                    addedIn = "1.2.3",
                    deprecatedIn = "2.3.4"
                )
            )
        )
        val metadataComponent = """
            |class Foo {
            |    const val foo = 3
            |}
        """.render().metadataForProperty(
            versionMetadataMap = mapOf("androidx.example.Foo" to metadata)
        )

        val versionMetadata = metadataComponent.data.versionMetadata
        assertThat(versionMetadata).isNotNull()

        assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
        assertThat(versionMetadata.data.deprecatedIn?.data?.name).isEqualTo("2.3.4")
    }

    @Test
    fun `API versions for extension functions and properties are generated correctly`() {
        // Extension functions/properties will appear as functions with receivers
        val metadata = mapOf(
            "androidx.example.TestKt" to ClassVersionMetadata(
                className = "androidx.example.TestKt",
                addedIn = "1.2.3",
                methodVersions = mapOf(
                    "extensionFun(androidx.example.Foo)" to
                        ClassVersionMetadata.MethodVersionMetadata(
                            methodName = "extensionFun(androidx.example.Foo)",
                            addedIn = "1.2.3"
                        ),
                    "getExtensionVal(androidx.example.Foo)" to
                        ClassVersionMetadata.MethodVersionMetadata(
                            methodName = "foo",
                            addedIn = "1.2.3"
                        )
                )
            )
        )
        val module = """
            |class Foo
            |
            |fun Foo.extensionFun(): Unit {}
            |val Foo.extensionVal: Int get() = 2
        """.render()

        val metadataComponents = listOf(
            module.metadataForMethod("extensionFun", versionMetadataMap = metadata),
            module.metadataForProperty("extensionVal", versionMetadataMap = metadata)
        )

        for (metadataComponent in metadataComponents) {
            val versionMetadata = metadataComponent.data.versionMetadata
            assertThat(versionMetadata).isNotNull()

            assertThat(versionMetadata!!.data.addedIn?.data?.name).isEqualTo("1.2.3")
            assertThat(versionMetadata.data.deprecatedIn).isNull()
        }
    }

    private fun DModule.metadataForClasslike(
        name: String = "Foo",
        baseSourceLink: String? = null,
        versionMetadataMap: Map<String, ClassVersionMetadata> = emptyMap()
    ): MetadataComponent =
        metadata(classlike(name)!!, baseSourceLink, versionMetadataMap)

    private fun DModule.metadataForMethod(
        name: String = "bar",
        baseSourceLink: String? = null,
        versionMetadataMap: Map<String, ClassVersionMetadata> = emptyMap()
    ): MetadataComponent =
        metadata(function(name)!!, baseSourceLink, versionMetadataMap)

    private fun DModule.metadataForProperty(
        name: String = "foo",
        baseSourceLink: String? = null,
        versionMetadataMap: Map<String, ClassVersionMetadata> = emptyMap()
    ): MetadataComponent =
        metadata(property(name)!!, baseSourceLink, versionMetadataMap)

    private fun DModule.metadata(
        documentable: Documentable,
        baseSourceLink: String? = null,
        versionMetadataMap: Map<String, ClassVersionMetadata> = emptyMap()
    ): MetadataComponent {
        val (holder, _) = holderAndProvider(
            module = this,
            baseSourceLink = baseSourceLink,
            versionMetadataMap = versionMetadataMap,
        )
        val metadataConverter = MetadataConverter(holder)

        return when (documentable) {
            is DClasslike -> metadataConverter.getMetadataForClasslike(documentable)
            is DFunction -> metadataConverter.getMetadataForFunction(documentable)
            is DProperty -> metadataConverter.getMetadataForProperty(documentable)
            else -> throw RuntimeException("Cannot create metadata component for $documentable")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
