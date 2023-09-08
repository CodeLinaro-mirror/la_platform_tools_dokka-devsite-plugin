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
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(Parameterized::class)
internal class DocumentablesHolderTest(
    private val displayLanguage: Language
) : ConverterTestBase(displayLanguage) {
    private val packageA = mock<DPackage> {
        on { name } doReturn "com.example.a"
        on { packageName } doReturn "com.example.a"
    }
    private val packageB = mock<DPackage> {
        on { name } doReturn "com.example.b"
        on { packageName } doReturn "com.example.b"
    }
    private val packageC = mock<DPackage> {
        on { name } doReturn "com.example.c"
        on { packageName } doReturn "com.example.c"
    }
    private val packageD = mock<DPackage> {
        on { name } doReturn "com.exclude.a"
        on { packageName } doReturn "com.exclude.a"
    }
    private val module = mock<DModule> {
        on { packages } doReturn listOf(packageC, packageB, packageD, packageA)
    }

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
        val excludedPackages = setOf(
            "com.example.b".toRegex(), "com.example.d".toRegex(),
            """.*\.exclude.*""".toRegex()
        )
        val packages = runBlocking {
            ConverterHolder(
                testClass = this@DocumentablesHolderTest,
                module = module,
                excludedPackages = excludedPackages
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
            arrayOf(Language.KOTLIN)
        )
    }
}
