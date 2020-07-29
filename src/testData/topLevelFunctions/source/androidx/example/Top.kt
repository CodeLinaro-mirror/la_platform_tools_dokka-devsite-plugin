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

package androidx.example

/** Humpty Dumpty sat on a wall... */
const val bool = true

val hello = Foo()

/** The Wheels on the Bus go round and round... */
val String.world: Int get() = 42

fun a() = Unit

/** Top level function docs next to [a]. This is a second sentence. Talk to [world]. */
fun b(i1: Int, i2: String, i3: Boolean, foo: Foo): String = ""

/** I'm so sad, they're deleteing me! */
@Deprecated("This method was too sad.")
fun sadBoi(): Nothing = error("Ouch")

class Foo
