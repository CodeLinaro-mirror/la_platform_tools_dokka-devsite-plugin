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
import com.google.devsite.components.pages.DevsitePage.Params
import com.google.devsite.components.symbols.MetadataComponent
import com.google.devsite.components.testing.NoopContextFreeComponent
import com.google.devsite.renderer.Language
import com.google.devsite.util.LibraryMetadata
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultDevsitePageTest {
    @Test
    fun `Java page renders correctly`() {
        val component = DefaultDevsitePage(
            Params(
                displayLanguage = Language.JAVA,
                path = "page.html",
                bookPath = "/reference/androidx/_book.yaml",
                title = "Page Title",
                content = NoopContextFreeComponent,
                metadataComponent = null,
                includedHeadTagPath = "_shared/_reference-head-tags.html"
            )
        )

        val output = createHTML().html {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<html devsite="true">
  <head>
    <title>Page Title</title>
{% setvar book_path %}/reference/androidx/_book.yaml{% endsetvar %}
{% include "_shared/_reference-head-tags.html" %}
  </head>
  <body>
    <h1>Page Title</h1>
    <div>noop</div>
  </body>
</html>
            """.trim()
        )
    }

    @Test
    fun `Kotlin page renders correctly`() {
        val component = DefaultDevsitePage(
            Params(
                displayLanguage = Language.KOTLIN,
                path = "page.html",
                bookPath = "/reference/androidx/_book.yaml",
                title = "Page Title",
                content = NoopContextFreeComponent,
                metadataComponent = null,
                includedHeadTagPath = "_shared/_reference-head-tags.html"
            )
        )

        val output = createHTML().html {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<html devsite="true">
  <head>
    <title>Page Title</title>
{% setvar book_path %}/reference/androidx/_book.yaml{% endsetvar %}
{% include "_shared/_reference-head-tags.html" %}
  </head>
  <body>
    <h1>Page Title</h1>
    <div>noop</div>
  </body>
</html>
            """.trim()
        )
    }

    @Test
    fun `Page with metadata renders correctly`() {
        val libraryMetadata = LibraryMetadata(
            groupId = "android.x",
            artifactId = "artifact",
            releaseNotesUrl = "https://d.android.com"
        )
        val versionMetadata = DefaultVersionMetadataComponent.createVersionMetadataWithBaseUrl(
            addedIn = "1.5.4",
            deprecatedIn = "1.6.0-alpha04",
            baseUrl = "https://developer.android.com/jetpack/androidx/releases/fragment"
        )
        val metadataComponent = DefaultMetadataComponent(
            MetadataComponent.Params(
                libraryMetadata = libraryMetadata,
                sourceLinkUrl = "https://cs.android.com",
                versionMetadata = versionMetadata
            )
        )
        val pageComponent = DefaultDevsitePage(
            Params(
                displayLanguage = Language.KOTLIN,
                path = "page.html",
                bookPath = "/reference/androidx/_book.yaml",
                title = "Page Title",
                content = NoopContextFreeComponent,
                metadataComponent = metadataComponent,
                includedHeadTagPath = "_shared/_reference-head-tags.html"
            )
        )

        val output = createHTML().html {
            pageComponent.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<html devsite="true">
  <head>
    <title>Page Title</title>
{% setvar book_path %}/reference/androidx/_book.yaml{% endsetvar %}
{% include "_shared/_reference-head-tags.html" %}
  </head>
  <body>
    <div id="metadata-info-block">
      <div id="maven-coordinates">Artifact: <a href="https://d.android.com">android.x:artifact</a></div>
      <div id="source-link"><a href="https://cs.android.com" class="external">View Source</a></div>
      <div id="version-metadata">
        <div id="added-in">Added in <a href="https://developer.android.com/jetpack/androidx/releases/fragment#1.5.4">1.5.4</a></div>
        <div id="deprecated-in">Deprecated in <a href="https://developer.android.com/jetpack/androidx/releases/fragment#1.6.0-alpha04">1.6.0-alpha04</a></div>
      </div>
    </div>
    <h1>Page Title</h1>
    <div>noop</div>
  </body>
</html>
            """.trim()
        )
    }

    @Test
    fun `Page with metadata without URL renders correctly`() {
        val libraryMetadata = LibraryMetadata(
            groupId = "android.x",
            artifactId = "artifact",
            releaseNotesUrl = ""
        )
        val metadataComponent = DefaultMetadataComponent(
            MetadataComponent.Params(
                libraryMetadata = libraryMetadata,
                sourceLinkUrl = null,
                versionMetadata = null
            )
        )
        val pageComponent = DefaultDevsitePage(
            Params(
                displayLanguage = Language.KOTLIN,
                path = "page.html",
                bookPath = "/reference/androidx/_book.yaml",
                title = "Page Title",
                content = NoopContextFreeComponent,
                metadataComponent = metadataComponent,
                includedHeadTagPath = "_shared/_reference-head-tags.html"
            )
        )

        val output = createHTML().html {
            pageComponent.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<html devsite="true">
  <head>
    <title>Page Title</title>
{% setvar book_path %}/reference/androidx/_book.yaml{% endsetvar %}
{% include "_shared/_reference-head-tags.html" %}
  </head>
  <body>
    <div id="metadata-info-block">
      <div id="maven-coordinates">Artifact: android.x:artifact</div>
    </div>
    <h1>Page Title</h1>
    <div>noop</div>
  </body>
</html>
            """.trim()
        )
    }

    @Test
    fun `Page with no included head tag path renders correctly`() {
        val component = DefaultDevsitePage(
            Params(
                displayLanguage = Language.JAVA,
                path = "page.html",
                bookPath = "/reference/androidx/_book.yaml",
                title = "Page Title",
                content = NoopContextFreeComponent,
                metadataComponent = null,
                includedHeadTagPath = null
            )
        )

        val output = createHTML().html {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<html devsite="true">
  <head>
    <title>Page Title</title>
{% setvar book_path %}/reference/androidx/_book.yaml{% endsetvar %}
  </head>
  <body>
    <h1>Page Title</h1>
    <div>noop</div>
  </body>
</html>
            """.trim()
        )
    }

    @Test
    fun `Page with different head tag path renders correctly`() {
        val component = DefaultDevsitePage(
            Params(
                displayLanguage = Language.JAVA,
                path = "page.html",
                bookPath = "/reference/androidx/_book.yaml",
                title = "Page Title",
                content = NoopContextFreeComponent,
                metadataComponent = null,
                includedHeadTagPath = "en/docs/reference/android/_reference-head-tags.html"
            )
        )

        val output = createHTML().html {
            component.render(this)
        }.trim()

        // language=html
        assertThat(output).isEqualTo(
            """
<html devsite="true">
  <head>
    <title>Page Title</title>
{% setvar book_path %}/reference/androidx/_book.yaml{% endsetvar %}
{% include "en/docs/reference/android/_reference-head-tags.html" %}
  </head>
  <body>
    <h1>Page Title</h1>
    <div>noop</div>
  </body>
</html>
            """.trim()
        )
    }
}
