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

import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.explodedChildren
import com.google.devsite.renderer.converters.filterOutJvmSynthetic
import com.google.devsite.renderer.converters.isExceptionClass
import com.google.devsite.renderer.converters.isOrdinaryCompanion
import com.google.devsite.renderer.converters.name
import com.google.devsite.renderer.converters.nameForSyntheticClass
import com.google.devsite.renderer.converters.packageName
import com.google.devsite.renderer.converters.setUpAnalysis
import com.google.devsite.renderer.converters.withJavaSynthetic
import com.google.devsite.util.LibraryMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.EnvironmentAndFacade
import org.jetbrains.dokka.base.translators.descriptors.ExternalDocumentablesProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.Bound
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeAlias
import org.jetbrains.dokka.model.DefinitelyNonNullable
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Dynamic
import org.jetbrains.dokka.model.FunctionalTypeConstructor
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.JavaModifier
import org.jetbrains.dokka.model.JavaObject
import org.jetbrains.dokka.model.JavaVisibility
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.PrimitiveJavaType
import org.jetbrains.dokka.model.TypeAliased
import org.jetbrains.dokka.model.TypeParameter
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Void
import org.jetbrains.dokka.model.WithCompanion
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import java.util.concurrent.atomic.AtomicInteger

/**
 * Centralized place to retrieve documentables.
 *
 * All doc rewriting should occur here.
 */
internal class DocumentablesHolder(
    module: DModule,
    scope: CoroutineScope,
    context: DokkaContext? = null,
    private val externalDocumentablesProvider: ExternalDocumentablesProvider? = null,
    private val excludedPackages: Set<Regex> = emptySet(),
    val showLibraryMetadata: Boolean = false,
    val fileMetadataMap: Map<String, LibraryMetadata> = emptyMap(),
) {
    internal var classlikesDone: AtomicInteger = AtomicInteger()

    private val packages = scope.async { computePackages(module) }

    private val classlikes = mutableMapOf<DRI, Deferred<List<DClasslike>>>()
    private val classes = mutableMapOf<DRI, Deferred<List<DClass>>>()
    private val syntheticClasses = mutableMapOf<DRI, Deferred<List<DClass>>>()
    private val enums = mutableMapOf<DRI, Deferred<List<DEnum>>>()
    private val interfaces = mutableMapOf<DRI, Deferred<List<DInterface>>>()
    private val annotations = mutableMapOf<DRI, Deferred<List<DAnnotation>>>()
    private val typeAliases = mutableMapOf<DRI, Deferred<List<DTypeAlias>>>()
    private val exceptions = mutableMapOf<DRI, Deferred<List<DClass>>>()
    private val objects = mutableMapOf<DRI, Deferred<List<DObject>>>()

    private val allClasslikes: Deferred<List<DClasslike>>
    private val nestedClasslikesJob: Job
    private val nestedClasslikes = mutableMapOf<DRI, Deferred<List<DClasslike>>>()
    private val classGraph: Deferred<ClassGraph>
    private val documentablesGraph: Deferred<DocumentablesGraph>
    private val analysisMap: Deferred<Map<DokkaConfiguration.DokkaSourceSet, EnvironmentAndFacade>>

    internal val logger = context?.logger ?: DokkaConsoleLogger(LoggingLevel.WARN)

    init {
        scope.apply {
            for (dPackage in module.packages) {
                val children = async { dPackage.explodedChildren }
                val syntheticClassList = async { computeSyntheticClasses(dPackage) }
                val classlikesList = async {
                    computeClasslikes(
                        children.await(),
                        syntheticClassList.await()
                    )
                }
                val classList = async { computeClasses(children.await()) }

                val enumList = async { computeEnums(children.await()) }
                val interfaceList = async { computeInterfaces(children.await()) }
                val annotationList = async { computeAnnotations(children.await()) }
                val typeAliasList = async { computeTypesAliases(dPackage) }
                val exceptionList = async { computeExceptions(children.await()) }
                val objectList = async { computeObjects(children.await()) }

                classlikes[dPackage.dri] = classlikesList
                classes[dPackage.dri] = classList
                syntheticClasses[dPackage.dri] = syntheticClassList
                enums[dPackage.dri] = enumList
                interfaces[dPackage.dri] = interfaceList
                annotations[dPackage.dri] = annotationList
                typeAliases[dPackage.dri] = typeAliasList
                exceptions[dPackage.dri] = exceptionList
                objects[dPackage.dri] = objectList
            }
        }

        analysisMap = scope.async { context?.let { setUpAnalysis(context) } ?: mapOf() }

        allClasslikes = scope.async { computeClasslikes(module) }
        classGraph = scope.async {
            computeClassGraph(
                allClasslikes.await(),
                externalDocumentablesProvider,
                context?.configuration?.sourceSets
            )
        }
        documentablesGraph = scope.async { computeDocumentablesGraph(classGraph.await()) }

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

    suspend fun classGraph(): Map<DRI, ClassNode> = classGraph.await()

    suspend fun documentablesGraph(): Map<DRI, Documentable> = documentablesGraph.await()

    suspend fun analysisMap(): Map<DokkaConfiguration.DokkaSourceSet, EnvironmentAndFacade> =
        analysisMap.await()

    suspend fun classlikesFor(dPackage: DPackage, displayLanguage: Language): List<DClasslike> {
        val classlikes = classlikes.getValue(dPackage.dri).await()
        val syntheticClasses = syntheticClasses.getValue(dPackage.dri).await()
        return if (displayLanguage == Language.JAVA) {
            classlikes
        } else {
            classlikes - syntheticClasses.toSet()
        }
    }

    suspend fun classlikesFor(classlike: DClasslike): List<DClasslike> {
        nestedClasslikesJob.join()
        return nestedClasslikes.getValue(classlike.dri).await()
    }

    suspend fun classesFor(dPackage: DPackage, displayLanguage: Language): List<DClass> {
        var classes = classes.getValue(dPackage.dri).await()
        if (displayLanguage == Language.KOTLIN)
            classes = classes.filterNot { it.isOrdinaryCompanion() }
        val syntheticClasses = syntheticClasses.getValue(dPackage.dri).await()
        return if (displayLanguage == Language.JAVA) {
            (classes + syntheticClasses).sortedBy { "${it.name()} ${it.dri}" }
        } else {
            classes - syntheticClasses.toSet()
        }
    }

    suspend fun enumsFor(dPackage: DPackage): List<DEnum> =
        enums.getValue(dPackage.dri).await()

    suspend fun interfacesFor(dPackage: DPackage): List<DInterface> =
        interfaces.getValue(dPackage.dri).await()

    suspend fun annotationsFor(dPackage: DPackage): List<DAnnotation> =
        annotations.getValue(dPackage.dri).await()

    suspend fun typeAliasesFor(dPackage: DPackage): List<DTypeAlias> =
        typeAliases.getValue(dPackage.dri).await()

    suspend fun exceptionsFor(dPackage: DPackage): List<DClass> =
        exceptions.getValue(dPackage.dri).await()

    suspend fun objectsFor(dPackage: DPackage, displayLanguage: Language): List<DObject> {
        return if (displayLanguage == Language.JAVA) {
            // TODO(b/203678085): Objects should be accessible from top-level static inner class
            emptyList()
        } else {
            objects.getValue(dPackage.dri).await()
        }
    }

    /**
     * Iterate through the all packages and create map of each class to its associated
     * extension functions.
     */
    suspend fun extensionFunctionMap(): HashMap<DRI, MutableList<DFunction>> {
        val extensionFunctionsMapping = HashMap<DRI, MutableList<DFunction>>()
        packages().forEach { dPackage ->
            dPackage.functions.forEach { function ->
                function.addToMapping(function.receiver?.type, extensionFunctionsMapping)
            }
        }
        return extensionFunctionsMapping
    }

    /**
     * If applicable, adds an extension function to the map, based on its receiver's type.
     * Takes the receiver's type as a param to allow recursive calling for nullable receivers.
     */
    private fun DFunction.addToMapping(rType: Bound?, map: HashMap<DRI, MutableList<DFunction>>) {
        when (rType) {
            null -> {} // no receiver
            is GenericTypeConstructor -> addToMapping(rType.dri, map)
            is DefinitelyNonNullable -> addToMapping(rType.inner, map)
            is Nullable -> addToMapping(rType.inner, map)
            // These types have no classlike pages, so we only document the extension fun
            // in the package summary
            is JavaObject, is PrimitiveJavaType, Void, // builtins
            is TypeAliased, is FunctionalTypeConstructor, is TypeParameter -> {}
            Dynamic -> TODO() // I don't think this case is possible?
            is UnresolvedBound -> throw RuntimeException("Unresolved receiver of $this")
            else -> throw RuntimeException("Unknown receiver for $this")
        }
    }

    private fun <T, V> T.addToMapping(v: V, map: HashMap<V, MutableList<T>>) {
        if (v !in map) map[v] = mutableListOf()
        map[v]!! += this
    }

    private fun computePackages(module: DModule): List<DPackage> {
        return module.packages
            .filterNot { thisPackage ->
                excludedPackages.any {
                    excludedRegex ->
                    excludedRegex.matches(thisPackage.packageName)
                }
            }.sortedBy { "${it.name} ${it.dri}" }
    }

    private suspend fun computeClasslikes(
        module: DModule
    ): List<DClasslike> {
        return computeClasslikes(
            module.packages.flatMap { classlikesFor(it, Language.JAVA) }
        ) // classlikesFor(JAVA) already contains synth
    }

    private fun computeClasslikes(
        docs: List<Documentable>,
        syntheticClasses: List<DClass> = emptyList()
    ): List<DClasslike> {
        return (docs.filterIsInstance<DClasslike>() + syntheticClasses)
            .filterNot { thisClasslike ->
                excludedPackages.any {
                    excludedRegex ->
                    excludedRegex.matches(thisClasslike.packageName())
                }
            }.sortedBy { "${it.name()} ${it.dri}" }
    }

    private fun computeClasses(docs: List<Documentable>): List<DClass> {
        return docs.filterIsInstance<DClass>().filterNot { it.isExceptionClass }
            .filterNot { thisClass ->
                excludedPackages.any {
                    excludedRegex ->
                    excludedRegex.matches(thisClass.packageName())
                }
            }.sortedBy { "${it.name()} ${it.dri}" }
    }

    /** Computes the syntheticClasses from top level functions that are used to document Kotlin as
     * Java
     */
    internal fun computeSyntheticClasses(dPackage: DPackage): List<DClass> {
        // functions that are JvmSynthetic are not accessible from Java, so they should not appear
        // in the documentation
        val javaFunctions = dPackage.functions.filterOutJvmSynthetic()
        val javaProperties = dPackage.properties.filterOutJvmSynthetic()
        return (javaFunctions + javaProperties)
            .mapToSyntheticNames()
            .map { (syntheticClassName, nodes) ->
                DClass(
                    dri = dPackage.dri.withClass(syntheticClassName),
                    name = syntheticClassName,
                    // TODO (b/168340963) handle kotlin as java properties
                    properties = nodes.filterIsInstance<DProperty>(),
                    constructors = emptyList(),
                    functions = nodes.filterIsInstance<DFunction>().map {
                        it.withJavaSynthetic(syntheticClassName)
                    }.sortedBy { "${it.name} ${it.dri}" },
                    classlikes = emptyList(),
                    sources = emptyMap(),
                    expectPresentInSet = null,
                    visibility = dPackage.sourceSets.associateWith { JavaVisibility.Public },
                    companion = null,
                    generics = emptyList(),
                    supertypes = emptyMap(),
                    documentation = emptyMap(),
                    modifier = dPackage.sourceSets.associateWith { JavaModifier.Final },
                    sourceSets = dPackage.sourceSets,
                    isExpectActual = false,
                    extra = PropertyContainer.empty()
                )
            }
    }

    /** Returns a map from String name of synthetic class that this Function (WithSources) would be
     * in to the Functions that are part of those classes
     *
     * This method uses @file:JvmName if it exists or the filename with "Kt" appended
     **/
    private fun List<WithSources>.mapToSyntheticNames(): Map<String, List<WithSources>> =
        map { it.sources to it }
            .groupBy({ (_, function) -> nameForSyntheticClass(function) }) { it.second }

    private fun computeEnums(docs: List<Documentable>): List<DEnum> {
        return docs.filterIsInstance<DEnum>().sortedBy { "${it.name()} ${it.dri}" }
    }

    private fun computeInterfaces(docs: List<Documentable>): List<DInterface> {
        return docs.filterIsInstance<DInterface>().sortedBy { "${it.name()} ${it.dri}" }
    }

    private fun computeAnnotations(docs: List<Documentable>): List<DAnnotation> {
        return docs.filterIsInstance<DAnnotation>().sortedBy { "${it.name()} ${it.dri}" }
    }

    private fun computeTypesAliases(dPackage: DPackage): List<DTypeAlias> {
        return dPackage.typealiases.sortedBy { "${it.name} ${it.dri}" }
    }

    private fun computeExceptions(docs: List<Documentable>): List<DClass> {
        return docs.filterIsInstance<DClass>().filter { it.isExceptionClass }
            .sortedBy { "${it.name()} ${it.dri}" }
    }

    private fun computeObjects(docs: List<Documentable>): List<DObject> {
        val companions = docs.filterIsInstance<WithCompanion>().mapNotNull { it.companion?.dri }
        val allObjects = docs.filterIsInstance<DObject>()
        val nonCompanions = allObjects.filter { !companions.contains(it.dri) }
        return nonCompanions.sortedBy { "${it.name()} ${it.dri}" }
    }
}
