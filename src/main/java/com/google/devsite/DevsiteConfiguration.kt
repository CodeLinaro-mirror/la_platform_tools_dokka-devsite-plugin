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

data class DevsiteConfiguration(
    // The devsite tenant arg is required, for the AndroidX docs this is "androidx"
    val tenant: String,
    // Versioned tenant is currently similar to devsite tenant, except java docs aren't generated
    // and kotlin docs don't go into a `kotlin` subdirectory. To be changed for b/248302613.
    val versionedTenant: String?,
    // Set of packages that Dackka will exclude for both Java and Kotlin refdoc generation
    val excludedPackages: List<String>?,
    // Packages to exclude for Java refdoc generation, in addition to those in excludedPackages
    val excludedPackagesForJava: List<String>?,
    // Packages to exclude for Kotlin refdoc generation, in addition to those in excludedPackages
    val excludedPackagesForKotlin: List<String>?,
    // The location of the JSON file containing the library metadata.
    val libraryMetadataFilename: String?
) : ConfigurableBlock {
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
