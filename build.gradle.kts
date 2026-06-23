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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.devsite.WriteSourceSetsTask
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

defaultTasks = mutableListOf("test", "jar", "shadowJar", "ktCheck", "publish", "zipTestResults")

group = "com.google.devsite"

version = "1.10.2" // This is appended to archiveBaseName in the ShadowJar task.

plugins {
    kotlin("jvm")
    alias(libs.plugins.shadow)
    alias(libs.plugins.ktfmt)
    id("application")
    id("maven-publish")
}

application { mainClass.set("org.jetbrains.dokka.MainKt") }

dependencies {
    implementation(libs.dokka.base)
    implementation(libs.dokka.core)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.module.kotlin)
    compileOnly(libs.dokka.analysis.api)
    runtimeOnly(libs.dokka.analysis.symbols)
    testImplementation(libs.dokka.base.test.utils) {
        exclude("org.jetbrains.dokka", "analysis-kotlin-descriptors")
    }

    implementation("org.jsoup:jsoup:1.16.2") // Remove when upstream updates their version

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.9.1")
    implementation(libs.kotlin.reflect)
    implementation(libs.coroutines.core)

    testImplementation(libs.kotlin.test)
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation(libs.dokka.test.api)

    implementation(libs.dokka.cli) // Used in CLI integration test

    implementation(libs.tracing)
    implementation(libs.tracing.wire)
}

val shadowJar =
    tasks.withType<ShadowJar> {
        archiveBaseName.set("dackka")
        isZip64 = true
        destinationDirectory.set(getDistributionDirectory())
    }

// Do not publish shadow jar to maven
val javaComponent = components["java"] as AdhocComponentWithVariants

javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }

tasks.withType<JavaCompile>().configureEach { options.release.set(11) }

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

val testTask =
    tasks.named<Test>("test") {
        dependsOn(tasks.withType<KotlinCompile>())
        // Ensure all source set information for integration tests is written out.
        subprojects { dependsOn(tasks.withType<WriteSourceSetsTask>()) }

        maxHeapSize = "16g"
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        testLogging.events =
            hashSetOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        if (isBuildingOnServer()) ignoreFailures = true
    }

val zipTask =
    project.tasks.register<Zip>("zipTestResults") {
        dependsOn(testTask)
        destinationDirectory.set(File(getDistributionDirectory(), "host-test-reports"))
        archiveFileName.set("dackka-tests.zip")
        from(project.file(testTask.flatMap { it.reports.junitXml.outputLocation }))
    }

ktfmt { kotlinLangStyle() }

publishing {
    publications {
        create<MavenPublication>(name = "Dackka") {
            from(components["java"])
            pom {
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers { developer { name.set("The Android Open Source Project") } }
                scm {
                    connection.set(
                        "scm:git:https://android.googlesource.com/platform/tools/dokka-devsite-plugin/"
                    )
                    url.set(
                        "https://android.googlesource.com/platform/tools/dokka-devsite-plugin//"
                    )
                }
            }
        }
    }

    repositories {
        maven { url = uri("file://${getDistributionDirectory().canonicalPath}/repo/repository") }
    }
}

/**
 * The build server will copy the contents of the distribution directory and make it available for
 * download.
 */
fun getDistributionDirectory(): File {
    return if (System.getenv("DIST_DIR") != null) {
        File(System.getenv("DIST_DIR"))
    } else {
        File(projectDir, "out/dist").apply { mkdirs() }
    }
}

fun isBuildingOnServer(): Boolean {
    return System.getenv("OUT_DIR") != null && System.getenv("DIST_DIR") != null
}
