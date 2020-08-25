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
import com.google.devsite.components.impl.UndocumentedSymbolDescription
import com.google.devsite.renderer.Language
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class DocTagConverterTest(
    private val language: Language
) : ConverterTestBase(language) {
    @Test
    fun `Empty description isn't documented`() {
        val source = """
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val description = converter.summaryDescription(root.doc())

            assertThat(description.javaClass)
                .isAssignableTo(UndocumentedSymbolDescription::class.java)
        }
    }

    @Test
    fun `Basic summary description has correct flags`() {
        val source = """
            |/** Hello World! */
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val description = converter.summaryDescription(root.doc())

            assertThat(description.data.summary).isTrue()
            assertThat(description.data.deprecation).isNull()
        }
    }

    @Ignore // TODO(b/166333285): support deprecation
    @Test
    fun `Deprecated summary description has correct flags`() {
        val source = """
            |@Deprecated("Bye")
            |class Foo
        """.trimMargin()

        testWithRootPageNode(source) t@{ root ->
            val converter = DocTagConverter(language, pathProvider())

            val description = converter.summaryDescription(root.doc())

            assertThat(description.data.summary).isTrue()
            assertThat(description.data.deprecation).isEqualTo("Bye")
        }
    }

    private fun RootPageNode.doc(): Documentable {
        val packageDoc = children
            .filterIsInstance<PackagePageNode>().single()
            .documentable as DPackage

        return packageDoc.classlikes.single()
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
