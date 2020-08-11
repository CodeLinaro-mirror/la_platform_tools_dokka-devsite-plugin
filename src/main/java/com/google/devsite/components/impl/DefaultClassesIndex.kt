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

import com.google.devsite.components.ClassesIndex
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.id
import kotlinx.html.p
import kotlinx.html.unsafe

/** Default implementation of the list of classes page. */
internal class DefaultClassesIndex(
    override val data: ClassesIndex.Params
) : ClassesIndex {
    init {
        require(data.alphabetizedClasses.isNotEmpty()) {
            "This page shouldn't be rendered if there are no classes."
        }
    }

    override fun render(html: FlowContent) = html.run {
        p {
            +"These are all the API classes. See all "
            a(data.packagesUrl) {
                +"API packages"
            }
            +"."
        }

        val sortedClasses = data.alphabetizedClasses.toSortedMap()
        div("jd-letterlist") {
            for ((letter) in sortedClasses.entries) {
                a("#letter_$letter") { +letter.toString() }
                unsafe { +"&nbsp;&nbsp;" }
            }
        }

        for ((letter, summary) in sortedClasses.entries) {
            h2 {
                id = "letter_$letter"
                +letter.toString()
            }

            summary.render(this)
        }
    }
}
