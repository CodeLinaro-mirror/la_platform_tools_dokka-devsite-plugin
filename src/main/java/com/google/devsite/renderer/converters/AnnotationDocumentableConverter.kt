/*
 * Copyright 2022 The Android Open Source Project
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

import com.google.devsite.components.impl.DefaultAnnotationComponent
import com.google.devsite.components.impl.DefaultAnnotationValueAnnotationParameter
import com.google.devsite.components.impl.DefaultArrayValueAnnotationParameter
import com.google.devsite.components.impl.DefaultNamedValueAnnotationParameter
import com.google.devsite.components.symbols.AnnotationComponent
import com.google.devsite.components.symbols.AnnotationParameter
import com.google.devsite.components.symbols.AnnotationValueAnnotationParameter
import com.google.devsite.components.symbols.ArrayValueAnnotationParameter
import com.google.devsite.components.symbols.NamedValueAnnotationParameter
import com.google.devsite.hasBeenHidden
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.AnnotationParameterValue
import org.jetbrains.dokka.model.AnnotationValue
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.ArrayValue
import org.jetbrains.dokka.model.ClassValue
import org.jetbrains.dokka.model.EnumValue
import org.jetbrains.dokka.model.LiteralValue
import org.jetbrains.dokka.model.StringValue

/**
 * Converts annotations into their components.
 */
internal class AnnotationDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder
) {
    /**
     * @param nullability the nullability of the annotated element. Contains information such as
     * source language and whether we care about the nullability of the annotated element.
     * @return the AnnotationComponents for the given annotations on the annotated element
     */
    fun annotationComponents(
        annotations: List<Annotations.Annotation>,
        nullability: Nullability,
    ): List<AnnotationComponent> {
        val injectedAnnotations = mutableListOf<Annotations.Annotation?>()
        if (annotations.any { it.isBadNonNull }) {
            injectedAnnotations.add(AT_NON_NULL) // Bad ones get filtered out later
            docsHolder.logger.warn(
                "Use @androidx.annotation.NonNull, not " +
                    "@${annotations.first{it.isBadNonNull}.dri}"
            )
            assert(nullability != Nullability.JAVA_NOT_ANNOTATED)
        }
        if (annotations.any { it.isBadNullable }) {
            injectedAnnotations.add(AT_NULLABLE) // Again, this generally means a bad test classpath
            docsHolder.logger.warn(
                "Use @androidx.annotation.Nullable, not " +
                    "@${annotations.first{it.isBadNullable}.dri}"
            )
            assert(nullability != Nullability.JAVA_NOT_ANNOTATED)
        }

        // NOTE: we inject @NonNull, but not @Nullable, as that is usually not useful to Java devs
        if (displayLanguage == Language.JAVA) {
            injectedAnnotations += nullability.renderAsJavaAnnotation()
        }

        return (annotations + injectedAnnotations).filterNotNull().filter { annotation ->
            shouldDocumentAnnotation(annotation, nullability)
        }.distinctBy { it.identifier }.map { annotation -> annotation.toDackkaAnnotation() }
    }

    /** @return true if a developer would find this annotation useful, false otherwise */
    private fun shouldDocumentAnnotation(
        annotation: Annotations.Annotation,
        nullability: Nullability,
    ): Boolean {
        // Not useful to developers
        if (annotation.isSuppressAnnotation() ||
            annotation.dri.packageName == "kotlin.jvm" ||
            annotation.dri.fullName in docsHolder.annotationsNotToDisplay
        ) return false
        // Surfaced separately
        if (annotation.isDeprecated()) return false

        if (annotation.dri.classNames in NULLABILITY_ANNOTATION_NAMES) {
            // Ignored and overwritten with androidx.annotation.NonNull
            if (annotation.isBadNullable || annotation.isBadNonNull) return false
            // Explicitly hidden nullability annotations
            if (nullability == Nullability.DONT_CARE) return false
            // Nullability annotations do not appear in Kotlin, even if explicit in Kotlin source
            if (displayLanguage == Language.KOTLIN) return false
        }

        return !hasBeenHidden(annotation.dri)
    }

    private fun Annotations.Annotation.toDackkaAnnotation(): AnnotationComponent {
        val type = pathProvider.linkForReference(dri)
        val params = params.map { (name, contents) -> contents.toComponent(name) }
        return DefaultAnnotationComponent(AnnotationComponent.Params(type, params))
    }

    private fun AnnotationParameterValue.toComponent(
        name: String? = null
    ): AnnotationParameter = when (this) {
        is StringValue -> DefaultNamedValueAnnotationParameter(
            NamedValueAnnotationParameter.Params(name, "\"${asString()}\"")
        )
        is LiteralValue, is EnumValue, is ClassValue ->
            DefaultNamedValueAnnotationParameter(
                NamedValueAnnotationParameter.Params(name, asString())
            )
        is ArrayValue -> DefaultArrayValueAnnotationParameter(
            ArrayValueAnnotationParameter.Params(
                name,
                innerAnnotationParameters = value.map { it.toComponent() }
            )
        )
        is AnnotationValue -> DefaultAnnotationValueAnnotationParameter(
            AnnotationValueAnnotationParameter.Params(
                name,
                annotationComponentValue = annotation.toDackkaAnnotation()
            )
        )
    }
}
