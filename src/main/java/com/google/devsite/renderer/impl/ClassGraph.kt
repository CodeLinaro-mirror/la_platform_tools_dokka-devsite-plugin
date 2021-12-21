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
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.JavaClassKindTypes
import org.jetbrains.dokka.model.KotlinClassKindTypes
import org.jetbrains.dokka.model.WithSupertypes
import java.util.TreeSet

internal typealias ClassGraph = Map<DRI, ClassNode>
internal typealias DocumentablesGraph = Map<DRI, Documentable>

/**
 * Generate of graph of type dependencies.
 *
 * This returns the type inheritance of the given [classlikes] as a graph. That is, each DRI is
 * associated with itself and its complete list of subclasses and parents.
 */
internal fun computeClassGraph(classlikes: List<DClasslike>): ClassGraph {
    val drisToClasslikes = classlikes.associateBy { it.dri }
    val classGraph: Map<DRI, MutableClassNode> = classlikes.associate { classlike ->
        classlike.dri to MutableClassNode(classlike)
    }

    for (classlike in classlikes) {
        recursivelyUpdateClasslikeSupertypesTree(classlike, classGraph, drisToClasslikes)
    }

    // TODO(b/168956053): remove drisToClasslikes.getValue(it) once dokka has cheap hashCode impl
    return classGraph.mapValues { (_, level) ->
        ClassNode(
            self = level.self,
            allSubClasses = level.allSubClasses.map { drisToClasslikes.getValue(it) },
            directSubClasses = level.directSubClasses.map { drisToClasslikes.getValue(it) },
            indirectSubClasses = level.indirectSubClasses.map { drisToClasslikes.getValue(it) },
            superClasses = level.superClasses.map { drisToClasslikes.getValue(it) },
            interfaces = level.interfaces.map { drisToClasslikes.getValue(it) }
        )
    }
}

/**
 * Generates a map that allows looking up each Documentable by its DRI
 */
internal fun computeDocumentablesGraph(classGraph: ClassGraph): DocumentablesGraph {

    // helper function for adding a Documentable to a graph
    fun addToDocumentablesGraph(graph: MutableMap<DRI, Documentable>, documentable: Documentable) {
        if (!graph.containsKey(documentable.dri)) {
            graph[documentable.dri] = documentable
            for (child in documentable.children) {
                addToDocumentablesGraph(graph, child)
            }
        }
    }

    // add each class to the graph, and recurse
    val result = mutableMapOf<DRI, Documentable>()
    for (documentable in classGraph.values) {
        addToDocumentablesGraph(result, documentable.self)
    }
    return result
}

/**
 * Updates the [classGraph] by traversing the supertype tree using [classlikes]. [leaf] will not
 * change, so it can be added to every parent's subclasses. The recursion occurs on [child].
 *
 * We must recursively traverse the hierarchy graph bottom up because Dokka only provides direct
 * parents as a DRI. We're assuming this will be performant because the JVM doesn't support multiple
 * inheritance, therefore yielding objects that tend to fan out top down, conversely fanning in
 * bottom up (what this method does).
 *
 * @param child the current classlike who's supertypes we will be traversing
 * @param classGraph the mutable type relation graph to be updated. [child] will be added to the set
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
    classGraph: Map<DRI, MutableClassNode>,
    classlikes: Map<DRI, DClasslike>,
    leaf: DClasslike = child
) {
    if (child !is WithSupertypes || child.supertypes.isEmpty()) return

    val supertypes = child.supertypes.values.single()
    for ((type, kind) in supertypes) {
        classGraph[type.dri]?.let { (_, all, direct, indirect) ->
            all.add(leaf.dri)
            direct.add(child.dri)
            if (child !== leaf) indirect.add(leaf.dri)
        }

        // TODO Support external types b/170124934
        // type.dri is not found in classlikes if it isn't from this package (or invocation?)
        // external types like stdlib appear in the supertypes but we filter them out by forcing
        // the use of classlikes here. This could just be DRI based and we'd connect the external
        // packages to the proper base URL.
        val supertype = classlikes[type.dri]
        if (supertype != null) {
            recursivelyUpdateClasslikeSupertypesTree(supertype, classGraph, classlikes, leaf)

            if (kind == JavaClassKindTypes.CLASS || kind == KotlinClassKindTypes.CLASS) {
                classGraph.getValue(leaf.dri).superClasses.add(supertype.dri)
            }

            if (kind == JavaClassKindTypes.INTERFACE || kind == KotlinClassKindTypes.INTERFACE) {
                classGraph.getValue(leaf.dri).interfaces.add(supertype.dri)
            }
        }
    }
}

internal data class ClassNode(
    val self: DClasslike,
    val allSubClasses: List<DClasslike>,
    val directSubClasses: List<DClasslike>,
    val indirectSubClasses: List<DClasslike>,
    val superClasses: List<DClasslike>,
    val interfaces: List<DClasslike>
)

// TODO(b/168956053): Use DClasslike directly once dokka has cheap hashCode impl
private data class MutableClassNode(
    val self: DClasslike,
    val allSubClasses: MutableSet<DRI> = TreeSet(classComparator),
    val directSubClasses: MutableSet<DRI> = TreeSet(classComparator),
    val indirectSubClasses: MutableSet<DRI> = TreeSet(classComparator),
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
