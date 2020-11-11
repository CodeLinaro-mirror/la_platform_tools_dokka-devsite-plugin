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

package com.google.devsite.renderer.impl

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.JavaClassKindTypes
import org.jetbrains.dokka.model.KotlinClassKindTypes
import org.jetbrains.dokka.model.WithSupertypes
import java.util.TreeSet

/**
 * Generate of graph of type dependencies.
 *
 * This returns the type inheritance of the given [classlikes] as a graph. That is, each DRI is
 * associated with its complete list of subclasses and parents.
 */
internal fun computeSubclassGraph(classlikes: List<DClasslike>): Map<DRI, Subclasses> {
    val drisToClasslikes = classlikes.associateBy { it.dri }
    val subclasses: Map<DRI, MutableSubclasses> = classlikes.associate { classlike ->
        classlike.dri to MutableSubclasses()
    }

    for (classlike in classlikes) {
        recursivelyUpdateClasslikeSupertypesTree(classlike, subclasses, drisToClasslikes)
    }

    // TODO(b/168956053): remove drisToClasslikes.getValue(it) once dokka has cheap hashCode impl
    return subclasses.mapValues { (_, level) ->
        Subclasses(
            all = level.all.map { drisToClasslikes.getValue(it) },
            direct = level.direct.map { drisToClasslikes.getValue(it) },
            indirect = level.indirect.map { drisToClasslikes.getValue(it) },
            superClasses = level.superClasses.map { drisToClasslikes.getValue(it) },
            interfaces = level.interfaces.map { drisToClasslikes.getValue(it) }
        )
    }
}

/**
 * Updates the [subclasses] by traversing the supertype tree using [classlikes]. [leaf] will not
 * change so it can be added to every parent's subclasses. The recursion occurs on [child].
 *
 * We must recursively traverse the hierarchy graph bottom up because Dokka only provides direct
 * parents as a DRI. We're assuming this will be performant because the JVM doesn't support multiple
 * inheritance, therefore yielding objects that tend to fan out top down, conversely fanning in
 * bottom up (what this method does).
 *
 * @param child the current classlike who's supertypes we will be traversing
 * @param subclasses the mutable type relation graph to be updated. [child] will be added to the set
 * direct subclasses for each of [child]'s supertypes. Similarly, [leaf] will be added to the set of
 * all subclasses for each of [child]'s supertypes. If [child] and [leaf] aren't the same (i.e. two
 * edges away from each other: A -> B -> C), we add [leaf] to the set of indirect subclasses for
 * each of [child]'s supertypes. Lastly, we add the type hierarchy path of [child] to the parents
 * of [leaf], ordered top-down.
 * @param classlikes reverse lookup map to get supertypes from DRIs
 * @param leaf constant classlike, storing the starting [child]
 */
private fun recursivelyUpdateClasslikeSupertypesTree(
    child: DClasslike,
    subclasses: Map<DRI, MutableSubclasses>,
    classlikes: Map<DRI, DClasslike>,
    leaf: DClasslike = child
) {
    if (child !is WithSupertypes || child.supertypes.isEmpty()) return

    val supertypes = child.supertypes.values.single()
    for ((type, kind) in supertypes) {
        subclasses[type.dri]?.let { (all, direct, indirect) ->
            all.add(leaf.dri)
            direct.add(child.dri)
            if (child !== leaf) indirect.add(leaf.dri)
        }

        // TODO Support external types
        // type.dri is not found in classlikes if it isn't from this package (or invocation?)
        // external types like stdlib appear in the supertypes but we filter them out by forcing
        // the use of classlikes here. This could just be DRI based and we'd connect the external
        // packages to the proper base URL.
        val supertype = classlikes[type.dri]
        if (supertype != null) {
            recursivelyUpdateClasslikeSupertypesTree(supertype, subclasses, classlikes, leaf)

            if (kind == JavaClassKindTypes.CLASS || kind == KotlinClassKindTypes.CLASS) {
                subclasses.getValue(leaf.dri).superClasses.add(supertype.dri)
            }

            if (kind == JavaClassKindTypes.INTERFACE || kind == KotlinClassKindTypes.INTERFACE) {
                subclasses.getValue(leaf.dri).interfaces.add(supertype.dri)
            }
        }
    }
}

internal data class Subclasses(
    val all: List<DClasslike>,
    val direct: List<DClasslike>,
    val indirect: List<DClasslike>,
    val superClasses: List<DClasslike>,
    val interfaces: List<DClasslike>
)

// TODO(b/168956053): Use DClasslike directly once dokka has cheap hashCode impl
private data class MutableSubclasses(
    val all: MutableSet<DRI> = TreeSet(classComparator),
    val direct: MutableSet<DRI> = TreeSet(classComparator),
    val indirect: MutableSet<DRI> = TreeSet(classComparator),
    val superClasses: MutableList<DRI> = mutableListOf(),
    val interfaces: MutableList<DRI> = mutableListOf()
) {
    private companion object {
        /**
         * Sort by the class name, but also compare by the package name since this determines
         * equality.
         */
        val classComparator = compareBy<DRI> { it.classNames + it.packageName }
    }
}
