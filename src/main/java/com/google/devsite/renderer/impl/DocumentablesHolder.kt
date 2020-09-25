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

import com.google.devsite.renderer.converters.explodedChildren
import com.google.devsite.renderer.converters.name
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DTypeAlias
import org.jetbrains.dokka.model.Documentable

/**
 * Centralized place to retrieve documentables.
 *
 * All doc rewriting should occur here.
 */
internal class DocumentablesHolder(module: DModule, scope: CoroutineScope) {
    private val packages = scope.async { computePackages(module) }

    private val classlikes = mutableMapOf<DRI, Deferred<List<DClasslike>>>()
    private val classes = mutableMapOf<DRI, Deferred<List<DClass>>>()
    private val enums = mutableMapOf<DRI, Deferred<List<DEnum>>>()
    private val interfaces = mutableMapOf<DRI, Deferred<List<DInterface>>>()
    private val annotations = mutableMapOf<DRI, Deferred<List<DAnnotation>>>()
    private val typeAliases = mutableMapOf<DRI, Deferred<List<DTypeAlias>>>()
    private val exceptions = mutableMapOf<DRI, Deferred<List<DClass>>>()

    private val allClasslikes: Deferred<List<DClasslike>>
    private val nestedClasslikesJob: Job
    private val nestedClasslikes = mutableMapOf<DRI, Deferred<List<DClasslike>>>()
    private val subclassGraph: Deferred<Map<DRI, Subclasses>>

    init {
        scope.apply {
            for (packageDoc in module.packages) {
                val children = async { packageDoc.explodedChildren }

                val classlikesList = async { computeClasslikes(children.await()) }
                val classList = async { computeClasses(children.await()) }
                val enumList = async { computeEnums(children.await()) }
                val interfaceList = async { computeInterfaces(children.await()) }
                val annotationList = async { computeAnnotations(children.await()) }
                val typeAliasList = async { computeTypesAliases(packageDoc) }
                val exceptionList = async { computeExceptions(classList.await()) }

                classlikes[packageDoc.dri] = classlikesList
                classes[packageDoc.dri] = classList
                enums[packageDoc.dri] = enumList
                interfaces[packageDoc.dri] = interfaceList
                annotations[packageDoc.dri] = annotationList
                typeAliases[packageDoc.dri] = typeAliasList
                exceptions[packageDoc.dri] = exceptionList
            }
        }

        allClasslikes = scope.async { computeClasslikes(module) }
        subclassGraph = scope.async { computeSubclassGraph(allClasslikes.await()) }

        nestedClasslikesJob = scope.launch {
            for (classlike in allClasslikes.await()) {
                val nestedClasslikesList =
                    async { computeClasslikes(classlike.explodedChildren) }

                nestedClasslikes[classlike.dri] = nestedClasslikesList
            }
        }
    }

    suspend fun packages(): List<DPackage> = packages.await()

    suspend fun allClasslikes(): List<DClasslike> = allClasslikes.await()

    suspend fun subclassGraph(): Map<DRI, Subclasses> = subclassGraph.await()

    suspend fun classlikesFor(packageDoc: DPackage): List<DClasslike> =
        classlikes.getValue(packageDoc.dri).await()

    suspend fun classlikesFor(classlike: DClasslike): List<DClasslike> {
        nestedClasslikesJob.join()
        return nestedClasslikes.getValue(classlike.dri).await()
    }

    suspend fun classesFor(packageDoc: DPackage): List<DClass> =
        classes.getValue(packageDoc.dri).await()

    suspend fun enumsFor(packageDoc: DPackage): List<DEnum> =
        enums.getValue(packageDoc.dri).await()

    suspend fun interfacesFor(packageDoc: DPackage): List<DInterface> =
        interfaces.getValue(packageDoc.dri).await()

    suspend fun annotationsFor(packageDoc: DPackage): List<DAnnotation> =
        annotations.getValue(packageDoc.dri).await()

    suspend fun typeAliasesFor(packageDoc: DPackage): List<DTypeAlias> =
        typeAliases.getValue(packageDoc.dri).await()

    suspend fun exceptionsFor(packageDoc: DPackage): List<DClass> =
        exceptions.getValue(packageDoc.dri).await()

    private fun computePackages(module: DModule): List<DPackage> {
        return module.packages.sortedBy { it.name }
    }

    private suspend fun computeClasslikes(module: DModule): List<DClasslike> {
        return computeClasslikes(module.packages.flatMap { classlikesFor(it) })
    }

    private fun computeClasslikes(docs: List<Documentable>): List<DClasslike> {
        return docs.filterIsInstance<DClasslike>().sortedBy { it.name() }
    }

    private fun computeClasses(docs: List<Documentable>): List<DClass> {
        return docs.filterIsInstance<DClass>().sortedBy { it.name() }
    }

    private fun computeEnums(docs: List<Documentable>): List<DEnum> {
        return docs.filterIsInstance<DEnum>().sortedBy { it.name() }
    }

    private fun computeInterfaces(docs: List<Documentable>): List<DInterface> {
        return docs.filterIsInstance<DInterface>().sortedBy { it.name() }
    }

    private fun computeAnnotations(docs: List<Documentable>): List<DAnnotation> {
        return docs.filterIsInstance<DAnnotation>().sortedBy { it.name() }
    }

    private fun computeTypesAliases(packageDoc: DPackage): List<DTypeAlias> {
        return packageDoc.typealiases.sortedBy { it.name }
    }

    private fun computeExceptions(docs: List<DClass>): List<DClass> {
        return docs.filter { clazz ->
            clazz.functions.any { function -> function.dri.classNames == "Throwable" }
        }
    }
}
