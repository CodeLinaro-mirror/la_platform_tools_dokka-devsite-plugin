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

import androidx.loader.app.LoaderManager;
import android.os.Parcelable;
import androidx.lifecycle.ViewModelStoreOwner;
import android.content.Context;
import android.app.Activity;
import android.content.res.Configuration;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Provides integration points with a {@link androidx.fragment.app.FragmentManager FragmentManager} for a fragment host.
 * <p>
 * It is the responsibility of the host to take care of the Fragment's lifecycle.
 * The methods provided by {@link androidx.fragment.app.FragmentController FragmentController} are for that purpose.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class FragmentController {

private FragmentController() { throw new RuntimeException("Stub!"); }

/**
 * Returns a {@link androidx.fragment.app.FragmentController FragmentController}.
 */

@androidx.annotation.NonNull
public static androidx.fragment.app.FragmentController createController(@androidx.annotation.NonNull androidx.fragment.app.FragmentHostCallback<?> callbacks) { throw new RuntimeException("Stub!"); }

/**
 * Returns a {@link androidx.fragment.app.FragmentManager FragmentManager} for this controller.
 */

@androidx.annotation.NonNull
public androidx.fragment.app.FragmentManager getSupportFragmentManager() { throw new RuntimeException("Stub!"); }

/**
 * Returns a {@link androidx.loader.app.LoaderManager LoaderManager}.
 *
 * @deprecated Loaders are managed separately from FragmentController and this now throws an
 * {@link java.lang.UnsupportedOperationException UnsupportedOperationException}. Use {@link androidx.loader.app.LoaderManager#getInstance LoaderManager#getInstance} to obtain a
 * LoaderManager.
 * @see androidx.loader.app.LoaderManager#getInstance
 */

@Deprecated
public androidx.loader.app.LoaderManager getSupportLoaderManager() { throw new RuntimeException("Stub!"); }

/**
 * Returns a fragment with the given identifier.
 */

@androidx.annotation.Nullable
public androidx.fragment.app.Fragment findFragmentByWho(@androidx.annotation.NonNull java.lang.String who) { throw new RuntimeException("Stub!"); }

/**
 * Returns the number of active fragments.
 */

public int getActiveFragmentsCount() { throw new RuntimeException("Stub!"); }

/**
 * Returns the list of active fragments.
 */

@androidx.annotation.NonNull
public java.util.List<androidx.fragment.app.Fragment> getActiveFragments(java.util.List<androidx.fragment.app.Fragment> actives) { throw new RuntimeException("Stub!"); }

/**
 * Attaches the host to the FragmentManager for this controller. The host must be
 * attached before the FragmentManager can be used to manage Fragments.
 */

public void attachHost(@androidx.annotation.Nullable androidx.fragment.app.Fragment parent) { throw new RuntimeException("Stub!"); }

/**
 * Instantiates a Fragment's view.
 *
 * @param parent The parent that the created view will be placed
 * in; <em>note that this may be null</em>.
 * @param name Tag name to be inflated.
 * @param context The context the view is being created in.
 * @param attrs Inflation attributes as specified in XML file.
 *
 * @return view the newly created view
 */

@androidx.annotation.Nullable
public android.view.View onCreateView(@androidx.annotation.Nullable android.view.View parent, @androidx.annotation.NonNull java.lang.String name, @androidx.annotation.NonNull android.content.Context context, @androidx.annotation.NonNull android.util.AttributeSet attrs) { throw new RuntimeException("Stub!"); }

/**
 * Marks the fragment state as unsaved. This allows for "state loss" detection.
 */

public void noteStateNotSaved() { throw new RuntimeException("Stub!"); }

/**
 * Saves the state for all Fragments.
 *
 * @see #restoreSaveState(Parcelable)
 */

@androidx.annotation.Nullable
public android.os.Parcelable saveAllState() { throw new RuntimeException("Stub!"); }

/**
 * Restores the saved state for all Fragments. The given Fragment list are Fragment
 * instances retained across configuration changes.
 *
 * @deprecated Have your {@link androidx.fragment.app.FragmentHostCallback FragmentHostCallback} implement {@link androidx.lifecycle.ViewModelStoreOwner ViewModelStoreOwner}
 * to automatically restore the Fragment's non configuration state and use
 * {@link #restoreSaveState(android.os.Parcelable)} to restore the Fragment's save state.
 */

@Deprecated
public void restoreAllState(@androidx.annotation.Nullable android.os.Parcelable state, @androidx.annotation.Nullable java.util.List<androidx.fragment.app.Fragment> nonConfigList) { throw new RuntimeException("Stub!"); }

/**
 * Restores the saved state for all Fragments. The given FragmentManagerNonConfig are Fragment
 * instances retained across configuration changes, including nested fragments
 *
 * @deprecated Have your {@link androidx.fragment.app.FragmentHostCallback FragmentHostCallback} implement {@link androidx.lifecycle.ViewModelStoreOwner ViewModelStoreOwner}
 * to automatically restore the Fragment's non configuration state and use
 * {@link #restoreSaveState(android.os.Parcelable)} to restore the Fragment's save state.
 */

@Deprecated
public void restoreAllState(@androidx.annotation.Nullable android.os.Parcelable state, @androidx.annotation.Nullable androidx.fragment.app.FragmentManagerNonConfig nonConfig) { throw new RuntimeException("Stub!"); }

/**
 * Restores the saved state for all Fragments.
 *
 * @param state the saved state containing the Parcelable returned by {@link #saveAllState()}
 * @see #saveAllState()
 */

public void restoreSaveState(@androidx.annotation.Nullable android.os.Parcelable state) { throw new RuntimeException("Stub!"); }

/**
 * Returns a list of Fragments that have opted to retain their instance across
 * configuration changes.
 *
 * @deprecated Have your {@link androidx.fragment.app.FragmentHostCallback FragmentHostCallback} implement {@link androidx.lifecycle.ViewModelStoreOwner ViewModelStoreOwner}
 * to automatically retain the Fragment's non configuration state.
 */

@Deprecated
@androidx.annotation.Nullable
public java.util.List<androidx.fragment.app.Fragment> retainNonConfig() { throw new RuntimeException("Stub!"); }

/**
 * Returns a nested tree of Fragments that have opted to retain their instance across
 * configuration changes.
 *
 * @deprecated Have your {@link androidx.fragment.app.FragmentHostCallback FragmentHostCallback} implement {@link androidx.lifecycle.ViewModelStoreOwner ViewModelStoreOwner}
 * to automatically retain the Fragment's non configuration state.
 */

@Deprecated
@androidx.annotation.Nullable
public androidx.fragment.app.FragmentManagerNonConfig retainNestedNonConfig() { throw new RuntimeException("Stub!"); }

/**
 * Moves all Fragments managed by the controller's FragmentManager
 * into the create state.
 * <p>Call when Fragments should be created.
 *
 * @see Fragment#onCreate(Bundle)
 */

public void dispatchCreate() { throw new RuntimeException("Stub!"); }

/**
 * Moves all Fragments managed by the controller's FragmentManager
 * into the activity created state.
 * <p>Call when Fragments should be informed their host has been created.
 *
 * @see Fragment#onActivityCreated(Bundle)
 */

public void dispatchActivityCreated() { throw new RuntimeException("Stub!"); }

/**
 * Moves all Fragments managed by the controller's FragmentManager
 * into the start state.
 * <p>Call when Fragments should be started.
 *
 * @see androidx.fragment.app.Fragment#onStart()
 */

public void dispatchStart() { throw new RuntimeException("Stub!"); }

/**
 * Moves all Fragments managed by the controller's FragmentManager
 * into the resume state.
 * <p>Call when Fragments should be resumed.
 *
 * @see androidx.fragment.app.Fragment#onResume()
 */

public void dispatchResume() { throw new RuntimeException("Stub!"); }

/**
 * Moves all Fragments managed by the controller's FragmentManager
 * into the pause state.
 * <p>Call when Fragments should be paused.
 *
 * @see androidx.fragment.app.Fragment#onPause()
 */

public void dispatchPause() { throw new RuntimeException("Stub!"); }

/**
 * Moves all Fragments managed by the controller's FragmentManager
 * into the stop state.
 * <p>Call when Fragments should be stopped.
 *
 * @see androidx.fragment.app.Fragment#onStop()
 */

public void dispatchStop() { throw new RuntimeException("Stub!"); }

/**
 * @deprecated This functionality has been rolled into {@link #dispatchStop()}.
 */

@Deprecated
public void dispatchReallyStop() { throw new RuntimeException("Stub!"); }

/**
 * Moves all Fragments managed by the controller's FragmentManager
 * into the destroy view state.
 * <p>Call when the Fragment's views should be destroyed.
 *
 * @see androidx.fragment.app.Fragment#onDestroyView()
 */

public void dispatchDestroyView() { throw new RuntimeException("Stub!"); }

/**
 * Moves Fragments managed by the controller's FragmentManager
 * into the destroy state.
 * <p>
 * If the {@link androidx.fragment.app.FragmentHostCallback} is an instance of {@link androidx.lifecycle.ViewModelStoreOwner ViewModelStoreOwner},
 * then retained Fragments and any other non configuration state such as any
 * {@link androidx.lifecycle.ViewModel} attached to Fragments will only be destroyed if
 * {@link androidx.lifecycle.ViewModelStore#clear()} is called prior to this method.
 * <p>
 * Otherwise, the FragmentManager will look to see if the
 * {@link androidx.fragment.app.FragmentHostCallback FragmentHostCallback} Context is an {@link android.app.Activity Activity}
 * and if {@link android.app.Activity#isChangingConfigurations() Activity#isChangingConfigurations()} returns true. In only that case
 * will non configuration state be retained.
 * <p>Call when Fragments should be destroyed.
 *
 * @see androidx.fragment.app.Fragment#onDestroy()
 */

public void dispatchDestroy() { throw new RuntimeException("Stub!"); }

/**
 * Lets all Fragments managed by the controller's FragmentManager know the multi-window mode of
 * the activity changed.
 * <p>Call when the multi-window mode of the activity changed.
 *
 * @see androidx.fragment.app.Fragment#onMultiWindowModeChanged
 */

public void dispatchMultiWindowModeChanged(boolean isInMultiWindowMode) { throw new RuntimeException("Stub!"); }

/**
 * Lets all Fragments managed by the controller's FragmentManager know the picture-in-picture
 * mode of the activity changed.
 * <p>Call when the picture-in-picture mode of the activity changed.
 *
 * @see androidx.fragment.app.Fragment#onPictureInPictureModeChanged
 */

public void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode) { throw new RuntimeException("Stub!"); }

/**
 * Lets all Fragments managed by the controller's FragmentManager
 * know a configuration change occurred.
 * <p>Call when there is a configuration change.
 *
 * @see androidx.fragment.app.Fragment#onConfigurationChanged(Configuration)
 */

public void dispatchConfigurationChanged(@androidx.annotation.NonNull android.content.res.Configuration newConfig) { throw new RuntimeException("Stub!"); }

/**
 * Lets all Fragments managed by the controller's FragmentManager
 * know the device is in a low memory condition.
 * <p>Call when the device is low on memory and Fragment's should trim
 * their memory usage.
 *
 * @see androidx.fragment.app.Fragment#onLowMemory()
 */

public void dispatchLowMemory() { throw new RuntimeException("Stub!"); }

/**
 * Lets all Fragments managed by the controller's FragmentManager
 * know they should create an options menu.
 * <p>Call when the Fragment should create an options menu.
 *
 * @return {@code true} if the options menu contains items to display
 * @see androidx.fragment.app.Fragment#onCreateOptionsMenu(Menu, MenuInflater)
 */

public boolean dispatchCreateOptionsMenu(@androidx.annotation.NonNull android.view.Menu menu, @androidx.annotation.NonNull android.view.MenuInflater inflater) { throw new RuntimeException("Stub!"); }

/**
 * Lets all Fragments managed by the controller's FragmentManager
 * know they should prepare their options menu for display.
 * <p>Call immediately before displaying the Fragment's options menu.
 *
 * @return {@code true} if the options menu contains items to display
 * @see androidx.fragment.app.Fragment#onPrepareOptionsMenu(Menu)
 */

public boolean dispatchPrepareOptionsMenu(@androidx.annotation.NonNull android.view.Menu menu) { throw new RuntimeException("Stub!"); }

/**
 * Sends an option item selection event to the Fragments managed by the
 * controller's FragmentManager. Once the event has been consumed,
 * no additional handling will be performed.
 * <p>Call immediately after an options menu item has been selected
 *
 * @return {@code true} if the options menu selection event was consumed
 * @see androidx.fragment.app.Fragment#onOptionsItemSelected(MenuItem)
 */

public boolean dispatchOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) { throw new RuntimeException("Stub!"); }

/**
 * Sends a context item selection event to the Fragments managed by the
 * controller's FragmentManager. Once the event has been consumed,
 * no additional handling will be performed.
 * <p>Call immediately after an options menu item has been selected
 *
 * @return {@code true} if the context menu selection event was consumed
 * @see androidx.fragment.app.Fragment#onContextItemSelected(MenuItem)
 */

public boolean dispatchContextItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) { throw new RuntimeException("Stub!"); }

/**
 * Lets all Fragments managed by the controller's FragmentManager
 * know their options menu has closed.
 * <p>Call immediately after closing the Fragment's options menu.
 *
 * @see androidx.fragment.app.Fragment#onOptionsMenuClosed(Menu)
 */

public void dispatchOptionsMenuClosed(@androidx.annotation.NonNull android.view.Menu menu) { throw new RuntimeException("Stub!"); }

/**
 * Execute any pending actions for the Fragments managed by the
 * controller's FragmentManager.
 * <p>Call when queued actions can be performed [eg when the
 * Fragment moves into a start or resume state].
 * @return {@code true} if queued actions were performed
 */

public boolean execPendingActions() { throw new RuntimeException("Stub!"); }

/**
 * Starts the loaders.
 *
 * @deprecated Loaders are managed separately from FragmentController
 */

@Deprecated
public void doLoaderStart() { throw new RuntimeException("Stub!"); }

/**
 * Stops the loaders, optionally retaining their state. This is useful for keeping the
 * loader state across configuration changes.
 *
 * @param retain When {@code true}, the loaders aren't stopped, but, their instances
 * are retained in a started state
 *
 * @deprecated Loaders are managed separately from FragmentController
 */

@Deprecated
public void doLoaderStop(boolean retain) { throw new RuntimeException("Stub!"); }

/**
 * Retains the state of each of the loaders.
 *
 * @deprecated Loaders are managed separately from FragmentController
 */

@Deprecated
public void doLoaderRetain() { throw new RuntimeException("Stub!"); }

/**
 * Destroys the loaders and, if their state is not being retained, removes them.
 *
 * @deprecated Loaders are managed separately from FragmentController
 */

@Deprecated
public void doLoaderDestroy() { throw new RuntimeException("Stub!"); }

/**
 * Lets the loaders know the host is ready to receive notifications.
 *
 * @deprecated Loaders are managed separately from FragmentController
 */

@Deprecated
public void reportLoaderStart() { throw new RuntimeException("Stub!"); }

/**
 * Returns a list of LoaderManagers that have opted to retain their instance across
 * configuration changes.
 *
 * @deprecated Loaders are managed separately from FragmentController
 */

@Deprecated
@androidx.annotation.Nullable
public androidx.collection.SimpleArrayMap<java.lang.String,androidx.loader.app.LoaderManager> retainLoaderNonConfig() { throw new RuntimeException("Stub!"); }

/**
 * Restores the saved state for all LoaderManagers. The given LoaderManager list are
 * LoaderManager instances retained across configuration changes.
 *
 * @see #retainLoaderNonConfig()
 *
 * @deprecated Loaders are managed separately from FragmentController
 */

@Deprecated
public void restoreLoaderNonConfig(androidx.collection.SimpleArrayMap<java.lang.String,androidx.loader.app.LoaderManager> loaderManagers) { throw new RuntimeException("Stub!"); }

/**
 * Dumps the current state of the loaders.
 *
 * @deprecated Loaders are managed separately from FragmentController
 */

@Deprecated
public void dumpLoaders(@androidx.annotation.NonNull java.lang.String prefix, @androidx.annotation.Nullable java.io.FileDescriptor fd, @androidx.annotation.NonNull java.io.PrintWriter writer, @androidx.annotation.Nullable java.lang.String[] args) { throw new RuntimeException("Stub!"); }
}

