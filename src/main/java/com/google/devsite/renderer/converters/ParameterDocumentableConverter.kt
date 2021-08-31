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
import com.google.devsite.components.impl.DefaultParameter
import com.google.devsite.components.impl.DefaultSymbolType
import com.google.devsite.components.impl.DefaultTypeParameter
import com.google.devsite.components.symbols.Parameter
import com.google.devsite.components.symbols.SymbolBase
import com.google.devsite.components.symbols.SymbolType
import com.google.devsite.components.symbols.TypeParameter as DokkaTypeParameter
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.DTypeParameter
import org.jetbrains.dokka.model.DefaultValue
import org.jetbrains.dokka.model.FunctionalTypeConstructor
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.JavaObject
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.PrimitiveJavaType
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.Star
import org.jetbrains.dokka.model.TypeAliased
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.TypeParameter as UpstreamTypeParameter
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Variance
import org.jetbrains.dokka.model.Void
import java.util.concurrent.ConcurrentHashMap

/** Converts parameter and parameter-likes into their components. */
internal class ParameterDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider
) {
    /**
     * Returns the component for a parameter.
     *
     * When rendering Kotlin, we look at annotations to determine if the type should be considered
     * nullable. When rendering Java, we look at the Dokka type information to determine if the
     * Kotlin type is nullable. Why are these flipped? Because if we were rendering Java with Java
     * sources, we would already have annotations. But rendering Java with Kotlin sources won't have
     * those nullability annotations so we need to look at the Kotlin type. Similarly, rendering
     * Kotlin with Kotlin sources has the nullability type info built in, but rendering Kotlin with
     * Java sources does not.
     */
    fun componentForParameter(
        param: DParameter,
        isSummary: Boolean
    ): Parameter = when (displayLanguage) {
        Language.JAVA -> componentForJavaProjection(
            proj = param.type,
            name = param.name ?: "receiver",
            modifiers = param.getExtraModifiers().modifiersFor(ModifierHints(Language.JAVA)),
            annotations = param.annotations()
        )
        Language.KOTLIN -> {
            val defaultValueExpression = param.extra.allOfType<DefaultValue>().singleOrNull()?.value
                ?.takeUnless { isSummary }
            componentForKotlinProjection(
                proj = param.type,
                name = param.name.orEmpty(),
                defaultValue = defaultValueExpression?.getValue(),
                modifiers = param.getExtraModifiers().modifiersFor(ModifierHints(Language.KOTLIN)),
                annotations = param.annotations()
            )
        }
    }

    fun componentForTypeParameter(
        param: DTypeParameter
    ): DokkaTypeParameter = DefaultTypeParameter(DokkaTypeParameter.Params(
        displayLanguage = displayLanguage,
        name = param.variantTypeParameter.inner.name,
        projections = param.bounds.map { componentForProjection(it, showNullability = false) }
    ))

    /**
     * Returns the component for a type projection.
     *
     * When rendering Java, we do not want to show nullability annotations because this is just a
     * type (e.g. return type), so nullability will be handled elsewhere. When rendering Kotlin,
     * we *do* want to show nullability information since it's built into the type. Thus, we look at
     * annotations in addition to the Dokka Nullable type.
     *
     * @param isReturnType used to determine if Unit return types should be converted to void
     */
    fun componentForProjection(
        proj: Projection,
        annotations: List<Annotations.Annotation> = emptyList(),
        isReturnType: Boolean = false,
        showNullability: Boolean = true
    ): Parameter = when (displayLanguage) {
        Language.JAVA -> componentForJavaProjection(
            proj,
            annotations = annotations,
            isReturnType = isReturnType,
            showNullability = showNullability
        )
        Language.KOTLIN -> componentForKotlinProjection(proj, annotations = annotations)
    }

    private fun Projection.typeIsNullableAtAll() = when (this) {
        is TypeConstructor -> {
            val className = dri.classNames.orEmpty()
            !(dri.packageName == "kotlin" &&
                // kotlin types we convert to java primitives don't get nullability
                (className in kotlinPrimitives ||
                    // Nothing is converted to void, so nullability isn't useful
                    className == "Nothing" ||
                    // Unit can be nullable, but that information is basically always useless
                    className == "Unit"))
        }
        else -> true
    }

    private fun componentForJavaProjection(
        proj: Projection,
        name: String = "",
        modifiers: List<String> = emptyList(),
        annotations: List<Annotations.Annotation> = emptyList(),
        isReturnType: Boolean = false,
        showNullability: Boolean = true
    ): Parameter {
        val nullable = proj.isNullable() || annotations.isNullable()

        return DefaultParameter(
            Parameter.Params(
                isLambda = false,
                name = name,
                type = proj.rewriteKotlinPrimitivesForJava(isReturnType).toComponent(),
                modifiers = modifiers,
                annotationComponents = annotations.annotationComponents(
                    pathProvider,
                    displayLanguage,
                    nullable,
                    showNullability = showNullability && proj.typeIsNullableAtAll()
                ),
                displayLanguage = Language.JAVA
            )
        )
    }

    /**
     * Tries to convert the [Projection] to it's Kotlin equivalent if it exists. Because this method
     * is called thousands of times, we memoize the results in [toKotlinTypeMemo].
     */
    private fun Projection.possiblyAsKotlin(): Projection {
        return toKotlinTypeMemo.getOrPut(this) {
            innerPossiblyAsKotlin()
        }
    }

    private fun Projection.innerPossiblyAsKotlin(): Projection = when (this) {
        is FunctionalTypeConstructor, is GenericTypeConstructor -> {
            (this as TypeConstructor).copy(
                projections = projections.map { it.possiblyAsKotlin() },
                dri = dri.possiblyAsKotlin()
            )
        }
        is Variance<*> -> inner.possiblyAsKotlin()
        else -> this
    }

    private fun componentForKotlinProjection(
        proj: Projection,
        name: String = "",
        defaultValue: String? = null,
        modifiers: List<String> = emptyList(),
        annotations: List<Annotations.Annotation> = emptyList()
    ): Parameter {
        val projKotlin = proj.possiblyAsKotlin()
        val isLambda = projKotlin.isLambda()
        val receiver = projKotlin.receiver()
        val primaryType = if (isLambda) {
            // Get the return type of the lambda
            componentForKotlinProjection(projKotlin.asTypeConstructor().projections.last())
        } else {
            projKotlin.toComponent(nullable = annotations.isNullable())
        }
        val lambdaModifiers: List<String> = if (projKotlin.isSuspend()) {
            listOf("suspend")
        } else {
            emptyList()
        }

        val lambdaParams: List<Parameter> = if (isLambda) {
            // Always ignore the return type of the lambda since that's handled by primaryType.
            val lambdaProjections = projKotlin.asTypeConstructor().projections.dropLast(1)
            if (receiver == null) {
                lambdaProjections.map(::componentForKotlinProjection)
            } else {
                // If the receiver is available, we also ignore the first type
                lambdaProjections.drop(1).map(::componentForKotlinProjection)
            }
        } else {
            emptyList()
        }

        val paramName = if (isLambda && name.isEmpty()) {
            projKotlin.asTypeConstructor().presentableName
        } else {
            name
        } ?: ""

        return DefaultParameter(
            Parameter.Params(
                displayLanguage = Language.KOTLIN,
                isLambda = isLambda,
                name = paramName,
                receiver = receiver,
                lambdaModifiers = lambdaModifiers,
                lambdaParams = lambdaParams,
                modifiers = modifiers,
                type = primaryType,
                annotationComponents = annotations.annotationComponents(
                    pathProvider,
                    displayLanguage,
                    annotations.isNullable()
                ),
                defaultValue = defaultValue
            )
        )
    }

    /** Converts a lambda receiver projection to its type component if available. */
    private fun Projection.receiver(): Parameter? = when (this) {
        is FunctionalTypeConstructor -> if (this.isExtensionFunction) {
            componentForProjection(projections.first())
        } else {
            null
        }

        is GenericTypeConstructor, is UpstreamTypeParameter, is PrimitiveJavaType,
        is UnresolvedBound, is JavaObject, Star, Void -> null
        is Nullable -> inner.receiver()
        is Variance<*> -> inner.receiver()
        is TypeAliased -> inner.receiver()
        else -> error("Unknown bound: $this")
    }

    /** Converts a documentable type to its type component, recursively expanding generics */
    private fun Projection.toComponent(nullable: Boolean = false): SymbolType {
        if (this is Variance<*>) {
            return inner.toComponent()
        }
        if (this is Nullable) {
            return inner.toComponent(nullable = displayLanguage == Language.KOTLIN)
        }

        if (this is TypeAliased) {
            return inner.toComponent()
        }

        val generics: List<SymbolBase> = when (this) {
            is TypeConstructor -> projections.map { componentForProjection(it) }
            is UpstreamTypeParameter, is PrimitiveJavaType, is UnresolvedBound,
            is JavaObject, Star, Void -> emptyList()
            else -> error("Unknown bound: $this")
        }

        return DefaultSymbolType(
            SymbolType.Params(
                displayLanguage = displayLanguage,
                type = toLink(),
                nullable,
                generics
            )
        )
    }

    /**
     * Converts a documentable type to a link component, assuming all generics have been resolved.
     */
    private fun Projection.toLink(): Link = when (this) {
        is TypeConstructor -> pathProvider.linkForReference(dri)
        is UpstreamTypeParameter -> DefaultLink(
            Link.Params(
                name = presentableName ?: name,
                url = ""
            )
        )
        Star -> DefaultLink(
            Link.Params(
                name = when (displayLanguage) {
                    Language.JAVA -> "?"
                    Language.KOTLIN -> "*"
                },
                url = ""
            )
        )
        Void -> when (displayLanguage) {
            Language.JAVA -> DefaultLink(Link.Params(name = "void", url = ""))
            Language.KOTLIN -> pathProvider.linkForReference(DRI("kotlin", "Unit"))
        }
        is JavaObject -> when (displayLanguage) {
            Language.JAVA -> pathProvider.linkForReference(DRI("java.lang", "Object"))
            Language.KOTLIN -> pathProvider.linkForReference(DRI("kotlin", "Any"))
        }
        is PrimitiveJavaType -> when (displayLanguage) {
            Language.JAVA -> DefaultLink(Link.Params(name = name, url = ""))
            Language.KOTLIN -> pathProvider.linkForReference(DRI("kotlin", name.capitalize()))
        }
        is UnresolvedBound -> DefaultLink(Link.Params(name = name, url = ""))
        else -> error("Unknown bound: $this")
    }

    /** Determine whether or not a param is a lambda using the kotlin function type. */
    private fun Projection.isLambda(): Boolean = when (this) {
        is TypeConstructor -> {
            val typeName = dri.classNames.orEmpty()
            dri.packageName == "kotlin" && typeName.startsWith("Function") || isSuspend()
        }
        is Nullable -> inner.isLambda()
        is Variance<*> -> inner.isLambda()
        is TypeAliased -> inner.isLambda()
        is UpstreamTypeParameter, is PrimitiveJavaType,
        is UnresolvedBound, is JavaObject, Star, Void -> false
        else -> error("Unknown bound: $this of type ${this::class.java}")
    }

    /** Gets the type constructor of a *lambda param only*. */
    private fun Projection.asTypeConstructor(): TypeConstructor = when (this) {
        is Variance<*> -> inner.asTypeConstructor()
        is Nullable -> inner.asTypeConstructor()
        is TypeAliased -> inner.asTypeConstructor()
        else -> this as TypeConstructor
    }

    /**
     * Runs through the tree of types, converting Kotlin primitives like Int, Boolean, etc. to their
     * Java counterparts. This is tricky because:
     *
     * - Ints and Chars are Integer and Character in Java (sigh)
     * - Nullable Kotlin primitives always have to be converted to their boxed types (since you
     *   can't return a null primitive in Java)
     * - Anything in a generic also has to be boxed
     * - Unit aka void can appear in lists and must therefore only be converted to void for return
     *   types
     */
    private fun Projection.rewriteKotlinPrimitivesForJava(
        isReturnType: Boolean = false,
        mustBoxPrimitive: Boolean = false
    ): Projection = when (this) {
        is FunctionalTypeConstructor, is GenericTypeConstructor -> {
            val typeConstructor = this as TypeConstructor
            val isStdlib = dri.packageName == "kotlin"
            val className = dri.classNames.orEmpty()

            if (isReturnType && isStdlib && className == "Unit") {
                Void
            } else if (isStdlib && className in kotlinPrimitives) {
                if (mustBoxPrimitive) {
                    when (className) {
                        "Char" -> typeConstructor.copy(DRI("java.lang", "Character"))
                        "Int" -> typeConstructor.copy(DRI("java.lang", "Integer"))
                        else -> typeConstructor.copy(DRI("java.lang", className))
                    }
                } else {
                    PrimitiveJavaType(className.toLowerCase())
                }
            } else if (isStdlib && className in kotlinPrimitiveArrays) {
                PrimitiveJavaType(className.removeSuffix("Array").toLowerCase() + "[]")
            } else {
                typeConstructor.copy(projections = projections.map {
                    // Generics can't be true primitives in Java
                    it.rewriteKotlinPrimitivesForJava(mustBoxPrimitive = true)
                }, dri = dri.possiblyAsJava())
            }
        }
        // Nullable types and variances can't be true primitives in Java
        is Nullable -> inner.rewriteKotlinPrimitivesForJava(mustBoxPrimitive = true)
        is Variance<*> -> inner.rewriteKotlinPrimitivesForJava(mustBoxPrimitive = true)
        else -> this
    }

    // Adds the functionality of a generic copy constructor for TypeConstructor. `copy` is defined by
    // GenericTypeConstructor and FunctionalTypeConstructor data subclasses
    private fun TypeConstructor.copy(
        dri: DRI = this.dri,
        projections: List<Projection> = this.projections
    ): Projection =
        when (this) {
            is GenericTypeConstructor -> this.copy(dri, projections)
            is FunctionalTypeConstructor -> this.copy(dri, projections)
        }

    private companion object {
        val kotlinPrimitives = setOf(
            "Boolean", "Byte", "Char", "Short", "Int", "Long", "Float", "Double"
        )
        val kotlinPrimitiveArrays = setOf(
            "BooleanArray",
            "ByteArray",
            "CharArray",
            "ShortArray",
            "IntArray",
            "LongArray",
            "FloatArray",
            "DoubleArray"
        )
        private val toKotlinTypeMemo = ConcurrentHashMap<Projection, Projection>()
    }
}

internal fun Projection.isSuspend(): Boolean = when (this) {
    is FunctionalTypeConstructor -> {
        this.isSuspendable
    }
    is Nullable -> inner.isSuspend()
    is Variance<*> -> inner.isSuspend()
    else -> false
}
