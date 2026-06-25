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

import com.google.devsite.renderer.DocumentablesWrapper
import com.google.devsite.renderer.MultiLanguageRenderer
import com.google.devsite.transformers.ComposeTransformer
import com.google.devsite.transformers.DocTagsForCheckedExceptionsTransformer
import com.google.devsite.transformers.PropagatedAnnotationsTransformer
import com.google.devsite.transformers.ResourceClassTransformer
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

class DevsitePlugin : DokkaPlugin() {

    private val dokkaBase by lazy { plugin<DokkaBase>() }
    internal val analysisPlugin by lazy { plugin<KotlinAnalysisPlugin>() }

    /**
     * "All of Dokka's plugin API is in preview and it can be changed in a backwards-incompatible
     * manner with a best-effort migration. By opting in, you (we) acknowledge the risks of relying
     * on preview API."
     */
    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement

    val translator by extending {
        CoreExtensions.documentableToPageTranslator providing
            {
                DocumentablesWrapper()
            } override
            dokkaBase.documentableToPageTranslator
    }

    val renderer by extending {
        CoreExtensions.renderer providing
            {
                MultiLanguageRenderer(
                    it,
                    dokkaBase.querySingle { outputWriter },
                    getDevsiteConfiguration(it),
                    analysisPlugin,
                )
            } override
            dokkaBase.htmlRenderer
    }

    val docTagsForCheckedExceptions by extending {
        CoreExtensions.documentableTransformer providing
            {
                DocTagsForCheckedExceptionsTransformer(getDevsiteConfiguration(it).tracer)
            }
    }

    val propagateAnnotations by extending {
        CoreExtensions.documentableTransformer providing
            {
                val devsiteConfiguration = getDevsiteConfiguration(it)
                PropagatedAnnotationsTransformer(
                    devsiteConfiguration.propagatingAnnotations,
                    devsiteConfiguration.tracer,
                )
            }
    }

    val composeTransformer by extending {
        CoreExtensions.documentableTransformer providing
            {
                ComposeTransformer(getDevsiteConfiguration(it).tracer)
            } order
            {
                after(docTagsForCheckedExceptions)
                after(propagateAnnotations)
            }
    }

    val resourceClassTransformer by extending {
        dokkaBase.preMergeDocumentableTransformer providing
            {
                ResourceClassTransformer(getDevsiteConfiguration(it).tracer)
            } order
            {
                // If an R class is removed and a package becomes empty, it should be filtered out.
                before(dokkaBase.emptyPackagesFilter)
            }
    }

    val privateAnnotationFilter by extending {
        dokkaBase.preMergeDocumentableTransformer providing
            {
                PreMergePrivateAnnotationRecorder(getDevsiteConfiguration(it).tracer)
            } order
            {
                before(dokkaBase.documentableVisibilityFilter)
            }
    }

    val preMergeHiddenFilter by extending {
        dokkaBase.preMergeDocumentableTransformer providing
            {
                val devsiteConfiguration = getDevsiteConfiguration(it)
                PreMergeHiddenDocumentableFilter(
                    it,
                    devsiteConfiguration.hidingAnnotations,
                    devsiteConfiguration.tracer,
                )
            } order
            {
                before(dokkaBase.emptyPackagesFilter)
            }
    }

    val hiddenPackageFilter by extending {
        CoreExtensions.documentableTransformer providing
            {
                PostMergePackageDocumentableFilter(getDevsiteConfiguration(it).tracer)
            }
    }
    // Override the upstream filtering out of methods inherited from mapped types
    // https://github.com/Kotlin/dokka/issues/3542
    val jvmMappedMethodsFilter by extending {
        dokkaBase.preMergeDocumentableTransformer with
            NoopTransformer override
            dokkaBase.jvmMappedMethodsFilter
    }

    // Cleans up the trace driver after all rendering is complete.
    val closeTraceDriver by extending {
        CoreExtensions.postActions providing
            {
                PostAction { getDevsiteConfiguration(it).traceDriver.close() }
            }
    }

    /** Dackka configuration values. Should be accessed through [getDevsiteConfiguration]. */
    private lateinit var devsiteConfiguration: DevsiteConfiguration

    /** Returns [devsiteConfiguration], initializing it based on the [dokkaContext] if needed. */
    private fun getDevsiteConfiguration(dokkaContext: DokkaContext): DevsiteConfiguration {
        // Only load the [DevsiteConfiguration] through [loadDevsiteConfiguration] once, because
        // otherwise the JSON would be reparsed each time.
        if (!::devsiteConfiguration.isInitialized) {
            devsiteConfiguration = loadDevsiteConfiguration(dokkaContext)
        }
        return devsiteConfiguration
    }

    private object NoopTransformer : PreMergeDocumentableTransformer {
        override fun invoke(modules: List<DModule>): List<DModule> = modules
    }
}

internal fun loadDevsiteConfiguration(dokkaContext: DokkaContext): DevsiteConfiguration {
    return checkNotNull(configuration<DevsitePlugin, DevsiteConfiguration>(dokkaContext)) {
        "Missing Dackka plugin configuration. See go/dackka#generating-docs for more detail."
    }
}
