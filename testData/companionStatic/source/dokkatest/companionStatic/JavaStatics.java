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

package dokkatest.companionStatic;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

// TODO: add tests around companion/static inheritance, including cross-language, protected static

// Expectations for java-as:
//

public class JavaStatics {
    public static int classStaticFunction() { return 4; }
    public static int classStaticProp = 3;

    public static class InnerJavaStatics {
        public int notStaticinnerField = 2;
        public int notStaticInnerFun() { return 1; }
        public static int staticInnerField = 0;
        public static int staticInnerFun() { return -1; }

        public @interface nestedNestedInterface {}
    }

    // A demonstration of how Kotlin-as-Java static access works
    private static void tests() {
        double athing = 0;
        String astring = "";
        athing = KotlinObjectsKt.topLevelFunction();
        athing = KotlinObjectsKt.getTopLevelProp();         // Only getter
        athing = KotlinObjectsKt.topLevelConst;             // Only backing field
        athing = KotlinObjectsKt.topLevelField;             // Only backing field
        astring = KotlinObjectsKt.getTopLevelLateinit();    // Both backing field and getter
        astring = KotlinObjectsKt.topLevelLateinit;         // Both backing field and getter

        athing = TopLevelObject.INSTANCE.namedTopLevelObjectFun();
        // Not valid; TopLevelObject is a static context and namedTopLevelObjectFun is not static
        // athing = TopLevelObject.namedTopLevelObjectFun();        // DO NOT DISPLAY
        // Not valid; TopLevelObject() is an object and cannot be instantiated (no public ctor)
        // athing = TopLevelObject().namedTopLevelObjectFun();      // DO NOT DISPLAY
        athing = TopLevelObject.getNamedTopLevelJvmStaticProp();
        athing = TopLevelObject.namedTopLevelJvmStaticFun();
        athing = TopLevelObject.namedTopLevelconst;
        athing = TopLevelObject.namedTopLevelJvmField;
        athing = TopLevelInheritingObject.INSTANCE.inheritingTopLevelObjectFun();
        astring = TopLevelInheritingObject.INSTANCE.getMessage(); // From Exception
        athing = TopLevelInheritingObject.getInheritingTopLevelJvmStaticProp();
        athing = TopLevelInheritingObject.inheritingTopLevelJvmStaticFun();
        athing = TopLevelInheritingObject.inheritingTopLevelConst;
        athing = TopLevelInheritingObject.inheritingTopLevelJvmField;
        athing = TopLevelMultiInheritingObject.INSTANCE.multiInheritingTopLevelObjectFun();
        astring = TopLevelMultiInheritingObject.INSTANCE.getMessage(); // From Exception
        // astring = TopLevelMultiInheritingObject.INSTANCE.getMultiInheritingTopLevelStaticProp();
        astring = TopLevelMultiInheritingObject.INSTANCE.reversed().toString(); // From Comparator
        astring = Comparator.naturalOrder().toString();
        // Not valid; static methods cannot be inherited like this      // From Comparator
        // astring = TopLevelMultiInheritingObject.naturalOrder().toString(); // DO NOT DISPLAY
        athing = TopLevelMultiInheritingObject.getMultiInheritingTopLevelStaticProp();
        athing = TopLevelMultiInheritingObject.multiInheritingTopLevelStaticFun();
        // athing = TopLevelMultiInheritingObject.getMessage(); // Non-static method; static context
        athing = TopLevelMultiInheritingObject.multiInheritingTopLevelConst;
        athing = TopLevelMultiInheritingObject.multiInheritingTopLevelField;
        athing = ContainerOfKotlinBoring.kotlinBoringCompanionConst;                        // Parent Only
        athing = ContainerOfKotlinBoring.kotlinBoringCompanionStaticField;                  // Parent Only
        athing = ContainerOfKotlinBoring.kotlinBoringCompanionStaticField2;                 // Parent Only
        athing = ContainerOfKotlinBoring.getKotlinBoringCompanionStaticProp();              // Is Duplicated
        athing = ContainerOfKotlinBoring.kotlinBoringCompanionStaticFun();                  // Is Duplicated
        athing = ContainerOfKotlinBoring.Companion.getKotlinBoringCompanionStaticProp();    // Is Duplicated
        athing = ContainerOfKotlinBoring.Companion.kotlinBoringCompanionStaticFun();        // Is Duplicated
        athing = ContainerOfKotlinBoring.Companion.getKotlinBoringCompanionObjectProp();    // Companion Only
        athing = ContainerOfKotlinBoring.Companion.kotlinBoringCompanionObjectFun();        // Companion Only
        // athing = ContainerOfBoring.Companion.INSTANCE;   // not generated for companions
        athing = ContainerOfNamed.namedCompanionConst;                          // Parent Only
        athing = ContainerOfNamed.getNamedCompanionStaticProp();                // Is Duplicated
        athing = ContainerOfNamed.NamedCompanion.getNamedCompanionStaticProp(); // Is Duplicated
        athing = ContainerOfNamed.NamedCompanion.getNamedCompanionObjectProp(); // Companion Only
        athing = ContainerOfNamed.NamedCompanion.namedCompanionObjectFun();     // Companion Only
        // Not valid; named companion object funs should not be in companion section as-Java.
        // AND they cannot be accessed through the container.
        // I.e. as-Java, "in 'companion fields'" == "can access as Class.Companion.theField"
        // athing = ContainerOfNamed.Companion.getNamedCompanionObjectProp();   // DO NOT DISPLAY
        athing = ContainerOfInheriting.inheritingCompanionConst;                  // Parent Only
        athing = ContainerOfInheriting.Companion.getInheritingCompanionObjectProp(); // Compn'n Only
        athing = ContainerOfInheriting.Companion.inheritingCompanionObjectFun();// Companion Only
        astring = ContainerOfInheriting.Companion.getMessage(); // From Exception. Companion Only
        // Note: Container.Companion is analogous to TopLevelObject.INSTANCE, NOT TopLevelObject
        astring = TopLevelObject.topLevelStaticLateInitVar;
        astring = TopLevelObject.getTopLevelStaticLateInitVar();
        astring = TopLevelObject.topLevelLateInitVar;
        astring = TopLevelObject.INSTANCE.getTopLevelLateInitVar();
        // Gives a warning: "static member accessed via instance reference"
        astring = TopLevelObject.INSTANCE.getTopLevelStaticLateInitVar();

     //   astring = ContainerOfLateinit.companionNotLateInitVar;
        astring = ContainerOfLateinit.companionLateInitVar;
        astring = ContainerOfLateinit.companionStaticLateInitVar;

     //   astring = ContainerOfLateinit.getCompanionNotLateInitVar();
     //   astring = ContainerOfLateinit.getCompanionLateInitVar();
        astring = ContainerOfLateinit.getCompanionStaticLateInitVar();

        astring = ContainerOfLateinit.Companion.getCompanionNotLateInitVar();
        astring = ContainerOfLateinit.Companion.getCompanionLateInitVar();
        // Does not give a warning about static member from instance reference
        astring = ContainerOfLateinit.Companion.getCompanionStaticLateInitVar();

     //   astring = ContainerOfLateinit.Companion.companionNotLateInitVar;
     //   astring = ContainerOfLateinit.Companion.companionLateInitVar;
     //   astring = ContainerOfLateinit.Companion.companionStaticLateInitVar;

        // astring = ContainerOfLateinit.Companion.companionLateInitVarWithStaticAccessors;
        astring = ContainerOfLateinit.companionLateInitVarWithStaticAccessors;
        astring = ContainerOfLateinit.Companion.getCompanionLateInitVarWithStaticAccessors();
        astring = ContainerOfLateinit.getCompanionLateInitVarWithStaticAccessors();
        ContainerOfLateinit.Companion.setCompanionLateInitVarWithStaticAccessors(astring);
        ContainerOfLateinit.setCompanionLateInitVarWithStaticAccessors(astring);

        // In conclusion, lateinit vars have a hoisted public static backing field
        // but not a hoisted public static getter
    }
}
