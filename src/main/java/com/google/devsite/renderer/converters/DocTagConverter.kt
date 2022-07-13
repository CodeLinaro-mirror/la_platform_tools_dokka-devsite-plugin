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

import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultAnnotatedLink
import com.google.devsite.components.impl.DefaultDescriptionComponent
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.impl.DefaultParameterComponent
import com.google.devsite.components.impl.DefaultPropertySignature
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableTitle
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.components.impl.DefaultTypeProjectionComponent
import com.google.devsite.components.impl.UndocumentedSymbolDescriptionComponent
import com.google.devsite.components.symbols.AnnotatedLink
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableTitle
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Callable
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeParameter
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Dynamic
import org.jetbrains.dokka.model.JavaObject
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.PrimitiveJavaType
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.Star
import org.jetbrains.dokka.model.StringValue
import org.jetbrains.dokka.model.TypeAliased
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.TypeParameter
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Variance
import org.jetbrains.dokka.model.Void
import org.jetbrains.dokka.model.WithChildren
import org.jetbrains.dokka.model.WithConstructors
import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.doc.Author
import org.jetbrains.dokka.model.doc.CodeBlock
import org.jetbrains.dokka.model.doc.Constructor
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Deprecated
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.NamedTagWrapper
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Property
import org.jetbrains.dokka.model.doc.Receiver
import org.jetbrains.dokka.model.doc.Return
import org.jetbrains.dokka.model.doc.Sample
import org.jetbrains.dokka.model.doc.See
import org.jetbrains.dokka.model.doc.Since
import org.jetbrains.dokka.model.doc.Suppress
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.doc.Throws
import org.jetbrains.dokka.model.doc.Version
import org.jetbrains.dokka.utilities.cast
import java.io.File

/** Extracts the handwritten documentation from documentables into the correct components. */
internal class DocTagConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder
) {
    private val analysisMap = runBlocking { docsHolder.analysisMap() }
    private val paramConverter = ParameterDocumentableConverter(displayLanguage, pathProvider)

    /** @return the hand-written javadoc */
    fun summaryDescription(
        documentable: Documentable,
        annotations: List<Annotations.Annotation> = emptyList()
    ): DescriptionComponent {
        return deprecationComponent(documentable, summary = true, annotations)
            ?: documentable.getDescription(summary = true)
    }

    /** metadata() can either take a WithSources Documentable, OR an isFromJava boolean */
    fun <T> metadata(
        documentable: T,
        returnType: TypeProjectionComponent? = null,
        paramNames: List<String> = emptyList(),
        annotations: List<Annotations.Annotation> = emptyList()
    ) where T : Documentable, T : WithSources =
        metadataImpl(documentable, returnType, paramNames, annotations)

    fun metadata(
        documentable: Documentable,
        returnType: TypeProjectionComponent? = null,
        paramNames: List<String> = emptyList(),
        annotations: List<Annotations.Annotation> = emptyList(),
        isFromJava: Boolean
    ) = metadataImpl(documentable, returnType, paramNames, annotations, isFromJava)

    /**
     * Returns a breakdown of the different metadata as a deprecation warning, description, and then
     * separate summaries. Examples include the list of parameters, return type, see also, throws,
     * etc.
     */
    private fun metadataImpl(
        documentable: Documentable,
        returnType: TypeProjectionComponent? = null,
        paramNames: List<String> = emptyList(),
        annotations: List<Annotations.Annotation> = emptyList(),
        isFromJavaParam: Boolean? = null
    ): List<ContextFreeComponent> {
        val isFromJava = if (isFromJavaParam != null) isFromJavaParam
        else {
            assert(documentable is WithSources)
            (documentable as WithSources).isFromJava()
        }
        val description = documentable.getDescription(summary = false)
        val deprecation = deprecationComponent(documentable, summary = false, annotations)
        val receiverParam = documentable.find<Receiver>()?.let {
            Param(it.root, "receiver")
        }
        val generics = if (documentable is WithGenerics) documentable.generics else emptyList()
        // Tags referring to something with the same name as the Documentable itself should instead
        // be put into the Description, which is handled in the getDescription method.
        val preparedTags = (listOfNotNull(receiverParam) + documentable.tags())
            .filter { (it as? NamedTagWrapper)?.name != documentable.name }
        val tagsByType = preparedTags.sortedWith(tagOrder(paramNames)).groupBy { it.javaClass }
        val tables = tagsByType.mapNotNull { (_, rawTags) ->
            // b/172000585
            var tags = handleUpstreamTagDuplication(documentable, rawTags, generics)
            if (tags.isEmpty()) return@mapNotNull null
            val firstTag = tags.first()
            if (documentable is DFunction && firstTag is Param) {
                val propertyClass = Property::class.java as Class<*>
                tags = tags + tagsByType[propertyClass].orEmpty()
            }
            // We know all the elements in `tags` will be of the same type, so we pick an arbitrary
            // one to do the switching and then cast the list to its type.
            @kotlin.Suppress("UNCHECKED_CAST")
            try {
                when (firstTag) {
                    is Param ->
                        params(tags as List<NamedTagWrapper>, generics, documentable, isFromJava)
                    is Return -> returnType(tags as List<Return>, checkNotNull(returnType))
                    is Throws -> throws(tags as List<Throws>)
                    is See -> see(tags as List<See>)
                    is Sample -> null // Samples are handled in the description
                    is Property ->
                        throw RuntimeException("Should have been consumed in description!")
                    is CustomTagWrapper -> null // TODO("b/163811276: custom tag wrapper")
                    is Since -> TODO("b/163811276: since")
                    is Constructor -> null // TODO("b/180525239: constructor")
                    // Documented separately above
                    is Description, is Deprecated, is Receiver -> null
                    // Don't care ;)
                    is Suppress, is Version, is Author -> null
                }
            } catch (e: Exception) {
                println(
                    "Exception thrown while handling ${firstTag::class.java} tags! " +
                        "Tags: $tags. Parent: ${documentable.name}"
                )
                throw e
            }
        }

        return listOfNotNull(deprecation, description, *tables.toTypedArray())
    }

    // Turn both "E" and "<E>" to "E"
    private fun ungenerify(name: String): String {
        if (name.startsWith('<') && name.endsWith('>')) return name.drop(1).dropLast(1)
        return name
    }
    private fun TagWrapper.name() = ungenerify((this as NamedTagWrapper).name)
    private fun List<TagWrapper>.names() = this.map { it.name() }

    /* Tags, in particular for property parameters, are propagated multiple times in upstream.
     * For example, @param t t_doc class Foo(val t) has Parameter(t, t_doc) duplicated many times.
     * On the class itself, on each property parameter of that class, and on the constructor.
     * This function filters out inappropriately propagated docs, and enforces that documentation
     * applies only to existing properties and parameters.
     */
    private fun handleUpstreamTagDuplication(
        documentable: Documentable,
        tags: List<TagWrapper>,
        generics: List<DTypeParameter>
    ): List<TagWrapper> {
        if (tags.first() is Param) {

            // This one is very strange, and I haven't been able to reproduce it in unit tests
            // A generic called "ToValue" is instead registered as being named "V"
            // TODO: Fix
            if (tags.names() == listOf("ToValue", "function") && generics.single().name == "V") {
                return tags.filter { (it as Param).name == "function" }
            }

            // Handle parameter properties, e.g. class AClass<Gen>(val propParam)
            when (documentable) {
                // doc is DClasslike. DClasslike's only valid @params are type params
                is DClasslike -> {
                    val genericNames = generics.map { it.name }
                    val invalidNames = tags.names().filter { it !in genericNames }.toMutableSet()
                    if (invalidNames.isEmpty()) return tags
                    // Enforce that the propagated documentation makes sense somewhere. Specifically
                    // documentation primarily aimed at a constructor may wind up on the DClass
                    // if the parameter being documented is a primary constructor property parameter
                    invalidNames.removeAll(
                        documentable.properties.map { it.name } +
                            (documentable as? WithConstructors)?.constructors?.map { constructor ->
                                constructor.parameters.map { it.name!! }
                            }?.flatten().orEmpty()
                    )
                    logComponentNotFoundWarning("@param", invalidNames, documentable)
                    // Use only docs for type parameters in the parameter documentation table
                    return tags.filter { it.name() in genericNames }
                }
                // doc is Property. It is possible that a parameter property is documented on the
                // class as @param. That doc is used as though it were @property. Handled there.
                // Properties can also have @param documentation for type parameters
                is DProperty -> {
                    val genericNames = generics.map { it.name }
                    val invalidNames = tags.names().filter {
                        it !in genericNames && it != documentable.name
                    }
                    logComponentNotFoundWarning("@param", invalidNames, documentable)
                    return tags
                }
                is DFunction -> {}
                else ->
                    throw RuntimeException("Can't apply @param to a ${documentable::class.java}")
            }
        } else if (tags.first() is Property) {
            // A DClasslike with @property applying to property parameters may have Parameter tags
            // In such a case, none of these tags should become docs *on the DClasslike itself*
            return when (documentable) {
                is DClasslike -> {
                    logComponentNotFoundWarning(
                        "@property",
                        tags.names().toSet().subtract(documentable.properties.map { it.name }),
                        documentable
                    )
                    emptyList()
                }
                is DParameter, is DProperty -> tags
                else ->
                    throw RuntimeException("Can't apply @property to ${documentable::class.java}")
            }
        }
        return tags
    }

    private fun logComponentNotFoundWarning(
        componentType: String,
        components: Iterable<String>,
        containingComponent: Documentable
    ) {
        if (components.none()) return
        val warning = "Unable to find what is referred to by" +
            components.joinToString { "\n\t$componentType $it" } +
            "\nin ${containingComponent::class.simpleName} ${containingComponent.name}" +
            "\nDid you make a typo? Are you trying to refer to something not visible to users?"
        docsHolder.logger.warn(warning)
    }

    /** params() can take either a WithSources Documentable, or an isFromJava boolean */
    private fun <T> params(
        tags: List<NamedTagWrapper>,
        dGenerics: List<DTypeParameter>,
        documentable: T,
    ) where T : Documentable, T : WithSources =
        paramsImpl(tags, dGenerics, documentable, documentable.isFromJava())

    private fun params(
        tags: List<NamedTagWrapper>,
        dGenerics: List<DTypeParameter>,
        documentable: Documentable,
        isFromJava: Boolean
    ) = paramsImpl(tags, dGenerics, documentable, isFromJava)

    private fun paramsImpl(
        tags: List<NamedTagWrapper>,
        dGenerics: List<DTypeParameter>,
        documentable: Documentable,
        isFromJavaParam: Boolean? = null
    ): SummaryList<TwoPaneSummaryItem<ParameterComponent, DescriptionComponent>> {
        val isFromJava = if (isFromJavaParam != null) isFromJavaParam
        else {
            assert(documentable is WithSources)
            (documentable as WithSources).isFromJava()
        }
        // @param can refer to parameters, lambda parameters, type parameters, or receivers.
        val allOptions = mutableMapOf<String, ParameterComponent>()
        if (documentable is DFunction) {
            allOptions.putAll(
                documentable.parameters.map {
                    it.name!! to paramConverter.componentForParameter(
                        param = it,
                        isSummary = false,
                        isFromJava = isFromJava
                    )
                }
            )
            allOptions.putAll(
                recursivelyGetLambdaParamNames(documentable.parameters.map { it.type }).map {
                    (it.presentableName ?: "") to paramConverter
                        .componentForLambdaParameter(it, isFromJava)
                }
            )
        }
        allOptions.putAll(
            dGenerics.map {
                it.name to paramConverter.componentForTypeParameter(it, isFromJava)
            }
        )
        if (documentable is Callable && documentable.receiver != null)
            allOptions[documentable.receiver!!.name ?: "receiver"] =
                paramConverter.componentForParameter(documentable.receiver!!, false, isFromJava)
        val params = tags.map { tag ->
            if (allOptions[tag.name()] == null) {
                throw RuntimeException(
                    "Unable to find what is referred to by \"@param " +
                        "${tag.name()}\" in ${documentable::class.simpleName} " +
                        "${documentable.name}, with contents: ${tag.text()}"
                )
            }
            val title = allOptions[tag.name()]!!
            DefaultTwoPaneSummaryItem(
                TwoPaneSummaryItem.Params(
                    title = title,
                    description = description(tag)
                )
            )
        }

        return DefaultSummaryList<TwoPaneSummaryItem<ParameterComponent, DescriptionComponent>>(
            SummaryList.Params<TwoPaneSummaryItem<ParameterComponent, DescriptionComponent>>(
                header = DefaultTableTitle(TableTitle.Params("Parameters")),
                items = params
            )
        )
    }

    /** For example, "a" and "b" in `fun foo((a: (b: String) -> int)) -> Unit)` */
    private fun recursivelyGetLambdaParamNames(
        argumentTypes: List<Projection>
    ): List<TypeConstructor> {
        val result = mutableListOf<TypeConstructor>()
        for (argumentType in argumentTypes) {
            when (argumentType) {
                is TypeConstructor -> {
                    if (argumentType.presentableName != null) result += argumentType
                    result += recursivelyGetLambdaParamNames(argumentType.projections)
                }
                is Variance<*> -> {
                    result += recursivelyGetLambdaParamNames(listOf(argumentType.inner))
                }
                is Nullable -> {
                    result += recursivelyGetLambdaParamNames(listOf(argumentType.inner))
                }
                is TypeParameter -> {
                    /* Type parameters can't be lambdas, and are fully squashed to strings. */
                }
                is TypeAliased -> { // No clear way to decide which
                    result += recursivelyGetLambdaParamNames(
                        setOf(argumentType.inner, argumentType.typeAlias).toList()
                    )
                }
                is PrimitiveJavaType, is JavaObject, Void, Dynamic, Star -> { /* Do nothing */ }
                is UnresolvedBound -> { /* Nothing we can do. We warn elsewhere for this case. */ }
            }
        }
        return result
    }

    private fun returnType(tags: List<Return>, returnType: TypeProjectionComponent):
        SummaryList<TwoPaneSummaryItem<TypeProjectionComponent, DescriptionComponent>> {
        val params = tags.map { tag ->
            DefaultTwoPaneSummaryItem(
                TwoPaneSummaryItem.Params(
                    title = returnType,
                    description = description(tag)
                )
            )
        }

        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(TableTitle.Params("Returns")),
                items = params
            )
        )
    }

    private fun throws(tags: List<Throws>):
        SummaryList<TwoPaneSummaryItem<ParameterComponent, DescriptionComponent>> {
        val params = tags.map { tag ->
            DefaultTwoPaneSummaryItem(
                TwoPaneSummaryItem.Params(
                    title = throwsToParameterComponent(tag),
                    description = description(tag)
                )
            )
        }

        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(TableTitle.Params("Throws")),
                items = params
            )
        )
    }

    private fun String.firstWord() = substring(0, indexOfFirst { it == ' ' })

    private fun throwsToParameterComponent(throws: Throws): ParameterComponent {
        var name = throws.name
        val link = if (throws.name in listOf("a", "an")) {
            println(
                "WARNING: do not use '${throws.name}' before the exception type in an @throws" +
                    " statement. This is against jdoc spec, will be an error in the next version " +
                    "of dackka, and your exception is not being linked and looks bad. " +
                    "This was observed in $throws."
            )
            name = throws.text().firstWord()
            DefaultLink(Link.Params(name, ""))
        } else if ("{@link" in name) {
            println(
                "WARNING: do not {@link the exception type in an @throws statement. @throws state" +
                    "ments are automatically linked. Manually java-linking them is against jdoc s" +
                    "pec, will be an error in the next version of dackka, and breaks linking beha" +
                    "vior causing them to actually *not* be linked. This was observed in $throws."
            )
            name = name.removePrefix("{@link ").removeSuffix("}")
            DefaultLink(Link.Params(name, ""))
        } else pathProvider.linkForReference(throws.exceptionAddress!!, throws.name)
        return DefaultParameterComponent(
            ParameterComponent.Params(
                displayLanguage = displayLanguage,
                name = name,
                type = DefaultTypeProjectionComponent(
                    TypeProjectionComponent.Params(
                        type = link,
                        displayLanguage = displayLanguage,
                        nullability = Nullability.DONT_CARE
                    )
                )
            )
        )
    }

    private fun see(tags: List<See>): SummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>> {
        val params = tags.map { tag ->
            DefaultTwoPaneSummaryItem(
                TwoPaneSummaryItem.Params(
                    title = tag.toLink(),
                    description = description(tag)
                )
            )
        }

        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(TableTitle.Params("See also")),
                items = params
            )
        )
    }

    /**
     * Gets a Description for the Documentable, or returns UndocumentedSymbolDescription()
     * Has special handling to inject @property documentation as a description, if it exists
     * Also applies to @param documentation that should become a description, i.e. property params
     */
    private fun Documentable.getDescription(summary: Boolean): DescriptionComponent {
        val components = mutableListOf<DocTag>()
        tags().forEach {
            when (it) {
                is Description -> {
                    it.children.forEach { child ->
                        recursivelyConsiderPsAndTextsForJavaSamples(
                            child, components, this.sourceSets.single().samples
                        )
                    }
                }
                is Sample -> {
                    val dri = it.name

                    // TODO: fix this to allow KMP to work. Currently asserts single-platform. b/181224204
                    val sourceSet = sourceSets.single()

                    val facade = analysisMap[sourceSet]?.facade
                        ?: throw RuntimeException("Cannot resolve facade: ${sourceSet.sourceSetID}")
                    val psiElement = fqNameToPsiElement(facade, dri)
                        ?: throw RuntimeException("Cannot find PsiElement corresponding to $dri")

                    val imports = processImports(psiElement)
                    val body = processBody(psiElement)

                    components.add(CodeBlock(listOf(Text(imports + body))))
                    components.addAll(it.children)
                }
                is NamedTagWrapper -> if (it.name == name) components.add(it.root)
                is Author, is Version, is Since, is Return, is Receiver, is Constructor,
                is Deprecated, is Suppress -> { /* TODO: We do not support these tags yet */ }
            }
        }
        if (components.isEmpty()) return UndocumentedSymbolDescriptionComponent()
        return description(components, summary, null)
    }

    private fun recursivelyConsiderPsAndTextsForJavaSamples(
        root: DocTag,
        components: MutableList<DocTag>,
        samples: Set<File>
    ) {
        if ("@sample" !in root.text()) components.add(root)
        else {
            when (root) {
                is Text -> {
                    val parts = root.body.split("{", "}")
                    for (part in parts) {
                        if ("@sample" !in part) {
                            if (part.isNotBlank()) components.add(Text(part.trim()))
                        } else components.add(
                            convertTextToJavaSample(Text(part.trim()), samples, docsHolder.logger)
                        )
                    }
                }
                is P -> {
                    for (child in root.children) {
                        recursivelyConsiderPsAndTextsForJavaSamples(child, components, samples)
                    }
                }
                // Having non-text components on the same line as a samples is not supported
                else -> throw RuntimeException("considered invalid type ${root::class} for sample")
            }
        }
    }

    private fun WithChildren<DocTag>.text(): String {
        return if (this is Text) this.body else children.joinToString(" ") { it.text() }
    }

    private fun description(
        soleComponent: TagWrapper,
        summary: Boolean = false,
        deprecation: String? = null
    ): DescriptionComponent {
        return description(soleComponent.children, summary, deprecation)
    }

    private fun description(
        components: List<DocTag> = emptyList(),
        summary: Boolean = false,
        deprecation: String? = null
    ): DescriptionComponent {
        return DefaultDescriptionComponent(
            DescriptionComponent.Params(
                pathProvider,
                components,
                summary,
                deprecation
            )
        )
    }

    /** Returns the component for a deprecation. */
    private fun deprecationComponent(
        documentable: Documentable,
        summary: Boolean,
        annotations: List<Annotations.Annotation>
    ): DescriptionComponent? {
        val deprecation = findDeprecation(documentable, annotations) ?: return null
        return description(deprecation.children, summary, documentable.deprecationText())
    }

    /**
     * Finds either the javadoc @deprecated tag or the Kotlin @Deprecated annotation [TagWrapper].
     */
    private fun findDeprecation(
        documentable: Documentable,
        annotations: List<Annotations.Annotation>
    ): Deprecated? {
        val javadocDeprecation = documentable.find<Deprecated>()
        if (javadocDeprecation != null) {
            // Prefer javadoc deprecation messages since they allow formatting
            return javadocDeprecation
        }

        val annotationDeprecationMessage = annotations.filter {
            it.isDeprecated()
        }.strictSingleOrNull()?.params?.get("message")?.cast<StringValue>()?.value ?: return null
        // Dokka makes message="foo" show up as "\"foo\"" since you typically want to show quotes
        // when rendering an annotation. Remove those outer quotes.
        val message = annotationDeprecationMessage.removeSurrounding("\"")

        return Deprecated(P(children = listOf(Text(message))))
    }

    private fun Documentable.deprecationText() =
        "This ${this.stringForType(displayLanguage)} is deprecated."

    /** Retrieves the doc tags of type [T]. */
    private inline fun <reified T> Documentable.find() =
        tags().filterIsInstance<T>().strictSingleOrNull()

    /**
     * @return the doc tags (aka human-written javadoc or kdoc) associated with this documentable
     */
    private fun Documentable.tags() = documentation.values.singleOrNull()?.children.orEmpty()

    /** Like singleOrNull, but requires that only one element be present if any. */
    private fun <T> List<T>.strictSingleOrNull() = if (isEmpty()) {
        null
    } else {
        single()
    }

    private fun tagOrder(paramNames: List<String>) = compareBy<TagWrapper> { tag ->
        when (tag) {
            is Deprecated -> 0
            is Description -> 1
            is Return -> 2
            is Constructor -> 3
            is Property -> 4
            is Receiver -> 5
            is Param -> 6
            is Throws -> 7
            is See -> 8
            is Sample -> 9
            is Since -> 10
            is Version -> 11
            is Author -> 12
            is Suppress -> 13
            is CustomTagWrapper -> 14
        }
    }.thenBy { tag ->
        when (tag) {
            is Param -> paramNames.indexOf(tag.name)
            else -> -1
        }
    }

    /**
     * Extract the see tag's reference into a link.
     *
     * This one is painful. An address is only sometimes there, other times there's a docs link
     * nested somewhere in the tree, and as a last resort the name is always present with whatever
     * a developer writes which could either be a fully qualified reference or just the URL
     * fragment.
     */
    private fun See.toLink(): Link {
        val address = address
        if (address != null) {
            return pathProvider.linkForReference(address)
        }
        val docsLink = root.explodedChildren.filterIsInstance<DocumentationLink>().singleOrNull()
        if (docsLink != null) {
            return pathProvider.linkForReference(docsLink.dri)
        }

        // TODO(b/167437580): figure out how to reliably parse links
        val segments = name.split("#")
        return if (segments.size == 1) {
            // Assume we have a fully qualified type
            val (packageName, typeName) = fullyQualifiedTypeToPackageNameAndType(segments.single())
            if (packageName.isEmpty() || typeName.isEmpty()) {
                // Turns out we didn't, so give up
                DefaultLink(Link.Params(name, url = ""))
            } else {
                pathProvider.linkForReference(DRI(packageName, typeName))
            }
        } else if (segments.size == 2) {
            val (type, anchor) = segments
            if (type.isEmpty()) {
                // Self link
                DefaultLink(Link.Params(anchor, anchor))
            } else {
                // Assume fully qualified link with anchor
                val (packageName, typeName) = fullyQualifiedTypeToPackageNameAndType(type)
                val url = pathProvider.forType(packageName, typeName)
                DefaultLink(Link.Params(typeName, "$url#$anchor"))
            }
        } else {
            error("Could not understand path: $name")
        }
    }

    /** Horrible guess-work to try and extract the package and type names. */
    private fun fullyQualifiedTypeToPackageNameAndType(full: String): Pair<String, String> {
        val parts = full.split(".")

        val packageName = parts.takeWhile { it.all(Char::isLowerCase) }.joinToString(".")
        val typeName = parts.takeLastWhile {
            if (it.isEmpty()) throw RuntimeException("empty element in FQTTPNAT. Full: $full")
            else it.first().isUpperCase()
        }.joinToString(".")

        return packageName to typeName
    }

    // Duplicated from PropertyDocumentableConverter. This is the price of global variables.
    internal fun DProperty.signature(isSummary: Boolean): PropertySignature {
        val receiver = receiver?.let {
            paramConverter.componentForParameter(
                param = it,
                isSummary = isSummary,
                isFromJava = isFromJava()
            )
        }
        return DefaultPropertySignature(
            PropertySignature.Params(
                // TODO(b/168136770): figure out path for default anchors
                name = pathProvider.linkForReference(dri),
                receiver = when (displayLanguage) {
                    Language.JAVA -> null
                    Language.KOTLIN -> receiver
                }
            )
        )
    }

    /**
     * Converts a generic List<Documentable> to a SummaryList.
     * Does nothing clever; only converts Documentables to links (by default with annotations)
     */
    internal fun docsToSummary(
        documentables: List<Documentable>,
        showAnnotations: Boolean = false
    ) = DefaultSummaryList<TwoPaneSummaryItem<Link, DescriptionComponent>>(
        SummaryList.Params(
            items = documentables
                .map { summaryForDocumentable(it, showAnnotations) }
        )
    )

    /**
     * Converts generic Documentables to TwoPaneSummaryItems, as simple maybe-annotated links
     * This is used for mini-signatures, e.g. nested types list, subclasses list, package summary
     */
    internal fun summaryForDocumentable(
        documentable: Documentable,
        showAnnotations: Boolean = false
    ): DefaultTwoPaneSummaryItem<Link, DescriptionComponent> {
        val annotations = documentable.annotations()
        return DefaultTwoPaneSummaryItem(
            TwoPaneSummaryItem.Params(
                title = if (showAnnotations) {
                    DefaultAnnotatedLink(
                        AnnotatedLink.Params(
                            annotations = annotations.annotationComponents(
                                pathProvider = pathProvider,
                                displayLanguage = displayLanguage,
                                nullability = Nullability.DONT_CARE // Not useful for these cases
                            ),
                            link = pathProvider.linkForReference(documentable.dri)
                        )
                    )
                } else {
                    pathProvider.linkForReference(documentable.dri)
                },
                description = summaryDescription(documentable, annotations)
            )
        )
    }
}
