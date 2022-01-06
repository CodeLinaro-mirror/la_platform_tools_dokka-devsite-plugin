/*
 * Copyright 2022 The Android Open Source Project
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

import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.orEmpty
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class RestrictToDocumentableFilter(dokkaContext: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(dokkaContext) {
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        return restrictToDri in d.directAnnotations || restrictToDri in d.fileLevelAnnotations
    }

    private operator fun SourceSetDependent<List<Annotations.Annotation>>.contains(dri: DRI) =
        any { (_, annotations) -> annotations.any { it.dri == dri } }

    private val Documentable.directAnnotations
        get() = annotations?.directAnnotations.orEmpty()

    private val Documentable.fileLevelAnnotations
        get() = annotations?.fileLevelAnnotations.orEmpty()

    private val Documentable.annotations
        get() = this.safeAs<WithExtraProperties<Documentable>>()
            ?.extra
            ?.get(Annotations)

    private val restrictToDri = DRI(packageName = "androidx.annotation", classNames = "RestrictTo")
}
