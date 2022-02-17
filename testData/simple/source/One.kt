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

package dokkatest.simple

import dokkatest.simple.Four

/**
 * Sample class 1 has a summary.
 * <br>
 * And details.
 * @constructor Primary constructor docs
 */
open class One(override val fore: String) : Four, Five() {

    /** Secondary constructor docs */
    constructor(blarg: Int) : this("$blarg")

    /** Property docs. */
    val v = 0

    /** Function docs. */
    fun w() = Unit
}

open class Five {
    /** Five Function docs. */
    fun fiveFunction() = Unit

    open fun thisShouldShowUpKt() = 0

    /** @suppress */
    open fun thisShouldNotShowUpKt() = 0

    /** @hide */
    open fun thisShouldNotShowUpItsKotlinHiddenWithTheAtHideWhichWeExplicitlyImplemented() = 0
}
