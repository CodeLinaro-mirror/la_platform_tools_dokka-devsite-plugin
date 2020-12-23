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

import android.app.Activity;
import android.os.Parcelable;
import androidx.activity.result.ActivityResultRegistryOwner;
import java.io.PrintWriter;
import android.view.LayoutInflater;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.contract.ActivityResultContract;
import android.content.IntentSender;
import androidx.activity.result.ActivityResultCallback;

/**
 * Integration points with the Fragment host.
 * <p>
 * Fragments may be hosted by any object; such as an {@link android.app.Activity Activity}. In order to
 * host fragments, implement {@link androidx.fragment.app.FragmentHostCallback FragmentHostCallback}, overriding the methods
 * applicable to the host.
 * <p>
 * FragmentManager changes its behavior based on what optional interfaces your
 * FragmentHostCallback implements. This includes the following:
 * <ul>
 *     <li><strong>{@link androidx.activity.result.ActivityResultRegistryOwner ActivityResultRegistryOwner}</strong>: Removes the need to
 *     override {@link #onStartIntentSenderFromFragment} or
 *     {@link #onRequestPermissionsFromFragment}.</li>
 *     <li><strong>{@link androidx.fragment.app.FragmentOnAttachListener FragmentOnAttachListener}</strong>: Removes the need to
 *     manually call {@link androidx.fragment.app.FragmentManager#addFragmentOnAttachListener FragmentManager#addFragmentOnAttachListener} from your
 *     host in order to receive {@link androidx.fragment.app.FragmentOnAttachListener#onAttachFragment FragmentOnAttachListener#onAttachFragment} callbacks
 *     for the {@link androidx.fragment.app.FragmentController#getSupportFragmentManager() FragmentController#getSupportFragmentManager()}.</li>
 *     <li><strong>{@link androidx.activity.OnBackPressedDispatcherOwner}</strong>: Removes
 *     the need to manually call
 *     {@link androidx.fragment.app.FragmentManager#popBackStackImmediate() FragmentManager#popBackStackImmediate()} when handling the system
 *     back button.</li>
 *     <li><strong>{@link androidx.lifecycle.ViewModelStoreOwner}</strong>: Removes the need
 *     for your {@link androidx.fragment.app.FragmentController FragmentController} to call
 *     {@link androidx.fragment.app.FragmentController#retainNestedNonConfig() FragmentController#retainNestedNonConfig()} or
 *     {@link androidx.fragment.app.FragmentController#restoreAllState(android.os.Parcelable,androidx.fragment.app.FragmentManagerNonConfig) FragmentController#restoreAllState(Parcelable, FragmentManagerNonConfig)}.</li>
 * </ul>
 *
 * @param <E> the type of object that's currently hosting the fragments. An instance of this
 *           class must be returned by {@link #onGetHost()}.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class FragmentHostCallback<E> extends androidx.fragment.app.FragmentContainer {

public FragmentHostCallback(@androidx.annotation.NonNull android.content.Context context, @androidx.annotation.NonNull android.os.Handler handler, int windowAnimations) { throw new RuntimeException("Stub!"); }

/**
 * Print internal state into the given stream.
 *
 * @param prefix Desired prefix to prepend at each line of output.
 * @param fd The raw file descriptor that the dump is being sent to.
 * @param writer The PrintWriter to which you should dump your state. This will be closed
 *                  for you after you return.
 * @param args additional arguments to the dump request.
 */

public void onDump(@androidx.annotation.NonNull java.lang.String prefix, @androidx.annotation.Nullable java.io.FileDescriptor fd, @androidx.annotation.NonNull java.io.PrintWriter writer, @androidx.annotation.Nullable java.lang.String[] args) { throw new RuntimeException("Stub!"); }

/**
 * Return {@code true} if the fragment's state needs to be saved.
 */

public boolean onShouldSaveFragmentState(@androidx.annotation.NonNull androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Return a {@link android.view.LayoutInflater LayoutInflater}.
 * See {@link android.app.Activity#getLayoutInflater() Activity#getLayoutInflater()}.
 */

@androidx.annotation.NonNull
public android.view.LayoutInflater onGetLayoutInflater() { throw new RuntimeException("Stub!"); }

/**
 * Return the object that's currently hosting the fragment. If a {@link androidx.fragment.app.Fragment Fragment}
 * is hosted by a {@link androidx.fragment.app.FragmentActivity FragmentActivity}, the object returned here should be
 * the same object returned from {@link androidx.fragment.app.Fragment#getActivity() Fragment#getActivity()}.
 */

@androidx.annotation.Nullable
public abstract E onGetHost();

/**
 * Invalidates the activity's options menu.
 * See {@link androidx.fragment.app.FragmentActivity#supportInvalidateOptionsMenu() FragmentActivity#supportInvalidateOptionsMenu()}
 */

public void onSupportInvalidateOptionsMenu() { throw new RuntimeException("Stub!"); }

/**
 * Starts a new {@link android.app.Activity Activity} from the given fragment.
 * See {@link androidx.fragment.app.FragmentActivity#startActivityForResult(android.content.Intent,int) FragmentActivity#startActivityForResult(Intent, int)}.
 */

public void onStartActivityFromFragment(@androidx.annotation.NonNull androidx.fragment.app.Fragment fragment, android.content.Intent intent, int requestCode) { throw new RuntimeException("Stub!"); }

/**
 * Starts a new {@link android.app.Activity Activity} from the given fragment.
 * See {@link androidx.fragment.app.FragmentActivity#startActivityForResult(android.content.Intent,int,android.os.Bundle) FragmentActivity#startActivityForResult(Intent, int, Bundle)}.
 */

public void onStartActivityFromFragment(@androidx.annotation.NonNull androidx.fragment.app.Fragment fragment, android.content.Intent intent, int requestCode, @androidx.annotation.Nullable android.os.Bundle options) { throw new RuntimeException("Stub!"); }

/**
 * Starts a new {@link android.content.IntentSender IntentSender} from the given fragment.
 * See {@link android.app.Activity#startIntentSender(android.content.IntentSender,android.content.Intent,int,int,int,android.os.Bundle) Activity#startIntentSender(IntentSender, Intent, int, int, int, Bundle)}.
 *
 * @deprecated Have your FragmentHostCallback implement {@link androidx.activity.result.ActivityResultRegistryOwner ActivityResultRegistryOwner}
 * to allow Fragments to use
 * {@link androidx.fragment.app.Fragment#registerForActivityResult(androidx.activity.result.contract.ActivityResultContract,androidx.activity.result.ActivityResultCallback) Fragment#registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
 * with {@link androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult StartIntentSenderForResult}. This method will still be called when Fragments
 * call the deprecated <code>startIntentSenderForResult()</code> method.
 */

@Deprecated
public void onStartIntentSenderFromFragment(@androidx.annotation.NonNull androidx.fragment.app.Fragment fragment, android.content.IntentSender intent, int requestCode, @androidx.annotation.Nullable android.content.Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, @androidx.annotation.Nullable android.os.Bundle options) throws android.content.IntentSender.SendIntentException { throw new RuntimeException("Stub!"); }

/**
 * Requests permissions from the given fragment.
 * See {@link androidx.fragment.app.FragmentActivity#requestPermissions(java.lang.String[],int) FragmentActivity#requestPermissions(String[], int)}
 *
 * @deprecated Have your FragmentHostCallback implement {@link androidx.activity.result.ActivityResultRegistryOwner ActivityResultRegistryOwner}
 * to allow Fragments to use
 * {@link androidx.fragment.app.Fragment#registerForActivityResult(androidx.activity.result.contract.ActivityResultContract,androidx.activity.result.ActivityResultCallback) Fragment#registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
 * with {@link androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions RequestMultiplePermissions}. This method will still be called when Fragments
 * call the deprecated <code>requestPermissions()</code> method.
 */

@Deprecated
public void onRequestPermissionsFromFragment(@androidx.annotation.NonNull androidx.fragment.app.Fragment fragment, @androidx.annotation.NonNull java.lang.String[] permissions, int requestCode) { throw new RuntimeException("Stub!"); }

/**
 * Checks whether to show permission rationale UI from a fragment.
 * See {@link androidx.fragment.app.FragmentActivity#shouldShowRequestPermissionRationale(java.lang.String) FragmentActivity#shouldShowRequestPermissionRationale(String)}
 */

public boolean onShouldShowRequestPermissionRationale(@androidx.annotation.NonNull java.lang.String permission) { throw new RuntimeException("Stub!"); }

/**
 * Return {@code true} if there are window animations.
 */

public boolean onHasWindowAnimations() { throw new RuntimeException("Stub!"); }

/**
 * Return the window animations.
 */

public int onGetWindowAnimations() { throw new RuntimeException("Stub!"); }

@androidx.annotation.Nullable
public android.view.View onFindViewById(int id) { throw new RuntimeException("Stub!"); }

public boolean onHasView() { throw new RuntimeException("Stub!"); }
}

