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


package androidx.fragment.app;

import androidx.lifecycle.LifecycleOwner;
import android.os.Bundle;

/**
 * A class that manages passing data between fragments.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public interface FragmentResultOwner {

/**
 * Sets the given result for the requestKey. This result will be delivered to a
 * {@link androidx.fragment.app.FragmentResultListener FragmentResultListener} that is called given to
 * {@link #setFragmentResultListener(java.lang.String,androidx.lifecycle.LifecycleOwner,androidx.fragment.app.FragmentResultListener)} with
 * the same requestKey. If no {@link androidx.fragment.app.FragmentResultListener FragmentResultListener} with the same key is set or the
 * Lifecycle associated with the listener is not at least
 * {@link androidx.lifecycle.Lifecycle.State#STARTED}, the result is stored until one becomes
 * available, or {@link #clearFragmentResult(java.lang.String)} is called with the same requestKey.
 *
 * @param requestKey key used to identify the result
 * @param result the result to be passed to another fragment
 */

public void setFragmentResult(@androidx.annotation.NonNull java.lang.String requestKey, @androidx.annotation.NonNull android.os.Bundle result);

/**
 * Clears the stored result for the given requestKey.
 *
 * This clears any result that was previously set via
 * {@link #setFragmentResult(java.lang.String,android.os.Bundle)} that hasn't yet been delivered to a
 * {@link androidx.fragment.app.FragmentResultListener FragmentResultListener}.
 *
 * @param requestKey key used to identify the result
 */

public void clearFragmentResult(@androidx.annotation.NonNull java.lang.String requestKey);

/**
 * Sets the {@link androidx.fragment.app.FragmentResultListener FragmentResultListener} for a given requestKey. Once the given
 * {@link androidx.lifecycle.LifecycleOwner LifecycleOwner} is at least in the {@link androidx.lifecycle.Lifecycle.State#STARTED}
 * state, any results set by {@link #setFragmentResult(java.lang.String,android.os.Bundle)} using the same
 * requestKey will be delivered to the
 * {@link androidx.fragment.app.FragmentResultListener#onFragmentResult(java.lang.String,android.os.Bundle) FragmentResultListener#onFragmentResult(String, Bundle)}. The callback will
 * remain active until the LifecycleOwner reaches the
 * {@link androidx.lifecycle.Lifecycle.State#DESTROYED} state or
 * {@link #clearFragmentResultListener(java.lang.String)} is called with the same requestKey.
 *
 * @param requestKey requestKey used to identify the result
 * @param lifecycleOwner lifecycleOwner for handling the result
 * @param listener listener for result changes
 */

public void setFragmentResultListener(@androidx.annotation.NonNull java.lang.String requestKey, @androidx.annotation.NonNull androidx.lifecycle.LifecycleOwner lifecycleOwner, @androidx.annotation.NonNull androidx.fragment.app.FragmentResultListener listener);

/**
 * Clears the stored {@link androidx.fragment.app.FragmentResultListener FragmentResultListener} for the given requestKey.
 *
 * This clears any {@link androidx.fragment.app.FragmentResultListener FragmentResultListener} that was previously set via
 * {@link #setFragmentResultListener(java.lang.String,androidx.lifecycle.LifecycleOwner,androidx.fragment.app.FragmentResultListener)}.
 *
 * @param requestKey key used to identify the result
 */

public void clearFragmentResultListener(@androidx.annotation.NonNull java.lang.String requestKey);
}

