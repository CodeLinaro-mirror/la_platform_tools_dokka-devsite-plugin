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

@file:Suppress(
    "unused",
    "UNUSED_PARAMETER",
    "RedundantSuspendModifier",
    "REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE"
)

package dokkatest.toplevel

/** Humpty Dumpty sat on a wall... */
const val bool = true

val hello = Foo()

/** The Wheels on the Bus go round and round... */
val String.world: Int get() = 42

fun a() = Unit

/** Top level function docs next to [a]. This is a second sentence. Talk to [world]. */
fun b(i1: Int?, i2: String?, i3: Boolean, foo: Foo): String = ""

/** I'm so sad, they're deleting me! */
@Deprecated("This method was too sad.")
fun sadBoi(): Nothing = error("Ouch")

/**
 * A brutally difficult function to render. Good luck!
 *
 * ![Sarcastic laugh](https://thumbs.gfycat.com/BothFabulousHarvestmen-size_restricted.gif)
 *
 * @throws IllegalStateException because the world is broken
 * @param a choose your own adventure!
 * @param c any number of ints
 * @return a list of... something?
 * @receiver what is even going on here
 * @param block Lots, and LOTS of lambdas. Oh, and suspending ones too.
 * @see b it's a lot simpler
 * @param stuff lambdas
 * @author Nobody cares :(
 */
@Wassup("hello", ["world"])
suspend inline fun <@Wassup T, R : @Wassup Number> (T.(@Wassup Int) -> List<@Wassup R>).foo(
    a: T,
    vararg c: Int,
    crossinline stuff: @Wassup(a = "bar") () -> (() -> String?)? = { { "Nests." } },
    @Wassup block: suspend Set<Boolean>.(
        cache: Map<String?, List<T>>,
        mapper: ((Double?) -> Double)
    ) -> @Wassup(f = ["foo"]) Collection<R?>
): () -> List<Float>? = { emptyList() }

class Foo

/** Howdy! */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class Wassup(val a: String = "", val f: Array<String> = [])

typealias Bar = Foo
