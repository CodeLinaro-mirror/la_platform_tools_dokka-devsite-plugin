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

class JavaSignatureProvider(ctcc: CommentsToContentConverter, logger: DokkaLogger) : SignatureProvider,
        JvmSignatureUtils by JavaSignatureUtils {

    private val contentBuilder = DevsitePageContentBuilder(ctcc, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Default)

    private val ignoredModifiers =
            setOf(KotlinModifier.Open, JavaModifier.Empty, KotlinModifier.Empty, KotlinModifier.Sealed)

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

    private fun signature(c: DClasslike): List<ContentNode> =
            javadocSignature(c) {
                annotations {
                    annotationsBlock(c)
                }
                modifiers {
                    text(c.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.plus(" ") ?: "")

                    if (c is DClass) {
                        text(c.modifier[it]?.takeIf { it !in ignoredModifiers }?.name?.plus(" ") ?: "")
                        text(c.modifiers()[it]?.toSignatureString() ?: "")
                    }

                    when (c) {
                        is DClass -> text("class")
                        is DInterface -> text("interface")
                        is DEnum -> text("enum")
                        is DObject -> text("class")
                        is DAnnotation -> text("@interface")
                    }
                }
                signatureWithoutModifiers {
                    link(c.name!!, c.dri)
                    if (c is WithGenerics) {
                        list(c.generics, prefix = "<", suffix = ">") {
                            +buildSignature(it)
                        }
                    }
                }
                supertypes {
                    if (c is WithSupertypes) {
                        c.supertypes.map { (p, dris) ->
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

    private fun signature(f: DFunction): List<ContentNode> =
            javadocSignature(f) {
                annotations {
                    annotationsBlock(f)
                }
                modifiers {
                    text(f.modifier[it]?.takeIf { it !in ignoredModifiers }?.name?.plus(" ") ?: "")
                    text(f.modifiers()[it]?.toSignatureString() ?: "")
                    list(f.generics, prefix = "<", suffix = "> ") {
                        +buildSignature(it)
                    }
                    signatureForProjection(f.type)
                }
                signatureWithoutModifiers {
                    link(f.name, f.dri)
                    text("(")
                    list(f.parameters) {
                        annotationsInline(it)
                        text(it.modifiers()[it]?.toSignatureString().orEmpty())
                        signatureForProjection(it.type)
                        text(Typography.nbsp.toString())
                        text(it.name.orEmpty())
                    }
                    text(")")
                }
            }

    private fun signature(p: DProperty): List<ContentNode> =
            javadocSignature(p) {
                annotations {
                    annotationsBlock(p)
                }
                modifiers {
                    text(p.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.plus(" ") ?: "")
                    text(p.modifier[it]?.name + " ")
                    text(p.modifiers()[it]?.toSignatureString() ?: "")
                    signatureForProjection(p.type)
                }
                signatureWithoutModifiers {
                    link(p.name, p.dri)
                }
            }

    private fun signature(e: DEnumEntry): List<ContentNode> =
            javadocSignature(e) {
                annotations {
                    annotationsBlock(e)
                }
                modifiers {
                    text(e.modifiers()[it]?.toSignatureString() ?: "")
                }
                signatureWithoutModifiers {
                    link(e.name, e.dri)
                }
            }

    private fun signature(t: DTypeParameter): List<ContentNode> =
            javadocSignature(t) {
                annotations {
                    annotationsBlock(t)
                }
                signatureWithoutModifiers {
                    text(t.name)
                }
                supertypes {
                    list(t.bounds, prefix = "extends ") {
                        signatureForProjection(it)
                    }
                }
            }

    private fun signature(p: DParameter): List<ContentNode> =
            javadocSignature(p) {
                modifiers {
                    signatureForProjection(p.type)
                }
                signatureWithoutModifiers {
                    link(p.name.orEmpty(), p.dri)
                }
            }

    private fun javadocSignature(
            d: Documentable,
            extra: PropertyContainer<ContentNode> = PropertyContainer.empty(),
            block: DevsitePageContentBuilder.DevsiteContentBuilder.(DokkaConfiguration.DokkaSourceSet) -> Unit
    ): List<ContentNode> =
            d.sourceSets.map { sourceSet ->
                contentBuilder.contentFor(d, ContentKind.Main) {
                    with(contentBuilder) {
                        javadocGroup(d.dri, d.sourceSets, extra) {
                            block(sourceSet)
                        }
                    }
                }
            }

    private fun PageContentBuilder.DocumentableContentBuilder.signatureForProjection(p: Projection): Unit = when (p) {
        is OtherParameter -> link(p.name, p.declarationDRI)
        is TypeConstructor -> group {
            link(p.dri.classNames.orEmpty(), p.dri)
            list(p.projections, prefix = "<", suffix = ">") {
                signatureForProjection(it)
            }
        }
        is Variance -> group {
            text(p.kind.toString() + " ")
            signatureForProjection(p.inner)
        }
        is Star -> text("?")
        is Nullable -> signatureForProjection(p.inner)
        is JavaObject, is Dynamic -> link("Object", DRI("java.lang", "Object"))
        is Void -> text("void")
        is PrimitiveJavaType -> text(p.name)
        is UnresolvedBound -> text(p.name)
    }

    private fun DRI.fqName(): String = "${packageName.orEmpty()}.${classNames.orEmpty()}"
}
