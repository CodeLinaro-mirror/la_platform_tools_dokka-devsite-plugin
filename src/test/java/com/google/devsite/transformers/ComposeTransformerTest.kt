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

package com.google.devsite.transformers

import com.google.common.truth.Truth.assertThat
import com.google.devsite.testing.createPluginsConfiguration
import com.google.devsite.testing.defaultDevsiteConfiguration
import com.google.devsite.testing.defaultPluginsConfiguration
import com.google.devsite.util.ComposeProperties
import com.google.devsite.util.composables
import com.google.devsite.util.composeModifiers
import com.google.devsite.util.hasComposeProperties
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage
import org.junit.Test

class ComposeTransformerTest : BaseTransformerTest() {
    override val defaultConfiguration =
        createDokkaConfiguration(
            createPluginsConfiguration(
                defaultDevsiteConfiguration.copy(applyComposeTransformer = true)
            )
        )

    private val composeStubs =
        """
        /src/androidx/compose/runtime/Composable.kt
        package androidx.compose.runtime
        annotation class Composable

        /src/androidx/compose/ui/Modifier.kt
        package androidx.compose.ui
        class Modifier
        """
            .trimIndent()

    /**
     * Helper to run compose tests. Supplies definitions of Composable and Modifier. The [test] will
     * run on the non-compose package in the generated module.
     */
    private fun testComposeTransformer(
        source: String,
        configuration: DokkaConfigurationImpl = defaultConfiguration,
        test: (DPackage) -> Unit,
    ) {
        val testFileHeader =
            """
            /src/com/example/Foo.kt
            package com.example
            import androidx.compose.ui.Modifier
            import androidx.compose.runtime.Composable
            """
                .trimIndent()
        val testFile = testFileHeader + "\n$source"
        val completeSource = composeStubs + "\n\n" + testFile
        testTransformer(completeSource, configuration) { dModule ->
            val dPackage = dModule.packages.single { it.name == "com.example" }
            test(dPackage)
        }
    }

    @Test
    fun `Test no change with no compose functions`() {
        testComposeTransformer(
            """
            class Foo
            """
                .trimIndent()
        ) { dPackage ->
            assertThat(dPackage.hasComposeProperties()).isFalse()
            assertThat(dPackage.composables()).isEmpty()
            assertThat(dPackage.composeModifiers()).isEmpty()
            assertThat(dPackage.functions).isEmpty()
        }
    }

    @Test
    fun `Test with composable`() {
        testComposeTransformer(
            """
            @Composable fun TestComposable() = Unit
            """
                .trimIndent()
        ) { dPackage ->
            assertThat(dPackage.hasComposeProperties()).isTrue()
            assertThat(dPackage.composeModifiers()).isEmpty()
            assertThat(dPackage.functions).hasSize(1)

            val composable = dPackage.composables().single()
            assertThat(composable.name).isEqualTo("TestComposable")
            assertThat(composable.packageName).isEqualTo("com.example")
            assertThat(composable.functions).hasSize(1)

            assertThat(ComposeProperties.isComposable(composable.functions.single())).isTrue()
            assertThat(ComposeProperties.isModifier(composable.functions.single())).isFalse()
        }
    }

    @Test
    fun `Test with modifier`() {
        testComposeTransformer(
            """
            fun Modifier.TestModifier() = Unit
            """
                .trimIndent()
        ) { dPackage ->
            assertThat(dPackage.hasComposeProperties()).isTrue()
            assertThat(dPackage.composables()).isEmpty()
            assertThat(dPackage.functions).hasSize(1)

            val modifier = dPackage.composeModifiers().single()
            assertThat(modifier.name).isEqualTo("TestModifier")
            assertThat(modifier.packageName).isEqualTo("com.example")
            assertThat(modifier.functions).hasSize(1)

            assertThat(ComposeProperties.isModifier(modifier.functions.single())).isTrue()
            assertThat(ComposeProperties.isComposable(modifier.functions.single())).isFalse()
        }
    }

    @Test
    fun `Test combination of compose and other functions`() {
        testComposeTransformer(
            """
            @Composable fun TestComposable() = Unit
            fun Modifier.TestModifier() = Unit
            fun nonComposeFunction() = Unit
            """
                .trimIndent()
        ) { dPackage ->
            assertThat(dPackage.hasComposeProperties()).isTrue()
            assertThat(dPackage.functions).hasSize(3)

            val composable = dPackage.composables().single()
            assertThat(composable.name).isEqualTo("TestComposable")
            assertThat(composable.packageName).isEqualTo("com.example")
            assertThat(composable.functions).hasSize(1)
            assertThat(ComposeProperties.isComposable(composable.functions.single())).isTrue()
            assertThat(ComposeProperties.isModifier(composable.functions.single())).isFalse()

            val modifier = dPackage.composeModifiers().single()
            assertThat(modifier.name).isEqualTo("TestModifier")
            assertThat(modifier.packageName).isEqualTo("com.example")
            assertThat(modifier.functions).hasSize(1)
            assertThat(ComposeProperties.isModifier(modifier.functions.single())).isTrue()
            assertThat(ComposeProperties.isComposable(modifier.functions.single())).isFalse()
        }
    }

    @Test
    fun `Test ordering of functions in group`() {
        fun DFunction.signature(): String {
            val receiverString = dri.callable?.receiver?.let { "$it." } ?: ""
            val parameters = dri.callable?.params?.joinToString() ?: ""
            return "$receiverString$name($parameters)"
        }

        testComposeTransformer(
            """
            @Composable fun TestComposable() = Unit
            @Composable fun String.TestComposable() = Unit
            @Composable fun TestComposable(i: Int) = Unit
            @Composable fun TestComposable(i: Int, s: String) = Unit
            @Composable fun Int.TestComposable() = Unit
            @Composable fun Int.TestComposable(s: String) = Unit
            @Composable fun TestComposable(s: String) = Unit
            """
                .trimIndent()
        ) { dPackage ->
            assertThat(dPackage.hasComposeProperties()).isTrue()
            assertThat(dPackage.composeModifiers()).isEmpty()
            assertThat(dPackage.functions).hasSize(7)

            val composable = dPackage.composables().single()
            assertThat(composable.name).isEqualTo("TestComposable")
            assertThat(composable.packageName).isEqualTo("com.example")
            assertThat(composable.functions).hasSize(7)

            // Ordering: by receiver (no receiver first), then number of parameters, then parameter
            // type strings.
            assertThat(composable.functions.map { it.signature() })
                .isEqualTo(
                    listOf(
                        "TestComposable()",
                        "TestComposable(kotlin.Int)",
                        "TestComposable(kotlin.String)",
                        "TestComposable(kotlin.Int, kotlin.String)",
                        "kotlin.Int.TestComposable()",
                        "kotlin.Int.TestComposable(kotlin.String)",
                        "kotlin.String.TestComposable()",
                    )
                )
        }
    }

    @Test
    fun `Test ordering of group`() {
        testComposeTransformer(
            """
            fun Modifier.TestModifierB() = Unit
            fun Modifier.TestModifierA() = Unit
            fun Modifier.TestModifierC() = Unit
            """
                .trimIndent()
        ) { dPackage ->
            assertThat(dPackage.hasComposeProperties()).isTrue()
            assertThat(dPackage.composables()).isEmpty()
            assertThat(dPackage.functions).hasSize(3)

            val modifiers = dPackage.composeModifiers()
            assertThat(modifiers.size).isEqualTo(3)
            for (modifier in modifiers) {
                assertThat(modifier.packageName).isEqualTo("com.example")
                assertThat(modifier.functions).hasSize(1)
            }
            // Ordering: by name.
            assertThat(modifiers.map { it.name })
                .isEqualTo(listOf("TestModifierA", "TestModifierB", "TestModifierC"))
        }
    }

    @Test
    fun `Test non-top-level compose functions`() {
        testComposeTransformer(
            """
            class Foo {
                @Composable fun TestComposable() = Unit
                fun Modifier.TestModifierB() = Unit
            }
            """
                .trimIndent()
        ) { dPackage ->
            assertThat(dPackage.hasComposeProperties()).isFalse()
            assertThat(dPackage.composables()).isEmpty()
            assertThat(dPackage.composeModifiers()).isEmpty()
            assertThat(dPackage.functions).isEmpty()

            // Only top-level functions count as composables/modifiers.
            val fooClass = dPackage.classlikes.single()
            assertThat(fooClass.name).isEqualTo("Foo")
            assertThat(fooClass.functions).hasSize(2)

            for (dFunction in fooClass.functions) {
                assertThat(ComposeProperties.isComposable(dFunction)).isFalse()
                assertThat(ComposeProperties.isModifier(dFunction)).isFalse()
            }
        }
    }

    @Test
    fun `Test transformer does not apply when disabled`() {
        testComposeTransformer(
            """
            @Composable fun TestComposable() = Unit
            fun Modifier.TestModifier() = Unit
            fun nonComposeFunction() = Unit
            """
                .trimIndent(),
            configuration = createDokkaConfiguration(defaultPluginsConfiguration),
        ) { dPackage ->
            assertThat(dPackage.hasComposeProperties()).isFalse()
            assertThat(dPackage.composables()).isEmpty()
            assertThat(dPackage.composeModifiers()).isEmpty()
            assertThat(dPackage.functions).hasSize(3)
        }
    }
}
