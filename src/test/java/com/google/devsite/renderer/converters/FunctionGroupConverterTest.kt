/*
 * Copyright 2026 The Android Open Source Project
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
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.pages.FunctionGroupPage
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.description
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.text
import com.google.devsite.testing.ConverterTestBase
import com.google.devsite.util.ComposeProperties
import com.google.devsite.util.ComposeTestUtils
import com.google.devsite.util.composables
import com.google.devsite.util.composeModifiers
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DModule
import org.junit.Test

internal class FunctionGroupConverterTest : ConverterTestBase(Language.KOTLIN) {
    @Test
    fun `Test single function composable page`() {
        val functionDoc = "A test composable."
        val composable =
            renderCompose(
                    """
                    /**
                     * $functionDoc
                     */
                    @Composable fun TestComposable() = Unit
                    """
                        .trimIndent()
                )
                .composablePage()
        val summary = composable.summaries().single()
        val detail = composable.details().single()

        assertThat(summary.name()).isEqualTo("TestComposable")
        assertThat(summary.description.data.summary).isTrue()
        assertThat(summary.description.text()).isEqualTo(functionDoc)

        assertThat(detail.data.name).isEqualTo("TestComposable")
        assertThat((detail.data.metadata.single() as DescriptionComponent).text())
            .isEqualTo(functionDoc)

        val expectedUrl =
            "/reference/kotlin/com/example/TestComposable.composable.html#TestComposable()"
        assertThat(summary.data.description.data.signature.url()).isEqualTo(expectedUrl)
        assertThat(detail.data.signature.url()).isEqualTo(expectedUrl)
    }

    @Test
    fun `Test single function modifier page`() {
        val functionDoc = "A test modifier."
        val composable =
            renderCompose(
                    """
                    /**
                     * $functionDoc
                     */
                    fun Modifier.TestModifier() = Unit
                    """
                        .trimIndent()
                )
                .modifierPage()
        val summary = composable.summaries().single()
        val detail = composable.details().single()

        assertThat(summary.name()).isEqualTo("TestModifier")
        assertThat(summary.description.data.summary).isTrue()
        assertThat(summary.description.text()).isEqualTo(functionDoc)

        assertThat(detail.data.name).isEqualTo("TestModifier")
        assertThat((detail.data.metadata.single() as DescriptionComponent).text())
            .isEqualTo(functionDoc)

        val expectedUrl =
            "/reference/kotlin/com/example/TestModifier.modifier.html#(androidx.compose.ui.Modifier).TestModifier()"
        assertThat(summary.data.description.data.signature.url()).isEqualTo(expectedUrl)
        assertThat(detail.data.signature.url()).isEqualTo(expectedUrl)
    }

    @Test
    fun `Test ordering of functions on page`() {
        val composable =
            renderCompose(
                    """
                    /** No receiver, no parameters. */
                    @Composable fun TestComposable() = Unit
                    /** Int receiver, no parameters. */
                    @Composable fun Int.TestComposable() = Unit
                    /** No receiver, one parameter. */
                    @Composable fun TestComposable(i: Int) = Unit
                    """
                        .trimIndent()
                )
                .composablePage("TestComposable")
        val descriptions = composable.summaries().map { it.description.text() }
        assertThat(descriptions)
            .isEqualTo(
                listOf(
                    "No receiver, no parameters.",
                    "No receiver, one parameter.",
                    "Int receiver, no parameters.",
                )
            )
    }

    @Test
    fun `Test DevsitePage elements for function group`() {
        val dModule =
            renderCompose(
                """
                fun Modifier.TestModifier() = Unit
                fun Modifier.TestModifier(i: Int) = Unit
                fun Modifier.TestModifier(s: String) = Unit
                """
                    .trimIndent()
            )
        val modifier = ComposeTestUtils.testPackage(dModule).composeModifiers().single()
        val converter = ConverterHolder(this@FunctionGroupConverterTest, dModule)
        val pageData = runBlocking { converter.functionGroupConverter.devsitePage(modifier).data }

        assertThat(pageData.title).isEqualTo("TestModifier")
        assertThat(pageData.displayLanguage).isEqualTo(Language.KOTLIN)
        assertThat(pageData.pathForSwitcher).isNull()
        assertThat(pageData.metadataComponent).isNull()

        val referenceObject = pageData.referenceObject!!.data
        assertThat(referenceObject.name).isEqualTo("TestModifier")
        assertThat(referenceObject.path).isEqualTo("com.example")
        assertThat(referenceObject.language).isEqualTo(Language.KOTLIN)
        assertThat(referenceObject.properties)
            .isEqualTo(
                listOf(
                    "(androidx.compose.ui.Modifier).TestModifier()",
                    "(androidx.compose.ui.Modifier).TestModifier(kotlin.Int)",
                    "(androidx.compose.ui.Modifier).TestModifier(kotlin.String)",
                )
            )
    }

    /**
     * Creates the [FunctionGroupPage] for the composable [name] in the test package of the module.
     */
    private fun DModule.composablePage(name: String = "TestComposable"): FunctionGroupPage {
        val composable = ComposeTestUtils.testPackage(this).composables().single { it.name == name }
        return functionGroupPage(composable)
    }

    /**
     * Creates the [FunctionGroupPage] for the modifier [name] in the test package of the module.
     */
    private fun DModule.modifierPage(name: String = "TestModifier"): FunctionGroupPage {
        val modifier =
            ComposeTestUtils.testPackage(this).composeModifiers().single { it.name == name }
        return functionGroupPage(modifier)
    }

    /** Creates a [FunctionGroupPage] from the given [functionGroup]. */
    private fun DModule.functionGroupPage(
        functionGroup: ComposeProperties.DFunctionGroup
    ): FunctionGroupPage {
        val converter = ConverterHolder(this@FunctionGroupConverterTest, this@functionGroupPage)
        return runBlocking { converter.functionGroupConverter.page(functionGroup) }
    }

    companion object {
        /** Shorthand for listing all the summary table entries on the page. */
        private fun FunctionGroupPage.summaries() = data.summary.data.items

        /** Shorthand for listing all the detail sections on the page. */
        private fun FunctionGroupPage.details() = data.detail

        /** Shorthand for getting the URL component of a signature. */
        private fun FunctionSignature.url() = data.name.data.url
    }
}
