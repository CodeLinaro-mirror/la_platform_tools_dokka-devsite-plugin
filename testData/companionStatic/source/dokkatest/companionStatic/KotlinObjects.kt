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

package dokkatest.companionStatic

import java.lang.Exception

// TODO: add tests around companion/static inheritance, including cross-language
// TODO: add tests for classes inside objects, and static inner classes

// Expectations for Kotlin-as:
// "Interesting" companion objects have their own page. "boring" ones do not.
//      Interesting = named OR has inherited elements
//          (TODO: or has nested classlikes or is annotated)
// In Kotlin, everything is duplicated between companion and container pages
//      EXCEPT elements the companion inherits (TODO: the nested types display should have extends)
// In Java
//      `const`s and `@JvmField`s are hoisted to the parent only
//      @JvmStatic functions (including getters and setters of @JvmStatic properties) are duplicated
//      non-@JvmStatic functions and properties are companion-only
//          We manually hoist these into parent's "companion Xs" sections iff companion not named

fun topLevelFunction() = 7

const val topLevelConst = 6.5
val topLevelProp = 6
lateinit var topLevelLateinit: String
// @JvmStatic var topLevelStaticVar = 5     // not valid
@JvmField val topLevelField = 5

// NOTE: top-level objects cannot be anonymous
object TopLevelObject {
    fun namedTopLevelObjectFun() = 5

    @JvmStatic fun namedTopLevelJvmStaticFun() = 4.75

    @JvmStatic val namedTopLevelJvmStaticProp = 4.5
    @JvmField val namedTopLevelJvmField = 4.25
    // '@JvmStatic' annotation is useless for '@JvmField' or 'const' properties
    // '@JvmField' cannot be applied to 'const' property
    const val namedTopLevelconst = 4.125
    lateinit var topLevelLateInitVar: String
    @JvmStatic lateinit var topLevelStaticLateInitVar: String

    // Modifier 'companion' is not applicable inside 'object'
    object InsideAnotherObjectObject {
        @JvmStatic val objectObjectStaticProperty = 5
        const val objectObjectNonStaticProperty = 5
    }
}

// Modifier 'open' is not applicable to '(companion )object'
object TopLevelInheritingObject : Exception() {
    fun inheritingTopLevelObjectFun() = 4

    @JvmStatic fun inheritingTopLevelJvmStaticFun() = 3.75

    @JvmStatic val inheritingTopLevelJvmStaticProp = 3.5
    @JvmField val inheritingTopLevelJvmField = 3.25
    const val inheritingTopLevelConst = 3.125
    // "modifier 'protected' is not applicable inside 'object'
    // protected val namedTopLevelObjectProp = 3
}

object TopLevelMultiInheritingObject : Exception(), Comparator<TopLevelMultiInheritingObject> {
    fun multiInheritingTopLevelObjectFun() = 3

    @JvmStatic fun multiInheritingTopLevelStaticFun() = 2.75

    @JvmStatic val multiInheritingTopLevelStaticProp = 2.5
    @JvmField val multiInheritingTopLevelField = 2.25
    const val multiInheritingTopLevelConst = 2.125

    override fun compare(
        o1: TopLevelMultiInheritingObject?,
        o2: TopLevelMultiInheritingObject?,
    ): Int {
        TODO("Not implemented")
    }
}

open class ContainerOfKotlinBoring {
    companion object {
        fun kotlinBoringCompanionObjectFun() = 2

        protected val kotlinBoringCompanionObjectProp = 1

        @JvmStatic protected fun kotlinBoringCompanionStaticFun() = 0.8

        @JvmStatic val kotlinBoringCompanionStaticProp = 0.6
        @JvmField val kotlinBoringCompanionStaticField = 0.4
        @JvmField protected val kotlinBoringCompanionStaticField2 = 0.2
        protected const val kotlinBoringCompanionConst = 0.1
    }
}

open class ContainerOfAlwaysBoring {
    companion object {
        @JvmStatic fun alwaysBoringCompanionStaticFun() = 0

        @JvmField val alwaysBoringCompanionStaticField = 0
        protected const val alwaysBoringCompanionConst = 0
    }
}

open class ContainerOfNamed {
    // Const 'val' are only allowed on top-level or in objects
    // const val nonCompanionConst = 9;
    companion object NamedCompanion {
        fun namedCompanionObjectFun() = 0

        @JvmStatic val namedCompanionStaticProp = -0.5
        protected val namedCompanionObjectProp = -1
        const val namedCompanionConst = -1.5
    }
}

open class ContainerOfInheriting {
    companion object : Exception() {
        fun inheritingCompanionObjectFun() = -2

        protected val inheritingCompanionObjectProp = -3
        protected const val inheritingCompanionConst = -4
    }
}

open class ContainerOfLateinit {
    companion object {
        var companionNotLateInitVar = "String"
        lateinit var companionLateInitVar: String
        @JvmStatic lateinit var companionStaticLateInitVar: String
        @get:JvmStatic @set:JvmStatic lateinit var companionLateInitVarWithStaticAccessors: String
    }
}

open class ContainerOfWithNested {
    companion object {
        public annotation class ExperimentalExternalReport
    }
}

// A demonstration of how Java-as-Kotlin static access works
private fun tests() {
    assert(JavaStatics.classStaticFunction() == 999)
    assert(JavaStatics.classStaticProp == 999)
    assert(ContainerOfKotlinBoring.kotlinBoringCompanionStaticField == 999.0) // Is Duplicated
    assert(
        ContainerOfKotlinBoring.Companion.kotlinBoringCompanionStaticField == 999.0
    ) // Is Duplicated
    assert(ContainerOfNamed.namedCompanionConst == 999.0) // Is Duplicated
    assert(ContainerOfNamed.NamedCompanion.namedCompanionConst == 999.0) // Is Duplicated
    // Not valid; named companions can't be accessed as Companion. However, they should still be in
    // the "Companion properties" section because they can be accessed as Container.theProp.
    // assert(ContainerOfNamed.Companion.namedCompanionConst == 999.0)
    assert(ContainerOfKotlinBoring.kotlinBoringCompanionObjectFun() == 999)

    assert(JavaStatics.InnerJavaStatics().notStaticInnerFun() == 999)
    assert(JavaStatics.InnerJavaStatics().notStaticinnerField == 999)
    assert(JavaStatics.InnerJavaStatics.staticInnerFun() == 999)
    assert(JavaStatics.InnerJavaStatics.staticInnerField == 999)
}
