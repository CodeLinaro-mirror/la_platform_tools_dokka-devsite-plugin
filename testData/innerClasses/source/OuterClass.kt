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

@file:Suppress("UNUSED_PARAMETER", "unused")

package dokkatest.inner

/** I'm the outsider. */
abstract class OuterClass {
    /** I need help. */
    protected abstract fun implementMe()

    /** I guide others to a treasure I cannot possess. */
    interface InnerInterface {
        /** I'm an insider. */
        fun inner(choice: InnerEnum)
    }

    /** I collect the stones. */
    open class InnerClass : OuterClass(), InnerInterface {
        public final override fun implementMe() = Unit

        override fun inner(choice: InnerEnum) = Unit
    }

    /** I'm at the bottom of the hierarchy. */
    class Leaf : InnerClass() {
        companion object {
            fun staticLeafCompanionMethod() = "Bud"
        }
    }

    /** I offer choices. */
    enum class InnerEnum {
        /** I'm first. */
        A,

        /** Second! */
        B,

        /** Third */
        C,
    }

    companion object {
        class InsideCompanionObject {}
    }
}
