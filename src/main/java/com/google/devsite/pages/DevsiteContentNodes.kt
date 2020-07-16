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

package com.google.devsite.pages

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.DCI
import org.jetbrains.dokka.pages.Kind
import org.jetbrains.dokka.pages.Style

enum class DevsiteContentKind : Kind {
    AllClasses, OverviewSummary, PackageSummary, Class, OverviewTree, PackageTree
}

abstract class DevsiteContentNode(
        dri: Set<DRI>,
        kind: Kind,
        override val sourceSets: Set<DokkaSourceSet>
) : ContentNode {
    override val dci: DCI = DCI(dri, kind)
    override val style: Set<Style> = emptySet()
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = this
}

interface DevsiteList {
    val tabTitle: String
    val colTitle: String
    val children: List<DevsiteListEntry>
}

interface DevsiteListEntry {
    val stringTag: String
}

class EmptyNode(
        dri: DRI,
        kind: Kind,
        override val sourceSets: Set<DokkaSourceSet>,
        override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override val dci: DCI = DCI(setOf(dri), kind)
    override val style: Set<Style> = emptySet()

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode =
            EmptyNode(dci.dri.first(), dci.kind, sourceSets, newExtras)

    override fun hasAnyContent(): Boolean = false
}

class DevsiteContentGroup(
        val dri: Set<DRI>,
        val kind: Kind,
        sourceSets: Set<DokkaSourceSet>,
        override val children: List<DevsiteContentNode>
) : DevsiteContentNode(dri, kind, sourceSets) {

    companion object {
        operator fun invoke(
                dri: Set<DRI>,
                kind: Kind,
                sourceSets: Set<DokkaSourceSet>,
                block: JavaContentGroupBuilder.() -> Unit
        ): DevsiteContentGroup =
                DevsiteContentGroup(dri, kind, sourceSets, JavaContentGroupBuilder(sourceSets).apply(block).list)
    }

    override fun hasAnyContent(): Boolean = children.isNotEmpty()
}

class JavaContentGroupBuilder(val sourceSets: Set<DokkaSourceSet>) {
    val list = mutableListOf<DevsiteContentNode>()
}

class TitleNode(
        val title: String,
        val subtitle: List<ContentNode>,
        val version: String,
        val parent: String?,
        val dri: Set<DRI>,
        val kind: Kind,
        sourceSets: Set<DokkaSourceSet>
) : DevsiteContentNode(dri, kind, sourceSets) {
    override fun hasAnyContent(): Boolean = !title.isBlank() || !version.isBlank() || subtitle.isNotEmpty()
}

fun JavaContentGroupBuilder.title(
        title: String,
        subtitle: List<ContentNode>,
        version: String,
        parent: String? = null,
        dri: Set<DRI>,
        kind: Kind
) {
    list.add(TitleNode(title, subtitle, version, parent, dri, kind, sourceSets))
}

class RootListNode(
        val entries: List<LeafListNode>,
        val dri: Set<DRI>,
        val kind: Kind,
        sourceSets: Set<DokkaSourceSet>
) : DevsiteContentNode(dri, kind, sourceSets) {
    override fun hasAnyContent(): Boolean = children.isNotEmpty()
}

class LeafListNode(
        val tabTitle: String,
        val colTitle: String,
        val entries: List<DevsiteListEntry>,
        val dri: Set<DRI>,
        val kind: Kind,
        sourceSets: Set<DokkaSourceSet>
) : DevsiteContentNode(dri, kind, sourceSets) {
    override fun hasAnyContent(): Boolean = children.isNotEmpty()
}

fun JavaContentGroupBuilder.rootList(
        dri: Set<DRI>,
        kind: Kind,
        rootList: List<DevsiteList>
) {
    val children = rootList.map {
        LeafListNode(it.tabTitle, it.colTitle, it.children, dri, kind, sourceSets)
    }
    list.add(RootListNode(children, dri, kind, sourceSets))
}

fun JavaContentGroupBuilder.leafList(
        dri: Set<DRI>,
        kind: Kind,
        leafList: DevsiteList
) {
    list.add(LeafListNode(leafList.tabTitle, leafList.colTitle, leafList.children, dri, kind, sourceSets))
}

fun JavadocList(tabTitle: String, colTitle: String, children: List<DevsiteListEntry>) = object : DevsiteList {
    override val tabTitle = tabTitle
    override val colTitle = colTitle
    override val children = children
}

class LinkDevsiteListEntry(
        val name: String,
        val dri: Set<DRI>,
        val kind: Kind = ContentKind.Symbol,
        val sourceSets: Set<DokkaSourceSet>
) : DevsiteListEntry {
    override val stringTag: String
        get() = if (builtString == null)
            throw IllegalStateException("stringTag for LinkJavadocListEntry accessed before build() call")
        else builtString!!

    private var builtString: String? = null

    fun build(body: (String, Set<DRI>, Kind, List<DokkaSourceSet>) -> String) {
        builtString = body(name, dri, kind, sourceSets.toList())
    }
}

data class RowDevsiteListEntry(val link: LinkDevsiteListEntry, val doc: List<ContentNode>) : DevsiteListEntry {
    override val stringTag: String = ""
}

data class DevsiteSignatureContentNode(
        val dri: DRI,
        val kind: Kind = ContentKind.Symbol,
        val annotations: ContentNode?,
        val modifiers: ContentNode?,
        val signatureWithoutModifiers: ContentNode,
        val supertypes: ContentNode?
) : DevsiteContentNode(setOf(dri), kind, signatureWithoutModifiers.sourceSets) {
    override fun hasAnyContent(): Boolean = true
}
