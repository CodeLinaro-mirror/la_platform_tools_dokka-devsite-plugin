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
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.querySingle

class DevsitePlugin : DokkaPlugin() {
    private val dokkaBase by lazy { plugin<DokkaBase>() }

    val translator by extending {
        CoreExtensions.documentableToPageTranslator providing {
            DocumentablesWrapper()
        } override dokkaBase.documentableToPageTranslator
    }

    val renderer by extending {
        CoreExtensions.renderer providing {
            MultiLanguageRenderer(dokkaBase.querySingle { outputWriter })
        } override dokkaBase.htmlRenderer
    }
}
