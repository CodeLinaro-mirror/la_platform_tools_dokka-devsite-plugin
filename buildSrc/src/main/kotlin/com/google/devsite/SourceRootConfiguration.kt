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

package com.google.devsite

import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named

/** Utilities to configure the source roots of a source set. */
object SourceRootConfiguration {
    /**
     * For a test [project] which uses source files (as opposed to prebuilts) listed in [sources],
     * returns the source roots which should be used. This is based on whether [hasSourceSamples] is
     * true: if it is, the samples tags in the sources need to be rewritten.
     */
    fun configureSources(
        project: Project,
        sources: FileCollection,
        hasSourceSamples: Property<Boolean>,
    ): Provider<FileCollection> {
        // Sets up a task to rewrite samples tags (see [rewriteSamplesTags]). This will only be run
        // if the project has samples.
        val rewriteTask =
            project.tasks.register("rewriteSamplesTags", Sync::class.java) { task ->
                task.from(sources)
                task.into(project.layout.buildDirectory.dir("rewrittenSources"))
                task.rewriteSamplesTags()
            }
        // If there are samples, use the rewritten sources, otherwise, use the original.
        return hasSourceSamples.map { hasSourceSamples ->
            if (hasSourceSamples) {
                project.files(rewriteTask)
            } else {
                sources
            }
        }
    }

    /** Unzips the source jars for the prebuilts from [artifactConfiguration]. */
    fun configureUnzipSources(
        project: Project,
        artifactConfiguration: Configuration,
        isKmp: Boolean = false,
    ): TaskProvider<Sync> {
        val sourcesConfiguration =
            project.getOrCreateConfiguration("test-sources") { configuration ->
                configuration.extendsFrom(artifactConfiguration)
                configuration.isTransitive = false
                configuration.isCanBeConsumed = false
                configuration.isCanBeResolved = true
                configuration.attributes {
                    it.attribute(
                        Usage.USAGE_ATTRIBUTE,
                        project.objects.named(
                            if (isKmp) "androidx-multiplatform-docs" else Usage.JAVA_RUNTIME
                        ),
                    )
                    it.attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.objects.named<Category>(Category.DOCUMENTATION),
                    )
                    it.attribute(
                        DocsType.DOCS_TYPE_ATTRIBUTE,
                        project.objects.named<DocsType>(DocsType.SOURCES),
                    )
                    it.attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        project.objects.named<LibraryElements>(LibraryElements.JAR),
                    )
                }
            }

        // When unzipping the source jars, exclude the META-INF directory, which isn't needed for
        // docs and will cause duplicate files between source jars.
        val jars =
            sourcesConfiguration.incoming
                .artifactView {}
                .files
                .elements
                .map { fileLocations ->
                    fileLocations.map { fileLocation ->
                        project.zipTree(fileLocation.asFile).matching { it.exclude("META-INF/") }
                    }
                }

        return project.tasks.register("unzipSources", Sync::class.java) { task ->
            task.into(project.layout.buildDirectory.dir("source"))
            task.from(jars)
            task.rewriteSamplesTags()
        }
    }

    /**
     * Based on the [sourceSetName], returns the source root subdirectory from the
     * [unzippedSourcesTask] output.
     */
    fun sourceRootsForKmpPrebuilts(
        project: Project,
        sourceSetName: String,
        unzippedSourcesTask: TaskProvider<Sync>,
    ): FileCollection {
        return project.files(
            unzippedSourcesTask.map { unzippedSourcesTask ->
                File(unzippedSourcesTask.destinationDir, sourceSetName)
            }
        )
    }

    /**
     * To work around a parser issue with `@sample` where when the tag is used in the middle of a
     * kdoc any links after the sample do not resolve (see b/427708573), rewrite `@sample` tags to
     * `@author #@sample`. The `@author` tag is not supported by dackka, so as a workaround for the
     * samples issue it replaces any author tags with samples.
     */
    internal fun CopySpec.rewriteSamplesTags() {
        filter { line -> line.replace(" * @sample ", " * @author #@sample ") }
    }
}
