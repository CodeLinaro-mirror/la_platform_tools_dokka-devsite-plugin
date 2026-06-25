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
import com.google.devsite.testing.defaultPluginsConfiguration
import org.junit.Test

class ResourceClassTransformerTest : BaseTransformerTest() {
    override val defaultConfiguration = createDokkaConfiguration(defaultPluginsConfiguration)

    @Test
    fun `Non resource classes are not filtered`() {
        testTransformer(
            """
            /src/com/sample/Foo.java
            package com.sample;
            public final class Foo {
                public static final class Bar {}
            }
            """
        ) { dModule ->
            val topLevelClasses = dModule.packages.single().classlikes
            assertThat(topLevelClasses).hasSize(1)
            val foo = topLevelClasses[0]
            assertThat(foo.name).isEqualTo("Foo")
            assertThat(foo.classlikes).hasSize(1)
            val bar = foo.classlikes[0]
            assertThat(bar.name).isEqualTo("Bar")
        }
    }

    @Test
    fun `Empty R class in otherwise empty package causes package to be filtered`() {
        testTransformer(
            """
            /src/com/sample/R.java
            package com.sample;
            public final class R {}

            /src/com/other/Other.java
            package com.other;
            public class Other {}
            """
        ) { dModule ->
            assertThat(dModule.packages.map { it.name }).containsExactly("com.other")
        }
    }

    @Test
    fun `Empty R class is filtered`() {
        testTransformer(
            """
            /src/com/sample/R.java
            package com.sample;
            public final class R {}

            /src/com/sample/Other.java
            package com.sample;
            public final class Other {}
            """
        ) { dModule ->
            val topLevelClasses = dModule.packages.single().classlikes
            assertThat(topLevelClasses.map { it.name }).containsExactly("Other")
        }
    }

    @Test
    fun `R class with all empty resource class is filtered`() {
        testTransformer(
            """
            /src/com/sample/R.java
            package com.sample;
            public final class R {
              public static final class id {}
            }

            /src/com/sample/Other.java
            package com.sample;
            public final class Other {}
            }
            """
        ) { dModule ->
            val topLevelClasses = dModule.packages.single().classlikes
            assertThat(topLevelClasses.map { it.name }).containsExactly("Other")
        }
    }

    @Test
    fun `Empty resource classes are filtered`() {
        testTransformer(
            """
            /src/com/sample/R.java
            package com.sample;
            public final class R {
              public static final class attr {
                /** Public attribute */
                public static int publicAttribute = 0;
              }
              public static final class id {}
              public static final class style {
                public static int publicStyle = 0;
              }
              public static final class styleable {}
            }
            """
        ) { dModule ->
            val topLevelClasses = dModule.packages.single().classlikes
            assertThat(topLevelClasses).hasSize(1)
            val rClass = topLevelClasses[0]
            assertThat(rClass.name).isEqualTo("R")
            val resourceClasses = rClass.classlikes.map { it.name }
            assertThat(resourceClasses).containsExactly("attr", "style")
        }
    }
}
