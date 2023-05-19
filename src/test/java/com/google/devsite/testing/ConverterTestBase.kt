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

package com.google.devsite.testing

import com.google.common.truth.Truth.assertThat
import com.google.devsite.DevsiteConfiguration
import com.google.devsite.DevsitePlugin
import com.google.devsite.TypeSummaryItem
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.PackageSummary
import com.google.devsite.components.symbols.AnnotationComponent
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.joinMaybePrefix
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.AnnotationDocumentableConverter
import com.google.devsite.renderer.converters.DocTagConverter
import com.google.devsite.renderer.converters.FunctionDocumentableConverter
import com.google.devsite.renderer.converters.MetadataConverter
import com.google.devsite.renderer.converters.ModifierHints
import com.google.devsite.renderer.converters.NonKmpPackageConverter
import com.google.devsite.renderer.converters.Nullability
import com.google.devsite.renderer.converters.ParameterDocumentableConverter
import com.google.devsite.renderer.converters.PropertyDocumentableConverter
import com.google.devsite.renderer.converters.isFromBaseClass
import com.google.devsite.renderer.impl.ClassGraph
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.computeDocumentablesGraph
import com.google.devsite.renderer.impl.paths.DefaultExternalDokkaLocationProvider
import com.google.devsite.renderer.impl.paths.DevsiteFilePathProvider
import com.google.devsite.renderer.impl.paths.ExternalDokkaLocationProvider
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.translators.descriptors.DefaultExternalDocumentablesProvider
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.toJsonString
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.mockito.Mockito
import java.io.File
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal abstract class ConverterTestBase(
    private val displayLanguage: Language = Language.JAVA
) : BaseAbstractTest() {
    protected fun List<String>.render(): DModule = testWithRootPageNode(this)

    protected fun String.render(
        java: Boolean = false,
        fileUseAnnotation: String = ""
    ): DModule = if (java) {
        testJavaWithRootPageNode(trimMargin())
    } else {
        testKotlinWithRootPageNode(trimMargin(), fileUseAnnotation)
    }

    protected fun String.renderJava(imports: List<String> = emptyList()) =
        testJavaWithRootPageNode(trimMargin(), imports)

    protected fun DModule.classlike(name: String? = null) = name?.let { explicitClasslike(it) }
        ?: packages.single().classlikes.firstOrNull { it.name !in listOf("Nullable", "NonNull") }

    protected fun DModule.explicitClasslike(name: String) =
        this.explicitClasslikes(name).singleOrNull() ?: throw RuntimeException()

    protected fun DModule.explicitClasslikes(name: String): List<DClasslike> {
        val normalClasses = packages.flatMap { it.classlikes }
            .mapNotNull { it.explicitSubClasslike(name) }

        val (holder, _) = holderAndProvider(this)
        val synthetics = packages.flatMap {
            holder.computeSyntheticClasses(it).filter { it.name == name }
        }
        return normalClasses + synthetics
    }

    private fun DClasslike.explicitSubClasslike(name: String): DClasslike? =
        if (this.name == name) this
        else this.classlikes.mapNotNull { it.explicitSubClasslike(name) }.singleOrNull()

    protected fun DModule.function(name: String? = null, classname: String? = null) =
        packages.single().functions.singleOrNull { it.name == name }
            ?: classlike(classname)?.functions?.singleOrNull { it.name == name }
            ?: packages.single().functions.singleOrNull { !it.dri.isFromBaseClass() }
            ?: classlike(classname)?.functions?.singleOrNull { !it.dri.isFromBaseClass() }

    protected fun DModule.constructor() = constructors().single()

    protected fun DModule.constructors() = (classlike() as? DClass)?.constructors?.ifEmpty { null }
        ?: (classlike()?.classlikes?.single() as DClass).constructors

    protected fun DModule.functions() =
        packages.single().functions.ifEmpty { null }
            ?: classlike()?.functions?.ifEmpty { null }
            ?: classlike()?.classlikes?.single()?.functions // Go down another layer for java

    protected fun DModule.property(name: String? = null) =
        packages.single().properties.singleOrNull { it.name == name }
            ?: classlike()?.properties?.singleOrNull { it.name == name }
            ?: packages.single().properties.singleOrNull()
            ?: classlike()?.properties?.singleOrNull()

    protected fun DModule.properties() =
        packages.single().properties.ifEmpty { null }
            ?: classlike()?.properties

    protected fun assertPath(actual: String, expected: String, prefix: String = "") {
        when (displayLanguage) {
            Language.JAVA -> assertThat(actual).isEqualTo("$prefix/reference/$expected")
            Language.KOTLIN -> assertThat(actual).isEqualTo("$prefix/reference/kotlin/$expected")
        }
    }

    protected fun pathProvider(
        externalLocationProvider: ExternalDokkaLocationProvider? = null,
        classGraph: ClassGraph = emptyMap()
    ): FilePathProvider {
        val documentablesGraph = computeDocumentablesGraph(classGraph)
        return when (displayLanguage) {
            Language.JAVA ->
                DevsiteFilePathProvider(
                    language = Language.JAVA,
                    docRootPath = "reference",
                    languagePath = "",
                    projectPath = "androidx",
                    locationProvider = externalLocationProvider,
                    classGraph = classGraph,
                    documentablesGraph = documentablesGraph,
                    includedHeadTagsPath = "_shared/_reference-head-tags.html",
                )
            Language.KOTLIN ->
                DevsiteFilePathProvider(
                    language = Language.KOTLIN,
                    docRootPath = "reference",
                    languagePath = "kotlin",
                    projectPath = "androidx",
                    locationProvider = externalLocationProvider,
                    classGraph = classGraph,
                    documentablesGraph = documentablesGraph,
                    includedHeadTagsPath = "_shared/_reference-head-tags.html",
                )
        }
    }

    protected fun javaOnly(block: () -> Unit) {
        if (displayLanguage == Language.JAVA) {
            block()
        }
    }

    protected fun kotlinOnly(block: () -> Unit) {
        if (displayLanguage == Language.KOTLIN) {
            block()
        }
    }

    private val externalLinks = mapOf(
        "coroutines" to "https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core",
        "android" to "https://developer.android.com/reference",
        "guava" to "https://guava.dev/releases/18.0/api/docs/package-list",
        "kotlin" to "https://kotlinlang.org/api/latest/jvm/stdlib/"
    ).map {
        ExternalDocumentationLink(
            url = URL(it.value),
            packageListUrl = File("testData").toPath()
                .resolve("package-lists/${it.key}/package-list").toUri().toURL()
        )
    }
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main")
                classpath = listOfNotNull(jvmStdlibPath, commonStdlibPath)
                externalDocumentationLinks = externalLinks
                documentedVisibilities = setOf(
                    DokkaConfiguration.Visibility.PUBLIC,
                    DokkaConfiguration.Visibility.PROTECTED
                )
            }
        }
        offlineMode = true
        pluginsConfigurations = mutableListOf(
            PluginConfigurationImpl(
                fqPluginName = "com.google.devsite.DevsitePlugin",
                serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
                values = DevsiteConfiguration(
                    docRootPath = "reference",
                    projectPath = "androidx",
                    excludedPackages = null,
                    excludedPackagesForJava = null,
                    excludedPackagesForKotlin = null,
                    libraryMetadataFilename = null,
                    javaDocsPath = "",
                    kotlinDocsPath = "kotlin",
                    packagePrefixToRemoveInToc = null,
                    baseSourceLink = null,
                    annotationsNotToDisplay = null,
                    annotationsNotToDisplayJava = null,
                    annotationsNotToDisplayKotlin = null,
                ).toJsonString()
            )
        )
    }
    private val dokkaGenerator =
        DokkaGenerator(configuration, DokkaConsoleLogger(LoggingLevel.WARN))
    private val context = dokkaGenerator.initializePlugins(
        configuration,
        DokkaConsoleLogger(LoggingLevel.WARN),
        emptyList()
    )
    private val mockRootPageNode: RootPageNode = Mockito.mock(RootPageNode::class.java)
    // This provider will only work on external links; the mock will fail it on internal links.
    // However, dackka has other methods for resolving internal links, so this is fine.
    internal val externalProvider =
        DefaultExternalDokkaLocationProvider(DokkaLocationProvider(mockRootPageNode, context))
    internal val externalDocumentablesProvider =
        DefaultExternalDocumentablesProvider(context)

    internal fun holderAndProvider(
        module: DModule,
        baseSourceLink: String? = null,
        hiddenAnnotations: Set<String> = emptySet()
    ): Pair<DocumentablesHolder, FilePathProvider> {
        val holder = runBlocking {
            DocumentablesHolder(
                displayLanguage,
                module,
                this,
                context = context,
                externalDocumentablesProvider = externalDocumentablesProvider,
                baseSourceLink = baseSourceLink,
                annotationsNotToDisplay = hiddenAnnotations
            )
        }
        val classGraph = runBlocking { holder.classGraph() }
        val pathProvider = pathProvider(
            externalLocationProvider = externalProvider,
            classGraph = classGraph
        )
        return holder to pathProvider
    }

    protected fun testWithRootPageNode(sourceFiles: List<String>): DModule = runBlocking {
        suspendCoroutine { cont ->
            testInline(
                sourceFiles.joinToString("\n\n"),
                configuration,
                pluginOverrides = listOf(NoopPlugin),
                loggerForTest = DokkaConsoleLogger(LoggingLevel.WARN)
            ) {
                renderingStage = { node: RootPageNode, _: DokkaContext ->
                    val module = (node as ModulePageNode).documentables.single() as DModule
                    cont.resume(module)
                }
            }
        }
    }

    protected fun String.renderWithoutLanguageHeader() = testWithRootPageNode(listOf(trimMargin()))

    protected fun kotlinHeader(name: String = "Test", fileAnnotations: List<String> = emptyList()) =
        (
            "|/src/main/kotlin/androidx/example/$name.kt\n" +
                fileAnnotations.joinMaybePrefix(postfix = "\n", separator = "\n") +
                "|package androidx.example"
            ).trimIndent() + "\n"

    private fun testKotlinWithRootPageNode(sourceCode: String, fileUseAnnotation: String): DModule {
        val header = kotlinHeader(fileAnnotations = listOf(fileUseAnnotation))
        return testWithRootPageNode(listOf(header + "|" + sourceCode.trimIndent()))
    }

    protected fun javaHeader(name: String = "Test") = """
        |/src/main/java/androidx/example/$name.java
        |package androidx.example;
    """.trimIndent() + "\n"

    /** Java does not support file-level annotations */
    protected fun javaFullHeader(name: String = "Test", imports: String = "") = javaHeader(name) + """
            $imports
            |import java.lang.annotation.Target;
            |import static java.lang.annotation.ElementType.*;
            |import kotlin.
            |package androidx.example;
            |@Target({METHOD, PARAMETER, FIELD, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE})
            |public @interface Nullable {
            |}
            |@Target({METHOD, PARAMETER, FIELD, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE})
            |public @interface NonNull {
            |}
            |public class Test {
    """.trimIndent()

    private fun testJavaWithRootPageNode(
        sourceCode: String,
        imports: List<String> = emptyList()
    ): DModule {
        val importText = imports.joinToString() { "|import $it;\n" }
        val source = javaFullHeader(imports = importText) + "|" + sourceCode.trimIndent()
        return testWithRootPageNode(listOf(source))
    }

    object NoopPlugin : DokkaPlugin() {
        private val devsite by lazy { plugin<DevsitePlugin>() }
        @OptIn(DokkaPluginApiPreview::class)
        override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement

        val renderer by extending {
            CoreExtensions.renderer providing { NoopRenderer } override devsite.renderer
        }

        private object NoopRenderer : Renderer {
            override fun render(root: RootPageNode) = Unit
        }
    }

    protected fun DModule.packagePage(): DevsitePage<PackageSummary> {
        val (holder, provider) = holderAndProvider(this)
        val metadataConverter = MetadataConverter(holder)
        val annotationConverter = AnnotationDocumentableConverter(displayLanguage, provider, holder)
        val paramConverter =
            ParameterDocumentableConverter(displayLanguage, provider, annotationConverter)
        val javadocConverter =
            DocTagConverter(displayLanguage, provider, holder, paramConverter, annotationConverter)
        val functionConverter = FunctionDocumentableConverter(
            displayLanguage, provider, javadocConverter, paramConverter,
            annotationConverter, metadataConverter
        )
        val propertyConverter = PropertyDocumentableConverter(
            displayLanguage, provider, javadocConverter, paramConverter,
            annotationConverter, metadataConverter
        )

        val converter =
            NonKmpPackageConverter(
                displayLanguage,
                packages.single(),
                provider,
                holder,
                functionConverter,
                propertyConverter,
                javadocConverter,
                paramConverter
            )
        return runBlocking { converter.summaryPage() }
    }

    private fun DModule.functionConverter(): FunctionDocumentableConverter {
        val (holder, provider) = holderAndProvider(this)
        val metadataConverter = MetadataConverter(holder)
        val annotationConverter = AnnotationDocumentableConverter(displayLanguage, provider, holder)
        val paramConverter =
            ParameterDocumentableConverter(displayLanguage, provider, annotationConverter)
        val javadocConverter =
            DocTagConverter(displayLanguage, provider, holder, paramConverter, annotationConverter)
        return FunctionDocumentableConverter(
            displayLanguage,
            provider,
            javadocConverter,
            paramConverter,
            annotationConverter,
            metadataConverter
        )
    }

    protected fun DModule.functionSummary(
        doc: DModule.() -> DFunction = ::smartDoc,
        hints: ModifierHints = defaultHints
    ): TypeSummaryItem<FunctionSignature> {
        return functionConverter().summary(this.doc(), hints.copy(isSummary = true))!!
    }

    protected fun DModule.functionSummaries(
        hints: ModifierHints = defaultHints
    ): Map<String, TypeSummaryItem<FunctionSignature>> {
        return functions()!!.associate {
            it.name to functionConverter().summary(it, hints.copy(isSummary = true))!!
        }
    }

    protected fun DModule.functionDetail(
        doc: DModule.() -> DFunction = ::smartDoc,
        hints: ModifierHints = defaultHints
    ): SymbolDetail<FunctionSignature> {
        return functionConverter().detail(this.doc(), hints)!!
    }

    protected fun DModule.functionSignature(
        doc: DModule.() -> DFunction = ::smartDoc
    ): FunctionSignature {
        return with(functionConverter()) {
            this@functionSignature.doc().signature(isSummary = false)
        }
    }

    protected fun DModule.functionSummary(
        funName: String,
        hints: ModifierHints = defaultHints
    ) = functionSummary({ this.function(funName)!! }, hints)

    protected fun DModule.functionDetail(
        funName: String,
        hints: ModifierHints = defaultHints
    ) = functionDetail({ this.function(funName)!! }, hints)

    protected fun DModule.annotationComponents(
        annotations: List<Annotations.Annotation>,
        nullability: Nullability = Nullability.DONT_CARE,
        hiddenAnnotations: Set<String> = emptySet()
    ): List<AnnotationComponent> {
        val (holder, pathProvider) = holderAndProvider(this, hiddenAnnotations = hiddenAnnotations)
        val converter = AnnotationDocumentableConverter(
            displayLanguage,
            pathProvider,
            holder
        )
        return converter.annotationComponents(annotations, nullability)
    }

    /** In case you aren't explicit, our best guess at what you want docs for. */
    private fun smartDoc(module: DModule): DFunction {
        return module.function() ?: module.constructor()
    }

    protected open lateinit var defaultHints: ModifierHints
}
