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

import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.HtmlComponent
import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultClassHierarchy
import com.google.devsite.components.impl.DefaultClassSignature
import com.google.devsite.components.impl.DefaultClasslike
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultInheritedSymbols
import com.google.devsite.components.impl.DefaultLibraryMetadataComponent
import com.google.devsite.components.impl.DefaultRelatedSymbols
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableTitle
import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.symbols.ClassSignature
import com.google.devsite.components.symbols.LibraryMetadataComponent
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.ClassHierarchy
import com.google.devsite.components.table.InheritedSymbolsList
import com.google.devsite.components.table.RelatedSymbols
import com.google.devsite.components.table.SingleColumnSummaryItem
import com.google.devsite.components.table.SummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableTitle
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import com.google.devsite.util.LibraryMetadata
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.html.Tag
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.parent
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.InheritedMember
import org.jetbrains.dokka.model.KotlinModifier
import org.jetbrains.dokka.model.WithAbstraction
import org.jetbrains.dokka.model.WithConstructors
import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.WithSupertypes
import org.jetbrains.dokka.model.properties.WithExtraProperties

/** Converts documentable class-likes into the classlike component. */
internal class ClasslikeDocumentableConverter(
    private val displayLanguage: Language,
    private val classlike: DClasslike,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder,
    private val classExtensionFunctions: List<DFunction> = emptyList()
) {
    private val javadocConverter = DocTagConverter(displayLanguage, pathProvider, docsHolder)
    private val functionConverter =
        FunctionDocumentableConverter(displayLanguage, pathProvider, javadocConverter)
    private val propertyConverter =
        PropertyDocumentableConverter(displayLanguage, pathProvider, javadocConverter)
    private val paramConverter =
        ParameterDocumentableConverter(displayLanguage, pathProvider)
    private val enumConverter =
        EnumValueDocumentableConverter(displayLanguage, pathProvider, javadocConverter)

    /** @return the classlike component */
    suspend fun classlike(): DevsitePage = coroutineScope {
        var declaredFunctions = if (displayLanguage == Language.JAVA)
            classlike.functions.myTypes() + classlike.gettersAndSetters()
        else classlike.functions.myTypes()

        var declaredProperties = classlike.properties.myTypes()
        var companionFunctions = classlike.companionFunctions()
        var companionProperties = classlike.companionProperties()
        val inheritedAll = classlike.children.inheritedTypes()

        // Java documentation needs to respect @jvm* annotations
        if (displayLanguage == Language.JAVA) {
            declaredFunctions = declaredFunctions.filterOutJvmSynthetic().map { it.withJvmName() }
            declaredProperties = declaredProperties.filterOutJvmSynthetic()
        }

        declaredFunctions = declaredFunctions.sortedBy { it.name }
        declaredProperties = declaredProperties.sortedBy { it.name }
        companionFunctions = companionFunctions.sortedBy { it.name }
        companionProperties = companionProperties.sortedBy { it.name }

        val enumValues = (classlike as? DEnum)?.entries.orEmpty().sortedBy { it.name }

        var allConstructors = (classlike as? WithConstructors)?.constructors.orEmpty()
            .sortedBy { it.parameters.size }
        if (classlike is WithConstructors && allConstructors.isEmpty() && classlike.isFromJava() &&
            classlike !is DAnnotation
        ) {
            allConstructors = listOf(createDefaultConstructorFor(classlike))
        }
        val annotations = classlike.annotations()

        val enumValuesSummary = async {
            enumValuesToSummary(enumValuesTitle(), enumValues)
        }
        val nestedTypesSummary = async {
            typesToSummary(docsHolder.classlikesFor(classlike))
        }
        val constantsSummary = async {
            propertiesToSummary(constantsTitle(), declaredProperties.constants())
        }
        val publicPropertiesSummary = async {
            propertiesToSummary(
                publicPropertiesTitle(displayLanguage),
                declaredProperties.filter(::isPublic)
            )
        }
        val protectedPropertiesSummary = async {
            propertiesToSummary(
                protectedPropertiesTitle(displayLanguage),
                declaredProperties.filter(::isProtected)
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
            functionsToSummary(
                publicCompanionFunctionsTitle(),
                companionFunctions.filter(::isPublic)
            )
        }
        val protectedCompanionFunctionsSummary = async {
            functionsToSummary(
                protectedCompanionFunctionsTitle(),
                companionFunctions.filter(::isProtected)
            )
        }
        val publicCompanionPropertiesSummary = async {
            propertiesToSummary(
                publicCompanionPropertiesTitle(),
                companionProperties.filter(::isPublic)
            )
        }
        val protectedCompanionPropertiesSummary = async {
            propertiesToSummary(
                protectedCompanionPropertiesTitle(),
                companionProperties.filter(::isProtected)
            )
        }

        val enumDetails =
            async { enumValuesToDetail(classlike as? DEnum, enumValues) }
        val constants =
            async { propertiesToDetail(declaredProperties.constants()) }
        val publicProperties =
            async { propertiesToDetail(declaredProperties.filter(::isPublic)) }
        val protectedProperties =
            async { propertiesToDetail(declaredProperties.filter(::isProtected)) }
        val publicConstructors =
            async { constructorsToDetail(allConstructors.filter(::isPublic)) }
        val protectedConstructors =
            async { constructorsToDetail(allConstructors.filter(::isProtected)) }
        val publicFunctions =
            async { functionsToDetail(declaredFunctions.filter(::isPublic)) }
        val protectedFunctions =
            async { functionsToDetail(declaredFunctions.filter(::isProtected)) }
        val publicCompanionFunctionsDetail =
            async { functionsToDetail(companionFunctions.filter(::isPublic)) }
        val protectedCompanionFunctionsDetail =
            async { functionsToDetail(companionFunctions.filter(::isProtected)) }
        val publicCompanionPropertiesDetail =
            async { propertiesToDetail(companionProperties.filter(::isPublic)) }
        val protectedCompanionPropertiesDetail =
            async { propertiesToDetail(companionProperties.filter(::isProtected)) }

        val signature = async { computeSignature() }
        val hierarchy = async { computeHierarchy() }
        val relatedSymbols = async { findRelatedSymbols() }
        val inheritedTypes = async { computeInheritedSymbols(inheritedAll) }
        val libraryMetadataComponent = async {
            if (docsHolder.showLibraryMetadata) {
                getLibraryMetadata()
            } else {
                null
            }
        }

        val allSymbols: MutableList<Pair<SummaryList<out SummaryItem>,
                Classlike.TitledList<SymbolDetail>>> = mutableListOf(
            nestedTypesSummary.await() to Classlike.TitledList(nestedTypesTitle(), emptyList()),
            enumValuesSummary.await() to Classlike.TitledList(
                enumValuesTitle(),
                enumDetails.await()
            ),
            constantsSummary.await() to Classlike.TitledList(
                constantsTitle(),
                constants.await()
            )
        )
        if (displayLanguage == Language.KOTLIN) {
            allSymbols.addAll(
                listOf(
                    publicCompanionFunctionsSummary.await() to Classlike.TitledList(
                        publicCompanionFunctionsTitle(),
                        publicCompanionFunctionsDetail.await()
                    ),
                    protectedCompanionFunctionsSummary.await() to Classlike.TitledList(
                        protectedCompanionFunctionsTitle(),
                        protectedCompanionFunctionsDetail.await()
                    ),
                    publicCompanionPropertiesSummary.await() to Classlike.TitledList(
                        publicCompanionPropertiesTitle(),
                        publicCompanionPropertiesDetail.await()
                    ),
                    protectedCompanionPropertiesSummary.await() to Classlike.TitledList(
                        protectedCompanionPropertiesTitle(),
                        protectedCompanionPropertiesDetail.await()
                    )
                )
            )
        }

        // fields appear before constructors in Java docs
        if (displayLanguage == Language.JAVA) {
            allSymbols.addAll(
                listOf(
                    publicPropertiesSummary.await() to Classlike.TitledList(
                        publicPropertiesTitle(displayLanguage),
                        publicProperties.await()
                    ),
                    protectedPropertiesSummary.await() to Classlike.TitledList(
                        protectedPropertiesTitle(displayLanguage),
                        protectedProperties.await()
                    )
                )
            )
        }

        allSymbols.addAll(
            listOf(
                publicConstructorsSummary.await() to Classlike.TitledList(
                    publicConstructorsTitle(),
                    publicConstructors.await()
                ),
                protectedConstructorsSummary.await() to Classlike.TitledList(
                    protectedConstructorsTitle(),
                    protectedConstructors.await()
                ),
                publicFunctionsSummary.await() to Classlike.TitledList(
                    publicMethodsTitle(displayLanguage),
                    publicFunctions.await()
                ),
                protectedFunctionsSummary.await() to Classlike.TitledList(
                    protectedMethodsTitle(displayLanguage),
                    protectedFunctions.await()
                )
            )
        )

        // properties are the last page section in kotlin docs
        if (displayLanguage == Language.KOTLIN) {
            allSymbols.addAll(
                listOf(
                    publicPropertiesSummary.await() to Classlike.TitledList(
                        publicPropertiesTitle(displayLanguage),
                        publicProperties.await()
                    ),
                    protectedPropertiesSummary.await() to Classlike.TitledList(
                        protectedPropertiesTitle(displayLanguage),
                        protectedProperties.await()
                    )
                )
            )
        }

        if (classExtensionFunctions.isNotEmpty()) {
            var extensionFunctions = classExtensionFunctions
                // Sort by the class the extension function came from first, so they will be grouped
                // together in a logical way
                .sortedBy { nameForSyntheticClass(it) + it.name }
            if (displayLanguage == Language.JAVA) {
                extensionFunctions = extensionFunctions.filterNot {
                    it.isSuspendFunction()
                }
            }
            val extensionFunctionsSummary = async {
                functionsToSummary(
                    extensionFunctionsTitle(),
                    extensionFunctions
                )
            }
            val extensionFunctionsDetail = async { functionsToDetail(extensionFunctions) }

            allSymbols.add(
                extensionFunctionsSummary.await() to Classlike.TitledList(
                    extensionFunctionsTitle(),
                    extensionFunctionsDetail.await()
                )
            )
        }

        DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage,
                path = pathProvider.relative.forReference(classlike.dri).url,
                bookPath = pathProvider.book,
                title = classlike.name(),
                content = DefaultClasslike(
                    Classlike.Params(
                        signature = signature.await(),
                        hierarchy = hierarchy.await(),
                        relatedSymbols = relatedSymbols.await(),
                        description = javadocConverter.metadata(
                            classlike,
                            annotations = annotations
                        ),
                        symbolTypes = allSymbols,
                        inheritedTypes = inheritedTypes.await(),
                        libraryMetadataComponent = libraryMetadataComponent.await()
                    )
                )
            )
        )
    }

    private fun typesToSummary(classlikes: List<DClasslike>):
        SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>> {
        val components = when (displayLanguage) {
            // When displaying Kotlin pages, companion functions will be inlined and the link to the
            // companion object can be omitted.
            Language.KOTLIN -> classlikes.withoutCompanion()
            else -> classlikes
        }.map {
            errorContextInjector(it) {
                classlike ->
                javadocConverter.summaryForDocumentable(classlike)
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
        SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>> {
        val modifierHints = ModifierHints(displayLanguage, isSummary = true, isInterface())
        val components = functions.map {
            errorContextInjector(it) {
                functionConverter.summary(it, modifierHints)
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
        SummaryList<SingleColumnSummaryItem<SymbolSummary>> {
        val components = constructors.map {
            errorContextInjector(it) {
                functionConverter.summaryForConstructor(it)
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

    private fun functionsToDetail(functions: List<DFunction>): List<SymbolDetail> {
        val modifierHints = ModifierHints(displayLanguage, isSummary = false, isInterface())
        return functions.map {
            errorContextInjector(it) {
                functionConverter.detail(it, modifierHints)
            }
        }
    }

    private fun constructorsToDetail(functions: List<DFunction>): List<SymbolDetail> {
        val modifierHints = ModifierHints(displayLanguage, isSummary = false, isInterface())
        return functions.map {
            errorContextInjector(it) {
                functionConverter.detailForConstructor(it, modifierHints)
            }
        }
    }

    private fun enumValuesToSummary(title: String, enumVals: List<DEnumEntry>):
        SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>> {
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

    private fun propertiesToSummary(
        name: String? = null,
        properties: List<DProperty>
    ): SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>> {
        val modifierHints = ModifierHints(displayLanguage, isSummary = true, isInterface())
        val components = properties.map {
            errorContextInjector(it) {
                propertyConverter.summary(it, modifierHints)
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

    private fun propertiesToDetail(properties: List<DProperty>): List<SymbolDetail> {
        val modifierHints = ModifierHints(displayLanguage, isSummary = false, isInterface())
        return properties.map {
            errorContextInjector(it) {
                propertyConverter.detail(it, modifierHints)
            }
        }
    }

    private fun enumValuesToDetail(
        dEnum: DEnum?,
        enumValues: List<DEnumEntry>
    ): List<SymbolDetail> {
        if (dEnum == null) {
            return emptyList()
        }
        val modifierHints = ModifierHints(displayLanguage, isSummary = false, isInterface())
        return enumValues.map {
            errorContextInjector(it) {
                enumConverter.detail(dEnum, it, modifierHints)
            }
        }
    }

    private suspend fun computeSignature(): ClassSignature {
        val modifiers = if (classlike is WithAbstraction && classlike is WithExtraProperties<*>) {
            classlike.modifiers().modifiersFor(ModifierHints(displayLanguage = displayLanguage))
        } else {
            emptyList()
        }
        val typeParameters = if (classlike is WithGenerics) {
            classlike.generics.map {
                errorContextInjector(it) {
                    paramConverter.componentForTypeParameter(it, classlike.isFromJava())
                }
            }
        } else {
            emptyList()
        }
        val annotations = classlike.annotations().annotationComponents(
            pathProvider = pathProvider,
            displayLanguage = displayLanguage,
            nullability = Nullability.DONT_CARE // Classlike definitions aren't nullable
        )

        if (classlike !is WithSupertypes) {
            return DefaultClassSignature(
                ClassSignature.Params(
                    displayLanguage = displayLanguage,
                    name = classlike.name(),
                    type = classlike.stringForType(displayLanguage),
                    modifiers = modifiers,
                    implements = emptyList(),
                    extends = emptyList(),
                    typeParameters = typeParameters,
                    annotationComponents = annotations
                )
            )
        }
        val extends = docsHolder.classGraph().getValue(classlike.dri).directSuperClasses.map {
            pathProvider.linkForReference(it.dri)
        }

        val implements = docsHolder.classGraph().getValue(classlike.dri).directInterfaces.map {
            pathProvider.linkForReference(it.dri)
        }

        return DefaultClassSignature(
            ClassSignature.Params(
                displayLanguage = displayLanguage,
                name = classlike.name(),
                type = classlike.stringForType(displayLanguage),
                modifiers = modifiers,
                implements = implements,
                extends = extends,
                typeParameters = typeParameters,
                annotationComponents = annotations
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
    ): List<InheritedSymbolsList> {
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
            .sortedBy { it.name }
            .partition { isConstant(it.modifiers()) }

        val constsSummary = consts.takeIf { it.isNotEmpty() }
            ?.createInheritedCategory(title = inheritedConstantsTitle()) {
                propertiesToSummary(properties = it)
            }

        val propertiesSummary = properties.takeIf { it.isNotEmpty() }
            ?.createInheritedCategory(title = inheritedPropertiesTitle(displayLanguage)) {
                propertiesToSummary(properties = it)
            }

        return listOfNotNull(functionsSummary, constsSummary, propertiesSummary)
    }

    private fun <T> List<T>.createInheritedCategory(
        title: String,
        summaryGen: (List<T>) -> SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>>
    ): InheritedSymbolsList where T : Documentable, T : WithExtraProperties<T> {
        fun createInheritedSymbolsList(parent: DRI, symbolList: List<T>):
            Pair<Link, SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>>> {
            val link = pathProvider.linkForReference(parent)
            val summary = summaryGen(symbolList)
            return link to summary
        }

        // val category = groupBy { it.driInheritedFrom() ?: it.dri.parent }
        val category = groupBy { it.dri.parent }
            .toSortedMap(compareBy { it.classNames })
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
     */
    private fun <T> T.driInheritedFrom(): DRI?
        where T : Documentable, T : WithExtraProperties<T> =
        extra[InheritedMember]?.inheritedFrom?.values?.toSet()?.singleOrNull()

    /** Finds the direct and indirect subclasses for this classlike, returning their component. */
    // We know our subclasses will always be DClasslikes
    @Suppress("UNCHECKED_CAST")
    private suspend fun findRelatedSymbols(): RelatedSymbols {
        val classNode = docsHolder.classGraph().getValue(classlike.dri)
        val directSubclasses = classNode.directSubClasses
        val indirectSubclasses = classNode.indirectSubClasses

        return DefaultRelatedSymbols(
            RelatedSymbols.Params(
                directSubclasses = linksForClasslikes(directSubclasses),
                directSummary = javadocConverter.docsToSummary(directSubclasses),
                indirectSubclasses = linksForClasslikes(indirectSubclasses),
                indirectSummary = javadocConverter.docsToSummary(indirectSubclasses)
            )
        )
    }

    private fun getLibraryMetadata(): LibraryMetadataComponent? {
        val path = getSourceFilePathWithoutFilename(classlike) ?: return null
        val jsonLibraryMetadata = findMatchingJsonLibraryMetadata(path)

        return if (jsonLibraryMetadata == null) {
            null
        } else {
            DefaultLibraryMetadataComponent(jsonLibraryMetadata)
        }
    }

    /**
     * Iterate through the library metadata list to find a [LibraryMetadata] that matches the
     * path for the current library being processed.  Otherwise, return null.
     */
    private fun findMatchingJsonLibraryMetadata(path: String): LibraryMetadata? {
        return docsHolder.libraryMetadata.firstOrNull {
            path.endsWith(it.sourceDir)
        }
    }

    /**
     * Get the source file path for a [DClasslike] without the filename.
     *
     * For example - this would return "androidx/paging/compose" if the path was
     * "androidx/paging/compose/LazyPagingItems.kt".
     *
     * Returns null if there is an error finding the path.
     */
    private fun getSourceFilePathWithoutFilename(classlike: DClasslike): String? {
        val logger = docsHolder.logger
        val sources = classlike.sources
        if (sources.isEmpty()) {
            logger.warn("Sources for ${classlike.name} is empty")
            return null
        }

        if (sources.size > 1) {
            logger.warn(
                "Multiple sources for ${classlike.name} detected. Source size is ${sources.size}"
            )
            return null
        }

        return sources.entries.first().value.path.substringBeforeLast('/')
    }

    /** Converts the classlikes to link components for use in the related symbols component. */
    private fun linksForClasslikes(docs: List<DClasslike>): List<Link> {
        return docs.map { pathProvider.linkForReference(it.dri) }
    }

    /**
     * Returns the list of declared symbols. That is, symbols directly owned by this class-like
     * and not found through the inheritance hierarchy.
     *
     * Class and package comparison isn't applicable for synthetic classes
     */
    private fun <T : Documentable> List<T>.myTypes(): List<T> {
        if (classlike.isSynthetic) {
            return this
        }
        return filter { symbol ->
            classlike.packageName() == symbol.dri.packageName && hasMatchingClassName(symbol)
        }
    }

    private fun hasMatchingClassName(symbol: Documentable) = (
        classlike.name() == symbol.dri.classNames ||
            (classlike as? DClass)?.companion?.name() == symbol.dri.classNames
        )

    private fun DClasslike.companionFunctions(): List<DFunction> =
        (this as? DClass)?.companion?.functions?.myTypes() ?: emptyList()

    private fun DClasslike.companionProperties(): List<DProperty> =
        (this as? DClass)?.companion?.properties?.myTypes() ?: emptyList()

    /**
     * Returns the list of inherited symbols, not from Any or Object
     * If class is synthetic there should be no inherited methods
     */
    private fun <T : Documentable> List<T>.inheritedTypes(): List<T> {
        if (classlike.isSynthetic) {
            return emptyList()
        }
        return filterNot { symbol ->
            (
                classlike.packageName() == symbol.dri.packageName &&
                    classlike.name() == symbol.dri.classNames
                ) ||
                symbol.dri.isFromBaseClass()
        }
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
            modifier = mapOf(classlike.visibility.keys.single() to KotlinModifier.Final),
            sourceSets = setOf(classlike.visibility.keys.single()),
            isExpectActual = false
        )

    private fun <I : Documentable, T : Tag, O : HtmlComponent<T>> errorContextInjector(
        documentable: I,
        toDo: (I) -> O,
    ): O {
        try {
            return toDo(documentable)
        } catch (e: Exception) {
            val message = "Error when handling ${documentable::class} ${documentable.name} " +
                "in ${classlike.name}"
            throw RuntimeException(message, e)
        }
    }

    /**
     * Returns all [Documentable]s from the list which are not the companion object of [classlike]
     */
    private fun List<Documentable>.withoutCompanion(): List<Documentable> =
        filterNot { it.dri == (classlike as? DClass)?.companion?.dri }

    private fun isInterface() = classlike is DInterface

    private fun isPublic(function: DFunction) = "public" in function.modifiers()
    private fun isProtected(function: DFunction) = "protected" in function.modifiers()

    /** Filters for public, non-constant properties. */
    private fun isPublic(property: DProperty): Boolean {
        val modifiers = property.modifiers()
        return "public" in modifiers && !isConstant(modifiers)
    }

    /** Filters for protected, non-constant properties. */
    private fun isProtected(property: DProperty): Boolean {
        val modifiers = property.modifiers()
        return "protected" in modifiers && !isConstant(modifiers)
    }

    /**
     * Returns true if function is a suspend function itself, or takes a suspend function as a
     * parameter.
     */
    private fun DFunction.isSuspendFunction() =
        type.isSuspend() || parameters.any { it.type.isSuspend() }

    private fun List<DProperty>.constants() = filter { isConstant(it.modifiers()) }
}

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
internal fun companionFunctionsTitle(): String =
    "companion ${methodsTitle(Language.KOTLIN)}"
internal fun companionPropertiesTitle(): String =
    "companion ${propertiesTitle(Language.KOTLIN)}"
internal fun publicCompanionFunctionsTitle(): String =
    "Public ${companionFunctionsTitle()}"
internal fun protectedCompanionFunctionsTitle(): String =
    "Protected ${companionFunctionsTitle()}"
internal fun publicCompanionPropertiesTitle(): String =
    "Public ${companionPropertiesTitle()}"
internal fun protectedCompanionPropertiesTitle(): String =
    "Protected ${companionPropertiesTitle()}"
