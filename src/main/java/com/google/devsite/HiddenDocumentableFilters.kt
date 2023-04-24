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

import com.google.devsite.renderer.converters.annotations
import com.google.devsite.renderer.converters.asString
import com.google.devsite.renderer.converters.explodedChildren
import com.google.devsite.renderer.converters.getExpectOrCommonSourceSet
import com.google.devsite.renderer.converters.isDeprecated
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.orEmpty
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * These filters remove items from the docs when they:
 * - have @hide in a comment
 * - have @removed in a comment
 * - have a @RestrictTo annotation
 * - are deprecated with DeprecationLevel.HIDDEN
 *
 * There are two filters, one runs before the dokka merge step and one runs after.
 * The post-merge filter looks at packages, and the pre-merge filter looks at everything else.
 * This is because Java and Kotlin files are split into different DPackages until the merge step.
 * Packages use @RestrictTo or @hide in package-info.java file if everything in the package should
 * be excluded from the docs. If this check ran pre-merge, only the Java files in the package would
 * be filtered out, as the Kotlin files are in a different DPackage at that point.
 */

/**
 * Pre-merge transformer: filter hidden documentables, with the exception of DPackages.
 * The reason for leaving most filtering pre-merge is that empty packages are removed before the
 * merge, so if this filter removes everything from a package (but not the package itself), the
 * package will be excluded from the docs. If this filter ran after a merge, then the empty package
 * would remain.
 */
class PreMergeHiddenDocumentableFilter(dokkaContext: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(dokkaContext) {
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val hide = d !is DPackage && d.isHidden()
        if (hide) addToHiddenSet(d)
        return hide
    }
}

/**
 * Post-merge transformer: filter hidden packages, now that Kotlin and Java files in the same
 * package have been merged into the same DPackage object.
 */
class PostMergePackageDocumentableFilter : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule {
        val filteredPackages = original.packages.filter {
            val hide = it.isHidden()
            if (hide) addToHiddenSet(it)
            !hide
        }
        return original.copy(packages = filteredPackages)
    }
}

private fun Documentable.isHidden(): Boolean =
    this.hasAnnotation(restrictToDri) || this.hasDeprecationLevelHidden() ||
        this.hasHideJavadocTag() || this.hasRemovedJavadocTag() ||
        this.hasAnnotation(gmsHideDri) || this.hasAnnotation(googleInternalDri)

private fun Documentable.hasDeprecationLevelHidden(): Boolean =
    this.annotations(getExpectOrCommonSourceSet()).any {
        it.isDeprecated() && ("DeprecationLevel.HIDDEN" in it.params["level"].asString())
    }

private fun Documentable.hasHideJavadocTag(): Boolean =
    this.documentation.any {
        (_, docs) ->
        docs.dfs { it is CustomTagWrapper && it.name.trim() == "hide" } != null
    }

private fun Documentable.hasRemovedJavadocTag(): Boolean =
    this.documentation.any {
        (_, docs) ->
        docs.dfs { it is CustomTagWrapper && it.name.trim() == "removed" } != null
    }

private fun Documentable.hasAnnotation(annotationDri: DRI): Boolean =
    annotationDri in this.directAnnotations || annotationDri in this.fileLevelAnnotations

private operator fun SourceSetDependent<List<Annotations.Annotation>>.contains(dri: DRI) =
    any { (_, annotations) -> annotations.any { it.dri == dri } }

private val Documentable.directAnnotations
    get() = annotations?.directAnnotations.orEmpty()

private val Documentable.fileLevelAnnotations
    get() = annotations?.fileLevelAnnotations.orEmpty()

private val Documentable.annotations
    // using .annotations() directly on the WithExtraProperties only returns the direct annotations
    get() = this.safeAs<WithExtraProperties<Documentable>>()
        ?.extra
        ?.get(Annotations)

private val restrictToDri = DRI(packageName = "androidx.annotation", classNames = "RestrictTo")
private val gmsHideDri =
    DRI(packageName = "com.google.android.gms.common.internal", classNames = "Hide")
private val googleInternalDri =
    DRI(packageName = "com.google.errorprone.annotations", classNames = "GoogleInternal")

fun hasBeenHidden(dri: DRI): Boolean {
    return hiddenDocumentables.contains(dri)
}
private fun addToHiddenSet(d: Documentable) {
    (d.explodedChildren + d).forEach { hiddenDocumentables.add(it.dri) }
}

private val hiddenDocumentables = mutableSetOf<DRI>()
