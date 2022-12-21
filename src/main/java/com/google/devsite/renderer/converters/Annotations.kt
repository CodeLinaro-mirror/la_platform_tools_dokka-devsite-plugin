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
import com.google.devsite.renderer.impl.paths.FilePathProvider
import com.google.devsite.strictSingleOrNull
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.AnnotationParameterValue
import org.jetbrains.dokka.model.AnnotationValue
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Annotations.Annotation
import org.jetbrains.dokka.model.ArrayValue
import org.jetbrains.dokka.model.Bound
import org.jetbrains.dokka.model.ClassValue
import org.jetbrains.dokka.model.DefinitelyNonNullable
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Dynamic
import org.jetbrains.dokka.model.EnumValue
import org.jetbrains.dokka.model.FunctionalTypeConstructor
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.JavaObject
import org.jetbrains.dokka.model.LiteralValue
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.PrimitiveJavaType
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.StringValue
import org.jetbrains.dokka.model.TypeAliased
import org.jetbrains.dokka.model.TypeParameter
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Void
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.properties.WithExtraProperties

/**
 * @param displayLanguage nullability annotations are present only in Java
 * @param nullability the nullability of the annotated element. Contains information such as source
 * language and whether we care about the nullability of the annotated element.
 *
 * @return the AnnotationComponents for the given annotations on the annotated element
 */
internal fun List<Annotation>.annotationComponents(
    pathProvider: FilePathProvider,
    displayLanguage: Language,
    nullability: Nullability
): List<AnnotationComponent> {
    val injectedAnnotations = mutableListOf<Annotation?>()
    if (any { it.isBadNonNull }) {
        injectedAnnotations.add(AT_NON_NULL) // Bad ones get filtered out later
        println("WARN: Use @androidx.annotation.NonNull, not @${first{it.isBadNonNull}.dri}")
        assert(nullability != Nullability.JAVA_NOT_ANNOTATED)
    }
    if (any { it.isBadNullable }) {
        injectedAnnotations.add(AT_NULLABLE) // Again, this generally means a bad test classpath
        println("WARN: Use @androidx.annotation.Nullable, not @${first{it.isBadNullable}.dri}")
        assert(nullability != Nullability.JAVA_NOT_ANNOTATED)
    }

    // NOTE: we inject @NonNull, but not @Nullable, as that is usually not useful to Java devs
    if (displayLanguage == Language.JAVA) {
        injectedAnnotations += nullability.renderAsJavaAnnotation()
    }

    return (this + injectedAnnotations).filterNotNull().filter { annotation ->
        shouldDocumentAnnotation(annotation, displayLanguage, nullability)
    }.distinctBy { it.identifier }.map { annotation -> annotation.toDackkaAnnotation(pathProvider) }
}

internal fun String?.orNull() = if (this == "") null else this

internal val DRI.fullName: String get() = (packageName.orNull()?.let { "$it." }) + classNames
internal val Annotation.identifier: String get() = "${dri.fullName}(${params.values.map { "$it" }})"

internal val AT_NULLABLE = Annotation(DRI("androidx.annotation", "Nullable"), emptyMap())
internal val AT_NON_NULL = Annotation(DRI("androidx.annotation", "NonNull"), emptyMap())

private fun Annotation.toDackkaAnnotation(pathProvider: FilePathProvider): AnnotationComponent {
    val type = pathProvider.linkForReference(dri)
    val params = params.map { (name, contents) -> contents.toComponent(name, pathProvider) }
    return DefaultAnnotationComponent(AnnotationComponent.Params(type, params))
}

/** @return true if an `@Nullable` annotation is present, false otherwise */
internal fun List<Annotation>.hasAtNullable(): Boolean =
    any { it.dri.classNames == "Nullable" || it.isBadNullable }
/** @return true if an `@NonNull` annotation is present, false otherwise */
internal fun List<Annotation>.hasAtNonNull(): Boolean =
    any { it.dri.classNames == "NonNull" || it.isBadNonNull }
/** @return true if the `@Deprecated` annotation is present, false otherwise */
internal fun List<Annotation>.isDeprecated(): Boolean = any { it.isDeprecated() }

/** We sometimes convert androidx annotations to the android. namespace */
private val Annotation.isBadNullable get() = dri.classNames == "Nullable" &&
    dri.fullName !in listOf(AT_NULLABLE.dri.fullName, "android.annotation.Nullable")
private val Annotation.isBadNonNull get() = dri.classNames == "NotNull" ||
    (
        dri.classNames == "NonNull" &&
            dri.fullName !in listOf(AT_NON_NULL.dri.fullName, "android.annotation.NonNull")
        )

/** @return the complete list of annotations for this type */
private fun WithExtraProperties<*>.annotations(sourceSet: DokkaConfiguration.DokkaSourceSet) =
    extra.allOfType<Annotations>().flatMap { annotations ->
        annotations.directAnnotations[sourceSet] ?: emptyList()
    }

internal fun Documentable.annotations(sourceSet: DokkaConfiguration.DokkaSourceSet) =
    (this as? WithExtraProperties<*>)?.annotations(sourceSet) ?: emptyList()

internal fun Projection.annotations(sourceSet: DokkaConfiguration.DokkaSourceSet) =
    (this as? Bound)?.annotations(sourceSet)
        ?: (this as? WithExtraProperties<*>)?.annotations(sourceSet) ?: emptyList()

internal fun WithExtraProperties<*>.sourceSetIndependentAnnotations(): List<Annotation> =
    extra.allOfType<Annotations>()
        .strictSingleOrNull()?.directAnnotations?.values?.firstOrNull() ?: emptyList()

private fun Bound.annotations(sourceSet: DokkaConfiguration.DokkaSourceSet): List<Annotation> =
    when (this) {
        is TypeParameter, is GenericTypeConstructor, is FunctionalTypeConstructor ->
            (this as WithExtraProperties<*>).annotations(sourceSet)
        is Nullable -> this.inner.annotations(sourceSet)
        is TypeAliased -> this.inner.annotations(sourceSet)
        is PrimitiveJavaType, Void, is JavaObject, Dynamic, is UnresolvedBound -> emptyList()
        is DefinitelyNonNullable -> this.inner.annotations(sourceSet).filter { it != AT_NULLABLE }
    }

internal fun WithExtraProperties<*>.allAnnotations() =
    extra.allOfType<Annotations>().flatMap { annotations ->
        annotations.directAnnotations.values
    }.flatten()

// TODO(KMP per-sourceset variance of deprecation status b/262711247)
internal fun Documentable.deprecationAnnotation() = annotations(getExpectOrCommonSourceSet())
    .deprecationAnnotation()
internal fun List<Annotations.Annotation>.deprecationAnnotation() =
    filter { it.isDeprecated() }.strictSingleOrNull()

/**
 * All existing WithSources are WithExtraProperties, and fileLevelAnnotations require sources.
 * @return the list of file-level annotations on this WithSource's source file
 */
internal fun <T> T.fileLevelAnnotations(sourceSet: DokkaConfiguration.DokkaSourceSet)
where T : WithSources, T : Documentable =
    (this as WithExtraProperties<*>).extra.allOfType<Annotations>().flatMap { annotations ->
        annotations.fileLevelAnnotations[sourceSet]
            ?: emptyList()
    }

/** @return true if the `@Deprecated` annotation is present, false otherwise */
internal fun Annotation.isDeprecated(): Boolean = dri.classNames == "Deprecated"

/** @return true if a developer would find this annotation useful, false otherwise */
private fun shouldDocumentAnnotation(
    annotation: Annotation,
    displayLanguage: Language,
    nullability: Nullability
): Boolean {
    val name = annotation.dri.classNames
    // Not useful to developers
    val isSuppressAnnotation = name in SUPPRESSION_ANNOTATION_NAMES
    val isKotlinJvmAnnotation = annotation.dri.packageName == "kotlin.jvm"
    val isExplicitlyBannedAnnotation = name in EXPLICITLY_BANNED_ANNOTATION_NAMES ||
        (displayLanguage == Language.KOTLIN && name in EXPLICITLY_BANNED_ANNOTATIONS_IN_KOTLIN)
    if (isSuppressAnnotation || isKotlinJvmAnnotation || isExplicitlyBannedAnnotation) return false
    // Surfaced separately
    if (annotation.isDeprecated()) return false

    if (name in NULLABILITY_ANNOTATION_NAMES) {
        // Ignored and overwritten with androidx.annotation.NonNull
        if (annotation.isBadNullable || annotation.isBadNonNull) return false
        // Explicitly hidden nullability annotations
        if (nullability == Nullability.DONT_CARE) return false
        // Nullability annotations do not appear in Kotlin, even if explicit in Kotlin source
        if (displayLanguage == Language.KOTLIN) return false
    }

    return !hasBeenHidden(annotation.dri)
}

internal fun Annotation.belongsOnReturnType() =
    dri.classNames in NULLABILITY_ANNOTATION_NAMES || dri.classNames?.shouldBeTypebound() ?: false

private val SUPPRESSION_ANNOTATION_NAMES = listOf("Suppress", "SuppressWarnings", "SuppressLint")
// We transform javax.validation.constraints.NotNull into androidx.annotation.NonNull and WARN:
internal val NULLABILITY_ANNOTATION_NAMES = listOf("NonNull", "Nullable", "NotNull")

private val EXPLICITLY_BANNED_ANNOTATION_NAMES = listOf(
    // This information is compose runtime implementation details; not useful for most
    // and those who would want it should be looking at source
    "Stable", "Immutable", "ReadOnlyComposable",
    // This opt-in requirement is non-propagating so developers don't need to know about it
    // https://kotlinlang.org/docs/opt-in-requirements.html#non-propagating-opt-in
    "OptIn",
    // This annotation is used mostly in paging, and was removed at the request of the paging team
    "CheckResult",
    // This annotation is apparently generated upstream. Dokka uses it for signature serialization
    "ParameterName", // It doesn't seem to be useful for developers
    // This annotations is not useful for developers but right now is @ShowAnnotation?
    "JsName",
    // This annotation is intended to target the compiler and is general not useful for devs
    "Override"
)
private val EXPLICITLY_BANNED_ANNOTATIONS_IN_KOTLIN = listOf(
    "ExtensionFunctionType"
)
// List of androidx annotations that (now that we are on Java 8) ideally would be migrated
// ANNOTATION_TARGET.METHOD -> ANNOTATION_TARGET.TYPE. If on a function, they refer to return type
private val KNOWN_TYPEBOUND_ANNOTATION_NAMES = listOf("Dimension", "Px", "Size")
// For androidx annotations. E.g. IntRes, IntRange, GravityInt, HalfFloat, ColorLong, UiContext
private val KNOWN_TYPEBOUND_ANNOTATION_SUFFIXES =
    listOf("Res", "Range", "Long", "Int", "Float", "Context")
private fun String.shouldBeTypebound() =
    finalWord() in KNOWN_TYPEBOUND_ANNOTATION_SUFFIXES || this in KNOWN_TYPEBOUND_ANNOTATION_NAMES

private fun String.finalWord(): String {
    val lastIndexOfCapital = lastOrNull { it.isUpperCase() }
        ?.let { indexOf(it) } ?: 0
    return substring(lastIndexOfCapital)
}

internal fun AnnotationParameterValue.toComponent(
    name: String? = null,
    pathProvider: FilePathProvider
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
            innerAnnotationParameters = value.map { it.toComponent(pathProvider = pathProvider) }
        )
    )
    is AnnotationValue -> DefaultAnnotationValueAnnotationParameter(
        AnnotationValueAnnotationParameter.Params(
            name,
            annotationComponentValue = annotation.toDackkaAnnotation(pathProvider)
        )
    )
}

internal fun AnnotationParameterValue?.asString() = when (this) {
    null -> ""
    is StringValue -> value
    is EnumValue -> enumName
    is ClassValue -> className
    is LiteralValue -> text()
    is AnnotationValue -> annotation.toString()
    is ArrayValue -> value.toString()
}

internal fun Annotation.nameAsString(): String = params["name"].asString()
