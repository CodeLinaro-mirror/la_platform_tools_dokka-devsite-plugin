/*
 * Copyright (C) 2020 The Android Open Source Project
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
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

defaultTasks = mutableListOf("test", "jar", "shadowJar", "ktlint")

repositories {
    maven("../../prebuilts/androidx/external")
}

plugins {
    kotlin("jvm") version "1.4.30"
    id("com.github.johnrengelman.shadow") version "4.0.4"
    id("application")
}

application {
    mainClassName = "org.jetbrains.dokka.MainKt"
}

val dokkaVersion = "1.5.0-dev-104"

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.3.9")

    implementation("org.jetbrains.dokka:dokka-base-test-utils:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-base:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-cli:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.11.1")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.12")
    testImplementation("com.google.truth:truth:1.0.1")
    testImplementation("org.jetbrains.dokka:dokka-test-api:$dokkaVersion")
}

group = "com.google.devsite"
version = "0.0.7" // This is appended to archiveBaseName in the ShadowJar task.

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("dackka")
    archiveClassifier.set(null as String?)
    archiveVersion.set(null as String?)
    isZip64 = true
    destinationDirectory.set(getDistributionDirectory())
}

sourceSets.test {
    // TODO(b/168667502): use glob to compile all integ tests
    java.srcDirs(
        "testData/innerClasses/source",
        "testData/simple/source",
        "testData/topLevelFunctions/source",
        "testData/inheritance/source",
        "testData/sampleAnnotation/source"
        //"testData/fragment/source", // These integration tests aren't valid code because they
        //"testData/paging/source",   // have missing dependencies
        //"testData/compose/source",
        //"testData/complicatedPlatform/source"
    )
}

val zipTask = project.tasks.register<Zip>("zipResultsOf${name.capitalize()}") {
    destinationDirectory.set(File(getDistributionDirectory(), "host-test-reports"))
    archiveFileName.set("dackka-tests.zip")
}

tasks.withType<Test> {
    maxHeapSize = "1g"
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    testLogging.events = hashSetOf(
        TestLogEvent.FAILED,
        TestLogEvent.PASSED,
        TestLogEvent.SKIPPED,
        TestLogEvent.STANDARD_OUT,
        TestLogEvent.STANDARD_ERROR
    )

    if (isBuildingOnServer()) ignoreFailures = true
    finalizedBy(zipTask)
    doFirst {
        zipTask.configure {
            from(reports.junitXml.destination)
        }
    }
}

val ktlintConfiguration by configurations.creating
dependencies {
    ktlintConfiguration("com.pinterest:ktlint:0.33.0")
}

val outputDir = "${project.buildDir}/reports/ktlint/"
val inputFiles = project.fileTree(mapOf("dir" to "src", "include" to "**/*.kt"))

val ktlint by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Check Kotlin code style."
    group = "Verification"
    classpath = ktlintConfiguration
    main = "com.pinterest.ktlint.Main"
    args = listOf("src/**/*.kt")
}

val ktlintFormat by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Fix Kotlin code style deviations."
    group = "Formatting"
    classpath = ktlintConfiguration
    main = "com.pinterest.ktlint.Main"
    args = listOf("-F", "src/**/*.kt")
}

/**
 * The build server will copy the contents of the distribution directory and make it available for
 * download.
 */
fun getDistributionDirectory(): File {
    return if (System.getenv("DIST_DIR") != null) {
        File(System.getenv("DIST_DIR"))
    } else {
        File("out/dist")
    }
}

fun isBuildingOnServer(): Boolean {
    return System.getenv("OUT_DIR") != null && System.getenv("DIST_DIR") != null
}
