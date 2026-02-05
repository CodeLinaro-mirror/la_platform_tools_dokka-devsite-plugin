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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

/**
 * Plugin for Dackka integration test projects which configures a `writeSourceSets` task to write
 * the Dokka source set configuration for the project as JSON.
 *
 * Projects applying this plugin should also apply the [JavaLibraryPlugin].
 *
 * A test project can either be from sources or from prebuilts.
 * * A sources test will use the source files of the gradle project.
 * * For a prebuilts test, the build file should include dependencies with the type `testArtifact`.
 *   The source jars for these dependencies will be used to generate the docs. The classpath will be
 *   built from the transitive dependencies of the test artifacts, but the `testClasspath`
 *   dependency type can be used to include additional files in the classpath (for instance Android
 *   jars).
 */
class DackkaTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val dackkaTestExtension =
            project.extensions.create("dackkaTest", DackkaTestExtension::class.java)

        // Define the `testArtifact` and `testClasspath` dependency types eagerly to avoid errors
        // compiling the build files.
        getArtifactConfiguration(project)
        getTestClasspathConfiguration(project)

        project.plugins.configureEach { plugin ->
            // Use the JavaLibraryPlugin to get source and classpath info for the project.
            val sourceSets =
                when (plugin) {
                    is JavaLibraryPlugin ->
                        singleSourceSet(project, dackkaTestExtension.hasSourceSamples)
                    else -> return@configureEach
                }
            WriteSourceSetsTask.setupTask(project, sourceSets, dackkaTestExtension.hasSourceSamples)
        }
    }

    /**
     * Returns a list containing the single source set of a regular (non-KMP) project, using
     * [JavaPluginExtension] to find the source files and classpath.
     */
    fun singleSourceSet(
        project: Project,
        hasSamples: Property<Boolean>,
    ): Provider<List<WriteSourceSetsTask.SourceSet>> {
        // For source tests, find the sources and classpath from the java extension.
        val extension = project.extensions.getByType<JavaPluginExtension>()
        val projectSourceSet =
            extension.sourceSets.getByName(org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME)
        val projectSourceRoots =
            SourceRootConfiguration.configureSources(
                project,
                projectSourceSet.java.sourceDirectories,
                hasSamples,
            )
        val projectClasspath = projectSourceSet.compileClasspath

        // For prebuilts test, unzip the source jars and find the classpath for the testArtifacts.
        val artifactConfiguration = getArtifactConfiguration(project)
        val unzippedSourcesTask =
            SourceRootConfiguration.configureUnzipSources(project, artifactConfiguration)
        val prebuiltsClasspath = createClasspathFromArtifacts(project, artifactConfiguration)

        // Combine the file collections for source and prebuilt test types in the source set.
        return projectSourceRoots.map { projectSourceRoots ->
            listOf(
                WriteSourceSetsTask.SourceSet(
                    name = "main",
                    sourceRoots = projectSourceRoots + project.files(unzippedSourcesTask),
                    classpath = projectClasspath + prebuiltsClasspath,
                    dependentSourceSets = emptyList(),
                    analysisPlatform = "jvm",
                )
            )
        }
    }

    /** Retrieves the [Configuration] for the `testArtifact` dependency type. */
    fun getArtifactConfiguration(project: Project): Configuration {
        return project.getOrCreateConfiguration("testArtifact") {
            it.isCanBeResolved = false
            it.isCanBeConsumed = false
        }
    }

    /** Retrieves the [Configuration] for the `testClasspath` dependency type. */
    fun getTestClasspathConfiguration(project: Project): Configuration {
        return project.getOrCreateConfiguration("testClasspath")
    }

    /**
     * Given an [artifactConfiguration] of prebuilts to use for the test, returns the transitive
     * dependencies required for the classpath.
     */
    fun createClasspathFromArtifacts(
        project: Project,
        artifactConfiguration: Configuration,
    ): FileCollection {
        // List both API and runtime dependencies. In theory only API dependencies should be
        // necessary to document the public API surface, but dependencies aren't always defined with
        // the correct api/implementation type.
        return createClasspathFromArtifactsForUsage(
            project = project,
            artifactConfiguration = artifactConfiguration,
            name = "runtime",
            usage = Usage.JAVA_RUNTIME,
        ) +
            createClasspathFromArtifactsForUsage(
                project = project,
                artifactConfiguration = artifactConfiguration,
                name = "api",
                usage = Usage.JAVA_API,
            )
    }

    /**
     * Returns the transitive dependencies of the [artifactConfiguration] prebuilts, with the
     * specified [usage]. The [name] should be a description of the [usage], used to disambiguate
     * this configuration from others.
     */
    private fun createClasspathFromArtifactsForUsage(
        project: Project,
        artifactConfiguration: Configuration,
        name: String,
        usage: String,
    ): FileCollection {
        // Also include transitive dependencies defined as `testClasspath`.
        val testClasspath = getTestClasspathConfiguration(project)
        val configurationName = "testClasspath-$name"
        return project.getOrCreateConfiguration(configurationName) { config ->
            config.extendsFrom(artifactConfiguration)
            config.extendsFrom(testClasspath)
            config.isCanBeConsumed = false
            config.isTransitive = true
            config.isCanBeResolved = true
            config.attributes {
                it.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    project.objects.named<Category>(Category.LIBRARY),
                )

                it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named<Usage>(usage))
            }
        }
    }
}

/**
 * Searches for a configuration with the given [name] in the project. If it doesn't already exist,
 * creates it with the [action].
 */
fun Project.getOrCreateConfiguration(
    name: String,
    action: (Configuration) -> Unit = {},
): Configuration {
    return project.configurations.findByName(name) ?: project.configurations.create(name, action)
}
