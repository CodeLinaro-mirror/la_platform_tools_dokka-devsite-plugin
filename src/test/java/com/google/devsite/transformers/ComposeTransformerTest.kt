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
import com.google.devsite.util.ComposeProperties
import com.google.devsite.util.ComposeTestUtils
import com.google.devsite.util.composables
import com.google.devsite.util.composeModifiers
import com.google.devsite.util.hasComposeProperties
import com.google.devsite.util.isForFunctionGroup
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.links.DRIExtraContainer
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage
import org.junit.Test

class ComposeTransformerTest : BaseTransformerTest() {
    override val defaultConfiguration =
        createDokkaConfiguration(createPluginsConfiguration(defaultDevsiteConfiguration))

    /**
     * Helper to run compose tests. Supplies definitions of Composable and Modifier. The [test] will
     * run on the non-compose package in the generated module.
     */
    private fun testComposeTransformer(
        source: String,
        configuration: DokkaConfigurationImpl = defaultConfiguration,
        test: (DPackage) -> Unit,
    ) {
        testTransformer(ComposeTestUtils.testFiles(source), configuration) { dModule ->
            val dPackage = ComposeTestUtils.testPackage(dModule)
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
            assertThat(composable.type).isEqualTo(ComposeProperties.COMPOSABLE_TYPE)
            assertThat(composable.name).isEqualTo("TestComposable")
            assertThat(composable.packageName).isEqualTo("com.example")
            assertThat(composable.functions).hasSize(1)
            assertThat(composable.children).isEqualTo(composable.functions)

            val dri = composable.dri
            assertThat(dri.packageName).isEqualTo("com.example")
            assertThat(dri.classNames).isEqualTo("TestComposable.composable")
            assertThat(DRIExtraContainer(dri.extra)[ComposeProperties.FunctionGroupDriExtra])
                .isNotNull()
            assertThat(dri.isForFunctionGroup()).isTrue()

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
            assertThat(modifier.type).isEqualTo(ComposeProperties.MODIFIER_TYPE)
            assertThat(modifier.name).isEqualTo("TestModifier")
            assertThat(modifier.packageName).isEqualTo("com.example")
            assertThat(modifier.functions).hasSize(1)
            assertThat(modifier.children).isEqualTo(modifier.functions)

            val dri = modifier.dri
            assertThat(dri.packageName).isEqualTo("com.example")
            assertThat(dri.classNames).isEqualTo("TestModifier.modifier")
            assertThat(DRIExtraContainer(dri.extra)[ComposeProperties.FunctionGroupDriExtra])
                .isNotNull()

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
            assertThat(composable.children).isEqualTo(composable.functions)
            assertThat(ComposeProperties.isComposable(composable.functions.single())).isTrue()
            assertThat(ComposeProperties.isModifier(composable.functions.single())).isFalse()

            val modifier = dPackage.composeModifiers().single()
            assertThat(modifier.name).isEqualTo("TestModifier")
            assertThat(modifier.packageName).isEqualTo("com.example")
            assertThat(modifier.functions).hasSize(1)
            assertThat(modifier.children).isEqualTo(modifier.functions)
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
            assertThat(composable.children).isEqualTo(composable.functions)

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
    fun `Test documentation of function group`() {
        testComposeTransformer(
            """
            /** Documentation for second composable. */
            @Composable fun TestComposable(i: Int) = Unit
            /** Documentation for first composable. */
            @Composable fun TestComposable() = Unit
            /** Documentation for third composable. */
            @Composable fun Int.TestComposable() = Unit
            """
        ) { dPackage ->
            val composable = dPackage.composables().single()
            // Don't test the exact structure of the Documentation object, just verify that it
            // contains the correct text somewhere in it.
            val documentationAsString = composable.documentation.toString()
            assertThat(documentationAsString).contains("Documentation for first composable.")
            assertThat(documentationAsString).doesNotContain("Documentation for second composable.")
            assertThat(documentationAsString).doesNotContain("Documentation for third composable.")
        }
    }

    @Test
    fun `Test source sets for non-KMP function group`() {
        testComposeTransformer(
            """
            @Composable fun TestComposable(i: Int) = Unit
            @Composable fun TestComposable() = Unit
            @Composable fun Int.TestComposable() = Unit
            """
        ) { dPackage ->
            val composable = dPackage.composables().single()
            assertThat(composable.sourceSets).hasSize(1)
            assertThat(composable.sourceSets.single().analysisPlatform.name).isEqualTo("jvm")
            assertThat(composable.expectPresentInSet).isNull()
        }
    }

    @Test
    fun `Test KMP function groups`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    name = "commonMain"
                    displayName = "commonMain"
                    sourceRoots = listOf("src/main")
                    analysisPlatform = "common"
                }
                sourceSet {
                    name = "jvmMain"
                    displayName = "jvmMain"
                    sourceRoots = listOf("src/jvmMain")
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(DokkaSourceSetID("root", "commonMain"))
                }
                sourceSet {
                    name = "nativeMain"
                    displayName = "nativeMain"
                    sourceRoots = listOf("src/nativeMain")
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(DokkaSourceSetID("root", "commonMain"))
                }
            }
            pluginsConfigurations = createPluginsConfiguration(defaultDevsiteConfiguration)
        }

        // This is appended to a stub created through ComposeTestUtils, so the package declaration
        // and imports don't need to be repeated here.
        val commonSource =
            """
            expect fun Modifier.WithExpects()
            expect fun Modifier.WithExpects(i: Int)

            expect fun Modifier.WithExpectAndNonExpect()

            fun Modifier.NoExpects() = Unit
            """
                .trimIndent()
        val jvmSource =
            """
            /src/jvmMain/com/example/Foo_jvm.kt
            package com.example
            import androidx.compose.ui.Modifier

            actual fun Modifier.WithExpects() = Unit
            actual fun Modifier.WithExpects(i: Int) = Unit

            actual fun Modifier.WithExpectAndNonExpect() = Unit

            fun Modifier.NoExpects(i: Int) = Unit

            fun Modifier.JvmOnly() = Unit

            fun Modifier.JvmAndNative() = Unit
            """
                .trimIndent()
        val nativeSource =
            """
            /src/nativeMain/com/example/Foo_native.kt
            package com.example
            import androidx.compose.ui.Modifier

            actual fun Modifier.WithExpects() = Unit
            actual fun Modifier.WithExpects(i: Int) = Unit

            actual fun Modifier.WithExpectAndNonExpect() = Unit
            // Non expect/actual version
            fun Modifier.WithExpectAndNonExpect(i: Int) = Unit

            fun Modifier.NoExpects(s: String) = Unit

            fun Modifier.NativeOnly() = Unit
            fun Modifier.NativeOnly(i: Int) = Unit

            fun Modifier.JvmAndNative(i: Int) = Unit
            """
                .trimIndent()

        testComposeTransformer(
            commonSource + "\n\n" + jvmSource + "\n\n" + nativeSource,
            configuration = configuration,
        ) { dPackage ->
            // All functions in the group are expect/actuals
            val withExpects = dPackage.composeModifiers().single { it.name == "WithExpects" }
            // Each `actual` is part of the same `DFunction` as the corresponding `expect`
            assertThat(withExpects.functions).hasSize(2)
            assertThat(withExpects.sourceSets.map { it.displayName })
                .containsExactly("commonMain", "jvmMain", "nativeMain")
            assertThat(withExpects.expectPresentInSet).isNotNull()
            assertThat(withExpects.expectPresentInSet!!.displayName).isEqualTo("commonMain")

            // There are both expect/actuals and non-expect/actuals in the group
            val withExpectAndNonExpect =
                dPackage.composeModifiers().single { it.name == "WithExpectAndNonExpect" }
            // Each `actual` is part of the same `DFunction` as the corresponding `expect`, there is
            // one non-expect/actual in nativeMain
            assertThat(withExpectAndNonExpect.functions).hasSize(2)
            assertThat(withExpectAndNonExpect.sourceSets.map { it.displayName })
                .containsExactly("commonMain", "jvmMain", "nativeMain")
            // Not all functions in the group have an expect
            assertThat(withExpectAndNonExpect.expectPresentInSet).isNull()

            val noExpects = dPackage.composeModifiers().single { it.name == "NoExpects" }
            assertThat(noExpects.functions).hasSize(3)
            assertThat(noExpects.sourceSets.map { it.displayName })
                .containsExactly("commonMain", "jvmMain", "nativeMain")
            assertThat(noExpects.expectPresentInSet).isNull()

            val jvmOnly = dPackage.composeModifiers().single { it.name == "JvmOnly" }
            assertThat(jvmOnly.functions).hasSize(1)
            assertThat(jvmOnly.sourceSets.map { it.displayName }).containsExactly("jvmMain")
            assertThat(jvmOnly.expectPresentInSet).isNull()

            val nativeOnly = dPackage.composeModifiers().single { it.name == "NativeOnly" }
            assertThat(nativeOnly.functions).hasSize(2)
            assertThat(nativeOnly.sourceSets.map { it.displayName }).containsExactly("nativeMain")
            assertThat(nativeOnly.expectPresentInSet).isNull()

            val jvmAndNative = dPackage.composeModifiers().single { it.name == "JvmAndNative" }
            assertThat(jvmAndNative.functions).hasSize(2)
            assertThat(jvmAndNative.sourceSets.map { it.displayName })
                .containsExactly("jvmMain", "nativeMain")
            assertThat(jvmAndNative.expectPresentInSet).isNull()

            assertThat(dPackage.composeModifiers()).hasSize(6)
        }
    }
}
