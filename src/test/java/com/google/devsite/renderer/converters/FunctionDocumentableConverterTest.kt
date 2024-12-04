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

import com.google.common.truth.Truth.assertThat
import com.google.devsite.TypeSummaryItem
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.LambdaTypeProjectionComponent
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.SymbolDetail.SymbolKind
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.functionSignature
import com.google.devsite.renderer.converters.testing.isAtNonNull
import com.google.devsite.renderer.converters.testing.isAtNullable
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.projectionName
import com.google.devsite.renderer.converters.testing.typeAnnotations
import com.google.devsite.renderer.converters.testing.typeName
import com.google.devsite.testing.ConverterTestBase
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class FunctionDocumentableConverterTest(
    displayLanguage: Language,
) : ConverterTestBase(displayLanguage) {

    override var defaultHints =
        ModifierHints(
            displayLanguage = displayLanguage,
            isSummary = false,
            type = DFunction::class.java,
            containingType = DClass::class.java,
            isFromJava = false, // There's no great way to do this. Currently only affects
            // `const` inject
        )

    @Test
    fun `Top level function summary component has correct default modifiers`() {
        val summary =
            """
            |fun foo() = Unit
        """
                .render()
                .functionSummary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).isEmpty() }
    }

    @Test
    fun `Function summary component ignores public modifier`() {
        val summary =
            """
            |public fun foo() = Unit
        """
                .render()
                .functionSummary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).isEmpty() }
        javaOnly { assertThat(returnz.modifiers).containsExactly("final") }
    }

    @Test
    fun `Function summary component has suspend modifier`() {
        val summary =
            """
            |suspend fun foo() = Unit
        """
                .render()
                .functionSummary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).containsExactly("suspend") }
    }

    @Test
    fun `Function summary component has inline modifier`() {
        val summary =
            """
            |inline fun foo() = Unit
        """
                .render()
                .functionSummary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).containsExactly("inline") }
    }

    @Test
    fun `Function summary for protected component in abstract class has no modifier`() {
        val summary =
            """
            |abstract class Foo {
            |    protected open fun foo() = Unit
            |}
        """
                .render()
                .functionSummary()

        val returnz = summary.returnSummary()

        // public and protected modifiers don't appear in summary
        kotlinOnly { assertThat(returnz.modifiers).containsExactly("open") }
        javaOnly { assertThat(returnz.modifiers).isEmpty() }
    }

    @Test
    fun `Function summary component in abstract class has abstract modifier`() {
        val summary =
            """
            |abstract class Foo {
            |    abstract fun foo()
            |}
        """
                .render()
                .functionSummary()

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("abstract")
    }

    @Test
    fun `Function summary component in class has open modifiers`() {
        val summary =
            """
            |class Foo {
            |    open fun foo() = Unit
            |}
        """
                .render()
                .functionSummary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).containsExactly("open") }
        javaOnly { assertThat(returnz.modifiers).isEmpty() }
    }

    @Test
    fun `Function summary abstract before suspend`() {
        val summary =
            """
            |class Foo {
            |    abstract suspend fun foo(): Unit
            |}
        """
                .render()
                .functionSummary()

        val returnz = summary.returnSummary()

        kotlinOnly {
            assertThat(returnz.modifiers).containsExactly("abstract", "suspend").inOrder()
        }
        javaOnly { assertThat(returnz.modifiers).containsExactly("abstract") }
    }

    @Test
    fun `Function summary component in interface has abstract modifiers when appropriate`() {
        val summariesK =
            """
            |interface Foo {
            |    fun foo()
            |    fun bar() = "default implementation"
            |}
        """
                .render()
                .functionSummaries(defaultHints.copy(containingType = DInterface::class.java))
        val summariesJ =
            """
            |public interface Foo {
            |    public void foo();
            |    public default void bar() {return "default implementation"; }
            |}
         """
                .render(java = true)
                .functionSummaries(defaultHints.copy(containingType = DInterface::class.java))

        for (summaries in listOf(summariesK, summariesJ)) {
            val fooReturnz = summaries["foo"]!!.returnSummary()
            javaOnly { assertThat(fooReturnz.modifiers).containsExactly("abstract") }
            kotlinOnly { assertThat(fooReturnz.modifiers).isEmpty() }
            val barReturnz = summaries["bar"]!!.returnSummary()
            javaOnly { assertThat(barReturnz.modifiers).containsExactly("default") }
            kotlinOnly {
                if (summaries == summariesJ) {
                    assertThat(barReturnz.modifiers).isEmpty()
                } else assertThat(barReturnz.modifiers).containsExactly("open")
            }
        }
    }

    @Test
    fun `Function summary component creates return type link`() {
        val summary =
            """
            |class A
            |fun foo(): A
        """
                .render()
                .functionSummary()

        val returnType = summary.returnSummary().type

        assertThat(returnType.link().name).isEqualTo("A")
        assertPath(returnType.link().url, "androidx/example/A.html")
    }

    @Test
    fun `Function summary component creates void return type link`() {
        val summary =
            """
            |fun foo() = Unit
        """
                .render()
                .functionSummary()

        val returnType = summary.returnSummary().type

        javaOnly {
            assertThat(returnType.link().name).isEqualTo("void")
            assertThat(returnType.link().url).isEmpty()
        }
        kotlinOnly {
            assertThat(returnType.link().name).isEqualTo("Unit")
            assertThat(returnType.link().url)
                .isEqualTo("https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html")
        }
    }

    @Test
    fun `Function summary component handles constructors`() {
        val summary =
            """
            |class MyClass
        """
                .render()
                .functionSummary()

        val constructor = summary.data.description

        assertThat(constructor.name()).isEqualTo("MyClass")
    }

    @Test
    fun `Function summary component creates return type generics`() {
        val summary =
            """
            |fun foo(): Map<String, List<Long>>
        """
                .render()
                .functionSummary()

        val returnz = summary.returnSummary()
        val generics = returnz.type.data.generics.items(2)
        val nestedGenerics = generics.last().data.generics.item()

        assertThat(generics.first().link().name).isEqualTo("String")
        assertThat(generics.last().link().name).isEqualTo("List")
        assertThat(nestedGenerics.link().name).isEqualTo("Long")
    }

    @Test
    fun `Function summary component creates signature with name`() {
        val summary =
            """
            |fun iAmACoolFunction()
        """
                .render()
                .functionSummary()

        val function = summary.data.description

        assertThat(function.name()).isEqualTo("iAmACoolFunction")
    }

    @Test
    fun `Function summary component creates extension receiver`() {
        val summary =
            """
            |fun String.foo()
        """
                .render()
                .functionSummary()

        val function = summary.data.description
        val signature = function.functionSignature()

        javaOnly {
            assertThat(signature.receiver).isNotNull()
            val param = signature.receiver!!
            assertNoLambdaStuff(param.data)

            val type = param.data.type
            assertThat(type.link().name).isEqualTo("TestKt")
        }

        kotlinOnly {
            assertThat(signature.receiver).isNotNull()
            val param = signature.receiver!!
            assertNoLambdaStuff(param.data)

            val type = param.data.type
            assertThat(type.link().name).isEqualTo("String")
            assertThat(type.link().url).contains("kotlin")
            assertThat(param.data.name).isEmpty()
        }
    }

    @Test
    fun `Function summary component creates extension receiver for proper type`() {
        val summary =
            """
            |fun Any.foo()
        """
                .render()
                .functionSummary()

        val function = summary.data.description
        val signature = function.data.signature

        javaOnly {
            assertThat(signature.data.receiver).isNotNull()
            val param = signature.data.receiver!!
            assertNoLambdaStuff(param.data)

            val type = param.data.type
            assertThat(type.link().name).isEqualTo("TestKt")
            assertThat(type.link().url).contains("androidx")
        }

        kotlinOnly {
            assertThat(signature.data.receiver).isNotNull()
            val param = signature.data.receiver!!
            assertNoLambdaStuff(param.data)

            val type = param.data.type
            assertThat(type.link().name).isEqualTo("Any")
            assertThat(type.link().url).contains("kotlin")
            assertThat(param.data.name).isEmpty()
        }
    }

    @Test
    fun `Function summary component creates params`() {
        val summary =
            """
            |fun foo(a: String)
        """
                .render()
                .functionSummary()

        val function = summary.data.description
        val param = function.data.signature.data.parameters.single()
        val paramType = param.data.type

        assertNoLambdaStuff(param.data)
        assertThat(param.data.name).isEqualTo("a")
        assertThat(paramType.link().name).isEqualTo("String")
    }

    @Test
    fun `Function summary component creates inline generics`() {
        val summary =
            """
            |fun <T: Number, U> foo() = Unit
        """
                .render()
                .functionSummary()
        val typeParams = summary.data.description.data.signature.data.typeParameters
        assertThat(typeParams.first().data.name).isEqualTo("T")
        assertThat(typeParams.first().projectionName()).isEqualTo("Number")
        assertThat(typeParams.last().data.name).isEqualTo("U")
        kotlinOnly { assertThat(typeParams.last().projectionName()).isEqualTo("Any") }
        javaOnly { assertThat(typeParams.last().projectionName()).isEqualTo("Object") }
    }

    @Test
    fun `Function signature component creates multiple inline generics`() {
        val inlineGenerics =
            """
            |fun <T: Number, U: List<String>, V: T> foo() = Unit
        """
                .render()
                .functionSummary()
                .data
                .description
                .data
                .signature
                .data
                .typeParameters

        assertThat(inlineGenerics.map { it.data.name }).isEqualTo(listOf("T", "U", "V"))
        assertThat(inlineGenerics[0].projectionName()).isEqualTo("Number")
        assertThat(inlineGenerics[1].projectionName()).isEqualTo("List")
        val generics = inlineGenerics[1].data.projections.single().data.generics
        assertThat(generics.single().link().name).isEqualTo("String")
        assertThat(inlineGenerics[2].projectionName()).isEqualTo("T")
    }

    @Test
    fun `Function summary component has correct relative link`() {
        val summary =
            """
            |fun <T : Number> List<String>.foo(t: T, a: Map<String, Int>, block: String.(Float) -> Double) = Unit
        """
                .render()
                .functionSummary()

        val function = summary.data.description
        val signature = function.data.signature

        assertPath(
            signature.data.name.data.url,
            "androidx/example/package-summary.html#" +
                "(kotlin.collections.List)" +
                ".foo(kotlin.Number,kotlin.collections.Map,kotlin.Function2)",
        )
    }

    @Test
    fun `Function summary component understands Java primitives`() {
        val summary =
            """
            |public void foo(
            |    boolean a, int b, double c, float d, short e, long f, char g, byte h) {}
        """
                .render(java = true)
                .functionSummary()

        val function = summary.data.description
        val returnType = summary.returnSummary().type.link()
        val signature = function.data.signature

        javaOnly {
            assertThat(returnType.name).isEqualTo("void")

            val expected =
                listOf("boolean", "int", "double", "float", "short", "long", "char", "byte")
            for ((i, param) in signature.data.parameters.withIndex()) {
                assertThat(param.data.type.link().name).isEqualTo(expected[i])
            }
        }
        kotlinOnly {
            assertThat(returnType.name).isEqualTo("Unit")

            val expected =
                listOf("Boolean", "Int", "Double", "Float", "Short", "Long", "Char", "Byte")
            for ((i, param) in signature.data.parameters.withIndex()) {
                assertThat(param.data.type.link().name).isEqualTo(expected[i])
            }
        }
    }

    @Test
    fun `Function summary component understands Java object`() {
        val summary =
            """
            |public Object foo() {}
        """
                .render(java = true)
                .functionSummary()

        val returnType = summary.returnSummary().type.link()

        javaOnly { assertThat(returnType.name).isEqualTo("Object") }
        kotlinOnly { assertThat(returnType.name).isEqualTo("Any") }
    }

    @Test // b/190477978 Patch upstream to generate default java constructor
    fun `Function summary component exists for (default) constructors`() {
        val summaryK =
            """
            |class Foo
        """
                .render()
                .functionSummary()
        val summaryJ =
            """
            |public class Foo {}
        """
                .render(java = true)
                .functionSummary()
        for (summary in listOf(summaryJ, summaryK)) {
            val ctor = summaryK.data.description
            assertThat(ctor.name()).isEqualTo("Foo")
        }
    }

    @Test
    fun `Function detail component has correct name`() {
        val detail =
            """
            |fun foo()
        """
                .render()
                .functionDetail()

        assertThat(detail.data.name).isEqualTo("foo")
    }

    @Test
    fun `Top level function detail component has correct default modifiers`() {
        val detail =
            """
            |fun foo() = Unit
        """
                .render()
                .functionDetail()

        javaOnly { assertThat(detail.data.modifiers).containsExactly("public", "final") }
        kotlinOnly { assertThat(detail.data.modifiers).isEmpty() }
    }

    @Test
    fun `Top level function detail component has annotations`() {
        val detail =
            """
            |annotation class Hello
            |@Hello fun foo() = Unit
        """
                .render()
                .functionDetail()

        assertThat(detail.data.annotationComponents).isNotEmpty()
    }

    @Test
    fun `Function summary and detail include nullability information in 4x Kotlin and Java`() {
        val moduleJ =
            """
                |@Nullable
                |public String nulla() { return null; }
                |public String platform() { return null; }
                |@NonNull
                |public String nonnaBefore() { return ""; }
                |public @NonNull String nonnaClose() { return ""; }
                """
                .render(java = true)
        val moduleK =
            """
                |fun nulla(): String? = null // nullability annotations in Kotlin are errors.
                |fun nonna(): String = "foo"
                """
                .render()
        fun DModule.sOrD(summary: Boolean, functionName: String): TypeProjectionComponent =
            if (summary) {
                functionSummary(functionName).data.title.data.type
            } else functionDetail(functionName).data.returnType
        for (isSummary in listOf(true, false)) {
            for (whichFun in listOf("nonna", "nulla", "nonnaBefore", "nonnaClose", "platform")) {
                val typeJ = if (whichFun == "nonna") null else moduleJ.sOrD(isSummary, whichFun)
                val typeK = if (whichFun.length != 5) null else moduleK.sOrD(isSummary, whichFun)
                for (aType in listOfNotNull(typeJ, typeK)) {
                    assertThat(aType.nullable).isEqualTo(whichFun in "nulla, platform")
                    val annotations = aType.annotations
                    assertThat(
                        annotations.singleOrNull()?.name?.let {
                            it in NULLABILITY_ANNOTATION_NAMES
                        },
                    )
                    kotlinOnly {
                        // We've decided to hide all nullability annotations as-kotlin even if they
                        // are present in Kotlin source, because they should not be in kotlin source
                        assertThat(annotations).isEmpty()
                    }
                    javaOnly {
                        assertThat(annotations.any { it.isAtNonNull })
                            .isEqualTo("nonna" in whichFun)
                        assertThat(annotations.any { it.isAtNullable })
                            .isEqualTo(whichFun == "nulla" && aType == typeJ)
                    }
                }
            }
        }
    }

    @Test
    fun `Function detail signature nullability is correct on example from Platform`() {
        val detail =
            """
            |/**
            | * Defines a mapping from an int value to a String. Such a mapping can be used
            | * in an @ExportedProperty to provide more meaningful values to the end user.
            | *
            | * @see android.view.ViewDebug.ExportedProperty
            | */
            |@Target({ ElementType.TYPE })
            |@Retention(RetentionPolicy.RUNTIME)
            |public @interface IntToString {
            |    /**
            |     * The original int value to map to a String.
            |     *
            |     * @return An arbitrary int value.
            |     */
            |    int from();
            |    /**
            |     * The String to use in place of the original int value.
            |     *
            |     * @return An arbitrary non-null String.
            |     */
            |    String to();
            |}
            |/**
            | * A mapping can be defined to map array indices to specific strings.
            | * A mapping can be used to see human readable values for the indices
            | * of an array:
            | *
            | * <pre>
            | * {@literal @}ViewDebug.ExportedProperty(indexMapping = {
            | *     {@literal @}ViewDebug.IntToString(from = 0, to = "INVALID"),
            | *     {@literal @}ViewDebug.IntToString(from = 1, to = "FIRST"),
            | *     {@literal @}ViewDebug.IntToString(from = 2, to = "SECOND")
            | * })
            | * private int[] mElements;
            | * <pre>
            | *
            | * @return An array of int to String mappings
            | *
            | * @see android.view.ViewDebug.IntToString
            | */
            |public IntToString[] indexMapping() default { };
        """
                .render(java = true)
                .functionDetail()
        val returnType = detail.data.returnType
        val primaryAnnotations = returnType.annotations

        kotlinOnly {
            val returnTypeGeneric = returnType.data.generics.single()
            assertThat(returnTypeGeneric.nullable).isTrue()
            assertThat(returnType.name()).isEqualTo("Array")
            assertThat(returnTypeGeneric.name()).isEqualTo("Test.IntToString")

            val genericAnnotations = returnTypeGeneric.annotations
            assertThat(genericAnnotations).isEmpty()
        }
        javaOnly { assertThat(returnType.name()).isEqualTo("Test.IntToString[]") }
        assertThat(returnType.nullable).isTrue()
        assertThat(primaryAnnotations).isEmpty()
    }

    @Test
    fun `Nullability annotations in java source are preserved, Fragment example`() {
        val signature =
            """
            |public @interface androidx.annotation.NonNull
            |/**
            | * Print internal state into the given stream.
            | *
            | * @param prefix Desired prefix to prepend at each line of output.
            | * @param fd The raw file descriptor that the dump is being sent to.
            | * @param writer The PrintWriter to which you should dump your state. This will be closed
            | *                  for you after you return.
            | * @param args additional arguments to the dump request.
            | */
            |public void onDump(@NonNull java.lang.String prefix, @Nullable java.io.FileDescriptor fd, @NonNull java.io.PrintWriter writer, @Nullable java.lang.String[] args) { throw new RuntimeException("Stub!"); }
        """
                .render(java = true)
                .functionDetail("onDump")
                .data
                .signature
        val param1 = signature.data.parameters[0]
        assertThat(param1.data.name).isEqualTo("prefix")
        assertThat(param1.typeName()).isEqualTo("String")
        assertThat(param1.nullable).isFalse()
        assertThat(param1.annotations).isEmpty()
        javaOnly { assertThat(param1.typeAnnotations().single().isAtNonNull).isTrue() }
    }

    @Test
    fun `Lambda parameter type is correct when identical to lambda return type or receiver`() {
        val summary =
            """
            |fun ScrollableState(consumeScrollDelta: Float.(Float) -> Float): ScrollableState {
            |    return DefaultScrollableState(consumeScrollDelta)
            |}
        """
                .render()
                .functionSummary("ScrollableState")
                .data
                .description
        kotlinOnly {
            val parameter = summary.data.signature.data.parameters.single()
            val lambda = (parameter.data.type as LambdaTypeProjectionComponent)
            assertThat(lambda.data.returnType.name()).isEqualTo("Float")
            assertThat(lambda.data.receiver!!.name()).isEqualTo("Float")
            assertThat(lambda.data.lambdaParams.size).isEqualTo(1)
            assertThat(lambda.data.lambdaParams.single().typeName()).isEqualTo("Float")
        }
    }

    @Test
    fun `Function detail component is marked as function type`() {
        val detail =
            """
            |fun foo()
        """
                .render()
                .functionDetail()

        assertThat(detail.data.symbolKind).isEqualTo(SymbolKind.FUNCTION)
    }

    @Test
    fun `Function detail component creates void return type link`() {
        val detail =
            """
            |fun foo() = Unit
        """
                .render()
                .functionDetail()

        val returnType = detail.data.returnType

        javaOnly {
            assertThat(returnType.link().name).isEqualTo("void")
            assertThat(returnType.link().url).isEmpty()
        }
        kotlinOnly {
            assertThat(returnType.link().name).isEqualTo("Unit")
            assertThat(returnType.link().url)
                .isEqualTo("https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html")
        }
    }

    @Test
    fun `Function detail component has correct anchors`() {
        val detail =
            """
            |fun <T : Number> List<String>.foo(t: T, a: Map<String, Int>, block: String.(Float) -> Double) = Unit
        """
                .render()
                .functionDetail()

        assertThat(detail.data.anchors)
            .containsExactly(
                "(kotlin.collections.List).foo(kotlin.Number,kotlin.collections.Map,kotlin.Function2)",
                "(kotlin.collections.List)" +
                    ".foo(kotlin.Number, kotlin.collections.Map, kotlin.Function2)",
                "-kotlin.collections.List-.foo-kotlin.Number-kotlin.collections.Map-kotlin.Function2-",
                "foo",
            )
    }

    @Test
    fun `Where statement generates correct bounds`() {
        val signatureK =
            """
            |fun <T> copyWhenGreater(list: List<T>, threshold: T): List<String>
            |   where T : CharSequence,
            |         T : Comparable<T> { return emptyList() }
        """
                .render()
                .functionSignature()
        val signatureJ =
            """
            |public <T extends Kotlin.CharSequence & Comparable<T>> List<String> copyWhenGreater(List<T> list, T threshold) {}
        """
                .render(java = true)
                .functionSignature()
        for (signature in listOf(signatureJ, signatureK)) {
            val bounds = signature.data.typeParameters.single().data.projections
            assertThat(bounds[0].data.type.data.name).isEqualTo("CharSequence")
            assertThat(bounds[1].data.type.data.name).isEqualTo("Comparable")
            assertThat(bounds[1].data.generics.single().data.type.data.name).isEqualTo("T")
        }
    }

    @Test // This is broken upstream: b/203682189, go/dokka-upstream-bug/2561
    fun `Overall nullability of array types is handled properly in 4x Java and Kotlin`() {
        val moduleK =
            """
            |fun foo(): IntArray?
            |fun oof(): IntArray
            |fun bar(param: List<Array<String>?>)
            |fun rab(param: List<Array<String>>)
        """
                .render()
        val moduleJ =
            """
            |public int[] foo();
            |public @NonNull int[] oof();
            |public void bar(java.util.List<int[]> param)
            |public void rab(java.util.List<@NonNull int[]> param)
        """
                .render(java = true)

        val drab = moduleJ.function("rab")!!.parameters.single()
        val genericDrab = (drab.type as GenericTypeConstructor).projections.single()
        // assertThat(genericDrab.annotations()).isNotEmpty() // This is a bug
        // assertThat(genericDrab.annotations().single().dri.classNames).contains("NonNull")

        for (module in listOf(moduleJ, moduleK)) {
            val rabSig = module.functionDetail("rab").data.signature
            val rabType = rabSig.data.parameters.single().data.type.data.generics.single()

            val fooType = module.functionDetail("foo").data.returnType
            val oofType = module.functionDetail("oof").data.returnType
            val barSig = module.functionDetail("bar").data.signature
            val barType = barSig.data.parameters.single().data.type.data.generics.single()

            assertThat(fooType.nullable).isTrue()
            assertThat(fooType.data.annotationComponents).isEmpty() // @Nullable is never injected
            assertThat(oofType.nullable).isFalse()

            assertThat(barType.nullable).isTrue()
            assertThat(barType.data.annotationComponents).isEmpty() // @Nullable is never injected

            javaOnly { assertThat(oofType.data.annotationComponents.single().isAtNonNull).isTrue() }
            kotlinOnly {
                assertThat(oofType.data.annotationComponents).isEmpty()
                assertThat(rabType.data.annotationComponents).isEmpty()
            }

            // This is a bug. We should be able to assert this for both source languages.
            // Upstream dokka does not pick up the annotation on the int[]
            if (module == moduleK) {
                assertThat(rabType.nullable).isFalse()
                javaOnly {
                    assertThat(rabType.data.annotationComponents.single().isAtNonNull).isTrue()
                }
            }
        }
    }

    @Test // This source is from compose.foundation.DarkTheme
    fun `Delegate-to-internal pattern works in Kotlin`() {
        val returnSummary =
            """
            fun isSystemInDarkTheme() = _isSystemInDarkTheme()

            internal expect fun _isSystemInDarkTheme(): Boolean
        """
                .render()
                .functionSummary("isSystemInDarkTheme")
                .returnSummary()
        assertThat(returnSummary.type.name().lowercase()).isEqualTo("boolean")
    }

    private fun assertNoLambdaStuff(data: ParameterComponent.Params) {
        assertThat(data.type is LambdaTypeProjectionComponent).isFalse()
    }

    private fun TypeSummaryItem<FunctionSignature>.returnSummary() = data.title.data

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Language.JAVA),
                arrayOf(Language.KOTLIN),
            )
    }
}
