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

import org.jetbrains.dokka.plugability.ConfigurableBlock

/**
 * Configuration for the [DevsitePlugin]. This should be provided as a JSON string in the
 * `pluginsConfiguration` of the config file provided to Dackka.
 *
 * @param docRootPath The beginning of every file path generated. If not specified, it defaults to
 * "reference".
 * @param javaDocsPath The Java version of the docs will be placed [docRootPath]/[javaDocsPath].
 * If [javaDocsPath] is null or unspecified, Java docs will not be generated. If [javaDocsPath] is
 * an empty string, the Java docs go directly in [docRootPath].
 * @param kotlinDocsPath Equivalent to [javaDocsPath] for the Kotlin docs. [javaDocsPath] and
 * [kotlinDocsPath] cannot both be null and cannot have the same value.
 * @param projectPath A subdirectory of the [javaDocsPath] and [kotlinDocsPath] where the
 * package-list, table of contents, package index, and class index are placed. If [projectPath] is
 * empty, these files are placed directly in the [javaDocsPath] and [kotlinDocsPath] directories.
 * Specifying a [projectPath] is required.
 * @param excludedPackages Set of packages that Dackka will exclude for both Java and Kotlin refdoc
 * generation. This is a list of regular expression strings. Defaults to an empty list.
 * @param excludedPackagesForJava Packages to exclude for Java refdoc generation, in addition to
 * those in [excludedPackages]. Defaults to an empty list.
 * @param excludedPackagesForKotlin Packages to exclude for Kotlin refdoc generation, in addition to
 * those in [excludedPackages]. Defaults to an empty list.
 * @param libraryMetadataFilename The location of the JSON file containing the library metadata.
 * Optional, if not specified, library metadata will not be displayed.
 */
data class DevsiteConfiguration(
    val docRootPath: String = "reference",
    val javaDocsPath: String?,
    val kotlinDocsPath: String?,
    val projectPath: String,
    val excludedPackages: List<String>?,
    val excludedPackagesForJava: List<String>?,
    val excludedPackagesForKotlin: List<String>?,
    val libraryMetadataFilename: String?,
) : ConfigurableBlock {
    init {
        if (javaDocsPath == null && kotlinDocsPath == null) {
            throw IllegalStateException(
                "Invalid Dackka configuration: at least one of `javaDocsPath` and " +
                    "`kotlinDocsPath` must be specified as non-null"
            )
        } else if (javaDocsPath == kotlinDocsPath) {
            throw IllegalStateException(
                "Invalid Dackka configuration: `javaDocsPath` and `kotlinDocsPath` cannot have " +
                    "the same value."
            )
        }
    }

    // Parse provided excluded package lists to regex sets
    private val computedExcludedPackagesForBoth: Set<Regex> by lazy {
        excludedPackages?.map { it.toRegex() }?.toSet() ?: emptySet()
    }

    val computedExcludedPackagesForJava: Set<Regex> by lazy {
        computedExcludedPackagesForBoth +
            (excludedPackagesForJava?.map { it.toRegex() }?.toSet() ?: emptySet())
    }

    val computedExcludedPackagesForKotlin: Set<Regex> by lazy {
        computedExcludedPackagesForBoth +
            (excludedPackagesForKotlin?.map { it.toRegex() }?.toSet() ?: emptySet())
    }
}
