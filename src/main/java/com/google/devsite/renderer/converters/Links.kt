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

import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.JavaClassReference
import org.jetbrains.dokka.links.Nullable
import org.jetbrains.dokka.links.RecursiveType
import org.jetbrains.dokka.links.StarProjection
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.links.TypeParam
import org.jetbrains.dokka.links.TypeReference

/**
 * Returns the anchor for a symbol, without the leading #.
 *
 * The default format is in Java 12 style: `foo(Foo,Bar)`.
 */
internal fun Callable.anchor(
    open: String = "(",
    separator: String = ",",
    close: String = ")",
): String {
    val receiverStr = if (receiver == null) "" else "$open${receiver!!.name()}$close."
    val signature = params.joinToString(separator) { it.name() }
    return "$receiverStr$name$open$signature$close"
}

private fun TypeReference.name(): String = when (this) {
    is JavaClassReference -> name
    is TypeConstructor -> fullyQualifiedName
    is Nullable -> wrapped.name()
    // Parameters aren't used in kdoc links, so this is only relevant for Java linking to Kotlin
    // Anything with multiple bounds in Java code is linked as the first bound....
    // (see "simple" integration test Fraggy#createType)
    is TypeParam -> bounds.first().name()
    is RecursiveType, StarProjection -> ""
}
