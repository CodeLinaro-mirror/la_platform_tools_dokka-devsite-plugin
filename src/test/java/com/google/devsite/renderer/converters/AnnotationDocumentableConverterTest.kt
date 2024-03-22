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
import com.google.devsite.renderer.converters.testing.exceptNonNull
import com.google.devsite.renderer.converters.testing.isAtNonNull
import com.google.devsite.renderer.converters.testing.isAtNullable
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class AnnotationDocumentableConverterTest(
    displayLanguage: Language,
) : ConverterTestBase(displayLanguage) {

    @Test
    fun `@Suppress annotations are ignored`() {
        val annotations = """
            |annotation class SuppressLint(val bar: String = "This is part of Lint, not Kotlin")
            |@Suppress("abc")
            |@SuppressLint("123")
            |@SuppressWarnings("do re mi")
            |fun foo() = Unit
        """.render().functionAnnotationComponents()

        assertThat(annotations.exceptNonNull()).isEmpty()
    }

    @Test
    fun `JVM annotations are ignored`() {
        val annotations = """
            |@JvmName("bar")
            |fun foo() = Unit
        """.render().functionAnnotationComponents()

        assertThat(annotations.exceptNonNull()).isEmpty()
    }

    @Test
    fun `Annotations in the do-not-document list are not displayed`() {
        val module = """
            |@Override
            |fun foo() = Unit
        """.render()

        val annotationsNoneHidden = module.functionAnnotationComponents()
        // When no annotations are hidden, Override is displayed
        assertThat(annotationsNoneHidden.exceptNonNull().single().name).isEqualTo("Override")

        // Override is not displayed when hidden
        val annotationsOverrideHidden =
            module.functionAnnotationComponents(hiddenAnnotations = setOf("java.lang.Override"))
        assertThat(annotationsOverrideHidden.exceptNonNull()).isEmpty()
    }

    @Test
    fun `@Deprecated annotations are ignored since they are surfaced separately`() {
        val annotations = """
            |@Deprecated("So long, farewell, auf wiedersehen, goodbye")
            |fun foo() = Unit
        """.render().functionAnnotationComponents()

        assertThat(annotations.exceptNonNull()).isEmpty()
    }

    @Test
    fun `@Deprecated annotation in list of Annotations is found`() {
        val annotations = """
            |@Deprecated("So long, farewell, auf wiedersehen, goodbye")
            |fun foo() = Unit
        """.render().function()!!.allAnnotations()

        assertThat(annotations.isDeprecated()).isTrue()
    }

    @Test
    fun `@Deprecated annotation in Annotation object is not found`() {
        val annotation = """
            |@FooAnnotation
            |fun foo() = Unit
        """.render().function()!!.allAnnotations()

        assertThat(annotation.isDeprecated()).isFalse()
    }

    @Test
    fun `Component has annotation type`() {
        val annotations = """
            |annotation class Hello
            |@Hello
            |fun foo() = Unit
        """.render().functionAnnotationComponents()

        val annotation = annotations.exceptNonNull().item()

        assertThat(annotation.link().name).isEqualTo("Hello")
        assertPath(annotation.link().url, "androidx/example/Hello.html")
    }

    @Test
    fun `Private annotation does not appear on function`() {
        val annotations = """
            |private annotation class PrivateAnnotation
            |internal annotation class InternalAnnotation
            |@PrivateAnnotation
            |@InternalAnnotation
            |fun foo() = Unit
        """.render().functionAnnotationComponents()
        assertThat(annotations).isEmpty()
    }

    @Test
    fun `Method component has annotation and value in 4x Kotlin and Java`() {
        val annotationsK = """
            |annotation class Hello(val bar: String)
            |@Hello("abc")
            |@Hello(bar = "baz")
            |fun foo() = Unit
        """.render().functionAnnotationComponents()
        val annotationsJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.METHOD)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |@Hello("abc")
            |@Hello(bar = "baz")
            |public void foo() {}
        """.render(java = true).functionAnnotationComponents()

        for (annotations in listOf(annotationsK, annotationsJ)) {
            val annotationOne = annotations.exceptNonNull().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.exceptNonNull().last()
            val parameterTwo = annotationTwo.data.parameters.item()

            // NOTE: "value" in java does not match "bar" in kotlin
            if (annotations == annotationsK) {
                assertThat(parameterOne.name).isEqualTo("bar")
            } else assertThat(parameterOne.name).isEqualTo("value")
            assertThat(parameterOne.value).isEqualTo("\"abc\"")
            assertThat(parameterTwo.name).isEqualTo("bar")
            assertThat(parameterTwo.value).isEqualTo("\"baz\"")
        }
    }

    @Test
    fun `Property component has annotation and value in 4x Kotlin and Java`() {
        val moduleK = """
            |annotation class Hello(val bar: String)
            |@Hello("abc")
            |@Hello(bar = "baz")
            |val foo: String = "foofoo"
        """.render()
        val annotationsK = moduleK.annotationComponents(moduleK.property()!!)
        val moduleJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.FIELD)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |@Hello("abc")
            |@Hello(bar = "baz")
            |public String foo = "foofoo"
        """.render(java = true)
        val annotationsJ = moduleJ.annotationComponents(moduleK.property()!!)

        for (annotations in listOf(annotationsK, annotationsJ)) {
            val annotationOne = annotations.exceptNonNull().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.exceptNonNull().last()
            val parameterTwo = annotationTwo.data.parameters.item()

            // NOTE: "value" in java does not match "bar" in kotlin
            if (annotations == annotationsK) {
                assertThat(parameterOne.name).isEqualTo("bar")
            } else assertThat(parameterOne.name).isEqualTo("value")
            assertThat(parameterOne.value).isEqualTo("\"abc\"")
            assertThat(parameterTwo.name).isEqualTo("bar")
            assertThat(parameterTwo.value).isEqualTo("\"baz\"")
        }
    }

    @Test
    fun `Parameter component has annotation and value in 4x Kotlin and Java`() {
        val moduleK = """
            |annotation class Hello(val bar: String)
            |fun foo(@Hello("abc") @Hello(bar = "baz") arg: String) = Unit
        """.render()
        val annotationsK = moduleK.annotationComponents(moduleK.function()!!.parameters.single())
        val moduleJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.PARAMETER)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |public void foo(@Hello("abc") @Hello(bar = "baz") @NonNull String arg)
        """.render(java = true)
        val annotationsJ = moduleJ.annotationComponents(moduleJ.function()!!.parameters.single())

        for (annotations in listOf(annotationsK, annotationsJ)) {
            val annotationOne = annotations.exceptNonNull().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.exceptNonNull().last()
            val parameterTwo = annotationTwo.data.parameters.item()

            if (annotations == annotationsK) {
                assertThat(parameterOne.name).isEqualTo("bar")
            } else assertThat(parameterOne.name).isEqualTo("value")
            assertThat(parameterOne.value).isEqualTo("\"abc\"")
            assertThat(parameterTwo.name).isEqualTo("bar")
            assertThat(parameterTwo.value).isEqualTo("\"baz\"")
        }
    }

    @Test
    fun `Type parameter component has annotation and value in 4x Kotlin and Java`() {
        val moduleK = """
            |annotation class Hello(val bar: String)
            |fun <@Hello("abc") @Hello(bar = "baz") T> foo(arg: String): List<T>
        """.render()
        val annotationsK = moduleK.annotationComponents(moduleK.function()!!.generics.single())
        val moduleJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.TYPE_PARAMETER)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |public <@Hello("abc") @Hello(bar = "baz") T> java.util.List<T> foo()
        """.render(java = true)
        val annotationsJ = moduleJ.annotationComponents(moduleJ.function()!!.generics.single())

        for (annotations in listOf(annotationsK, annotationsJ)) {
            val annotationOne = annotations.exceptNonNull().first()
            val parameterOne = annotationOne.data.parameters.item()
            val annotationTwo = annotations.exceptNonNull().last()
            val parameterTwo = annotationTwo.data.parameters.item()

            if (annotations == annotationsK) {
                assertThat(parameterOne.name).isEqualTo("bar")
            } else assertThat(parameterOne.name).isEqualTo("value")
            assertThat(parameterOne.value).isEqualTo("\"abc\"")
            assertThat(parameterTwo.name).isEqualTo("bar")
            assertThat(parameterTwo.value).isEqualTo("\"baz\"")
        }
    }

    @Test
    fun `Type parameter type has annotation and value in 4x Kotlin and Java`() {
        fun DModule.genericBoundsAnnotations() =
            function()!!.generics.single().bounds.single().annotations(getExpectOrCommonSourceSet())
        val moduleK = """
            |annotation class Hello(val bar: String)
            |fun <T : @Hello("baz") String> foo(arg: String): List<T>
        """.render()
        val annotationsK = moduleK.annotationComponents(moduleK.genericBoundsAnnotations())
        val moduleJ = """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.TYPE_USE)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |public <T extends @Hello(bar = "baz") String> java.util.List<T> foo() {
            |    return null;
            |}
        """.render(java = true)
        val annotationsJ = moduleJ.annotationComponents(moduleJ.genericBoundsAnnotations())

        for (annotations in listOf(annotationsJ, annotationsK)) {
            val annotationOne = annotations.first()
            val parameterOne = annotationOne.data.parameters.item()
            assertThat(parameterOne.name).isEqualTo("bar")
            assertThat(parameterOne.value).isEqualTo("\"baz\"")
        }
    }

    @Test
    fun `Nullability annotations are created correctly from Java source`() {
        val module = """
            |@Nullable
            |public String nullable() { return null; }
            |public String noAnnotation() { return null; }
            |@NonNull
            |public String nonNull() { return ""; }
        """.renderJava(imports = listOf("import org.jetbrains.annotations.NotNull"))

        val noAnnotationAnnotations =
            module.functionAnnotationComponents("noAnnotation", Nullability.JAVA_NOT_ANNOTATED)
        val nullableAnnotations =
            module.functionAnnotationComponents("nullable", Nullability.JAVA_ANNOTATED_NULLABLE)
        val nonNullAnnotations =
            module.functionAnnotationComponents("nonNull", Nullability.JAVA_ANNOTATED_NOT_NULL)

        javaOnly {
            assertThat(noAnnotationAnnotations).isEmpty()
            assertThat(nullableAnnotations.single().isAtNullable).isTrue()
            assertThat(nonNullAnnotations.single().isAtNonNull).isTrue()
        }

        // Kotlin docs retain NO nullability annotations
        kotlinOnly {
            assertThat(noAnnotationAnnotations).isEmpty()
            assertThat(nullableAnnotations).isEmpty()
            assertThat(nonNullAnnotations).isEmpty()
        }
    }

    @Test
    fun `Nullability annotations are created correctly from Kotlin source`() {
        val module = """
        |annotation class NonNull
        |annotation class Nullable
        |@Nullable
        |fun nullable(): String? = null
        |@NonNull
        |fun nonNull(): String = "foo"
        |fun noAnnotation(): String = "foo"
        |
        """.render()

        val noAnnotationAnnotations =
            module.functionAnnotationComponents("noAnnotation", Nullability.KOTLIN_DEFAULT)
        val nullableAnnotations =
            module.functionAnnotationComponents("nullable", Nullability.KOTLIN_NULLABLE)
        val nonNullAnnotations =
            module.functionAnnotationComponents("nonNull", Nullability.KOTLIN_DEFAULT)

        javaOnly {
            // Non-null annotation is injected
            assertThat(noAnnotationAnnotations.single().isAtNonNull).isTrue()
            assertThat(nullableAnnotations.single().isAtNullable).isTrue()
            assertThat(nonNullAnnotations.single().isAtNonNull).isTrue()
        }

        // Kotlin docs retain NO nullability annotations
        kotlinOnly {
            assertThat(noAnnotationAnnotations).isEmpty()
            assertThat(nullableAnnotations).isEmpty()
            assertThat(nonNullAnnotations).isEmpty()
        }
    }

    @Test
    fun `Nullability annotation is found`() {
        val annotations = """
            |annotation class Nullable
            |@Nullable
            |fun foo() = Unit
        """.render().function()!!.allAnnotations()

        assertThat(annotations.hasAtNullable()).isTrue()
    }

    @Test
    fun `Nullability annotation is not injected for Kotlin-nullable type`() {
        val module = """
            |val foo: Int? = null
        """.render()
        val annotations =
            module.annotationComponents(module.property()!!, Nullability.KOTLIN_NULLABLE)

        assertThat(annotations).isEmpty()
    }

    @Test
    fun `Nullability annotation isn't doubly injected for @Nullable Kotlin-nullable type`() {
        val module = """
            |annotation class Nullable
            |@Nullable val foo: Int? = null
        """.render()
        val annotations =
            module.annotationComponents(module.property()!!, Nullability.KOTLIN_NULLABLE)

        javaOnly {
            assertThat(annotations.single().isAtNullable).isTrue()
        }
        kotlinOnly {
            assertThat(annotations).isEmpty()
        }
    }

    @Test
    fun `Long annotation parameter values are parsed correctly`() {
        val module = """
            |@Target([AnnotationTarget.VALUE_PARAMETER])
            |annotation class Foo(bar: Long)

            |fun baz(@Foo(bar = 100) arg: Long): Long = 1
        """.render()
        val annotations = module.annotationComponents(module.function()!!.parameters.single())
        val paramValue = annotations.first().data.parameters.single()
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
        """.render().functionAnnotationComponents().exceptNonNull()

        assertThat(annotations.size).isEqualTo(1)
        assertThat(annotations.item().link().name).isEqualTo("VisibleAnnotation")
    }

    private fun DModule.annotationComponents(
        element: Documentable,
        nullability: Nullability = Nullability.DONT_CARE,
        hiddenAnnotations: Set<String> = emptySet(),
    ): List<AnnotationComponent> =
        annotationComponents(element.allAnnotations(), nullability, hiddenAnnotations)

    private fun DModule.functionAnnotationComponents(
        name: String = "foo",
        nullability: Nullability = Nullability.DONT_CARE,
        hiddenAnnotations: Set<String> = emptySet(),
    ): List<AnnotationComponent> =
        annotationComponents(function(name)!!, nullability, hiddenAnnotations)

    private fun AnnotationComponent.link(): Link.Params = data.type.data

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN),
        )
    }
}
