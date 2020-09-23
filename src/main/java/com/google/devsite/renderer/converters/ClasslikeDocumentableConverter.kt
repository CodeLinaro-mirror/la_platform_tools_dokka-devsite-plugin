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

import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultClasslike
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultRelatedSymbols
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableTitle
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.table.RelatedSymbols
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableTitle
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithConstructors
import org.jetbrains.dokka.model.WithSupertypes
import org.jetbrains.dokka.model.properties.WithExtraProperties

/** Converts documentable class-likes into the classlike component. */
internal class ClasslikeDocumentableConverter(
    private val displayLanguage: Language,
    private val classlike: DClasslike,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder
) {
    private val javadocConverter = DocTagConverter(displayLanguage, pathProvider)
    private val functionConverter =
        FunctionDocumentableConverter(displayLanguage, pathProvider, javadocConverter)
    private val propertyConverter =
        PropertyDocumentableConverter(displayLanguage, pathProvider, javadocConverter)

    /** @return the classlike component */
    suspend fun classlike(): DevsitePage = coroutineScope {
        val declaredFunctions = classlike.functions.myTypes().sortedBy { it.name }
        val declaredProperties = classlike.properties.myTypes().sortedBy { it.name }
        val declaredConstructors = (classlike as? WithConstructors)?.constructors.orEmpty()
            .sortedBy { it.parameters.size }
        val annotations = (classlike as? WithExtraProperties<*>)?.annotations().orEmpty()

        val nestedTypesSummary = async {
            typesToSummary(docsHolder.classlikesFor(classlike))
        }
        val constantsSummary = async {
            propertiesToSummary(constantsTitle(), declaredProperties.constants())
        }
        val publicPropertiesSummary = async {
            propertiesToSummary(publicPropertiesTitle(), declaredProperties.filter(::isPublic))
        }
        val protectedPropertiesSummary = async {
            propertiesToSummary(
                protectedPropertiesTitle(),
                declaredProperties.filter(::isProtected)
            )
        }
        val publicConstructorsSummary = async {
            constructorsToSummary(
                publicConstructorsTitle(),
                declaredConstructors.filter(::isPublic)
            )
        }
        val protectedConstructorsSummary = async {
            constructorsToSummary(
                protectedConstructorsTitle(),
                declaredConstructors.filter(::isProtected)
            )
        }
        val publicFunctionsSummary = async {
            functionsToSummary(publicMethodsTitle(), declaredFunctions.filter(::isPublic))
        }
        val protectedFunctionsSummary = async {
            functionsToSummary(protectedMethodsTitle(), declaredFunctions.filter(::isProtected))
        }

        val constants =
            async { propertiesToDetail(declaredProperties.constants()) }
        val publicProperties =
            async { propertiesToDetail(declaredProperties.filter(::isPublic)) }
        val protectedProperties =
            async { propertiesToDetail(declaredProperties.filter(::isProtected)) }
        val publicConstructors =
            async { functionsToDetail(declaredConstructors.filter(::isPublic)) }
        val protectedConstructors =
            async { functionsToDetail(declaredConstructors.filter(::isProtected)) }
        val publicFunctions =
            async { functionsToDetail(declaredFunctions.filter(::isPublic)) }
        val protectedFunctions =
            async { functionsToDetail(declaredFunctions.filter(::isProtected)) }

        val relatedSymbols = async { findRelatedSymbols() }

        val allSymbols = listOf(
            nestedTypesSummary.await() to Classlike.SymbolType(nestedTypesTitle(), emptyList()),
            constantsSummary.await() to Classlike.SymbolType(
                constantsTitle(),
                constants.await()
            ),
            publicPropertiesSummary.await() to Classlike.SymbolType(
                publicPropertiesTitle(),
                publicProperties.await()
            ),
            protectedPropertiesSummary.await() to Classlike.SymbolType(
                protectedPropertiesTitle(),
                protectedProperties.await()
            ),
            publicConstructorsSummary.await() to Classlike.SymbolType(
                publicConstructorsTitle(),
                publicConstructors.await()
            ),
            protectedConstructorsSummary.await() to Classlike.SymbolType(
                protectedConstructorsTitle(),
                protectedConstructors.await()
            ),
            publicFunctionsSummary.await() to Classlike.SymbolType(
                publicMethodsTitle(),
                publicFunctions.await()
            ),
            protectedFunctionsSummary.await() to Classlike.SymbolType(
                protectedMethodsTitle(),
                protectedFunctions.await()
            )
        )

        DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage,
                path = pathProvider.relative.forReference(classlike.dri).url,
                bookPath = pathProvider.book,
                title = classlike.name(),
                content = DefaultClasslike(
                    Classlike.Params(
                        relatedSymbols = relatedSymbols.await(),
                        description = javadocConverter.metadata(
                            classlike,
                            annotations = annotations
                        ),
                        symbolTypes = allSymbols
                    )
                )
            )
        )
    }

    private fun typesToSummary(classlikes: List<Documentable>): SummaryList {
        val components = classlikes.map { classlike ->
            DefaultTwoPaneSummaryItem(
                TwoPaneSummaryItem.Params(
                    title = pathProvider.linkForReference(classlike.dri),
                    description = javadocConverter.summaryDescription(classlike)
                )
            )
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

    private fun functionsToSummary(name: String, functions: List<DFunction>): SummaryList {
        val modifierHints = ModifierHints(displayLanguage, isSummary = true, isInterface())
        val components = functions.map {
            functionConverter.summary(it, modifierHints)
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

    private fun constructorsToSummary(name: String, constructors: List<DFunction>): SummaryList {
        val components = constructors.map(functionConverter::summaryForConstructor)

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
            functionConverter.detail(it, modifierHints)
        }
    }

    private fun propertiesToSummary(name: String, properties: List<DProperty>): SummaryList {
        val modifierHints = ModifierHints(displayLanguage, isSummary = true, isInterface())
        val components = properties.map {
            propertyConverter.summary(it, modifierHints)
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

    private fun propertiesToDetail(properties: List<DProperty>): List<SymbolDetail> {
        val modifierHints = ModifierHints(displayLanguage, isSummary = false, isInterface())
        return properties.map {
            propertyConverter.detail(it, modifierHints)
        }
    }

    /**
     * Returns the list of declared symbols. That is, symbols directly owned by this class-like
     * and not found through the inheritance hierarchy.
     */
    private fun <T : Documentable> List<T>.myTypes() = filter { function ->
        classlike.packageName() == function.dri.packageName &&
            classlike.name() == function.dri.classNames
    }

    /** Finds the direct and indirect subclasses for this classlike, returning their component. */
    // We know our subclasses will always be DClasslikes
    @Suppress("UNCHECKED_CAST")
    private suspend fun findRelatedSymbols(): RelatedSymbols {
        val withSuperTypes = docsHolder.allClasslikes().filterIsInstance<WithSupertypes>()
        val directSubclasses = withSuperTypes.filter { child ->
            child.supertypes.values.single().any { (type, _) ->
                type.dri == classlike.dri
            }
        } as List<DClasslike>

        val classlikes = docsHolder.allClasslikes().associateBy { it.dri }
        val allSubclasses = classlikes.values
            .filterIsInstance<WithSupertypes>()
            .filter { isIndirectSubclass(it, classlikes) } as List<DClasslike>
        val indirectSubclasses = allSubclasses - directSubclasses

        return DefaultRelatedSymbols(
            RelatedSymbols.Params(
                directSubclasses = linksForClasslikes(directSubclasses),
                directSummary = summaryForClasslikes(directSubclasses),
                indirectSubclasses = linksForClasslikes(indirectSubclasses),
                indirectSummary = summaryForClasslikes(indirectSubclasses)
            )
        )
    }

    /**
     * Determines if this is an indirect subclass by traversing the type hierarchy using
     * [classlikes].
     */
    private fun isIndirectSubclass(
        child: WithSupertypes,
        classlikes: Map<DRI, DClasslike>
    ): Boolean {
        val supertypes = child.supertypes.values.single()
        return supertypes.any { (type, _) ->
            if (type.dri == classlike.dri) return true

            val parent = classlikes[type.dri]
            if (parent is WithSupertypes) {
                isIndirectSubclass(parent, classlikes)
            } else {
                false
            }
        }
    }

    /** Converts the classlikes to link components for use in the related symbols component. */
    private fun linksForClasslikes(docs: List<DClasslike>): List<Link> {
        return docs.map { pathProvider.linkForReference(it.dri) }
    }

    /** Converts the classlikes to a summary component for use in the related symbols component. */
    private fun summaryForClasslikes(docs: List<DClasslike>): SummaryList {
        return DefaultSummaryList(SummaryList.Params(
            items = docs.map { classlike ->
                val annotations =
                    (classlike as? WithExtraProperties<*>)?.annotations().orEmpty()

                DefaultTwoPaneSummaryItem(
                    TwoPaneSummaryItem.Params(
                        title = pathProvider.linkForReference(classlike.dri),
                        description = javadocConverter.summaryDescription(
                            classlike,
                            annotations
                        )
                    )
                )
            }
        ))
    }

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

    private fun List<DProperty>.constants() = filter { isConstant(it.modifiers()) }

    private fun nestedTypesTitle() = "Nested types"

    private fun publicConstructorsTitle() = "Public ${constructorsTitle()}"
    private fun protectedConstructorsTitle() = "Protected ${constructorsTitle()}"
    private fun constructorsTitle(): String = "constructors"

    private fun publicMethodsTitle() = "Public ${methodsTitle()}"
    private fun protectedMethodsTitle() = "Protected ${methodsTitle()}"
    private fun methodsTitle(): String = when (displayLanguage) {
        Language.JAVA -> "methods"
        Language.KOTLIN -> "functions"
    }

    private fun publicPropertiesTitle() = "Public ${propertiesTitle()}"
    private fun protectedPropertiesTitle() = "Protected ${propertiesTitle()}"
    private fun propertiesTitle(): String = when (displayLanguage) {
        Language.JAVA -> "fields"
        Language.KOTLIN -> "properties"
    }

    private fun constantsTitle() = "Constants"
}
