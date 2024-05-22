/*
 * Copyright 2021 The Android Open Source Project
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

package com.google.devsite.renderer.impl

import com.google.common.truth.Truth
import com.google.devsite.renderer.Language
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.DokkaSourceSetImpl
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeAlias
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class DocumentablesHolderTest(
    displayLanguage: Language,
) : ConverterTestBase(displayLanguage) {
    private val packageA = fakeDPackage(dri = DRI(packageName = "com.example.a"))
    private val packageB = fakeDPackage(dri = DRI(packageName = "com.example.b"))
    private val packageC = fakeDPackage(dri = DRI(packageName = "com.example.c"))
    private val packageD = fakeDPackage(dri = DRI(packageName = "com.exclude.a"))
    private val module = fakeDModule(packages = listOf(packageC, packageB, packageD, packageA))

    @Test
    fun `computePackages returns list of packages sorted by package name`() {
        val expected = listOf("com.example.a", "com.example.b", "com.example.c", "com.exclude.a")
            .toTypedArray()
        val packages = runBlocking {
            ConverterHolder(this@DocumentablesHolderTest, module).holder.packages()
        }
        val result = packages.map { it.packageName }.toTypedArray()
        Truth.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `computePackages returns list of packages with packages filtered out`() {
        val expected = listOf("com.example.a", "com.example.c").toTypedArray()
        val excludedPackageSet = setOf(
            "com.example.b".toRegex(),
            "com.example.d".toRegex(),
            """.*\.exclude.*""".toRegex(),
        )
        val excludedPackages =
            mapOf(Language.KOTLIN to excludedPackageSet, Language.JAVA to excludedPackageSet)
        val packages = runBlocking {
            ConverterHolder(
                testClass = this@DocumentablesHolderTest,
                module = module,
                excludedPackages = excludedPackages,
            ).holder.packages()
        }
        val result = packages.map { it.packageName }.toTypedArray()
        Truth.assertThat(result).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN),
        )
    }
}

private fun fakeDPackage(
    dri: DRI = DRI(),
    functions: List<DFunction> = emptyList(),
    properties: List<DProperty> = emptyList(),
    classlikes: List<DClasslike> = emptyList(),
    typealiases: List<DTypeAlias> = emptyList(),
    docs: SourceSetDependent<DocumentationNode> = emptyMap(),
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet> =
        setOf(DokkaSourceSetImpl(sourceSetID = DokkaSourceSetID("", ""))),
) = DPackage(dri, functions, properties, classlikes, typealiases, docs, sourceSets = sourceSets)
private fun fakeDModule(
    name: String = "FAKE",
    packages: List<DPackage> = emptyList(),
    documentation: SourceSetDependent<DocumentationNode> = emptyMap(),
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet> =
        setOf(DokkaSourceSetImpl(sourceSetID = DokkaSourceSetID("", ""))),
) = DModule(name, packages, documentation, sourceSets = sourceSets)
