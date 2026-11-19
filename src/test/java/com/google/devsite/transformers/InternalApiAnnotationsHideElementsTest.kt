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

package com.google.devsite.transformers

import com.google.common.truth.Truth.assertThat
import com.google.devsite.testing.createPluginsConfiguration
import com.google.devsite.testing.defaultDevsiteConfiguration
import com.google.devsite.testing.defaultPluginsConfiguration
import org.junit.Test

class InternalApiAnnotationsHideElementsTest : BaseTransformerTest() {
    override val defaultConfiguration =
        createDokkaConfiguration(
            createPluginsConfiguration(
                defaultDevsiteConfiguration.copy(internalApiAnnotationsHideElements = true)
            )
        )

    @Test
    fun `Internal API annotations do not hide when config value is false`() {
        testTransformer(
            """
            /src/com/sample/Api.kt
            package com.example
            annotation class InternalExampleApi
            @InternalExampleApi
            class Foo
            """,
            // Use a configuration where `internalApiAnnotationsHideElements` is false
            configuration = createDokkaConfiguration(defaultPluginsConfiguration),
        ) { dModule ->
            assertThat(dModule.packages[0].classlikes.singleOrNull { it.name == "Foo" }).isNotNull()
        }
    }

    @Test
    fun `Internal API annotation hides elements`() {
        testTransformer(
            """
            /src/com/sample/Api.kt
            package com.example
            annotation class InternalExampleApi
            @InternalExampleApi
            class HiddenApi
            class PublicApi
            """
        ) { dModule ->
            assertThat(dModule.packages[0].classlikes.singleOrNull { it.name == "HiddenApi" })
                .isNull()
        }
    }

    @Test
    fun `InternalApi annotation hides elements`() {
        testTransformer(
            """
            /src/com/sample/Api.kt
            package com.example
            annotation class InternalApi
            @InternalApi
            class HiddenApi
            class PublicApi
            """
        ) { dModule ->
            assertThat(dModule.packages[0].classlikes.singleOrNull { it.name == "HiddenApi" })
                .isNull()
        }
    }

    @Test
    fun `Non internal API annotation does not hide elements`() {
        testTransformer(
            """
            /src/com/sample/Api.kt
            package com.example
            annotation class PublicApi
            @PublicApi
            class Foo
            """
        ) { dModule ->
            assertThat(dModule.packages[0].classlikes.singleOrNull { it.name == "Foo" }).isNotNull()
        }
    }

    @Test
    fun `Annotation with internal in name does not hide elements`() {
        testTransformer(
            """
            /src/com/sample/Api.kt
            package com.example
            annotation class NotInternalExampleApi
            @NotInternalExampleApi
            class Foo
            """
        ) { dModule ->
            assertThat(dModule.packages[0].classlikes.singleOrNull { it.name == "Foo" }).isNotNull()
        }
    }
}
