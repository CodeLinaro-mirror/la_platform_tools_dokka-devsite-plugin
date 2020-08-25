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

package com.google.devsite.renderer.converters

import com.google.devsite.components.impl.DefaultDescription
import com.google.devsite.components.impl.UndocumentedSymbolDescription
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.Deprecated
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.TagWrapper
import com.google.devsite.components.Description as DescriptionComponent

/** Extracts the hand written documentation from documentables into the correct components. */
internal class DocTagConverter(
    private val language: Language,
    private val pathProvider: FilePathProvider
) {
    /** @return the hand-written javadoc */
    fun summaryDescription(doc: Documentable): DescriptionComponent {
        val allTags = doc.tags()

        val deprecation = allTags.filterIsInstance<Deprecated>().strictSingleOrNull()
        return if (deprecation == null) {
            val description = allTags.filterIsInstance<Description>().strictSingleOrNull()
            if (description == null) {
                UndocumentedSymbolDescription()
            } else {
                description(description, summary = true)
            }
        } else {
            description(deprecation, summary = true, deprecation = doc.deprecationText())
        }
    }

    private fun description(
        tag: TagWrapper,
        summary: Boolean = false,
        deprecation: String? = null
    ): DescriptionComponent {
        return DefaultDescription(DescriptionComponent.Params(tag.root, summary, deprecation))
    }

    private fun Documentable.deprecationText() =
        "This ${deprecationStringForType(this)} is deprecated."

    private fun deprecationStringForType(doc: Documentable): String = when (doc) {
        is DClass -> "class"
        is DInterface -> "interface"
        is DEnum -> "enum"
        is DAnnotation -> "annotation"
        is DFunction -> when (language) {
            Language.JAVA -> "method"
            Language.KOTLIN -> "function"
        }
        is DProperty -> when (language) {
            Language.JAVA -> "field"
            Language.KOTLIN -> "property"
        }
        else -> error("Unsupported deprecated type: $doc")
    }

    /**
     * @return the doc tags (aka human-written javadoc or kdoc) associated with this documentable
     */
    private fun Documentable.tags() = documentation.values.singleOrNull()?.children.orEmpty()

    /** Like singleOrNull, but requires that only one element be present if any. */
    private fun <T> List<T>.strictSingleOrNull() = if (isEmpty()) {
        null
    } else {
        single()
    }
}
