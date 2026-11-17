/*
 * Copyright 2026 The Android Open Source Project
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

package com.google.devsite.transformers

import androidx.tracing.Tracer
import com.google.devsite.renderer.converters.getExpectOrCommonSourceSet
import com.google.devsite.renderer.converters.isOverride
import com.google.devsite.util.trace
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.WithCompanion
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer

/**
 * Propagates documentation between related [org.jetbrains.dokka.model.Documentable]s.
 *
 * If a [DProperty] has no documentation, pulls documentation from the getter method.
 */
class PropagatedDocTagsTransformer(private val tracer: Tracer) : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule {
        return tracer.trace("PropagatedDocTagsTransformer") {
            original.copy(packages = original.packages.map(::transform))
        }
    }

    private fun transform(dPackage: DPackage): DPackage {
        return dPackage.copy(
            classlikes = dPackage.classlikes.map(::transform),
            properties = dPackage.properties.map(::transform),
        )
    }

    private fun transform(dClasslike: DClasslike): DClasslike {
        // Only apply the transformation to properties defined on the class itself. Inherited
        // properties sometimes have odd documentation on their getters (b/587389349)
        val (inheritedProperties, classProperties) =
            dClasslike.properties.partition { it.isOverride(it.getExpectOrCommonSourceSet()) }
        val transformedProperties = classProperties.map(::transform) + inheritedProperties

        val transformedClasslikes = dClasslike.classlikes.map(::transform)
        return when (dClasslike) {
            is WithCompanion -> {
                val transformedCompanion = dClasslike.companion?.let { transform(it) } as? DObject
                when (dClasslike) {
                    is DClass ->
                        dClasslike.copy(
                            properties = transformedProperties,
                            classlikes = transformedClasslikes,
                            companion = transformedCompanion,
                        )
                    is DAnnotation ->
                        dClasslike.copy(
                            properties = transformedProperties,
                            classlikes = transformedClasslikes,
                            companion = transformedCompanion,
                        )
                    is DEnum ->
                        dClasslike.copy(
                            properties = transformedProperties,
                            classlikes = transformedClasslikes,
                            companion = transformedCompanion,
                            entries = dClasslike.entries.map(::transform),
                        )
                    is DInterface ->
                        dClasslike.copy(
                            properties = transformedProperties,
                            classlikes = transformedClasslikes,
                            companion = transformedCompanion,
                        )
                    else -> error("Unexpected DClasslike/WithCompanion $dClasslike")
                }
            }
            is DObject ->
                dClasslike.copy(
                    properties = transformedProperties,
                    classlikes = transformedClasslikes,
                )
        }
    }

    private fun transform(dEnumEntry: DEnumEntry): DEnumEntry {
        return dEnumEntry.copy(
            properties = dEnumEntry.properties.map(::transform),
            classlikes = dEnumEntry.classlikes.map(::transform),
        )
    }

    private fun transform(dProperty: DProperty): DProperty {
        // For each source set, if there is no documentation on the property itself, add
        // documentation from the getter if it exists.
        val newDocumentation =
            dProperty.sourceSets.mapNotNull { sourceSet ->
                if (dProperty.documentation[sourceSet]?.children.isNullOrEmpty()) {
                    dProperty.getter?.documentation[sourceSet]?.let { sourceSet to it }
                } else {
                    null
                }
            }
        return dProperty.copy(documentation = dProperty.documentation + newDocumentation)
    }
}
