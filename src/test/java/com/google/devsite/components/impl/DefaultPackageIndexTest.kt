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
import com.google.devsite.components.pages.PackageIndex.Params
import com.google.devsite.components.testing.NoopSummaryList
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultPackageIndexTest {
    @Test
    fun `Package list renders correctly`() {
        val component = DefaultPackageIndex(Params("classes.html", NoopSummaryList()))

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>These are all the API packages. See all <a href="classes.html">API classes</a>.</p>
  <div>noop</div>
</body>
            """
                    .trim(),
            )
    }
}
