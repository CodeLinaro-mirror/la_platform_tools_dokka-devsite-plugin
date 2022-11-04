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

package dokkatest.sampleAnnotation


/**
 * top-level extension property docs
 *
 * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
 *
 * and after some text, we have another sample
 *
 * @sample dokkatest.sampleAnnotation.samples.AnotherSampleInTheSameFile
 */
val List<String>.topLevelExtensionProperty get() = Pair(10f, 20f)

/**
 * top-level non-extension property docs
 *
 * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
 *
 * and this is a Java sample from Kotlin source using Kotlin syntax (does not work):
 * @BROKENsample dokkatest.sampleAnnotation.samples.FragmentArgumentsSupport.onCreate
 *
 * and this is a Java sample from Kotlin source using Java syntax:
 *
 * {@sample frameworks/support/samples/Support4Demos/src/main/java/com/example/android/supportv4/app/FragmentArgumentsSupport.java
 *      fragment}
 */
val topLevelProperty: String? = null

/**
 * top-level extension function docs
 *
 * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
 *
 * and this is an XML sample from Kotlin source:
 * {@sample frameworks/support/samples/Support4Demos/src/main/res/layout/fragment_arguments_support.xml from_attributes}
 */
fun List<String>.topLevelExtensionFunction() = Pair(10f, 20f)

/**
 * top-level non-extension function docs
 *
 * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
 */
fun topLevelFunction(): Unit {}

/**
 * top-level class docs
 *
 * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
 */
class TopLevelClass {
    /**
     * in-class extension property docs
     *
     * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
     */
    val List<String>.inClassExtensionProperty get() = Pair(10f, 20f)

    /**
     * class non-extension property docs
     *
     * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
     */
    val classProperty: String? = null

    /**
     * in-class extension function docs
     *
     * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
     */
    fun List<String>.inClassExtensionFunction() = Pair(10f, 20f)

    /**
     * top-level non-extension property docs
     *
     * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
     */
    fun classFunction(): Unit {}

    /**
     * inner class docs
     *
     * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
     */
    class InnerClass {}
    /**
     * inner interface docs
     *
     * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
     */
    interface InnerInterface {}
}

/**
 * inner class docs
 *
 * @sample dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
 */
interface topLevelInterface {}
