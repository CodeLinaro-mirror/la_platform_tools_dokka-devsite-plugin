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
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Plugin for Dackka integration test projects which configures a `writeSourceSets` task to write
 * the Dokka source set configuration for the project as JSON.
 *
 * Projects applying this plugin should also apply the [JavaLibraryPlugin].
 */
class DackkaTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.configureEach { plugin ->
            // Use the JavaLibraryPlugin to get source and classpath info for the project.
            val sourceSets =
                when (plugin) {
                    is JavaLibraryPlugin -> singleSourceSet(project)
                    else -> return@configureEach
                }
            WriteSourceSetsTask.setupTask(project, sourceSets)
        }
    }

    /**
     * Returns a list containing the single source set of a regular (non-KMP) project, using
     * [JavaPluginExtension] to find the source files and classpath.
     */
    fun singleSourceSet(project: Project): List<WriteSourceSetsTask.SourceSet> {
        val extension = project.extensions.getByType<JavaPluginExtension>()
        val projectSourceSet =
            extension.sourceSets.getByName(org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME)
        val projectClasspath = projectSourceSet.compileClasspath

        return listOf(
            WriteSourceSetsTask.SourceSet(
                name = "main",
                sourceRoots = projectSourceSet.java.sourceDirectories,
                classpath = projectClasspath,
                dependentSourceSets = emptyList(),
                analysisPlatform = "jvm",
            )
        )
    }
}
