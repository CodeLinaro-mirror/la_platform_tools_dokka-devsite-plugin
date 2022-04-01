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

package dokkatest.kotlin

import kotlin.jvm.JvmField

open class KotlinSuperClass {
    /**
     * My kotlin super class public property
     */
    var kotlinSuperProperty: Int = 0
    /**
     * My kotlin super class public field
     */
    @JvmField
    var kotlinSuperField: Int = 0
    /** prop1 property */
    var prop1: Int = 1
    /** prop2 property */
    val prop2: Int = 2
    /** prop3 property */
    @get:JvmName("myNewProp3")
    var prop3: Int = 3
    /** prop4 property */
    @JvmField
    var prop4: Int = 4
    fun myFun(): Int = 2
    private fun myPrivateFun(): Int = 4
}
