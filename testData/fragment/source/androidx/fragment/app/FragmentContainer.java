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

import android.content.Context;
import android.os.Bundle;

/**
 * Callbacks to a {@link androidx.fragment.app.Fragment Fragment}'s container.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class FragmentContainer {

public FragmentContainer() { throw new RuntimeException("Stub!"); }

/**
 * Return the view with the given resource ID. May return {@code null} if the
 * view is not a child of this container.
 */

@androidx.annotation.Nullable
public abstract android.view.View onFindViewById(int id);

/**
 * Return {@code true} if the container holds any view.
 */

public abstract boolean onHasView();

/**
 * Creates an instance of the specified fragment, can be overridden to construct fragments
 * with dependencies, or change the fragment being constructed. By default just calls
 * {@link androidx.fragment.app.Fragment#instantiate(android.content.Context,java.lang.String,android.os.Bundle) Fragment#instantiate(Context, String, Bundle)}.
 * @deprecated Use {@link androidx.fragment.app.FragmentManager#setFragmentFactory FragmentManager#setFragmentFactory} to control how Fragments are
 * instantiated.
 */

@Deprecated
@androidx.annotation.NonNull
public androidx.fragment.app.Fragment instantiate(@androidx.annotation.NonNull android.content.Context context, @androidx.annotation.NonNull java.lang.String className, @androidx.annotation.Nullable android.os.Bundle arguments) { throw new RuntimeException("Stub!"); }
}

