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

import com.google.devsite.pages.DevsiteSignatureContentNode
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.utilities.DokkaLogger

class DevsitePageContentBuilder(
        commentsConverter: CommentsToContentConverter,
        signatureProvider: SignatureProvider,
        logger: DokkaLogger
) : PageContentBuilder(commentsConverter, signatureProvider, logger) {

    fun PageContentBuilder.DocumentableContentBuilder.javadocGroup(
            dri: DRI = mainDRI.first(),
            sourceSets: Set<DokkaConfiguration.DokkaSourceSet> = mainSourcesetData,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DevsiteContentBuilder.() -> Unit
    ) {
        +DevsiteContentBuilder(
                mainDri = dri,
                mainExtra = extra,
                mainSourceSet = sourceSets
        ).apply(block).build()
    }

    open inner class DevsiteContentBuilder(
            private val mainDri: DRI,
            private val mainExtra: PropertyContainer<ContentNode>,
            private val mainSourceSet: Set<DokkaConfiguration.DokkaSourceSet>
    ) {
        var annotations: ContentNode? = null
        var modifiers: ContentNode? = null
        var signatureWithoutModifiers: ContentNode? = null
        var supertypes: ContentNode? = null

        fun annotations(block: PageContentBuilder.DocumentableContentBuilder.() -> Unit) {
            val built = buildContentForBlock(block)
            if (built.hasAnyContent()) annotations = built
        }

        fun modifiers(block: PageContentBuilder.DocumentableContentBuilder.() -> Unit) {
            val built = buildContentForBlock(block)
            if (built.hasAnyContent()) modifiers = built
        }

        fun signatureWithoutModifiers(block: PageContentBuilder.DocumentableContentBuilder.() -> Unit) {
            signatureWithoutModifiers = buildContentForBlock(block)
        }

        fun supertypes(block: PageContentBuilder.DocumentableContentBuilder.() -> Unit) {
            val built = buildContentForBlock(block)
            if (built.hasAnyContent()) supertypes = built
        }

        private fun buildContentForBlock(block: PageContentBuilder.DocumentableContentBuilder.() -> Unit) =
                contentFor(
                        dri = mainDri,
                        sourceSets = mainSourceSet,
                        kind = ContentKind.Symbol,
                        extra = mainExtra,
                        block = block
                )

        fun build(): DevsiteSignatureContentNode = DevsiteSignatureContentNode(
                dri = mainDri,
                annotations = annotations,
                modifiers = modifiers,
                signatureWithoutModifiers = signatureWithoutModifiers
                        ?: throw IllegalStateException("DevsiteSignatureContentNode should have at least a signature"),
                supertypes = supertypes
        )
    }
}
