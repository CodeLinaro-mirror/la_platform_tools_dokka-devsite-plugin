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
import com.google.devsite.components.Description
import com.google.devsite.components.Link
import com.google.devsite.components.pages.ClassIndex
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.TableOfContents
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.Parameter
import com.google.devsite.components.symbols.SymbolBase
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.SymbolType
import com.google.devsite.components.symbols.TypeParameter
import com.google.devsite.components.table.SingleColumnSummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableTitle
import com.google.devsite.components.table.TwoPaneSummaryItem
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.Text
import com.google.devsite.components.symbols.Annotation as AnnotationComponent

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

internal fun TwoPaneSummaryItem.link(): Link.Params = (data.title as Link).data
internal fun TwoPaneSummaryItem.summary() = data.description as SymbolSummary
internal fun TwoPaneSummaryItem.name(): String = (this.data.title as? Parameter)?.data?.name
    ?: (this.data.description as SymbolSummary).name()
internal fun TwoPaneSummaryItem.description() = (data.description as? Description)
    ?: (data.description as SymbolSummary).data.description
internal fun SingleColumnSummaryItem.description() = (data.description as? Description)
    ?: (data.description as SymbolSummary).data.description

internal fun SymbolSummary.name(): String = data.signature.data.name.data.name
internal fun SymbolSummary.signature() = (data.signature as FunctionSignature).data

internal fun Description.text() = this.data.components.joinToString(" ") { it.deepText() }

private fun DocTag.deepText(): String = (this as? Text)?.body
    ?: children.joinToString(" ") { it.deepText() }

internal fun TypeParameter.projectionName() = this.data.projections.single().link().name
internal fun Parameter.generics() = this.data.primary.asType().data.generics

internal fun SymbolBase.asType(): SymbolType = when (this) {
    is Parameter -> data.primary.asType()
    is SymbolType -> this
    else -> error("Not supported: $this")
}

internal fun SymbolType.link(): Link.Params = data.type.data
internal fun SymbolBase.link(): Link.Params = when (this) {
    is Parameter -> data.primary.asType().link()
    is SymbolType -> data.type.data
    else -> error("Not supported: $this")
}
internal fun AnnotationComponent.link(): Link.Params = data.type.data

internal fun List<AnnotationComponent>.exceptNonNull() = this.filter { it.link().name != "NonNull" }
