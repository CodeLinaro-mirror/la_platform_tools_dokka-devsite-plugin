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
import com.google.devsite.components.Link
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.LambdaTypeProjectionComponent
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolDetail.SymbolKind
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.testing.isAtNonNull
import com.google.devsite.renderer.converters.testing.isAtNullable
import com.google.devsite.renderer.converters.testing.item
import com.google.devsite.renderer.converters.testing.items
import com.google.devsite.renderer.converters.testing.link
import com.google.devsite.renderer.converters.testing.name
import com.google.devsite.renderer.converters.testing.projectionName
import com.google.devsite.renderer.converters.testing.signature
import com.google.devsite.renderer.converters.testing.summary
import com.google.devsite.renderer.converters.testing.typeName
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.testing.ConverterTestBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DModule
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertFails

@RunWith(Parameterized::class)
internal class FunctionDocumentableConverterTest(
    private val language: Language
) : ConverterTestBase(language) {

    @Test
    fun `Top level function summary component has correct default modifiers`() {
        val summary = """
            |fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).isEmpty() }
    }

    @Test
    fun `Function summary component ignores public modifier`() {
        val summary = """
            |public fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).isEmpty() }
        javaOnly { assertThat(returnz.modifiers).containsExactly("final") }
    }

    @Test
    fun `Function summary component has suspend modifier`() {
        val summary = """
            |suspend fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).containsExactly("suspend") }
    }

    @Test
    fun `Function summary component has inline modifier`() {
        val summary = """
            |inline fun foo() = Unit
        """.render().summary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).containsExactly("inline") }
    }

    @Ignore // TODO(b/165112358): foo doesn't show up in the dokka model
    @Test
    fun `Function summary component in abstract class has protected modifier`() {
        val summary = """
            |abstract class Foo {
            |    protected open fun foo() = Unit
            |}
        """.render().summary()

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("protected")
    }

    @Test
    fun `Function summary component in abstract class has abstract modifier`() {
        val summary = """
            |abstract class Foo {
            |    abstract fun foo()
            |}
        """.render().summary()

        val returnz = summary.returnSummary()

        assertThat(returnz.modifiers).containsExactly("abstract")
    }

    @Test
    fun `Function summary component in class has open modifiers`() {
        val summary = """
            |class Foo {
            |    open fun foo() = Unit
            |}
        """.render().summary()

        val returnz = summary.returnSummary()

        kotlinOnly { assertThat(returnz.modifiers).containsExactly("open") }
        javaOnly { assertThat(returnz.modifiers).isEmpty() }
    }

    @Test
    fun `Function summary abstract before suspend`() {
        val summary = """
            |class Foo {
            |    abstract suspend fun foo(): Unit
            |}
        """.render().summary()

        val returnz = summary.returnSummary()

        kotlinOnly {
            assertThat(returnz.modifiers).containsExactly("abstract", "suspend").inOrder()
        }
        javaOnly { assertThat(returnz.modifiers).containsExactly("abstract") }
    }

    @Test
    fun `Function summary component in interface has abstract modifiers when appropriate`() {
        val summariesK = """
            |interface Foo {
            |    fun foo()
            |    fun bar() = "default implementation"
            |}
        """.render().functionSummaries(ModifierHints(language, isInterface = true))
        val summariesJ = """
            |public interface Foo {
            |    public void foo();
            |    public default void bar() {return "default implementation"; }
            |}
        """.render(java = true).functionSummaries(ModifierHints(language, isInterface = true))

        for (summaries in listOf(summariesK, summariesJ)) {
            val fooReturnz = summaries["foo"]!!.returnSummary()
            javaOnly { assertThat(fooReturnz.modifiers).containsExactly("abstract") }
            kotlinOnly { assertThat(fooReturnz.modifiers).isEmpty() }
            val barReturnz = summaries["bar"]!!.returnSummary()
            javaOnly { assertThat(barReturnz.modifiers).containsExactly("default") }
            kotlinOnly {
                if (summaries == summariesJ) assertThat(barReturnz.modifiers).isEmpty()
                else assertThat(barReturnz.modifiers).containsExactly("open") }
        }
    }

    @Test
    fun `Function summary component creates return type link`() {
        val summary = """
            |class A
            |fun foo(): A
        """.render().summary()

        val returnType = summary.returnSummary().type

        assertThat(returnType.link().name).isEqualTo("A")
        assertPath(returnType.link().url, "androidx/example/A.html")
    }

    @Test
    fun `Function summary component creates void return type link`() {
        val summary = """
            |fun foo() = Unit
        """.render().summary()

        val returnType = summary.returnSummary().type

        javaOnly {
            assertThat(returnType.link().name).isEqualTo("void")
            assertThat(returnType.link().url).isEmpty()
        }
        kotlinOnly {
            assertThat(returnType.link().name).isEqualTo("Unit")
            assertPath(returnType.link().url, "kotlin/Unit.html")
        }
    }

    @Test
    fun `Function summary component handles constructors`() {
        val summary = """
            |class MyClass
        """.render().summary()

        val constructor = summary.data.description as SymbolSummary

        assertThat(constructor.name()).isEqualTo("MyClass")
    }

    @Test
    fun `Function summary component creates return type generics`() {
        val summary = """
            |fun foo(): Map<String, List<Long>>
        """.render().summary()

        val returnz = summary.returnSummary()
        val generics = returnz.type.data.generics.items(2)
        val nestedGenerics = generics.last().data.generics.item()

        assertThat(generics.first().link().name).isEqualTo("String")
        assertThat(generics.last().link().name).isEqualTo("List")
        assertThat(nestedGenerics.link().name).isEqualTo("Long")
    }

    @Test
    fun `Function summary component creates signature with name`() {
        val summary = """
            |fun iAmACoolFunction()
        """.render().summary()

        val function = summary.summary()

        assertThat(function.name()).isEqualTo("iAmACoolFunction")
    }

    @Test
    fun `Function summary component creates extension receiver`() {
        val summary = """
            |fun String.foo()
        """.render().summary()

        val function = summary.summary()
        val signature = function.signature()

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
        val summary = """
            |fun Any.foo()
        """.render().summary()

        val function = summary.summary()
        val signature = function.signature()

        javaOnly {
            assertThat(signature.receiver).isNotNull()
            val param = signature.receiver!!
            assertNoLambdaStuff(param.data)

            val type = param.data.type
            assertThat(type.link().name).isEqualTo("TestKt")
            assertThat(type.link().url).contains("androidx")
        }

        kotlinOnly {
            assertThat(signature.receiver).isNotNull()
            val param = signature.receiver!!
            assertNoLambdaStuff(param.data)

            val type = param.data.type
            assertThat(type.link().name).isEqualTo("Any")
            assertThat(type.link().url).contains("kotlin")
            assertThat(param.data.name).isEmpty()
        }
    }

    @Test
    fun `Function summary component creates params`() {
        val summary = """
            |fun foo(a: String)
        """.render().summary()

        val function = summary.summary()
        val param = function.param()
        val paramType = param.data.type

        assertNoLambdaStuff(param.data)
        assertThat(param.data.name).isEqualTo("a")
        assertThat(paramType.link().name).isEqualTo("String")
    }

    @Test
    fun `Function summary component creates inline generics`() {
        val summary = """
            |fun <T: Number, U> foo() = Unit
        """.render().summary()
        val typeParams = summary.summary().signature().typeParameters
        assertThat(typeParams.first().data.name).isEqualTo("T")
        assertThat(typeParams.first().projectionName()).isEqualTo("Number")
        assertThat(typeParams.last().data.name).isEqualTo("U")
        kotlinOnly { assertThat(typeParams.last().projectionName()).isEqualTo("Any") }
        javaOnly { assertThat(typeParams.last().projectionName()).isEqualTo("Object") }
    }

    @Test
    fun `Function signature component creates multiple inline generics`() {
        val inlineGenerics = """
            |fun <T: Number, U: List<String>, V: T> foo() = Unit
        """.render().summary().summary().signature().typeParameters

        assertThat(inlineGenerics.map { it.data.name }).isEqualTo(listOf("T", "U", "V"))
        assertThat(inlineGenerics[0].projectionName()).isEqualTo("Number")
        assertThat(inlineGenerics[1].projectionName()).isEqualTo("List")
        val generics = inlineGenerics[1].data.projections.single().data.generics
        assertThat(generics.single().link().name).isEqualTo("String")
        assertThat(inlineGenerics[2].projectionName()).isEqualTo("T")
    }

    @Test
    fun `Function summary component has correct relative link`() {
        val summary = """
            |fun <T : Number> List<String>.foo(t: T, a: Map<String, Int>, block: String.(Float) -> Double) = Unit
        """.render().summary()

        val function = summary.summary()
        val signature = function.data.signature

        assertPath(
            signature.data.name.data.url,
            "androidx/example/package-summary.html#" +
                "(kotlin.collections.List)" +
                ".foo(kotlin.Number,kotlin.collections.Map,kotlin.Function2)"
        )
    }

    @Test
    fun `Function summary component understands Java primitives`() {
        val summary = """
            |public void foo(
            |    boolean a, int b, double c, float d, short e, long f, char g, byte h) {}
        """.render(java = true).summary()

        val function = summary.summary()
        val returnType = summary.returnSummary().type.link()
        val signature = function.signature()

        javaOnly {
            assertThat(returnType.name).isEqualTo("void")

            val expected =
                listOf("boolean", "int", "double", "float", "short", "long", "char", "byte")
            for ((i, param) in signature.parameters.withIndex()) {
                assertThat(param.data.type.link().name).isEqualTo(expected[i])
            }
        }
        kotlinOnly {
            assertThat(returnType.name).isEqualTo("Unit")

            val expected =
                listOf("Boolean", "Int", "Double", "Float", "Short", "Long", "Char", "Byte")
            for ((i, param) in signature.parameters.withIndex()) {
                assertThat(param.data.type.link().name).isEqualTo(expected[i])
            }
        }
    }

    @Test
    fun `Function summary component understands Java object`() {
        val summary = """
            |public Object foo() {}
        """.render(java = true).summary()

        val returnType = summary.returnSummary().type.link()

        javaOnly { assertThat(returnType.name).isEqualTo("Object") }
        kotlinOnly { assertThat(returnType.name).isEqualTo("Any") }
    }

    @Test // b/190477978 Patch upstream to generate default java constructor
    fun `Function summary component exists for (default) constructors`() {
        val summaryK = """
            |class Foo
        """.render().summary()
        val constructor = summaryK.data.description as SymbolSummary
        assertThat(constructor.name()).isEqualTo("Foo")

        assertFails {
            val jjj = """
            |public class Foo {}
            """.render(java = true)
            val summaryJ = jjj.summary()
            // val constructor = summaryJ.data.description as SymbolSummary
        }
    }

    @Test
    fun `Function detail component has correct name`() {
        val detail = """
            |fun foo()
        """.render().detail()

        assertThat(detail.data.name).isEqualTo("foo")
    }

    @Test
    fun `Top level function detail component has correct default modifiers`() {
        val detail = """
            |fun foo() = Unit
        """.render().detail()

        javaOnly { assertThat(detail.data.modifiers).containsExactly("public", "final") }
        kotlinOnly { assertThat(detail.data.modifiers).isEmpty() }
    }

    @Test
    fun `Top level function detail component has annotations`() {
        val detail = """
            |annotation class Hello
            |@Hello fun foo() = Unit
        """.render().detail()

        assertThat(detail.data.annotationComponents).isNotEmpty()
    }

    @Test
    fun `Function summary and detail include nullability information in 4x Kotlin and Java`() {
        val moduleJ = """
                |public @interface NotNull {}
                |@Nullable
                |public String nulla1() { return null; }
                |public String nulla2() { return null; }
                |@NonNull
                |public String nonna1() { return ""; }
                |public @NonNull String nonna2() { return ""; }
                """.render(java = true)
        val moduleK = """
                |annotation class Nullable
                |annotation class NonNull
                |@Nullable
                |fun nulla1(): String? = null
                |fun nulla2(): String? = null
                |@NonNull
                |fun nonna1(): String = "foo"
                |fun nonna2(): String = "foo"
                """.render()
        fun DModule.sOrD(summary: Boolean, functionName: String): TypeProjectionComponent =
            if (summary) (summary(functionName).data.title as TypeSummary).data.type
            else detail(functionName).data.returnType
        for (isSummary in listOf(true, false)) {
            for (whichFun in listOf("nonna1", "nonna2", "nulla1", "nulla2")) {
                val typeJ = moduleJ.sOrD(isSummary, whichFun)
                val typeK = moduleK.sOrD(isSummary, whichFun)
                for (aType in listOf(typeJ, typeK)) {
                    assertThat(aType.nullable).isEqualTo("nulla" in whichFun)
                    val annotations = aType.data.annotationComponents
                    assertThat(annotations.singleOrNull()?.name?.let {
                        it in NULLABILITY_ANNOTATION_NAMES })
                    kotlinOnly {
                        // We've decided to hide all nullability annotations as-kotlin even if they
                        // are present in Kotlin source, because they should not be in kotlin source
                        assertThat(annotations).isEmpty()
                    }
                    javaOnly {
                        assertThat(annotations.any { it.isAtNonNull })
                            .isEqualTo("nonna" in whichFun)
                        assertThat(annotations.any { it.isAtNullable })
                            .isEqualTo(whichFun == "nulla1")
                    }
                }
            }
        }
    }

    @Test
    fun `Function detail signature nullability is correct on example from Platform`() {
        val detail = """
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
            | * @see #mapping()
            | */
            |public IntToString[] indexMapping() default { };
        """.render(java = true).detail()
        val returnType = detail.data.returnType
        val primaryAnnotations = returnType.data.annotationComponents

        kotlinOnly {
            val returnTypeGeneric = returnType.data.generics.single()
            assertThat(returnTypeGeneric.nullable).isTrue()
            assertThat(returnType.name()).isEqualTo("Array")
            assertThat(returnTypeGeneric.name()).isEqualTo("Test.IntToString")

            val genericAnnotations = returnTypeGeneric.data.annotationComponents
            assertThat(genericAnnotations).isEmpty()
        }
        javaOnly {
            assertThat(returnType.name()).isEqualTo("Test.IntToString[]")
        }
        assertThat(returnType.nullable).isTrue()
        assertThat(primaryAnnotations).isEmpty()
    }

    @Test
    fun `Nullability annotations in java source are preserved, Fragment example`() {
        val signature = """
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
        """.render(java = true).detail("onDump").data.signature as FunctionSignature
        val param1 = signature.data.parameters[0]
        assertThat(param1.data.name).isEqualTo("prefix")
        assertThat(param1.typeName()).isEqualTo("String")
        assertThat(param1.nullable).isFalse()
        assertThat(param1.data.annotationComponents).isEmpty()
        javaOnly { assertThat(param1.data.type.data.annotationComponents.single().isAtNonNull) }
    }

    @Test
    fun `Lambda parameter type is correct when identical to lambda return type or receiver`() {
        val summary = """
            |fun ScrollableState(consumeScrollDelta: Float.(Float) -> Float): ScrollableState {
            |    return DefaultScrollableState(consumeScrollDelta)
            |}
        """.render().summary("ScrollableState").data.description as SymbolSummary
        kotlinOnly {
            val parameter = (summary.data.signature as FunctionSignature).data.parameters.single()
            val lambda = (parameter.data.type as LambdaTypeProjectionComponent)
            assertThat(lambda.data.type.data.name).isEqualTo("Float")
            assertThat(lambda.data.receiver!!.name()).isEqualTo("Float")
            assertThat(lambda.data.lambdaParams.size).isEqualTo(1)
            assertThat(lambda.data.lambdaParams.single().typeName()).isEqualTo("Float")
        }
    }

    @Test
    fun `Function detail component is marked as function type`() {
        val detail = """
            |fun foo()
        """.render().detail()

        assertThat(detail.data.symbolKind).isEqualTo(SymbolKind.FUNCTION)
    }

    @Test
    fun `Function detail component creates void return type link`() {
        val detail = """
            |fun foo() = Unit
        """.render().detail()

        val returnType = detail.data.returnType

        javaOnly {
            assertThat(returnType.link().name).isEqualTo("void")
            assertThat(returnType.link().url).isEmpty()
        }
        kotlinOnly {
            assertThat(returnType.link().name).isEqualTo("Unit")
            assertPath(returnType.link().url, "kotlin/Unit.html")
        }
    }

    @Test
    fun `Function detail component has correct anchors`() {
        val detail = """
            |fun <T : Number> List<String>.foo(t: T, a: Map<String, Int>, block: String.(Float) -> Double) = Unit
        """.render().detail()

        assertThat(detail.data.anchors).containsExactly(
            "(kotlin.collections.List).foo(kotlin.Number,kotlin.collections.Map,kotlin.Function2)",
            "(kotlin.collections.List)" +
                ".foo(kotlin.Number, kotlin.collections.Map, kotlin.Function2)",
            "-kotlin.collections.List-.foo-kotlin.Number-kotlin.collections.Map-kotlin.Function2-",
            "foo"
        )
    }

    @Test
    fun `Where statement generates correct bounds`() {
        val signatureK = """
            |fun <T> copyWhenGreater(list: List<T>, threshold: T): List<String>
            |   where T : CharSequence,
            |         T : Comparable<T> { return emptyList() }
        """.render().signature()
        val signatureJ = """
            |public <T extends Kotlin.CharSequence & Comparable<T>> List<String> copyWhenGreater(List<T> list, T threshold) {}
        """.render(java = true).signature()
        for (signature in listOf(signatureJ, signatureK)) {
            val bounds = signature.data.typeParameters.single().data.projections
            assertThat(bounds[0].data.type.data.name).isEqualTo("CharSequence")
            assertThat(bounds[1].data.type.data.name).isEqualTo("Comparable")
            assertThat(bounds[1].data.generics.single().data.type.data.name).isEqualTo("T")
        }
    }

    private fun assertNoLambdaStuff(data: ParameterComponent.Params) {
        assertThat(data.type is LambdaTypeProjectionComponent).isFalse()
    }

    private fun DModule.summary(
        doc: DModule.() -> DFunction = ::smartDoc,
        hints: ModifierHints = ModifierHints(language)
    ): TwoPaneSummaryItem {
        val holder = runBlocking { DocumentablesHolder(this@summary, this) }
        val classGraph = runBlocking { holder.classGraph() }
        val docConverter = DocTagConverter(language, pathProvider(classGraph = classGraph), holder)
        val converter = FunctionDocumentableConverter(
            language,
            pathProvider(classGraph = classGraph),
            docConverter
        )
        return converter.summary(this.doc(), hints.copy(isSummary = true))
    }

    private fun DModule.summary(funName: String, hints: ModifierHints = ModifierHints(language)) =
        summary({ this.function(funName)!! }, hints)

    private fun DModule.detail(funName: String, hints: ModifierHints = ModifierHints(language)) =
        detail({ this.function(funName)!! }, hints)

    private fun DModule.functionSummaries(
        hints: ModifierHints = ModifierHints(language)
    ): Map<String, TwoPaneSummaryItem> {
        val holder = runBlocking { DocumentablesHolder(this@functionSummaries, this) }
        val classGraph = runBlocking { holder.classGraph() }
        val docConverter = DocTagConverter(language, pathProvider(classGraph = classGraph), holder)
        val converter = FunctionDocumentableConverter(
            language,
            pathProvider(classGraph = classGraph),
            docConverter
        )
        return functions()!!.map {
            it.name to converter.summary(it, hints.copy(isSummary = true))
        }.toMap()
    }

    private fun DModule.detail(
        doc: DModule.() -> DFunction = ::smartDoc,
        hints: ModifierHints = ModifierHints(language)
    ): SymbolDetail {
        val holder = runBlocking { DocumentablesHolder(this@detail, this) }
        val classGraph = runBlocking { holder.classGraph() }
        val docConverter = DocTagConverter(language, pathProvider(classGraph = classGraph), holder)
        val converter = FunctionDocumentableConverter(
            language,
            pathProvider(classGraph = classGraph),
            docConverter
        )
        return converter.detail(this.doc(), hints)
    }

    private fun DModule.signature(
        doc: DModule.() -> DFunction = ::smartDoc
    ): FunctionSignature {
        val holder = runBlocking { DocumentablesHolder(this@signature, this) }
        val classGraph = runBlocking { holder.classGraph() }
        val docConverter = DocTagConverter(language, pathProvider(classGraph = classGraph), holder)
        val converter = FunctionDocumentableConverter(
            language,
            pathProvider(classGraph = classGraph),
            docConverter
        )
        return with(converter) { this@signature.doc().signature(isSummary = false) }
    }

    /** In case you aren't explicit, our best guess at what you want docs for. */
    private fun smartDoc(module: DModule): DFunction {
        return module.function() ?: module.constructor()
    }

    private fun ParameterComponent.link(): Link.Params = data.type.link()

    private fun SymbolSummary.param(): ParameterComponent = signature().parameters.item()

    private fun TwoPaneSummaryItem.returnSummary(): TypeSummary.Params =
        (data.title as TypeSummary).data

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Language.JAVA),
            arrayOf(Language.KOTLIN)
        )
    }
}
