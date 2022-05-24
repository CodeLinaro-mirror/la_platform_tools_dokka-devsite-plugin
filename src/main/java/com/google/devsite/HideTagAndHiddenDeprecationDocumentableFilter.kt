/*
 * Copyright 2021 The Android Open Source Project
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

import com.google.devsite.renderer.converters.annotations
import com.google.devsite.renderer.converters.asString
import com.google.devsite.renderer.converters.isDeprecated
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.plugability.DokkaContext

class HideTagAndHiddenDeprecationDocumentableFilter(dokkaContext: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(dokkaContext) {
    override fun shouldBeSuppressed(d: Documentable): Boolean =
        d.documentation.any {
            (_, docs) ->
            docs.dfs { it is CustomTagWrapper && it.name.trim() == "hide" } != null
        } ||
            d.annotations().any {
                it.isDeprecated() && ("DeprecationLevel.HIDDEN" in it.params["level"].asString())
            }
}
