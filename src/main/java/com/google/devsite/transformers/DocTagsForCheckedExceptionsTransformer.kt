package com.google.devsite.transformers

import org.jetbrains.dokka.analysis.kotlin.markdown.MARKDOWN_ELEMENT_FILE_NAME
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.CheckedExceptions
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.model.doc.Throws as ThrowsTag

/**
 * Adds documentation tags representing checked exceptions from java.
 */
class DocTagsForCheckedExceptionsTransformer : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule =
        original.copy(
            packages = original.packages.map { p ->
                p.copy(classlikes = p.classlikes.map(::transformClasslike))
            },
        )

    private fun transformClasslike(classlike: DClasslike): DClasslike =
        when (classlike) {
            is DInterface -> classlike.copy(
                functions = classlike.functions.map(::transformFunction),
                classlikes = classlike.classlikes.map(::transformClasslike),
            )
            is DClass -> classlike.copy(
                functions = classlike.functions.map(::transformFunction),
                classlikes = classlike.classlikes.map(::transformClasslike),
            )
            is DEnum -> classlike.copy(
                functions = classlike.functions.map(::transformFunction),
                classlikes = classlike.classlikes.map(::transformClasslike),
            )
            is DObject -> classlike.copy(
                functions = classlike.functions.map(::transformFunction),
                classlikes = classlike.classlikes.map(::transformClasslike),
            )
            is DAnnotation -> classlike
        }

    private fun transformFunction(
        function: DFunction,
    ): DFunction {
        val allExceptions = function.extra[CheckedExceptions]?.exceptions
        return if (allExceptions.isNullOrEmpty()) {
            function
        } else {
            val newDocs = allExceptions.entries.map { (set, exceptions) ->
                val oldDoc = function.documentation[set] ?: DocumentationNode(emptyList())
                set to documentThrows(oldDoc, exceptions)
            }

            function.copy(documentation = function.documentation + newDocs)
        }
    }

    // This is hacky; copied from upstream
    private fun DRI.fqName(): String? = "$packageName.$classNames"
        .takeIf { packageName != null && classNames != null }

    private fun documentThrows(
        oldDoc: DocumentationNode,
        exceptions: List<DRI>,
    ): DocumentationNode {
        val knownThrows = oldDoc.children.filterIsInstance<ThrowsTag>()
            .mapNotNull { it.exceptionAddress }
            .toSet()

        val throwTags = exceptions.minus(knownThrows).map {
            ThrowsTag(
                CustomDocTag(name = MARKDOWN_ELEMENT_FILE_NAME),
                it.fqName().orEmpty(),
                it,
            )
        }

        return oldDoc.copy(children = oldDoc.children + throwTags)
    }
}
