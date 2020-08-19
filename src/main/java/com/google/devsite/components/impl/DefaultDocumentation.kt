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

import com.google.devsite.components.Documentation
import kotlinx.html.FlowContent
import kotlinx.html.OL
import kotlinx.html.TABLE
import kotlinx.html.TBODY
import kotlinx.html.TFOOT
import kotlinx.html.THEAD
import kotlinx.html.TR
import kotlinx.html.UL
import kotlinx.html.a
import kotlinx.html.b
import kotlinx.html.br
import kotlinx.html.code
import kotlinx.html.del
import kotlinx.html.div
import kotlinx.html.em
import kotlinx.html.h3
import kotlinx.html.h4
import kotlinx.html.h5
import kotlinx.html.h6
import kotlinx.html.hr
import kotlinx.html.li
import kotlinx.html.ol
import kotlinx.html.p
import kotlinx.html.span
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
import org.jetbrains.dokka.model.doc.A
import org.jetbrains.dokka.model.doc.Author
import org.jetbrains.dokka.model.doc.B
import org.jetbrains.dokka.model.doc.Big
import org.jetbrains.dokka.model.doc.BlockQuote
import org.jetbrains.dokka.model.doc.Br
import org.jetbrains.dokka.model.doc.Cite
import org.jetbrains.dokka.model.doc.CodeInline
import org.jetbrains.dokka.model.doc.Constructor
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Dd
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.Dfn
import org.jetbrains.dokka.model.doc.Dir
import org.jetbrains.dokka.model.doc.Div
import org.jetbrains.dokka.model.doc.Dl
import org.jetbrains.dokka.model.doc.DocTag
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
import org.jetbrains.dokka.model.doc.Property
import org.jetbrains.dokka.model.doc.Receiver
import org.jetbrains.dokka.model.doc.Script
import org.jetbrains.dokka.model.doc.Section
import org.jetbrains.dokka.model.doc.Since
import org.jetbrains.dokka.model.doc.Small
import org.jetbrains.dokka.model.doc.Span
import org.jetbrains.dokka.model.doc.Strikethrough
import org.jetbrains.dokka.model.doc.Strong
import org.jetbrains.dokka.model.doc.Sub
import org.jetbrains.dokka.model.doc.Sup
import org.jetbrains.dokka.model.doc.Suppress
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
import org.jetbrains.dokka.model.doc.Version

/** Default implementation of the hand-written documentation for a symbol. */
internal class DefaultDocumentation(
    override val data: Documentation.Params
) : Documentation {
    override fun render(html: FlowContent) = html.run {
        for (tag in data.tags) {
            when (tag) {
                is Description -> renderTags(listOf(tag.root))
                // Don't crash because it's used in the integration tests
//                is Param -> TODO("b/163811276: ${tag.javaClass.simpleName}")
//                is Return -> TODO("b/163811276: ${tag.javaClass.simpleName}")
//                is Throws -> TODO("b/163811276: ${tag.javaClass.simpleName}")
//                is Deprecated -> TODO("b/163811276: ${tag.javaClass.simpleName}")
//                is See -> TODO("b/163811276: ${tag.javaClass.simpleName}")
//                is Sample -> TODO("b/163811276: ${tag.javaClass.simpleName}")
                is Property -> TODO("b/163811276: ${tag.javaClass.simpleName}")
                is CustomTagWrapper -> TODO("b/163811276: ${tag.javaClass.simpleName}")
                is Author -> TODO("b/163811276: ${tag.javaClass.simpleName}")
                is Version -> TODO("b/163811276: ${tag.javaClass.simpleName}")
                is Since -> TODO("b/163811276: ${tag.javaClass.simpleName}")
                is Receiver -> TODO("b/163811276: ${tag.javaClass.simpleName}")
                is Constructor -> TODO("b/163811276: ${tag.javaClass.simpleName}")
                is Suppress -> TODO("b/163811276: ${tag.javaClass.simpleName}")
            }
        }
    }

    private fun FlowContent.renderTags(tags: List<DocTag>) {
        for (tag in tags) {
            when (tag) {
                is Text -> +tag.body
                is P -> p { renderTags(tag.children) }
                is A -> a(tag.params.getValue("href")) { renderTags(tag.children) }
                is B, is Strong -> b { renderTags(tag.children) }
                Br -> br { renderTags(tag.children) }
                is H3 -> h3 { renderTags(tag.children) }
                is H4 -> h4 { renderTags(tag.children) }
                is H5 -> h5 { renderTags(tag.children) }
                is H6 -> h6 { renderTags(tag.children) }
                is I, is Em -> em { renderTags(tag.children) }
                is Div -> div { renderTags(tag.children) }
                is Span -> span { renderTags(tag.children) }
                is Strikethrough -> del { renderTags(tag.children) }
                is Sub -> sub { renderTags(tag.children) }
                is Sup -> sup { renderTags(tag.children) }
                is Table -> table { renderTable(tag.children) }
                is Ol -> ol { renderOrderedList(tag.children) }
                is Ul -> ul { renderUnorderedList(tag.children) }
                HorizontalRule -> hr { renderTags(tag.children) }
                is CodeInline -> code { renderTags(tag.children) }
                // Don't crash because it's used in the integration tests
//                is CodeBlock -> TODO("b/163811276: ${tag.javaClass.simpleName}")
//                is Pre -> TODO("b/163811276: ${tag.javaClass.simpleName}")
//                is DocumentationLink -> TODO("b/163811276: ${tag.javaClass.simpleName}")
                is Img -> TODO("b/163811276: ${tag.javaClass.simpleName}")
                is BlockQuote -> TODO("b/163811276: ${tag.javaClass.simpleName}")

                is Html, is Head, is Meta, is Header, is Title, is H1, is H2, is Footer, is IFrame,
                is Main, is Menu, is Nav, is Index ->
                    throw NotImplementedError(
                        "Inline HTML pages are not supported: ${tag.javaClass.simpleName}."
                    )
                is Small, is Big, is Cite, is Dd, is Dfn, is Dir, is Font, is Frame, is FrameSet,
                is Input, is Link, is Listing, is NoFrames, is Tt, is U, is Var, is Script,
                is NoScript, is Section, is CustomDocTag, is Dl, is Dt ->
                    throw NotImplementedError("Unknown use case for ${tag.javaClass.simpleName}.")
                is THead, is TBody, is Td, is TFoot, is Th, is Tr ->
                    error("Not in table context: ${tag.javaClass.simpleName}.")
                // TODO(b/165400860): javadoc parsing is completely broken
//                is Li -> error("Not in list context: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun TABLE.renderTable(tags: List<DocTag>) {
        for (tag in tags) {
            when (tag) {
                is THead -> thead { renderTableHeader(tag.children) }
                is TBody -> tbody { renderTableBody(tag.children) }
                is TFoot -> tfoot { renderTableFooter(tag.children) }
                is Th -> tr { renderTableRow(tag.children, isHeader = true) }
                is Tr -> tr { renderTableRow(tag.children, isHeader = false) }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun THEAD.renderTableHeader(tags: List<DocTag>) {
        for (tag in tags) {
            when (tag) {
                is Tr -> tr { renderTableRow(tag.children, isHeader = true) }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun TBODY.renderTableBody(tags: List<DocTag>) {
        for (tag in tags) {
            when (tag) {
                is Tr -> tr { renderTableRow(tag.children, isHeader = false) }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun TFOOT.renderTableFooter(tags: List<DocTag>) {
        for (tag in tags) {
            when (tag) {
                is Tr -> tr { renderTableRow(tag.children, isHeader = false) }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun TR.renderTableRow(tags: List<DocTag>, isHeader: Boolean) {
        for (tag in tags) {
            when (tag) {
                is Td -> td { renderTags(tag.children) }
                is P -> if (isHeader) {
                    // KotlinX.HTML seems broken here: we can't render paragraphs or divs
                    // TODO(b/164125463): Figure out how to add other elements
                    th { +(tag.children.single() as Text).body }
                } else {
                    td { renderTags(tag.children) }
                }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun OL.renderOrderedList(tags: List<DocTag>) {
        for (tag in tags) {
            when (tag) {
                is Li -> li { renderTags(tag.children) }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun UL.renderUnorderedList(tags: List<DocTag>) {
        for (tag in tags) {
            when (tag) {
                is Li -> li { renderTags(tag.children) }
                else -> error("No other tags allowed: ${tag.javaClass.simpleName}.")
            }
        }
    }
}
