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

package com.google.devsite.transformers

import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel

/**
 * Provides common functionality for testing
 * [org.jetbrains.dokka.transformers.documentation.DocumentableTransformer]s.
 */
abstract class BaseTransformerTest :
    BaseAbstractTest(TestLogger(DokkaConsoleLogger(LoggingLevel.WARN))) {

    /** The configuration to use in tests if an override is not provided to [testTransformer]. */
    protected abstract val defaultConfiguration: DokkaConfigurationImpl

    /** Creates a configuration with a simple jvm source set and the [pluginsConfiguration]. */
    protected fun createDokkaConfiguration(
        pluginsConfiguration: MutableList<PluginConfigurationImpl>
    ): DokkaConfigurationImpl {
        return dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    analysisPlatform = "jvm"
                    classpath += jvmStdlibPath!!
                    // This defaults to false in the source set builder DSL, even though it defaults
                    // to true normally.
                    skipEmptyPackages = true
                }
            }
            pluginsConfigurations = pluginsConfiguration
        }
    }

    /**
     * Runs a [test] for the resultant module from transforming the [source] code based on the
     * [configuration].
     */
    fun testTransformer(
        source: String,
        configuration: DokkaConfigurationImpl = defaultConfiguration,
        test: (DModule) -> Unit,
    ) {
        testInline(
            source,
            configuration,
            // Skip the rendering step, stop after transformers are applied.
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = test
        }
    }
}
