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
    kotlin("multiplatform")
    id("dackka-test-plugin")
}

dackkaTest {
    hasSourceSamples = true
    createAndroidTarget = true
}

kotlin { jvm() }

val composeVersion = "1.7.8"
val composeMaterial3Version = "1.3.2"

dependencies {
    testArtifact("androidx.compose.animation:animation:$composeVersion")
    testArtifact("androidx.compose.animation:animation-core:$composeVersion")
    testArtifact("androidx.compose.animation:animation-graphics:$composeVersion")
    testArtifact("androidx.compose.foundation:foundation:$composeVersion")
    testArtifact("androidx.compose.foundation:foundation-layout:$composeVersion")
    testArtifact("androidx.compose.material3:material3:$composeMaterial3Version")
    testArtifact("androidx.compose.material3:material3-window-size-class:$composeMaterial3Version")
    testArtifact("androidx.compose.runtime:runtime:$composeVersion")
    testArtifact("androidx.compose.ui:ui:$composeVersion")
    testArtifact("androidx.compose.ui:ui-geometry:$composeVersion")
    testArtifact("androidx.compose.ui:ui-graphics:$composeVersion")
    testArtifact("androidx.compose.ui:ui-test:$composeVersion")
    testArtifact("androidx.compose.ui:ui-test-junit4:$composeVersion")
    testArtifact("androidx.compose.ui:ui-text:$composeVersion")
    testArtifact("androidx.compose.ui:ui-tooling:$composeVersion")
    testArtifact("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    testArtifact("androidx.compose.ui:ui-unit:$composeVersion")
    testArtifact("androidx.compose.ui:ui-util:$composeVersion")

    // The robolectric jars provide definitions for android dependencies.
    testClasspath(libs.robolectric.android)
    testClasspath(libs.robolectric.sandbox)

    // These artifacts are not declared as dependencies for any of the prebuilts used for this test,
    // but their APIs are referenced from docs.
    testClasspath("androidx.coordinatorlayout:coordinatorlayout:1.0.0")
    testClasspath("androidx.core:core:1.5.0")
}
