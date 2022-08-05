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

import com.google.devsite.components.ShouldBreak
import com.google.devsite.components.render
import com.google.devsite.components.symbols.ClassSignature
import com.google.devsite.joinMaybePrefix
import com.google.devsite.renderer.Language
import kotlinx.html.FlowContent
import kotlinx.html.pre

internal data class DefaultClassSignature(
    override val data: ClassSignature.Params
) : ClassSignature {

    override fun render(into: FlowContent) = into.run {
        pre {
            data.annotationComponents.render(into, separator = " ", terminator = { +" " })
            +(data.modifiers + data.type + data.name).joinToString(separator = " ")

            data.typeParameters.render(into, ShouldBreak.NO, brackets = "<>")

            when (data.displayLanguage) {
                Language.JAVA -> {
                    data.extends.render(into, header = { +" extends " })
                    data.implements.render(into, header = { +" $inheritancePhrase " })
                }
                Language.KOTLIN -> {
                    (data.extends + data.implements).render(into, header = { +" : " })
                }
            }
        }
    }

    override fun toString() = data.annotationComponents.joinMaybePrefix(postfix = " ") +
        (data.modifiers + data.type + data.name).joinToString(separator = " ") +
        data.typeParameters.joinMaybePrefix(prefix = "<", postfix = ">") +
        if (data.displayLanguage == Language.JAVA) {
            data.extends.joinMaybePrefix(prefix = " extends ") +
                data.implements.joinMaybePrefix(prefix = inheritancePhrase)
        } else (data.extends + data.implements).joinMaybePrefix(prefix = " : ")

    private val inheritancePhrase = if (data.type == "interface") "extends" else "implements"
}
