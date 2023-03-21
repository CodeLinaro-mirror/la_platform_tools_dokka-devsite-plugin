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
import com.google.devsite.renderer.converters.gettersAndSetters
import com.google.devsite.renderer.converters.isExceptionClass
import com.google.devsite.renderer.converters.isHoistedFromCompanion
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
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.driOrNull
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
    val displayLanguage: Language,
    module: DModule,
    scope: CoroutineScope,
    context: DokkaContext? = null,
    private val externalDocumentablesProvider: ExternalDocumentablesProvider? = null,
    // TODO(handle packages with no common or JVM targets, as-Java. b/265948930)
    private val excludedPackages: Set<Regex> = emptySet(),
    val fileMetadataMap: Map<String, LibraryMetadata> = emptyMap(),
    val baseSourceLink: String? = null,
    val annotationsNotToDisplay: Set<String> = emptySet(),
) {
    internal var classlikesDone: AtomicInteger = AtomicInteger()

    private val packages = scope.async { computePackages(module) }
    // Includes even should-not-be-displayed classlikes
    private val classlikes = mutableMapOf<DRI, Deferred<List<DClasslike>>>()
    private val classes = mutableMapOf<DRI, Deferred<List<DClass>>>()
    private val syntheticClasses = mutableMapOf<DRI, Deferred<Set<DClass>>>()
    private val enums = mutableMapOf<DRI, Deferred<List<DEnum>>>()
    private val interfaces = mutableMapOf<DRI, Deferred<List<DInterface>>>()
    private val annotations = mutableMapOf<DRI, Deferred<List<DAnnotation>>>()
    private val typeAliases = mutableMapOf<DRI, Deferred<List<DTypeAlias>>>()
    private val exceptions = mutableMapOf<DRI, Deferred<List<DClass>>>()
    private val companions = mutableMapOf<DRI, Deferred<Map<DRI, DObject>>>()
    private val interestingObjects = mutableMapOf<DRI, Deferred<List<DObject>>>()
    private val extensionFunctionMap = scope.async { computeExtensionFunctionMap() }
    private val extensionPropertyMap = scope.async { computeExtensionPropertyMap() }

    private val allClasslikes: Deferred<List<DClasslike>>
    private val allCompanions: Deferred<Map<DRI, DObject>>
    private val nestedClasslikesJob: Job
    // Filtering for should-show should be done by accessors of this field
    private val nestedClasslikes = mutableMapOf<DRI, Deferred<List<DClasslike>>>()
    private val classGraph: Deferred<ClassGraph>
    private val documentablesGraph: Deferred<DocumentablesGraph>
    private val analysisMap: Deferred<Map<DokkaConfiguration.DokkaSourceSet, DokkaResolutionFacade>>

    internal val logger = context?.logger ?: DokkaConsoleLogger(LoggingLevel.WARN)

    init {
        scope.apply {
            for (dPackage in module.packages) {
                val children = async { dPackage.explodedChildren }
                val companionsMap = async {
                    computeCompanions(children.await().filterIsInstance<WithCompanion>())
                }
                val interestingObjectsList = async {
                    computeInterestingObjects(
                        children.await().filterIsInstance<DObject>(),
                        companionsMap.await().keys
                    )
                }
                val syntheticClassList = async { computeSyntheticClasses(dPackage) }
                val combinedClasslikesList = async {
                    computeClasslikes(
                        children.await(),
                        syntheticClassList.await()
                    )
                }

                val classList = async { computeClasses(combinedClasslikesList.await()) }

                val enumList = async { computeEnums(children.await()) }
                val interfaceList = async { computeInterfaces(children.await()) }
                val annotationList = async { computeAnnotations(children.await()) }
                val typeAliasList = async { computeTypesAliases(dPackage) }
                val exceptionList = async { computeExceptions(children.await()) }

                classlikes[dPackage.dri] = combinedClasslikesList
                classes[dPackage.dri] = classList
                syntheticClasses[dPackage.dri] = syntheticClassList
                enums[dPackage.dri] = enumList
                interfaces[dPackage.dri] = interfaceList
                annotations[dPackage.dri] = annotationList
                typeAliases[dPackage.dri] = typeAliasList
                exceptions[dPackage.dri] = exceptionList
                interestingObjects[dPackage.dri] = interestingObjectsList
                companions[dPackage.dri] = companionsMap
            }
        }

        analysisMap = scope.async { context?.let { setUpAnalysis(context) } ?: mapOf() }

        allClasslikes = scope.async { computeClasslikes(module) }
        allCompanions = scope.async {
            module.packages.flatMap { companions[it.dri]!!.await().entries }
                .associate { it.key to it.value }
        }
        classGraph = scope.async {
            computeClassGraph(
                allClasslikes.await() + allCompanions.await().values,
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

    suspend fun analysisMap(): Map<DokkaConfiguration.DokkaSourceSet, DokkaResolutionFacade> =
        analysisMap.await()

    suspend fun classlikesFor(dPackage: DPackage): List<DClasslike> =
        classlikes.getValue(dPackage.dri).await()

    /**
     * Returns a classlike's nested classlikes.
     * Does not include should-not-be-documented Documentables.
     */
    suspend fun nestedClasslikesFor(classlike: DClasslike): List<DClasslike> {
        nestedClasslikesJob.join()
        val theseNestedClasslikes = nestedClasslikes.getValue(classlike.dri).await()
        // Remove boring companions, as defined by [shouldNotBeDisplayed]
        return theseNestedClasslikes.filterNot { shouldNotBeDisplayed(it) }
    }

    suspend fun classesFor(dPackage: DPackage): List<DClass> =
        classes.getValue(dPackage.dri).await()

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

    suspend fun interestingObjectsFor(dPackage: DPackage) =
        interestingObjects.getValue(dPackage.dri).await()

    suspend fun extensionFunctionsFor(dClasslike: DClasslike) =
        extensionFunctionMap.await().getOrDefault(dClasslike.dri, emptyList())

    suspend fun extensionPropertiesFor(dClasslike: DClasslike) =
        extensionPropertyMap.await().getOrDefault(dClasslike.dri, emptyList())

    /**
     * Iterate through the all packages and create map of each class to its associated
     * extension functions.
     */
    private suspend fun computeExtensionFunctionMap():
        HashMap<DRI, MutableList<DFunction>> {
        val extensionFunctionsMapping = HashMap<DRI, MutableList<DFunction>>()
        packages().forEach { dPackage ->
            dPackage.functions.forEach { function ->
                function.addToMapping(function.receiver?.type, extensionFunctionsMapping)
            }
            if (displayLanguage == Language.JAVA) {
                dPackage.properties.gettersAndSetters().forEach { accessor ->
                    accessor.addToMapping(
                        accessor.receiver?.type,
                        extensionFunctionsMapping
                    )
                }
            }
        }
        return extensionFunctionsMapping
    }

    /**
     * Iterate through the all packages and create map of each class to its associated
     * extension functions.
     */
    private suspend fun computeExtensionPropertyMap(): HashMap<DRI, MutableList<DProperty>> {
        val extensionPropertiesMapping = HashMap<DRI, MutableList<DProperty>>()
        // Extension properties are only as-Java as accessors, so they count as extension functions
        if (displayLanguage == Language.KOTLIN) {
            packages().forEach { dPackage ->
                dPackage.properties.forEach { property ->
                    property.addToMapping(
                        property.receiver?.type?.driOrNull,
                        extensionPropertiesMapping
                    )
                }
            }
        }
        return extensionPropertiesMapping
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

    private fun <T, V> T.addToMapping(v: V?, map: HashMap<V, MutableList<T>>) {
        if (v == null) return
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

    /** Returns all should-be-documented classlikes in this module. */
    private suspend fun computeClasslikes(module: DModule): List<DClasslike> {
        return computeClasslikes(
            module.packages.flatMap { classlikesFor(it) }
        ) // classlikesFor already contains synth for Java
    }

    /** Returns all classlikes among all given documentables. */
    private suspend fun computeClasslikes(
        docs: List<Documentable>,
        syntheticClasses: Set<DClass> = emptySet()
    ): List<DClasslike> {
        return (docs.filterIsInstance<DClasslike>() + syntheticClasses)
            .filterNot { thisClasslike ->
                excludedPackages.any {
                    excludedRegex ->
                    excludedRegex.matches(thisClasslike.packageName())
                }
            }.filterNot { shouldNotBeDisplayed(it) }
            .sortedBy { "${it.name()} ${it.dri}" }
    }

    /**
     * Takes a sorted list of all classlikes and filters to the non-exception classes.
     */
    private fun computeClasses(classlikes: List<DClasslike>): List<DClass> =
        classlikes.filterIsInstance<DClass>().filterNot { it.isExceptionClass }

    /** Computes the syntheticClasses from top level functions that are used to document Kotlin as
     * Java
     */
    internal fun computeSyntheticClasses(dPackage: DPackage): Set<DClass> {
        // Synthetic classes don't exist in Kotlin, no reason to compute them
        if (displayLanguage == Language.KOTLIN) return emptySet()

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
                    properties = nodes.filterIsInstance<DProperty>()
                        .map { it.withJavaSynthetic(syntheticClassName) },
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
            }.toSet()
    }

    /** Returns a map from String name of synthetic class that this Function (WithSources) would be
     * in to the Functions that are part of those classes
     *
     * This method uses @file:JvmName if it exists or the filename with "Kt" appended
     **/
    private fun <T> List<T>.mapToSyntheticNames(): Map<String, List<T>>
    where T : Documentable, T : WithSources =
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

    /** Returns a Map<DRI, DObject> because `Set<Documentable>.contains` is unusable b/232944038. */
    private fun computeCompanions(docs: List<WithCompanion>) =
        docs.mapNotNull { it.companion }.associateBy { it.dri }

    /** Computes the list of objects that are interesting in the display language */
    private suspend fun computeInterestingObjects(
        allObjects: List<DObject>,
        companions: Set<DRI>
    ): List<DObject> {
        // Un-ordinary companions are companions, but also appear in the ToC.
        val interestingObjects = allObjects.filter { !it.isBoringCompanion() }
        // Error-level enforcement that non-companion objects aren't named 'Companion'
        // Thus, we can elsewhere freely use `isBoringCompanion` without checking companionhood.
        (allObjects.filter { it.name == "Companion" }.map { it.dri } - companions).forEach {
            throw RuntimeException(
                "Object with illegal name: named 'Companion' but is not a companion object: $it."
            )
        }
        return interestingObjects.sortedBy { "${it.name()} ${it.dri}" }
    }

    fun isCompanion(dObject: DObject) = runBlocking { dObject.dri in allCompanions.await().keys }

    /**
     * Returns whether this DObject is an ordinary companion and is not significant enough to show
     * on its own. This requires that it be not named, not inherit anything, and *not contain
     * anything that is not hoisted onto the containing object* (extension functions and properties
     * are not hoisted onto the containing object).
     *
     * This function can also be used on DObjects where it is unknown whether it is a companion at
     * all. This works because we enforce non-companion objects being named 'Companion' as an error.
     *
     * Returns true: is both a companion and uninteresting
     * Returns false: either is not a companion, or is interesting
     */
    private suspend fun DObject.isBoringCompanion(): Boolean =
        name == "Companion" &&
            supertypes.all { it.value.isEmpty() } &&
            children.all { it.isHoistedFromCompanion(displayLanguage) } &&
            // Even if all properties are hoisted, their Java accessors may not be.
            (
                displayLanguage != Language.JAVA || properties.gettersAndSetters()
                    .all { it.isHoistedFromCompanion(displayLanguage) }
                ) &&
            extensionFunctionsFor(this).isEmpty() &&
            extensionPropertiesFor(this).isEmpty()

    /**
     * Determines whether the [classlike] should not be displayed, which is true for objects that
     * aren't considered interesting.
     */
    internal suspend fun shouldNotBeDisplayed(classlike: DClasslike) =
        // TODO: can this reuse `interestingObjects` instead of calling `isBoringCompanion` again?
        classlike is DObject && classlike.isBoringCompanion()
}
