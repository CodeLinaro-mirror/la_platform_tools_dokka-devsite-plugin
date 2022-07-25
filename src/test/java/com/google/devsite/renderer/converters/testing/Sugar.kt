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
import com.google.devsite.TypeSummaryItem
import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.Link
import com.google.devsite.components.pages.ClassIndex
import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.TableOfContents
import com.google.devsite.components.symbols.AnnotationComponent
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.MappedTypeProjectionComponent
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.SymbolSignature
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeParameterComponent
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.InheritedSymbolsList
import com.google.devsite.components.table.SingleColumnSummaryItem
import com.google.devsite.components.table.SummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.joinMaybePrefix
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.Text

internal fun <T, C : Collection<T>> C.item(): T = items(1).single()

internal fun <T, C : Collection<T>> C.items(size: Int?) = apply {
    if (size != null) assertThat(this).hasSize(size)
}

internal fun TableOfContents.items(size: Int?) = data.packages.items(size)
internal fun TableOfContents.item() = items(1).single()
internal fun ClassIndex.items(size: Int?) = data.alphabetizedClasses.entries.items(size)
internal fun ClassIndex.item() = items(1).single()

internal fun <T> DevsitePage.content(): T = data.content as T

internal fun <T : ContextFreeComponent, V : ContextFreeComponent>
SummaryList<TwoPaneSummaryItem<T, V>>.item() = items(1).single()
internal fun <T : ContextFreeComponent, V : ContextFreeComponent>
SummaryList<TwoPaneSummaryItem<T, V>>.items(size: Int? = null) = data.items.items(size)
internal fun <T : ContextFreeComponent> SummaryList<SingleColumnSummaryItem<T>>.sItems(
    size: Int? = null
) = data.items.items(size)
internal fun <T : ContextFreeComponent, V : ContextFreeComponent>
SummaryList<TwoPaneSummaryItem<T, V>>.single() = items().single()
internal fun <T : ContextFreeComponent> SummaryList<SingleColumnSummaryItem<T>>.single() =
    sItems().single()
internal val SummaryList<*>.size get() = data.items.size
internal fun <V : SummaryItem> SummaryList<V>.first() = data.items.first()

internal fun TwoPaneSummaryItem<Link, DescriptionComponent>.link() = data.title.data
@JvmName("paramDescriptionTwoPaneSummaryItemLink")
internal fun TwoPaneSummaryItem<ParameterComponent, DescriptionComponent>.link() =
    data.title.data.type.link()

@get:JvmName("FunctionSummaryItemGetDescription")
internal val <T : SymbolSignature> TypeSummaryItem<T>.description get() = data.description

internal fun TwoPaneSummaryItem<TypeSummary, *>.modifiers() = data.title.data.modifiers
internal fun Classlike.modifiers() = data.signature.data.modifiers

@JvmName("TypeSymbolSummaryName")
internal fun <T : SymbolSignature> TypeSummaryItem<T>.name():
    String = this.data.description.name()
internal fun TwoPaneSummaryItem<ParameterComponent, DescriptionComponent>
.name(): String = this.data.title.data.name
@JvmName("LinkDescriptionName")
internal fun TwoPaneSummaryItem<Link, DescriptionComponent>
.name(): String = this.data.title.data.name
internal fun Iterable<TypeSummaryItem<PropertySignature>>.nonInstance() =
    filterNot { it.data.title.data.type.name() == "INSTANCE" }

internal fun SymbolSummary<*>.name(): String = data.signature.data.name.data.name
internal fun SymbolSummary<FunctionSignature>.functionSignature() = data.signature.data

internal fun DescriptionComponent.text() =
    this.data.components.joinToString(" ") { it.text() }

internal fun DocTag.text(): String = (this as? Text)?.body
    ?: children.joinToString(" ") { it.text() }

internal fun TypeParameterComponent.projectionName() = this.data.type.name()
internal fun ParameterComponent.generics() = this.data.type.data.generics

internal fun TypeProjectionComponent.link(): Link.Params = data.type.data
internal fun ParameterComponent.link(): Link.Params = data.type.link()
internal fun TypeProjectionComponent.alternativeLink(): Link.Params? =
    (this as? MappedTypeProjectionComponent)?.data?.alternativePrefix?.data

internal fun TypeProjectionComponent.name() = link().name
internal fun FunctionSignature.receiverTypeName() = data.receiver!!.data.type.let {
    it.link().name + it.data.nullability.renderAsKotlinSuffix()
}

internal fun ParameterComponent.typeName() = data.type.name()
internal fun ParameterComponent.fullTypeName() =
    typeName() + generics().joinMaybePrefix(prefix = "<", postfix = ">") { it.name() }
internal fun ParameterComponent.typeAnnotations() = data.type.annotations

internal fun AnnotationComponent.link(): Link.Params = data.type.data
internal val AnnotationComponent.isAtNullable get() = this.link().name == "Nullable"
internal val AnnotationComponent.isAtNonNull get() = this.link().name == "NonNull"
internal fun List<AnnotationComponent>.exceptNonNull() = this.filter { it.link().name != "NonNull" }
internal fun Pair<SummaryList<*>, Classlike.TitledList<*>>.title() = first.data.header!!.data.title

@JvmName("titleFunctionSignature")
internal fun InheritedSymbolsList<FunctionSignature>.title() = data.header.data.title
internal fun InheritedSymbolsList<PropertySignature>.title() = data.header.data.title
@JvmName("fromFunctionSignature")
internal fun InheritedSymbolsList<FunctionSignature>.from(name: String) =
    data.inheritedSymbolSummaries.entries.singleOrNull { (key, _) -> key.data.name == name }
internal fun InheritedSymbolsList<PropertySignature>.from(name: String) =
    data.inheritedSymbolSummaries.entries.singleOrNull { (key, _) -> key.data.name == name }

internal fun Classlike.companionName() = data.nestedTypesSummary.item().link().name
