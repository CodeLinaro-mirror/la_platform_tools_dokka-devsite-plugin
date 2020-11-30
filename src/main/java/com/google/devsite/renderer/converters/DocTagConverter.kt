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
import com.google.devsite.components.Link
import com.google.devsite.components.Raw
import com.google.devsite.components.impl.DefaultDescription
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.impl.DefaultPropertySignature
import com.google.devsite.components.impl.DefaultRaw
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableTitle
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.components.impl.UndocumentedSymbolDescription
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.table.SummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableTitle
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.parent
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Callable
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeParameter
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.StringValue
import org.jetbrains.dokka.model.WithConstructors
import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.doc.Author
import org.jetbrains.dokka.model.doc.Constructor
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Deprecated
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocumentationLink
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
import com.google.devsite.components.Description as DescriptionComponent

/** Extracts the hand written documentation from documentables into the correct components. */
internal class DocTagConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder
) {
    private val classGraph = runBlocking { docsHolder.subclassGraph() }
    private val paramConverter = ParameterDocumentableConverter(displayLanguage, pathProvider)

    /** @return the hand-written javadoc */
    fun summaryDescription(
        doc: Documentable,
        annotations: List<Annotations.Annotation> = emptyList()
    ): DescriptionComponent {
        val deprecation = deprecationComponent(doc, summary = true, annotations)
        return if (deprecation == null) {
            val description = doc.find<Description>()
            if (description == null) {
                UndocumentedSymbolDescription()
            } else {
                description(description, summary = true)
            }
        } else {
            deprecation
        }
    }

    /**
     * Returns a breakdown of the different metadata as a deprecation warning, description, and then
     * separate summaries. Examples include the list of parameters, return type, see also, throws,
     * etc.
     */
    @kotlin.Suppress("UNCHECKED_CAST")
    fun metadata(
        doc: Documentable,
        returnType: ContextFreeComponent? = null,
        paramNames: List<String> = emptyList(),
        annotations: List<Annotations.Annotation> = emptyList()
    ): List<ContextFreeComponent> {
        var description = doc.find<Description>()?.let(::description)
        val deprecation = deprecationComponent(doc, summary = false, annotations)
        val receiverParam = doc.find<Receiver>()?.let {
            Param(it.root, "receiver")
        }
        val parameters = if (doc is DFunction) doc.parameters else emptyList()
        val generics = if (doc is WithGenerics) doc.generics else emptyList()

        val preparedTags = listOfNotNull(receiverParam) + doc.tags()
        val tagsByType = preparedTags.sortedWith(tagOrder(paramNames)).groupBy { it.javaClass }
        val tables = tagsByType.mapNotNull { (_, rawTags) ->
            // b/172000585
            var tags = specialHandlingForUpstreamParameterBugs(doc, rawTags, generics)
            if (tags.isEmpty()) return@mapNotNull null
            val firstTag = tags.first()
            if (firstTag is Param && doc is DProperty) {
                // DProperties can be documented as @param. In this case, the @param's contents
                // should be moved into the description for the property.
                tags.firstOrNull { (it as Param).name == doc.name }?.let {
                    tags = tags - it
                    description = if (description == null) description(it)
                    else description(doc.find<Description>()!!, additionalDesc = it)
                }
                if (tags.isEmpty()) return@mapNotNull null
            }
            // We know all the elements in `tags` will be of the same type, so we pick an arbitrary
            // one to do the switching and then cast the list to its type.
            @kotlin.Suppress("UNCHECKED_CAST")
            when (firstTag) {
                is Param -> params(tags as List<Param>, parameters, generics, doc)
                is Return -> returnType(tags as List<Return>, checkNotNull(returnType))
                is Throws -> throws(tags as List<Throws>)
                is See -> see(tags as List<See>)
                is Sample -> null // TODO("b/163811276: sample")
                // TODO b/173798088 handle (property) parameters documented as @property
                // TODO b/173034166 handle (property) parameters documented as @param
                is Property -> null // TODO("b/173034166")
                is CustomTagWrapper -> null // TODO("b/163811276: custom tag wrapper")
                is Since -> TODO("b/163811276: since")
                is Constructor -> TODO("b/163811276: constructor")
                // Documented separately above
                is Description, is Deprecated, is Receiver -> null
                // Don't care ;)
                is Suppress, is Version, is Author -> null
            }
        }

        return listOfNotNull(deprecation, description, *tables.toTypedArray())
    }

    private fun List<TagWrapper>.names() = this.map { ungenerify((it as Param).name) }

    private fun specialHandlingForUpstreamParameterBugs(
        doc: Documentable,
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
            // doc is DClasslike. DClasslike's only valid @params are type params
            if (doc is DClasslike) {
                val genericNames = generics.map { it.name }
                val invalidNames = tags.names().filter { it !in genericNames }
                if (invalidNames.isEmpty()) return tags
                // enforce that the propagated documentation makes sense somewhere
                val possibleTrueReferents = doc.properties.map { it.name }.toMutableList()
                if (doc is WithConstructors) {
                    for (constructor in doc.constructors) {
                        possibleTrueReferents.addAll(constructor.parameters.map { it.name!! })
                    }
                }
                assert(invalidNames.all { it in possibleTrueReferents })
                // Use only documentation for type parameters in the parameter documentation table
                return tags.filter { ungenerify((it as Param).name) in genericNames }
            }
            // doc is Property. It is possible that a parameter property is documented on the class
            // as @param. That doc should be used as though it were @property. Handled there.
            // Properties can also have @param documentation for type parameters
            if (doc is DProperty) {
                val genericNames = generics.map { it.name }
                val invalidNames = tags.names().filter { it !in genericNames && it != doc.name }
                if (invalidNames.isEmpty()) return tags
                // enforce that the propagated documentation makes sense somewhere
                val parentClasslike = classGraph.getValue(doc.dri.parent)
                val parentDocNames = parentClasslike.self.documentation.values.single().children
                    .filterIsInstance<Param>().map { it.name }
                assert(invalidNames.all { it in parentDocNames })
                return tags.filter { ungenerify((it as Param).name) in genericNames }
            }
            // DFunction constructors may have propagated @property documentation, handled there
        }
        return tags
    }

    private fun params(
        tags: List<Param>,
        dParams: List<DParameter>,
        dGenerics: List<DTypeParameter>,
        doc: Documentable // for debugging
    ): SummaryList {
        // @param can refer to parameters, type parameters, receivers
        val allOptions = mutableMapOf<String, ContextFreeComponent>()
        allOptions.putAll(dParams.map {
            it.name!! to paramConverter.componentForParameter(it, false) })
        allOptions.putAll(dGenerics.map {
            it.name to paramConverter.componentForTypeParameter(it, false) })
        if (doc is Callable && doc.receiver != null) allOptions[doc.receiver!!.name ?: "receiver"] =
            paramConverter.componentForParameter(doc.receiver!!, false)

        val params = tags.map { tag ->
            val title = allOptions[ungenerify(tag.name)]!!
            DefaultTwoPaneSummaryItem(
                TwoPaneSummaryItem.Params(
                    title = title,
                    description = description(tag)
                )
            )
        }

        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(TableTitle.Params("Parameters")),
                items = params as List<SummaryItem>
            )
        )
    }

    // Turn both "E" and "<E>" to "E"
    private fun ungenerify(name: String): String {
        if (name.startsWith('<') && name.endsWith('>')) return name.drop(1).dropLast(1)
        return name
    }

    private fun returnType(tags: List<Return>, returnType: ContextFreeComponent): SummaryList {
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

    private fun throws(tags: List<Throws>): SummaryList {
        val params = tags.map { tag ->
            DefaultTwoPaneSummaryItem(
                TwoPaneSummaryItem.Params(
                    title = DefaultRaw(Raw.Params(tag.name)),
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

    private fun see(tags: List<See>): SummaryList {
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

    private fun description(
        tag: TagWrapper,
        summary: Boolean = false,
        deprecation: String? = null,
        additionalDesc: TagWrapper? = null
    ): DescriptionComponent {
        return DefaultDescription(
            DescriptionComponent.Params(
                pathProvider,
                tag.root,
                summary,
                deprecation,
                additionalDesc?.root
            )
        )
    }

    /** Returns the component for a deprecation. */
    private fun deprecationComponent(
        doc: Documentable,
        summary: Boolean,
        annotations: List<Annotations.Annotation>
    ): DescriptionComponent? {
        val deprecation = findDeprecation(doc, annotations) ?: return null
        return description(deprecation, summary, deprecation = doc.deprecationText())
    }

    /**
     * Finds either the javadoc @deprecated tag or the Kotlin @Deprecated annotation [TagWrapper].
     */
    private fun findDeprecation(
        doc: Documentable,
        annotations: List<Annotations.Annotation>
    ): Deprecated? {
        val javadocDeprecation = doc.find<Deprecated>()
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
        val typeName = parts.takeLastWhile { it.first().isUpperCase() }.joinToString(".")

        return packageName to typeName
    }

    // Duplicated from PropertyDocumentableConverter. This is the price of global variables.
    internal fun DProperty.signature(isSummary: Boolean): PropertySignature {
        val receiver = receiver?.let { paramConverter.componentForParameter(it, isSummary) }
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
}
