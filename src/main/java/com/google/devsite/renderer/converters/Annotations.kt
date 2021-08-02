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
import com.google.devsite.components.impl.DefaultAnnotationValueAnnotationParameter
import com.google.devsite.components.impl.DefaultArrayValueAnnotationParameter
import com.google.devsite.components.impl.DefaultNamedValueAnnotationParameter
import com.google.devsite.components.symbols.AnnotationParameter
import com.google.devsite.components.symbols.AnnotationValueAnnotationParameter
import com.google.devsite.components.symbols.ArrayValueAnnotationParameter
import com.google.devsite.components.symbols.NamedValueAnnotationParameter
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
import org.jetbrains.dokka.model.LiteralValue
import org.jetbrains.dokka.model.StringValue
import org.jetbrains.dokka.model.properties.WithExtraProperties
import kotlin.Boolean
import kotlin.String
import com.google.devsite.components.symbols.Annotation as AnnotationComponent

/** @return the components for the provided dokka model annotations */
internal fun List<Annotation>.annotationComponents(
    pathProvider: FilePathProvider,
    displayLanguage: Language,
    nullable: Boolean,
    showNullability: Boolean = true
): List<AnnotationComponent> {
    val injectedAnnotations = mutableListOf<Annotation>()
    if (nullable && displayLanguage == Language.JAVA && !isNullable() && showNullability) {
        injectedAnnotations += Annotation(DRI("androidx.annotation", "Nullable"), emptyMap())
    }
    if (!nullable && displayLanguage == Language.JAVA && !isNonNull() && showNullability) {
        injectedAnnotations += Annotation(DRI("androidx.annotation", "NonNull"), emptyMap())
    }

    return (this + injectedAnnotations).filter { annotation ->
        shouldDocumentAnnotation(annotation, displayLanguage, showNullability)
    }.map { annotation -> annotation.toDackkaAnnotation(pathProvider) }
}

private fun Annotation.toDackkaAnnotation(pathProvider: FilePathProvider): AnnotationComponent {
    val type = pathProvider.linkForReference(dri)
    val params = params.map { (name, contents) ->
        contents.toComponent(name, pathProvider)
    }
    return DefaultAnnotation(AnnotationComponent.Params(type, params))
}

/** @return true if the `@Nullable` annotation is present, false otherwise */
internal fun List<Annotation>.isNullable(): Boolean = any { it.dri.classNames == "Nullable" }

internal fun List<Annotation>.isNonNull(): Boolean = any { it.dri.classNames == "NonNull" }

/** @return true if the `@Deprecated` annotation is present, false otherwise */
internal fun List<Annotation>.isDeprecated(): Boolean = any { it.isDeprecated() }

/** @return the complete list of annotations for this type */
internal fun WithExtraProperties<*>.annotations(): List<Annotation> {
    return extra.allOfType<Annotations>().flatMap { annotations ->
        annotations.directAnnotations.values.singleOrNull() ?: emptyList()
    }
}

/** @return the complete list of annotations for this type */
internal fun WithExtraProperties<*>.fileLevelAnnotations(): List<Annotation> {
    return extra.allOfType<Annotations>().flatMap { annotations ->
        annotations.fileLevelAnnotations.values.singleOrNull() ?: emptyList()
    }
}

/** @return true if the `@Deprecated` annotation is present, false otherwise */
internal fun Annotation.isDeprecated(): Boolean = dri.classNames == "Deprecated"

/** @return true if a developer would find this annotation useful, false otherwise */
private fun shouldDocumentAnnotation(
    annotation: Annotation,
    language: Language,
    showNullability: Boolean = true
): Boolean {
    val name = annotation.dri.classNames
    // Not useful to developers
    val isSuppressAnnotation = name in SUPPRESSION_ANNOTATION_NAMES
    val isKotlinJvmAnnotation = annotation.dri.packageName == "kotlin.jvm"
    val isExplicitlyBannedAnnotation = name in EXPLICITLY_BANNED_ANNOTATION_NAMES

    // Surfaced separately
    val isDeprecatedAnnotation = annotation.isDeprecated()
    val isNullabilityAnnotation = name in NULLABILITY_ANNOTATION_NAMES

    return !isSuppressAnnotation &&
        !isKotlinJvmAnnotation &&
        !isExplicitlyBannedAnnotation &&
        !isDeprecatedAnnotation &&
        // Keep nullability annotations for Java, if we should show nullability
        ((language == Language.JAVA && showNullability) || !isNullabilityAnnotation)
}

private val SUPPRESSION_ANNOTATION_NAMES = listOf("Suppress", "SuppressWarnings", "SuppressLint")
private val NULLABILITY_ANNOTATION_NAMES = listOf("NonNull", "Nullable")
private val EXPLICITLY_BANNED_ANNOTATION_NAMES = listOf(
    // This information is compose runtime implementation details; not useful for most
    // and those who would want it should be looking at source
    "Stable", "Immutable", "ReadOnlyComposable",
    // This opt-in requirement is non-propagating so developers don't need to know about it
    // https://kotlinlang.org/docs/opt-in-requirements.html#non-propagating-opt-in
    "OptIn",
    // This annotation is used mostly in paging, and was removed at the request of the paging team
    "CheckResult"
)

internal fun AnnotationParameterValue.toComponent(
    name: String? = null,
    pathProvider: FilePathProvider
): AnnotationParameter = when (this) {
    is StringValue -> DefaultNamedValueAnnotationParameter(
        NamedValueAnnotationParameter.Params(name, "\"${value}\""))
    is LiteralValue -> DefaultNamedValueAnnotationParameter(
        NamedValueAnnotationParameter.Params(name, text())
    )
    is EnumValue -> DefaultNamedValueAnnotationParameter(
        NamedValueAnnotationParameter.Params(name, enumName))
    is ClassValue -> DefaultNamedValueAnnotationParameter(
        NamedValueAnnotationParameter.Params(name, className))
    is ArrayValue -> DefaultArrayValueAnnotationParameter(
        ArrayValueAnnotationParameter.Params(
            name,
            innerAnnotationParameters = value.map { it.toComponent(pathProvider = pathProvider) }
        )
    )
    is AnnotationValue -> DefaultAnnotationValueAnnotationParameter(
        AnnotationValueAnnotationParameter.Params(
            name,
            annotationValue = annotation.toDackkaAnnotation(pathProvider)
        )
    )
}

internal fun Annotation.nameAsString(): String? = (params["name"] as? StringValue)?.value

private const val LONG_ANNO_PARAM_SUFFIX = ".toLong()"

private fun StringValue.isProbablyLong(): Boolean = value.endsWith(LONG_ANNO_PARAM_SUFFIX)
private fun StringValue.cleanedLongValue(): String = value.removeSuffix(LONG_ANNO_PARAM_SUFFIX)
