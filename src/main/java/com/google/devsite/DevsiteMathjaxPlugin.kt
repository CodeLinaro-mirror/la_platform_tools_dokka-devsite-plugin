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

package com.google.devsite

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.pages.PageTransformer

class DevsiteMathjaxPlugin : DokkaPlugin() {
    val transformer by extending {
        CoreExtensions.pageTransformer with MathjaxTransformer
    }
}

private const val ANNOTATION = "usesMathJax"
private const val MATHJAX_TAG = "<devsite-mathjax config=\"TeX-AMS_SVG\"></devsite-mathjax>"

object MathjaxTransformer : PageTransformer {
    override fun invoke(input: RootPageNode) = input.transformContentPagesTree {
        it.modified(
            embeddedResources = it.embeddedResources + if (it.isNeedingMathjax) listOf(MATHJAX_TAG) else emptyList()
        )
    }

    private val ContentPage.isNeedingMathjax
        get() = documentable?.documentation?.values
            ?.flatMap { it.children }
            .orEmpty()
            .any { (it as? CustomTagWrapper)?.name == ANNOTATION }
}