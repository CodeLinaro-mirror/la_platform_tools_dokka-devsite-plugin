/*
 * Copyright 2018 The Android Open Source Project
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


package androidx.fragment.app;


/**
 * Interface used to control the instantiation of {@link androidx.fragment.app.Fragment Fragment} instances.
 * Implementations can be registered with a {@link androidx.fragment.app.FragmentManager FragmentManager} via
 * {@link androidx.fragment.app.FragmentManager#setFragmentFactory(androidx.fragment.app.FragmentFactory) FragmentManager#setFragmentFactory(FragmentFactory)}.
 *
 * @see androidx.fragment.app.FragmentManager#setFragmentFactory(FragmentFactory)
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class FragmentFactory {

public FragmentFactory() { throw new RuntimeException("Stub!"); }

/**
 * Parse a Fragment Class from the given class name. The resulting Class is kept in a global
 * cache, bypassing the {@link java.lang.Class#forName(java.lang.String) Class#forName(String)} calls when passed the same
 * class name again.
 *
 * @param classLoader The default classloader to use for loading the Class
 * @param className The class name of the fragment to parse.
 * @return Returns the parsed Fragment Class
 * @throws androidx.fragment.app.Fragment.InstantiationException If there is a failure in parsing
 * the given fragment class.  This is a runtime exception; it is not
 * normally expected to happen.
 */

@androidx.annotation.NonNull
public static java.lang.Class<? extends androidx.fragment.app.Fragment> loadFragmentClass(@androidx.annotation.NonNull java.lang.ClassLoader classLoader, @androidx.annotation.NonNull java.lang.String className) { throw new RuntimeException("Stub!"); }

/**
 * Create a new instance of a Fragment with the given class name. This uses
 * {@link #loadFragmentClass(java.lang.ClassLoader,java.lang.String)} and the empty
 * constructor of the resulting Class by default.
 *
 * @param classLoader The default classloader to use for instantiation
 * @param className The class name of the fragment to instantiate.
 * @return Returns a new fragment instance.
 * @throws androidx.fragment.app.Fragment.InstantiationException If there is a failure in instantiating
 * the given fragment class.  This is a runtime exception; it is not
 * normally expected to happen.
 */

@androidx.annotation.NonNull
public androidx.fragment.app.Fragment instantiate(@androidx.annotation.NonNull java.lang.ClassLoader classLoader, @androidx.annotation.NonNull java.lang.String className) { throw new RuntimeException("Stub!"); }
}

