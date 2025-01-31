package com.google.devsite.transformers

import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import com.google.devsite.testing.ConverterTestBase
import com.google.devsite.testing.defaultPluginsConfiguration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Throws as ThrowsTag
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.junit.Ignore
import org.junit.Test

class DocTagsForCheckedExceptionsTest :
    BaseAbstractTest(TestLogger(DokkaConsoleLogger(LoggingLevel.WARN))) {
    private val driCorrespondence: Correspondence<ThrowsTag, String> =
        Correspondence.transforming(
            { it?.exceptionAddress?.toString() },
            "has DRI equal to",
        )

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
                analysisPlatform = "jvm"
                classpath += jvmStdlibPath!!
            }
        }
        pluginsConfigurations = defaultPluginsConfiguration
    }

    @Test
    fun `thrown exceptions are added to the doc string`() {
        testInline(
            """
            /src/com/sample/UnderTest.java
            package com.sample;
            import java.io.IOException;
            import java.util.concurrent.TimeoutException;

            public class UnderTest {
                /**
                 * Some comment.
                 */
                public void tested() throws IOException, TimeoutException {}
            }
            """
                .trimIndent(),
            configuration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                val docs: DocumentationNode =
                    (mod.packages.single().classlikes.single() as DClass)
                        .functions
                        .single()
                        .documentation
                        .values
                        .single()

                assertThat(docs.children.filterIsInstance<ThrowsTag>())
                    .comparingElementsUsing(driCorrespondence)
                    .containsExactly(
                        "java.io/IOException///PointingToDeclaration/",
                        "java.util.concurrent/TimeoutException///PointingToDeclaration/",
                    )
            }
        }
    }

    @Test
    fun `thrown exceptions are added to the empty doc string`() {
        testInline(
            """
            /src/com/sample/UnderTest.java
            package com.sample;
            import java.io.IOException;
            import java.util.concurrent.TimeoutException;

            public class UnderTest {
                public void tested() throws IOException, TimeoutException {}
            }
            """
                .trimIndent(),
            configuration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                val docs: DocumentationNode =
                    (mod.packages.single().classlikes.single() as DClass)
                        .functions
                        .single()
                        .documentation
                        .values
                        .single()

                assertThat(docs.children.filterIsInstance<ThrowsTag>())
                    .comparingElementsUsing(driCorrespondence)
                    .containsExactly(
                        "java.io/IOException///PointingToDeclaration/",
                        "java.util.concurrent/TimeoutException///PointingToDeclaration/",
                    )
            }
        }
    }

    @Test
    fun `exceptions already declared in the doc string are not added again`() {
        testInline(
            """
            /src/com/sample/UnderTest.java
            package com.sample;
            import java.io.IOException;
            import java.util.concurrent.TimeoutException;

            public class UnderTest {
                /**
                 * Some comment line.
                 * @throws IOException
                 * @throws IllegalStateException
                 */
                public void tested() throws IOException, TimeoutException {}
            }
            """
                .trimIndent(),
            configuration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                val docs: DocumentationNode =
                    (mod.packages.single().classlikes.single() as DClass)
                        .functions
                        .single()
                        .documentation
                        .values
                        .single()

                assertThat(docs.children.filterIsInstance<ThrowsTag>())
                    .comparingElementsUsing(driCorrespondence)
                    .containsExactly(
                        "java.io/IOException///PointingToDeclaration/",
                        "java.util.concurrent/TimeoutException///PointingToDeclaration/",
                        "java.lang/IllegalStateException///PointingToDeclaration/",
                    )
            }
        }
    }

    @Test
    fun `doc string is not changed when it lists all the exceptions`() {
        testInline(
            """
            /src/com/sample/UnderTest.java
            package com.sample;
            import java.io.IOException;
            import java.util.concurrent.TimeoutException;

            public class UnderTest {
                /**
                 * Some comment line.
                 * @throws IOException
                 * @throws IllegalStateException
                 * @throws TimeoutException
                 */
                public void tested() throws IOException, TimeoutException {}
            }
            """
                .trimIndent(),
            configuration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                val docs: DocumentationNode =
                    (mod.packages.single().classlikes.single() as DClass)
                        .functions
                        .single()
                        .documentation
                        .values
                        .single()

                assertThat(docs.children.filterIsInstance<ThrowsTag>())
                    .comparingElementsUsing(driCorrespondence)
                    .containsExactly(
                        "java.io/IOException///PointingToDeclaration/",
                        "java.util.concurrent/TimeoutException///PointingToDeclaration/",
                        "java.lang/IllegalStateException///PointingToDeclaration/",
                    )
            }
        }
    }

    @Test
    @Ignore("Needs fixes in dokka")
    fun `checked exceptions are documented for kotlin inheritors`() {
        testInline(
            """
            /src/com/sample/Trait.java
            package com.sample;
            import java.io.IOException;
            import java.util.concurrent.TimeoutException;

            public interface Trait {
                public void tested() throws IOException, TimeoutException
            }

            /src/com/sample/Impl.kt
            package com.sample

            class Impl: Trait {
                fun tested(): Unit = println(7)
            }
            """
                .trimIndent(),
            configuration,
            pluginOverrides = listOf(ConverterTestBase.NoopPlugin),
        ) {
            documentablesTransformationStage = { mod ->
                val docs: DocumentationNode =
                    (mod.packages.single().classlikes.single() as DClass)
                        .functions
                        .single()
                        .documentation
                        .values
                        .single()

                assertThat(docs.children.filterIsInstance<ThrowsTag>())
                    .comparingElementsUsing(driCorrespondence)
                    .containsExactly(
                        "java.io/IOException///PointingToDeclaration/",
                        "java.util.concurrent/TimeoutException///PointingToDeclaration/",
                    )
            }
        }
    }
}
