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

package com.google.devsite

import java.util.Locale

/** Enables calling `!nullableBool ?: false` rather than a built-in less readable alternative. */
internal operator fun Boolean?.not() = this?.let { !it }

internal fun String.startsWithAnyOf(prefixes: List<String>) = prefixes.any { this.startsWith(it) }

internal fun String.capitalize() = this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}

/** The same as joinToString but doesn't print prefix or postfix if the list is empty. */
internal fun <T> Collection<T>.joinMaybePrefix(
    prefix: String = "",
    postfix: String = "",
    separator: CharSequence = ", ",
    transform: ((T) -> CharSequence)? = null
) = if (this.isEmpty()) ""
else joinToString(prefix = prefix, postfix = postfix, separator = separator, transform = transform)

/** Performs an outer/tensor product. tensorOf((a,b), (c,d)) is ((a,c), (a,d), (b,c), (b,d)) */
fun <T> tensorOf(vararg lists: Iterable<T>): List<List<T>> =
    lists.fold(listOf(listOf())) { accumulated, nextDimension ->
        accumulated.flatMap { aSlice -> nextDimension.map { element -> aSlice + element } }
    }
/** Version of tensorOf that uses arrays. Because Kotlin's slices and list/array dance are silly. */
inline fun <reified T> tensorOf(vararg arrays: Array<T>): Array<Array<T>> =
    tensorOf(*(arrays.map { it.asIterable() }.toTypedArray()))
        .map { it.toTypedArray() }.toTypedArray()
