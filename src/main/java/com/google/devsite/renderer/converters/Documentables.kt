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

import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithChildren

/** Recursively expands all children. */
internal val <T> WithChildren<T>.explodedChildren: List<T>
    get() = children + children.filterIsInstance<WithChildren<T>>().flatMap { it.explodedChildren }

/**
 * Returns the type's name. Do not use [Documentable.name] as it won't include the outer class.
 */
internal fun DClasslike.name() = dri.classNames!!

internal fun DClasslike.packageName() = dri.packageName!!

internal fun DModule.sortedPackages() = packages.sortedBy { it.name }

internal fun DPackage.classlikes() =
    explodedChildren.filterIsInstance<DClasslike>().sortedBy { it.name() }

internal fun DPackage.classes() =
    explodedChildren.filterIsInstance<DClass>().sortedBy { it.name() }

internal fun DPackage.enums() =
    explodedChildren.filterIsInstance<DEnum>().sortedBy { it.name() }

internal fun DPackage.interfaces() =
    explodedChildren.filterIsInstance<DInterface>().sortedBy { it.name() }

internal fun DPackage.annotations() =
    explodedChildren.filterIsInstance<DAnnotation>().sortedBy { it.name() }

internal fun DPackage.typeAliases() = typealiases.sortedBy { it.name }

internal fun DPackage.exceptions() = classes().filter { clazz ->
    clazz.functions.any { function -> function.dri.classNames == "Throwable" }
}

internal fun DPackage.topLevelConstants() =
    properties.filter { isConstant(it.modifiers()) }.sortedBy { it.name }

internal fun DPackage.topLevelProperties() = properties
    .filterNot { isConstant(it.modifiers()) }
    .filter { it.receiver == null }
    .sortedBy { it.name }

internal fun DPackage.topLevelFunctions() =
    functions.filter { it.receiver == null }.sortedBy { it.name }

internal fun DPackage.extensionProperties() =
    properties.filterNot { it.receiver == null }.sortedBy { it.name }

internal fun DPackage.extensionFunctions() =
    functions.filterNot { it.receiver == null }.sortedBy { it.name }
