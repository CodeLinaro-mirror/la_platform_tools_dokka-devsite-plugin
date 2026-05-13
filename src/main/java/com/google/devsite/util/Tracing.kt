/*
 * Copyright 2026 The Android Open Source Project
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

package com.google.devsite.util

import androidx.tracing.Tracer
import androidx.tracing.wire.TraceDriver
import androidx.tracing.wire.TraceSink
import java.io.File
import kotlinx.coroutines.Dispatchers
import okio.appendingSink
import okio.buffer

internal object Tracing {
    fun createTraceDriver(traceFile: String?): TraceDriver {
        return if (traceFile != null) {
            val trace = File(traceFile)
            trace.parentFile.mkdirs()
            // If the trace file already exists, overwriting part of it would corrupt the file.
            trace.delete()
            val traceSink =
                TraceSink(sequenceId = 1, trace.appendingSink().buffer(), Dispatchers.IO)
            TraceDriver(traceSink)
        } else {
            TraceDriver.getStubTraceDriver()
        }
    }
}

internal inline fun <T> Tracer.trace(
    name: String,
    metadata: Pair<String, String>? = null,
    crossinline block: () -> T,
): T {
    return trace(
        category = "main",
        name = name,
        metadataBlock = { metadata?.let { (name, value) -> addMetadataEntry(name, value) } },
        block = block,
    )
}

internal suspend inline fun <T> Tracer.traceCoroutine(
    name: String,
    metadata: Pair<String, String>? = null,
    crossinline block: suspend () -> T,
): T {
    return traceCoroutine(
        category = "main",
        name = name,
        metadataBlock = { metadata?.let { (name, value) -> addMetadataEntry(name, value) } },
        block = block,
    )
}
