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

package com.google.devsite.renderer.converters

import com.google.devsite.ClasslikeSummaryList
import com.google.devsite.ConstructorSummaryList
import com.google.devsite.FunctionSummaryList
import com.google.devsite.LinkDescriptionSummaryList
import com.google.devsite.PropertySummaryList
import com.google.devsite.TypeSummaryItem
import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultClassHierarchy
import com.google.devsite.components.impl.DefaultClasslike
import com.google.devsite.components.impl.DefaultClasslikeSignature
import com.google.devsite.components.impl.DefaultClasslikeSummary
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultDevsitePlatformSelector
import com.google.devsite.components.impl.DefaultInheritedSymbols
import com.google.devsite.components.impl.DefaultMetadataComponent
import com.google.devsite.components.impl.DefaultRelatedSymbols
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableRowSummaryItem
import com.google.devsite.components.impl.DefaultTableTitle
import com.google.devsite.components.impl.emptyInheritedSymbolsList
import com.google.devsite.components.impl.emptySummaryList
import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.pages.Classlike.TitledList
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.symbols.ClasslikeSignature
import com.google.devsite.components.symbols.ClasslikeSummary
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.MetadataComponent
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSignature
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.table.ClassHierarchy
import com.google.devsite.components.table.InheritedSymbolsList
import com.google.devsite.components.table.RelatedSymbols
import com.google.devsite.components.table.SummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableRowSummaryItem
import com.google.devsite.components.table.TableTitle
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.ClassGraph
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import com.google.devsite.util.LibraryMetadata
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.parent
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.dokka.model.ExtraModifiers
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.InheritedMember
import org.jetbrains.dokka.model.KotlinModifier
import org.jetbrains.dokka.model.WithConstructors
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.WithSupertypes
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.model.toAdditionalModifiers

/** Converts documentable class-likes into the classlike component. */
internal abstract class ClasslikeDocumentableConverter(
    private val displayLanguage: Language,
    private val classlike: DClasslike,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder,
    private val classExtensionFunctions: List<DFunction> = emptyList(),
    private val classExtensionProperties: List<DProperty> = emptyList()
) {
    protected abstract val header: DefaultDevsitePlatformSelector?

    private val paramConverter = ParameterDocumentableConverter(displayLanguage, pathProvider)
    // TODO(KMP b/254490320)
    protected val javadocConverter = DocTagConverter(displayLanguage, pathProvider, docsHolder)
    // TODO(KMP b/256172699)
    private val enumConverter =
        EnumValueDocumentableConverter(displayLanguage, pathProvider, javadocConverter)

    protected abstract val functionToSummaryConverter:
        (DFunction, ModifierHints) -> TypeSummaryItem<FunctionSignature>
    protected abstract val functionToDetailConverter:
        (DFunction, ModifierHints) -> SymbolDetail<FunctionSignature>
    protected abstract val propertyToSummaryConverter:
        (DProperty, ModifierHints) -> TypeSummaryItem<PropertySignature>
    protected abstract val propertyToDetailConverter:
        (DProperty, ModifierHints) -> SymbolDetail<PropertySignature>
    protected abstract val constructorToSummaryConverter:
        (DFunction) -> TableRowSummaryItem<Nothing?, SymbolSummary<FunctionSignature>>
    protected abstract val constructorToDetailConverter:
        (DFunction, ModifierHints) -> SymbolDetail<FunctionSignature>

    /** @return the classlike component */
    suspend fun classlike(): DevsitePage<Classlike> = coroutineScope {
        var (declaredFunctions, declaredProperties) = classlike.nonInheritedTypes()
        val inheritedAll = (classlike.children + classlike.properties.gettersAndSetters())
            .inheritedTypes(classlike.supertypesForDisplayLanguage())
        var (companionFunctions, companionProperties) = classlike.companionFunctionsAndProperties()

        // Java documentation needs to respect @jvm* annotations
        if (displayLanguage == Language.JAVA) {
            declaredFunctions = declaredFunctions.filterOutJvmSynthetic().map { it.withJvmName() }
            declaredProperties = declaredProperties.filterOutJvmSynthetic()
        }

        // Some symbols are moved from the companion object type to the enclosing class in java
        if (displayLanguage == Language.JAVA) {
            // Objects that are not top-level
            if (classlike is DObject && docsHolder.isCompanion(classlike)) {
                // Hoist companion JvmFields
                declaredProperties = declaredProperties.filterNot { it.isJvmField() }
            } else if (classlike is DObject) {
                declaredProperties += DProperty(
                    dri = classlike.dri.copy(
                        callable = Callable(name = "INSTANCE", params = emptyList())
                    ),
                    name = "INSTANCE",
                    documentation = emptyMap(),
                    expectPresentInSet = classlike.expectPresentInSet,
                    sources = classlike.sources,
                    visibility = classlike.visibility,
                    type = GenericTypeConstructor(dri = classlike.dri, projections = emptyList()),
                    receiver = null,
                    setter = null,
                    getter = null,
                    modifier = emptyMap(),
                    sourceSets = classlike.sourceSets,
                    generics = emptyList(),
                    isExpectActual = false,
                    extra = PropertyContainer.withAll(
                        classlike.sourceSets.map {
                            mapOf(
                                it to setOf(ExtraModifiers.JavaOnlyModifiers.Static)
                            ).toAdditionalModifiers()
                        } + classlike.sourceSets.map {
                            Annotations(
                                mapOf(
                                    it to listOf(
                                        Annotations.Annotation(
                                            dri = DRI(
                                                packageName = "kotlin.jvm",
                                                classNames = "JvmField"
                                            ),
                                            params = emptyMap()
                                        )
                                    )
                                )
                            )
                        }
                    )
                )
            } else {
                // Classlikes that are not (top-level) objects
                declaredProperties += companionProperties.filter { it.isJavaStaticField() }
                declaredProperties += companionProperties.filter { it.isJvmFieldAnnotated() }.map {
                    // It is technically incorrect to put @JvmStatic on a property, but we use this
                    // to remember that we should later inject the `static` modifier to this
                    it.withNewExtras(it.extra.addAnnotation(JvmStatic))
                }
                declaredFunctions += companionFunctions.filter { it.isJavaStaticMethod() }
            }
        }

        declaredFunctions = declaredFunctions.sortedBy {
            "${it.name} ${it.receiver} ${it.parameters.joinToString(" ")}"
        }
        declaredProperties = declaredProperties.sortedBy { it.name }
        companionFunctions = companionFunctions.sortedBy {
            "${it.name} ${it.receiver} ${it.parameters.joinToString(" ")}"
        }
        companionProperties = companionProperties.sortedBy { it.name }

        val enumValues = (classlike as? DEnum)?.entries.orEmpty().sortedBy { it.name }

        var allConstructors = (classlike as? WithConstructors)?.constructors.orEmpty().sortedBy {
            String.format("%03d", it.parameters.size) + " " + it.parameters.joinToString()
        }
        if (classlike is WithConstructors && allConstructors.isEmpty() && classlike.isFromJava() &&
            classlike !is DAnnotation
        ) {
            allConstructors = listOf(createDefaultConstructorFor(classlike))
        }
        val enumValuesSummary = async {
            enumValuesToSummary(enumValuesTitle(), enumValues)
        }
        val nestedTypesSummary = async {
            nestedTypesToSummary(
                // These are filtered for not-shown classlikes when accessed
                docsHolder.nestedClasslikesFor(classlike, displayLanguage),
                docsHolder.classGraph()
            )
        }
        val constantsSummary = async {
            propertiesToSummary(
                constantsTitle(), (declaredProperties + companionProperties).constants()
            )
        }
        val publicPropertiesSummary = async {
            propertiesToSummary(
                publicPropertiesTitle(displayLanguage),
                declaredProperties.filter(::isPublicNonConst)
            )
        }
        val protectedPropertiesSummary = async {
            propertiesToSummary(
                protectedPropertiesTitle(displayLanguage),
                declaredProperties.filter(::isProtectedNonConst)
            )
        }
        val publicConstructorsSummary = async {
            constructorsToSummary(
                publicConstructorsTitle(),
                allConstructors.filter(::isPublic)
            )
        }
        val protectedConstructorsSummary = async {
            constructorsToSummary(
                protectedConstructorsTitle(),
                allConstructors.filter(::isProtected)
            )
        }
        val publicFunctionsSummary = async {
            functionsToSummary(
                publicMethodsTitle(displayLanguage),
                declaredFunctions.filter(::isPublic)
            )
        }
        val protectedFunctionsSummary = async {
            functionsToSummary(
                protectedMethodsTitle(displayLanguage),
                declaredFunctions.filter(::isProtected)
            )
        }
        val publicCompanionFunctionsSummary = async {
            emptyIfJava() ?: functionsToSummary(
                publicCompanionFunctionsTitle(),
                companionFunctions.filter(::isPublic)
            )
        }
        val protectedCompanionFunctionsSummary = async {
            emptyIfJava() ?: functionsToSummary(
                protectedCompanionFunctionsTitle(),
                companionFunctions.filter(::isProtected)
            )
        }
        val publicCompanionPropertiesSummary = async {
            emptyIfJava() ?: propertiesToSummary(
                publicCompanionPropertiesTitle(),
                companionProperties.filter(::isPublicNonConst)
            )
        }
        val protectedCompanionPropertiesSummary = async {
            emptyIfJava() ?: propertiesToSummary(
                protectedCompanionPropertiesTitle(),
                companionProperties.filter(::isProtectedNonConst)
            )
        }

        val enumDetails = (classlike as? DEnum)?.let {
            async { enumValuesToDetail(it, enumValues) }
        }
        val constantsDetails =
            async { propertiesToDetail((declaredProperties + companionProperties).constants()) }
        val publicPropertiesDetails =
            async { propertiesToDetail(declaredProperties.filter(::isPublicNonConst)) }
        val protectedPropertiesDetails =
            async { propertiesToDetail(declaredProperties.filter(::isProtectedNonConst)) }
        val publicConstructorsDetails =
            async { constructorsToDetail(allConstructors.filter(::isPublic)) }
        val protectedConstructorsDetails =
            async { constructorsToDetail(allConstructors.filter(::isProtected)) }
        val publicFunctionsDetails =
            async { functionsToDetail(declaredFunctions.filter(::isPublic)) }
        val protectedFunctionsDetails =
            async { functionsToDetail(declaredFunctions.filter(::isProtected)) }
        val publicCompanionFunctionsDetail = async {
            emptyListIfJava() ?: functionsToDetail(companionFunctions.filter(::isPublic))
        }
        val protectedCompanionFunctionsDetail = async {
            emptyListIfJava() ?: functionsToDetail(companionFunctions.filter(::isProtected))
        }
        val publicCompanionPropertiesDetail = async {
            emptyListIfJava() ?: propertiesToDetail(companionProperties.filter(::isPublicNonConst))
        }
        val protectedCompanionPropertiesDetail = async {
            emptyListIfJava()
                ?: propertiesToDetail(companionProperties.filter(::isProtectedNonConst))
        }

        val signature = async { computeSignature(classlike, docsHolder.classGraph()) }
        val hierarchy = async { computeHierarchy() }
        val relatedSymbols = async { findRelatedSymbols() }
        val inheritedTypes = async { computeInheritedSymbols(inheritedAll) }
        val metadataComponent = async { getMetadata() }

        var extensionFunctions = classExtensionFunctions +
            if (displayLanguage == Language.JAVA)
                classExtensionProperties.gettersAndSetters(allowDefault = true)
            else emptyList()
        extensionFunctions = extensionFunctions
            // Sort by the class the extension function came from first, so they will be grouped
            // together in a logical way
            .sortedBy { nameForSyntheticClass(it) + it.name }
            // Convert DRIs to this class so link from summary to detail will stay on class page
            .map { it.withDRIOfClass(classlike) }
        if (displayLanguage == Language.JAVA) {
            extensionFunctions = extensionFunctions.filterNot {
                it.isSuspendFunction()
            }
        }
        val extensionFunctionsSummary =
            async { functionsToSummary(extensionFunctionsTitle(), extensionFunctions) }
        val extensionFunctionsDetail = async { functionsToDetail(extensionFunctions) }

        // Extension properties are only as-Java as accessors, so they count as extension functions
        var extensionProperties = when (displayLanguage) {
            Language.KOTLIN -> classExtensionProperties
            Language.JAVA -> emptyList()
        }
        extensionProperties = extensionProperties
            // Sort by the class the extension property came from first, so they will be grouped
            // together in a logical way
            .sortedBy { nameForSyntheticClass(it) + it.name }
            // Convert DRIs to this class so link from summary to detail will stay on class page
            .map { it.withDRIOfClass(classlike) }
        val extensionPropertiesSummary =
            async { propertiesToSummary(extensionPropertiesTitle(), extensionProperties) }
        val extensionPropertiesDetail = async { propertiesToDetail(extensionProperties) }

        val processed = docsHolder.classlikesDone.getAndIncrement()
        if (processed % 100 == 0) {
            docsHolder.logger.debug("Dackka: $displayLanguage classlikes processed: $processed")
        }

        val (inheritedFunctions, inheritedConstants, inheritedProperties) = inheritedTypes.await()

        DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage,
                path = pathProvider.forReference(classlike.dri).url,
                bookPath = pathProvider.book,
                title = classlike.name(),
                content = DefaultClasslike(
                    Classlike.Params(
                        header = header,
                        displayLanguage = displayLanguage,
                        signature = signature.await(),
                        hierarchy = hierarchy.await(),
                        relatedSymbols = relatedSymbols.await(),
                        description = javadocConverter.metadata(classlike),
                        nestedTypesSummary = nestedTypesSummary.await(),
                        enumValuesSummary = enumValuesSummary.await(),
                        enumValuesDetails = TitledList(
                            enumValuesTitle(),
                            enumDetails?.await() ?: emptyList()
                        ),
                        constantsSummary = constantsSummary.await(),
                        constantsDetails = TitledList(
                            constantsTitle(),
                            constantsDetails.await()
                        ),
                        publicCompanionFunctionsSummary = publicCompanionFunctionsSummary.await(),
                        publicCompanionFunctionsDetails = TitledList(
                            publicCompanionFunctionsTitle(),
                            publicCompanionFunctionsDetail.await()
                        ),
                        protectedCompanionFunctionsSummary = protectedCompanionFunctionsSummary
                            .await(),
                        protectedCompanionFunctionsDetails = TitledList(
                            protectedCompanionFunctionsTitle(),
                            protectedCompanionFunctionsDetail.await()
                        ),
                        publicCompanionPropertiesSummary = publicCompanionPropertiesSummary.await(),
                        publicCompanionPropertiesDetails = TitledList(
                            publicCompanionPropertiesTitle(),
                            publicCompanionPropertiesDetail.await()
                        ),
                        protectedCompanionPropertiesSummary = protectedCompanionPropertiesSummary
                            .await(),
                        protectedCompanionPropertiesDetails = TitledList(
                            protectedCompanionPropertiesTitle(),
                            protectedCompanionPropertiesDetail.await()
                        ),
                        publicConstructorsSummary = publicConstructorsSummary.await(),
                        publicConstructorsDetails = TitledList(
                            publicConstructorsTitle(),
                            publicConstructorsDetails.await()
                        ),
                        protectedConstructorsSummary = protectedConstructorsSummary.await(),
                        protectedConstructorsDetails = TitledList(
                            protectedConstructorsTitle(),
                            protectedConstructorsDetails.await()
                        ),
                        publicFunctionsSummary = publicFunctionsSummary.await(),
                        publicFunctionsDetails = TitledList(
                            publicMethodsTitle(displayLanguage),
                            publicFunctionsDetails.await()
                        ),
                        protectedFunctionsSummary = protectedFunctionsSummary.await(),
                        protectedFunctionsDetails = TitledList(
                            protectedMethodsTitle(displayLanguage),
                            protectedFunctionsDetails.await()
                        ),
                        publicPropertiesSummary = publicPropertiesSummary.await(),
                        publicPropertiesDetails = TitledList(
                            publicPropertiesTitle(displayLanguage),
                            publicPropertiesDetails.await()
                        ),
                        protectedPropertiesSummary = protectedPropertiesSummary.await(),
                        protectedPropertiesDetails = TitledList(
                            protectedPropertiesTitle(displayLanguage),
                            protectedPropertiesDetails.await()
                        ),
                        extensionFunctionsSummary = extensionFunctionsSummary.await(),
                        extensionFunctionsDetails = TitledList(
                            extensionFunctionsTitle(),
                            extensionFunctionsDetail.await()
                        ),
                        extensionPropertiesSummary = extensionPropertiesSummary.await(),
                        extensionPropertiesDetails = TitledList(
                            extensionPropertiesTitle(),
                            extensionPropertiesDetail.await()
                        ),
                        inheritedFunctions = inheritedFunctions ?: emptyInheritedSymbolsList(),
                        inheritedConstants = inheritedConstants ?: emptyInheritedSymbolsList(),
                        inheritedProperties = inheritedProperties ?: emptyInheritedSymbolsList(),
                    )
                ),
                metadataComponent = metadataComponent.await(),
                includedHeadTagPath = pathProvider.includedHeadTagsPath,
            )
        )
    }

    /** This is intended to be used as (some thing) = emptyIfJava ?: actuallyComputeItIfKotlin() */
    private fun <T : SummaryItem> emptyIfJava() =
        if (displayLanguage == Language.JAVA) emptySummaryList<T>() else null

    private fun <T> emptyListIfJava() =
        if (displayLanguage == Language.JAVA) emptyList<T>() else null

    private fun nestedTypesToSummary(nestedClasslikes: List<DClasslike>, classGraph: ClassGraph):
        ClasslikeSummaryList {
        val components = nestedClasslikes.map { nestedClasslike ->
            errorContextInjector(nestedClasslike) {
                DefaultTableRowSummaryItem(
                    TableRowSummaryItem.Params(
                        title = null,
                        description = DefaultClasslikeSummary(
                            ClasslikeSummary.Params(
                                signature = computeSignature(nestedClasslike, classGraph),
                                description = javadocConverter.summaryDescription(nestedClasslike)
                            )
                        ) as ClasslikeSummary
                    )
                )
            }
        }

        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(
                    TableTitle.Params(
                        title = nestedTypesTitle(),
                        big = true
                    )
                ),
                items = components
            )
        )
    }

    private fun functionsToSummary(name: String? = null, functions: List<DFunction>):
        FunctionSummaryList {
        val components = functions.map {
            val modifierHints = ModifierHints(
                displayLanguage = displayLanguage,
                type = DFunction::class.java,
                containingType = classlike::class.java,
                isFromJava = classlike.isFromJava(),
                isSummary = true,
                injectStatic = it.isJavaStaticMethod()
            )
            errorContextInjector(it) {
                functionToSummaryConverter(it, modifierHints)
            }
        }

        return DefaultSummaryList(
            SummaryList.Params(
                header = name?.let {
                    DefaultTableTitle(
                        TableTitle.Params(
                            title = name,
                            big = true
                        )
                    )
                },
                items = components
            )
        )
    }

    private fun constructorsToSummary(name: String, constructors: List<DFunction>):
        ConstructorSummaryList {
        val components = constructors.map {
            errorContextInjector(it) {
                constructorToSummaryConverter(it)
            }
        }

        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(
                    TableTitle.Params(
                        title = name,
                        big = true
                    )
                ),
                items = components
            )
        )
    }

    private fun functionsToDetail(functions: List<DFunction>):
        List<SymbolDetail<FunctionSignature>> {
        return functions.map {
            val modifierHints = ModifierHints(
                displayLanguage = displayLanguage,
                isSummary = false,
                type = DFunction::class.java,
                containingType = classlike::class.java,
                isFromJava = classlike.isFromJava(),
                injectStatic = it.isJavaStaticMethod()
            )
            errorContextInjector(it) {
                functionToDetailConverter(it, modifierHints)
            }
        }
    }

    private fun constructorsToDetail(functions: List<DFunction>):
        List<SymbolDetail<FunctionSignature>> {
        val modifierHints = ModifierHints(
            displayLanguage = displayLanguage,
            type = DFunction::class.java,
            containingType = classlike::class.java,
            isFromJava = classlike.isFromJava(),
            isSummary = false,
            isConstructor = true
        )
        return functions.map {
            errorContextInjector(it) {
                constructorToDetailConverter(it, modifierHints)
            }
        }
    }

    private fun enumValuesToSummary(title: String, enumVals: List<DEnumEntry>):
        LinkDescriptionSummaryList {
        val components = enumVals.map { errorContextInjector(it) { enumConverter.summary(it) } }
        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(
                    TableTitle.Params(
                        title = title,
                        big = true
                    )
                ),
                items = components
            )
        )
    }

    private fun propertiesToSummary(name: String? = null, properties: List<DProperty>):
        PropertySummaryList {
        val components = properties.map {
            val modifierHints = ModifierHints(
                displayLanguage = displayLanguage,
                type = DProperty::class.java,
                containingType = classlike::class.java,
                isFromJava = classlike.isFromJava(),
                isSummary = true,
                injectStatic = it.isJavaStaticField()
            )
            errorContextInjector(it) {
                propertyToSummaryConverter(it, modifierHints)
            }
        }

        val title = name?.let {
            DefaultTableTitle(
                TableTitle.Params(
                    title = it,
                    big = true
                )
            )
        }

        return DefaultSummaryList(
            SummaryList.Params(
                header = title,
                items = components
            )
        )
    }

    private fun propertiesToDetail(properties: List<DProperty>):
        List<SymbolDetail<PropertySignature>> {
        return properties.map {
            val modifierHints = ModifierHints(
                displayLanguage = displayLanguage,
                type = DProperty::class.java,
                containingType = classlike::class.java,
                isFromJava = classlike.isFromJava(),
                isSummary = false,
                injectStatic = it.isJavaStaticField()
            )
            errorContextInjector(it) {
                propertyToDetailConverter(it, modifierHints)
            }
        }
    }

    private fun enumValuesToDetail(dEnum: DEnum, enumValues: List<DEnumEntry>):
        List<SymbolDetail<PropertySignature>> {
        val modifierHints = ModifierHints(
            displayLanguage,
            type = DEnumEntry::class.java,
            containingType = classlike::class.java,
            isFromJava = classlike.isFromJava(),
            isSummary = false
        )
        return enumValues.map {
            errorContextInjector(it) {
                enumConverter.detail(dEnum, it, modifierHints)
            }
        }
    }

    protected open fun computeSignature(
        classlike: DClasslike,
        classGraph: ClassGraph,
        sourceSet: DokkaSourceSet = classlike.getExpectOrCommonSourceSet()
    ): ClasslikeSignature {
        val modifiers = classlike.modifiers(sourceSet).modifiersFor(
            ModifierHints(
                displayLanguage,
                type = classlike::class.java,
                containingType = null,
                injectStatic = classlike is DObject && displayLanguage == Language.JAVA,
                isFromJava = classlike.isFromJava(),
                isSummary = false
            )
        )
        // Does not vary by sourceSet
        val typeParameters = classlike.generics().map {
            errorContextInjector(it) {
                paramConverter.componentForTypeParameter(it, classlike.isFromJava())
            }
        }

        if (classlike !is WithSupertypes) {
            return DefaultClasslikeSignature(
                ClasslikeSignature.Params(
                    displayLanguage = displayLanguage,
                    name = pathProvider.linkForReference(classlike.dri, classlike.name()),
                    type = classlike.stringForType(displayLanguage),
                    modifiers = modifiers,
                    implements = emptyList(),
                    extends = emptyList(),
                    typeParameters = typeParameters,
                    annotationComponents = classlike.annotations(sourceSet).annotationComponents(
                        pathProvider = pathProvider,
                        displayLanguage = displayLanguage,
                        nullability = Nullability.DONT_CARE // Classlike definitions aren't null
                    )
                )
            )
        }
        // TODO(KMP ClassGraph b/253454963)
        val extends = classGraph.getValue(classlike.dri).directSuperClasses.map {
            pathProvider.linkForReference(it.dri)
        }
        val implements = classGraph.getValue(classlike.dri).directInterfaces.map {
            pathProvider.linkForReference(it.dri)
        }

        return DefaultClasslikeSignature(
            ClasslikeSignature.Params(
                displayLanguage = displayLanguage,
                name = pathProvider.linkForReference(classlike.dri, classlike.name()),
                type = classlike.stringForType(displayLanguage),
                modifiers = modifiers,
                implements = implements,
                extends = extends,
                typeParameters = typeParameters,
                annotationComponents = classlike.annotations(sourceSet).annotationComponents(
                    pathProvider = pathProvider,
                    displayLanguage = displayLanguage,
                    nullability = Nullability.DONT_CARE // Classlike definitions aren't null
                )
            )
        )
    }

    /** Walks up this class' type hierarchy and returns the hierarchy component. */
    private suspend fun computeHierarchy(): ClassHierarchy {
        if (classlike !is WithSupertypes) {
            return DefaultClassHierarchy(ClassHierarchy.Params(parents = emptyList()))
        }

        val parents = docsHolder.classGraph().getValue(classlike.dri).superClasses
        if (parents.isEmpty()) {
            // Don't show the hierarchy if this class only extends Any/Object
            return DefaultClassHierarchy(ClassHierarchy.Params(parents = emptyList()))
        }

        val classHierarchyRootDri = when (displayLanguage) {
            Language.JAVA -> DRI(packageName = "java.lang", classNames = "Object")
            Language.KOTLIN -> DRI(packageName = "kotlin", classNames = "Any")
        }
        val classHierarchyRootLink = pathProvider
            .linkForReference(classHierarchyRootDri, classHierarchyRootDri.fullName)
        val thisLink = pathProvider.linkForReference(classlike.dri, classlike.dri.fullName)

        val parentLinks = parents.map { classlike ->
            pathProvider.linkForReference(classlike.dri, classlike.dri.fullName)
        }
        val allLinks = listOf(classHierarchyRootLink) + parentLinks + listOf(thisLink)
        return DefaultClassHierarchy(ClassHierarchy.Params(allLinks))
    }

    /**
     * Creates a list of InheritedSymbols from a list of DFunctions
     */
    private fun computeInheritedSymbols(
        symbolList: List<Documentable>
    ): Triple<
        InheritedSymbolsList<FunctionSignature>?,
        InheritedSymbolsList<PropertySignature>?,
        InheritedSymbolsList<PropertySignature>?
        > {
        val symbols = when (displayLanguage) {
            Language.JAVA -> symbolList.filterOutJvmSynthetic()
            Language.KOTLIN -> symbolList
        }

        val functions = symbols.filterIsInstance<DFunction>()
            .sortedWith(functionSignatureComparator())
        val functionsRenamed = when (displayLanguage) {
            Language.JAVA -> functions.map { it.withJvmName() }
            Language.KOTLIN -> functions
        }
        val functionsSummary =
            functionsRenamed.takeIf { it.isNotEmpty() }
                ?.createInheritedCategory(title = inheritedMethodsTitle(displayLanguage)) {
                    functionsToSummary(functions = it)
                }

        val (consts, properties) = symbols.filterIsInstance<DProperty>()
            .sortedBy { "${it.name} ${it.dri}" }
            .partition { it.isConstant() }

        val constsSummary = consts.takeIf { it.isNotEmpty() }
            ?.createInheritedCategory(title = inheritedConstantsTitle()) {
                propertiesToSummary(properties = it)
            }

        val propertiesSummary = properties.takeIf { it.isNotEmpty() }
            ?.createInheritedCategory(title = inheritedPropertiesTitle(displayLanguage)) {
                propertiesToSummary(properties = it)
            }

        return Triple(functionsSummary, constsSummary, propertiesSummary)
    }

    private fun <T, U : SymbolSignature> List<T>.createInheritedCategory(
        title: String,
        summaryGen: (List<T>) -> SummaryList<TypeSummaryItem<U>>
    ): InheritedSymbolsList<U> where T : Documentable, T : WithExtraProperties<T> {
        fun createInheritedSymbolsList(parentDri: DRI, symbolList: List<T>):
            Pair<Link, SummaryList<TypeSummaryItem<U>>> {
            val link = pathProvider.linkForReference(parentDri, parentDri.fullName)
            val summary = summaryGen(symbolList)
            return link to summary
        }

        // val category = groupBy { it.driInheritedFrom() ?: it.dri.parent }
        val category = groupBy { it.dri.parent }
            .toSortedMap(compareBy { it.classNames + " " + it.fullName })
            .entries.associate { (k, v) -> createInheritedSymbolsList(k, v) }

        return DefaultInheritedSymbols(
            InheritedSymbolsList.Params(
                header = DefaultTableTitle(
                    TableTitle.Params(title, big = true)
                ),
                inheritedSymbolSummaries = category
            )
        )
    }

    /**
     * WARNING: does not work properly
     * The dri from which this documentable is inherited, or null.
     *
     * `extra[InheritedMember].inheritedFrom` does not actually contain where inherited members are
     * inherited from
     */ /*
    private fun <T> T.driInheritedFrom(): DRI?
        where T : Documentable, T : WithExtraProperties<T> =
        extra[InheritedMember]?.inheritedFrom?.values?.toSet()?.singleOrNull() */

    /** Finds the direct and indirect subclasses for this classlike, returning their component. */
    // We know our subclasses will always be DClasslikes
    private suspend fun findRelatedSymbols(): RelatedSymbols {
        val classNode = docsHolder.classGraph().getValue(classlike.dri)
        val directSubclasses = classNode.directSubClasses
        val indirectSubclasses = classNode.indirectSubClasses

        return DefaultRelatedSymbols(
            RelatedSymbols.Params(
                // TODO(KMP; b/254490320)
                directSubclasses = linksForClasslikes(directSubclasses),
                directSummary = javadocConverter.docsToSummaryDefault(directSubclasses),
                indirectSubclasses = linksForClasslikes(indirectSubclasses),
                indirectSummary = javadocConverter.docsToSummaryDefault(indirectSubclasses)
            )
        )
    }

    /**
     * Creates a metadata component for this classlike. If [getSourceEntries] returns null for the
     * classlike, this will also return null as the source entry is needed to create both the
     * library metadata and the source link.
     */
    private fun getMetadata(): MetadataComponent? {
        val entries = getSourceEntries(classlike) ?: return null
        val paths = entries.map { entry -> getSourceFilePath(entry) }
        val jsonLibraryMetadata = findMatchingJsonLibraryMetadata(paths)
        val sourceUrl = createLinkToSource(paths)

        return DefaultMetadataComponent(
            MetadataComponent.Params(
                libraryMetadata = jsonLibraryMetadata,
                sourceLinkUrl = sourceUrl
            )
        )
    }

    /**
     * Iterate through the library metadata Map to find a [LibraryMetadata] that matches the
     * current class being processed.  Otherwise, return null.
     */
    private fun findMatchingJsonLibraryMetadata(paths: List<String>): LibraryMetadata? {
        if (paths.size > 1) {
            docsHolder.logger.warn(
                "Multiple sources exist for ${classlike.name}. Artifact ID metadata will not be " +
                    "displayed"
            )
            return null
        }
        val path = paths.single()

        return docsHolder.fileMetadataMap[path]
    }

    /**
     * Finds the source entries associated with the classlike. Returns null and logs a warning
     * if there are no source entries.
     */
    private fun getSourceEntries(classlike: DClasslike): Set<SourceEntry>? {
        val logger = docsHolder.logger
        val sources = classlike.sources
        if (sources.isEmpty()) {
            logger.warn("Sources for ${classlike.name} is empty")
            return null
        }

        return sources.entries
    }

    /**
     * Get the source file path from the [SourceEntry] relative to the root of the source directory.
     *
     * For example - this would return "androidx/paging/compose/LazyPagingItems.kt" if the path was
     * "/location/to/root/of/source/files/androidx/paging/compose/LazyPagingItems.kt".
     */
    private fun getSourceFilePath(sourceEntry: SourceEntry): String {
        val sourceRoots = sourceEntry.key.sourceRoots.map { it.toString() }
        val fullFilePath = sourceEntry.value.path
        // Find the source root that the file path starts with, so it can be trimmed off.
        // This assumes the full file path always begins with one of the source roots.
        val relevantSourceRoot = sourceRoots.first { fullFilePath.startsWith(it) }
        val filePath = fullFilePath.substringAfter(relevantSourceRoot)
        return filePath.removePrefix("/")
    }

    /**
     * Creates a link to the source of the classlike using the base URL from the configuration.
     *
     * Returns null if there was no base source link in the configuration.
     */
    private fun createLinkToSource(paths: List<String>): String? {
        // Reduce the list of paths to a single path by taking the common prefix of all of them.
        val path = paths.reduce { currPrefix, nextPath -> currPrefix.commonPrefixWith(nextPath) }
        return docsHolder.baseSourceLink?.format(path, classlike.dri.fullName)
    }

    /** Converts the classlikes to link components for use in the related symbols component. */
    private fun linksForClasslikes(docs: List<DClasslike>): List<Link> {
        return docs.map { pathProvider.linkForReference(it.dri) }
    }

    /**
     * Returns lists of functions and properties directly owned by this class-like.
     */
    private suspend fun DClasslike.nonInheritedTypes(): Pair<List<DFunction>, List<DProperty>> {
        val allFunctions = if (displayLanguage == Language.KOTLIN) this.functions
        else this.functions + this.properties.gettersAndSetters()

        val supertypes = this.supertypesForDisplayLanguage()

        return Pair(
            allFunctions.nonInheritedTypes(supertypes, this),
            this.properties.nonInheritedTypes(supertypes, this)
        )
    }

    /**
     * Returns the list of declared symbols. That is, symbols directly owned by the provided
     * class-like and not found through the inheritance hierarchy.
     *
     * Class and package comparison isn't applicable for synthetic classes.
     */
    private inline fun <reified T : Documentable> List<T>.nonInheritedTypes(
        supertypes: Set<DRI>,
        forClass: DClasslike,
    ): List<T> {
        if (forClass.isSynthetic) {
            return this
        }
        return filter { symbol -> !symbol.isInherited(supertypes) && !symbol.dri.isFromBaseClass() }
            .map { symbol -> symbol.withDRIOfClass(forClass) }
    }

    /**
     * If the DProperty or DFunction does not already have a DRI with the given class, makes a copy
     * of it with a new DRI. Defined over Documentables because both DProperty and Function have a
     * `copy` method defined because they are data classes, but there isn't a way to specify the
     * Documentable must be a data class.
     */
    private inline fun <reified T : Documentable> T.withDRIOfClass(forClass: DClasslike): T {
        return if (forClass.packageName() == this.dri.packageName &&
            forClass.name() == this.dri.classNames
        ) {
            this
        } else {
            val dri = this.dri.copy(
                packageName = forClass.packageName(), classNames = forClass.name()
            )
            when (this) {
                is DFunction -> this.copy(dri) as T
                is DProperty -> this.copy(dri) as T
                else -> throw RuntimeException()
            }
        }
    }

    /**
     * Gather superclasses and interfaces for this class, converting mapped types when applicable.
     */
    private suspend fun DClasslike.supertypesForDisplayLanguage(): Set<DRI> {
        val classNode = docsHolder.classGraph()[this.dri] ?: return emptySet()
        return (classNode.superClasses + classNode.interfaces)
            .map { it.dri.possiblyConvertMappedType(displayLanguage) }
            .toSet()
    }

    private suspend fun DClasslike.companionFunctionsAndProperties():
        Pair<List<DFunction>, List<DProperty>> {
        return (this as? DClass)?.companion?.nonInheritedTypes()
            ?: return Pair(emptyList(), emptyList())
    }

    /**
     * Returns the list of inherited symbols, not from Any or Object
     * If class is synthetic there should be no inherited methods
     */
    private fun <T : Documentable> List<T>.inheritedTypes(supertypes: Set<DRI>): List<T> {
        if (classlike.isSynthetic) {
            return emptyList()
        }
        return filter { symbol -> symbol.isInherited(supertypes) && !symbol.dri.isFromBaseClass() }
    }

    private fun <T : Documentable> T.isInherited(supertypes: Set<DRI>): Boolean {
        // Convert mapped type for the function to make sure the DRI lines up with supertypes.
        val classDRI = DRI(dri.packageName, dri.classNames)
            .possiblyConvertMappedType(displayLanguage)
        return supertypes.contains(classDRI)
    }

    private fun createDefaultConstructorFor(classlike: DClasslike) =
        DFunction(
            dri = classlike.dri.copy(callable = Callable(classlike.name!!, null, emptyList())),
            name = classlike.name!!,
            isConstructor = true,
            parameters = emptyList(),
            documentation = emptyMap(),
            expectPresentInSet = null,
            sources = emptyMap(),
            visibility = classlike.visibility,
            type = GenericTypeConstructor(dri = classlike.dri, projections = emptyList()),
            generics = emptyList(),
            receiver = null,
            modifier = classlike.visibility.keys.associateWith { KotlinModifier.Final },
            sourceSets = classlike.sourceSets,
            isExpectActual = false
        )

    private fun <I : Documentable, O> errorContextInjector(
        documentable: I,
        toDo: (I) -> O,
    ): O {
        try {
            return toDo(documentable)
        } catch (e: Exception) {
            var message = "Error when handling ${documentable::class} ${documentable.name} " +
                "in ${classlike.name}"
            if (documentable is WithSources) message += ", " + documentable.getErrorLocation()
            throw RuntimeException(message, e)
        }
    }
}

typealias SourceEntry = Map.Entry<DokkaSourceSet, DocumentableSource>

/**
 * Returns whether this DObject is an ordinary companion and is not signifcant enough to show on its
 * own. This requires that it be not named, not inherit anything, and not contain anything that is
 * not hoisted onto the containing object.
 *
 * This function can also be used on DObjects where it is unknown whether it is a companion at all.
 * This works because we enforce non-companion objects being named 'Companion' as an error.
 *
 * Returns true: is both a companion and uninteresting
 * Returns false: either is not a companion, or is interesting
 */
internal fun DObject.isOrdinaryCompanion(): Boolean =
    name == "Companion" &&
        supertypes.all { it.value.isEmpty() } &&
        children.none { it is DClasslike }

internal fun DClasslike.shouldNotBeDisplayed(displayLanguage: Language) =
    displayLanguage == Language.KOTLIN && this is DObject && this.isOrdinaryCompanion()

/**
 * Returns all [Documentable]s from the list which are not a companion object of [classlike] that
 * has only hoistable elements, such that direct use of the companion is never warranted.
 *
 * This is a best-effort temporary approximation. Even as-Java, companions can be fully hoistable.
 */
internal fun List<DClasslike>.withoutNeglectableCompanionOf(
    classlike: DClasslike,
    displayLanguage: Language
) = filterNot {
    classlike is DClass && it.dri == classlike.companion?.dri &&
        it.shouldNotBeDisplayed(displayLanguage)
}

// an `actual` cannot narrow visibility, but can widen it TODO(b/262710702)
private fun isPublic(element: Documentable) =
    "public" in element.modifiers(element.getExpectOrCommonSourceSet())
private fun isProtected(element: Documentable) =
    "protected" in element.modifiers(element.getExpectOrCommonSourceSet())
/** Filter out constants, because those have a separate display sections from properties. */
private fun isPublicNonConst(prop: DProperty) = isPublic(prop) && !prop.isConstant()
private fun isProtectedNonConst(prop: DProperty) = isProtected(prop) && !prop.isConstant()

/**
 * Returns true if function is a suspend function itself, or takes a suspend function as a
 * parameter.
 */
private fun DFunction.isSuspendFunction() =
    type.isSuspend() || parameters.any { it.type.isSuspend() }

private fun List<DProperty>.constants() = filter { it.isConstant() }.toSet().toList()

internal fun nestedTypesTitle() = "Nested types"

internal fun publicConstructorsTitle() = "Public ${constructorsTitle()}"
internal fun protectedConstructorsTitle() = "Protected ${constructorsTitle()}"
internal fun constructorsTitle(): String = "constructors"

internal fun publicMethodsTitle(displayLanguage: Language) =
    "Public ${methodsTitle(displayLanguage)}"
internal fun inheritedMethodsTitle(displayLanguage: Language) =
    "Inherited ${methodsTitle(displayLanguage)}"
internal fun protectedMethodsTitle(displayLanguage: Language) =
    "Protected ${methodsTitle(displayLanguage)}"
internal fun methodsTitle(displayLanguage: Language): String = when (displayLanguage) {
    Language.JAVA -> "methods"
    Language.KOTLIN -> "functions"
}

internal fun publicPropertiesTitle(displayLanguage: Language) =
    "Public ${propertiesTitle(displayLanguage)}"
internal fun inheritedPropertiesTitle(displayLanguage: Language) =
    "Inherited ${propertiesTitle(displayLanguage)}"
internal fun protectedPropertiesTitle(displayLanguage: Language) =
    "Protected ${propertiesTitle(displayLanguage)}"
internal fun propertiesTitle(displayLanguage: Language): String = when (displayLanguage) {
    Language.JAVA -> "fields"
    Language.KOTLIN -> "properties"
}

internal fun constantsTitle() = "Constants"
internal fun inheritedConstantsTitle() = "Inherited ${constantsTitle()}"
internal fun enumValuesTitle() = "Enum Values"
// Extension functions and companions are a Kotlin-only feature and only show up in as-Kotlin
internal fun extensionFunctionsTitle() = "Extension functions"
internal fun extensionPropertiesTitle() = "Extension properties"
internal fun companionFunctionsTitle(): String = "companion ${methodsTitle(Language.KOTLIN)}"
internal fun companionPropertiesTitle(): String = "companion ${propertiesTitle(Language.KOTLIN)}"
internal fun publicCompanionFunctionsTitle(): String = "Public ${companionFunctionsTitle()}"
internal fun protectedCompanionFunctionsTitle(): String = "Protected ${companionFunctionsTitle()}"
internal fun publicCompanionPropertiesTitle(): String = "Public ${companionPropertiesTitle()}"
internal fun protectedCompanionPropertiesTitle(): String = "Protected ${companionPropertiesTitle()}"
