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
import com.google.devsite.components.SummaryList
import com.google.devsite.components.TableTitle
import com.google.devsite.components.TwoPaneSummaryItem
import com.google.devsite.components.impl.DefaultDescription
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.impl.DefaultRaw
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableTitle
import com.google.devsite.components.impl.DefaultTwoPaneSummaryItem
import com.google.devsite.components.impl.UndocumentedSymbolDescription
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.Author
import org.jetbrains.dokka.model.doc.Constructor
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Deprecated
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Property
import org.jetbrains.dokka.model.doc.Receiver
import org.jetbrains.dokka.model.doc.Return
import org.jetbrains.dokka.model.doc.Sample
import org.jetbrains.dokka.model.doc.See
import org.jetbrains.dokka.model.doc.Since
import org.jetbrains.dokka.model.doc.Suppress
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.model.doc.Throws
import org.jetbrains.dokka.model.doc.Version
import com.google.devsite.components.Description as DescriptionComponent

/** Extracts the hand written documentation from documentables into the correct components. */
internal class DocTagConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider
) {
    /** @return the hand-written javadoc */
    fun summaryDescription(doc: Documentable): DescriptionComponent {
        val deprecation = doc.find<Deprecated>()
        return if (deprecation == null) {
            val description = doc.find<Description>()
            if (description == null) {
                UndocumentedSymbolDescription()
            } else {
                description(description, summary = true)
            }
        } else {
            description(deprecation, summary = true, deprecation = doc.deprecationText())
        }
    }

    /**
     * Returns a breakdown of the different metadata as a deprecation warning, description, and then
     * separate summaries. Examples include the list of parameters, return type, see also, throws,
     * etc.
     */
    fun metadata(
        doc: Documentable,
        returnType: ContextFreeComponent? = null
    ): List<ContextFreeComponent> {
        val description = doc.find<Description>()?.let(::description)
        val deprecation = doc.find<Deprecated>()?.let {
            description(it, deprecation = doc.deprecationText())
        }
        val receiverParam = doc.find<Receiver>()?.let {
            Param(it.root, "receiver")
        }

        val preparedTags = listOfNotNull(receiverParam) + doc.tags()
        val tables = preparedTags.groupBy { it.javaClass }.mapNotNull { (_, tags) ->
            // We know all the elements in `tags` will be of the same type, so we pick an arbitrary
            // one to do the switching and then cast the list to its type.
            @kotlin.Suppress("UNCHECKED_CAST")
            when (tags.first()) {
                is Param -> params(tags as List<Param>)
                is Return -> returnType(tags as List<Return>, checkNotNull(returnType))
                is Throws -> throws(tags as List<Throws>)
                is See -> see(tags as List<See>)
                is Sample -> null // TODO("b/163811276: sample")
                is Property -> TODO("b/163811276: property")
                is CustomTagWrapper -> TODO("b/163811276: custom tag wrapper")
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
            val target = tag.address!!
            val path = pathProvider.forType(target.packageName!!, target.classNames.orEmpty())

            DefaultTwoPaneSummaryItem(
                TwoPaneSummaryItem.Params(
                    title = DefaultLink(
                        Link.Params(
                            name = tag.name,
                            url = path
                        )
                    ),
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
        return DefaultDescription(DescriptionComponent.Params(tag.root, summary, deprecation))
    }

    private fun Documentable.deprecationText() =
        "This ${deprecationStringForType(this)} is deprecated."

    private fun deprecationStringForType(doc: Documentable): String = when (doc) {
        is DClass -> "class"
        is DInterface -> "interface"
        is DEnum -> "enum"
        is DAnnotation -> "annotation"
        is DFunction -> when (displayLanguage) {
            Language.JAVA -> "method"
            Language.KOTLIN -> "function"
        }
        is DProperty -> when (displayLanguage) {
            Language.JAVA -> "field"
            Language.KOTLIN -> "property"
        }
        else -> error("Unsupported deprecated type: $doc")
    }

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
}
