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

import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jsoup.Jsoup
import org.junit.Test
import utils.TestOutputWriterPlugin

internal const val LIB_PATH = "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.6/MathJax.js?config=TeX-AMS_SVG&latest"

class MathjaxPluginTest : AbstractCoreTest() {
  @Test
  fun `Basic test`() {
    val configuration = dokkaConfiguration {
      sourceSets {
        sourceSet {
          sourceRoots = listOf("src/main/kotlin/test/Test.kt")
        }
      }
    }
    val source =
      """
            |/src/main/kotlin/test/Test.kt
            |package example
            | /**
            | * @usesMathJax
            | *
            | * \(\alpha_{out} = \alpha_{dst}\)
            | * \(C_{out} = C_{dst}\)
            | */
            | fun test(): String = ""
            """.trimIndent()
    val writerPlugin = TestOutputWriterPlugin()
    testInline(
      source,
      configuration,
      pluginOverrides = listOf(writerPlugin)
    ) {
      renderingStage = {
        _, _ -> Jsoup
        .parse(writerPlugin.writer.contents["root/example/test.html"])
        .head()
        .select("link, script")
        .let {
          assert(it.`is`("[href=$LIB_PATH], [src=$LIB_PATH]"))
        }
      }
    }
  }

  @Test
  fun noMathjaxTest() {
    val configuration = dokkaConfiguration {
      sourceSets {
        sourceSet {
          sourceRoots = listOf("src/main/kotlin/test/Test.kt")
        }
      }
    }
    val source =
      """
            |/src/main/kotlin/test/Test.kt
            |package example
            | /**
            | * Just a regular kdoc
            | */
            | fun test(): String = ""
            """.trimIndent()
    val writerPlugin = TestOutputWriterPlugin()
    testInline(
      source,
      configuration,
      pluginOverrides = listOf(writerPlugin)
    ) {
      renderingStage = {
        _, _ -> Jsoup
        .parse(writerPlugin.writer.contents["root/example/test.html"])
        .head()
        .select("link, script")
        .let {
          assert(!it.`is`("[href=${LIB_PATH}], [src=${LIB_PATH}]"))
        }
      }
    }
  }

}