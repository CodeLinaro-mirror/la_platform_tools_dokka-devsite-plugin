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

package com.google.devsite.renderer.converters

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.AnalysisEnvironment
import org.jetbrains.dokka.analysis.DokkaMessageCollector
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.analysis.EnvironmentAndFacade
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.idea.kdoc.resolveKDocSampleLink
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * This invokes the EnvironmentAndFacade object to turn a DRI
 * (like "dokkatest.sampleAnnotation.samples.FunctionContainingClassSample") into a PSIElement
 * which is part of the abstract syntax tree representation of kotlin code.
 */
internal fun fqNameToPsiElement(
    resolutionFacade: DokkaResolutionFacade,
    functionName: String
): PsiElement? {
    val packageName = functionName.takeWhile { it != '.' }
    val descriptor = resolutionFacade.resolveSession.getPackageFragment(FqName(packageName))
        ?: throw RuntimeException("Cannot find descriptor for package $packageName")
    val symbol = resolveKDocSampleLink(
        BindingContext.EMPTY,
        resolutionFacade,
        descriptor,
        functionName.split(".")
    ).firstOrNull() ?: throw RuntimeException("Unresolved function $functionName in @sample")
    return DescriptorToSourceUtils.descriptorToDeclaration(symbol)
}

/**
 * This takes a PSIElement and returns the source code it is associated with. For example, if
 * the PSIElement represents a function, it returns the source code of the function.
 */
internal fun processBody(psiElement: PsiElement): String {
    val text = processSampleBody(psiElement).trim { it == '\n' || it == '\r' }.trimEnd()
    val lines = text.split("\n")
    val indent = lines.filter(String::isNotBlank).map {
        it.takeWhile(Char::isWhitespace).count() }.minOrNull() ?: 0
    return lines.joinToString("\n") { it.drop(indent) }
}

// Auxiliary function for processBody
private fun processSampleBody(psiElement: PsiElement): String = when (psiElement) {
    is KtDeclarationWithBody -> {
        val bodyExpression = psiElement.bodyExpression
        when (bodyExpression) {
            is KtBlockExpression -> bodyExpression.text.removeSurrounding("{", "}")
            else -> bodyExpression!!.text
        }
    }
    else -> psiElement.text
}

/**
 * We know these imports should not be included in the sampled code, even though they are
 * referenced.
 */
private val importsToIgnore = listOf("androidx.annotation.Sampled")
    .map { ImportPath.fromString(it) }

// Based on old dokka's implementation, hide all non-androidx import statements
private val androidxPackage = Name.identifier("androidx")

/**
 * This takes a PSIElement and returns the list of import statements it requires.
 * For example, the list of import statements associated with all elements in a function's
 * source code.
 * Certain import statements are guaranteed to be removed, for example the import of the
 * `@Sampled` annotation itself.
 * If the file contains multiple such functions, and therefore import statements which are not
 * associated with that function's source code, those import statements are not included.
 */
internal fun processImports(psiElement: PsiElement): String {
    val psiFile = psiElement.containingFile

    val sampleExpressionCalls = mutableSetOf<String>()
    (psiElement as KtDeclarationWithBody).bodyExpression!!.accept(object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
            sampleExpressionCalls.addIfNotNull(expression.calleeExpression?.text)
            super.visitCallExpression(expression)
        }
    })
    if (psiFile is KtFile) {
        val filteredImports = psiFile.importList?.imports?.filter { element ->
            val fqImportName = element.importPath?.fqName ?: return@filter false
            // Hide all non-androidx imports
            if (!fqImportName.startsWith(androidxPackage)) return@filter false
            // Hide all explicitly ignored imports (like androidx.annotations.Sampled)
            if (element.importPath in importsToIgnore) return@filter false
            // Hide non-import statements that somehow sneak in here? From old dokka.
            if (element !is KtImportDirective) return@filter false
            // Hide empty lines
            if (element.text.trim().isEmpty()) return@filter false

            // Return whether any of the code in the sample uses this import
            return@filter sampleExpressionCalls.any { call ->
                call == fqImportName.shortName().identifier
            }
        }.orEmpty()
        // Don't spam blank lines if there are no imports (post-filtering)
        if (filteredImports.isEmpty()) { return "" }
        // The first blank line doesn't appear in rendered html, just makes raws look nicer
        return "\n" + filteredImports.joinToString(separator = "\n") { it.text } + "\n\n"
    } else { throw RuntimeException("${psiFile::class} is not a supported sample file type") }
}

/**
 * Constructs EnvironmentAndFacade objects for each sourceSet.
 * These are mysterious upstream objects capable of complex parsing of the Kotlin abstract syntax
 * tree of the files they are generated from. In particular, they can create PSIElements for kotlin
 * code objects given a DRI like dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
 *
 * These are generated for each sourceSet with samples, and returned as a map.
 */
internal fun setUpAnalysis(context: DokkaContext) = context.configuration.sourceSets
    .filter { it.samples.isNotEmpty() }.map {
        sourceSet -> sourceSet to AnalysisEnvironment(
        DokkaMessageCollector(context.logger),
        sourceSet.analysisPlatform
    ).run {
        if (analysisPlatform == Platform.jvm) {
            addClasspath(PathUtil.getJdkClassesRootsFromCurrentJre())
        }
        sourceSet.classpath.forEach(::addClasspath)

        addSources(sourceSet.samples.toList())

        loadLanguageVersionSettings(sourceSet.languageVersion, sourceSet.apiVersion)

        val environment = createCoreEnvironment()
        val (facade, _) = createResolutionFacade(environment)
        EnvironmentAndFacade(environment, facade)
    }
}.toMap()
