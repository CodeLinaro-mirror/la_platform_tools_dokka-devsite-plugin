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
import com.google.devsite.components.impl.DefaultRaw
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableTitle
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.components.impl.UndocumentedSymbolDescription
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableTitle
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.StringValue
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
    private val pathProvider: FilePathProvider
) {
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
    fun metadata(
        doc: Documentable,
        returnType: ContextFreeComponent? = null,
        paramNames: List<String> = emptyList(),
        annotations: List<Annotations.Annotation> = emptyList()
    ): List<ContextFreeComponent> {
        val description = doc.find<Description>()?.let(::description)
        val deprecation = deprecationComponent(doc, summary = false, annotations)
        val receiverParam = doc.find<Receiver>()?.let {
            Param(it.root, "receiver")
        }

        val preparedTags = listOfNotNull(receiverParam) + doc.tags()
        val tagsByType = preparedTags.sortedWith(tagOrder(paramNames)).groupBy { it.javaClass }
        val tables = tagsByType.mapNotNull { (_, tags) ->
            // We know all the elements in `tags` will be of the same type, so we pick an arbitrary
            // one to do the switching and then cast the list to its type.
            @kotlin.Suppress("UNCHECKED_CAST")
            when (tags.first()) {
                is Param -> params(tags as List<Param>)
                is Return -> returnType(tags as List<Return>, checkNotNull(returnType))
                is Throws -> throws(tags as List<Throws>)
                is See -> see(tags as List<See>)
                is Sample -> null // TODO("b/163811276: sample")
                is Property -> null // TODO("b/163811276: property")
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

    private fun params(tags: List<Param>): SummaryList {
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
                header = DefaultTableTitle(TableTitle.Params("Parameters")),
                items = params
            )
        )
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
        deprecation: String? = null
    ): DescriptionComponent {
        return DefaultDescription(
            DescriptionComponent.Params(
                pathProvider,
                tag.root,
                summary,
                deprecation
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
            it.dri.classNames == "Deprecated"
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
}
