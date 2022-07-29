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

package com.google.devsite.renderer.impl.paths

import com.google.devsite.components.impl.DefaultTypeProjectionComponent
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Nullability
import com.google.devsite.renderer.impl.ClassGraph
import com.google.devsite.renderer.impl.DocumentablesGraph

/** Base file paths for DAC Kotlin consumption. */
internal abstract class DacKotlinFilePathProviderBase(
    tenant: String,
    pathPrefix: String? = null,
    locationProvider: ExternalDokkaLocationProvider? = null,
    classGraph: ClassGraph,
    documentablesGraph: DocumentablesGraph
) : DacFilePathProviderBase(
    tenant = tenant,
    pathPrefix = pathPrefix,
    locationProvider = locationProvider,
    classGraph = classGraph,
    documentablesGraph = documentablesGraph
) {
    override val ANY = DefaultTypeProjectionComponent(
        TypeProjectionComponent.Params(
            type = this.linkForReference(ANY_DRI[Language.KOTLIN]!!),
            nullability = Nullability.KOTLIN_DEFAULT,
            displayLanguage = Language.KOTLIN
        )
    )
}
