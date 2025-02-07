/*
 * Copyright 2024 The Android Open Source Project
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

import com.google.devsite.renderer.converters.addAnnotations
import com.google.devsite.renderer.converters.annotations
import com.google.devsite.renderer.converters.companion
import com.google.devsite.renderer.converters.fullName
import com.google.devsite.renderer.converters.getExpectOrCommonSourceSet
import org.jetbrains.dokka.model.Annotations.Annotation
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithConstructors
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer

/** Propagates annotations from elements to their members. */
class PropagatedAnnotationsTransformer(
    private val propagatingAnnotations: List<String>,
) : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule {
        if (propagatingAnnotations.isEmpty()) return original

        return original.copy(packages = original.packages.map { transform(it) })
    }

    private fun transform(original: DPackage): DPackage {
        val annotationsToPropagate = original.getPropagatingAnnotations()
        return original.copy(
            functions = original.functions.map { transform(it, annotationsToPropagate) },
            properties = original.properties.map { transform(it, annotationsToPropagate) },
            classlikes = original.classlikes.map { transform(it, annotationsToPropagate) },
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun transform(original: DClasslike, parentAnnotations: Set<Annotation>): DClasslike {
        val (newExtra, annotationsToPropagate) =
            when (original) {
                is DClass -> original.propagateAnnotations(parentAnnotations)
                is DAnnotation -> original.propagateAnnotations(parentAnnotations)
                is DEnum -> original.propagateAnnotations(parentAnnotations)
                is DInterface -> original.propagateAnnotations(parentAnnotations)
                is DObject -> original.propagateAnnotations(parentAnnotations)
            }

        // Transform all applicable members of the classlike.
        val newFunctions = original.functions.map { transform(it, annotationsToPropagate) }
        val newProperties = original.properties.map { transform(it, annotationsToPropagate) }
        val newClasslikes = original.classlikes.map { transform(it, annotationsToPropagate) }
        val newCompanion =
            original.companion()?.let { transform(it, annotationsToPropagate) as DObject }
        val newConstructors =
            if (original is WithConstructors) {
                original.constructors.map { transform(it, annotationsToPropagate) }
            } else {
                emptyList()
            }

        return when (original) {
            is DClass ->
                original.copy(
                    constructors = newConstructors,
                    functions = newFunctions,
                    properties = newProperties,
                    classlikes = newClasslikes,
                    companion = newCompanion,
                    extra = newExtra as PropertyContainer<DClass>,
                )
            is DAnnotation ->
                original.copy(
                    constructors = newConstructors,
                    functions = newFunctions,
                    properties = newProperties,
                    classlikes = newClasslikes,
                    companion = newCompanion,
                    extra = newExtra as PropertyContainer<DAnnotation>,
                )
            is DEnum ->
                original.copy(
                    entries = original.entries.map { transform(it, annotationsToPropagate) },
                    constructors = newConstructors,
                    functions = newFunctions,
                    properties = newProperties,
                    classlikes = newClasslikes,
                    companion = newCompanion,
                    extra = newExtra as PropertyContainer<DEnum>,
                )
            is DInterface ->
                original.copy(
                    functions = newFunctions,
                    properties = newProperties,
                    classlikes = newClasslikes,
                    companion = newCompanion,
                    extra = newExtra as PropertyContainer<DInterface>,
                )
            is DObject ->
                original.copy(
                    functions = newFunctions,
                    properties = newProperties,
                    classlikes = newClasslikes,
                    extra = newExtra as PropertyContainer<DObject>,
                )
        }
    }

    private fun transform(original: DFunction, parentAnnotations: Set<Annotation>): DFunction {
        // Annotations are not propagated to function parameters.
        if (parentAnnotations.isEmpty()) return original
        val (newExtra, _) = original.propagateAnnotations(parentAnnotations)
        return original.copy(
            extra = newExtra,
        )
    }

    private fun transform(original: DProperty, parentAnnotations: Set<Annotation>): DProperty {
        val (newExtra, annotationsToPropagate) = original.propagateAnnotations(parentAnnotations)
        if (annotationsToPropagate.isEmpty()) return original
        return original.copy(
            getter = original.getter?.let { transform(it, annotationsToPropagate) },
            setter = original.setter?.let { transform(it, annotationsToPropagate) },
            extra = newExtra,
        )
    }

    private fun transform(original: DEnumEntry, parentAnnotations: Set<Annotation>): DEnumEntry {
        // Annotations are not propagated to members of enum entries because they aren't in docs.
        if (parentAnnotations.isEmpty()) return original
        val (newExtra, _) = original.propagateAnnotations(parentAnnotations)
        return original.copy(
            extra = newExtra,
        )
    }

    /**
     * Propagates the [parentAnnotations] to this documentable, returning the new extra properties
     * containing the annotations and a set of annotations to propagate to this documetable's
     * children -- the [parentAnnotations] plus any propagating annotations present on the
     * documentable.
     *
     * For KMP, this propagates annotations from the common source set to all source sets.
     * (b/262711247: differing deprecation status between source sets isn't handled)
     */
    private fun <T> T.propagateAnnotations(
        parentAnnotations: Set<Annotation>
    ): Pair<PropertyContainer<T>, Set<Annotation>> where
    T : Documentable,
    T : WithExtraProperties<T> {
        val elementAnnotations = getPropagatingAnnotations()
        val annotationsToAdd = parentAnnotations - elementAnnotations
        val annotationsToPropagate = parentAnnotations + elementAnnotations

        val transformed =
            if (annotationsToAdd.isNotEmpty()) {
                extra.addAnnotations(annotationsToAdd, sourceSets)
            } else {
                extra
            }
        return Pair(transformed, annotationsToPropagate)
    }

    private fun Documentable.getPropagatingAnnotations(): Set<Annotation> {
        return annotations(getExpectOrCommonSourceSet())
            .filter { propagatingAnnotations.contains(it.dri.fullName) }
            .toSet()
    }
}
