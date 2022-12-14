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
import com.google.devsite.components.symbols.AnnotationComponent
import com.google.devsite.components.symbols.NamedValueAnnotationParameter
import com.google.devsite.components.symbols.name
import com.google.devsite.components.symbols.value
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.Language.JAVA
import com.google.devsite.renderer.Language.KOTLIN
import com.google.devsite.renderer.converters.testing.exceptNonNull
import com.google.devsite.renderer.converters.testing.isAtNonNull
import com.google.devsite.renderer.converters.testing.isAtNullable
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations.Annotation
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DModule
import org.junit.Test

internal class AnnotationsTest : ConverterTestBase() {
    @Test
    fun `@Suppress annotations are ignored`() {
        val annotations = """
            |annotation class SuppressLint(val bar: String = "This is part of Lint, not Kotlin")
            |@Suppress("abc")
            |@SuppressLint("123")
            |@SuppressWarnings("do re mi")
            |fun foo() = Unit
        """.render().functionAnnotations()

        assertThat(annotations.components().exceptNonNull()).isEmpty()
    }

    @Test
    fun `JVM annotations are ignored`() {
        val annotations = """
            |@JvmName("bar")
            |fun foo() = Unit
        """.render().functionAnnotations()

        assertThat(annotations.components().exceptNonNull()).isEmpty()
    }

    @Test
    fun `Annotations in the do-not-document list are not displayed`() {
        val annotation = """
            |@Override
            |fun foo() = Unit
        """.render().functionAnnotations()

        // When no annotations are hidden, Override is displayed
        assertThat(annotation.components().exceptNonNull().single().name).isEqualTo("Override")

        // Override is not displayed when hidden
        val hidden = setOf("java.lang.Override")
        assertThat(annotation.components(hiddenAnnotations = hidden).exceptNonNull()).isEmpty()
    }

    @Test
    fun `@Deprecated annotations are ignored since they are surfaced separately`() {
        val annotations = """
            |@Deprecated("So long, farewell, auf wiedersehen, goodbye")
            |fun foo() = Unit
        """.render().functionAnnotations()

        assertThat(annotations.components().exceptNonNull()).isEmpty()
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

        val annotation = annotations.components().exceptNonNull().item()

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
            val annotationOne = annotations.components(isFromJava = annotations == annotationsJ)
                .exceptNonNull().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.components(isFromJava = annotations == annotationsJ)
                .exceptNonNull().last()
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
        """.render().property()!!.allAnnotations()
        val annotationsJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.FIELD)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |@Hello("abc")
            |@Hello(bar = "baz")
            |public String foo = "foofoo"
        """.render(java = true).property()!!.allAnnotations()

        for (annotations in listOf(annotationsK, annotationsJ)) {
            val annotationOne = annotations.components(isFromJava = annotations == annotationsJ)
                .exceptNonNull().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.components(isFromJava = annotations == annotationsJ)
                .exceptNonNull().last()
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
        """.render().function()!!.parameters.single().allAnnotations()
        val annotationsJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.PARAMETER)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |public void foo(@Hello("abc") @Hello(bar = "baz") @NonNull String arg)
        """.render(java = true).function()!!.parameters.single().allAnnotations()

        for (annotations in listOf(annotationsK, annotationsJ)) {
            val annotationOne = annotations.components(isFromJava = annotations == annotationsJ)
                .exceptNonNull().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.components(isFromJava = annotations == annotationsJ)
                .exceptNonNull().last()
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
        """.render().function()!!.generics.single().allAnnotations()
        val annotationsJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.TYPE_PARAMETER)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |public <@Hello("abc") @Hello(bar = "baz") T> java.util.List<T> foo()
        """.render(java = true).function()!!.generics.single().allAnnotations()

        for (annotations in listOf(annotationsK, annotationsJ)) {
            val annotationOne = annotations.components(isFromJava = annotations == annotationsJ)
                .exceptNonNull().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.components(isFromJava = annotations == annotationsJ)
                .exceptNonNull().last()
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
        fun DFunction.genericBoundsAnnotations() =
            generics.single().bounds.single().annotations(getExpectOrCommonSourceSet())
        val functionKotlin = """
            |annotation class Hello(val bar: String)
            |fun <T : @Hello("baz") String> foo(arg: String): List<T>
        """.render().function()!!
        val functionJava = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.TYPE_USE)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |public <T extends @Hello(bar = "baz") String> java.util.List<T> foo() {
            |    return null;
            |}
        """.render(java = true).function()!!

        for (function in listOf(functionJava, functionKotlin)) {
            val annotations = function.genericBoundsAnnotations()
            val annotationOne = annotations.components(isFromJava = function == functionJava)
                .first()
            val parameterOne = annotationOne.data.parameters.item()
            assertThat(parameterOne.name).isEqualTo("bar")
            assertThat(parameterOne.value).isEqualTo("\"baz\"")
        }
    }

    @Test
    fun `Nullability annotation is kept and discarded in Java and Kotlin as appropriate`() {
        val moduleJ = """
            |@Nullable
            |public String nulla() { return null; }
            |public String platform() { return null; }
            |@NonNull
            |public String nonna1() { return ""; }
            |@NotNull
            |public String nonna2() { return ""; }
        """.renderJava(imports = listOf("import org.jetbrains.annotations.NotNull"))
        val moduleK = """
            |annotation class Nullable
            |annotation class NonNull
            |@Nullable
            |fun nulla(): String? = null
            |@NonNull
            |fun nonna1(): String = "foo"
            |fun nonna2(): String = "foo"
        """.render()
        for (whichFun in listOf("nonna1", "nonna2", "nulla", "platform")) {
            val annotationsJ = moduleJ.functionAnnotations(whichFun)
            val annotationsK = if (whichFun != "platform") moduleK.functionAnnotations(whichFun)
            else null
            for (annotations in listOfNotNull(annotationsK, annotationsJ)) {
                val isFromJava = (annotations === annotationsJ) // compare by reference
                val isKotlinNullable = !isFromJava && "nulla" in whichFun
                val annotationsAsJ = annotations.components(JAVA, isKotlinNullable, isFromJava)
                val annotationsAsK = annotations.components(KOTLIN, isKotlinNullable, isFromJava)
                // Java docs retain explicit nullability in java source, and get injected @NonNull
                if ("nonna" in whichFun) assertThat(annotationsAsJ.single().isAtNonNull).isTrue()
                else if (whichFun == "nulla")
                    assertThat(annotationsAsJ.single().isAtNullable).isTrue()
                else assertThat(annotationsAsJ).isEmpty()
                // Kotlin docs retain NO nullability annotations EVEN IF explicit in Kotlin source
                assertThat(annotationsAsK).isEmpty()
            }
        }
    }

    @Test
    fun `Nullability annotation is found`() {
        val annotations = """
            |annotation class Nullable
            |@Nullable
            |fun foo() = Unit
        """.render().functionAnnotations()

        assertThat(annotations.hasAtNullable()).isTrue()
    }

    @Test
    fun `Nullability annotation is not injected for Kotlin-nullable type as-Java`() {
        val annotations = emptyList<Annotation>()
            .components(JAVA, isKotlinNullable = true, isFromJava = false)

        assertThat(annotations).isEmpty()
    }

    @Test
    fun `Nullability annotation isn't doubly injected for @Nullable Kotlin-nullable type`() {
        val annotations = listOf(
            Annotation(DRI("androidx.annotation", "Nullable"), emptyMap())
        ).components(isKotlinNullable = true, isFromJava = false)

        assertThat(annotations.single().isAtNullable).isTrue()
    }

    @Test
    fun `Nullability annotation is NOT injected for Kotlin-nullable type as-Kotlin`() {
        val annotations = emptyList<Annotation>()
            .components(KOTLIN, isKotlinNullable = true, isFromJava = false)

        assertThat(annotations).isEmpty()
    }

    @Test
    fun `Long annotation parameter values are parsed correctly`() {
        val annotationsKt = """
            |@Target([AnnotationTarget.VALUE_PARAMETER])
            |annotation class Foo(bar: Long)

            |fun baz(@Foo(bar = 100) arg: Long): Long = 1
        """.render().function()!!.parameters.single().allAnnotations()
        val paramValue = annotationsKt.components(isFromJava = false)
            .first().data.parameters.single()
        val data = (paramValue as NamedValueAnnotationParameter).data
        assertThat(data.name).isEqualTo("bar")
        assertThat(data.value).isEqualTo("100")
    }

    @Test
    fun `Hidden annotations are not displayed`() {
        val annotations = """
            |/** @hide */
            |annotation class HiddenAnnotation
            |annotation class VisibleAnnotation
            |@HiddenAnnotation
            |@VisibleAnnotation
            |fun foo() = Unit
        """.render().functionAnnotations()

        val components = annotations.components().exceptNonNull()
        assertThat(components.size).isEqualTo(1)
        assertThat(components.item().link().name).isEqualTo("VisibleAnnotation")
    }

    private fun DModule.functionAnnotations(name: String = "foo"): List<Annotation> {
        return function(name)!!.allAnnotations()
    }

    private fun DModule.functionReturnAnnotations(name: String = "foo"): List<Annotation> {
        return function(name)!!.type.annotations(function(name)!!.getExpectOrCommonSourceSet())
    }

    private fun List<Annotation>.components(
        displayLanguage: Language = JAVA,
        isKotlinNullable: Boolean = false,
        isFromJava: Boolean = true,
        hiddenAnnotations: Set<String> = emptySet()
    ) = annotationComponents(
        pathProvider = pathProvider(),
        displayLanguage = displayLanguage,
        nullability = if (isKotlinNullable) Nullability.KOTLIN_NULLABLE
        else this.inferNullability()
            ?: defaultNullability(isFromJava),
        annotationsNotToDocument = hiddenAnnotations
    )

    private fun AnnotationComponent.link(): Link.Params = data.type.data
}
