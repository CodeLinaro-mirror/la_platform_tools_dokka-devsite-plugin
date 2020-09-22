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

package com.google.devsite.components.impl

import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.renderer.Language
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.title
import kotlinx.html.unsafe

/** Default implementation of the root component for devsite. */
internal class DefaultDevsitePage(
    override val data: DevsitePage.Params
) : DevsitePage {
    override fun render(html: HTML) = html.run {
        attributes["devsite"] = "true"
        head {
            title { +data.title }
            unsafe { +"{% setvar book_path %}${data.bookPath}{% endsetvar %}\n" }
            unsafe { +"{% include \"_shared/_reference-head-tags.html\" %}\n" }
        }

        body {
            h1 { +data.title }

            unsafe { +"{% setvar page_path %}${data.path}{% endsetvar %}\n" }
            unsafe { +"{% setvar can_switch %}1{% endsetvar %}\n" }
            when (data.displayLanguage) {
                Language.JAVA -> unsafe { +"{% include \"reference/_java_switcher2.md\" %}\n" }
                Language.KOTLIN -> unsafe { +"{% include \"reference/_kotlin_switcher2.md\" %}\n" }
            }

            data.content.render(this)
        }
    }
}
