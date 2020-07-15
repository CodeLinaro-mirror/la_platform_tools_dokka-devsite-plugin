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
    filter({ text ->
        // Dackka requires absolute paths, so expand "$projectDir" into its value
        text.replace("\$projectDir", "$projectDir")
    })
}

tasks.register<JavaExec>("run") {
    description = "Ensures that dackka can be invoked as a .jar from the command line"
    dependsOn(project.configurations.getByName("runnerJar"))
    dependsOn("generateConfig")
    classpath = files({
        project.configurations.getByName("runnerJar").resolvedConfiguration.files
    })
    args = listOf("${project.buildDir}/resources/config.json")
    doFirst {
        generatedDir.deleteRecursively()
    }
}

tasks.register<Task>("verifyRun") {
    dependsOn("run")
    doLast {
        val expectedPaths = listOf("index.html",
            "androidx.paging/Pager.html",
            "androidx.paging/PagingDataKt.html",
            "androidx.paging/PagingData.html")
        for (relativePath in expectedPaths) {
            val path = "${generatedDir}/$relativePath"
            if (!file(path).exists()) {
                throw GradleException("Failed to create " + path)
            }
        }
    }
}
tasks.register<Task>("test") {
    dependsOn("verifyRun")
}
