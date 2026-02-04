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
import com.google.devsite.components.symbols.ClasslikeSignature
import com.google.devsite.components.symbols.TypeParameterComponent
import com.google.devsite.components.testing.NoopAnnotationComponent
import com.google.devsite.components.testing.NoopFilePathProvider
import com.google.devsite.components.testing.NoopLink
import com.google.devsite.components.testing.NoopTypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.EmptyModifiers
import com.google.devsite.renderer.converters.Modifiers
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultClasslikeSignatureTest {
    @Test
    fun `Class signature renders correctly in Java`() {
        val component =
            DefaultClasslikeSignature(
                ClasslikeSignature.Params(
                    displayLanguage = Language.JAVA,
                    name = NoopLink("Foo"),
                    type = "class",
                    modifiers = Modifiers("public", "abstract"),
                    extends = listOf(NoopLink("Anyclass")),
                    implements = listOf(NoopLink("SomeInterface")),
                    typeParameters =
                        listOf(
                            DefaultTypeParameterComponent(
                                TypeParameterComponent.Params(
                                    name = "GenericType",
                                    projections =
                                        listOf(NoopTypeProjectionComponent("GenericSupertype")),
                                    pathProvider = NoopFilePathProvider(),
                                    displayLanguage = Language.KOTLIN,
                                )
                            )
                        ),
                    annotationComponents = listOf(NoopAnnotationComponent("@GenericAnnotation")),
                )
            )

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        Truth.assertThat(output)
            .isEqualTo(
                """
<body>@GenericAnnotation<br>public abstract class Foo&lt;GenericType&nbsp;:&nbsp;GenericSupertype&gt; extends Anyclass implements SomeInterface</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Class signature renders correctly in Kotlin`() {
        val component =
            DefaultClasslikeSignature(
                ClasslikeSignature.Params(
                    displayLanguage = Language.KOTLIN,
                    name = NoopLink("Foo"),
                    type = "class",
                    modifiers = Modifiers("open"),
                    extends = listOf(NoopLink("Anyclass")),
                    implements = listOf(NoopLink("SomeInterface")),
                    typeParameters =
                        listOf(
                            DefaultTypeParameterComponent(
                                TypeParameterComponent.Params(
                                    name = "GenericType",
                                    projections =
                                        listOf(NoopTypeProjectionComponent("GenericSupertype")),
                                    pathProvider = NoopFilePathProvider(),
                                    displayLanguage = Language.KOTLIN,
                                )
                            )
                        ),
                    annotationComponents = listOf(NoopAnnotationComponent("@GenericAnnotation")),
                )
            )

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        Truth.assertThat(output)
            .isEqualTo(
                """
<body>@GenericAnnotation<br>open class Foo&lt;GenericType&nbsp;:&nbsp;GenericSupertype&gt; : Anyclass, SomeInterface</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Interfaces extend other interfaces`() { // and do not "implement" them
        val component =
            DefaultClasslikeSignature(
                ClasslikeSignature.Params(
                    displayLanguage = Language.JAVA,
                    name = NoopLink("Foo"),
                    type = "interface",
                    modifiers = EmptyModifiers,
                    extends = listOf(),
                    implements = listOf(NoopLink("SomeInterface")),
                    typeParameters = listOf(),
                    annotationComponents = emptyList(),
                )
            )

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        Truth.assertThat(output)
            .isEqualTo(
                """
<body>interface Foo extends SomeInterface</body>
        """
                    .trim()
            )
    }
}
