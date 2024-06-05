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

import com.google.common.truth.Truth.assertThat
import com.google.devsite.components.table.TableTitle.Params
import kotlinx.html.stream.createHTML
import kotlinx.html.tr
import org.junit.Test

class DefaultTableTitleTest {
    @Test
    fun `Small header renders correctly`() {
        val component = DefaultTableTitle(Params("Title"))

        val output = createHTML().tr { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<tr>
  <th colspan="100%">Title</th>
</tr>
            """
                    .trim(),
            )
    }

    @Test
    fun `Big header renders correctly`() {
        val component = DefaultTableTitle(Params("Title", big = true))

        val output = createHTML().tr { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<tr>
  <th colspan="100%"><h3>Title</h3></th>
</tr>
            """
                    .trim(),
            )
    }
}
