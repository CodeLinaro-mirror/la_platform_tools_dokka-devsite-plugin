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
import com.google.devsite.components.Description.Params
import com.google.devsite.testing.ConverterTestBase
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.junit.Ignore
import org.junit.Test

internal class DefaultDescriptionTest : ConverterTestBase() {
    @Test
    fun `Single sentence renders correctly`() {
        val component = """
            |/** Hello world! */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>Hello world!</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Deprecation renders renders correctly`() {
        val component = """
            |/** Hello world! */
            |class Foo
        """.render().description(
            deprecation = "This class is deprecated."
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p class="caution"><strong>This class is deprecated.</strong><br>Hello world!</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Summary trims documentation on period`() {
        val component = """
            |/**
            | * 1 2 3. 4 5 6
            | *
            | * Stuff.
            | */
            |class Foo
        """.render().description(
            summary = true
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>1 2 3.</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Deprecation summary renders renders correctly`() {
        val component = """
            |/** Hello world! */
            |class Foo
        """.render().description(
            summary = true,
            deprecation = "This class is deprecated."
        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p><em>This class is deprecated.</em> Hello world!</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Paragraphs render correctly`() {
        val component = """
            |/**
            | * There was an old lady who swallowed a fly.
            | * I dunno why she swallowed that fly,
            | * Perhaps she'll die.
            | *
            | * ...
            | *
            | * There was an old lady who swallowed a cow.
            | * I don't know how she swallowed a cow!
            | * She swallowed the cow to catch the goat...
            | * She swallowed the goat to catch the dog...
            | * She swallowed the dog to catch the cat...
            | * She swallowed the cat to catch the bird ...
            | * She swallowed the bird to catch the spider
            | * That wiggled and wiggled and tickled inside her.
            | * She swallowed the spider to catch the fly.
            | * But I dunno why she swallowed that fly
            | * Perhaps she'll die.
            | *
            | * There was an old lady who swallowed a horse -
            | * She's dead, of course.
            | */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>There was an old lady who swallowed a fly. I dunno why she swallowed that fly, Perhaps she'll die.</p>
  <p>...</p>
  <p>There was an old lady who swallowed a cow. I don't know how she swallowed a cow! She swallowed the cow to catch the goat... She swallowed the goat to catch the dog... She swallowed the dog to catch the cat... She swallowed the cat to catch the bird ... She swallowed the bird to catch the spider That wiggled and wiggled and tickled inside her. She swallowed the spider to catch the fly. But I dunno why she swallowed that fly Perhaps she'll die.</p>
  <p>There was an old lady who swallowed a horse - She's dead, of course.</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Line breaks render correctly`() {
        val component = """
            |/**
            | * A \
            | * B \
            | * C.
            | */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>A <br>B <br>C.</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Inline code renders correctly`() {
        val component = """
            |/** The `Boolean` type has two possible values: `true` or `false`. */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>The <code>Boolean</code> type has two possible values: <code>true</code> or <code>false</code>.</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Formatted text renders correctly`() {
        val component = """
            |/** *Italics*, **Bold**, ***Both***, ~~Bad~~. */
            |class Foo
        """.render().description()

        val output = createHTML(prettyPrint = false).body {
            component.render(this)
        }.trim()

        // TODO(b/163860333): the strikethrough gets removed because reasons ¯\_(ツ)_/¯
        // language=html
        assertThat(output).isEqualTo(
            """
<body><p><em>Italics</em>, <b>Bold</b>, <b><em>Both</em></b>, <del></del>.</p></body>
            """.trim()
        )
    }

    @Test
    fun `Itemized list renders correctly`() {
        val component = """
            |/**
            | * Stuff:
            | *   - Thing 1
            | *   - Thing 2
            | *   - Thing 3
            | */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>Stuff:</p>
  <ul>
    <li>
      <p>Thing 1</p>
    </li>
    <li>
      <p>Thing 2</p>
    </li>
    <li>
      <p>Thing 3</p>
    </li>
  </ul>
</body>
            """.trim()
        )
    }

    @Test
    fun `Numbered list renders correctly`() {
        val component = """
            |/**
            | * Stuff:
            | *   1. Thing 1
            | *   2. Thing 2
            | *   3. Thing 3
            | */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>Stuff:</p>
  <ol>
    <li>
      <p>Thing 1</p>
    </li>
    <li>
      <p>Thing 2</p>
    </li>
    <li>
      <p>Thing 3</p>
    </li>
  </ol>
</body>
            """.trim()
        )
    }

    @Test
    fun `Nested lists render correctly`() {
        val component = """
            |/**
            | * Stuff:
            | * *   a
            | *     1.  a
            | *     2.  b
            | *     3.  c
            | * *   b
            | *     -   a
            | *     -   b
            | *     -   c
            | *         *   a
            | * *   c
            | */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>Stuff:</p>
  <ul>
    <li>
      <p>a</p>
    </li>
    <ol>
      <li>
        <p>a</p>
      </li>
      <li>
        <p>b</p>
      </li>
      <li>
        <p>c</p>
      </li>
    </ol>
    <li>
      <p>b</p>
    </li>
    <ul>
      <li>
        <p>a</p>
      </li>
      <li>
        <p>b</p>
      </li>
      <li>
        <p>c</p>
      </li>
      <ul>
        <li>
          <p>a</p>
        </li>
      </ul>
    </ul>
    <li>
      <p>c</p>
    </li>
  </ul>
</body>
            """.trim()
        )
    }

    @Test
    fun `Table renders correctly`() {
        val component = """
            |/**
            | * | Tables   |      Are      |       Cool |
            | * |----------|:-------------:|-----------:|
            | * | col 1 is |  left-aligned | ${'$'}1600 |
            | * | col 2 is |    centered   |   ${'$'}12 |
            | * | col 3 is | right-aligned |    ${'$'}1 |
            | */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // TODO(b/163856933): fix the bad formatting
        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <table>
    <tr>
      <th>Tables   </th>
      <th>      Are      </th>
      <th>       Cool </th>
    </tr>
    <tr>
      <td> col 1 is </td>
      <td>  left-aligned </td>
      <td> ${'$'}1600 </td>
    </tr>
    <tr>
      <td> col 2 is </td>
      <td>    centered   </td>
      <td>   ${'$'}12 </td>
    </tr>
    <tr>
      <td> col 3 is </td>
      <td> right-aligned </td>
      <td>    ${'$'}1 </td>
    </tr>
  </table>
</body>
            """.trim()
        )
    }

    @Test
    fun `Code block renders correctly`() {
        val component = """
            |/**
            | * Welcome:
            | *
            | * ```kotlin
            | * fun main() {
            | *     println("Hello World!")
            | * }
            | * ```
            | */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>Welcome:</p>
  <pre>fun main() {<br>    println(&quot;Hello World!&quot;)<br>}</pre>
</body>
            """.trim()
        )
    }

    @Test
    fun `Plain link renders correctly`() {
        val component = """
            |/** Click [here](http://meme). */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p>Click <a href="http://meme">here</a>.</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Link to type renders correctly`() {
        val component = """
            |class Bar
            |
            |/** [Bar] is pretty cool. */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p><code><a href="/reference/androidx/example/Bar.html">Bar</a></code> is pretty cool.</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Link to symbol renders correctly`() {
        val component = """
            |fun bar() = Unit
            |
            |/** [bar] is pretty cool. */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p><code><a href="/reference/androidx/example/package-summary.html#bar()">bar</a></code> is pretty cool.</p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Image renders correctly`() {
        val component = """
            |/** ![Alt text](/path/to/img.jpg "Image Title") */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p><img alt="Alt text" src="/path/to/img.jpg"></p>
</body>
            """.trim()
        )
    }

    @Test
    fun `Quote renders correctly`() {
        val component = """
            |/**
            | * > Two things are infinite: the universe and human stupidity; and I'm not sure about
            | * > the universe. -- Albert Einstein
            | */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <blockquote>
    <p>Two things are infinite: the universe and human stupidity; and I'm not sure about the universe. -- Albert Einstein</p>
  </blockquote>
</body>
            """.trim()
        )
    }

    @Ignore // TODO(b/163860333): Strikethrough broken
    @Test
    fun `Strikethrough renders correctly`() {
        val component = """
            |/** ~~Hello~~ world! */
            |class Foo
        """.render().description()

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<body>
  <p><del>Hello</del> world!</p>
</body>
            """.trim()
        )
    }

    private fun DModule.description(
        summary: Boolean = false,
        deprecation: String? = null
    ): DefaultDescription {
        val tag = packages.single().classlikes.single { it.name == "Foo" }.tag()
        return DefaultDescription(Params(pathProvider(), tag, summary, deprecation))
    }

    private fun Documentable.tag() =
        documentation.values.singleOrNull()?.children.orEmpty().single().root
}
