package com.google.devsite.components.symbols

import com.google.devsite.components.Link
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Nullability

internal interface MappedTypeProjectionComponent : TypeProjectionComponent {
    override val data: Params

    override fun length() = super.length() + "()".length + data.alternativePrefix.length()

    class Params(
        type: Link,
        val alternativePrefix: Link,
        annotationComponents: List<AnnotationComponent> = emptyList(),
        nullability: Nullability,
        generics: List<TypeProjectionComponent> = emptyList()
    ) : TypeProjectionComponent.Params(
        type = type,
        annotationComponents = annotationComponents,
        nullability = nullability,
        generics = generics,
        displayLanguage = Language.KOTLIN
    )
}
