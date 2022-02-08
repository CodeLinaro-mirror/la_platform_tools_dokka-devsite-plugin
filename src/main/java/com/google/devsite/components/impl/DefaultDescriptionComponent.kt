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

package com.google.devsite.components.impl

import com.google.devsite.components.DescriptionComponent
import kotlinx.html.DL
import kotlinx.html.FlowContent
import kotlinx.html.OL
import kotlinx.html.TABLE
import kotlinx.html.TBODY
import kotlinx.html.TFOOT
import kotlinx.html.THEAD
import kotlinx.html.TR
import kotlinx.html.UL
import kotlinx.html.a
import kotlinx.html.aside
import kotlinx.html.b
import kotlinx.html.blockQuote
import kotlinx.html.br
import kotlinx.html.caption
import kotlinx.html.code
import kotlinx.html.dd
import kotlinx.html.del
import kotlinx.html.div
import kotlinx.html.dl
import kotlinx.html.dt
import kotlinx.html.em
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.h4
import kotlinx.html.h5
import kotlinx.html.h6
import kotlinx.html.hr
import kotlinx.html.img
import kotlinx.html.li
import kotlinx.html.ol
import kotlinx.html.p
import kotlinx.html.pre
import kotlinx.html.span
import kotlinx.html.stream.createHTML
import kotlinx.html.strong
import kotlinx.html.sub
import kotlinx.html.sup
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tfoot
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.ul
import kotlinx.html.unsafe
import org.jetbrains.dokka.model.doc.A
import org.jetbrains.dokka.model.doc.B
import org.jetbrains.dokka.model.doc.Big
import org.jetbrains.dokka.model.doc.BlockQuote
import org.jetbrains.dokka.model.doc.Br
import org.jetbrains.dokka.model.doc.Caption
import org.jetbrains.dokka.model.doc.Cite
import org.jetbrains.dokka.model.doc.CodeBlock
import org.jetbrains.dokka.model.doc.CodeInline
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.Dd
import org.jetbrains.dokka.model.doc.Dfn
import org.jetbrains.dokka.model.doc.Dir
import org.jetbrains.dokka.model.doc.Div
import org.jetbrains.dokka.model.doc.Dl
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.Dt
import org.jetbrains.dokka.model.doc.Em
import org.jetbrains.dokka.model.doc.Font
import org.jetbrains.dokka.model.doc.Footer
import org.jetbrains.dokka.model.doc.Frame
import org.jetbrains.dokka.model.doc.FrameSet
import org.jetbrains.dokka.model.doc.H1
import org.jetbrains.dokka.model.doc.H2
import org.jetbrains.dokka.model.doc.H3
import org.jetbrains.dokka.model.doc.H4
import org.jetbrains.dokka.model.doc.H5
import org.jetbrains.dokka.model.doc.H6
import org.jetbrains.dokka.model.doc.Head
import org.jetbrains.dokka.model.doc.Header
import org.jetbrains.dokka.model.doc.HorizontalRule
import org.jetbrains.dokka.model.doc.Html
import org.jetbrains.dokka.model.doc.I
import org.jetbrains.dokka.model.doc.IFrame
import org.jetbrains.dokka.model.doc.Img
import org.jetbrains.dokka.model.doc.Index
import org.jetbrains.dokka.model.doc.Input
import org.jetbrains.dokka.model.doc.Li
import org.jetbrains.dokka.model.doc.Link
import org.jetbrains.dokka.model.doc.Listing
import org.jetbrains.dokka.model.doc.Main
import org.jetbrains.dokka.model.doc.Menu
import org.jetbrains.dokka.model.doc.Meta
import org.jetbrains.dokka.model.doc.Nav
import org.jetbrains.dokka.model.doc.NoFrames
import org.jetbrains.dokka.model.doc.NoScript
import org.jetbrains.dokka.model.doc.Ol
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Pre
import org.jetbrains.dokka.model.doc.Script
import org.jetbrains.dokka.model.doc.Section
import org.jetbrains.dokka.model.doc.Small
import org.jetbrains.dokka.model.doc.Span
import org.jetbrains.dokka.model.doc.Strikethrough
import org.jetbrains.dokka.model.doc.Strong
import org.jetbrains.dokka.model.doc.Sub
import org.jetbrains.dokka.model.doc.Sup
import org.jetbrains.dokka.model.doc.TBody
import org.jetbrains.dokka.model.doc.TFoot
import org.jetbrains.dokka.model.doc.THead
import org.jetbrains.dokka.model.doc.Table
import org.jetbrains.dokka.model.doc.Td
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.doc.Th
import org.jetbrains.dokka.model.doc.Title
import org.jetbrains.dokka.model.doc.Tr
import org.jetbrains.dokka.model.doc.Tt
import org.jetbrains.dokka.model.doc.U
import org.jetbrains.dokka.model.doc.Ul
import org.jetbrains.dokka.model.doc.Var

/** Default implementation of the hand-written documentation for a symbol. */
internal class DefaultDescriptionComponent(
    override val data: DescriptionComponent.Params
) : DescriptionComponent {
    override fun render(into: FlowContent) = into.run {
        if (data.deprecation == null) {
            if (data.summary) {
                renderTags(data.components.take(1), State())
            } else {
                renderTags(data.components, State())
            }
        } else {
            if (data.summary) {
                // Displays the deprecation message in a table cell (e.g. class summary table)
                p {
                    strong { +data.deprecation }
                    +" "
                    renderTags(data.components.take(1), State())
                }
            } else {
                // Displays the deprecation message in notice box with "caution" styling
                aside("caution") {
                    strong { +data.deprecation }
                    br()
                    renderTags(data.components, State())
                }
            }
        }
    }

    private fun FlowContent.renderTags(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            if (state.terminate) break
            val link = tag.params["href"]
            val isHtml = tag.params["content-type"] == "html"
            when (tag) {
                is Text -> if (data.summary) {
                    if (tag.body.endsWith(".") || tag.body.contains(". ")) {
                        +tag.body.replaceAfter(". ", "").trimEnd()
                        state.terminate = true
                    } else {
                        if (isHtml) {
                            consumer.onTagContentUnsafe { raw(tag.body) }
                        } else {
                            +tag.body
                        }
                    }
                } else {
                    if (tag.children.isEmpty()) {
                        if (isHtml) {
                            consumer.onTagContentUnsafe { raw(tag.body) }
                        } else {
                            +tag.body
                        }
                    } else {
                        if (link == null) {
                            renderTags(tag.children, state)
                        } else {
                            a(link) { +tag.body }
                        }
                    }
                }
                is P -> p { renderTags(tag.children, state) }
                is A -> a(link) { renderTags(tag.children, state) }
                is B, is Strong -> b { renderTags(tag.children, state) }
                Br -> br { renderTags(tag.children, state) }
                is H1 -> h1 { renderTags(tag.children, state) }
                is H2 -> h2 { renderTags(tag.children, state) }
                is H3 -> h3 { renderTags(tag.children, state) }
                is H4 -> h4 { renderTags(tag.children, state) }
                is H5 -> h5 { renderTags(tag.children, state) }
                is H6 -> h6 { renderTags(tag.children, state) }
                is I, is Em -> em { renderTags(tag.children, state) }
                is Div -> div { renderTags(tag.children, state) }
                is Dl -> dl { renderDescriptionList(tag.children, state) }
                is Span -> span { renderTags(tag.children, state) }
                is Strikethrough -> del { renderTags(tag.children, state) }
                is Sub -> sub { renderTags(tag.children, state) }
                is Sup -> sup { renderTags(tag.children, state) }
                is Table -> table { renderTable(tag.children, state) }
                is Ol -> ol { renderOrderedList(tag.children, state) }
                is Ul -> ul { renderUnorderedList(tag.children, state) }
                HorizontalRule -> hr { renderTags(tag.children, state) }
                is CodeInline -> code { renderTags(tag.children, state) }
                is Pre, is CodeBlock -> pre("prettyprint") { renderTags(tag.children, state) }
                is DocumentationLink -> code {
                    val url = data.pathProvider.forReference(tag.dri).url
                    a(url) {
                        renderTags(tag.children, state)
                    }
                }
                is Img -> img(src = tag.params.getValue("href"), alt = tag.params["alt"]) {
                    renderTags(tag.children, state)
                }
                is BlockQuote -> blockQuote { renderTags(tag.children, state) }
                is CustomDocTag -> { renderTags(tag.children, state) }
                is Html, is Head, is Meta, is Header, is Title, is Footer, is IFrame,
                is Main, is Menu, is Nav, is Index ->
                    throw NotImplementedError("Inline HTML pages are not supported: " +
                        "${tag.javaClass.simpleName}. Context: $tags.")
                is Small, is Big, is Cite, is Dfn, is Dir, is Font, is Frame, is FrameSet,
                is Input, is Link, is Listing, is NoFrames, is Tt, is U, is Var, is Script,
                is NoScript, is Section -> throw NotImplementedError("Unknown use case for " +
                    "${tag.javaClass.simpleName}.  Context: $tags.")
                is THead, is TBody, is Td, is TFoot, is Th, is Tr ->
                    error("Not in table context: ${tag.javaClass.simpleName}.  Context: $tags.")
                is Li -> error("Not in list context: ${tag.javaClass.simpleName}. The <li> tag " +
                    "must be contained in a parent element (such as <ol>, <ul>, or <menu>). " +
                    "Context: $tags.")
                is Dd, is Dt -> error("Not in list context: ${tag.javaClass.simpleName}. The <dt>" +
                    " or <dd> tag <must be contained in a <dl> element. Context: $tags.")
                is Caption -> TODO("Support this tag")
            }
        }
    }

    // TODO: remove improper handling of dt b/217941159
    private fun DL.renderDescriptionList(tags: List<DocTag>, state: State) {
        for (tag in tags) {

            when (tag) {
                is Dd -> dd { renderTags(tag.children, state) }
                is Dt -> dt {
                    unsafe { +createHTML().p { renderTags(tag.children, state) } }
                }
                is Dl -> renderTags(listOf(tag), state)
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun TABLE.renderTable(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is THead -> thead { renderTableHeader(tag.children, state) }
                is TBody -> tbody { renderTableBody(tag.children, state) }
                is TFoot -> tfoot { renderTableFooter(tag.children, state) }
                is Th -> tr { renderTableRow(tag.children, isHeader = true, state) }
                is Tr -> tr { renderTableRow(tag.children, isHeader = false, state) }
                is Caption -> caption { renderTags(tag.children, state) }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun THEAD.renderTableHeader(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is Tr -> tr { renderTableRow(tag.children, isHeader = true, state) }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun TBODY.renderTableBody(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is Tr -> tr { renderTableRow(tag.children, isHeader = false, state) }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun TFOOT.renderTableFooter(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is Tr -> tr { renderTableRow(tag.children, isHeader = false, state) }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun TR.renderTableRow(tags: List<DocTag>, isHeader: Boolean, state: State) {
        for (tag in tags) {
            when (tag) {
                is Td -> td { renderTags(tag.children, state) }
                is P -> if (isHeader) {
                    // KotlinX.HTML seems broken here: we can't render paragraphs or divs
                    // TODO(b/164125463): Figure out how to add other elements
                    th { +(tag.children.single() as Text).body }
                } else {
                    td { renderTags(tag.children, state) }
                }
                // <th> is being converted to Text class
                // TODO(b/193096057): determine root cause
                is Text -> th { +tag.body }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun OL.renderOrderedList(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is Li -> li { renderTags(tag.children, state) }
                is Ol, is Ul -> renderTags(listOf(tag), state)
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun UL.renderUnorderedList(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is Li -> li { renderTags(tag.children, state) }
                is Ol, is Ul -> renderTags(listOf(tag), state)
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    /**
     * Mutable state holder for tag rendering. We aren't using fields to preserve thread safety and
     * idempotency of the component.
     */
    private class State(var terminate: Boolean = false)
}
