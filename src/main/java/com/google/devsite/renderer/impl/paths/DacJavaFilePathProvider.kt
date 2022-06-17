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

package com.google.devsite.renderer.impl.paths

import com.google.devsite.components.impl.DefaultTypeProjectionComponent
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Nullability
import com.google.devsite.renderer.impl.ClassGraph
import com.google.devsite.renderer.impl.DocumentablesGraph
import org.jetbrains.dokka.links.DRI

/** Creates file paths for DAC Java consumption. */
internal class DacJavaFilePathProvider(
    tenant: String,
    dlp: ExternalDokkaLocationProvider? = null,
    classGraph: ClassGraph,
    documentablesGraph: DocumentablesGraph
) : DacFilePathProviderBase(
    tenant, locationProvider = dlp, classGraph = classGraph,
    documentablesGraph = documentablesGraph
) {
    init {
        ANY_LINK[Language.JAVA] = linkForReference(DRI("java.lang", "Object"))
        ANY[Language.JAVA] = DefaultTypeProjectionComponent(
            TypeProjectionComponent.Params(
                type = linkForReference(DRI("java.lang", "Object")),
                nullability = Nullability.JAVA_NOT_ANNOTATED,
                displayLanguage = Language.JAVA
            )
        )
    }
}
