/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.devsite.integration

import com.google.devsite.DevsiteConfiguration
import com.google.devsite.capitalize
import com.google.devsite.testing.IntegrationTestBase
import com.google.devsite.testing.TestOutputWriterPlugin
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.toJsonString
import org.junit.Test
import testApi.testRunner.SourceSetsBuilder
import java.io.File
import java.net.URL

class KmpTest : IntegrationTestBase() {
    @Test
    fun `Validate kmp classes`() {
        validateDirectory("kmp")
    }

    override fun validateDirectory(
        path: String,
        sampleLocations: List<String>,
        includeFiles: List<String>,
        docRootPath: String,
        projectPath: String?,
        javaDocsDirectory: String?,
        kotlinDocsDirectory: String?,
        suffix: String
    ) {
        val baseDir = "testData/$path"
        val sourceDir = "$baseDir/source"

        val externalLinks = mapOf(
            "coroutines" to "https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core",
            "android" to "https://developer.android.com/reference",
            "guava" to "https://guava.dev/releases/18.0/api/docs/package-list",
            "kotlin" to "https://kotlinlang.org/api/latest/jvm/stdlib/"
        ).map {
            ExternalDocumentationLink(
                url = URL(it.value),
                packageListUrl = File("testData").toPath()
                    .resolve("package-lists/${it.key}/package-list").toUri().toURL()
            )
        }
        fun SourceSetsBuilder.createSourceSet(
            name: String,
            displayName: String = name.capitalize(),
            analysisPlatform: String = name,
            dependentSourceSets: Set<DokkaSourceSetID> = emptySet()
        ) = sourceSet {
            this.name = name
            this.displayName = displayName
            val sources = File(sourceDir + "/" + name + "Main").absoluteFile
            check(sources.isDirectory) { "$sources does not exist or is not a directory" }
            sourceRoots = listOf(sources.absolutePath)
            classpath = classpathFromFile("testData/classpath.txt")
            externalDocumentationLinks = externalLinks
            documentedVisibilities = setOf(
                DokkaConfiguration.Visibility.PUBLIC,
                DokkaConfiguration.Visibility.PROTECTED
            )
            this.analysisPlatform = analysisPlatform
            this.dependentSourceSets = dependentSourceSets
        }
        // TODO: write a test that has multiple libraries across the source sets

        val inferredProjectPath = projectPath
            ?: File(sourceDir).listFiles().orEmpty().singleOrNull { it.isDirectory }?.name
            ?: "dokkatest"

        val configuration = dokkaConfiguration {
            sourceSets {
                val common = createSourceSet("common")
                createSourceSet("jvm", "JVM", "jvm", setOf(common.value.sourceSetID))
                createSourceSet("native", dependentSourceSets = setOf(common.value.sourceSetID))
            }
            offlineMode = true
            pluginsConfigurations = mutableListOf(
                PluginConfigurationImpl(
                    fqPluginName = "com.google.devsite.DevsitePlugin",
                    serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
                    values = DevsiteConfiguration(
                        docRootPath = docRootPath,
                        projectPath = inferredProjectPath,
                        excludedPackages = null,
                        excludedPackagesForJava = null,
                        excludedPackagesForKotlin = null,
                        libraryMetadataFilename = null,
                        javaDocsPath = javaDocsDirectory,
                        kotlinDocsPath = kotlinDocsDirectory
                    ).toJsonString()
                )
            )
        }

        val writerPlugin = TestOutputWriterPlugin()
        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                verifyOutput(writerPlugin, "$baseDir/docs")
            }
        }
    }
}
