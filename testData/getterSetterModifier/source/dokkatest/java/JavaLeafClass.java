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

package dokkatest.java;

import dokkatest.kotlin.KotlinSuperClass;

public class JavaLeafClass extends KotlinSuperClass {
    /**
     * My java leaf class private backing field
     */
    private int javaLeafBackingField;
    /**
     * My java leaf class getter docs
     */
    public int getJavaLeafBackingField() {
        return javaLeafBackingField;
    }
    /**
     * My java leaf class setter docs
     */
    public void setJavaLeafBackingField(int x) { this.javaLeafBackingField = javaLeafBackingField; }
    /**
     * My java leaf class public field
     */
    public int javaLeafField;
}
