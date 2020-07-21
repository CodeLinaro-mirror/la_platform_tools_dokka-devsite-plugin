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

package com.google.devsite.renderer

import com.google.devsite.pages.AllClassesPageInstaller
import com.google.devsite.pages.MathjaxTransformer
import kotlinx.html.FlowContent
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.stream.createHTML
import kotlinx.html.title
import kotlinx.html.unsafe
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.pages.ContentEmbeddedResource
import org.jetbrains.dokka.pages.ContentHeader
import org.jetbrains.dokka.pages.ContentList
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.ContentTable
import org.jetbrains.dokka.pages.ContentText
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.plugability.DokkaContext

class DevsiteRenderer(
    context: DokkaContext
) : DefaultRenderer<FlowContent>(context) {
    override val preprocessors = listOf(
            MathjaxTransformer,
            AllClassesPageInstaller
    )

    override fun buildError(node: ContentNode) {
    }

    override fun buildPage(
        page: ContentPage,
        content: (FlowContent, ContentPage) -> Unit
    ): String = buildHtml(page, page.embeddedResources) {
        div {
            id = "content"
            attributes["pageIds"] = page.dri.first().toString()
            content(this, page)
        }
    }

    private fun buildHtml(page: PageNode, resources: List<String>/*TODO*/, content: FlowContent.() -> Unit): String {
        return createHTML().html {
            attributes["devsite"] = "true"
            head {
                title {
                    +page.name
                }
                unsafe { +"{% setvar book_path %}/reference/kotlin/androidx/_book.yaml{% endsetvar %}\n{% include \"_shared/_reference-head-tags.html\" %}\n" }
            }
            body {
                content()
            }
        }
    }

    override fun FlowContent.buildHeader(level: Int, node: ContentHeader, content: FlowContent.() -> Unit) {
    }

    override fun FlowContent.buildLink(address: String, content: FlowContent.() -> Unit) {
    }

    override fun FlowContent.buildList(
        node: ContentList,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaConfiguration.DokkaSourceSet>?
    ) {
    }

    override fun FlowContent.buildNavigation(page: PageNode) {
    }

    override fun FlowContent.buildNewLine() {
    }

    override fun FlowContent.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage) {
    }

    override fun FlowContent.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaConfiguration.DokkaSourceSet>?
    ) {
    }

    override fun FlowContent.buildText(textNode: ContentText) {
    }
}
