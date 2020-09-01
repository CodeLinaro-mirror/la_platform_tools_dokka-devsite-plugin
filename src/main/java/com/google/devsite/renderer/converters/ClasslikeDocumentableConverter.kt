/*
 * Copyright 2020 The Android Open Source Project
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

package com.google.devsite.renderer.converters

import com.google.devsite.components.Classlike
import com.google.devsite.components.DevsitePage
import com.google.devsite.components.FunctionDetail
import com.google.devsite.components.SummaryList
import com.google.devsite.components.TableTitle
import com.google.devsite.components.impl.DefaultClasslike
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableTitle
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction

/** Converts documentable class-likes into the classlike component. */
internal class ClasslikeDocumentableConverter(
    private val displayLanguage: Language,
    private val classlike: DClasslike,
    private val pathProvider: FilePathProvider
) {
    private val javadocConverter = DocTagConverter(displayLanguage, pathProvider)
    private val functionConverter =
        FunctionDocumentableConverter(displayLanguage, pathProvider, javadocConverter)

    /** @return the classlike component */
    suspend fun classlike(): DevsitePage = coroutineScope {
        val declaredFunctions = classlike.myFunctions()
        val publicFunctionsSummary = async {
            functionsToSummary(publicMethodsTitle(), declaredFunctions.public())
        }
        val protectedFunctionsSummary = async {
            functionsToSummary(protectedMethodsTitle(), declaredFunctions.protected())
        }

        val publicFunctions = async { functionsToDetail(declaredFunctions.public()) }
        val protectedFunctions = async { functionsToDetail(declaredFunctions.protected()) }

        val allSymbols = listOf(
            publicFunctionsSummary.await() to Classlike.SymbolType(
                publicMethodsTitle(),
                publicFunctions.await()
            ),
            protectedFunctionsSummary.await() to Classlike.SymbolType(
                protectedMethodsTitle(),
                protectedFunctions.await()
            )
        )

        DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage,
                path = pathProvider.relative.forReference(classlike.dri).url,
                bookPath = pathProvider.book,
                title = classlike.name(),
                content = DefaultClasslike(
                    Classlike.Params(
                        description = javadocConverter.metadata(classlike),
                        symbolTypes = allSymbols
                    )
                )
            )
        )
    }

    private fun functionsToSummary(name: String, functions: List<DFunction>): SummaryList {
        val components = functions.map {
            functionConverter.summary(it)
        }

        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(
                    TableTitle.Params(
                        title = name,
                        big = true
                    )
                ),
                items = components
            )
        )
    }

    private fun functionsToDetail(functions: List<DFunction>): List<FunctionDetail> {
        return functions.map {
            functionConverter.detail(it)
        }
    }

    /**
     * Returns the list of declared functions. That is, functions directly owned by this class-like
     * and not found through the inheritance hierarchy.
     */
    private fun DClasslike.myFunctions() = functions.filter { function ->
        classlike.packageName() == function.dri.packageName &&
            classlike.name() == function.dri.classNames
    }

    private fun List<DFunction>.public() = withVisibility("public")
    private fun List<DFunction>.protected() = withVisibility("protected")
    private fun List<DFunction>.withVisibility(type: String) = filter { function ->
        type == function.visibility.values.single().name
    }.sortedBy { it.name }

    private fun publicMethodsTitle() = "Public ${methodsTitle()}"
    private fun protectedMethodsTitle() = "Protected ${methodsTitle()}"
    private fun methodsTitle(): String = when (displayLanguage) {
        Language.JAVA -> "methods"
        Language.KOTLIN -> "functions"
    }
}
