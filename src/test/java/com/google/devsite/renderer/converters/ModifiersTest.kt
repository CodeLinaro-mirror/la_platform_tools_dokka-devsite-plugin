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
import com.google.devsite.renderer.Language
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DProperty
import org.junit.Test
import kotlin.test.assertFails

internal class ModifiersTest : ConverterTestBase() {

    private val kotlinHints: ModifierHints = ModifierHints(
        Language.KOTLIN,
        isSummary = false,
        type = DFunction::class.java,
        containingType = DClass::class.java,
        isFromJava = false // There's no great way to do this. Currently only affects `const` inject
    )
    private val javaHints: ModifierHints = ModifierHints(
        Language.JAVA,
        isSummary = false,
        type = DFunction::class.java,
        containingType = DClass::class.java,
        isFromJava = true
    )

    private val dummyDoc = DModule("dummy", emptyList(), emptyMap(), null, emptySet())

    @Test
    fun `Public modifier is found`() {
        val modifiers = """
            |fun foo() = Unit
        """.render().modifierz()

        assertThat(modifiers).contains("public")
    }

    @Test
    fun `Public modifier is found in Java`() {
        val modifiers = """
            |public void foo()
        """.render(java = true).modifierz()

        assertThat(modifiers).contains("public")
        assertThat(modifiers).hasSize(1)
    }

    @Test
    fun `Multiple modifiers are found`() {
        val modifiers = """
            |class Foo {
            |    public abstract fun foo()
            |}
        """.render().modifierz()

        assertThat(modifiers).hasSize(2)
    }

    @Test
    fun `Protected modifier is found`() {
        val modifiers = """
            |abstract class Foo {
            |  protected fun foo() = Unit
            |}
        """.render().modifierz()

        assertThat(modifiers).contains("protected")
    }

    @Test
    fun `Suspend modifier is found`() {
        val modifiers = """
            |suspend fun foo() = Unit
        """.render().modifierz()

        assertThat(modifiers).contains("suspend")
    }

    @Test
    fun `Inline modifier is found`() {
        val modifiers = """
            |inline fun foo() = Unit
        """.render().modifierz()

        assertThat(modifiers).contains("inline")
    }

    @Test
    fun `Abstract modifier is found`() {
        val modifiers = """
            |abstract class Foo {
            |    abstract fun foo()
            |}
        """.render().modifierz()

        assertThat(modifiers).contains("abstract")
    }

    @Test
    fun `Open modifier is found`() {
        val modifiers = """
            |class Foo {
            |    open fun foo() = Unit
            |}
        """.render().modifierz()

        assertThat(modifiers).contains("open")
    }

    @Test
    fun `Const modifier is considered constant`() {
        val modifiers = listOf("const")

        assertThat(dummyDoc.isConstant(modifiers)).isTrue()
    }

    @Test
    fun `Static final modifiers are considered constant`() {
        val modifiers = listOf("static", "final")

        assertThat(dummyDoc.isConstant(modifiers)).isTrue()
    }

    @Test
    fun `Unknown Kotlin modifiers are stripped from Java`() {
        val modifiers = listOf(
            "suspend",
            "inline",
            "noinline",
            "reified",
            "operator",
            "override",
            "open"
        )

        assertThat(modifiers.modifiersFor(javaHints)).isEmpty()
    }

    @Test
    fun `Java static modifier is converted in Kotlin`() {
        val modifiers = listOf("static")
        // Java-static methods must be used specially in Kotlin
        // https://kotlinlang.org/docs/java-interop.html#accessing-static-members
        // TODO b/203678085: allow modifiers to be links
        assertThat(modifiers.modifiersFor(kotlinHints.copy(isFromJava = true)).single())
            .isEqualTo("java-static")
    }

    @Test
    fun `Kotlin const modifier is rewritten to static final in Java, only for properties`() {
        val modifiers = listOf("const")

        assertFails { modifiers.modifiersFor(javaHints) }

        val hints = javaHints.copy(type = DProperty::class.java)
        assertThat(modifiers.modifiersFor(hints)).containsExactly("static", "final")
    }

    @Test
    fun `Kotlin public modifier is removed in Kotlin`() {
        val modifiers = listOf("public")

        assertThat(modifiers.modifiersFor(kotlinHints)).isEmpty()
    }

    @Test
    fun `Kotlin override modifier is removed in Kotlin`() {
        val modifiers = listOf("override")

        assertThat(modifiers.modifiersFor(kotlinHints)).isEmpty()
    }

    @Test
    fun `Kotlin final modifier is removed in Kotlin`() {
        val modifiers = listOf("final")

        assertThat(modifiers.modifiersFor(kotlinHints)).isEmpty()
    }

    @Test
    fun `Kotlin final modifier is kept if an override is present in Kotlin`() {
        val modifiers = listOf("override", "final")

        assertThat(modifiers.modifiersFor(kotlinHints)).containsExactly("final")
    }

    @Test
    fun `Kotlin abstract modifier is removed if in an interface in Kotlin`() {
        val hints = kotlinHints.copy(containingType = DInterface::class.java)
        val modifiers = listOf("abstract")

        assertThat(modifiers.modifiersFor(hints)).isEmpty()
    }

    @Test
    fun `Visibility modifiers are removed in a summary`() {
        val hints = javaHints.copy(isSummary = true)
        val modifiers = listOf("public", "protected")

        assertThat(modifiers.modifiersFor(hints)).isEmpty()
    }

    private fun DModule.modifierz(): List<String> {
        return function("foo")!!.modifierz()
    }

    private fun DFunction.modifierz() = modifiers(getExpectOrCommonSourceSet())

    @Test
    fun `"default" modifier for interfaces works`() {
        val theInterface = """
            |public interface DefaultLifecycleObserver {
            |    /**
            |     * Notifies that {@code ON_CREATE} event occurred.
            |     *
            |     * @param owner the component, whose state was changed
            |     */
            |    public default String onCreate(String owner) {}
            |    public String nonDefaultMethod(String arg) {}
            |}
        """.render(java = true).classlike()!!.classlikes.single()
        val onCreateModifiers = theInterface.functions.single { it.name == "onCreate" }
            .modifierz()
        val nonDefaultModifiers = theInterface.functions.single { it.name == "nonDefaultMethod" }
            .modifierz()
        val hintsJ = javaHints.copy(containingType = DInterface::class.java)
        val hintsK = kotlinHints.copy(containingType = DInterface::class.java)
        assertThat(onCreateModifiers.modifiersFor(hintsJ).single()).isEqualTo("default")
        assertThat(nonDefaultModifiers.modifiersFor(hintsJ).single()).isEqualTo("abstract")
        assertThat(onCreateModifiers.modifiersFor(hintsK)).isEmpty()
        assertThat(nonDefaultModifiers.modifiersFor(hintsK)).isEmpty()
    }
}
