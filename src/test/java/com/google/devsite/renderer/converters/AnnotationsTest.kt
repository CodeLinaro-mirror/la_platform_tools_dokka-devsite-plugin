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
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.junit.Test
import kotlin.Boolean
import com.google.devsite.components.symbols.Annotation as AnnotationComponent

internal class AnnotationsTest : ConverterTestBase() {
    @Test
    fun `@Suppress annotations are ignored`() {
        val annotations = """
            |@Suppress("abc")
            |fun foo() = Unit
        """.render().functionAnnotations()

        assertThat(annotations.components()).isEmpty()
    }

    @Test
    fun `JVM annotations are ignored`() {
        val annotations = """
            |@JvmName("bar")
            |fun foo() = Unit
        """.render().functionAnnotations()

        assertThat(annotations.components()).isEmpty()
    }

    @Test
    fun `@Deprecated annotations are ignored since they are surfaced separately`() {
        val annotations = """
            |@Deprecated("So long, farewell, auf wiedersehen, goodbye")
            |fun foo() = Unit
        """.render().functionAnnotations()

        assertThat(annotations.components()).isEmpty()
    }

    @Test
    fun `@Deprecated annotation in list of Annotations is found`() {
        val annotations = """
            |@Deprecated("So long, farewell, auf wiedersehen, goodbye")
            |fun foo() = Unit
        """.render().functionAnnotations()

        assertThat(annotations.isDeprecated()).isTrue()
    }

    @Test
    fun `@Deprecated annotation in Annotation object is found`() {
        val annotation = """
            |@Deprecated("So long, farewell, auf wiedersehen, goodbye")
            |fun foo() = Unit
        """.render().functionAnnotations().first()

        assertThat(annotation.isDeprecated()).isTrue()
    }

    @Test
    fun `@Deprecated annotation in Annotation object is not found`() {
        val annotation = """
            |@FooAnnotation
            |fun foo() = Unit
        """.render().functionAnnotations().first()

        assertThat(annotation.isDeprecated()).isFalse()
    }

    @Test
    fun `Component has annotation type`() {
        val annotations = """
            |annotation class Hello
            |@Hello
            |fun foo() = Unit
        """.render().functionAnnotations()

        val annotation = annotations.components().item()

        assertThat(annotation.link().name).isEqualTo("Hello")
        assertPath(annotation.link().url, "androidx/example/Hello.html")
    }

    @Test
    fun `Method component has annotation and value in 4x Kotlin and Java`() {
        val annotationsK = """
            |annotation class Hello(val bar: String)
            |@Hello("abc")
            |@Hello(bar = "baz")
            |fun foo() = Unit
        """.render().functionAnnotations()
        val annotationsJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.METHOD)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |@Hello("abc")
            |@Hello(bar = "baz")
            |public void foo() {}
        """.render(java = true).functionAnnotations()

        for (annotations in listOf(annotationsK, annotationsJ)) {
            val annotationOne = annotations.components().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.components().last()
            val parameterTwo = annotationTwo.data.parameters.item()

            // NOTE: "value" in java does not match "bar" in kotlin
            if (annotations == annotationsK) assertThat(parameterOne.name).isEqualTo("bar")
            else assertThat(parameterOne.name).isEqualTo("value")
            assertThat(parameterOne.value).isEqualTo("\"abc\"")
            assertThat(parameterTwo.name).isEqualTo("bar")
            assertThat(parameterTwo.value).isEqualTo("\"baz\"")
        }
    }

    @Test
    fun `Property component has annotation and value in 4x Kotlin and Java`() {
        val annotationsK = """
            |annotation class Hello(val bar: String)
            |@Hello("abc")
            |@Hello(bar = "baz")
            |val foo: String = "foofoo"
        """.render().property()!!.annotations()
        val annotationsJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.FIELD)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |@Hello("abc")
            |@Hello(bar = "baz")
            |public String foo = "foofoo"
        """.render(java = true).property()!!.annotations()

        for (annotations in listOf(annotationsK, annotationsJ)) {
            val annotationOne = annotations.components().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.components().last()
            val parameterTwo = annotationTwo.data.parameters.item()

            // NOTE: "value" in java does not match "bar" in kotlin
            if (annotations == annotationsK) assertThat(parameterOne.name).isEqualTo("bar")
            else assertThat(parameterOne.name).isEqualTo("value")
            assertThat(parameterOne.value).isEqualTo("\"abc\"")
            assertThat(parameterTwo.name).isEqualTo("bar")
            assertThat(parameterTwo.value).isEqualTo("\"baz\"")
        }
    }

    @Test
    fun `Parameter component has annotation and value in 4x Kotlin and Java`() {
        val annotationsK = """
            |annotation class Hello(val bar: String)
            |fun foo(@Hello("abc") @Hello(bar = "baz") arg: String) = Unit
        """.render().function()!!.parameters.single().annotations()
        val annotationsJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.PARAMETER)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |public void foo(@Hello("abc") @Hello(bar = "baz") String arg)
        """.render(java = true).function()!!.parameters.single().annotations()

        for (annotations in listOf(annotationsK, annotationsJ)) {
            val annotationOne = annotations.components().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.components().last()
            val parameterTwo = annotationTwo.data.parameters.item()

            if (annotations == annotationsK) assertThat(parameterOne.name).isEqualTo("bar")
            else assertThat(parameterOne.name).isEqualTo("value")
            assertThat(parameterOne.value).isEqualTo("\"abc\"")
            assertThat(parameterTwo.name).isEqualTo("bar")
            assertThat(parameterTwo.value).isEqualTo("\"baz\"")
        }
    }

    @Test
    fun `Type parameter component has annotation and value in 4x Kotlin and Java`() {
        val annotationsK = """
            |annotation class Hello(val bar: String)
            |fun <@Hello("abc") @Hello(bar = "baz") T> foo(arg: String): List<T>
        """.render().function()!!.generics.single().annotations()
        val annotationsJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.TYPE_PARAMETER)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |public <@Hello("abc") @Hello(bar = "baz") T> List<T> foo()
        """.render(java = true).function()!!.generics.single().annotations()

        for (annotations in listOf(annotationsK, annotationsJ)) {
            val annotationOne = annotations.components().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.components().last()
            val parameterTwo = annotationTwo.data.parameters.item()

            if (annotations == annotationsK) assertThat(parameterOne.name).isEqualTo("bar")
            else assertThat(parameterOne.name).isEqualTo("value")
            assertThat(parameterOne.value).isEqualTo("\"abc\"")
            assertThat(parameterTwo.name).isEqualTo("bar")
            assertThat(parameterTwo.value).isEqualTo("\"baz\"")
        }
    }

    @Test
    fun `Type parameter type has annotation and value in 4x Kotlin and Java`() {
        val boundsKotlin = """
            |annotation class Hello(val bar: String)
            |fun <T : @Hello("baz") String> foo(arg: String): List<T>
        """.render().function()!!.generics.single().bounds.single() as WithExtraProperties<*>
        val wrapper = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.TYPE_USE)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |public <T extends @Hello(bar = "baz") String> List<T> foo() {
            |    return null;
            |}
        """.render(java = true).function()!!.generics.single().bounds.single() as Nullable
        val boundsJava = wrapper.inner as WithExtraProperties<*>

        for (annotations in listOf(boundsKotlin.annotations(), boundsJava.annotations())) {
            val annotationOne = annotations.components().first()
            val parameterOne = annotationOne.data.parameters.item()

            assertThat(parameterOne.name).isEqualTo("bar")
            assertThat(parameterOne.value).isEqualTo("\"baz\"")
        }
    }

    @Test
    fun `Nullability annotation is kept and discarded in Java and Kotlin as appropriate`() {
        val annotationsJ = """
            |/**
            | * Stuff
            | */
            |@Nullable
            |public String foo() {
            |   return null;
            |}
        """.render(java = true).functionAnnotations()
        val annotationsK = """
            |annotation class Nullable
            |@Nullable
            |fun foo() = Unit
        """.render().functionAnnotations()

        for (annotations in listOf(annotationsK, annotationsJ)) {
            assertThat(annotations.components()).isNotEmpty()
            assertThat(annotations.components(Language.KOTLIN)).isEmpty()
        }
    }

    @Test
    fun `Nullability annotation is found`() {
        val annotations = """
            |annotation class Nullable
            |@Nullable
            |fun foo() = Unit
        """.render().functionAnnotations()

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

    private fun DModule.functionAnnotations(): List<Annotation> {
        return function("foo")!!.annotations()
    }

    private fun List<Annotation>.components(
        language: Language = Language.JAVA,
        forcedNullable: Boolean = false
    ) = annotationComponents(pathProvider(), language, forcedNullable)

    private fun AnnotationComponent.link(): Link.Params = data.type.data
}
