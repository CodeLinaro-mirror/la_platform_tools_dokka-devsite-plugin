/*
 * Copyright 2021 The Android Open Source Project
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

package dokkatest.inheritance;

import java.util.List;

public class JavaSubClass extends KotlinSuperClass {
    /**
     * javaSubClassFunction docs
     *
     * @param bar JavaSubClassFooDocs
     */
    public String javaSubClassFunction(String bar) {
        return "JavaSubClass";
    }

    // Expected: KotlinLeafClass : JavaSubClass, when accessed from Java, preserves this boxing diff
    // Actual: all do not appear properly in KotlinLeafClass as-Java. b/234132128
    public int unboxed = 5;
    public Integer boxed = 5;
    public int[] arrayOfUnboxed = {};
    public Integer[] arrayOfBoxed = {};
    public List<Integer> listOfBoxed = null;
}
