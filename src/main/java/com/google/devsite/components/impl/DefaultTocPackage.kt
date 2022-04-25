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

import com.google.devsite.components.symbols.TocPackage

/** Default implementation of the toc. */
internal class DefaultTocPackage(
    override val data: TocPackage.Params
) : TocPackage {
    override fun render(into: StringBuilder) = into.run {
        appendLine("- title: \"${data.name}\"")
        appendLine("  path: \"${data.packageUrl}\"")
        appendLine()

        val content = listOf(
            data.interfaces,
            data.classes,
            data.enums,
            data.exceptions,
            data.annotations,
            data.typeAliases,
            data.objects
        ).flatten()
        if (content.isEmpty()) return

        appendLine("  section:")

        renderTypes("Interfaces", data.interfaces)
        renderTypes("Classes", data.classes)
        renderTypes("Enums", data.enums)
        renderTypes("Exceptions", data.exceptions)
        renderTypes("Annotations", data.annotations)
        renderTypes("Type aliases", data.typeAliases)
        renderTypes("Objects", data.objects)
    }

    private fun StringBuilder.renderTypes(sectionName: String, types: List<TocPackage.Type>) {
        if (types.isEmpty()) return

        appendLine("  - title: \"$sectionName\"")
        appendLine()
        appendLine("    section:")

        for (type in types) {
            renderType(type)
        }

        appendLine()
    }

    private fun StringBuilder.renderType(type: TocPackage.Type) {
        appendLine("    - title: \"${type.name}\"")
        appendLine("      path: \"${type.url}\"")
    }

    override fun toString() = "Table of Contents for package ${data.name} at ${data.packageUrl}. " +
        "Interfaces: ${data.interfaces}, Classes: ${data.classes}, Enums: ${data.enums}, " +
        "Exceptions: ${data.exceptions}, Annotations: ${data.annotations}, " +
        "TypeAliases: ${data.typeAliases}, Objects: ${data.objects}."
}
