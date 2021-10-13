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

import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Memoizers.isFromJavaMap
import org.jetbrains.dokka.base.transformers.documentables.isException
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
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
import org.jetbrains.dokka.model.StringConstant
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.WithAbstraction
import org.jetbrains.dokka.model.WithChildren
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.WithVisibility
import org.jetbrains.dokka.model.isJvmName
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.model.toAdditionalModifiers
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Recursively expands all children. */
internal val <T> WithChildren<T>.explodedChildren: List<T>
    get() = children + children.filterIsInstance<WithChildren<T>>().flatMap { it.explodedChildren }

/**
 * Returns the type's name. Do not use [Documentable.name] as it won't include the outer class.
 */
internal fun DClasslike.name() = dri.classNames!!

internal fun DClasslike.packageName() = dri.packageName!!

private val baseClasses = listOf("kotlin.Any", "java.lang.Object", "kotlin.Enum",
    "java.lang.Enum", "java.lang.annotation.Annotation")
/**
 * Returns true if this dri is from a build in base class like Any, Object, Enum, Annotation
 */
internal fun DRI.isFromBaseClass(): Boolean {
    val classAndPackage = packageName?.plus(".").plus(classNames)
    return baseClasses.contains(classAndPackage)
}

private object Memoizers {
    val isFromJavaMap: ConcurrentHashMap<Documentable, Boolean> =
        ConcurrentHashMap<Documentable, Boolean>()
}

/**
 * Infer whether this Documentable is from java source,
 * and thus whether it's nullable if not annotated.
 * Memoized.
 */
internal fun Documentable.isFromJava() = isFromJavaMap.getOrPut(this) {
    if (this is WithVisibility && this.visibility.isNotEmpty())
        !visibility.values.any { it is KotlinVisibility }
    if (this is WithAbstraction && this.modifier.isNotEmpty())
        !modifier.values.any { it is KotlinModifier }
    val sourceFileExtensions = getPossibleSourceFiles().map { it.path }
        .filter { "package-info.java" !in it }
        .map { it.substringAfterLast('.') }
    when {
        // No Java files -> default is NonNull      (this bypasses e.g. .xml/.gradle)
        sourceFileExtensions.all { it !in listOf("java", "class") } -> false
        // No Kotlin files -> default is nullable
        sourceFileExtensions.all { it !in listOf("kt") } -> true
        // We don't know. Default to not injecting @NonNull (the primary use of isFromJava)
        else -> true // (i.e. do not make the strict NonNull assumption for unspecified types)
    }
}

private fun DRI.isExternal() = packageName != null &&
    (packageName!!.startsWith("java") || packageName!!.startsWith("Kotlin") ||
    ("google" !in packageName!! && "android" !in packageName!!))

internal fun Documentable.getPossibleSourceFiles(): List<File> {
    val codeFiles = if (this is WithSources) {
        this.sources.entries.map { File(it.value.path) }
    } else {
        sourceSets.map { it.sourceRoots.map { it.getCodeFileDescendants() } }.flatten().flatten()
    }

    if (codeFiles.isEmpty()) throw RuntimeException("No sources found for $dri")
    if (codeFiles.size == 1) return codeFiles
    return codeFiles
}

private fun File.getCodeFileDescendants(): List<File> =
    if (this.extension.toLowerCase() in listOf("java", "kt", "js", "class")) listOf(this)
    else this.listFiles()?.map { it.getCodeFileDescendants() }?.flatten() ?: emptyList()

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
    is DObject -> "object"
    is DTypeAlias -> "type alias"
    is DParameter -> "parameter"
    else -> error("Unsupported type: $this")
}

/**
 * Returns if a class is an Exception or not
 * isException, the built in method in Dokka, only considers its supertype so we also look for
 * functions that are Throwable
 * https://github.com/Kotlin/dokka/issues/1557
 */
val DClass.isExceptionClass: Boolean
    get() = isException || functions.any { function -> function.dri.classNames == "Throwable" }

// TODO(b/173138586) replace with something else when implementing JvmName
val DClasslike.isSynthetic: Boolean
    get() = name().endsWith("Kt")

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
        extra = extra.addAll(sourceSets.map {
            mapOf(it to setOf(ExtraModifiers.JavaOnlyModifiers.Static)).toAdditionalModifiers()
        })
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
fun WithExtraProperties<*>.jvmName(): String? {
    return annotations().firstOrNull { it.isJvmName() }?.nameAsString()
}

/**
 * Returns the value of the file:@JvmName if one exists or null
 */
fun WithExtraProperties<*>.jvmFileName(): String? {
    return fileLevelAnnotations().firstOrNull { it.isJvmName() }?.nameAsString()
}
/**
 * Returns the value of the file:@JvmName if one exists or null
 */
fun <T> nameForSyntheticClass(entry: T): String where T : WithSources, T : WithExtraProperties<*> {
    return entry.jvmFileName() ?: entry.sources.let {
        it.entries.first().value.path.split("/").last().split(".").first() + "Kt"
    }
}

fun DFunction.driForSyntheticClass() = DRI(dri.packageName, nameForSyntheticClass(this))

/**
 * Filters out elements that are annotated with @JvmSynthetic
 */
fun <T> List<T>.filterOutJvmSynthetic(): List<T>
    where T : WithExtraProperties<*> = this.filterNot {
        it.annotations().any { it.dri.classNames.equals("JvmSynthetic") }
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
    shortClassName.identifier + (outerClassId?.classNames()?.let { ".$it" } ?: "")

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
 * Returns property getters / setters. Omits generated Kotlin getters and setters which can be
 * identified by looking for a callable name like <get-foo> or <set-bar>.
 */
fun DClasslike.gettersAndSetters(): List<DFunction> {
    return properties.flatMap {
        listOf(it.getter, it.setter)
    }.filterNotNull().filterNot {
        val callableName = it.dri.callable?.name ?: ""
        callableName.startsWith("<get-") || callableName.startsWith("<set-")
    }
}
