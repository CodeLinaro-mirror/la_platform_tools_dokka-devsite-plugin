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
import com.google.devsite.components.Link
import com.google.devsite.components.symbols.MetadataComponent
import com.google.devsite.components.symbols.SymbolDetail.Params
import com.google.devsite.components.symbols.SymbolDetail.SymbolKind
import com.google.devsite.components.symbols.VersionMetadataComponent
import com.google.devsite.components.testing.NoopAnnotationComponent
import com.google.devsite.components.testing.NoopContextFreeComponent
import com.google.devsite.components.testing.NoopFunctionSignature
import com.google.devsite.components.testing.NoopTypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Modifiers
import com.google.devsite.util.LibraryMetadata
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultSymbolDetailTest {
    @Test
    fun `Simple Java function renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                name = "foo",
                returnType = NoopTypeProjectionComponent("void"),
                symbolKind = SymbolKind.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                anchors = linkedSetOf(),
                metadata = emptyList(),
                displayLanguage = Language.JAVA
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item">
    <div class="api-name-block">
      <div>
        <h3>foo</h3>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">void&nbsp;foo()</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Simple Kotlin function renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                name = "foo",
                returnType = NoopTypeProjectionComponent("Unit"),
                symbolKind = SymbolKind.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                anchors = linkedSetOf(),
                metadata = emptyList(),
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item">
    <div class="api-name-block">
      <div>
        <h3>foo</h3>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">fun&nbsp;foo():&nbsp;Unit</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Simple Kotlin property renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                name = "foo",
                returnType = NoopTypeProjectionComponent("Unit"),
                symbolKind = SymbolKind.READ_ONLY_PROPERTY,
                signature = NoopFunctionSignature("foo"),
                anchors = linkedSetOf(),
                metadata = emptyList(),
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item">
    <div class="api-name-block">
      <div>
        <h3>foo</h3>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">val&nbsp;foo:&nbsp;Unit</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Simple Kotlin constructor renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                name = "MyClass",
                returnType = NoopTypeProjectionComponent("Unit"),
                symbolKind = SymbolKind.CONSTRUCTOR,
                signature = NoopFunctionSignature("MyClass()"),
                anchors = linkedSetOf(),
                metadata = emptyList(),
                displayLanguage = Language.KOTLIN
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item">
    <div class="api-name-block">
      <div>
        <h3>MyClass</h3>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">MyClass()</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Simple Java constructor renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                name = "MyClass",
                returnType = NoopTypeProjectionComponent("Unit"),
                symbolKind = SymbolKind.CONSTRUCTOR,
                signature = NoopFunctionSignature("MyClass()"),
                anchors = linkedSetOf(),
                metadata = emptyList(),
                displayLanguage = Language.JAVA
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item">
    <div class="api-name-block">
      <div>
        <h3>MyClass</h3>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">MyClass()</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Java function with annotations renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                name = "foo",
                returnType = NoopTypeProjectionComponent("void"),
                symbolKind = SymbolKind.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                anchors = linkedSetOf(),
                metadata = emptyList(),
                displayLanguage = Language.JAVA,
                annotationComponents = listOf(
                    NoopAnnotationComponent("@Foo"),
                    NoopAnnotationComponent("@Bar")
                )
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item">
    <div class="api-name-block">
      <div>
        <h3>foo</h3>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">@Foo<br>@Bar<br>void&nbsp;foo()</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Java function with modifiers renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                name = "foo",
                returnType = NoopTypeProjectionComponent("void"),
                symbolKind = SymbolKind.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                anchors = linkedSetOf(),
                metadata = emptyList(),
                displayLanguage = Language.JAVA,
                modifiers = Modifiers("protected", "abstract")
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item">
    <div class="api-name-block">
      <div>
        <h3>foo</h3>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">protected&nbsp;abstract&nbsp;void&nbsp;foo()</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Kotlin function with modifiers renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                name = "foo",
                returnType = NoopTypeProjectionComponent("Unit"),
                symbolKind = SymbolKind.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                anchors = linkedSetOf(),
                metadata = emptyList(),
                displayLanguage = Language.KOTLIN,
                modifiers = Modifiers("protected", "abstract")
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item">
    <div class="api-name-block">
      <div>
        <h3>foo</h3>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">protected&nbsp;abstract&nbsp;fun&nbsp;foo():&nbsp;Unit</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Function anchors render correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                name = "foo",
                returnType = NoopTypeProjectionComponent("void"),
                symbolKind = SymbolKind.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                anchors = linkedSetOf("foo(a,b)", "foo(a, b)", "foo-a-b-"),
                metadata = emptyList(),
                displayLanguage = Language.JAVA
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item"><a name="foo(a, b)"></a><a name="foo-a-b-"></a>
    <div class="api-name-block">
      <div>
        <h3 id="foo(a,b)">foo</h3>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">void&nbsp;foo()</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Function with metadata renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                name = "foo",
                returnType = NoopTypeProjectionComponent("void"),
                symbolKind = SymbolKind.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                anchors = linkedSetOf(),
                metadata = listOf(NoopContextFreeComponent, NoopContextFreeComponent),
                displayLanguage = Language.JAVA
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item">
    <div class="api-name-block">
      <div>
        <h3>foo</h3>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">void&nbsp;foo()</pre>
    <div>noop</div>
    <div>noop</div>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Function with extension function package renders correctly`() {
        val component = DefaultSymbolDetail(
            Params(
                name = "foo",
                returnType = NoopTypeProjectionComponent("void"),
                symbolKind = SymbolKind.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                anchors = linkedSetOf(),
                metadata = emptyList(),
                displayLanguage = Language.JAVA,
                extFunctionClass = "MyClassKt"
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item">
    <div class="api-name-block">
      <div>
        <h3>MyClassKt.foo</h3>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">void&nbsp;foo()</pre>
  </div>
</div>
            """.trim()
        )
    }

    @Test
    fun `Simple Java function with metadata renders correctly`() {
        val metadataComponent = DefaultMetadataComponent(
            MetadataComponent.Params(
                libraryMetadata = LibraryMetadata(
                    groupId = "testGroup",
                    artifactId = "testArtifactId",
                    releaseNotesUrl = "https://d.android.com"
                ),
                sourceLinkUrl = "https://cs.android.com",
                versionMetadata = DefaultVersionMetadataComponent(
                    VersionMetadataComponent.Params(
                        addedIn = DefaultLink(Link.Params(name = "API Level 8", url = "")),
                        deprecatedIn = DefaultLink(Link.Params(name = "API Level 12", url = ""))
                    )
                )
            )
        )
        val component = DefaultSymbolDetail(
            Params(
                name = "foo",
                returnType = NoopTypeProjectionComponent("void"),
                symbolKind = SymbolKind.FUNCTION,
                signature = NoopFunctionSignature("foo()"),
                anchors = linkedSetOf(),
                metadata = emptyList(),
                displayLanguage = Language.JAVA,
                metadataComponent = metadataComponent
            )
        )

        val output = createHTML().div {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<div>
  <div class="api-item">
    <div class="api-name-block">
      <div>
        <h3>foo</h3>
      </div>
      <div id="metadata-info-block">
        <div id="maven-coordinates">Artifact: <a href="https://d.android.com">testGroup:testArtifactId</a></div>
        <div id="source-link"><a href="https://cs.android.com" class="external">View Source</a></div>
        <div id="version-metadata">
          <div id="added-in">Added in API Level 8</div>
          <div id="deprecated-in">Deprecated in API Level 12</div>
        </div>
      </div>
    </div>
    <pre class="api-signature no-pretty-print">void&nbsp;foo()</pre>
  </div>
</div>
            """.trim()
        )
    }
}
