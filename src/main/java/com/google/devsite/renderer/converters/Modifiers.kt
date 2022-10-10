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
import org.jetbrains.dokka.model.AdditionalModifiers
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithAbstraction
import org.jetbrains.dokka.model.WithVisibility
import org.jetbrains.dokka.model.properties.WithExtraProperties

/** @return the complete list of modifiers for this type */
internal fun Documentable.modifiers(): List<String> {
    val result = mutableListOf<String>()
    if (this is WithAbstraction)
        result += this.modifier.values.map { it.name }.filter { it.isNotEmpty() }
    if (this is WithVisibility)
        result += listOf(visibility.values.single().name)
    if (this is WithExtraProperties<*>)
        result += getExtraModifiers()
    return result
}

/**
 *  Returns a list of modifiers stored in the AdditionalModifiers extra field
 *  i.e. VarArg
 */
internal fun <T> T.getExtraModifiers(): List<String>
    where T : WithExtraProperties<*> {
    return extra.allOfType<AdditionalModifiers>().flatMap { modifiers ->
        modifiers.content.values.single().map { it.name }.filter { it.isNotEmpty() }
    }
}

/** @return true if the modifiers represent a constant symbol, false otherwise */
internal fun isConstant(modifiers: List<String>) =
    "const" in modifiers || ("static" in modifiers && "final" in modifiers)

internal open class Modifiers(baselist: List<String>) : ArrayList<String>(baselist) {
    constructor(vararg items: String) : this(items.asList())
}
internal object EmptyModifiers : Modifiers()

/** Returns a filtered and re-written list of modifiers. */
internal fun List<String>.modifiersFor(
    hints: ModifierHints
): Modifiers {
    val modifiers = toMutableSet()

    when (hints.displayLanguage) {
        Language.JAVA -> {
            // Rewrite known modifiers
            if ("const" in modifiers) {
                if (!hints.isProperty) throw RuntimeException("'const' on a ${hints.type}?")
                modifiers.add("static")
                modifiers.add("final") // this happens for `const val`
            }

            // What does @JvmStatic do?
            // It causes the annotated function/property-and-accessors to be hoisted to the
            // containing object. It does _not_ affect the presence of the `static` keyword. All
            // methods that are on or hoisted from companion objects are _always_ static.
            // As such, @JvmStatic needs no handling here--or it wouldn't if we didn't use it as
            // an internal flag to represent that something has been hoisted. It is later converted
            // into injectStatic. (This is convenient because it makes java -> kotlin easy).

            if (hints.injectStatic) {
                modifiers.add("static")
            }

            // `lateinit` on a companion var causes the creation of a hoisted public backing field
            // but _not_ hoisted public accessors.
            // However, this needs no handling here, as given that the compiler performs the
            // hoisting, for the same reason that @JvmStatic does not require special handling.

            // Interface methods are public by default; showing it is not useful
            if (hints.inInterface) {
                modifiers.remove("public")
            }

            // Members of companion objects and top-level objects become static
            // As do top-level elements that get converted to be in *Kt files, even if extensions
            if (hints.inObject || hints.inPackage) {
                modifiers.add("static")
            }

            // Java uses the "default" modifier on interface non-abstract methods
            // but Dokka upstream inverts this to make non-default interface methods "abstract"
            if ("abstract" !in modifiers && hints.inInterface) {
                modifiers.add("default")
            }

            // Java constructors can't be `final`
            if (hints.isConstructor) modifiers.remove("final")

            // These modifiers don't exist in Java
            modifiers.remove("suspend")
            modifiers.remove("inline")
            modifiers.remove("noinline")
            modifiers.remove("crossinline")
            modifiers.remove("reified")
            modifiers.remove("operator")
            modifiers.remove("override")
            modifiers.remove("open")
            modifiers.remove("sealed")
            modifiers.remove("const")
            modifiers.remove("vararg")
        }
        Language.KOTLIN -> {
            if ("static" in modifiers && "final" in modifiers &&
                hints.isProperty && hints.isFromJava
            ) {
                modifiers.remove("static")
                modifiers.remove("final")
                modifiers.add("const")
            }

            // We do not do this because it is usually not useful for Kotlin users of Java code
            // if ("final" !in modifiers) modifiers.add("open")

            // Align default modifiers
            modifiers.remove("public")
            if ("override" !in modifiers) {
                modifiers.remove("final")
            }
            if (hints.inInterface) {
                modifiers.remove("abstract")
            }

            // These modifiers don't exist in Kotlin
            if ("static" in modifiers) {
                modifiers.remove("static")
                if (hints.isFromJava) modifiers.add("java-static")
            }

            // Not useful
            modifiers.remove("override")
        }
    }

    if (hints.isSummary) {
        modifiers.remove("public")
        modifiers.remove("protected")
    }

    return Modifiers(modifiers.toList().sortedBy { m -> modifierOrder.indexOf(m) })
}

/**
 * Kotlin modifier order
 * (from https://kotlinlang.org/docs/reference/coding-conventions.html#modifiers)
 */
val modifierOrder = listOf(
    // visibility (one of)
    "public", "protected", "private", "internal",
    // Multi-platform (one of)
    "expect", "actual",
    // Containing Scope (one of)
    "static", "java-static",
    // Extensibility (one of)
    "final", "open", "abstract", "sealed", "const",
    // Other (could be more than one)
    "external", "override", "lateinit", "tailrec", "vararg", "suspend", "inner",
    // Types (one of)
    "enum", "annotation", "fun",
    // More (could be more than one)
    "companion", "inline", "infix", "operator", "data"
)

/**
 * Provides modifier hints for what should be shown in the documentation.
 *
 * Note: this is an imperfect approximation that won't be correct in all cases, but Dokka doesn't
 * give us a better solution without replicating compiler functionality. The crux of the problem is
 * that Dokka always includes modifiers even if they weren't specified in the code. Example:
 *
 * ```
 * interface Foo { fun bar() }
 * ```
 *
 * The Dokka modifiers will include `abstract`. While technically correct, that's just noise from
 * the compiler for developers. These hints give us a way of saying "look, developers will know this
 * modifier is implicit." That said, we can only go so far without replicating too much compiler
 * functionality. For example:
 *
 * ```
 * abstract Foo { protected abstract fun foo() }
 * class Bar { public override fun foo() }
 * ```
 *
 * Without digging through the hierarchy to see that `foo()` is actually protected by default, we
 * have no way of knowing we should keep the `public` modifier. (Note: this doesn't affect our
 * understanding that the function is public and should go in the "Public functions" section. It
 * just means the signature will be "incorrect" since it doesn't reflect the real source code.)
 *
 * Sometimes we need to inject the static modifier. For example, when we have an @JvmField hoisted
 * from a companion object, it becomes static. Upstream doesn't inject, and it's not from an Object.
 */
internal data class ModifierHints(
    val displayLanguage: Language,
    val type: Class<out Documentable>,
    val containingType: Class<out Documentable>?,
    val isFromJava: Boolean,
    val isSummary: Boolean = false,
    val injectStatic: Boolean = false,
    val isConstructor: Boolean = false
) {
    val inInterface get() = containingType == DInterface::class.java
    val inObject get() = containingType == DObject::class.java
    val inPackage get() = containingType == DPackage::class.java
    val isProperty get() = type == DProperty::class.java
}
