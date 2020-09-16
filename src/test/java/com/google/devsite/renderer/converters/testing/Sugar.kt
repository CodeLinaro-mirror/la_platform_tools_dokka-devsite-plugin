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
import com.google.devsite.components.ClassIndex
import com.google.devsite.components.Component
import com.google.devsite.components.DevsitePage
import com.google.devsite.components.FunctionSummary
import com.google.devsite.components.Link
import com.google.devsite.components.Parameter
import com.google.devsite.components.ParameterBase
import com.google.devsite.components.ParameterType
import com.google.devsite.components.SummaryList
import com.google.devsite.components.TableOfContents
import com.google.devsite.components.TableTitle
import com.google.devsite.components.TwoPaneSummaryItem

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

internal fun TwoPaneSummaryItem.link(): Link.Params = (data.title as Link).data
internal fun TwoPaneSummaryItem.functionSummary() = data.description as FunctionSummary
internal fun FunctionSummary.name(): String = data.signature.data.name.data.name
internal fun SummaryList.title(): String = (data.header as TableTitle).data.title

internal fun ParameterBase.asType(): ParameterType = when (this) {
    is Parameter -> data.primary.asType()
    is ParameterType -> this
    else -> error("Not supported: $this")
}

internal fun ParameterType.link(): Link.Params = data.type.data
internal fun ParameterBase.link(): Link.Params = when (this) {
    is Parameter -> data.primary.asType().link()
    is ParameterType -> data.type.data
    else -> error("Not supported: $this")
}
