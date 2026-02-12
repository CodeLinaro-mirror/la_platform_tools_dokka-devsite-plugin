/*
 * Copyright 2024 The Android Open Source Project
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

import com.google.devsite.DevsiteConfiguration
import com.google.devsite.renderer.converters.allAnnotations
import com.google.devsite.renderer.converters.companion
import com.google.devsite.testing.ConverterTestBase
import com.google.devsite.testing.defaultPluginsConfiguration
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.transformers.documentables.isDeprecated
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.toCompactJsonString
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.junit.Test

class PropagatedAnnotationsTransformerTest :
    BaseAbstractTest(TestLogger(DokkaConsoleLogger(LoggingLevel.WARN))) {
    private val configuration = createConfiguration(defaultPluginsConfiguration)

    private fun createConfiguration(
        plugins: MutableList<PluginConfigurationImpl>
    ): DokkaConfigurationImpl {
        return dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    analysisPlatform = "jvm"
                    classpath += jvmStdlibPath!!
                }
            }
            pluginsConfigurations = plugins
        }
    }

    @Test
    fun `test propagation from class to members`() {
        testInline(
            """
            /src/com/sample/Foo.kt
            package com.sample
            @Deprecated
            class Foo {
                fun foo() = Unit
                val v = 0
                companion object
                inner class Bar
            }
            """
                .trimIndent(),
            configuration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                val fooClass = mod.packages.single().classlikes.single() as DClass
                assertTrue(fooClass.isDeprecated())
                assertTrue(fooClass.constructors.single().isDeprecated())
                assertTrue(fooClass.functions.single().isDeprecated())
                assertTrue(fooClass.properties.single().isDeprecated())
                assertTrue(fooClass.companion()!!.isDeprecated())
                assertTrue(
                    (fooClass.classlikes.single { it.name == "Bar" } as DClass).isDeprecated()
                )
            }
        }
    }

    @Test
    fun `test propagation from properties to accessors`() {
        testInline(
            """
            /src/com/sample/Foo.kt
            package com.sample
            @Deprecated
            var v = 0
            """
                .trimIndent(),
            configuration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                val pkg = mod.packages.single()
                val topLevelProperty = pkg.properties.single()
                assertTrue(topLevelProperty.isDeprecated())
                assertTrue(topLevelProperty.getter!!.isDeprecated())
                assertTrue(topLevelProperty.setter!!.isDeprecated())
            }
        }
    }

    @Test
    fun `test propagation from enum to entries`() {
        testInline(
            """
            /src/com/sample/Foo.kt
            package com.sample
            @Deprecated
            enum class Foo {
                ONE
            }
            """
                .trimIndent(),
            configuration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                val fooEnum = mod.packages.single().classlikes.single() as DEnum
                assertTrue(fooEnum.isDeprecated())
                assertTrue(fooEnum.entries.single().isDeprecated())
            }
        }
    }

    @Test
    fun `test propagation from package to members`() {
        testInline(
            """
            /src/com/sample/package-info.java
            @Deprecated
            package com.sample;
            /src/com/sample/Foo.kt
            package com.sample
            class Foo
            val v = 0
            fun foo() = Unit
            """
                .trimIndent(),
            configuration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                val pkg = mod.packages.single()
                assertTrue(pkg.isDeprecated())
                assertTrue((pkg.classlikes.single() as DClass).isDeprecated())
                assertTrue(pkg.properties.single().isDeprecated())
                assertTrue(pkg.functions.single().isDeprecated())
            }
        }
    }

    @Test
    fun `test multiple annotations propagated`() {
        // Create custom configuration with the annotations to propagate
        val customConfiguration =
            createConfiguration(
                mutableListOf(
                    PluginConfigurationImpl(
                        fqPluginName = "com.google.devsite.DevsitePlugin",
                        serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
                        values =
                            DevsiteConfiguration(
                                    docRootPath = "reference",
                                    projectPath = "androidx",
                                    excludedPackages = null,
                                    excludedPackagesForJava = null,
                                    excludedPackagesForKotlin = null,
                                    libraryMetadataFilename = null,
                                    versionMetadataFilenames = null,
                                    javaDocsPath = "",
                                    kotlinDocsPath = "kotlin",
                                    packagePrefixToRemoveInToc = null,
                                    baseSourceLink = null,
                                    basePropertySourceLink = null,
                                    baseFunctionSourceLink = null,
                                    annotationsNotToDisplay = null,
                                    annotationsNotToDisplayJava = null,
                                    annotationsNotToDisplayKotlin = null,
                                    hidingAnnotations = emptyList(),
                                    propagatingAnnotations =
                                        listOf("com.sample.A", "com.sample.B", "com.sample.C"),
                                )
                                .toCompactJsonString(),
                    )
                )
            )
        testInline(
            """
            /src/com/sample/Foo.kt
            package com.sample
            annotation class A
            annotation class B
            annotation class C
            @A
            class Foo {
                fun foo() = Unit
                @B
                @C
                inner class Bar {
                    fun bar() = Unit
                }
            }
            """
                .trimIndent(),
            customConfiguration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                fun checkAnnotations(d: Documentable, expected: List<String>) {
                    assertContentEquals(
                        expected.sorted(),
                        d.allAnnotations().map { it.dri.classNames!! }.sorted(),
                    )
                }
                val fooClass = mod.packages.single().classlikes.single { it.name == "Foo" }
                checkAnnotations(fooClass, listOf("A"))
                checkAnnotations(fooClass.functions.single(), listOf("A"))
                val barClass = fooClass.classlikes.single()
                checkAnnotations(barClass, listOf("A", "B", "C"))
                checkAnnotations(barClass.functions.single(), listOf("A", "B", "C"))
            }
        }
    }

    @Test
    fun `test recursive propagation`() {
        testInline(
            """
            /src/com/sample/Foo.kt
            package com.sample
            @Deprecated
            class Foo {
                var v = 0
                companion object {
                    fun foo() = Unit
                }
            }
            """
                .trimIndent(),
            configuration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                val fooClass = mod.packages.single().classlikes.single() as DClass
                assertTrue(fooClass.isDeprecated())
                val property = fooClass.properties.single()
                assertTrue(property.isDeprecated())
                assertTrue(property.getter!!.isDeprecated())
                assertTrue(property.setter!!.isDeprecated())

                val companionObject = fooClass.companion()!!
                assertTrue(companionObject.isDeprecated())
                assertTrue(companionObject.functions.single().isDeprecated())
            }
        }
    }

    @Test
    fun `test propagation when there are file level annotations`() {
        testInline(
            """
            /src/com/sample/Foo.kt
            @file:Suppress("SomeIssue")
            package com.sample
            @Deprecated
            class Foo {
                var v = 0
                fun foo() = Unit
                companion object
                inner class Bar
            }
            """
                .trimIndent(),
            configuration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                val fooClass = mod.packages.single().classlikes.single() as DClass
                assertTrue(fooClass.isDeprecated())
                assertTrue(fooClass.constructors.single().isDeprecated())
                assertTrue(fooClass.functions.single().isDeprecated())
                assertTrue(fooClass.properties.single().isDeprecated())
                assertTrue(fooClass.companion()!!.isDeprecated())
                assertTrue(
                    (fooClass.classlikes.single { it.name == "Bar" } as DClass).isDeprecated()
                )
            }
        }
    }
}
