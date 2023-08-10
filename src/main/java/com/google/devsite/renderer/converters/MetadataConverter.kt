/*
 * Copyright 2023 The Android Open Source Project
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

import com.google.common.annotations.VisibleForTesting
import com.google.devsite.components.impl.DefaultMetadataComponent
import com.google.devsite.components.impl.DefaultVersionMetadataComponent
import com.google.devsite.components.symbols.MetadataComponent
import com.google.devsite.components.symbols.VersionMetadataComponent
import com.google.devsite.renderer.converters.ParameterDocumentableConverter.Companion.rewriteKotlinPrimitivesForJava
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.util.LibraryMetadata
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.Contravariance
import org.jetbrains.dokka.model.Covariance
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeParameter
import org.jetbrains.dokka.model.DefinitelyNonNullable
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.dokka.model.Dynamic
import org.jetbrains.dokka.model.FunctionalTypeConstructor
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.Invariance
import org.jetbrains.dokka.model.JavaObject
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.PrimitiveJavaType
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.Star
import org.jetbrains.dokka.model.TypeAliased
import org.jetbrains.dokka.model.TypeParameter
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Void
import org.jetbrains.dokka.model.WithSources

/**
 * Creates metadata components (a section containing information such as artifact ID and source
 * links) for documentables.
 */
internal class MetadataConverter(
    private val docsHolder: DocumentablesHolder
) {
    /**
     * Creates a metadata component for the classlike.
     */
    fun getMetadataForClasslike(classlike: DClasslike): MetadataComponent {
        val paths = classlike.getSourceFilePaths()
        val libraryMetadata = paths?.let { classlike.findMatchingLibraryMetadata(it) }
        val sourceUrl = paths?.let { classlike.createLinkToSource(it) }
        val versionMetadata = classlike.findMatchingVersionMetadata(
            libraryMetadata?.releaseNotesUrl
        )

        return DefaultMetadataComponent(
            MetadataComponent.Params(
                libraryMetadata = libraryMetadata,
                sourceLinkUrl = sourceUrl,
                versionMetadata = versionMetadata
            )
        )
    }

    /**
     * Creates a metadata component for the [function].
     */
    fun getMetadataForFunction(function: DFunction): MetadataComponent {
        val paths = function.getSourceFilePaths()
        val libraryMetadata = paths?.let { function.findMatchingLibraryMetadata(it) }
        val versionMetadata = function.findMatchingVersionMetadata(libraryMetadata?.releaseNotesUrl)

        return DefaultMetadataComponent(
            MetadataComponent.Params(
                // TODO(b/264828018): display artifact ID and source link for some functions
                libraryMetadata = null,
                sourceLinkUrl = null,
                versionMetadata = versionMetadata
            )
        )
    }

    /**
     * Creates a metadata component for the [property].
     */
    fun getMetadataForProperty(property: DProperty): MetadataComponent {
        val paths = property.getSourceFilePaths()
        val libraryMetadata = paths?.let { property.findMatchingLibraryMetadata(it) }
        val versionMetadata = property.findMatchingVersionMetadata(libraryMetadata?.releaseNotesUrl)

        return DefaultMetadataComponent(
            MetadataComponent.Params(
                // TODO(b/264828018): display artifact ID and source link for some properties
                libraryMetadata = null,
                sourceLinkUrl = null,
                versionMetadata = versionMetadata
            )
        )
    }

    /**
     * Iterate through the library metadata Map to find a [LibraryMetadata] that matches the
     * current class being processed.  Otherwise, return null.
     */
    private fun Documentable.findMatchingLibraryMetadata(paths: List<String>): LibraryMetadata? {
        val path = if (paths.size > 1) {
            // If there are multiple paths, this is probably KMP and the paths end in ".kt",
            // ".jvm.kt", ".native.kt", etc. Pick out the ".kt" path.
            paths.singleOrNull { it.indexOf(".") == it.lastIndexOf(".") }
        } else {
            paths.single()
        }

        return docsHolder.fileMetadataMap[path]
    }

    /**
     * Query the API version metadata Map to find a [VersionMetadataComponent] that matches the
     * current class being processed and append a release URL.  Otherwise, return null.
     */
    private fun DClasslike.findMatchingVersionMetadata(
        releaseNotesUrl: String?
    ): VersionMetadataComponent? {
        val classVersionMetadata = docsHolder.versionMetadataMap[dri.fullName]

        return classVersionMetadata?.let {
            DefaultVersionMetadataComponent.createVersionMetadataWithBaseUrl(
                it.addedIn,
                it.deprecatedIn,
                releaseNotesUrl
            )
        }
    }

    /**
     * Query the API version metadata Map to find a [VersionMetadataComponent] that matches the
     * current function being processed and append a release URL.  Otherwise, return null.
     */
    private fun DFunction.findMatchingVersionMetadata(
        releaseNotesUrl: String?
    ): VersionMetadataComponent? {
        val classVersionMetadata = docsHolder.versionMetadataMap[containingClassName()]
        val methodVersionMetadata = classVersionMetadata?.methodVersions?.get(
            apiSinceMethodSignature(this)
        )

        return methodVersionMetadata?.let {
            DefaultVersionMetadataComponent.createVersionMetadataWithBaseUrl(
                it.addedIn,
                it.deprecatedIn,
                releaseNotesUrl
            )
        }
    }

    /**
     * Query the API version metadata map to find a [VersionMetadataComponent] that matches the
     * current property being processed and append a release URL.  Otherwise, return null.
     * Many properties will be represented in the version metadata map by their accessors, so this
     * looks for metadata of the getter if metadata can't be found for the property itself.
     */
    private fun DProperty.findMatchingVersionMetadata(
        releaseNotesUrl: String?
    ): VersionMetadataComponent? {
        val classVersionMetadata = docsHolder.versionMetadataMap[containingClassName()]
        val propertyVersionMetadata = classVersionMetadata?.fieldVersions?.get(name)

        return propertyVersionMetadata?.let {
            DefaultVersionMetadataComponent.createVersionMetadataWithBaseUrl(
                it.addedIn,
                it.deprecatedIn,
                releaseNotesUrl
            )
        } ?: getter?.findMatchingVersionMetadata(releaseNotesUrl)
    }

    /**
     * Constructs the fully-qualified name for the containing class of the [Documentable] in the
     * Java view of the API. This means if the [Documentable] is a top-level function or property,
     * a synthetic class name is used.
     */
    private fun <T> T.containingClassName(): String where T : WithSources, T : Documentable =
        "${dri.packageName}.${dri.classNames ?: nameForSyntheticClass(this)}"

    /**
     * Finds the filepaths associated with the documentable's source entries.
     *
     * Returns null if there are no source entries, or no source entries with file paths, which is
     * the case for all synthetic classes and functions.
     */
    private fun <T> T.getSourceFilePaths(): List<String>?
        where T : WithSources, T : Documentable {
        val paths = sources.entries.mapNotNull { it.getSourceFilePath() }
        if (paths.isEmpty()) return null
        return paths
    }

    /**
     * Get the source file path from the [SourceEntry] relative to the root of the source directory,
     * if possible.
     *
     * For example - this would return "androidx/paging/compose/LazyPagingItems.kt" if the path was
     * "/location/to/root/of/source/files/androidx/paging/compose/LazyPagingItems.kt".
     */
    private fun SourceEntry.getSourceFilePath(): String? {
        val sourceRoots = key.sourceRoots.map { it.toString() }
        val fullFilePath = value.path
        // Find the source root that the file path starts with, so it can be trimmed off.
        // This assumes the full file path always begins with one of the source roots, if it doesn't
        // the entry may be from an external source and this returns null.
        val relevantSourceRoot = sourceRoots.firstOrNull { fullFilePath.startsWith(it) }
            ?: return null
        val filePath = fullFilePath.substringAfter(relevantSourceRoot)
        return filePath.removePrefix("/")
    }

    /**
     * Creates a link to the source of the classlike using the base URL from the configuration.
     *
     * Returns null if there was no base source link in the configuration.
     */
    private fun Documentable.createLinkToSource(paths: List<String>): String? {
        // Reduce the list of paths to a single path by taking the common prefix of all of them.
        val path = paths.reduce { currPrefix, nextPath -> currPrefix.commonPrefixWith(nextPath) }
        return docsHolder.baseSourceLink?.format(path, dri.fullName)
    }

    companion object {

        /**
         * Converts a method signature to a string that matches the formatting in the apiSince JSON
         */
        @VisibleForTesting
        fun apiSinceMethodSignature(function: DFunction): String {
            // The metadata uses the Java API, so use the JvmName if it exists
            val functionName = function.jvmName() ?: function.name

            val generics = if (function.generics.isEmpty()) {
                ""
            } else {
                "<" + function.generics.joinToString(", ") { it.metalavaName() } + ">"
            }

            // The metadata uses the Java API, move the receiver to a parameter
            val parameters = if (function.receiver != null) {
                function.convertReceiverForJava().parameters
            } else {
                function.parameters
            }

            val paramTypes = parameters.map { param ->
                val basicTypeName = param.type.rewriteKotlinPrimitivesForJava(
                    useQualifiedTypes = true
                ).metalavaName()

                // Kotlin varargs are separate from the type representation
                // The other parameter modifier that impacts the signature is `suspend`, which is
                // handled in [FunctionalTypeConstructor.functionalTypeMetalavaName()]
                val modifiers = param.modifiers(param.getExpectOrCommonSourceSet())
                val additional = if (modifiers.contains("vararg")) "..." else ""

                "$basicTypeName$additional"
            }.toMutableList()

            // `suspend` functions have a continuation arg in the Java API that does not appear in
            // the Kotlin representation. Other function modifiers do not impact the Java signature
            if (function.modifiers(function.getExpectOrCommonSourceSet()).contains("suspend")) {
                paramTypes += "kotlin.coroutines.Continuation<? super kotlin.Unit>"
            }

            return """$functionName$generics(${paramTypes.joinToString(",")})"""
        }

        /**
         * Converts the [Projection] to its Java-style type name, which is what is used in the
         * apiSince metadata.
         *
         * This is based on [ParameterDocumentableConverter.Companion.nameForJavaArray], but meant
         * to match the signatures generated by metalava.
         */
        private fun Projection.metalavaName(): String = when (this) {
            is TypeParameter -> name
            // possiblyAsJava() is needed here as Metalava generates a Java view of types
            // Example: both java.lang.String and kotlin.String are represented as java.lang.String
            is GenericTypeConstructor -> {
                val nested = if (projections.isEmpty()) {
                    ""
                } else {
                    """<${projections.joinToString(",") { it.metalavaName() }}>"""
                }
                "${dri.possiblyAsJava().fullName}$nested"
            }
            is Nullable -> inner.metalavaName()
            is DefinitelyNonNullable -> inner.metalavaName()
            is TypeAliased -> inner.metalavaName()
            is UnresolvedBound -> name
            // `? extends Object` is redundant, it is in the metadata as "?"
            is Covariance<*> -> if (inner is JavaObject) {
                "?"
            } else {
                "? extends ${inner.metalavaName()}"
            }
            is Contravariance<*> -> "? super ${inner.metalavaName()}"
            is Invariance<*> -> inner.metalavaName()
            is PrimitiveJavaType -> name
            Void -> "void"
            Star -> "?"
            is JavaObject -> "java.lang.Object"
            is FunctionalTypeConstructor -> functionalTypeMetalavaName()
            Dynamic -> throw RuntimeException("Invalid State: trying to get name of a Dynamic")
        }

        /**
         * Converts the [FunctionalTypeConstructor] to its Java-style type name, which is what is
         * used in the apiSince metadata.
         */
        private fun FunctionalTypeConstructor.functionalTypeMetalavaName(): String {
            // `suspend` function types appear as `kotlin.coroutines.SuspendFunction<N>` in the
            // model, but in the Java signature as `kotlin.jvm.functions.Function<N+1>`
            val typeName = if (isSuspendable) {
                val num = dri.classNames!!.substringAfter("SuspendFunction").toInt() + 1
                "kotlin.jvm.functions.Function$num"
            } else {
                dri.fullName
            }

            val paramNames = projections.dropLast(1).map {
                val name = it.metalavaName()
                // Non-object param types appear as contravariance in the metadata, while
                // object params just appear as Object -- except for in `suspend` functions, where
                // `? super Object` does show up.
                if (it is Invariance<*> && (name != "java.lang.Object" || isSuspendable)) {
                    "? super $name"
                } else {
                    name
                }
            }
            val returnName = projections.last().let { type ->
                type.metalavaName().let { name ->
                    if (isSuspendable) {
                        // `suspend` functions have their return types wrapped in a Continuation, and
                        // an extra `?` added at the end of the list of generics
                        "? super kotlin.coroutines.Continuation<? super $name>,?"
                    } else if (name == "java.lang.Object") {
                        // An object return type appears in the metadata as "?"
                        "?"
                    } else {
                        val innerType = (type as? Invariance<*>)?.inner?.unwrapNullability()
                        if (
                            innerType is PrimitiveJavaType ||
                            (innerType is GenericTypeConstructor && innerType.projections.isEmpty())
                        ) {
                            // Simple types appear as-is in the metadata, more complex types include
                            // "? extends" first
                            name
                        } else {
                            "? extends $name"
                        }
                    }
                }
            }
            val nested = (paramNames + returnName).joinToString(",")

            return "$typeName<$nested>"
        }

        /**
         * Converts the [DTypeParameter] to its Java-style type name, which is what is used in the
         * apiSince metadata.
         */
        private fun DTypeParameter.metalavaName(): String {
            val boundsNames = bounds.map { it.metalavaName() }
                // `extends java.lang.Object` is redundant and not included in the metadata
                // Filtering by if `it !is JavaObject` doesn't work because the `JavaObject` may
                // be nested in a different `Projection`
                .filter { it != "java.lang.Object" }
            val bounds = if (boundsNames.isEmpty()) { "" } else {
                // This is always "extends", even if the bound represents an interface
                " extends " + boundsNames.joinToString(" & ")
            }
            return name + bounds
        }

        /**
         * Remove an outer nullability wrapper from the [Projection], if one exists.
         * Does not recur into nested projections.
         */
        private fun Projection.unwrapNullability(): Projection =
            when (this) {
                is Nullable -> inner
                is DefinitelyNonNullable -> inner
                else -> this
            }
    }
}

typealias SourceEntry = Map.Entry<DokkaConfiguration.DokkaSourceSet, DocumentableSource>
