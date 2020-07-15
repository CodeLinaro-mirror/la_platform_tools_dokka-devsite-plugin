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

package com.google.devsite.location

import com.google.devsite.pages.AllClassesPage
import com.google.devsite.pages.AnchorableJavadocNode
import com.google.devsite.pages.DevsiteClasslikePageNode
import com.google.devsite.pages.DevsiteContentKind
import com.google.devsite.pages.DevsiteEntryNode
import com.google.devsite.pages.DevsiteFunctionNode
import com.google.devsite.pages.DevsitePackagePageNode
import com.google.devsite.pages.DevsitePropertyNode
import com.google.devsite.pages.LinkDevsiteListEntry
import com.google.devsite.pages.TreeViewPage
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.resolvers.local.BaseLocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.Nullable
import org.jetbrains.dokka.links.parent
import org.jetbrains.dokka.model.OtherParameter
import org.jetbrains.dokka.model.PrimitiveJavaType
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import java.util.HashMap
import java.util.IdentityHashMap

class DevsiteLocationProvider(
        pageRoot: RootPageNode,
        dokkaContext: DokkaContext
) : BaseLocationProvider(dokkaContext) {
    private val pathIndex = IdentityHashMap<PageNode, List<String>>().apply {
        fun registerPath(page: PageNode, prefix: List<String> = emptyList()) {
            val newPrefix = prefix + page.takeIf { it is DevsitePackagePageNode }?.name.orEmpty()
            val path = (prefix + when (page) {
                is AllClassesPage -> listOf("allclasses")
                is TreeViewPage -> if (page.classes == null)
                    listOf("overview-tree")
                else
                    listOf("package-tree")
                is ContentPage -> if (page.dri.isNotEmpty() && page.dri.first().classNames != null)
                    listOfNotNull(page.dri.first().classNames)
                else if (page is DevsitePackagePageNode)
                    listOf(page.name, "package-summary")
                else
                    listOf("index")
                else -> emptyList()
            }).filterNot { it.isEmpty() }

            put(page, path)
            page.children.forEach { registerPath(it, newPrefix) }
        }
        put(pageRoot, listOf("index"))
        pageRoot.children.forEach { registerPath(it) }
    }

    private val nodeIndex = HashMap<DRI, PageNode>().apply {
        fun registerNode(node: PageNode) {
            if (node is ContentPage) put(node.dri.first(), node)
            node.children.forEach(::registerNode)
        }
        registerNode(pageRoot)
    }

    private operator fun IdentityHashMap<PageNode, List<String>>.get(
            dri: DRI
    ) = this[nodeIndex[dri]]

    private fun List<String>.relativeTo(context: List<String>): String {
        val contextPath = context.dropLast(1)
        val commonPathElements = zip(contextPath).takeWhile { (a, b) -> a == b }.count()
        return (List(contextPath.size - commonPathElements) { ".." } + this.drop(commonPathElements)).joinToString("/")
    }

    private fun DevsiteClasslikePageNode.findAnchorableByDRI(dri: DRI): AnchorableJavadocNode? =
            (constructors + methods + entries + properties).firstOrNull { it.dri == dri }

    override fun resolve(dri: DRI, sourceSets: Set<DokkaSourceSet>, context: PageNode?): String {
        return nodeIndex[dri]?.let { resolve(it, context) }
                ?: nodeIndex[dri.parent]?.let {
                    val anchor =
                            when (val anchorElement = (it as? DevsiteClasslikePageNode)?.findAnchorableByDRI(dri)) {
                                is DevsiteFunctionNode -> anchorElement.getAnchor()
                                is DevsiteEntryNode -> anchorElement.name
                                is DevsitePropertyNode -> anchorElement.name
                                else -> anchorForDri(dri)
                            }
                    "${resolve(it, context, skipExtension = true)}.html#$anchor"
                }
                ?: getExternalLocation(dri, sourceSets)
    }

    private fun DevsiteFunctionNode.getAnchor(): String =
            "$name-${
                parameters.joinToString(",%20") {
                    when (val bound = it.typeBound) {
                        is TypeConstructor -> bound.dri.classNames.orEmpty()
                        is OtherParameter -> bound.name
                        is PrimitiveJavaType -> bound.name
                        is UnresolvedBound -> bound.name
                        else -> bound.toString()
                    }
                }
            }-"

    fun anchorForFunctionNode(node: DevsiteFunctionNode) = node.getAnchor()

    private fun anchorForDri(dri: DRI): String =
            dri.callable?.let { callable ->
                "${callable.name}-${
                    callable.params.joinToString(",%20") {
                        ((it as? Nullable)?.wrapped ?: it).toString()
                    }
                }-"
            } ?: dri.classNames.orEmpty()

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String =
            pathIndex[node]?.relativeTo(pathIndex[context].orEmpty())?.let {
                if (skipExtension) it.removeSuffix(".html") else it
            } ?: run {
                throw IllegalStateException("Path for ${node::class.java.canonicalName}:${node.name} not found")
            }

    fun resolve(link: LinkDevsiteListEntry, contextRoot: PageNode? = null, skipExtension: Boolean = true) =
            pathIndex[link.dri.first()]?.let {
                when (link.kind) {
                    DevsiteContentKind.Class -> it
                    DevsiteContentKind.OverviewSummary -> it.dropLast(1) + "index"
                    DevsiteContentKind.PackageSummary -> it.dropLast(1) + "package-summary"
                    DevsiteContentKind.AllClasses -> it.dropLast(1) + "allclasses"
                    DevsiteContentKind.OverviewTree -> it.dropLast(1) + "overview-tree"
                    DevsiteContentKind.PackageTree -> it.dropLast(1) + "package-tree"
                    else -> it
                }
            }?.relativeTo(pathIndex[contextRoot].orEmpty())?.let { if (skipExtension) "$it.html" else it }.orEmpty()

    override fun resolveRoot(node: PageNode): String {
        TODO("Not yet implemented")
    }

    override fun ancestors(node: PageNode): List<PageNode> {
        TODO("Not yet implemented")
    }
}
