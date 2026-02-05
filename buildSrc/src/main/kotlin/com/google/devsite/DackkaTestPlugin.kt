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
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Plugin for Dackka integration test projects which configures a `writeSourceSets` task to write
 * the Dokka source set configuration for the project as JSON.
 *
 * Projects applying this plugin should also apply either the [JavaLibraryPlugin] (for regular jvm
 * projects) or the Kotlin multiplatform plugin (for KMP projects).
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
            // Use the JavaLibraryPlugin or KotlinMultiplatformPluginWrapper to get source and
            // classpath info for the project.
            val sourceSets =
                when (plugin) {
                    is JavaLibraryPlugin ->
                        singleSourceSet(project, dackkaTestExtension.hasSourceSamples)
                    is KotlinMultiplatformPluginWrapper ->
                        multiplatformSourceSets(
                            project,
                            dackkaTestExtension.hasSourceSamples,
                            dackkaTestExtension.createAndroidTarget,
                        )
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

    /**
     * Returns a list containing the source sets of a KMP project, using
     * [KotlinMultiplatformExtension] to find the source files and classpath.
     */
    fun multiplatformSourceSets(
        project: Project,
        hasSamples: Property<Boolean>,
        createAndroidTarget: Property<Boolean>,
    ): Provider<List<WriteSourceSetsTask.SourceSet>> {
        // Unzip the source jar for a prebuilts test, which contains all source sets.
        val artifactConfiguration = getArtifactConfiguration(project)
        val unzippedSourcesTask =
            SourceRootConfiguration.configureUnzipSources(
                project,
                artifactConfiguration,
                isKmp = true,
            )

        // List all main compilations.
        val kmpExtension = project.extensions.getByType<KotlinMultiplatformExtension>()
        val allCompilations = project.objects.listProperty<KotlinCompilation<*>>()
        kmpExtension.targets.configureEach { target ->
            val mainCompilation = target.compilations.named(KotlinCompilation.MAIN_COMPILATION_NAME)
            allCompilations.add(mainCompilation)
        }

        return allCompilations.zip(createAndroidTarget) { allCompilations, addAndroidTarget ->
            kmpExtension.sourceSets.mapNotNull { sourceSet ->
                // Only use main source sets.
                if (sourceSet.name.endsWith("Test")) return@mapNotNull null

                // Find all the compilations which this source set contributes to.
                val associatedCompilations =
                    allCompilations.filter { compilation ->
                        sourceSet in compilation.allKotlinSourceSets
                    }

                // For source tests, find the sources and classpath for the source set. The
                // classpath is aggregated from all compilations which the source set is part of.
                val projectSourceRoots =
                    SourceRootConfiguration.configureSources(
                        project,
                        sourceSet.kotlin.sourceDirectories,
                        hasSamples,
                        sourceSet.name,
                    )
                val projectClasspath =
                    associatedCompilations.fold<KotlinCompilation<*>, FileCollection>(
                        project.files()
                    ) { fc, compilation ->
                        fc + compilation.compileDependencyFiles
                    }

                // Compute source roots and classpath for a prebuilts test.
                val prebuiltsSourceRoots =
                    SourceRootConfiguration.sourceRootsForKmpPrebuilts(
                        project,
                        sourceSet.name,
                        unzippedSourcesTask,
                    ) +
                        // If this is the jvm source set and there needs to be a combined android
                        // target, also include the androidMain source root.
                        if (addAndroidTarget && sourceSet.name == "jvmMain") {
                            SourceRootConfiguration.sourceRootsForKmpPrebuilts(
                                project,
                                "androidMain",
                                unzippedSourcesTask,
                            )
                        } else {
                            project.files()
                        }

                val prebuiltsClasspath =
                    classpathForKmpPrebuilts(project, associatedCompilations) +
                        // If this is the jvm source set and there needs to be a combined android
                        // target, also find all dependencies with the android target type.
                        if (addAndroidTarget && sourceSet.name == "jvmMain") {
                            createClasspathFromArtifacts(
                                project,
                                artifactConfiguration,
                                multiplatformTargetType = KotlinPlatformType.androidJvm,
                            )
                        } else {
                            project.files()
                        }

                // Find which platform this source set should be considered. If it is part of
                // compilations of more than one platform type, it is treated as common.
                val allPlatforms =
                    associatedCompilations
                        .map {
                            // Android and jvm are the same type for Dackka's purposes.
                            when (val platformType = it.platformType) {
                                KotlinPlatformType.androidJvm -> KotlinPlatformType.jvm
                                else -> platformType
                            }
                        }
                        .toSet()
                val analysisPlatform = allPlatforms.singleOrNull()?.name ?: "common"

                // Combine the file collections for source and prebuilt test types in the source set
                WriteSourceSetsTask.SourceSet(
                    name = sourceSet.name,
                    sourceRoots = prebuiltsSourceRoots + projectSourceRoots.get(),
                    classpath = prebuiltsClasspath + projectClasspath,
                    dependentSourceSets = sourceSet.dependsOnTransitive().map { it.name },
                    analysisPlatform = analysisPlatform,
                )
            }
        }
    }

    /** Returns the transitive set of all source sets which this source set depends on. */
    private fun KotlinSourceSet.dependsOnTransitive(): Set<KotlinSourceSet> {
        val dependsOnSet = mutableSetOf<KotlinSourceSet>()
        fun processSourceSet(sourceSet: KotlinSourceSet) {
            if (sourceSet !in dependsOnSet) {
                dependsOnSet += sourceSet
                for (dependency in sourceSet.dependsOn) {
                    processSourceSet(dependency)
                }
            }
        }

        for (dependency in dependsOn) {
            processSourceSet(dependency)
        }
        return dependsOnSet
    }

    /**
     * Finds the classpath for a particular KMP source set for a prebuilts test based on the
     * [associatedCompilations] of the source set.
     */
    private fun classpathForKmpPrebuilts(
        project: Project,
        associatedCompilations: List<KotlinCompilation<*>>,
    ): FileCollection {
        val artifactConfiguration = getArtifactConfiguration(project)
        val associatedTargets = associatedCompilations.map { it.target }
        val associatedClasspaths =
            associatedTargets.mapNotNull { target ->
                // Skip the common metadata compilation.
                if (target.platformType == KotlinPlatformType.common) return@mapNotNull null
                createClasspathFromArtifacts(
                    project = project,
                    artifactConfiguration = artifactConfiguration,
                    multiplatformTargetType = target.platformType,
                    // Include the attributes associated with the target to get the correct files.
                    extraAttributes = target.attributes,
                )
            }
        // Join together classpaths for all targets associated with the source set.
        return associatedClasspaths.fold<FileCollection, FileCollection>(project.files()) {
            fc,
            compilation ->
            fc + compilation
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
     *
     * For a KMP project, the [multiplatformTargetType] should be included to use as an additional
     * attribute in the configuration. In addition, any provided [extraAttributes] associated with
     * the target will be used as well.
     */
    fun createClasspathFromArtifacts(
        project: Project,
        artifactConfiguration: Configuration,
        multiplatformTargetType: KotlinPlatformType? = null,
        extraAttributes: AttributeContainer? = null,
    ): FileCollection {
        // List both API and runtime dependencies. In theory only API dependencies should be
        // necessary to document the public API surface, but dependencies aren't always defined with
        // the correct api/implementation type.
        return createClasspathFromArtifactsForUsage(
            project = project,
            artifactConfiguration = artifactConfiguration,
            name = "runtime",
            jvmUsage = Usage.JAVA_RUNTIME,
            // There is not a corresponding runtime usage attribute for kotlin which works here.
            kotlinUsage = null,
            multiplatformTargetType = multiplatformTargetType,
            extraAttributes = extraAttributes,
        ) +
            createClasspathFromArtifactsForUsage(
                project = project,
                artifactConfiguration = artifactConfiguration,
                name = "api",
                jvmUsage = Usage.JAVA_API,
                kotlinUsage = "kotlin-api",
                multiplatformTargetType = multiplatformTargetType,
                extraAttributes = extraAttributes,
            )
    }

    /**
     * Returns the transitive dependencies of the [artifactConfiguration] prebuilts. If the
     * [multiplatformTargetType] is jvm/android or null, the [jvmUsage] is used as an attribute,
     * otherwise the [kotlinUsage] is used if it is non-null.
     *
     * The [name] should be a description of the [jvmUsage]/[kotlinUsage], used to disambiguate this
     * configuration from others.
     *
     * For a KMP project, the [multiplatformTargetType] should be included to use as an additional
     * attribute in the configuration. In addition, any provided [extraAttributes] associated with
     * the target will be used as well.
     */
    private fun createClasspathFromArtifactsForUsage(
        project: Project,
        artifactConfiguration: Configuration,
        name: String,
        jvmUsage: String,
        kotlinUsage: String?,
        multiplatformTargetType: KotlinPlatformType?,
        extraAttributes: AttributeContainer?,
    ): FileCollection {
        // Consider this a jvm target if it is not KMP, or android/jvm target type.
        val isJvm =
            multiplatformTargetType == null ||
                multiplatformTargetType == KotlinPlatformType.androidJvm ||
                multiplatformTargetType == KotlinPlatformType.jvm
        // The kotlinUsage is used for non-jvm targets, so if this is non-jvm and there is no
        // kotlinUsage, there is no associated classpath.
        if (kotlinUsage == null && !isJvm) return project.files()

        // Also include transitive dependencies defined as `testClasspath`.
        val testClasspath = getTestClasspathConfiguration(project)
        // It is not supported to have multiple compilation targets with the same type, so using the
        // type here should not result in any collisions.
        val configurationName = "testClasspath${multiplatformTargetType?.name.orEmpty()}-$name"
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

                val usage =
                    if (isJvm) {
                        jvmUsage
                    } else {
                        // This must be non-null, because there was a return above if the target is
                        // non-jvm and kotlinUsage is null.
                        kotlinUsage!!
                    }
                it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named<Usage>(usage))

                // Add more attributes for KMP projects.
                if (multiplatformTargetType != null) {
                    it.attribute(KotlinPlatformType.attribute, multiplatformTargetType)
                }
                // It seems like `addAllLater` should be a cleaner way to copy all the attributes
                // from one attribute container to another, but it is experimental and doesn't seem
                // to work here.
                extraAttributes?.keySet()?.forEach { key ->
                    val attributeValue = extraAttributes.getAttribute(key)!!
                    @Suppress("UNCHECKED_CAST")
                    it.attribute(key as Attribute<String>, attributeValue as String)
                }
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
