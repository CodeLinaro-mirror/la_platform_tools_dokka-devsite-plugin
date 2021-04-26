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

import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.renderer.impl.paths.FilePathProvider
import com.google.devsite.renderer.impl.paths.PACKAGE_SUMMARY_NAME
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.JavaClassReference
import org.jetbrains.dokka.links.Nullable
import org.jetbrains.dokka.links.RecursiveType
import org.jetbrains.dokka.links.StarProjection
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.links.TypeParam
import org.jetbrains.dokka.links.TypeReference

/** @see forReference */
internal fun FilePathProvider.linkForReference(dri: DRI): Link {
    val ref = forReference(dri)
    return DefaultLink(Link.Params(ref.name, ref.url))
}

/**
 * Creates a deep link to a symbol or type. Links to packages, class-likes, top-level/extension
 * functions, and symbols within a type are supported.
 */
internal fun FilePathProvider.forReference(dri: DRI): ReferencePath {

    val packageName = dri.packageName.orEmpty().ifBlank { "[JVM root]" }
    val className = dri.classNames
    val symbol = dri.callable

    // if the DokkaLocationProvider can resolve the dri, then we accept that
    locationProvider?.resolve(dri)?.let {
        val text = symbol?.name ?: className ?: packageName
        return ReferencePath(text, it)
    }

    val (typeName, typeUrl) = if (className == null) {
        packageName to forType(packageName, PACKAGE_SUMMARY_NAME)
    } else {
        className to forType(packageName, className)
    }

    // Exclude specific packages from being linked.
    // In the future we might want to only link to things we *know* we've generated docs for by
    // passing around a collection of valid locations but that could have performance implications
    if (nonDocumentablePackages.getOrDefault(packageName, false)) {
        return ReferencePath(typeName, "")
    }

    return if (symbol == null) {
        ReferencePath(typeName, typeUrl)
    } else {
        ReferencePath(symbol.name, "$typeUrl#${symbol.anchor()}")
    }
}

/**
 * Returns the anchor for a symbol, without the leading #.
 *
 * The default format is in Java 12 style: `foo(Foo,Bar)`.
 */
internal fun Callable.anchor(
    open: String = "(",
    separator: String = ",",
    close: String = ")"
): String {
    val receiverStr = if (receiver == null) "" else "$open${receiver!!.name()}$close."
    val signature = params.joinToString(separator) { it.name() }
    return "$receiverStr$name$open$signature$close"
}

private fun TypeReference.name(): String = when (this) {
    is JavaClassReference -> name
    is TypeConstructor -> fullyQualifiedName
    is Nullable -> wrapped.name()
    is TypeParam -> bounds.single().name()
    is RecursiveType, StarProjection -> ""
}

internal data class ReferencePath(val name: String, val url: String)

private val nonDocumentablePackages = listOf(
    "kotlin.jvm.functions"
).associateWith { true }
