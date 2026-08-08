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

import com.google.common.truth.Truth
import com.google.devsite.components.Raw
import com.google.devsite.components.symbols.ClasslikeDescription
import com.google.devsite.components.symbols.Platform
import com.google.devsite.components.table.ClassHierarchy
import com.google.devsite.components.table.RelatedSymbols
import com.google.devsite.components.testing.NoopClasslikeSignature
import com.google.devsite.components.testing.NoopTypeProjectionComponent
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultClasslikeDescriptionTest {
    @Test
    fun `Class description renders correctly`() {
        val component =
            DefaultClasslikeDescription(
                ClasslikeDescription.Params(
                    header = DefaultDevsitePlatformSelector(listOf(Platform.COMMON)),
                    hierarchy =
                        DefaultClassHierarchy(
                            ClassHierarchy.Params(
                                parents = listOf(NoopTypeProjectionComponent("some class"))
                            )
                        ),
                    primarySignature = NoopClasslikeSignature(),
                    relatedSymbols =
                        DefaultRelatedSymbols(
                            RelatedSymbols.Params(
                                emptyList(),
                                emptySummaryList(),
                                emptyList(),
                                emptySummaryList(),
                            )
                        ),
                    descriptionDocs = listOf(DefaultRaw(Raw.Params("description description docs"))),
                )
            )

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        Truth.assertThat(output)
            .isEqualTo(
                """
<body>
  <devsite-select  id="platform" label="Select a platform"><select multiple="multiple"><option selected="selected" value="platform-Common/All">Common/All</option></select></devsite-select >
  <p>
    <pre>Signature</pre>
  </p>
  <div class="devsite-table-wrapper">
    <table class="jd-inheritance-table">
      <tbody>
        <tr>
          <td colspan="1">some class</td>
        </tr>
      </tbody>
    </table>
  </div>
  <hr>
description description docs</body>
            """
                    .trim()
            )
    }
}
