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

package com.google.devsite.components.impl

import com.google.common.truth.Truth
import com.google.devsite.components.symbols.ClassSignature
import com.google.devsite.components.testing.NoopLink
import com.google.devsite.renderer.Language
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultClassSignatureTest {

    @Test
    fun `Class signature renders correctly in Java`() {
        val component = DefaultClassSignature(ClassSignature.Params(
            Language.JAVA,
            name = "Foo",
            type = "class",
            modifiers = listOf("public", "abstract"),
            extends = listOf(NoopLink("Anyclass")),
            implements = listOf(NoopLink("SomeInterface")))
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        Truth.assertThat(output).isEqualTo(
            """
<body>
  <pre>public abstract class Foo extends Anyclass implements SomeInterface</pre>
</body>
            """.trim()
        )
    }

    @Test
    fun `Class signature renders correctly in Kotlin`() {
        val component = DefaultClassSignature(ClassSignature.Params(
            Language.KOTLIN,
            name = "Foo",
            type = "class",
            modifiers = listOf("open"),
            extends = listOf(NoopLink("Anyclass")),
            implements = listOf(NoopLink("SomeInterface")))
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        Truth.assertThat(output).isEqualTo(
            """
<body>
  <pre>open class Foo : Anyclass, SomeInterface</pre>
</body>
            """.trim()
        )
    }
}
