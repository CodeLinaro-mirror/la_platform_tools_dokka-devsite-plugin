/*
 * Copyright (C) 2020 The Android Open Source Project
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

// This project confirms that Dokka can be invoked as a .jar from the command line

val generatedDir = project.file("${buildDir}/docs")

configurations.create("runnerJar")

dependencies.add(
    "runnerJar",
    project.dependencies.project(
        mapOf(
            "path" to ":",
            "configuration" to "shadow"
        )
    )
)

tasks.register<Copy>("generateConfig") {
    from("src/test/resources")
    into("${project.buildDir}/resources")
    filter { text ->
        // Dackka requires absolute paths, so expand "$projectDir" into its value
        text.replace("\$projectDir", "$projectDir")
    }
}

tasks.register<JavaExec>("run") {
    description = "Ensures that dackka can be invoked as a .jar from the command line"

    dependsOn("generateConfig", project.configurations.getByName("runnerJar"))
    classpath = files({
        project.configurations.getByName("runnerJar").resolvedConfiguration.files
    })
    outputs.dir(generatedDir)

    args = listOf(
        "${project.buildDir}/resources/config.json",
        "-moduleName", "sample",
        "-loggingLevel", "WARN"
    )

    /* doFirst {
        generatedDir.deleteRecursively()
    }*/
}

tasks.register("verifyRun") {
    dependsOn("run")
    inputs.dir(generatedDir)
    outputs.file(File(temporaryDir, "fake-output-for-up-to-date-checks"))

    doLast {
        val expectedPaths = listOf(
            "reference/androidx/index.html",
            "reference/androidx/classes.html",
            "reference/androidx/packages.html"
        )

        for (relativePath in expectedPaths) {
            val path = "${generatedDir}/$relativePath"
            if (!file(path).exists()) {
                throw GradleException("Failed to create $path")
            }
        }
    }
}

tasks.register("test") {
    // dependsOn("verifyRun")
}
