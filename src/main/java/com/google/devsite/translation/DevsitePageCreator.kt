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

package com.google.devsite.translation

import com.google.devsite.pages.DevsiteClasslikePageNode
import com.google.devsite.pages.DevsiteContentGroup
import com.google.devsite.pages.DevsiteContentKind
import com.google.devsite.pages.DevsiteContentNode
import com.google.devsite.pages.DevsiteEntryNode
import com.google.devsite.pages.DevsiteFunctionNode
import com.google.devsite.pages.DevsiteModulePageNode
import com.google.devsite.pages.DevsitePackagePageNode
import com.google.devsite.pages.DevsiteParameterNode
import com.google.devsite.pages.DevsitePropertyNode
import com.google.devsite.pages.DevsiteSignatureContentNode
import com.google.devsite.pages.JavadocList
import com.google.devsite.pages.LinkDevsiteListEntry
import com.google.devsite.pages.RowDevsiteListEntry
import com.google.devsite.pages.leafList
import com.google.devsite.pages.rootList
import com.google.devsite.pages.title
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithConstructors
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.model.firstChildOfTypeOrNull
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.ContentGroup
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentText
import org.jetbrains.dokka.pages.DCI
import kotlin.reflect.KClass

open class DevsitePageCreator(
        private val signatureProvider: SignatureProvider
) {

    fun pageForModule(m: DModule): DevsiteModulePageNode =
            DevsiteModulePageNode(
                    name = m.name.ifEmpty { "root" },
                    content = contentForModule(m),
                    children = m.packages.map { pageForPackage(it) },
                    dri = setOf(m.dri)
            )

    fun pageForPackage(p: DPackage) =
            DevsitePackagePageNode(p.name, contentForPackage(p), setOf(p.dri), p,
                    p.classlikes.mapNotNull { pageForClasslike(it) } // TODO: nested classlikes
            )

    fun pageForClasslike(c: DClasslike): DevsiteClasslikePageNode? =
            c.highestJvmSourceSet?.let { jvm ->
                DevsiteClasslikePageNode(
                        name = c.name.orEmpty(),
                        content = contentForClasslike(c),
                        dri = setOf(c.dri),
                        signature = signatureForNode(c, jvm),
                        description = c.descriptionToContentNodes(),
                        constructors = (c as? WithConstructors)?.constructors?.mapNotNull { it.toJavadocFunction() }
                                .orEmpty(),
                        methods = c.functions.mapNotNull { it.toJavadocFunction() },
                        entries = (c as? DEnum)?.entries?.map {
                            DevsiteEntryNode(
                                    it.dri,
                                    it.name,
                                    signatureForNode(it, jvm),
                                    it.descriptionToContentNodes(jvm)
                            )
                        }.orEmpty(),
                        classlikes = c.classlikes.mapNotNull { pageForClasslike(it) },
                        properties = c.properties.map {
                            DevsitePropertyNode(
                                    it.dri,
                                    it.name,
                                    signatureForNode(it, jvm),
                                    it.descriptionToContentNodes(jvm)
                            )
                        },
                        documentable = c,
                        extras = (c as? WithExtraProperties<Documentable>)?.extra ?: PropertyContainer.empty()
                )
            }

    private fun contentForModule(m: DModule): DevsiteContentNode =
            DevsiteContentGroup(
                    setOf(m.dri),
                    DevsiteContentKind.OverviewSummary,
                    m.jvmSourceSets.toSet()
            ) {
                title(m.name, m.brief(), "0.0.1", dri = setOf(m.dri), kind = ContentKind.Main)
                leafList(setOf(m.dri),
                        ContentKind.Packages, JavadocList(
                        "Packages", "Package",
                        m.packages.sortedBy { it.name }.map { p ->
                            RowDevsiteListEntry(
                                    LinkDevsiteListEntry(
                                            p.name,
                                            setOf(p.dri),
                                            DevsiteContentKind.PackageSummary,
                                            sourceSets
                                    ),
                                    p.brief()
                            )
                        }
                ))
            }

    private fun contentForPackage(p: DPackage): DevsiteContentNode =
            DevsiteContentGroup(
                    setOf(p.dri),
                    DevsiteContentKind.PackageSummary,
                    p.jvmSourceSets.toSet()
            ) {
                title(p.name, p.brief(), "0.0.1", dri = setOf(p.dri), kind = ContentKind.Packages)
                val rootList = p.classlikes.groupBy { it::class }.map { (key, value) ->
                    JavadocList(key.tabTitle, key.colTitle, value.map { c ->
                        RowDevsiteListEntry(
                                LinkDevsiteListEntry(c.name ?: "", setOf(c.dri), DevsiteContentKind.Class, sourceSets),
                                c.brief()
                        )
                    })
                }
                rootList(setOf(p.dri), DevsiteContentKind.Class, rootList)
            }

    private val KClass<out DClasslike>.colTitle: String
        get() = when (this) {
            DClass::class -> "Class"
            DObject::class -> "Object"
            DAnnotation::class -> "Annotation"
            DEnum::class -> "Enum"
            DInterface::class -> "Interface"
            else -> ""
        }

    private val KClass<out DClasslike>.tabTitle: String
        get() = colTitle + if (colTitle.last() != 's') "s" else "es"

    private fun contentForClasslike(c: DClasslike): DevsiteContentNode =
            DevsiteContentGroup(
                    setOf(c.dri),
                    DevsiteContentKind.Class,
                    c.jvmSourceSets.toSet()
            ) {
                title(
                        c.name.orEmpty(),
                        c.brief(),
                        "0.0.1",
                        parent = c.dri.packageName,
                        dri = setOf(c.dri),
                        kind = DevsiteContentKind.Class
                )
            }

    private fun DFunction.toJavadocFunction() = highestJvmSourceSet?.let { jvm ->
        DevsiteFunctionNode(
                name = name,
                dri = dri,
                signature = signatureForNode(this, jvm),
                brief = brief(jvm),
                parameters = parameters.mapNotNull {
                    val signature = signatureForNode(it, jvm)
                    signature.modifiers?.let { type ->
                        DevsiteParameterNode(
                                name = it.name.orEmpty(),
                                type = type,
                                description = it.brief(),
                                typeBound = it.type,
                                dri = it.dri
                        )
                    }
                },
                extras = extra
        )
    }

    private val Documentable.jvmSourceSets
        get() = sourceSets.filter { it.analysisPlatform == Platform.jvm }

    private val Documentable.highestJvmSourceSet
        get() = jvmSourceSets.let { sources ->
            sources.firstOrNull { it != expectPresentInSet } ?: sources.firstOrNull()
        }

    private val firstSentenceRegex = Regex("^((?:[^.?!]|[.!?](?!\\s))*[.!?])")

    private inline fun <reified T : TagWrapper> Documentable.findNodeInDocumentation(sourceSetData: DokkaSourceSet?): T? =
            documentation[sourceSetData]?.firstChildOfTypeOrNull<T>()

    private fun Documentable.descriptionToContentNodes(sourceSet: DokkaSourceSet? = highestJvmSourceSet) =
            contentNodesFromType<Description>(sourceSet)

    private fun DParameter.paramsToContentNodes(sourceSet: DokkaSourceSet? = highestJvmSourceSet) =
            contentNodesFromType<Param>(sourceSet)

    private inline fun <reified T : TagWrapper> Documentable.contentNodesFromType(sourceSet: DokkaSourceSet?) =
            findNodeInDocumentation<T>(sourceSet)?.let {
                DocTagToContentConverter.buildContent(
                        it.root,
                        DCI(setOf(dri), DevsiteContentKind.OverviewSummary),
                        sourceSets.toSet()
                )
            }.orEmpty()

    fun List<ContentNode>.nodeForJvm(jvm: DokkaSourceSet): ContentNode =
            first { it.sourceSets.contains(jvm) }

    private fun Documentable.brief(sourceSet: DokkaSourceSet? = highestJvmSourceSet): List<ContentNode> =
            briefFromContentNodes(descriptionToContentNodes(sourceSet))

    private fun briefFromContentNodes(description: List<ContentNode>): List<ContentNode> {
        val contents = mutableListOf<ContentNode>()
        for (node in description) {
            if (node is ContentText && firstSentenceRegex.containsMatchIn(node.text)) {
                contents.add(node.copy(text = firstSentenceRegex.find(node.text)?.value.orEmpty()))
                break
            } else {
                contents.add(node)
            }
        }
        return contents
    }

    private fun DParameter.brief(sourceSet: DokkaSourceSet? = highestJvmSourceSet): List<ContentNode> =
            briefFromContentNodes(paramsToContentNodes(sourceSet).dropWhile { it is ContentDRILink })

    private fun ContentNode.asJavadocNode(): DevsiteSignatureContentNode =
            (this as ContentGroup).firstChildOfTypeOrNull<DevsiteSignatureContentNode>()
                    ?: throw IllegalStateException("No content for javadoc signature found")

    private fun signatureForNode(documentable: Documentable, sourceSet: DokkaSourceSet): DevsiteSignatureContentNode =
            signatureProvider.signature(documentable).nodeForJvm(sourceSet).asJavadocNode()
}
