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

defaultTasks = mutableListOf("test", "jar", "shadowJar", "ktlint", "publish")

repositories {
    maven("../../prebuilts/androidx/external")
    maven("../../prebuilts/androidx/internal")
}

plugins {
    kotlin("jvm") version "1.6.21"
    id("com.github.johnrengelman.shadow") version "7.1.1"
    id("application")
    id("maven-publish")
}

application {
    mainClass.set("org.jetbrains.dokka.MainKt")
}
val dokkaVersion = "1.6.20-dev-154"
val jacksonVersion = "2.13.1"

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.0-native-mt")

    implementation("org.jetbrains.dokka:dokka-base-test-utils:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-base:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-cli:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("org.jetbrains.dokka:dokka-test-api:$dokkaVersion")
    testImplementation("org.mockito:mockito-inline:4.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
}

group = "com.google.devsite"
version = "0.0.17" // This is appended to archiveBaseName in the ShadowJar task.

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

val shadowJar = tasks.withType<ShadowJar> {
    archiveBaseName.set("dackka")
    isZip64 = true
    destinationDirectory.set(getDistributionDirectory())
}

// Do not publish shadow jar to maven
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
    skip()
}

val testData by sourceSets.creating {
    java.srcDirs(
        "testData/innerClasses/source",
        "testData/simple/source",
        "testData/topLevelFunctions/source",
        "testData/linking/source",
        "testData/inheritance/source",
        "testData/sampleAnnotation/source",
        "testData/annotations/source",
        "testData/restrictTo/source",
        "testData/getterSetterModifier/source/",
        "testData/fragment/source",
        "testData/paging/source",
//            "testData/compose/source", // this project seems to require multiplatform build
        "testData/complicatedPlatform/source"
    )
}

val testDataImpl = project.configurations.getByName(testData.implementationConfigurationName)
val testDataAars by project.configurations.creating


dependencies {
    testDataImpl("io.reactivex.rxjava3:rxjava:3.0.0")
    testDataImpl("io.reactivex.rxjava2:rxjava:2.2.9")
    testDataImpl("org.robolectric:sandbox:4.7.3")
    testDataImpl("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.2")
    testDataImpl("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.5.2")
    testDataImpl("org.jetbrains.kotlinx:kotlinx-coroutines-rx3:1.5.2")
    testDataImpl("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.5.2")
    testDataImpl("org.robolectric:android-all-instrumented:12-robolectric-7732740-i3")

    testDataImpl(fileTree("$buildDir/exploded"))

    testDataAars("androidx.lifecycle:lifecycle-livedata-core:2.4.0")
    testDataAars("androidx.lifecycle:lifecycle-viewmodel:2.4.0")
    testDataAars("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    testDataAars("androidx.lifecycle:lifecycle-livedata-ktx:2.4.0")
    testDataAars("androidx.recyclerview:recyclerview:1.2.1")
    testDataAars("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    testDataAars("androidx.compose.foundation:foundation:1.0.5")
}

val explodeAars by tasks.registering(Sync::class) {
    into("$buildDir/exploded")
    from(testDataAars) {
        include("*.jar")
    }

    testDataAars.files.filter { it.extension == "aar" }.forEach { arch ->
        from(zipTree(arch)) {
            include("classes.jar")
            rename { arch.nameWithoutExtension + ".jar" }
        }
    }
}

val classpathForTests by tasks.registering(ClasspathForTestsTask::class) {
    dependsOn(explodeAars)
    classpath = testData.compileClasspath
    location.set(file("testData/classpath.txt"))
}

val compileTestDataKotlin: KotlinCompile by tasks.getting(KotlinCompile::class) {
    kotlinOptions {
        // we are only checking if the classpath is complete
        //freeCompilerArgs += "-Xdisable-phases=Codegen"
    }
    dependsOn(explodeAars)
}

tasks.getByName("test") {
    dependsOn(classpathForTests)
    dependsOn(compileTestDataKotlin) // this will check classpath for all needed dependencies
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
            from(reports.junitXml.outputLocation)
        }
    }
}

val ktlintConfiguration: Configuration by configurations.creating
dependencies {
    ktlintConfiguration("com.pinterest:ktlint:0.43.0")
}

val outputDir = "${project.buildDir}/reports/ktlint/"
val inputFiles = project.fileTree(mapOf("dir" to "src", "include" to "**/*.kt"))

val ktlint by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Check Kotlin code style."
    group = "Verification"
    classpath = ktlintConfiguration
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("src/**/*.kt")
}

val ktlintFormat by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Fix Kotlin code style deviations."
    group = "Formatting"
    classpath = ktlintConfiguration
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("-F", "src/**/*.kt")
}

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
                developers {
                    developer {
                        name.set("The Android Open Source Project")
                    }
                }
                scm {
                    connection.set("scm:git:https://android.googlesource.com/platform/tools/dokka-devsite-plugin/")
                    url.set("https://android.googlesource.com/platform/tools/dokka-devsite-plugin//")
                }
            }
        }
    }

    repositories {
        maven {
            url = uri("file://${getDistributionDirectory().canonicalPath}/repo/repository")
        }
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

abstract class ClasspathForTestsTask: DefaultTask() {
    @get:Classpath abstract var classpath: FileCollection
    @get:OutputFile abstract val location: RegularFileProperty

    @TaskAction
    fun run(){
        location.get().asFile.writeText(classpath.joinToString(separator = "\n"))
    }
}
