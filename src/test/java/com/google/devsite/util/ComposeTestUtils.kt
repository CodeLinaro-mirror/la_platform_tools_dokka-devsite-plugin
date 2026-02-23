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

package com.google.devsite.util

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage

object ComposeTestUtils {
    /** Stub definitions of Composable and Modifier so that the classes resolve in tests. */
    private val stubs =
        """
        /src/main/androidx/compose/runtime/Composable.kt
        package androidx.compose.runtime
        annotation class Composable

        /src/main/androidx/compose/ui/Modifier.kt
        package androidx.compose.ui
        class Modifier
        """
            .trimIndent()

    /** Non-compose package used in tests. */
    private const val PACKAGE_NAME: String = "com.example"

    /** File name, package declaration, and imports for a test file that uses compose. */
    private val testFileHeader =
        """
        /src/main/${PACKAGE_NAME.replace(".", "/")}/Foo.kt
        package $PACKAGE_NAME
        import androidx.compose.ui.Modifier
        import androidx.compose.runtime.Composable
        """
            .trimIndent()

    /**
     * Returns the string that should be used for running tests based on the file [body] which may
     * use compose classes. The [stubs] will be included so compose references resolve.
     */
    fun testFiles(body: String): String {
        return stubs + "\n\n" + testFileHeader + "\n" + body
    }

    /** From the [dModule], returns the package with [PACKAGE_NAME]. */
    fun testPackage(dModule: DModule): DPackage {
        return dModule.packages.single { it.name == PACKAGE_NAME }
    }
}
