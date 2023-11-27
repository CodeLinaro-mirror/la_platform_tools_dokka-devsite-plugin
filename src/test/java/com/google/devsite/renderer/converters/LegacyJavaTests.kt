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
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DModule
import org.junit.Ignore
import org.junit.Test

/**
 * Tests derived from the sprint on old dokka.
 * These tests were originally generated when we noticed old dokka doing things wrong, fixed them,
 * and then wrote tests to verify that we had fixed them.
 * These are the legacy tests that take Java source
 *
 * These tests do not purely test dackka, they test some functionality of dokka.
 * However, that makes sense as they are not true unit tests, but format-agnostic parity tests.
 */
// TODO: get all of these tests working, and un-ignore them
internal class LegacyJavaTests : ConverterTestBase() {

    /**
     * Protected functions not showing up: This is https://github.com/Kotlin/dokka/issues/434
     */
    @Ignore
    @Test
    fun `Java functions with visibilities render correctly`() {
        val variedVisibilityTestClass = """
            public void publicBar() {}
            protected void protectedBar() {}
            private void privateBar() {}
        """
        val javaFunctions = variedVisibilityTestClass.render(java = true).functions()!!
        val kotlinFunctions = variedVisibilityTestClass.render(java = false).functions()!!

        assertThat(javaFunctions.size).isEqualTo(2)
        assertThat(javaFunctions.map { it.name })
            .containsExactlyElementsIn(listOf("publicBar", "protectedBar"))
        assertThat(kotlinFunctions.size).isEqualTo(2)
        assertThat(kotlinFunctions.map { it.name })
            .containsExactlyElementsIn(listOf("publicBar", "protectedBar"))
    }

    internal fun DModule.classlikes() = packages.flatMap { it.classlikes }
    internal fun DModule.classes() = classlikes().filterIsInstance<DClass>()
    // internal fun DModule.functions() = classes().flatMap { it -> it.functions }
    // internal fun DModule.constructors() = classes().flatMap { it -> it.constructors }
}
