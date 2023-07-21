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
    fun `apiSinceMethodSignature formats Java method to match apiSince metadata string`() {
        val functions = """
            |public class Foo {
            |    public void bar() {}
            |    public void bar(String param1) {}
            |}
        """.render(java = true).functions()!!
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[0])).isEqualTo("bar()")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[1]))
            .isEqualTo("bar(java.lang.String)")

        // TODO (b/292023516): add more test cases
    }

    @Test
    fun `apiSinceMethodSignature formats Kotlin function to match apiSince metadata string`() {
        val functions = """
            |class Foo {
            |    fun bar() {}
            |    fun bar(param1: String) {}
            |}
        """.render().functions()!!
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[0])).isEqualTo("bar()")
        assertThat(MetadataConverter.apiSinceMethodSignature(functions[1]))
            .isEqualTo("bar(java.lang.String)")

        // TODO (b/292023516): add more test cases
    }

    @Test
    fun `API version for a synthetic class is generated correctly`() {
        val module = """
            |fun topLevelFun(): Unit {}
        """.render()

        // The version metadata is generated by metalava based on the Java API
        val metadataMap = mapOf(
            "androidx.example.TestKt" to ClassVersionMetadata(
                className = "androidx.example.TestKt",
                addedIn = "1.2.3",
                deprecatedIn = null,
                methodVersions = mapOf(
                    "topLevelFun()" to ClassVersionMetadata.MethodVersionMetadata(
                        methodName = "topLevelFun()",
                        addedIn = "1.2.3",
                        deprecatedIn = null
                    )
                )
            )
        )

        javaOnly {
            val metadataComponent = module.metadataForClasslike(
                name = "TestKt",
                versionMetadataMap = metadataMap
            )
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
