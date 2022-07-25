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

import com.google.devsite.capitalize
import com.google.devsite.not
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Memoizers.isFromJavaMap
import com.google.devsite.renderer.impl.ClassGraph
import com.google.devsite.startsWithAnyOf
import org.jetbrains.dokka.analysis.PsiDocumentableSource
import org.jetbrains.dokka.base.transformers.documentables.isException
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.BooleanConstant
import org.jetbrains.dokka.model.ComplexExpression
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeAlias
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.DoubleConstant
import org.jetbrains.dokka.model.Expression
import org.jetbrains.dokka.model.ExtraModifiers
import org.jetbrains.dokka.model.FloatConstant
import org.jetbrains.dokka.model.IntegerConstant
import org.jetbrains.dokka.model.KotlinModifier
import org.jetbrains.dokka.model.KotlinVisibility
import org.jetbrains.dokka.model.Modifier
import org.jetbrains.dokka.model.StringConstant
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Visibility
import org.jetbrains.dokka.model.WithAbstraction
import org.jetbrains.dokka.model.WithChildren
import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.WithSupertypes
import org.jetbrains.dokka.model.WithVisibility
import org.jetbrains.dokka.model.isJvmName
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.model.toAdditionalModifiers
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** For use when generating error messages. Is slow. */
internal fun <T> T.getErrorLocation(): String where T : WithSources, T : Documentable {
    val sourceFilePath = this.sources.values.single().path
    val result = "in declaration of $name in file $sourceFilePath"
    // Regex that matches the declaration of `this`
    val matcher = when (sourceFilePath.substringAfterLast(".")) {
        "kt" ->
            """(fun|val|var|class|interface|enum|object) (<*> )?([a-zA-Z_0-9]+\.(<*>)?)?$name"""
                .toRegex()
        "java" ->
            (
                """(public|protected) (static |final )*""" +
                    """(class |enum |(@)?interface )?[a-zA-Z_0-9]+(<*>)? $name"""
                ).toRegex()
        "class" -> return result // this type's source is in a prebuilt?
        else -> {
            // This means the error occurred while parsing a synthetic element
            if ("org.jetbrains.kotlin.descriptors" in sourceFilePath) return result
            else throw RuntimeException("Unknown file type for $sourceFilePath")
        }
    }
    // Assume that the type params can't take up more than 3 lines
    File(sourceFilePath).readLines().windowed(size = 3, step = 1).forEachIndexed { index, lines ->
        if (matcher.containsMatchIn(lines.joinToString())) {
            return "$result at line ${index + 2}." // The last line in the window
        }
    }
    // Multiple possible reasons for failure. For example, java syntax does not lend itself to
    // allowing declaractions to be identified by regex, so there is a good chance it could fail.
    return "$result, line number could not be determined."
}

@JvmName("This is internal and will never be used from JVM")
internal fun Documentable.getErrorLocation() = if (this is WithSources) this.getErrorLocation()
else "File location could not be determined."

/** Recursively expands all children. */
internal val <T> WithChildren<T>.explodedChildren: List<T>
    get() = children + children.filterIsInstance<WithChildren<T>>().flatMap { it.explodedChildren }

/**
 * Returns the type's name. Do not use [Documentable.name] as it won't include the outer class.
 */
internal fun DClasslike.name() = dri.classNames!!

internal fun DClasslike.generics() = (this as? WithGenerics)?.generics ?: emptyList()

internal fun DClasslike.hasSupertypes(classGraph: ClassGraph) =
    if (this !is WithSupertypes) false else {
        classGraph.getValue(dri).superClasses.isNotEmpty() ||
            classGraph.getValue(dri).interfaces.isNotEmpty()
    }

internal fun DClasslike.packageName() = dri.packageName!!

private val baseClasses = listOf(
    "kotlin.Any", "java.lang.Object", "kotlin.Enum",
    "java.lang.Enum", "java.lang.annotation.Annotation"
)
/**
 * Returns true if this dri is from a build in base class like Any, Object, Enum, Annotation
 */
internal fun DRI.isFromBaseClass(): Boolean {
    val classAndPackage = packageName?.plus(".").plus(classNames)
    return baseClasses.contains(classAndPackage)
}

private object Memoizers {
    val isFromJavaMap: ConcurrentHashMap<Hashable, Boolean> =
        ConcurrentHashMap<Hashable, Boolean>()
}

/** go/dokka-upstream-bug/2620. Because Documentables aren't remotely efficiently hashable. */
internal data class Hashable(
    val clazz: Class<out WithSources>,
    val isSynthetic: Boolean?,
    val dri: DRI?,
    val visibility: Collection<Visibility>,
    val modifiers: Collection<Modifier>,
    val isPsi: Boolean?
)

private fun WithSources.toHashable() = Hashable(
    clazz = this::class.java,
    isSynthetic = (this as? DClasslike)?.isSynthetic,
    dri = if (this is Documentable) this.dri else null,
    visibility = if (this is WithVisibility) this.visibility.values else emptyList(),
    modifiers = if (this is WithAbstraction) this.modifier.values else emptyList(),
    isPsi = this.sources.entries.singleOrNull()?.value is PsiDocumentableSource
)

/**
 * Infer whether this Documentable is from java source,
 * and thus whether it's nullable if not annotated.
 * Memoized.
 */
internal fun WithSources.isFromJava() = this.toHashable().isFromJava()
private fun Hashable.isFromJava() =
    isFromJavaMap.getOrPut(this) {
        if (isSynthetic == true) false
        else if (visibility.isNotEmpty()) !visibility.any { it is KotlinVisibility }
        else if (modifiers.isNotEmpty()) !modifiers.any { it is KotlinModifier }
        else isPsi == true
    }

internal fun Documentable.isJavaStaticField() = this is DProperty && run {
    val modifiers = modifiers()
    "const" in modifiers || "lateinit" in modifiers || isStaticAnnotated()
}

internal fun Documentable.isJavaStaticMethod() = this is DFunction &&
    (isStaticAnnotated() || isStaticAccessor())

internal fun DFunction.isStaticAccessor() = false
// extra[OriginalProperty]?.original?.isStaticAnnotated() ?: false TODO(b/168340963 accessors)

internal fun Documentable.isStaticAnnotated() =
    annotations().any { it.dri == JvmStatic.dri }

private val INTERNAL_PACKAGES = listOf("java", "Kotlin", "google", "android")
internal fun DRI.isExternal() = !packageName?.startsWithAnyOf(INTERNAL_PACKAGES) ?: true

/**
 * @param displayLanguage the Language of the docs this Documentable will be displayed in
 * @return the String name that represents this type when displayed
 */
fun Documentable.stringForType(displayLanguage: Language): String = when (this) {
    is DClass -> "class"
    is DInterface -> "interface"
    is DEnum -> "enum"
    is DEnumEntry -> "enum value"
    is DAnnotation -> "annotation"
    is DFunction -> when (displayLanguage) {
        Language.JAVA -> "method"
        Language.KOTLIN -> "function"
    }
    is DProperty -> when (displayLanguage) {
        Language.JAVA -> "field"
        Language.KOTLIN -> "property"
    }
    is DObject -> when (displayLanguage) {
        Language.KOTLIN -> "object"
        Language.JAVA -> "class"
    }
    is DTypeAlias -> "type alias"
    is DParameter -> "parameter"
    else -> error("Unsupported type: $this")
}

/**
 * Returns if a class is an Exception or not
 * isException, the built-in method in Dokka, only considers its supertype, so we also look for
 * functions that are Throwable
 * https://github.com/Kotlin/dokka/issues/1557
 */
val DClass.isExceptionClass: Boolean
    get() = isException || functions.any { function -> function.dri.classNames == "Throwable" }

/**
 * Returns whether the java class was synthetically generated from a Kotlin extension function class
 * Assumes the class this is being called on is Java.
 */
val DClasslike.isSynthetic: Boolean
    get() = name().endsWith("Kt") || this.jvmFileName() != null

/**
 * Converts a top level function to its representation under a Java synthetic class
 * and with JvmName
 * Replaces the dri to point to the synthetic class and applies the static modifier
 */
fun DFunction.withJavaSynthetic(syntheticClassName: String): DFunction {
    val jvmName = jvmName() ?: name
    return copy(
        name = jvmName,
        // this needs to be the dri IN the synthetic class
        dri = dri.copy(
            classNames = syntheticClassName,
            callable = dri.callable?.copy(name = jvmName)
        ),
        // put the static modifier on functions in the synthetic class
        extra = extra.addAll(
            sourceSets.map {
                mapOf(it to setOf(ExtraModifiers.JavaOnlyModifiers.Static)).toAdditionalModifiers()
            }
        )
    )
}

/**
 * Converts a level function to its presentation with JvmName
 */
fun DFunction.withJvmName(): DFunction {
    val jvmName = jvmName() ?: return this
    return copy(
        name = jvmName,
        dri = dri.copy(callable = dri.callable?.copy(name = jvmName))
    )
}

internal fun DFunction.matches(other: DFunction): Boolean =
    this.receiver == other.receiver &&
        this.parameters == other.parameters &&
        this.dri.packageName == other.dri.packageName &&
        this.jvmName() == other.jvmName()

/**
 * [Comparator] which sorts [DFunction] by name, then number of params, and then params names if
 * necessary.
 */
fun functionSignatureComparator(): Comparator<DFunction> = compareBy(
    { it.name },
    { it.parameters.size },
    { it.signatureAsString() }
)

private fun DFunction.signatureAsString() =
    "$name(${parameters.joinToString(separator = ", ") { it.paramAsString() }})"

private fun DParameter.paramAsString() =
    "${name ?: ""}: " +
        "${(type as? UnresolvedBound)?.name ?: (type as? TypeConstructor)?.dri?.classNames}"

/**
 * Returns the value of the @JvmName for this function if one exists or null
 */
fun Documentable.jvmName(): String? {
    return annotations().firstOrNull { it.isJvmName() }?.nameAsString()
}

/**
 * Returns the value of the file:@JvmName if one exists or null
 */
fun WithSources.jvmFileName(): String? {
    return fileLevelAnnotations().firstOrNull { it.isJvmName() }?.nameAsString()
}

/**
 * Returns the value of the file:@JvmName if one exists or null
 */
fun nameForSyntheticClass(entry: WithSources): String {
    return entry.jvmFileName() ?: entry.sources.let {
        it.entries.first().value.path.split("/").last().split(".").first() + "Kt"
    }
}

fun DFunction.driForSyntheticClass() = DRI(dri.packageName, nameForSyntheticClass(this))

/**
 * Filters out elements that are annotated with @JvmSynthetic
 */
fun <T : Documentable> List<T>.filterOutJvmSynthetic(): List<T> = this.filterNot { elem ->
    elem.annotations().any { it.dri.classNames.equals("JvmSynthetic") }
}

/** Adds an annotation to a Documentable. Often used for injecting e.g. @JvmStatic. */
internal fun <T> PropertyContainer<T>.addAnnotation(newA: Annotations.Annotation):
    PropertyContainer<T>
    where T : WithExtraProperties<T>, T : AnnotationTarget {
    val annotationsWithoutJvmName = get(Annotations)?.let { annotations ->
        annotations.copy(
            (annotations.directAnnotations).map { (sourceset, annotations) ->
                sourceset to (annotations + newA)
            }.toMap() + annotations.fileLevelAnnotations
        )
    }
    val extraWithoutAnnotations: PropertyContainer<T> = minus(Annotations)

    return extraWithoutAnnotations.addAll(listOfNotNull(annotationsWithoutJvmName))
}

internal val JvmStatic = Annotations.Annotation(DRI("kotlin.jvm", "JvmStatic"), params = emptyMap())

internal fun DRI.possiblyConvertMappedType(displayLanguage: Language) =
    when (displayLanguage) {
        Language.JAVA -> possiblyAsJava()
        Language.KOTLIN -> possiblyAsKotlin()
    }

/**
 * Uses the JavaToKotlinClassMap to possibly convert a dri to its Java equivalent
 * https://kotlinlang.org/docs/reference/java-interop.html#mapped-types
 */
internal fun DRI.possiblyAsJava(): DRI {
    val fullyQualifiedName = packageName?.let { "$it." } + classNames
    // Use the fully qualified name to look up the class in the map
    return JavaToKotlinClassMap.mapKotlinToJava(FqName(fullyQualifiedName).toUnsafe())?.let {
        DRI(
            packageName = it.packageFqName.asString(),
            classNames = it.classNames(),
            callable = this.callable,
            extra = null,
            target = PointingToDeclaration
        )
    } ?: this
}
/**
 * Uses the JavaToKotlinClassMap to possibly convert a dri to its Kotlin equivalent
 * https://kotlinlang.org/docs/reference/java-interop.html#mapped-types
 */
internal fun DRI.possiblyAsKotlin(): DRI {
    val fullyQualifiedName = packageName?.let { "$it." } + classNames
    // Use the fully qualified name to look up the class in the map
    return JavaToKotlinClassMap.mapJavaToKotlin(FqName(fullyQualifiedName))?.let {
        DRI(
            packageName = it.packageFqName.asString(),
            classNames = it.classNames(),
            callable = this.callable,
            extra = null,
            target = PointingToDeclaration
        )
    } ?: this
}

private fun ClassId.classNames(): String =
    generateSequence(this) { it.outerClassId }
        .map { it.shortClassName.identifier }
        .reduce { acc, pref -> "$pref.$acc" }

/**
 * Returns the string representation of an [Expression] value, mostly relying on the toString
 * implementation of that type, but wrapping [String] values in quotes for presentation, and
 * stripping trailing zeros and appending 'f' or 'd' to floats and doubles respectively.
 */
fun Expression.getValue(): String? = when (this) {
    is ComplexExpression -> value
    is IntegerConstant -> "$value"
    is BooleanConstant -> "$value"
    is StringConstant -> "\"$value\""
    is DoubleConstant -> "$value"
    is FloatConstant -> "${value}f"
    else -> null
}

/**
 * Returns property getters / setters. Omits generated Kotlin getters and setters (which can be
 * identified by looking for a callable name like <get-foo> or <set-bar>) unless explicitly allowed.
 */
fun List<DProperty>.gettersAndSetters(allowDefault: Boolean = false): List<DFunction> {
    return flatMap {
        listOf(it.getter, it.setter)
    }.map {
        val callableName = it?.dri?.callable?.name ?: ""
        if (callableName.startsWith("<get-") || callableName.startsWith("<set-"))
            if (allowDefault) it!!.withFixedName()
            else null
        else it
    }.filterNotNull()
}

/** Fixes the name of synthetic accessors, e.g. <get-bar> to getBar */
private fun DFunction.withFixedName() = copy(
    dri = dri.copy(
        callable = dri.callable!!.copy(
            name = fixCallableName(dri.callable?.name ?: "")
        )
    )
)

private fun fixCallableName(badName: String) = when {
    badName.startsWith("<get-") ->
        "get" + badName.removePrefix("<get-").removeSuffix(">").capitalize()
    badName.startsWith("<set-") ->
        "set" + badName.removePrefix("<set-").removeSuffix(">").capitalize()
    else -> throw RuntimeException("This should never happen; error fixing accessor name")
}

private fun DRI.isAtJvmField(): Boolean = packageName == "kotlin.jvm" && classNames == "JvmField"

private fun Annotations.Annotation.isAtJvmField(): Boolean = dri.isAtJvmField()
internal fun DProperty.isJvmFieldAnnotated() =
    annotations().any { it.isAtJvmField() }

/**
 * Returns whether property is annotated as @JvmField
 */
fun DProperty.isJvmField(): Boolean {
    return isJvmFieldAnnotated() || "const" in modifiers()
}

internal fun List<DFunction>.names() = map { it.name }
@JvmName("internalAndThusKotlinOnly")
internal fun List<DParameter>.names() = map { it.name }
@JvmName("internalAndThusKotlinOnlyAlso")
internal fun List<DProperty>.names() = map { it.name }
