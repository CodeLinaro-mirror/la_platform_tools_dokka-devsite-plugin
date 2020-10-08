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
import com.google.devsite.components.Link
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations.Annotation
import org.jetbrains.dokka.model.DModule
import org.junit.Ignore
import org.junit.Test
import kotlin.Boolean
import com.google.devsite.components.symbols.Annotation as AnnotationComponent

internal class AnnotationsTest : ConverterTestBase() {
    @Test
    fun `@Suppress annotations are ignored`() {
        val annotations = """
            |@Suppress("abc")
            |fun foo() = Unit
        """.render().annotations()

        assertThat(annotations.components()).isEmpty()
    }

    @Ignore // TODO(b/168667502): why isn't this on the classpath?
    @Test
    fun `JVM annotations are ignored`() {
        val annotations = """
            |@JvmName("bar")
            |fun foo() = Unit
        """.render().annotations()

        assertThat(annotations.components()).isEmpty()
    }

    @Test
    fun `@Deprecated annotations are ignored since they are surfaced separately`() {
        val annotations = """
            |@Deprecated("So long, farewell, auf wiedersehen, goodbye")
            |fun foo() = Unit
        """.render().annotations()

        assertThat(annotations.components()).isEmpty()
    }

    @Test
    fun `@Deprecated annotation in list of Annotations is found`() {
        val annotations = """
            |@Deprecated("So long, farewell, auf wiedersehen, goodbye")
            |fun foo() = Unit
        """.render().annotations()

        assertThat(annotations.isDeprecated()).isTrue()
    }

    @Test
    fun `@Deprecated annotation in Annotation object is found`() {
        val annotation = """
            |@Deprecated("So long, farewell, auf wiedersehen, goodbye")
            |fun foo() = Unit
        """.render().annotations().first()

        assertThat(annotation.isDeprecated()).isTrue()
    }

    @Test
    fun `@Deprecated annotation in Annotation object is not found`() {
        val annotation = """
            |@FooAnnotation
            |fun foo() = Unit
        """.render().annotations().first()

        assertThat(annotation.isDeprecated()).isFalse()
    }

    @Test
    fun `Component has annotation type`() {
        val annotations = """
            |annotation class Hello
            |@Hello
            |fun foo() = Unit
        """.render().annotations()

        val annotation = annotations.components().item()

        assertThat(annotation.link().name).isEqualTo("Hello")
        assertPath(annotation.link().url, "androidx/example/Hello.html")
    }

    @Test
    fun `Component has annotation value`() {
        val annotations = """
            |annotation class Hello(val foo: String)
            |@Hello("abc")
            |fun foo() = Unit
        """.render().annotations()

        val annotation = annotations.components().item()
        val parameter = annotation.data.parameters.item()

        assertThat(parameter.name).isEqualTo("foo")
        assertThat(parameter.value).isEqualTo("\"abc\"")
    }

    @Test
    fun `Nullability annotation is kept in Java`() {
        val annotations = """
            |annotation class Nullable
            |@Nullable
            |fun foo() = Unit
        """.render().annotations()

        assertThat(annotations.components()).isNotEmpty()
    }

    @Test
    fun `Nullability annotation is removed in Kotlin`() {
        val annotations = """
            |annotation class Nullable
            |@Nullable
            |fun foo() = Unit
        """.render().annotations()

        assertThat(annotations.components(Language.KOTLIN)).isEmpty()
    }

    @Test
    fun `Nullability annotation is found`() {
        val annotations = """
            |annotation class Nullable
            |@Nullable
            |fun foo() = Unit
        """.render().annotations()

        assertThat(annotations.isNullable()).isTrue()
    }

    @Test
    fun `Nullability annotation is injected for nullable type in Java`() {
        val annotations = emptyList<Annotation>().components(Language.JAVA, forcedNullable = true)

        assertThat(annotations).isNotEmpty()
    }

    @Test
    fun `Nullability annotation isn't doubly injected for nullable type`() {
        val annotations = listOf(
            Annotation(DRI("androidx.annotation", "Nullable"), emptyMap())
        ).components(forcedNullable = true)

        assertThat(annotations).hasSize(1)
    }

    @Test
    fun `Nullability annotation is NOT injected for nullable type in Kotlin`() {
        val annotations = emptyList<Annotation>().components(Language.KOTLIN, forcedNullable = true)

        assertThat(annotations).isEmpty()
    }

    private fun DModule.annotations(): List<Annotation> {
        val packageDoc = packages.single()
        val function = packageDoc.functions.singleOrNull()
            ?: packageDoc.classlikes.single().functions.single { it.name == "foo" }

        return function.annotations()
    }

    private fun List<Annotation>.components(
        language: Language = Language.JAVA,
        forcedNullable: Boolean = false
    ) = annotationComponents(pathProvider(), language, forcedNullable)

    private fun AnnotationComponent.link(): Link.Params = data.type.data
}
