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

import com.google.devsite.components.impl.DefaultAnnotation
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.AnnotationParameterValue
import org.jetbrains.dokka.model.AnnotationValue
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Annotations.Annotation
import org.jetbrains.dokka.model.ArrayValue
import org.jetbrains.dokka.model.ClassValue
import org.jetbrains.dokka.model.EnumValue
import org.jetbrains.dokka.model.StringValue
import org.jetbrains.dokka.model.properties.WithExtraProperties
import com.google.devsite.components.symbols.Annotation as AnnotationComponent

/** @return the components for the provided dokka model annotations */
internal fun List<Annotation>.annotationComponents(
    pathProvider: FilePathProvider,
    displayLanguage: Language,
    nullable: Boolean
): List<AnnotationComponent> {
    val injectedAnnotations = mutableListOf<Annotation>()
    if (nullable && !isNullable()) {
        injectedAnnotations += Annotation(DRI("androidx.annotation", "Nullable"), emptyMap())
    }

    return (this + injectedAnnotations).filter { annotation ->
        shouldDocumentAnnotation(annotation, displayLanguage)
    }.map { annotation ->
        val type = pathProvider.linkForReference(annotation.dri)
        val params = annotation.params.map { (name, contents) ->
            AnnotationComponent.Parameter(name, contents.toComponent())
        }

        DefaultAnnotation(AnnotationComponent.Params(type, params))
    }
}

/** @return true if the `@Nullable` annotation is present, false otherwise */
internal fun List<Annotation>.isNullable(): Boolean = any { it.dri.classNames == "Nullable" }

/** @return true if the `@Deprecated` annotation is present, false otherwise */
internal fun List<Annotation>.isDeprecated(): Boolean = any { it.isDeprecated() }

/** @return the complete list of annotations for this type */
internal fun WithExtraProperties<*>.annotations(): List<Annotation> {
    return extra.allOfType<Annotations>().flatMap { annotations ->
        annotations.content.values.single()
    }
}
/** @return true if the `@Deprecated` annotation is present, false otherwise */
internal fun Annotation.isDeprecated(): Boolean = dri.classNames == "Deprecated"

/** @return true if a developer would find this annotation useful, false otherwise */
private fun shouldDocumentAnnotation(annotation: Annotation, language: Language): Boolean {
    // Not useful to developers
    val isSuppressAnnotation = annotation.dri.classNames == "Suppress" ||
        annotation.dri.classNames == "SuppressWarnings"
    val isKotlinJvmAnnotation = annotation.dri.packageName == "kotlin.jvm"
    // Surfaced separately
    val isDeprecatedAnnotation = annotation.isDeprecated()
    val isNullabilityAnnotation = annotation.dri.classNames == "NonNull" ||
        annotation.dri.classNames == "Nullable"

    return !isSuppressAnnotation &&
        !isKotlinJvmAnnotation &&
        !isDeprecatedAnnotation &&
        // Keep nullability annotations for Java
        (language == Language.JAVA || !isNullabilityAnnotation)
}

internal fun AnnotationParameterValue.toComponent(): String = when (this) {
    is StringValue -> value
    is EnumValue -> enumName
    is ClassValue -> className
    is ArrayValue -> value.joinToString(prefix = "[", postfix = "]") { it.toComponent() }
    is AnnotationValue -> TODO("Unknown use case.")
}
