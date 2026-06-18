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
import com.google.devsite.components.DescriptionComponent.Params
import com.google.devsite.testing.ConverterTestBase
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.junit.Test

internal class DefaultDescriptionComponentTest : ConverterTestBase() {
    @Test
    fun `Single sentence renders correctly`() {
        val component =
            """
            |/** Hello world! */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>Hello world!</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Deprecation renders renders correctly`() {
        val component =
            """
            |/** Hello world! */
            |class Foo
        """
                .render()
                .description(deprecation = "This class is deprecated.")

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <aside class="caution"><strong>This class is deprecated.</strong><br>
    <p>Hello world!</p>
  </aside>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Template literals are removed from deprecation stanza`() {
        val component =
            """
            |/** Hello world! */
            |class Foo
        """
                .render()
                .description(
                    deprecation = "This class is deprecated {{ template }} {% include _bad.html %}."
                )

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <aside class="caution"><strong>This class is deprecated { { template }} { % include _bad.html %}.</strong><br>
    <p>Hello world!</p>
  </aside>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Summary trims documentation on period`() {
        val component =
            """
            |/**
            | * 1 2 3. 4 5 6
            | *
            | * Stuff.
            | */
            |class Foo
        """
                .render()
                .description(summary = true)

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>1 2 3.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Summary escapes template literals`() {
        val component =
            """
            |/** Hello world {% include _file.html %} */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                // there is now a space between the { and % at the beginning, this causes the
                // template to get rendered literally rather than actually pulling any content
                """
<body>
  <p>Hello world { % include _file.html %}</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Summary ignores period in word`() {
        val component =
            """
            |/**
            | * This is foo.bar, blah blah.
            | *
            | * Stuff.
            | */
            |class Foo
        """
                .render()
                .description(summary = true)

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>This is foo.bar, blah blah.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Summary ignores period in link`() {
        val component =
            """
            |/** [Foo.Bar] has great drinks. */
            |class Foo
        """
                .render()
                .description(summary = true)

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>Foo.Bar has great drinks.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Summary breaks on period space code tag`() {
        val component1 =
            """
            |/**
            | * This is a complete sentence. <code>Foo.hashCode</code> is a function.
            | */
            |class Foo
        """
                .render()
                .description(summary = true)
        val component2 =
            """
            |/**
            | * This is a complete sentence.
            | * <code>Foo.hashCode</code> is a function.
            | */
            |class Foo
        """
                .render()
                .description(summary = true)

        val output1 = createHTML().body { component1.render(this) }.trim()
        val output2 = createHTML().body { component2.render(this) }.trim()
        val expected =
            """
<body>
  <p>This is a complete sentence.</p>
</body>
            """
                .trim()
        // language=html
        assertThat(output1).isEqualTo(expected)
        assertThat(output2).isEqualTo(expected)
    }

    @Test
    fun `Summary breaks on period space markdown-code`() {
        val component =
            """
            |/**
            | * This [Animatable] function creates a float value holder that automatically
            | * animates its value when the value is changed via [animateTo]. [Animatable] supports value
            | * change during an ongoing value change animation.
            | */
            |class Foo
        """
                .render()
                .description(summary = true)

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>This Animatable function creates a float value holder that automatically animates its value when the value is changed via animateTo.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Summary breaks on new paragraph even without ending period`() {
        val component =
            """
            |/**
            | * Animation will be forced to end when its value reaches upper/lower bound (if they have
            | * been defined, e.g. via [Animatable.updateBounds])
            | *
            | * Unlike [Finished], when an animation ends due to [BoundReached], it often falls short
            | * from its initial target, and the remaining velocity is often non-zero. Both the end value
            | * and the remaining velocity can be obtained via [AnimationResult].
            | */
            |class Foo
        """
                .render()
                .description(summary = true)

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>Animation will be forced to end when its value reaches upper/lower bound (if they have been defined, e.g. via Animatable.updateBounds)</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Spacing does not confuse sentence-end detector`() {
        val component =
            """
            |/**
            | * the amount of time (in milliseconds) the animation will take to finish.
            | *                     Defaults to [DefaultDuration]
            | */
            |class Foo
        """
                .render()
                .description(summary = true)

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>the amount of time (in milliseconds) the animation will take to finish.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Deprecation summary renders renders correctly`() {
        val component =
            """
            |/**
            | * Hello world!
            | */
            |class Foo
        """
                .render()
                .description(summary = true, deprecation = "This class is deprecated.")

        val output = createHTML().body { component.render(this) }.trim()
        // TODO(b/171570474) Work around for EOL space introduced by Dokka and is required to make
        // the
        // test pass but, stripped by the IDE
        // language=html
        assertThat(output)
            .isEqualTo(
                "<body>\n" +
                    "  <p><strong>This class is deprecated.</strong>\n" +
                    "    <p>Hello world!</p>\n" +
                    "  </p>\n" +
                    "   </body>".trim()
            )
    }

    @Test
    fun `Template literals are removed from deprecation summary`() {
        val component =
            """
            |/**
            | * Hello world!
            | */
            |class Foo
        """
                .render()
                .description(
                    summary = true,
                    deprecation = "This class is deprecated {{ template }} {% include _bad.html %}.",
                )

        val output = createHTML().body { component.render(this) }.trim()
        // TODO(b/171570474) Work around for EOL space introduced by Dokka and is required to make
        // the
        // test pass but, stripped by the IDE
        // language=html
        assertThat(output)
            .isEqualTo(
                "<body>\n" +
                    "  <p><strong>This class is deprecated { { template }} { % include _bad.html %}.</strong>\n" +
                    "    <p>Hello world!</p>\n" +
                    "  </p>\n" +
                    "   </body>".trim()
            )
    }

    @Test
    fun `Paragraphs render correctly`() {
        val component =
            """
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
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>There was an old lady who swallowed a fly. I dunno why she swallowed that fly, Perhaps she'll die.</p>
  <p>...</p>
  <p>There was an old lady who swallowed a cow. I don't know how she swallowed a cow! She swallowed the cow to catch the goat... She swallowed the goat to catch the dog... She swallowed the dog to catch the cat... She swallowed the cat to catch the bird ... She swallowed the bird to catch the spider That wiggled and wiggled and tickled inside her. She swallowed the spider to catch the fly. But I dunno why she swallowed that fly Perhaps she'll die.</p>
  <p>There was an old lady who swallowed a horse - She's dead, of course.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Line breaks render correctly`() {
        val component =
            """
            |/**
            | * A \
            | * B \
            | * C.
            | */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>A <br>B <br>C.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Inline code renders correctly in 4x Kotlin and Java`() {
        val componentK =
            """
            |/** The `Boolean` type has two possible values: `true` or `false`. */
            |class Foo
        """
                .render()
                .description()
        val componentJ =
            """
            |/** The {@code Boolean} type has two possible values: {@code true} or {@code false}. */
            |public class Foo
        """
                .render(java = true)
                .description()

        for (component in listOf(componentJ, componentK)) {
            val output = createHTML().body { component.render(this) }.trim()

            // language=html
            assertThat(output)
                .isEqualTo(
                    """
<body>
  <p>The <code>Boolean</code> type has two possible values: <code>true</code> or <code>false</code>.</p>
</body>
            """
                        .trim()
                )
        }
    }

    @Test
    fun `Formatted text renders correctly`() {
        val component =
            """
            |/** *Italics*, **Bold**, ***Both***, ~~Bad~~. */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML(prettyPrint = false).body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body><p><em>Italics</em>, <b>Bold</b>, <em><b>Both</b></em>, <del>Bad</del>.</p></body>
            """
                    .trim()
            )
    }

    @Test
    fun `Itemized list renders correctly`() {
        val component =
            """
            |/**
            | * Stuff:
            | *   - Thing 1
            | *   - Thing 2
            | *   - Thing 3
            | */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
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
            """
                    .trim()
            )
    }

    @Test
    fun `Numbered list renders correctly`() {
        val component =
            """
            |/**
            | * Stuff:
            | *   1. Thing 1
            | *   2. Thing 2
            | *   3. Thing 3
            | */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
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
            """
                    .trim()
            )
    }

    @Test
    fun `Numbered list renders correctly from html`() {
        val component =
            """
            |/**
            | * <ol>
            | *    <li>
            | *      <p>Thing 1</p>
            | *    </li>
            | *    <li>
            | *      <p>Thing 2</p>
            | *    </li>
            | *    <li>
            | *     <p>Thing 3</p>
            | *    </li>
            | * </ol>
            | */
            |public class Foo {}
        """
                .render(java = true)
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
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
            """
                    .trim()
            )
    }

    @Test
    fun `Demonstrate that markdown shouldn't render into heading from Javadoc`() {
        val component =
            """
            |/**
            | * <h2>h2 tag</h2>
            | * <h3>h3 tag</h3>
            | * ### markdown header
            | */
            |public class Foo {}
        """
                .render(java = true)
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <h2>h2 tag</h2>
  <h3>h3 tag</h3>
 ### markdown header</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Demonstrate that markdown should render into heading from Kdoc`() {
        val component =
            """
            |/**
            | * <h2>h2 tag</h2>
            | * <h3>h3 tag</h3>
            | * ### markdown header
            | */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p><h2>h2 tag</h2> <h3>h3 tag</h3></p>
  <h3>markdown header</h3>
</body>
            """
                    .trim()
            )
    }

    // TODO fix handling @link inside dt, dd b/217937742
    @Test
    fun `Description list renders correctly in kotlin`() {
        val component =
            """
            |/**
            | * <dl>
            | *     <dt>
            | *        <code>name="<i>name</i>"</code>
            | *    </dt>
            | *     <dd>
            | *         A URI path segment.
            | *     </dd>
            | *     <dt>
            | *         An {@link Intent} to navigate.
            | *    </dt>
            | *    <dd>
            | *         The subdirectory you're sharing.
            | *     </dd>
            | * </dl>
            | */
            |public class Foo {}
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body><dl>
     <dt>
        <code>name="<i>name</i>"</code>
    </dt>
     <dd>
         A URI path segment.
     </dd>
     <dt>
         An {@link Intent} to navigate.
    </dt>
    <dd>
         The subdirectory you're sharing.
     </dd>
</dl></body>
            """
                    .trim()
            )
    }

    // TODO fix handling @link inside dt, dd b/217937742
    // TODO remove improper handling of dt that requires <p> b/217941159
    @Test
    fun `Description list renders correctly in java`() {
        val component =
            """
            |/**
            | * <dl>
            | *     <dt>
            | *        <code>name="<i>name</i>"</code>
            | *    </dt>
            | *     <dd>
            | *         A URI path segment.
            | *     </dd>
            | *     <dt>
            | *         An {@link Intent} to navigate
            | *    </dt>
            | *    <dd>
            | *         The subdirectory you're sharing.
            | *     </dd>
            | * </dl>
            | */
            |public class Foo {}
        """
                .render(java = true)
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <dl>
    <dt><p><code>name=&quot;<em>name</em>&quot;</code></p>
</dt>
    <dd> A URI path segment. </dd>
    <dt><p> An Intent to navigate </p>
</dt>
    <dd> The subdirectory you're sharing. </dd>
  </dl>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Nested lists render correctly`() {
        val component =
            """
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
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
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
            """
                    .trim()
            )
    }

    @Test
    fun `Table renders correctly`() {
        val component =
            """
            |/**
            | * | Tables   |      Are      |       Cool |
            | * |----------|:-------------:|-----------:|
            | * | col 1 is |  left-aligned | ${'$'}1600 |
            | * | col 2 is |    centered   |   ${'$'}12 |
            | * | col 3 is | right-aligned |    ${'$'}1 |
            | */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <table>
    <tr>
      <td>Tables</td>
      <td>Are</td>
      <td>Cool</td>
    </tr>
    <tr>
      <td>col 1 is</td>
      <td>left-aligned</td>
      <td>${'$'}1600</td>
    </tr>
    <tr>
      <td>col 2 is</td>
      <td>centered</td>
      <td>${'$'}12</td>
    </tr>
    <tr>
      <td>col 3 is</td>
      <td>right-aligned</td>
      <td>${'$'}1</td>
    </tr>
  </table>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Code blocks render correctly`() {
        val component =
            """
            |/**
            | * Welcome:
            | *
            | * ```kotlin
            | * fun main() {
            | *     println("Hello World!")
            | * }
            | * ```
            | *
            | *     fun thisIsACodeBlock() {
            | *         val butWhy = "per markdown spec, because four-spaces prefix"
            | *     }
            | */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>Welcome:</p>
  <pre class="prettyprint">fun main() {<br>    println(&quot;Hello World!&quot;)<br>}</pre>
  <pre class="prettyprint">fun thisIsACodeBlock() {
    val butWhy = &quot;per markdown spec, because four-spaces prefix&quot;
}</pre>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Plain link renders correctly`() {
        val component =
            """
            |/** Click [here](http://meme). */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>Click <a href="http://meme">here</a>.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Link to type renders correctly`() {
        val component =
            """
            |class Bar
            |
            |/** [Bar] is pretty cool. */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p><code><a href="/reference/androidx/example/Bar.html">Bar</a></code> is pretty cool.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Link to symbol renders correctly`() {
        val component =
            """
            |fun bar() = Unit
            |
            |/** [bar] is pretty cool. */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p><code><a href="/reference/androidx/example/package-summary.html#bar()">bar</a></code> is pretty cool.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Named link to type renders correctly`() {
        val component =
            """
            |class BarIsVeryVeryVeryVeryLongNamed
            |
            |/** [Special snowflake snowflake snowflake snowflake snowflake][BarIsVeryVeryVeryVeryLongNamed]
            | * is pretty cool.
            | */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p><code><a href="/reference/androidx/example/BarIsVeryVeryVeryVeryLongNamed.html">Special snowflake snowflake snowflake snowflake snowflake</a></code> is pretty cool.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Image renders correctly`() {
        val component =
            """
            |/** ![Alt text](/path/to/img.jpg "Image Title") */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p><img alt="Alt text" src="/path/to/img.jpg"></p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Quote renders correctly`() {
        val component =
            """
            |/**
            | * > Two things are infinite: the universe and human stupidity; and I'm not sure about
            | * > the universe. -- Albert Einstein
            | */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <blockquote>
    <p>Two things are infinite: the universe and human stupidity; and I'm not sure about the universe. -- Albert Einstein</p>
  </blockquote>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Strikethrough renders correctly`() {
        val component =
            """
            |/** ~~Hello~~ world! */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>
    <del>Hello</del>
 world!</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Pre and @code handled correctly`() {
        val component =
            """
            |/**
            | * {@code
            | *     fun thisIsANonPreCodeBlock() {
            | *         doNotPreserveWhitespace: String         \\ blah
            | *     }
            | * }
            | * <pre>{@code
            | *     fun thisIsAPreCodeBlock() {
            | *         DOPreserveWhitespace: String            \\ blah
            | *     }
            | * }</pre>
            |*/
            |public class Foo()
        """
                .render(java = true)
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // These look the same, but pasting them into an HTML file correctly displays:
        //
        // fun thisIsANonPreCodeBlock() { doNotPreserveWhitespace: String \\ blah }
        //
        //     fun thisIsAPreCodeBlock() {
        //         DOPreserveWhitespace: String            \\ blah
        //     }
        assertThat(output)
            .isEqualTo(
                """<body>
  <p><code>
    fun thisIsANonPreCodeBlock() {
        doNotPreserveWhitespace: String         \\ blah
    }
</code></p>
  <pre class="prettyprint">
    fun thisIsAPreCodeBlock() {
        DOPreserveWhitespace: String            \\ blah
    }
</pre>
</body>"""
            )
    }

    @Test
    fun `table captions handled correctly`() {
        val component =
            """
            |/**
            | * <table>
            | * <caption>Uri patterns and following API calls for MediaControllerCompat methods</caption>
            | * <tr>
            | * <th>Uri patterns</th><th>Following API calls</th><th>Method</th>
            | * </tr><tr>
            | * <td rowspan="2">{@code androidx://media3-session/setMediaUri?uri=[uri]}</td>
            | * <td>{@link #prepare}</td>
            | * <td>{@link MediaControllerCompat.TransportControls#prepareFromUri prepareFromUri}
            | * </tr>
            | * </tr>
            | * </table>
            | */
            |public class Foo()
        """
                .render(java = true)
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        assertThat(output)
            .isEqualTo(
                """<body>
  <table>
    <caption>Uri patterns and following API calls for MediaControllerCompat methods</caption>
    <tbody>
      <tr>
        <th>Uri patterns</th>
        <th>Following API calls</th>
        <th>Method</th>
      </tr>
      <tr>
        <td><code>androidx://media3-session/setMediaUri?uri=[uri]</code></td>
        <td>prepare</td>
        <td>prepareFromUri</td>
      </tr>
    </tbody>
  </table>
</body>"""
            )
    }

    @Test // NOTE: kdoc markdown-style links do not work inside <pre> tags
    fun `Links inside pre render correctly`() {
        val componentJ =
            """
            |/**
            | * a {@link Foo}
            | * <pre>
            | * public void onCreate() {
            | *     if (DEVELOPER_MODE) {
            | *         StrictMode.setThreadPolicy(new {@link Foo pFooey}()
            | *                 .detectDiskReads()
            | * </pre>
            | */
            |public class Foo
        """
                .render(java = true)
                .description()
        val componentK =
            """
            |/**
            | * a [Foo]
            | * <pre>
            | * public void onCreate() {
            | *     if (DEVELOPER_MODE) {
            | *         StrictMode.setThreadPolicy(new [Foo]()
            | *                 .detectDiskReads()
            | * </pre>
            | */
            |public class Foo
        """
                .render()
                .description()

        val outputJ = createHTML().body { componentJ.render(this) }.trim()
        val outputK = createHTML().body { componentK.render(this) }.trim()

        // language=html
        assertThat(outputJ)
            .isEqualTo(
                """
<body>
  <p>a <code><a href="/reference/androidx/example/Test.Foo.html">Foo</a></code></p>
  <pre class="prettyprint">public void onCreate() {
    if (DEVELOPER_MODE) {
        StrictMode.setThreadPolicy(new <code><a href="/reference/androidx/example/Test.Foo.html">pFooey</a></code>()
                .detectDiskReads()
</pre>
</body>
        """
                    .trim()
            )
        // language=html
        assertThat(outputK)
            .isEqualTo(
                """
<body>
  <p>a <code><a href="/reference/androidx/example/Foo.html">Foo</a></code></p>
<pre>
public void onCreate() {
     if (DEVELOPER_MODE) {
         StrictMode.setThreadPolicy(new [Foo]()
                 .detectDiskReads()
</pre></body>
        """
                    .trim()
            )
    }

    @Test
    fun `HTML link with docRoot renders correctly`() {
        val component =
            """
            |/** Click <a href="{@docRoot}guide">here</a>. */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p>Click <a href="/guide">here</a>.</p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Heading renders with correct attributes`() {
        val component =
            """
            |/** <h2 id="sample-formats">Fields relevant to sample formats</h2> */
            |class Foo
        """
                .render()
                .description()

        val output = createHTML().body { component.render(this) }.trim()

        // language=html
        assertThat(output)
            .isEqualTo(
                """
<body>
  <p><h2 id="sample-formats">Fields relevant to sample formats</h2></p>
</body>
            """
                    .trim()
            )
    }

    @Test
    fun `Test @usesMathJax`() {
        val classlike =
            """
            |/**
            | * {@usesMathJax}
            | *
            | * Destination pixels covered by the source are cleared to 0.
            | *
            | * <p>\(\alpha_{out} = 0\)</p>
            | * <p>\(C_{out} = 0\)</p>
            | */
            |public class Foo {}
        """
                .render(java = true)

        val summary =
            createHTML().body { classlike.description(summary = true).render(this) }.trim()
        // No MathJax tag in summary (and only the first line included)
        // language=html
        assertThat(summary)
            .isEqualTo(
                """
                <body>
                  <p> Destination pixels covered by the source are cleared to 0.</p>
                </body>
                """
                    .trimIndent()
            )

        val detail =
            createHTML().body { classlike.description(summary = false).render(this) }.trim()
        // Inserted MathJax HTML in detail
        // language=html
        assertThat(detail)
            .isEqualTo(
                """
                <body>
                  <p><devsite-mathjax config="TeX-AMS_SVG"></devsite-mathjax> Destination pixels covered by the source are cleared to 0. </p>
                  <p>\(\alpha_{out} = 0\)</p>
                  <p>\(C_{out} = 0\)</p>
                </body>
                """
                    .trimIndent()
            )
    }

    private fun DModule.description(
        summary: Boolean = false,
        deprecation: String? = null,
    ): DefaultDescriptionComponent {
        val tag = explicitClasslike("Foo").tag()
        val converterHolder = ConverterHolder(this@DefaultDescriptionComponentTest, this)
        return DefaultDescriptionComponent(
            Params(converterHolder.provider, tag.children, summary, deprecation)
        )
    }

    private fun Documentable.tag() =
        documentation.values.singleOrNull()?.children.orEmpty().single().root
}
