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
import com.google.devsite.renderer.converters.explodedChildren
import com.google.devsite.testing.defaultPluginsConfiguration
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.Text
import org.junit.Test

class PropagatedDocTagsTransformerTest : BaseTransformerTest() {
    override val defaultConfiguration = createDokkaConfiguration(defaultPluginsConfiguration)

    @Test
    fun `documentation from java getter is propagated`() {
        testTransformer(
            """
            /src/com/sample/Foo.java
            package com.sample
            public class Foo {
                private int foo = 0;
                /** Docs on getter */
                public int getFoo() { return foo; }
            }
            """
        ) { dModule ->
            val fooProperty = dModule.packages.single().classlikes.single().properties.single()
            fooProperty.assertDocContents("Docs on getter")
            fooProperty.getter!!.assertDocContents("Docs on getter")
        }
    }

    @Test
    fun `documentation from kotlin getter is propagated`() {
        testTransformer(
            """
            /src/com/sample/Foo.kt
            package com.sample
            val foo: Int
                /** Docs on getter */
                get() = 0
            """
        ) { dModule ->
            val fooProperty = dModule.packages.single().properties.single()
            fooProperty.assertDocContents("Docs on getter")
            fooProperty.getter!!.assertDocContents("Docs on getter")
        }
    }

    @Test
    fun `documentation from java getter is not propagated if the property has docs`() {
        testTransformer(
            """
            /src/com/sample/Foo.java
            package com.sample
            public class Foo {
                /** Docs on property */
                private int foo = 0;
                /** Docs on getter */
                public int getFoo() { return foo; }
            }
            """
        ) { dModule ->
            val fooProperty = dModule.packages.single().classlikes.single().properties.single()
            fooProperty.assertDocContents("Docs on property")
            fooProperty.getter!!.assertDocContents("Docs on getter")
        }
    }

    @Test
    fun `documentation from kotlin getter is not propagated if the property has docs`() {
        testTransformer(
            """
            /src/com/sample/Foo.kt
            package com.sample
            /** Docs on property */
            val foo: Int
                /** Docs on getter */
                get() = 0
            """
        ) { dModule ->
            val fooProperty = dModule.packages.single().properties.single()
            fooProperty.assertDocContents("Docs on property")
            fooProperty.getter!!.assertDocContents("Docs on getter")
        }
    }

    @Test
    fun `documentation from getter is not propagated for inherited property`() {
        testTransformer(
            """
            /src/com/sample/FooList.kt
            package com.sample
            /** Docs on class */
            class FooList(private val list: List<String>): List<String> by list
            """
        ) { dModule ->
            val sizeProperty = dModule.packages.single().classlikes.single().properties.single()
            sizeProperty.assertDocContents("")
            // TODO(b/587389349): the class docs don't make sense on the size accessor
            sizeProperty.getter!!.assertDocContents("Docs on class")
        }
    }

    /**
     * Aggregates all [Text] content in the documentation to a string and asserts that it is equal
     * to [contents].
     */
    private fun Documentable.assertDocContents(contents: String) {
        val descriptions =
            documentation.values
                .singleOrNull()
                ?.explodedChildren
                .orEmpty()
                .filterIsInstance<Description>()
        val textTags = descriptions.flatMap { it.explodedChildren }.filterIsInstance<Text>()
        val text = textTags.joinToString("\n") { it.body }
        assertThat(text).isEqualTo(contents)
    }
}
