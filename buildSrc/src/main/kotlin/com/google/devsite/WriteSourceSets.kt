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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
import testApi.testRunner.DokkaSourceSetBuilder

/** Writes the [sourceSets] to [sourceSetOutputFile] as JSON formatted dokka source sets. */
abstract class WriteSourceSetsTask : DefaultTask() {
    /** The source sets of a project. */
    @get:Nested abstract val sourceSets: ListProperty<SourceSet>

    /**
     * The directory containing package lists used for tests. See [getExternalLinks] for the
     * expected package lists.
     */
    @get:InputDirectory abstract val packageListDir: DirectoryProperty

    /** A directory containing samples, if there are any. */
    @get:[InputDirectory Optional]
    abstract val sourceSamples: DirectoryProperty

    /** The file to write source sets as JSON. */
    @get:OutputFile abstract val sourceSetOutputFile: RegularFileProperty

    /**
     * Directory in which to store jars files unzipped from any aars included in the classpath of a
     * source set, because dackka can't process aars directly.
     */
    @get:OutputDirectory abstract val explodedAarsDir: DirectoryProperty

    @TaskAction
    fun run() {
        val externalLinks = getExternalLinks()

        // Create a mapping from source set name to source roots, filtering out any empty source
        // directories. If a source set has no non-empty source roots, it is not included.
        val sourceSetToRoots =
            sourceSets
                .get()
                .associate { sourceSet ->
                    sourceSet.name to
                        sourceSet.sourceRoots.files.filter { !it.listFiles().isNullOrEmpty() }
                }
                .filterValues { roots -> roots.isNotEmpty() }

        val dokkaSourceSets =
            sourceSets.get().mapNotNull { sourceSet ->
                // Find the non-empty source roots computed above. Do not create the source set if
                // there are no non-empty source roots.
                val sourceRootDirs = sourceSetToRoots[sourceSet.name] ?: return@mapNotNull null

                DokkaSourceSetBuilder(moduleName = "root")
                    .apply {
                        name = sourceSet.name
                        displayName = sourceSet.name

                        sourceRoots = sourceRootDirs.map { it.absolutePath }
                        classpath = listClasspath(sourceSet.classpath)

                        analysisPlatform = sourceSet.analysisPlatform
                        dependentSourceSets =
                            sourceSet.dependentSourceSets
                                // Don't include any source sets that don't have non-empty source
                                // roots, because they will not be included in the JSON.
                                .filter { it in sourceSetToRoots.keys }
                                .map { DokkaSourceSetID(scopeId = "root", sourceSetName = it) }
                                .toSet()

                        // Include samples for either the main source set of a regular JVM project
                        // or the commonMain source set of a KMP project (these are the two cases
                        // where a source set has no dependent source sets, because all other KMP
                        // source sets depend on commonMain).
                        samples =
                            if (sourceSet.dependentSourceSets.isEmpty()) {
                                listOfNotNull(
                                    sourceSamples.asFile.orNull
                                        ?.takeIf { it.exists() }
                                        ?.absolutePath
                                )
                            } else {
                                emptyList()
                            }

                        externalDocumentationLinks = externalLinks
                        documentedVisibilities =
                            setOf(
                                DokkaConfiguration.Visibility.PUBLIC,
                                DokkaConfiguration.Visibility.PROTECTED,
                            )
                    }
                    .build()
            }

        if (dokkaSourceSets.isEmpty()) {
            error("No source sets")
        }

        jacksonObjectMapper().writeValue(sourceSetOutputFile.asFile.get(), dokkaSourceSets)
    }

    /**
     * Returns the paths of all files which should be included in a classpath based on the input
     * [classpath]. Dackka cannot process aars, so all aar files are unzipped and the `classes.jar`
     * files inside is used instead (the unzipped jars are placed in [explodedAarsDir].
     *
     * In the AndroidX build the aar -> jar transformation is handled with AGP, but this build
     * doesn't depend on AGP.
     */
    fun listClasspath(classpath: FileCollection): List<String> {
        val (aars, other) = classpath.partition { it.extension == "aar" }

        val transformedAars =
            if (aars.isNotEmpty()) {
                val transformedAarDir = explodedAarsDir.asFile.get()
                transformedAarDir.mkdirs()

                // Take the `classes.jar` file from each aar, rename it based on the aar file name,
                // and store it in the exploded aars dir.
                aars.map { aar ->
                    val outputFile = File(transformedAarDir, aar.nameWithoutExtension + ".jar")
                    ZipInputStream(aar.inputStream()).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name.endsWith("classes.jar")) {
                                val fos = FileOutputStream(outputFile)
                                fos.write(zis.readBytes())
                                break
                            }
                            entry = zis.nextEntry
                        }
                        zis.closeEntry()
                        zis.close()
                    }
                    outputFile
                }
            } else {
                emptyList()
            }

        val classpathFiles = other + transformedAars
        return classpathFiles.map { it.absolutePath }
    }

    /** Returns the external link configuration for the package lists from [packageListDir]. */
    private fun getExternalLinks(): List<ExternalDocumentationLinkImpl> {
        return mapOf(
                "coroutines" to "https://kotlinlang.org/api/kotlinx.coroutines",
                "android" to "https://developer.android.com/reference",
                "guava" to "https://guava.dev/releases/18.0/api/docs/package-list",
                "kotlin" to "https://kotlinlang.org/api/core/kotlin-stdlib/",
            )
            .map {
                ExternalDocumentationLink(
                    url = URI(it.value).toURL(),
                    packageListUrl =
                        File(packageListDir.asFile.get(), "${it.key}/package-list").toURI().toURL(),
                )
            }
    }

    /** A source set input for the task. */
    data class SourceSet(
        /** The name of the source set. */
        @Input val name: String,
        /** The root directories of the source files. */
        @InputFiles val sourceRoots: FileCollection,
        /** The classpath (jars, aars, and klibs) for the source set. */
        @Classpath val classpath: FileCollection,
        /** The names of the other source sets which this one depends on. */
        @Internal val dependentSourceSets: List<String>,
        /** The Kotlin analysis platform (e.g. common, jvm, native). */
        @Input val analysisPlatform: String,
    )

    companion object {
        /** Configures a "writeSourceSets" task for the project based on [sourceSets]. */
        fun setupTask(
            project: Project,
            sourceSets: Provider<List<SourceSet>>,
            hasSourceSamples: Property<Boolean>,
        ) {
            project.tasks.register("writeSourceSets", WriteSourceSetsTask::class.java) { task ->
                task.packageListDir.set(project.layout.projectDirectory.dir("../package-lists"))
                task.sourceSetOutputFile.set(project.layout.buildDirectory.file("sourceSets.json"))
                task.sourceSets.set(sourceSets)
                // Provide the samples directory only when it is expected to exist.
                task.sourceSamples.set(
                    hasSourceSamples.map { hasSourceSamples ->
                        if (hasSourceSamples) {
                            project.layout.projectDirectory.dir("samples")
                        } else {
                            null
                        }
                    }
                )
                task.explodedAarsDir.set(project.layout.buildDirectory.dir("explodedAars"))
            }
        }
    }
}
