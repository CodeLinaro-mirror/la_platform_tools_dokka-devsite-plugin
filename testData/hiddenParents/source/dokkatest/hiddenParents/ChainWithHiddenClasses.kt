/*
 * Copyright 2022 The Android Open Source Project
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

package dokkatest.hiddenParents

/** @hide */
open class HiddenAncestorClass {
    fun ancestorClassFunction(): Unit {}

    val ancestorClassProperty = 1
}

open class VisibleGreatGrandparentClass : HiddenAncestorClass() {
    fun greatGrandparentClassFunction(): Unit {}

    val greatGrandparentClassProperty = 2

    open fun greatGrandparentClassFunctionOverriddenByParent() = Unit
}

/** @hide */
open class HiddenGrandparentClass : VisibleGreatGrandparentClass() {
    fun grandparentClassFunction(): Unit {}

    val grandparentClassProperty = 3
}

/** @hide */
open class HiddenParentClass : HiddenGrandparentClass() {
    fun parentClassFunction(): Unit {}

    val parentClassProperty = 4

    open fun parentClassFunctionOverriddenByChild() = Unit

    // This is originally defined in a visible class, but is hidden because it is overridden by a
    // hidden class (and not later overridden by a visible class) and the RestrictTo lint check is
    // based on the method definition lowest in the class hierarchy.
    override fun greatGrandparentClassFunctionOverriddenByParent() = Unit
}

class VisibleExtendingChild : HiddenParentClass() {
    fun childFunction(): Unit {}

    val childProperty = 5

    // This is originally defined in a hidden class, but is visible because it is overridden by a
    // visible class.
    override fun parentClassFunctionOverriddenByChild() = Unit
}
