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

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

/** Extension to define values needed to configure source sets for a dackka test project. */
abstract class DackkaTestExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * Whether the project has samples files as sources (not as prebuilts). The samples are expected
     * to be in a `samples` subdirectory of the project.
     */
    val hasSourceSamples: Property<Boolean> = objects.property<Boolean>().convention(false)

    /**
     * Whether to create a fake android target for a KMP test, which is merged into a jvm source
     * set. The reason for not using a real android target is because it would require using AGP and
     * having an android sdk as part of the dackka checkout (robolectric jars are used as fake
     * android dependencies where they are needed). The android source set is combined with the jvm
     * source set because dackka does not treat them as different platform types.
     */
    val createAndroidTarget: Property<Boolean> = objects.property<Boolean>().convention(false)
}
