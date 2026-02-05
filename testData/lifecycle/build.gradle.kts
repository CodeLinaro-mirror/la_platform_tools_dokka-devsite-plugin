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

plugins {
    id("dackka-test-plugin")
    `java-library`
}

val lifecycleVersion = "2.6.0"
dependencies {
    testArtifact("androidx.lifecycle:lifecycle-common:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-compiler:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-livedata:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-livedata-core:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-livedata-core-ktx:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-process:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-reactivestreams:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-reactivestreams-ktx:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-runtime:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-runtime-testing:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-viewmodel:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    testArtifact("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")

    // The robolectric jars provide definitions for android dependencies.
    testClasspath(libs.robolectric.android)
    testClasspath(libs.robolectric.sandbox)

    // These libraries are referenced from the lifecycle docs, but are not declared as dependencies.
    testClasspath("io.reactivex.rxjava2:rxjava:2.2.9")
    testClasspath("androidx.fragment:fragment:1.6.0")
}
