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

package com.google.devsite.signatures

import com.google.devsite.translation.DevsitePageContentBuilder
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.signatures.JvmSignatureUtils
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.kotlinAsJava.signatures.JavaSignatureUtils
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeParameter
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Dynamic
import org.jetbrains.dokka.model.JavaClassKindTypes
import org.jetbrains.dokka.model.JavaModifier
import org.jetbrains.dokka.model.JavaObject
import org.jetbrains.dokka.model.JavaVisibility
import org.jetbrains.dokka.model.KotlinModifier
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.OtherParameter
import org.jetbrains.dokka.model.PrimitiveJavaType
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.Star
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Variance
import org.jetbrains.dokka.model.Void
import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.WithSupertypes
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.utilities.DokkaLogger

/**
 * Provides Java signatures for source files of all types.
 *
 * For example, this class takes a Kotlin function and generates a Java method signature.
 */
class JavaSignatureProvider(
        converter: CommentsToContentConverter,
        logger: DokkaLogger
) : SignatureProvider, JvmSignatureUtils by JavaSignatureUtils {

    private val contentBuilder = DevsitePageContentBuilder(converter, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Default)

    private val ignoredModifiers = setOf(
            KotlinModifier.Open,
            JavaModifier.Empty,
            KotlinModifier.Empty,
            KotlinModifier.Sealed
    )

    override fun signature(documentable: Documentable): List<ContentNode> = when (documentable) {
        is DFunction -> signature(documentable)
        is DProperty -> signature(documentable)
        is DClasslike -> signature(documentable)
        is DEnumEntry -> signature(documentable)
        is DTypeParameter -> signature(documentable)
        is DParameter -> signature(documentable)
        else -> throw NotImplementedError(
                "Cannot generate signature for ${documentable::class.qualifiedName} ${documentable.name}"
        )
    }

    private fun signature(documentableClassLike: DClasslike): List<ContentNode> {
        return javadocSignature(documentableClassLike) {
            annotations {
                annotationsBlock(documentableClassLike)
            }
            modifiers {
                text(documentableClassLike.visibility[it]?.takeIf {
                    it !in ignoredVisibilities
                }?.name?.plus(" ") ?: "")

                if (documentableClassLike is DClass) {
                    text(documentableClassLike.modifier[it]?.takeIf {
                        it !in ignoredModifiers
                    }?.name?.plus(" ") ?: "")
                    text(documentableClassLike.modifiers()[it]?.toSignatureString() ?: "")
                }

                when (documentableClassLike) {
                    is DClass -> text("class")
                    is DInterface -> text("interface")
                    is DEnum -> text("enum")
                    is DObject -> text("class")
                    is DAnnotation -> text("@interface")
                }
            }
            signatureWithoutModifiers {
                link(documentableClassLike.name!!, documentableClassLike.dri)
                if (documentableClassLike is WithGenerics) {
                    list(documentableClassLike.generics, prefix = "<", suffix = ">") {
                        +buildSignature(it)
                    }
                }
            }
            supertypes {
                if (documentableClassLike is WithSupertypes) {
                    documentableClassLike.supertypes.map { (p, dris) ->
                        val (classes, interfaces) = dris.partition { it.kind == JavaClassKindTypes.CLASS }
                        list(classes, prefix = " extends ", sourceSets = setOf(p)) {
                            link(it.dri.sureClassNames, it.dri, sourceSets = setOf(p))
                        }
                        list(interfaces, prefix = " implements ", sourceSets = setOf(p)) {
                            link(it.dri.sureClassNames, it.dri, sourceSets = setOf(p))
                        }
                    }
                }
            }
        }
    }

    private fun signature(documentableFunction: DFunction): List<ContentNode> {
        return javadocSignature(documentableFunction) {
            annotations {
                annotationsBlock(documentableFunction)
            }
            modifiers {
                text(documentableFunction.modifier[it]?.takeIf { it !in ignoredModifiers }?.name?.plus(" ") ?: "")
                text(documentableFunction.modifiers()[it]?.toSignatureString() ?: "")
                list(documentableFunction.generics, prefix = "<", suffix = "> ") { +buildSignature(it) }
                signatureForProjection(documentableFunction.type)
            }
            signatureWithoutModifiers {
                link(documentableFunction.name, documentableFunction.dri)
                text("(")
                list(documentableFunction.parameters) {
                    annotationsInline(it)
                    text(it.modifiers()[it]?.toSignatureString().orEmpty())
                    signatureForProjection(it.type)
                    text(Typography.nbsp.toString())
                    text(it.name.orEmpty())
                }
                text(")")
            }
        }
    }

    private fun signature(documentableProperty: DProperty): List<ContentNode> {
        return javadocSignature(documentableProperty) {
            annotations {
                annotationsBlock(documentableProperty)
            }
            modifiers {
                text(documentableProperty.visibility[it]?.takeIf {
                    it !in ignoredVisibilities
                }?.name?.plus(" ") ?: "")
                text(documentableProperty.modifier[it]?.name + " ")
                text(documentableProperty.modifiers()[it]?.toSignatureString() ?: "")
                signatureForProjection(documentableProperty.type)
            }
            signatureWithoutModifiers {
                link(documentableProperty.name, documentableProperty.dri)
            }
        }
    }

    private fun signature(documentableEnumEntry: DEnumEntry): List<ContentNode> {
        return javadocSignature(documentableEnumEntry) {
            annotations {
                annotationsBlock(documentableEnumEntry)
            }
            modifiers {
                text(documentableEnumEntry.modifiers()[it]?.toSignatureString() ?: "")
            }
            signatureWithoutModifiers {
                link(documentableEnumEntry.name, documentableEnumEntry.dri)
            }
        }
    }

    private fun signature(documentableTypeParameter: DTypeParameter): List<ContentNode> {
        return javadocSignature(documentableTypeParameter) {
            annotations {
                annotationsBlock(documentableTypeParameter)
            }
            signatureWithoutModifiers {
                text(documentableTypeParameter.name)
            }
            supertypes {
                list(documentableTypeParameter.bounds, prefix = "extends ") {
                    signatureForProjection(it)
                }
            }
        }
    }

    private fun signature(documentableParameter: DParameter): List<ContentNode> {
        return javadocSignature(documentableParameter) {
            modifiers {
                signatureForProjection(documentableParameter.type)
            }
            signatureWithoutModifiers {
                link(documentableParameter.name.orEmpty(), documentableParameter.dri)
            }
        }
    }

    private fun javadocSignature(
            documentable: Documentable,
            extra: PropertyContainer<ContentNode> = PropertyContainer.empty(),
            block: DevsitePageContentBuilder.DevsiteContentBuilder.(DokkaConfiguration.DokkaSourceSet) -> Unit
    ): List<ContentNode> {
        return documentable.sourceSets.map { sourceSet ->
            contentBuilder.contentFor(documentable, ContentKind.Main) {
                with(contentBuilder) {
                    javadocGroup(documentable.dri, documentable.sourceSets, extra) {
                        block(sourceSet)
                    }
                }
            }
        }
    }

    private fun PageContentBuilder.DocumentableContentBuilder.signatureForProjection(projection: Projection) {
        return when (projection) {
            is OtherParameter -> link(projection.name, projection.declarationDRI)
            is TypeConstructor -> group {
                link(projection.dri.classNames.orEmpty(), projection.dri)
                list(projection.projections, prefix = "<", suffix = ">") {
                    signatureForProjection(it)
                }
            }
            is Variance -> group {
                text(projection.kind.toString() + " ")
                signatureForProjection(projection.inner)
            }
            is Star -> text("?")
            is Nullable -> signatureForProjection(projection.inner)
            is JavaObject, is Dynamic -> link("Object", DRI("java.lang", "Object"))
            is Void -> text("void")
            is PrimitiveJavaType -> text(projection.name)
            is UnresolvedBound -> text(projection.name)
        }
    }

    private fun DRI.fullyQualifiedName(): String = "${packageName.orEmpty()}.${classNames.orEmpty()}"
}
