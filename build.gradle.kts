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

group = "com.google.devsite"
version = "1.3.0" // This is appended to archiveBaseName in the ShadowJar task.

val dokkaVersion = "1.8.10-dev-203"
val kotlinVersion = "1.8.0"
val jacksonVersion = "2.13.1"
val coroutinesVersion = "1.6.3"
plugins {
    kotlin("jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "7.1.1"
    id("application")
    id("maven-publish")
}

application {
    mainClass.set("org.jetbrains.dokka.MainKt")
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-analysis:$dokkaVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")

    implementation("org.jetbrains.dokka:dokka-base-test-utils:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-base:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-cli:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("org.jetbrains.dokka:dokka-test-api:$dokkaVersion")
    testImplementation("org.mockito:mockito-inline:4.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
}

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

val kmpIntegrationSourceDirs = listOf("simple-kmp", "datastore-kmp")
// compose requires compose compiler plugin, fragment is fragmentary (missing internal dependencies)
val unCompilableIntegrationTestSourceDirs = listOf("collections-ktx", "compose", "fragment")
val (kmpSourceDirs, javaSourceDirs) = File("testData").listFiles()!!
    .mapNotNull { testDir -> testDir.listFiles()?.singleOrNull { "source" in it.name } }
    .filterNot { unCompilableIntegrationTestSourceDirs.any { badName -> badName in it.path  } }
    .partition { kmpIntegrationSourceDirs.any { badName -> badName in it.path  } }
val javaTestDataSS: SourceSet by sourceSets.creating {
    javaSourceDirs.forEach { java.srcDir(it) }
}
val kmpTestDataSSs: List<SourceSet> = kmpSourceDirs.flatMap {
    it.listFiles()!!.map { sourceSetDir ->
        val ss = sourceSets.maybeCreate(sourceSetDir.name)
        //ss.java.srcDir(sourceSetDir) // TODO(make KMP testData compile)
        ss
    }
}

val testDataImpl = project.configurations.getByName(javaTestDataSS.implementationConfigurationName)
val testDataAars by project.configurations.creating
val testDataParent by project.configurations.sourceArtifacts
testDataParent.isCanBeResolved = false
fun Configuration.setResolveSources(isKmp: Boolean = false) {
    isTransitive = false
    isCanBeConsumed = false
    attributes {
        attribute(
            Usage.USAGE_ATTRIBUTE,
            project.objects.named(if (isKmp) "androidx-multiplatform-docs" else Usage.JAVA_RUNTIME)
        )
        attribute(
            Category.CATEGORY_ATTRIBUTE,
            project.objects.named(Category.DOCUMENTATION)
        )
        attribute(
            DocsType.DOCS_TYPE_ATTRIBUTE,
            project.objects.named(DocsType.SOURCES)
        )
        attribute(
            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
            project.objects.named(LibraryElements.JAR)
        )
    }
}
testDataParent.setResolveSources()
val testDataSources by project.configurations.creating
testDataSources.extendsFrom(testDataParent)
testDataSources.setResolveSources()
val testDataSourcesKmp by project.configurations.creating
testDataSourcesKmp.extendsFrom(testDataParent)
testDataSourcesKmp.setResolveSources(isKmp = true)

val lifecycleVersion = "2.5.1"
val collectionsVersion = "1.3.0-alpha02"
val composeVersion = "1.4.0-alpha05"
val composeMaterial3Version = "1.1.0-alpha03"
dependencies {
    testDataImpl("io.reactivex.rxjava3:rxjava:3.0.0")
    testDataImpl("io.reactivex.rxjava2:rxjava:2.2.9")
    testDataImpl("org.robolectric:sandbox:4.8.1")
    testDataImpl("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
    testDataImpl("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$coroutinesVersion")
    testDataImpl("org.jetbrains.kotlinx:kotlinx-coroutines-rx3:$coroutinesVersion")
    testDataImpl("org.jetbrains.kotlinx:kotlinx-coroutines-guava:$coroutinesVersion")
    testDataImpl("org.robolectric:android-all-instrumented:12-robolectric-7732740-i4")
    testDataImpl("junit:junit:4.13.2")
    testDataImpl("com.google.truth:truth:1.1.3")
    testDataImpl("com.android.tools.build:gradle:8.0.0-alpha07")

    testDataImpl(fileTree("$buildDir/exploded"))

    testDataAars("androidx.lifecycle:lifecycle-livedata-core:2.4.0")
    testDataAars("androidx.lifecycle:lifecycle-viewmodel:2.4.0")
    testDataAars("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    testDataAars("androidx.lifecycle:lifecycle-livedata-ktx:2.4.0")
    testDataAars("androidx.recyclerview:recyclerview:1.2.1")
    testDataAars("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    testDataAars("androidx.compose.foundation:foundation:1.0.5")
    testDataAars("androidx.activity:activity:1.6.0-rc01")
    testDataAars("androidx.paging:paging-common:3.2.0-alpha02")

    testDataSources("androidx.fragment:fragment:1.6.0-alpha01")
    // TODO: publish sample source code in a way accessible to dackka (/studio) b/153171116
    // testDataSources("androidx.fragment:fragment-samples:1.6.0-alpha01")
    testDataSources("androidx.lifecycle:lifecycle-common:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-compiler:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-livedata:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-livedata-core:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-livedata-core-ktx:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-process:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-reactivestreams:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-reactivestreams-ktx:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-runtime:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-runtime-testing:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-viewmodel:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")
    testDataSources("androidx.activity:activity:1.6.0-beta01")
    testDataSources("androidx.activity:activity-ktx:1.6.0-beta01")
    testDataSources("androidx.ads:ads-identifier:1.0.0-alpha04")
    testDataSources("androidx.ads:ads-identifier-common:1.0.0-alpha04")
    testDataSources("androidx.ads:ads-identifier-provider:1.0.0-alpha04")
    testDataSourcesKmp("androidx.annotation:annotation:1.6.0-alpha01")
    testDataSources("androidx.annotation:annotation-experimental:1.3.0")
    //testDataSources("androidx.annotation:annotation-experimental-lint:1.0.0-rc01") // need dep
    testDataSources("androidx.appcompat:appcompat:1.6.0-beta01")
    testDataSources("androidx.appcompat:appcompat-resources:1.6.0-beta01")
    testDataSources("androidx.appsearch:appsearch:1.1.0-alpha01")
    testDataSources("androidx.appsearch:appsearch-builtin-types:1.1.0-alpha01")
    testDataSources("androidx.appsearch:appsearch-compiler:1.1.0-alpha01")
    testDataSources("androidx.appsearch:appsearch-ktx:1.1.0-alpha01")
    testDataSources("androidx.appsearch:appsearch-debug-view:1.1.0-alpha01")
    testDataSources("androidx.appsearch:appsearch-platform-storage:1.1.0-alpha01")
    testDataSources("androidx.appsearch:appsearch-local-storage:1.1.0-alpha01")
    testDataSources("androidx.arch.core:core-common:2.1.0")
    testDataSources("androidx.arch.core:core-runtime:2.1.0")
    testDataSources("androidx.arch.core:core-testing:2.1.0")
    testDataSources("androidx.asynclayoutinflater:asynclayoutinflater:1.0.0")
    testDataSources("androidx.autofill:autofill:1.2.0-beta01")
    testDataSources("androidx.benchmark:benchmark:1.0.0-alpha03")
    testDataSources("androidx.benchmark:benchmark-common:1.2.0-alpha01")
    testDataSources("androidx.benchmark:benchmark-gradle-plugin:1.2.0-alpha01")
    testDataImpl(project.files("" + project.gradle.gradleHomeDir + "/lib/gradle-kotlin-dsl-" +
        project.gradle.gradleVersion + ".jar")) // Needed for benchmark-gradle-plugin
    // testDataImpl(gradleKotlinDslOf(project)) // should be equivalent to ^^ but does not work
    testDataImpl(gradleApi())
    testDataSources("androidx.benchmark:benchmark-junit4:1.2.0-alpha01")
    testDataSources("androidx.benchmark:benchmark-macro:1.2.0-alpha01")
    testDataSources("androidx.benchmark:benchmark-macro-junit4:1.2.0-alpha01")
    testDataSources("androidx.biometric:biometric:1.2.0-alpha04")
    testDataSources("androidx.biometric:biometric-ktx:1.2.0-alpha04")
    testDataSources("androidx.browser:browser:1.4.0")
    testDataSources("androidx.camera:camera-camera2:1.2.0-alpha04")
    testDataSources("androidx.camera:camera-camera2-pipe:1.0.0-alpha01")
    testDataSources("androidx.camera:camera-camera2-pipe-testing:1.0.0-alpha01")
    testDataSources("androidx.camera:camera-core:1.2.0-alpha04")
    testDataSources("androidx.camera:camera-extensions:1.2.0-alpha04")
    testDataSources("androidx.camera:camera-lifecycle:1.2.0-alpha04")
    testDataSources("androidx.camera:camera-mlkit-vision:1.2.0-alpha04")
    testDataAars("com.google.mlkit:vision-interfaces:16.0.0")
    testDataSources("androidx.camera:camera-extensions:1.2.0-alpha04")
    testDataSources("androidx.camera:camera-previewview:1.1.0-beta02")
    testDataSources("androidx.camera:camera-video:1.2.0-alpha04")
    testDataSources("androidx.camera:camera-view:1.2.0-alpha04")
    testDataSources("androidx.camera:camera-viewfinder:1.2.0-alpha04")
    testDataSources("androidx.car.app:app:1.3.0-alpha01")
    testDataSources("androidx.car.app:app-aaos:1.0.0-alpha01")
    testDataSources("androidx.car.app:app-automotive:1.3.0-alpha01")
    testDataSources("androidx.car.app:app-projected:1.3.0-alpha01")
    testDataSources("androidx.car.app:app-testing:1.3.0-alpha01")
    // testDataSources("androidx.car:car:1.0.0-alpha7") // Obsolete artifacts
    // testDataSources("androidx.car:car-cluster:1.0.0-alpha5")
    // testDataSources("androidx.car:car-moderator:1.0.0-alpha1")
    testDataSources("androidx.cardview:cardview:1.0.0")
    // We do not test against androidx.test, because they are not part of the androidx build
    // and also publish source jars with problematic no-write-permission on parts
    testDataAars("androidx.test.uiautomator:uiautomator:2.2.0") // no source jar
    testDataSources("androidx.tracing:tracing:1.2.0-alpha01")
    testDataSources("androidx.tracing:tracing-ktx:1.2.0-alpha01")
    testDataSources("androidx.tracing:tracing-perfetto:1.0.0-alpha01")
    testDataSources("androidx.tracing:tracing-perfetto-binary:1.0.0-alpha02")
    testDataSources("androidx.tracing:tracing-perfetto-common:1.0.0-alpha01")

    testDataSources("androidx.paging:paging-common:3.2.0-alpha02")
    testDataSources("androidx.paging:paging-common-ktx:3.2.0-alpha02")
    testDataSources("androidx.paging:paging-runtime:3.2.0-alpha02")
    testDataSources("androidx.paging:paging-runtime-ktx:3.2.0-alpha02")
    testDataSources("androidx.paging:paging-rxjava2:3.2.0-alpha02")
    testDataSources("androidx.paging:paging-rxjava2-ktx:3.2.0-alpha02")
    testDataSources("androidx.paging:paging-rxjava3:3.2.0-alpha02")
    testDataSources("androidx.paging:paging-guava:3.2.0-alpha02")
    testDataSources("androidx.paging:paging-compose:1.0.0-alpha16")

    // Compose is KMP, but they don't publish KMP source jars
    testDataSources("androidx.compose.animation:animation:$composeVersion")
    testDataSources("androidx.compose.animation:animation-core:$composeVersion")
    testDataSources("androidx.compose.animation:animation-graphics:$composeVersion")
    testDataSources("androidx.compose.foundation:foundation:$composeVersion")
    testDataSources("androidx.compose.foundation:foundation-layout:$composeVersion")
    testDataSources("androidx.compose.material3:material3:$composeMaterial3Version")
    testDataSources("androidx.compose.material3:material3-window-size-class:$composeMaterial3Version")
    testDataSources("androidx.compose.runtime:runtime:$composeVersion")
    testDataSources("androidx.compose.ui:ui:$composeVersion")
    testDataSources("androidx.compose.ui:ui-geometry:$composeVersion")
    testDataSources("androidx.compose.ui:ui-graphics:$composeVersion")
    testDataSources("androidx.compose.ui:ui-text:$composeVersion")
    testDataSources("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    testDataSources("androidx.compose.ui:ui-unit:$composeVersion")
    testDataSources("androidx.compose.ui:ui-util:$composeVersion")

    testDataSourcesKmp("androidx.collection:collection:$collectionsVersion")
    testDataSourcesKmp("androidx.datastore:datastore-core:1.1.0-alpha01")
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

val explodeSources by tasks.registering {
    (testDataSources.files + testDataSourcesKmp.files).filter {
        it.nameWithoutExtension.endsWith("sources")
    }.forEach { arch ->
        sync {
            val splitName = arch.nameWithoutExtension.split("-")
            val versionInd =
                splitName.indexOfFirst { '.' in it } // index of first block in version num
            val baseName = splitName.subList(0, versionInd).joinToString(separator = "-")
            logger.debug("Unzipping prebuilt for $baseName")
            from(zipTree(arch))
            into("$buildDir/explodedSources/$baseName")
        }
    }
}

val classpathForTests by tasks.registering(ClasspathForTestsTask::class) {
    dependsOn(explodeAars)
    dependsOn(explodeSources)
    classpath = javaTestDataSS.compileClasspath
    kmpTestDataSSs.forEach { classpath += it.compileClasspath }
    location.set(file("testData/classpath.txt"))
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-Xmulti-platform"
    }
    dependsOn(explodeAars)
}

tasks.getByName("test") {
    dependsOn(classpathForTests)
    dependsOn(tasks.withType<KotlinCompile>())
}

val zipTask = project.tasks.register<Zip>("zipResultsOf${name.capitalize()}") {
    destinationDirectory.set(File(getDistributionDirectory(), "host-test-reports"))
    archiveFileName.set("dackka-tests.zip")
}

tasks.withType<Test> {
    maxHeapSize = "4g"
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
