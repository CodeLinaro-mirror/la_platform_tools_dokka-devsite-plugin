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

package androidx.collection

actual open class FooSparseArray<E> {
    actual constructor(initialCapacity: Int)

    internal actual var garbage: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    internal actual var keys: LongArray
        get() = TODO("Not yet implemented")
        set(value) {}

    internal actual var values: Array<Any?>
        get() = TODO("Not yet implemented")
        set(value) {}

    actual open fun size(): Int {
        TODO("Not yet implemented")
    }

    internal actual var size: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    actual open operator fun get(key: Long): E? {
        TODO("Not yet implemented")
    }

    @Suppress(names = ["KotlinOperator"])
    actual open fun get(key: Long, defaultValue: E): E {
        TODO("Not yet implemented")
    }

    @Deprecated(
        message = "Alias for `remove(key)`.",
        replaceWith = ReplaceWith(expression = "remove(key)"),
    )
    actual open fun delete(key: Long) {}

    actual open fun remove(key: Long) {}

    actual open fun remove(key: Long, value: E): Boolean {
        TODO("Not yet implemented")
    }

    actual open fun removeAt(index: Int) {}

    actual open fun replace(key: Long, value: E): E? {
        TODO("Not yet implemented")
    }

    actual open fun replace(key: Long, oldValue: E, newValue: E): Boolean {
        TODO("Not yet implemented")
    }

    actual open fun put(key: Long, value: E) {}

    actual open fun putAll(other: FooSparseArray<out E>) {}

    actual open fun putIfAbsent(key: Long, value: E): E? {
        TODO("Not yet implemented")
    }

    actual open fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    actual open fun keyAt(index: Int): Long {
        TODO("Not yet implemented")
    }

    actual open fun valueAt(index: Int): E {
        TODO("Not yet implemented")
    }

    actual open fun setValueAt(index: Int, value: E) {}

    actual open fun indexOfKey(key: Long): Int {
        TODO("Not yet implemented")
    }

    actual open fun indexOfValue(value: E): Int {
        TODO("Not yet implemented")
    }

    actual open fun containsKey(key: Long): Boolean {
        TODO("Not yet implemented")
    }

    actual open fun containsValue(value: E): Boolean {
        TODO("Not yet implemented")
    }

    actual open fun clear() {}

    actual open fun append(key: Long, value: E) {}
}
