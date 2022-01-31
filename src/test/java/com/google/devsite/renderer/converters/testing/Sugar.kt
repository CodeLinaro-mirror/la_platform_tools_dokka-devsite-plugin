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

@file:Suppress("UNCHECKED_CAST")

package com.google.devsite.renderer.converters.testing

import com.google.common.truth.Truth.assertThat
import com.google.devsite.components.Component
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.Link
import com.google.devsite.components.pages.ClassIndex
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.TableOfContents
import com.google.devsite.components.symbols.AnnotationComponent
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.MappedTypeProjectionComponent
import com.google.devsite.components.symbols.MiniSignature
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeParameterComponent
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.components.table.SingleColumnSummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableTitle
import com.google.devsite.components.table.TwoPaneSummaryItem
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.Text

internal fun <T> Collection<T>.item(): T = items(1).single()

internal fun <T> Collection<T>.items(size: Int?) = apply {
    if (size != null) assertThat(this).hasSize(size)
}

internal fun <R> Component<*>.item(): R = items<R>(1).item()

internal fun <R> Component<*>.items(size: Int?): Collection<R> {
    val items = when (this) {
        is SummaryList -> data.items
        is TableOfContents -> data.packages
        is ClassIndex -> data.alphabetizedClasses.entries
        else -> error("Unknown type: $javaClass")
    } as Collection<R>

    return items.items(size)
}

internal fun <T> DevsitePage.content(): T = data.content as T

internal fun SummaryList.item() = item<TwoPaneSummaryItem>()
internal fun SummaryList.items(size: Int? = null) = items<TwoPaneSummaryItem>(size)
internal fun SummaryList.sItems(size: Int? = null) = items<SingleColumnSummaryItem>(size)
internal fun SummaryList.single() = items().single()
internal fun SummaryList.size() = items().size
internal fun SummaryList.title(): String = (data.header as TableTitle).data.title

internal fun TwoPaneSummaryItem.link(): Link.Params = (data.title as? Link)?.data
    ?: (data.title as MiniSignature).data.link.data
internal fun TwoPaneSummaryItem.summary() = data.description as SymbolSummary
internal fun TwoPaneSummaryItem.name(): String =
    (this.data.title as? ParameterComponent)?.data?.name
    ?: (this.data.description as SymbolSummary).name()
internal fun TwoPaneSummaryItem.description() = (data.description as? DescriptionComponent)
    ?: (data.description as SymbolSummary).data.description
internal fun SingleColumnSummaryItem.description() = (data.description as? DescriptionComponent)
    ?: (data.description as SymbolSummary).data.description

internal fun SymbolSummary.name(): String = data.signature.data.name.data.name
internal fun SymbolSummary.signature() = (data.signature as FunctionSignature).data

internal fun DescriptionComponent.text() = this.data.components.joinToString(" ") { it.text() }

internal fun DocTag.text(): String = (this as? Text)?.body
    ?: children.joinToString(" ") { it.text() }

internal fun TypeParameterComponent.projectionName() = this.data.projections.single().name()
internal fun ParameterComponent.generics() = this.data.type.data.generics

internal fun TypeProjectionComponent.link(): Link.Params = data.type.data
internal fun ParameterComponent.link(): Link.Params = data.type.link()
internal fun TypeProjectionComponent.alternativeLink(): Link.Params? =
    (this as? MappedTypeProjectionComponent)?.data?.alternativePrefix?.data

internal fun TypeProjectionComponent.name() = link().name
internal fun ParameterComponent.typeName() = data.type.name()
internal fun ParameterComponent.typeAnnotations() = data.type.data.annotationComponents

internal fun AnnotationComponent.link(): Link.Params = data.type.data
internal val AnnotationComponent.isAtNullable get() = this.link().name == "Nullable"
internal val AnnotationComponent.isAtNonNull get() = this.link().name == "NonNull"

internal fun List<AnnotationComponent>.exceptNonNull() = this.filter { it.link().name != "NonNull" }
