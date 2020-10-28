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
import org.jetbrains.dokka.base.transformers.documentables.isException
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.Variance
import org.jetbrains.dokka.model.WithChildren

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

/** @return true if this is a nullable type, false otherwise */
internal fun Projection.isNullable(): Boolean = when (this) {
    is Nullable -> true
    is Variance<*> -> inner.isNullable()
    else -> false
}

/**
 * @param displayLanguage the Language of the docs this Documentable will be displayed in
 * @return the String name that represents this type when displayed
 */
fun Documentable.stringForType(displayLanguage: Language): String = when (this) {
    is DClass -> "class"
    is DInterface -> "interface"
    is DEnum -> "enum"
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
    else -> error("Unsupported type: $this")
}

/* Returns if a class is an Exception or not
   isException, the built in method in Dokka, only considers its supertype so we also look for
   functions that are Throwable
   https://github.com/Kotlin/dokka/issues/1557
 */
val DClass.isExceptionClass: Boolean
    get() = isException || functions.any { function -> function.dri.classNames == "Throwable" }
