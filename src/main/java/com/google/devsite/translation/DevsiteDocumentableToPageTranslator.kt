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

package com.google.devsite.translation

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator

class DevsiteDocumentableToPageTranslator(
        private val signatureProvider: SignatureProvider
) : DocumentableToPageTranslator {
    override fun invoke(module: DModule): RootPageNode =
            DevsitePageCreator(signatureProvider).pageForModule(module)
}
