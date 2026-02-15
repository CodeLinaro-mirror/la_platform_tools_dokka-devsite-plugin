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

dackkaTest {
    hasSourceSamples = true
}

val pagingVersion = "3.4.0"
dependencies {
    testArtifact("androidx.paging:paging-common:$pagingVersion")
    testArtifact("androidx.paging:paging-common-ktx:$pagingVersion")
    testArtifact("androidx.paging:paging-runtime:$pagingVersion")
    testArtifact("androidx.paging:paging-runtime-ktx:$pagingVersion")
    testArtifact("androidx.paging:paging-rxjava2:$pagingVersion")
    testArtifact("androidx.paging:paging-rxjava2-ktx:$pagingVersion")
    testArtifact("androidx.paging:paging-rxjava3:$pagingVersion")
    testArtifact("androidx.paging:paging-guava:$pagingVersion")

    // The robolectric jars provide definitions for android dependencies.
    testClasspath(libs.robolectric.android)
    testClasspath(libs.robolectric.sandbox)
}
