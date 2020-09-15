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
import org.jetbrains.dokka.model.DModule
import org.junit.Ignore
import org.junit.Test

internal class ModifiersTest : ConverterTestBase() {
    @Test
    fun `Public modifier is found`() {
        val modifiers = """
            |fun foo() = Unit
        """.render().modifierz()

        assertThat(modifiers).contains("public")
    }

    @Ignore // TODO(b/165112358): foo doesn't show up in the dokka model
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

        assertThat(isConstant(modifiers)).isTrue()
    }

    @Test
    fun `Static final modifiers are considered constant`() {
        val modifiers = listOf("static", "final")

        assertThat(isConstant(modifiers)).isTrue()
    }

    @Test
    fun `Unknown Kotlin modifiers are stripped from Java`() {
        val hints = ModifierHints(Language.JAVA)
        val modifiers = listOf(
            "suspend",
            "inline",
            "noinline",
            "reified",
            "operator",
            "override",
            "open"
        )

        assertThat(modifiers.modifiersFor(hints)).isEmpty()
    }

    @Test
    fun `Unknown Java modifiers are stripped from Kotlin`() {
        val hints = ModifierHints(Language.KOTLIN)
        val modifiers = listOf("static")

        assertThat(modifiers.modifiersFor(hints)).isEmpty()
    }

    @Test
    fun `Kotlin const modifier is rewritten to static final in Java`() {
        val hints = ModifierHints(Language.JAVA)
        val modifiers = listOf("const")

        assertThat(modifiers.modifiersFor(hints)).containsExactly("static", "final")
    }

    @Test
    fun `Kotlin public modifier is removed in Kotlin`() {
        val hints = ModifierHints(Language.KOTLIN)
        val modifiers = listOf("public")

        assertThat(modifiers.modifiersFor(hints)).isEmpty()
    }

    @Test
    fun `Kotlin override modifier is removed in Kotlin`() {
        val hints = ModifierHints(Language.KOTLIN)
        val modifiers = listOf("override")

        assertThat(modifiers.modifiersFor(hints)).isEmpty()
    }

    @Test
    fun `Kotlin final modifier is removed in Kotlin`() {
        val hints = ModifierHints(Language.KOTLIN)
        val modifiers = listOf("final")

        assertThat(modifiers.modifiersFor(hints)).isEmpty()
    }

    @Test
    fun `Kotlin final modifier is kept if an override is present in Kotlin`() {
        val hints = ModifierHints(Language.KOTLIN)
        val modifiers = listOf("override", "final")

        assertThat(modifiers.modifiersFor(hints)).containsExactly("final")
    }

    @Test
    fun `Kotlin abstract modifier is removed if in an interface in Kotlin`() {
        val hints = ModifierHints(Language.KOTLIN, isInterface = true)
        val modifiers = listOf("abstract")

        assertThat(modifiers.modifiersFor(hints)).isEmpty()
    }

    @Test
    fun `Visibility modifiers are removed in a summary`() {
        val hints = ModifierHints(Language.JAVA, isSummary = true)
        val modifiers = listOf("public", "protected")

        assertThat(modifiers.modifiersFor(hints)).isEmpty()
    }

    private fun DModule.modifierz(): List<String> {
        val packageDoc = packages.single()
        val function = packageDoc.functions.singleOrNull()
            ?: packageDoc.classlikes.single().functions.single { it.name == "foo" }

        return function.modifiers()
    }
}
