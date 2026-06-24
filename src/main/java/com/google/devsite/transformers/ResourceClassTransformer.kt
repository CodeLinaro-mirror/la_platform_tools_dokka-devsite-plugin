/*
 * Copyright 2026 The Android Open Source Project
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

package com.google.devsite.transformers

import androidx.tracing.Tracer
import com.google.devsite.util.trace
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer

/** Filters empty R resource classes. */
class ResourceClassTransformer(private val tracer: Tracer) : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule {
        return tracer.trace("ResourceClassTransformer") {
            original.copy(packages = original.packages.map { transform(it) })
        }
    }

    private fun transform(dPackage: DPackage): DPackage {
        return dPackage.copy(classlikes = dPackage.classlikes.mapNotNull { transform(it) })
    }

    /**
     * If [dClasslike] is not an R class, returns the same [dClasslike].
     *
     * If [dClasslike] is an R class, filters its subclasses to only those with public resources. If
     * there are no remaining subclasses, returns null. Otherwise, returns the class with the
     * filtered set of subclasses.
     */
    private fun transform(dClasslike: DClasslike): DClasslike? {
        if (dClasslike !is DClass || dClasslike.name != "R") return dClasslike

        val subclasses = dClasslike.classlikes.filter { it.properties.isNotEmpty() }
        return if (subclasses.isEmpty()) {
            null
        } else {
            dClasslike.copy(classlikes = subclasses)
        }
    }
}
