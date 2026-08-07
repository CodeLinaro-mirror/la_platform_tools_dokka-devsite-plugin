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

package com.google.devsite.util

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.documentable.ExternalDocumentableProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.plugability.querySingle

/**
 * A wrapper around an [ExternalDocumentableProvider] which locks when [getClasslike] is called.
 *
 * The default [ExternalDocumentableProvider] is not safe to use in a multithreaded context. This
 * implementation prevents errors from the Kotlin compiler.
 */
class LockingExternalDocumentableProvider(analysisPlugin: KotlinAnalysisPlugin) :
    ExternalDocumentableProvider {
    private val lock = ReentrantLock()

    private val delegateProvider = analysisPlugin.querySingle { externalDocumentableProvider }

    override fun getClasslike(dri: DRI, sourceSet: DokkaConfiguration.DokkaSourceSet): DClasslike? {
        return lock.withLock { delegateProvider.getClasslike(dri, sourceSet) }
    }
}
