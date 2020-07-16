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

import com.google.devsite.location.DevsiteLocationProviderFactory
import com.google.devsite.renderer.DevsiteRenderer
import com.google.devsite.signatures.JavaSignatureProvider
import com.google.devsite.translation.DevsiteDocumentableToPageTranslator
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.kotlinAsJava.KotlinAsJavaPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.querySingle

class DevsitePlugin : DokkaPlugin() {
    val dokkaBase by lazy { plugin<DokkaBase>() }
    val kotlinAsJava by lazy { plugin<KotlinAsJavaPlugin>() }

    val locationProvider by extending {
        dokkaBase.locationProviderFactory providing {
            DevsiteLocationProviderFactory(it)
        } override dokkaBase.locationProvider
    }

    val renderer by extending {
        CoreExtensions.renderer providing {
            DevsiteRenderer(it)
        } override dokkaBase.htmlRenderer
    }

    val pageTranslator by extending {
        CoreExtensions.documentableToPageTranslator providing {
            DevsiteDocumentableToPageTranslator(dokkaBase.querySingle { signatureProvider })
        } override dokkaBase.documentableToPageTranslator
    }

    val javadocSignatureProvider by extending {
        dokkaBase.signatureProvider providing {
            JavaSignatureProvider(it.single(dokkaBase.commentsToContentConverter), it.logger)
        } override kotlinAsJava.javaSignatureProvider
    }
}
