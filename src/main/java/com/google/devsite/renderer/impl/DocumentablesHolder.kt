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
import com.google.devsite.renderer.converters.name
import com.google.devsite.renderer.converters.nameForSyntheticClass
import com.google.devsite.renderer.converters.packageName
import com.google.devsite.renderer.converters.setUpAnalysis
import com.google.devsite.renderer.converters.withJavaSynthetic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.EnvironmentAndFacade
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeAlias
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.JavaModifier
import org.jetbrains.dokka.model.JavaVisibility
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel

/**
 * Centralized place to retrieve documentables.
 *
 * All doc rewriting should occur here.
 */
internal class DocumentablesHolder(
    module: DModule,
    scope: CoroutineScope,
    context: DokkaContext? = null,
    private val excludedPackages: Set<Regex> = emptySet()
) {
    private val packages = scope.async { computePackages(module) }

    private val classlikes = mutableMapOf<DRI, Deferred<List<DClasslike>>>()
    private val classes = mutableMapOf<DRI, Deferred<List<DClass>>>()
    private val syntheticClasses = mutableMapOf<DRI, Deferred<List<DClass>>>()
    private val enums = mutableMapOf<DRI, Deferred<List<DEnum>>>()
    private val interfaces = mutableMapOf<DRI, Deferred<List<DInterface>>>()
    private val annotations = mutableMapOf<DRI, Deferred<List<DAnnotation>>>()
    private val typeAliases = mutableMapOf<DRI, Deferred<List<DTypeAlias>>>()
    private val exceptions = mutableMapOf<DRI, Deferred<List<DClass>>>()

    private val allClasslikes: Deferred<List<DClasslike>>
    private val nestedClasslikesJob: Job
    private val nestedClasslikes = mutableMapOf<DRI, Deferred<List<DClasslike>>>()
    private val classGraph: Deferred<ClassGraph>
    private val documentablesGraph: Deferred<DocumentablesGraph>
    private val analysisMap: Deferred<Map<DokkaConfiguration.DokkaSourceSet, EnvironmentAndFacade>>

    internal val logger = context?.logger ?: DokkaConsoleLogger(LoggingLevel.WARN)

    init {
        scope.apply {
            for (packageDoc in module.packages) {
                val children = async { packageDoc.explodedChildren }
                val syntheticClassList = async { computeSyntheticClasses(packageDoc) }
                val classlikesList = async { computeClasslikes(children.await(),
                    syntheticClassList.await()) }
                val classList = async { computeClasses(children.await()) }

                val enumList = async { computeEnums(children.await()) }
                val interfaceList = async { computeInterfaces(children.await()) }
                val annotationList = async { computeAnnotations(children.await()) }
                val typeAliasList = async { computeTypesAliases(packageDoc) }
                val exceptionList = async { computeExceptions(children.await()) }

                classlikes[packageDoc.dri] = classlikesList
                classes[packageDoc.dri] = classList
                syntheticClasses[packageDoc.dri] = syntheticClassList
                enums[packageDoc.dri] = enumList
                interfaces[packageDoc.dri] = interfaceList
                annotations[packageDoc.dri] = annotationList
                typeAliases[packageDoc.dri] = typeAliasList
                exceptions[packageDoc.dri] = exceptionList
            }
        }

        analysisMap = scope.async { context?.let { setUpAnalysis(context) } ?: mapOf() }

        allClasslikes = scope.async { computeClasslikes(module) }
        classGraph = scope.async { computeClassGraph(allClasslikes.await()) }
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

    suspend fun classlikesFor(packageDoc: DPackage): List<DClasslike> =
        classlikes.getValue(packageDoc.dri).await()

    suspend fun classlikesFor(classlike: DClasslike): List<DClasslike> {
        nestedClasslikesJob.join()
        return nestedClasslikes.getValue(classlike.dri).await()
    }

    suspend fun classesFor(packageDoc: DPackage, displayLanguage: Language): List<DClass> {
        return if (displayLanguage == Language.JAVA) {
            (classes.getValue(packageDoc.dri).await() +
                syntheticClasses.getValue(packageDoc.dri).await()).sortedBy { it.name() }
        } else {
            classes.getValue(packageDoc.dri).await()
        }
    }

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

    /**
     * Iterate through the all packages and create map of each class to its associated
     * extension functions.
     */
    suspend fun extensionFunctionMap(): HashMap<String, MutableList<DFunction>> {
        val extensionFunctionsMapping = HashMap<String, MutableList<DFunction>>()
        packages().forEach { packageDoc ->
            packageDoc.functions.forEach { function ->
                val receiver = function.receiver
                if (receiver != null) {
                    try {
                        val genericTypeConstructor = receiver.type as GenericTypeConstructor
                        val className = genericTypeConstructor.dri.classNames.toString()
                        val list =
                            extensionFunctionsMapping.getOrDefault(className, mutableListOf())
                        list.add(function)
                        extensionFunctionsMapping[className] = list
                    } catch (_: ClassCastException) {
                        // Can't cast function.type; skip to next function
                    }
                }
            }
        }
        return extensionFunctionsMapping
    }

    private fun computePackages(module: DModule): List<DPackage> {
        return module.packages
            .filterNot { thisPackage -> excludedPackages.any {
                excludedRegex -> excludedRegex.matches(thisPackage.packageName)
            } }.sortedBy { it.name }
    }

    private suspend fun computeClasslikes(
        module: DModule
    ): List<DClasslike> {
        return computeClasslikes(
            module.packages.flatMap { classlikesFor(it) }) // classlikesFor already contains synth
    }

    private fun computeClasslikes(
        docs: List<Documentable>,
        syntheticClasses: List<DClass> = emptyList()
    ): List<DClasslike> {
        return (docs.filterIsInstance<DClasslike>() + syntheticClasses)
            .filterNot { thisClasslike -> excludedPackages.any {
            excludedRegex -> excludedRegex.matches(thisClasslike.packageName())
        } }.sortedBy { it.name() }
    }

    private fun computeClasses(docs: List<Documentable>): List<DClass> {
        return docs.filterIsInstance<DClass>().filterNot { it.isExceptionClass }
        .filterNot { thisClass -> excludedPackages.any {
            excludedRegex -> excludedRegex.matches(thisClass.packageName())
        } }.sortedBy { it.name() }
    }

    /** Computes the syntheticClasses from top level functions that are used to document Kotlin as
     * Java
     */
    private fun computeSyntheticClasses(packageDoc: DPackage): List<DClass> {
        // functions that are JvmSynthetic are not accessible from Java, so they should not appear
        // in the documentation
        val javaFunctions = packageDoc.functions.filterOutJvmSynthetic()
        val javaProperties = packageDoc.properties.filterOutJvmSynthetic()
        return (javaFunctions + javaProperties)
            .mapToSyntheticNames()
            .map { (syntheticClassName, nodes) ->
                DClass(
                    dri = packageDoc.dri.withClass(syntheticClassName),
                    name = syntheticClassName,
                    // TODO (b/168340963) handle kotlin as java properties
                    properties = nodes.filterIsInstance<DProperty>(),
                    constructors = emptyList(),
                    functions = nodes.filterIsInstance<DFunction>().map {
                        it.withJavaSynthetic(syntheticClassName)
                    }.sortedBy { it.name },
                    classlikes = emptyList(),
                    sources = emptyMap(),
                    expectPresentInSet = null,
                    visibility = packageDoc.sourceSets.associateWith { JavaVisibility.Public },
                    companion = null,
                    generics = emptyList(),
                    supertypes = emptyMap(),
                    documentation = emptyMap(),
                    modifier = packageDoc.sourceSets.associateWith { JavaModifier.Final },
                    sourceSets = packageDoc.sourceSets,
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
    private fun <T> List<T>.mapToSyntheticNames()
        where T : WithSources, T : WithExtraProperties<*> =
        map { it.sources to it }
            .groupBy({ (_, function) -> nameForSyntheticClass(function) }) { it.second }

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

    private fun computeExceptions(docs: List<Documentable>): List<DClass> {
        return docs.filterIsInstance<DClass>().filter { it.isExceptionClass }.sortedBy { it.name() }
    }
}
