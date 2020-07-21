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

import com.intellij.psi.PsiClass
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.DescriptorDocumentableSource
import org.jetbrains.dokka.analysis.PsiDocumentableSource
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Bound
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithExpectActual
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.resolve.DescriptorUtils.getClassDescriptorForType

interface DevsitePageNode : ContentPage

class DevsiteModulePageNode(
    override val name: String,
    override val content: DevsiteContentNode,
    override val children: List<PageNode>,
    override val dri: Set<DRI>
) :
    RootPageNode(),
    DevsitePageNode {

    override val documentable: Documentable? = null
    override val embeddedResources: List<String> = emptyList()
    override fun modified(name: String, children: List<PageNode>): RootPageNode =
        DevsiteModulePageNode(name, content, children, dri)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage = DevsiteModulePageNode(name, content as DevsiteContentNode, children, dri)
}

class DevsitePackagePageNode(
    override val name: String,
    override val content: DevsiteContentNode,
    override val dri: Set<DRI>,

    override val documentable: Documentable? = null,
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : DevsitePageNode {

    override fun modified(
        name: String,
        children: List<PageNode>
    ): PageNode = DevsitePackagePageNode(
        name,
        content,
        dri,
        documentable,
        children,
        embeddedResources
    )

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage =
        DevsitePackagePageNode(
            name,
            content as DevsiteContentNode,
            dri,
            documentable,
            children,
            embeddedResources
        )
}

sealed class AnchorableJavadocNode(open val dri: DRI)

data class DevsiteEntryNode(
    override val dri: DRI,
    val name: String,
    val signature: DevsiteSignatureContentNode,
    val brief: List<ContentNode>
) : AnchorableJavadocNode(dri)

data class DevsiteParameterNode(
    override val dri: DRI,
    val name: String,
    val type: ContentNode,
    val description: List<ContentNode>,
    val typeBound: Bound
) : AnchorableJavadocNode(dri)

data class DevsitePropertyNode(
    override val dri: DRI,
    val name: String,
    val signature: DevsiteSignatureContentNode,
    val brief: List<ContentNode>
) : AnchorableJavadocNode(dri)

data class DevsiteFunctionNode(
    val signature: DevsiteSignatureContentNode,
    val brief: List<ContentNode>,
    val parameters: List<DevsiteParameterNode>,
    val name: String,
    override val dri: DRI,
    val extras: PropertyContainer<DFunction> = PropertyContainer.empty()
) : AnchorableJavadocNode(dri)

class DevsiteClasslikePageNode(
    override val name: String,
    override val content: DevsiteContentNode,
    override val dri: Set<DRI>,
    val signature: DevsiteSignatureContentNode,
    val description: List<ContentNode>,
    val constructors: List<DevsiteFunctionNode>,
    val methods: List<DevsiteFunctionNode>,
    val entries: List<DevsiteEntryNode>,
    val classlikes: List<DevsiteClasslikePageNode>,
    val properties: List<DevsitePropertyNode>,
    override val documentable: Documentable? = null,
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf(),
    val extras: PropertyContainer<Documentable>
) : DevsitePageNode {

    val kind: String? = documentable?.kind()
    val packageName = dri.first().packageName

    override fun modified(
        name: String,
        children: List<PageNode>
    ): PageNode = DevsiteClasslikePageNode(
        name,
        content,
        dri,
        signature,
        description,
        constructors,
        methods,
        entries,
        classlikes,
        properties,
        documentable,
        children,
        embeddedResources,
        extras
    )

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage =
        DevsiteClasslikePageNode(
            name,
            content as DevsiteContentNode,
            dri,
            signature,
            description,
            constructors,
            methods,
            entries,
            classlikes,
            properties,
            documentable,
            children,
            embeddedResources,
            extras
        )
}

class AllClassesPage(val classes: List<DevsiteClasslikePageNode>) : DevsitePageNode {
    val classEntries =
        classes.map { LinkDevsiteListEntry(it.name, it.dri, ContentKind.Classlikes, it.sourceSets().toSet()) }

    override val name: String = "All Classes"
    override val dri: Set<DRI> = setOf(DRI.topLevel)

    override val documentable: Documentable? = null
    override val embeddedResources: List<String> = emptyList()

    override val content: ContentNode =
        EmptyNode(
            DRI.topLevel,
            ContentKind.Classlikes,
            classes.flatMap { it.sourceSets() }.toSet()
        )

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage = TODO()

    override fun modified(name: String, children: List<PageNode>): PageNode =
        TODO()

    override val children: List<PageNode> = emptyList()
}

class TreeViewPage(
    override val name: String,
    val packages: List<DevsitePackagePageNode>?,
    val classes: List<DevsiteClasslikePageNode>?,
    override val dri: Set<DRI>,
    override val documentable: Documentable?,
    val root: PageNode
) : DevsitePageNode {
    init {
        assert(packages == null || classes == null)
        assert(packages != null || classes != null)
    }

    private val documentables = root.children.filterIsInstance<ContentPage>().flatMap { node ->
        getDocumentableEntries(node)
    }.groupBy({ it.first }) { it.second }.map { (l, r) -> l to r.first() }.toMap()

    private val descriptorMap = getDescriptorMap()
    private val inheritanceTuple = generateInheritanceTree()
    internal val classGraph = inheritanceTuple.first
    internal val interfaceGraph = inheritanceTuple.second

    override val children: List<PageNode> = emptyList()

    val title = when (documentable) {
        is DPackage -> "$name Class Hierarchy"
        else -> "All packages"
    }

    val kind = when (documentable) {
        is DPackage -> "package"
        else -> "main"
    }

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage =
        TreeViewPage(
            name,
            packages = children.filterIsInstance<DevsitePackagePageNode>().takeIf { it.isNotEmpty() },
            classes = children.filterIsInstance<DevsiteClasslikePageNode>().takeIf { it.isNotEmpty() },
            dri = dri,
            documentable = documentable,
            root = root
        )

    override fun modified(name: String, children: List<PageNode>): PageNode =
        TreeViewPage(
            name,
            packages = children.filterIsInstance<DevsitePackagePageNode>().takeIf { it.isNotEmpty() },
            classes = children.filterIsInstance<DevsiteClasslikePageNode>().takeIf { it.isNotEmpty() },
            dri = dri,
            documentable = documentable,
            root = root
        )

    override val embeddedResources: List<String> = emptyList()

    override val content: ContentNode = EmptyNode(
        DRI.topLevel,
        ContentKind.Classlikes,
        emptySet()
    )

    private fun generateInheritanceTree(): Pair<List<InheritanceNode>, List<InheritanceNode>> {
        val mergeMap = mutableMapOf<DRI, InheritanceNode>()

        fun addToMap(info: InheritanceNode, map: MutableMap<DRI, InheritanceNode>) {
            if (map.containsKey(info.dri))
                map.computeIfPresent(info.dri) { _, info2 ->
                    info.copy(children = (info.children + info2.children).distinct())
                }!!.children.forEach { addToMap(it, map) }
            else
                map[info.dri] = info
        }

        fun collect(dri: DRI): InheritanceNode =
            InheritanceNode(
                dri,
                mergeMap[dri]?.children.orEmpty().map { collect(it.dri) },
                mergeMap[dri]?.interfaces.orEmpty(),
                mergeMap[dri]?.isInterface ?: false
            )

        fun classTreeRec(node: InheritanceNode): List<InheritanceNode> = if (node.isInterface) {
            node.children.flatMap(::classTreeRec)
        } else {
            listOf(node.copy(children = node.children.flatMap(::classTreeRec)))
        }

        fun classTree(node: InheritanceNode) = classTreeRec(node).singleOrNull()

        fun interfaceTreeRec(node: InheritanceNode): List<InheritanceNode> = if (node.isInterface) {
            listOf(node.copy(children = node.children.filter { it.isInterface }))
        } else {
            node.children.flatMap(::interfaceTreeRec)
        }

        fun interfaceTree(node: InheritanceNode) = interfaceTreeRec(node).firstOrNull() // TODO.single()

        fun gatherPsiClasses(psi: PsiClass): List<Pair<PsiClass, List<PsiClass>>> = psi.supers.toList().let { l ->
            listOf(psi to l) + l.flatMap { gatherPsiClasses(it) }
        }

        val psiInheritanceTree = documentables.flatMap { (_, v) -> (v as? WithExpectActual)?.sources?.values.orEmpty() }
            .filterIsInstance<PsiDocumentableSource>().mapNotNull { it.psi as? PsiClass }
            .flatMap(::gatherPsiClasses)
            .flatMap { entry -> entry.second.map { it to entry.first } }
            .let {
                it + it.map { it.second to null }
            }
            .groupBy({ it.first }) { it.second }
            .map { it.key to it.value.filterNotNull().distinct() }
            .map { (k, v) ->
                InheritanceNode(
                    DRI.from(k),
                    v.map { InheritanceNode(DRI.from(it)) },
                    k.supers.filter { it.isInterface }.map { DRI.from(it) },
                    k.isInterface
                )
            }

        val descriptorInheritanceTree = descriptorMap.flatMap { (_, v) ->
            v.typeConstructor.supertypes
                .map { getClassDescriptorForType(it) to v }
        }
            .let {
                it + it.map { it.second to null }
            }
            .groupBy({ it.first }) { it.second }
            .map { it.key to it.value.filterNotNull().distinct() }
            .map { (k, v) ->
                InheritanceNode(
                    DRI.from(k),
                    v.map { InheritanceNode(DRI.from(it)) },
                    k.typeConstructor.supertypes.map { getClassDescriptorForType(it) }
                        .mapNotNull { cd ->
                            cd.takeIf { it.kind == ClassKind.INTERFACE }?.let { DRI.from(it) }
                        },
                    isInterface = k.kind == ClassKind.INTERFACE
                )
            }

        descriptorInheritanceTree.forEach { addToMap(it, mergeMap) }
        psiInheritanceTree.forEach { addToMap(it, mergeMap) }

        val rootNodes = mergeMap.entries.filter {
            it.key.classNames in setOf("Any", "Object") // TODO: Probably should be matched by DRI, not just className
        }.map {
            collect(it.value.dri)
        }

        return rootNodes.let { Pair(it.mapNotNull(::classTree), it.mapNotNull(::interfaceTree)) }
    }

    private fun generateInterfaceGraph() {
        documentables.values.filterIsInstance<DInterface>()
    }

    private fun getDocumentableEntries(node: ContentPage): List<Pair<DRI, Documentable>> =
        listOfNotNull(node.documentable?.let { it.dri to it }) +
            node.children.filterIsInstance<ContentPage>().flatMap(::getDocumentableEntries)

    private fun getDescriptorMap(): Map<DRI, ClassDescriptor> {
        val map: MutableMap<DRI, ClassDescriptor> = mutableMapOf()
        documentables
            .mapNotNull { (k, v) ->
                v.descriptorForPlatform()?.let { k to it }?.also { (k, v) -> map[k] = v }
            }.map { it.second }.forEach { gatherSupertypes(it, map) }

        return map.toMap()
    }

    private fun gatherSupertypes(descriptor: ClassDescriptor, map: MutableMap<DRI, ClassDescriptor>) {
        map.putIfAbsent(DRI.from(descriptor), descriptor)
        descriptor.typeConstructor.supertypes.map { getClassDescriptorForType(it) }
            .forEach { gatherSupertypes(it, map) }
    }

    private fun Documentable?.descriptorForPlatform(platform: Platform = Platform.jvm) =
        (this as? WithExpectActual).descriptorForPlatform(platform)

    private fun WithExpectActual?.descriptorForPlatform(platform: Platform = Platform.jvm) = this?.let {
        it.sources.entries.find { it.key.analysisPlatform == platform }?.value?.let { it as? DescriptorDocumentableSource }?.descriptor as? ClassDescriptor
    }

    data class InheritanceNode(
        val dri: DRI,
        val children: List<InheritanceNode> = emptyList(),
        val interfaces: List<DRI> = emptyList(),
        val isInterface: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean = other is InheritanceNode && other.dri == dri
        override fun hashCode(): Int = dri.hashCode()
    }
}

private fun Documentable.kind(): String? =
    when (this) {
        is DClass -> "class"
        is DEnum -> "enum"
        is DAnnotation -> "annotation"
        is DObject -> "object"
        is DInterface -> "interface"
        else -> null
    }
