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
import android.view.ViewGroup;
import android.view.LayoutInflater;
import androidx.lifecycle.Lifecycle;
import android.view.View;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelStore;
import android.content.Intent;
import android.app.Activity;
import androidx.loader.app.LoaderManager;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.ActivityResultCallback;
import android.os.Handler;
import android.content.IntentSender;
import android.util.AttributeSet;
import android.animation.Animator;
import android.view.animation.Animation;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.view.ContextMenu;
import android.view.View.OnCreateContextMenuListener;
import android.view.ContextMenu.ContextMenuInfo;
import java.io.PrintWriter;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.ActivityResultRegistryOwner;

/**
 * Static library support version of the framework's {@link android.app.Fragment}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation. See the framework {@link android.app.Fragment}
 * documentation for a class overview.
 *
 * <p>The main differences when using this support version instead of the framework version are:
 * <ul>
 *  <li>Your activity must extend {@link androidx.fragment.app.FragmentActivity FragmentActivity}
 *  <li>You must call {@link androidx.fragment.app.FragmentActivity#getSupportFragmentManager FragmentActivity#getSupportFragmentManager} to get the
 *  {@link androidx.fragment.app.FragmentManager FragmentManager}
 * </ul>
 *
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Fragment implements android.content.ComponentCallbacks, android.view.View.OnCreateContextMenuListener, androidx.lifecycle.LifecycleOwner, androidx.lifecycle.ViewModelStoreOwner, androidx.lifecycle.HasDefaultViewModelProviderFactory, androidx.savedstate.SavedStateRegistryOwner, androidx.activity.result.ActivityResultCaller {

/**
 * Constructor used by the default {@link androidx.fragment.app.FragmentFactory FragmentFactory}. You must
 * {@link androidx.fragment.app.FragmentManager#setFragmentFactory(androidx.fragment.app.FragmentFactory) set a custom FragmentFactory}
 * if you want to use a non-default constructor to ensure that your constructor
 * is called when the fragment is re-instantiated.
 *
 * <p>It is strongly recommended to supply arguments with {@link #setArguments}
 * and later retrieved by the Fragment with {@link #getArguments}. These arguments
 * are automatically saved and restored alongside the Fragment.
 *
 * <p>Applications should generally not implement a constructor. Prefer
 * {@link #onAttach(android.content.Context)} instead. It is the first place application code can run where
 * the fragment is ready to be used - the point where the fragment is actually associated with
 * its context. Some applications may also want to implement {@link #onInflate} to retrieve
 * attributes from a layout resource, although note this happens when the fragment is attached.
 */

public Fragment() { throw new RuntimeException("Stub!"); }

/**
 * Alternate constructor that can be called from your default, no argument constructor to
 * provide a default layout that will be inflated by
 * {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)}.
 *
 * <pre class="prettyprint">
 * class MyFragment extends Fragment {
 *   public MyFragment() {
 *     super(R.layout.fragment_main);
 *   }
 * }
 * </pre>
 *
 * You must
 * {@link androidx.fragment.app.FragmentManager#setFragmentFactory(androidx.fragment.app.FragmentFactory) set a custom FragmentFactory}
 * if you want to use a non-default constructor to ensure that your constructor is called
 * when the fragment is re-instantiated.
 *
 * @see #Fragment()
 * @see #onCreateView(LayoutInflater, ViewGroup, Bundle)
 */

public Fragment(int contentLayoutId) { throw new RuntimeException("Stub!"); }

/**
 * {@inheritDoc}
 * <p>
 * Overriding this method is no longer supported and this method will be made
 * <code>final</code> in a future version of Fragment.
 */

@androidx.annotation.NonNull
public androidx.lifecycle.Lifecycle getLifecycle() { throw new RuntimeException("Stub!"); }

/**
 * Get a {@link androidx.lifecycle.LifecycleOwner LifecycleOwner} that represents the {@link #getView() Fragment's View}
 * lifecycle. In most cases, this mirrors the lifecycle of the Fragment itself, but in cases
 * of {@link androidx.fragment.app.FragmentTransaction#detach(androidx.fragment.app.Fragment) detached} Fragments, the lifecycle of the
 * Fragment can be considerably longer than the lifecycle of the View itself.
 * <p>
 * Namely, the lifecycle of the Fragment's View is:
 * <ol>
 * <li>{@link androidx.lifecycle.Lifecycle.Event#ON_CREATE created} after {@link #onViewStateRestored(android.os.Bundle)}</li>
 * <li>{@link androidx.lifecycle.Lifecycle.Event#ON_START started} after {@link #onStart()}</li>
 * <li>{@link androidx.lifecycle.Lifecycle.Event#ON_RESUME resumed} after {@link #onResume()}</li>
 * <li>{@link androidx.lifecycle.Lifecycle.Event#ON_PAUSE paused} before {@link #onPause()}</li>
 * <li>{@link androidx.lifecycle.Lifecycle.Event#ON_STOP stopped} before {@link #onStop()}</li>
 * <li>{@link androidx.lifecycle.Lifecycle.Event#ON_DESTROY destroyed} before {@link #onDestroyView()}</li>
 * </ol>
 *
 * The first method where it is safe to access the view lifecycle is
 * {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)} under the condition that you must
 * return a non-null view (an IllegalStateException will be thrown if you access the view
 * lifecycle but don't return a non-null view).
 * <p>The view lifecycle remains valid through the call to {@link #onDestroyView()}, after which
 * {@link #getView()} will return null, the view lifecycle will be destroyed, and this method
 * will throw an IllegalStateException. Consider using
 * {@link #getViewLifecycleOwnerLiveData()} or {@link androidx.fragment.app.FragmentTransaction#runOnCommit(java.lang.Runnable) FragmentTransaction#runOnCommit(Runnable)}
 * to receive a callback for when the Fragment's view lifecycle is available.
 * <p>
 * This should only be called on the main thread.
 * <p>
 * Overriding this method is no longer supported and this method will be made
 * <code>final</code> in a future version of Fragment.
 *
 * @return A {@link androidx.lifecycle.LifecycleOwner LifecycleOwner} that represents the {@link #getView() Fragment's View}
 * lifecycle.
 * @throws java.lang.IllegalStateException if the {@link #getView() Fragment's View is null}.
 */

@androidx.annotation.NonNull
public androidx.lifecycle.LifecycleOwner getViewLifecycleOwner() { throw new RuntimeException("Stub!"); }

/**
 * Retrieve a {@link androidx.lifecycle.LiveData LiveData} which allows you to observe the
 * {@link #getViewLifecycleOwner() lifecycle of the Fragment's View}.
 * <p>
 * This will be set to the new {@link androidx.lifecycle.LifecycleOwner LifecycleOwner} after {@link #onCreateView} returns a
 * non-null View and will set to null after {@link #onDestroyView()}.
 * <p>
 * Overriding this method is no longer supported and this method will be made
 * <code>final</code> in a future version of Fragment.
 *
 * @return A LiveData that changes in sync with {@link #getViewLifecycleOwner()}.
 */

@androidx.annotation.NonNull
public androidx.lifecycle.LiveData<androidx.lifecycle.LifecycleOwner> getViewLifecycleOwnerLiveData() { throw new RuntimeException("Stub!"); }

/**
 * Returns the {@link androidx.lifecycle.ViewModelStore ViewModelStore} associated with this Fragment
 * <p>
 * Overriding this method is no longer supported and this method will be made
 * <code>final</code> in a future version of Fragment.
 *
 * @return a {@code ViewModelStore}
 * @throws java.lang.IllegalStateException if called before the Fragment is attached i.e., before
 * onAttach().
 */

@androidx.annotation.NonNull
public androidx.lifecycle.ViewModelStore getViewModelStore() { throw new RuntimeException("Stub!"); }

/**
 * {@inheritDoc}
 *
 * <p>The {@link #getArguments() Fragment's arguments} when this is first called will be used
 * as the defaults to any {@link androidx.lifecycle.SavedStateHandle} passed to a view model
 * created using this factory.</p>
 */

@androidx.annotation.NonNull
public androidx.lifecycle.ViewModelProvider.Factory getDefaultViewModelProviderFactory() { throw new RuntimeException("Stub!"); }

@androidx.annotation.NonNull
public final androidx.savedstate.SavedStateRegistry getSavedStateRegistry() { throw new RuntimeException("Stub!"); }

/**
 * Like {@link #instantiate(android.content.Context,java.lang.String,android.os.Bundle)} but with a null
 * argument Bundle.
 * @deprecated Use {@link androidx.fragment.app.FragmentManager#getFragmentFactory() FragmentManager#getFragmentFactory()} and
 * {@link androidx.fragment.app.FragmentFactory#instantiate(java.lang.ClassLoader,java.lang.String) FragmentFactory#instantiate(ClassLoader, String)}
 */

@Deprecated
@androidx.annotation.NonNull
public static androidx.fragment.app.Fragment instantiate(@androidx.annotation.NonNull android.content.Context context, @androidx.annotation.NonNull java.lang.String fname) { throw new RuntimeException("Stub!"); }

/**
 * Create a new instance of a Fragment with the given class name.  This is
 * the same as calling its empty constructor, setting the {@link java.lang.ClassLoader ClassLoader} on the
 * supplied arguments, then calling {@link #setArguments(android.os.Bundle)}.
 *
 * @param context The calling context being used to instantiate the fragment.
 * This is currently just used to get its ClassLoader.
 * @param fname The class name of the fragment to instantiate.
 * @param args Bundle of arguments to supply to the fragment, which it
 * can retrieve with {@link #getArguments()}.  May be null.
 * @return Returns a new fragment instance.
 * @throws androidx.fragment.app.Fragment.InstantiationException If there is a failure in instantiating
 * the given fragment class.  This is a runtime exception; it is not
 * normally expected to happen.
 * @deprecated Use {@link androidx.fragment.app.FragmentManager#getFragmentFactory() FragmentManager#getFragmentFactory()} and
 * {@link androidx.fragment.app.FragmentFactory#instantiate(java.lang.ClassLoader,java.lang.String) FragmentFactory#instantiate(ClassLoader, String)}, manually calling
 * {@link #setArguments(android.os.Bundle)} on the returned Fragment.
 */

@Deprecated
@androidx.annotation.NonNull
public static androidx.fragment.app.Fragment instantiate(@androidx.annotation.NonNull android.content.Context context, @androidx.annotation.NonNull java.lang.String fname, @androidx.annotation.Nullable android.os.Bundle args) { throw new RuntimeException("Stub!"); }

/**
 * Subclasses can not override equals().
 */

public final boolean equals(@androidx.annotation.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

/**
 * Subclasses can not override hashCode().
 */

public final int hashCode() { throw new RuntimeException("Stub!"); }

@androidx.annotation.NonNull
public java.lang.String toString() { throw new RuntimeException("Stub!"); }

/**
 * Return the identifier this fragment is known by.  This is either
 * the android:id value supplied in a layout or the container view ID
 * supplied when adding the fragment.
 */

public final int getId() { throw new RuntimeException("Stub!"); }

/**
 * Get the tag name of the fragment, if specified.
 */

@androidx.annotation.Nullable
public final java.lang.String getTag() { throw new RuntimeException("Stub!"); }

/**
 * Supply the construction arguments for this fragment.
 * The arguments supplied here will be retained across fragment destroy and
 * creation.
 * <p>This method cannot be called if the fragment is added to a FragmentManager and
 * if {@link #isStateSaved()} would return true.</p>
 */

public void setArguments(@androidx.annotation.Nullable android.os.Bundle args) { throw new RuntimeException("Stub!"); }

/**
 * Return the arguments supplied when the fragment was instantiated,
 * if any.
 */

@androidx.annotation.Nullable
public final android.os.Bundle getArguments() { throw new RuntimeException("Stub!"); }

/**
 * Return the arguments supplied when the fragment was instantiated.
 *
 * @throws java.lang.IllegalStateException if no arguments were supplied to the Fragment.
 * @see #getArguments()
 */

@androidx.annotation.NonNull
public final android.os.Bundle requireArguments() { throw new RuntimeException("Stub!"); }

/**
 * Returns true if this fragment is added and its state has already been saved
 * by its host. Any operations that would change saved state should not be performed
 * if this method returns true, and some operations such as {@link #setArguments(android.os.Bundle)}
 * will fail.
 *
 * @return true if this fragment's state has already been saved by its host
 */

public final boolean isStateSaved() { throw new RuntimeException("Stub!"); }

/**
 * Set the initial saved state that this Fragment should restore itself
 * from when first being constructed, as returned by
 * {@link androidx.fragment.app.FragmentManager#saveFragmentInstanceState(androidx.fragment.app.Fragment)  FragmentManager.saveFragmentInstanceState}.
 *
 * @param state The state the fragment should be restored from.
 */

public void setInitialSavedState(@androidx.annotation.Nullable androidx.fragment.app.Fragment.SavedState state) { throw new RuntimeException("Stub!"); }

/**
 * Optional target for this fragment.  This may be used, for example,
 * if this fragment is being started by another, and when done wants to
 * give a result back to the first.  The target set here is retained
 * across instances via {@link androidx.fragment.app.FragmentManager#putFragment  FragmentManager.putFragment()}.
 *
 * @param fragment The fragment that is the target of this one.
 * @param requestCode Optional request code, for convenience if you
 * are going to call back with {@link #onActivityResult(int,int,android.content.Intent)}.
 *
 * @deprecated Instead of using a target fragment to pass results, the fragment requesting a
 * result should use
 * {@link androidx.fragment.app.FragmentManager#setFragmentResultListener(java.lang.String,androidx.lifecycle.LifecycleOwner,androidx.fragment.app.FragmentResultListener) FragmentManager#setFragmentResultListener(String, LifecycleOwner,
 * FragmentResultListener)} to register a {@link androidx.fragment.app.FragmentResultListener FragmentResultListener} with a {@code
 * requestKey} using its {@link #getParentFragmentManager() parent fragment manager}. The
 * fragment delivering a result should then call
 * {@link androidx.fragment.app.FragmentManager#setFragmentResult(java.lang.String,android.os.Bundle) FragmentManager#setFragmentResult(String, Bundle)} using the same {@code requestKey}.
 * Consider using {@link #setArguments} to pass the {@code requestKey} if you need to support
 * dynamic request keys.
 */

@Deprecated
public void setTargetFragment(@androidx.annotation.Nullable androidx.fragment.app.Fragment fragment, int requestCode) { throw new RuntimeException("Stub!"); }

/**
 * Return the target fragment set by {@link #setTargetFragment}.
 *
 * @deprecated Instead of using a target fragment to pass results, use
 * {@link androidx.fragment.app.FragmentManager#setFragmentResult(java.lang.String,android.os.Bundle) FragmentManager#setFragmentResult(String, Bundle)} to deliver results to
 * {@link androidx.fragment.app.FragmentResultListener FragmentResultListener} instances registered by other fragments via
 * {@link androidx.fragment.app.FragmentManager#setFragmentResultListener(java.lang.String,androidx.lifecycle.LifecycleOwner,androidx.fragment.app.FragmentResultListener) FragmentManager#setFragmentResultListener(String, LifecycleOwner,
 * FragmentResultListener)}.
 */

@Deprecated
@androidx.annotation.Nullable
public final androidx.fragment.app.Fragment getTargetFragment() { throw new RuntimeException("Stub!"); }

/**
 * Return the target request code set by {@link #setTargetFragment}.
 *
 * @deprecated When using the target fragment replacement of
 * {@link androidx.fragment.app.FragmentManager#setFragmentResultListener(java.lang.String,androidx.lifecycle.LifecycleOwner,androidx.fragment.app.FragmentResultListener) FragmentManager#setFragmentResultListener(String, LifecycleOwner,
 * FragmentResultListener)} and {@link androidx.fragment.app.FragmentManager#setFragmentResult(java.lang.String,android.os.Bundle) FragmentManager#setFragmentResult(String, Bundle)},
 * consider using {@link #setArguments} to pass a {@code requestKey} if you need to support
 * dynamic request keys.
 */

@Deprecated
public final int getTargetRequestCode() { throw new RuntimeException("Stub!"); }

/**
 * Return the {@link android.content.Context Context} this fragment is currently associated with.
 *
 * @see #requireContext()
 */

@androidx.annotation.Nullable
public android.content.Context getContext() { throw new RuntimeException("Stub!"); }

/**
 * Return the {@link android.content.Context Context} this fragment is currently associated with.
 *
 * @throws java.lang.IllegalStateException if not currently associated with a context.
 * @see #getContext()
 */

@androidx.annotation.NonNull
public final android.content.Context requireContext() { throw new RuntimeException("Stub!"); }

/**
 * Return the {@link androidx.fragment.app.FragmentActivity FragmentActivity} this fragment is currently associated with.
 * May return {@code null} if the fragment is associated with a {@link android.content.Context Context}
 * instead.
 *
 * @see #requireActivity()
 */

@androidx.annotation.Nullable
public final androidx.fragment.app.FragmentActivity getActivity() { throw new RuntimeException("Stub!"); }

/**
 * Return the {@link androidx.fragment.app.FragmentActivity FragmentActivity} this fragment is currently associated with.
 *
 * @throws java.lang.IllegalStateException if not currently associated with an activity or if associated
 * only with a context.
 * @see #getActivity()
 */

@androidx.annotation.NonNull
public final androidx.fragment.app.FragmentActivity requireActivity() { throw new RuntimeException("Stub!"); }

/**
 * Return the host object of this fragment. May return {@code null} if the fragment
 * isn't currently being hosted.
 *
 * @see #requireHost()
 */

@androidx.annotation.Nullable
public final java.lang.Object getHost() { throw new RuntimeException("Stub!"); }

/**
 * Return the host object of this fragment.
 *
 * @throws java.lang.IllegalStateException if not currently associated with a host.
 * @see #getHost()
 */

@androidx.annotation.NonNull
public final java.lang.Object requireHost() { throw new RuntimeException("Stub!"); }

/**
 * Return <code>requireActivity().getResources()</code>.
 */

@androidx.annotation.NonNull
public final android.content.res.Resources getResources() { throw new RuntimeException("Stub!"); }

/**
 * Return a localized, styled CharSequence from the application's package's
 * default string table.
 *
 * @param resId Resource id for the CharSequence text
 */

@androidx.annotation.NonNull
public final java.lang.CharSequence getText(int resId) { throw new RuntimeException("Stub!"); }

/**
 * Return a localized string from the application's package's
 * default string table.
 *
 * @param resId Resource id for the string
 */

@androidx.annotation.NonNull
public final java.lang.String getString(int resId) { throw new RuntimeException("Stub!"); }

/**
 * Return a localized formatted string from the application's package's
 * default string table, substituting the format arguments as defined in
 * {@link java.util.Formatter} and {@link java.lang.String#format}.
 *
 * @param resId Resource id for the format string
 * @param formatArgs The format arguments that will be used for substitution.
 */

@androidx.annotation.NonNull
public final java.lang.String getString(int resId, @androidx.annotation.Nullable java.lang.Object... formatArgs) { throw new RuntimeException("Stub!"); }

/**
 * Return the FragmentManager for interacting with fragments associated
 * with this fragment's activity.  Note that this will be non-null slightly
 * before {@link #getActivity()}, during the time from when the fragment is
 * placed in a {@link androidx.fragment.app.FragmentTransaction FragmentTransaction} until it is committed and
 * attached to its activity.
 *
 * <p>If this Fragment is a child of another Fragment, the FragmentManager
 * returned here will be the parent's {@link #getChildFragmentManager()}.
 *
 * @see #getParentFragmentManager()
 * @deprecated This has been removed in favor of <code>getParentFragmentManager()</code> which
 * throws an {@link java.lang.IllegalStateException IllegalStateException} if the FragmentManager is null. Check if
 * {@link #isAdded()} returns <code>false</code> to determine if the FragmentManager is
 * <code>null</code>.
 */

@Deprecated
@androidx.annotation.Nullable
public final androidx.fragment.app.FragmentManager getFragmentManager() { throw new RuntimeException("Stub!"); }

/**
 * Return the FragmentManager for interacting with fragments associated
 * with this fragment's activity.  Note that this will be available slightly
 * before {@link #getActivity()}, during the time from when the fragment is
 * placed in a {@link androidx.fragment.app.FragmentTransaction FragmentTransaction} until it is committed and
 * attached to its activity.
 *
 * <p>If this Fragment is a child of another Fragment, the FragmentManager
 * returned here will be the parent's {@link #getChildFragmentManager()}.
 *
 * @throws java.lang.IllegalStateException if not associated with a transaction or host.
 */

@androidx.annotation.NonNull
public final androidx.fragment.app.FragmentManager getParentFragmentManager() { throw new RuntimeException("Stub!"); }

/**
 * Return the FragmentManager for interacting with fragments associated
 * with this fragment's activity.  Note that this will be available slightly
 * before {@link #getActivity()}, during the time from when the fragment is
 * placed in a {@link androidx.fragment.app.FragmentTransaction FragmentTransaction} until it is committed and
 * attached to its activity.
 *
 * <p>If this Fragment is a child of another Fragment, the FragmentManager
 * returned here will be the parent's {@link #getChildFragmentManager()}.
 *
 * @throws java.lang.IllegalStateException if not associated with a transaction or host.
 * @see #getParentFragmentManager()
 * @deprecated This has been renamed to <code>getParentFragmentManager()</code> to make it
 * clear that you are accessing the FragmentManager that contains this Fragment and not the
 * FragmentManager associated with child Fragments.
 */

@Deprecated
@androidx.annotation.NonNull
public final androidx.fragment.app.FragmentManager requireFragmentManager() { throw new RuntimeException("Stub!"); }

/**
 * Return a private FragmentManager for placing and managing Fragments
 * inside of this Fragment.
 */

@androidx.annotation.NonNull
public final androidx.fragment.app.FragmentManager getChildFragmentManager() { throw new RuntimeException("Stub!"); }

/**
 * Returns the parent Fragment containing this Fragment.  If this Fragment
 * is attached directly to an Activity, returns null.
 */

@androidx.annotation.Nullable
public final androidx.fragment.app.Fragment getParentFragment() { throw new RuntimeException("Stub!"); }

/**
 * Returns the parent Fragment containing this Fragment.
 *
 * @throws java.lang.IllegalStateException if this Fragment is attached directly to an Activity or
 * other Fragment host.
 * @see #getParentFragment()
 */

@androidx.annotation.NonNull
public final androidx.fragment.app.Fragment requireParentFragment() { throw new RuntimeException("Stub!"); }

/**
 * Return true if the fragment is currently added to its activity.
 */

public final boolean isAdded() { throw new RuntimeException("Stub!"); }

/**
 * Return true if the fragment has been explicitly detached from the UI.
 * That is, {@link androidx.fragment.app.FragmentTransaction#detach(androidx.fragment.app.Fragment)  FragmentTransaction.detach(Fragment)} has been used on it.
 */

public final boolean isDetached() { throw new RuntimeException("Stub!"); }

/**
 * Return true if this fragment is currently being removed from its
 * activity.  This is  <em>not</em> whether its activity is finishing, but
 * rather whether it is in the process of being removed from its activity.
 */

public final boolean isRemoving() { throw new RuntimeException("Stub!"); }

/**
 * Return true if the layout is included as part of an activity view
 * hierarchy via the &lt;fragment&gt; tag.  This will always be true when
 * fragments are created through the &lt;fragment&gt; tag, <em>except</em>
 * in the case where an old fragment is restored from a previous state and
 * it does not appear in the layout of the current state.
 */

public final boolean isInLayout() { throw new RuntimeException("Stub!"); }

/**
 * Return true if the fragment is in the resumed state.  This is true
 * for the duration of {@link #onResume()} and {@link #onPause()} as well.
 */

public final boolean isResumed() { throw new RuntimeException("Stub!"); }

/**
 * Return true if the fragment is currently visible to the user.  This means
 * it: (1) has been added, (2) has its view attached to the window, and
 * (3) is not hidden.
 */

public final boolean isVisible() { throw new RuntimeException("Stub!"); }

/**
 * Return true if the fragment has been hidden.  By default fragments
 * are shown.  You can find out about changes to this state with
 * {@link #onHiddenChanged}.  Note that the hidden state is orthogonal
 * to other states -- that is, to be visible to the user, a fragment
 * must be both started and not hidden.
 */

public final boolean isHidden() { throw new RuntimeException("Stub!"); }

/**
 * Called when the hidden state (as returned by {@link #isHidden()} of
 * the fragment has changed.  Fragments start out not hidden; this will
 * be called whenever the fragment changes state from that.
 * @param hidden True if the fragment is now hidden, false otherwise.
 */

public void onHiddenChanged(boolean hidden) { throw new RuntimeException("Stub!"); }

/**
 * Control whether a fragment instance is retained across Activity
 * re-creation (such as from a configuration change). If set, the fragment
 * lifecycle will be slightly different when an activity is recreated:
 * <ul>
 * <li> {@link #onDestroy()} will not be called (but {@link #onDetach()} still
 * will be, because the fragment is being detached from its current activity).
 * <li> {@link #onCreate(android.os.Bundle)} will not be called since the fragment
 * is not being re-created.
 * <li> {@link #onAttach(android.app.Activity)} and {@link #onActivityCreated(android.os.Bundle)} <b>will</b>
 * still be called.
 * </ul>
 *
 * @param retain <code>true</code> to retain this fragment instance across configuration
 *               changes, <code>false</code> otherwise.
 *
 * @see #getRetainInstance()
 * @deprecated Instead of retaining the Fragment itself, use a non-retained Fragment and keep
 * retained state in a ViewModel attached to that Fragment. The ViewModel's constructor and
 * its onCleared() callback provide the signal for initial creation and final destruction of
 * the retained state.
 */

@Deprecated
public void setRetainInstance(boolean retain) { throw new RuntimeException("Stub!"); }

/**
 * Returns <code>true</code> if this fragment instance's state will be retained across
 * configuration changes, and <code>false</code> if it will not.
 *
 * @return whether or not this fragment instance will be retained.
 * @see #setRetainInstance(boolean)
 *
 * @deprecated Instead of retaining the Fragment itself, use a non-retained Fragment and keep
 * retained state in a ViewModel attached to that Fragment. The ViewModel's constructor and
 * its onCleared() callback provide the signal for initial creation and final destruction of
 * the retained state.
 */

@Deprecated
public final boolean getRetainInstance() { throw new RuntimeException("Stub!"); }

/**
 * Report that this fragment would like to participate in populating
 * the options menu by receiving a call to {@link #onCreateOptionsMenu}
 * and related methods.
 *
 * @param hasMenu If true, the fragment has menu items to contribute.
 */

public void setHasOptionsMenu(boolean hasMenu) { throw new RuntimeException("Stub!"); }

/**
 * Set a hint for whether this fragment's menu should be visible.  This
 * is useful if you know that a fragment has been placed in your view
 * hierarchy so that the user can not currently seen it, so any menu items
 * it has should also not be shown.
 *
 * @param menuVisible The default is true, meaning the fragment's menu will
 * be shown as usual.  If false, the user will not see the menu.
 */

public void setMenuVisibility(boolean menuVisible) { throw new RuntimeException("Stub!"); }

/**
 * Set a hint to the system about whether this fragment's UI is currently visible
 * to the user. This hint defaults to true and is persistent across fragment instance
 * state save and restore.
 *
 * <p>An app may set this to false to indicate that the fragment's UI is
 * scrolled out of visibility or is otherwise not directly visible to the user.
 * This may be used by the system to prioritize operations such as fragment lifecycle updates
 * or loader ordering behavior.</p>
 *
 * <p><strong>Note:</strong> This method may be called outside of the fragment lifecycle.
 * and thus has no ordering guarantees with regard to fragment lifecycle method calls.</p>
 *
 * @param isVisibleToUser true if this fragment's UI is currently visible to the user (default),
 *                        false if it is not.
 *
 * @deprecated If you are manually calling this method, use
 * {@link androidx.fragment.app.FragmentTransaction#setMaxLifecycle(androidx.fragment.app.Fragment,androidx.lifecycle.Lifecycle.State) FragmentTransaction#setMaxLifecycle(Fragment, Lifecycle.State)} instead. If
 * overriding this method, behavior implemented when passing in <code>true</code> should be
 * moved to {@link androidx.fragment.app.Fragment#onResume() Fragment#onResume()}, and behavior implemented when passing in
 * <code>false</code> should be moved to {@link androidx.fragment.app.Fragment#onPause() Fragment#onPause()}.
 */

@Deprecated
public void setUserVisibleHint(boolean isVisibleToUser) { throw new RuntimeException("Stub!"); }

/**
 * @return The current value of the user-visible hint on this fragment.
 * @see #setUserVisibleHint(boolean)
 *
 * @deprecated Use {@link androidx.fragment.app.FragmentTransaction#setMaxLifecycle(androidx.fragment.app.Fragment,androidx.lifecycle.Lifecycle.State) FragmentTransaction#setMaxLifecycle(Fragment, Lifecycle.State)}
 * instead.
 */

@Deprecated
public boolean getUserVisibleHint() { throw new RuntimeException("Stub!"); }

/**
 * Return the LoaderManager for this fragment.
 *
 * @deprecated Use
 * {@link androidx.loader.app.LoaderManager#getInstance(androidx.lifecycle.LifecycleOwner) LoaderManager.getInstance(this)}.
 */

@Deprecated
@androidx.annotation.NonNull
public androidx.loader.app.LoaderManager getLoaderManager() { throw new RuntimeException("Stub!"); }

/**
 * Call {@link android.app.Activity#startActivity(android.content.Intent) Activity#startActivity(Intent)} from the fragment's
 * containing Activity.
 */

public void startActivity(android.content.Intent intent) { throw new RuntimeException("Stub!"); }

/**
 * Call {@link android.app.Activity#startActivity(android.content.Intent,android.os.Bundle) Activity#startActivity(Intent, Bundle)} from the fragment's
 * containing Activity.
 */

public void startActivity(android.content.Intent intent, @androidx.annotation.Nullable android.os.Bundle options) { throw new RuntimeException("Stub!"); }

/**
 * Call {@link android.app.Activity#startActivityForResult(android.content.Intent,int) Activity#startActivityForResult(Intent, int)} from the fragment's
 * containing Activity.
 *
 * @param intent The intent to start.
 * @param requestCode The request code to be returned in
 * {@link androidx.fragment.app.Fragment#onActivityResult(int,int,android.content.Intent) Fragment#onActivityResult(int, int, Intent)} when the activity exits. Must be
 *                    between 0 and 65535 to be considered valid. If given requestCode is
 *                    greater than 65535, an IllegalArgumentException would be thrown.
 *
 * @deprecated use
 * {@link #registerForActivityResult(androidx.activity.result.contract.ActivityResultContract,androidx.activity.result.ActivityResultCallback)}
 * passing in a {@link androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult StartActivityForResult} object for the {@link androidx.activity.result.contract.ActivityResultContract ActivityResultContract}.
 */

@Deprecated
public void startActivityForResult(android.content.Intent intent, int requestCode) { throw new RuntimeException("Stub!"); }

/**
 * Call {@link android.app.Activity#startActivityForResult(android.content.Intent,int,android.os.Bundle) Activity#startActivityForResult(Intent, int, Bundle)} from the fragment's
 * containing Activity.
 *
 * @param intent The intent to start.
 * @param requestCode The request code to be returned in
 * {@link androidx.fragment.app.Fragment#onActivityResult(int,int,android.content.Intent) Fragment#onActivityResult(int, int, Intent)} when the activity exits. Must be
 *                    between 0 and 65535 to be considered valid. If given requestCode is
 *                    greater than 65535, an IllegalArgumentException would be thrown.
 * @param options Additional options for how the Activity should be started. See
 * {@link android.content.Context#startActivity(android.content.Intent,android.os.Bundle) Context#startActivity(Intent, Bundle)} for more details. This value may be null.
 *
 * @deprecated use
 * {@link #registerForActivityResult(androidx.activity.result.contract.ActivityResultContract,androidx.activity.result.ActivityResultCallback)}
 * passing in a {@link androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult StartActivityForResult} object for the {@link androidx.activity.result.contract.ActivityResultContract ActivityResultContract}.
 */

@Deprecated
public void startActivityForResult(android.content.Intent intent, int requestCode, @androidx.annotation.Nullable android.os.Bundle options) { throw new RuntimeException("Stub!"); }

/**
 * Call {@link android.app.Activity#startIntentSenderForResult(android.content.IntentSender,int,android.content.Intent,int,int,int,android.os.Bundle) Activity#startIntentSenderForResult(IntentSender, int, Intent, int, int, int,
 * Bundle)} from the fragment's containing Activity.
 *
 * @param intent The IntentSender to launch.
 * @param requestCode The request code to be returned in
 * {@link androidx.fragment.app.Fragment#onActivityResult(int,int,android.content.Intent) Fragment#onActivityResult(int, int, Intent)} when the activity exits. Must be
 *                    between 0 and 65535 to be considered valid. If given requestCode is
 *                    greater than 65535, an IllegalArgumentException would be thrown.
 * @param fillInIntent If non-null, this will be provided as the intent parameter to
 * {@link android.content.IntentSender#sendIntent(android.content.Context,int,android.content.Intent,android.content.IntentSender.OnFinished,android.os.Handler) IntentSender#sendIntent(Context, int, Intent, IntentSender.OnFinished, Handler)}.
 *                     This value may be null.
 * @param flagsMask Intent flags in the original IntentSender that you would like to change.
 * @param flagsValues Desired values for any bits set in <code>flagsMask</code>.
 * @param extraFlags Always set to 0.
 * @param options Additional options for how the Activity should be started. See
 * {@link android.content.Context#startActivity(android.content.Intent,android.os.Bundle) Context#startActivity(Intent, Bundle)} for more details. This value may be null.
 *
 * @deprecated use
 * {@link #registerForActivityResult(androidx.activity.result.contract.ActivityResultContract,androidx.activity.result.ActivityResultCallback)}
 * passing in a {@link androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult StartIntentSenderForResult} object for the
 * {@link androidx.activity.result.contract.ActivityResultContract ActivityResultContract}.
 */

@Deprecated
public void startIntentSenderForResult(android.content.IntentSender intent, int requestCode, @androidx.annotation.Nullable android.content.Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, @androidx.annotation.Nullable android.os.Bundle options) throws android.content.IntentSender.SendIntentException { throw new RuntimeException("Stub!"); }

/**
 * Receive the result from a previous call to
 * {@link #startActivityForResult(android.content.Intent,int)}.  This follows the
 * related Activity API as described there in
 * {@link android.app.Activity#onActivityResult(int,int,android.content.Intent) Activity#onActivityResult(int, int, Intent)}.
 *
 * @param requestCode The integer request code originally supplied to
 *                    startActivityForResult(), allowing you to identify who this
 *                    result came from.
 * @param resultCode The integer result code returned by the child activity
 *                   through its setResult().
 * @param data An Intent, which can return result data to the caller
 *               (various data can be attached to Intent "extras").
 *
 * @deprecated use
 * {@link #registerForActivityResult(androidx.activity.result.contract.ActivityResultContract,androidx.activity.result.ActivityResultCallback)}
 * with the appropriate {@link androidx.activity.result.contract.ActivityResultContract ActivityResultContract} and handling the result in the
 * {@link androidx.activity.result.ActivityResultCallback#onActivityResult(java.lang.Object) callback}.
 */

@Deprecated
public void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable android.content.Intent data) { throw new RuntimeException("Stub!"); }

/**
 * Requests permissions to be granted to this application. These permissions
 * must be requested in your manifest, they should not be granted to your app,
 * and they should have protection level {@link android.content.pm.PermissionInfo
 * #PROTECTION_DANGEROUS dangerous}, regardless whether they are declared by
 * the platform or a third-party app.
 * <p>
 * Normal permissions {@link android.content.pm.PermissionInfo#PROTECTION_NORMAL}
 * are granted at install time if requested in the manifest. Signature permissions
 * {@link android.content.pm.PermissionInfo#PROTECTION_SIGNATURE} are granted at
 * install time if requested in the manifest and the signature of your app matches
 * the signature of the app declaring the permissions.
 * </p>
 * <p>
 * Call {@link #shouldShowRequestPermissionRationale(java.lang.String)} before calling this API to
 * check if the system recommends to show a rationale dialog before asking for a permission.
 * </p>
 * <p>
 * If your app does not have the requested permissions the user will be presented
 * with UI for accepting them. After the user has accepted or rejected the
 * requested permissions you will receive a callback on {@link
 * #onRequestPermissionsResult(int,java.lang.String[],int[])} reporting whether the
 * permissions were granted or not.
 * </p>
 * <p>
 * Note that requesting a permission does not guarantee it will be granted and
 * your app should be able to run without having this permission.
 * </p>
 * <p>
 * This method may start an activity allowing the user to choose which permissions
 * to grant and which to reject. Hence, you should be prepared that your activity
 * may be paused and resumed. Further, granting some permissions may require
 * a restart of you application. In such a case, the system will recreate the
 * activity stack before delivering the result to {@link
 * #onRequestPermissionsResult(int,java.lang.String[],int[])}.
 * </p>
 * <p>
 * When checking whether you have a permission you should use {@link
 * android.content.Context#checkSelfPermission(String)}.
 * </p>
 * <p>
 * Calling this API for permissions already granted to your app would show UI
 * to the user to decided whether the app can still hold these permissions. This
 * can be useful if the way your app uses the data guarded by the permissions
 * changes significantly.
 * </p>
 *
 * @param permissions The requested permissions.
 * @param requestCode Application specific request code to match with a result reported to
 * {@link #onRequestPermissionsResult(int,java.lang.String[],int[])}. Must be between 0 and 65535 to
 *                    be considered valid. If given requestCode is greater than 65535, an
 *                    IllegalArgumentException would be thrown.
 *
 * @see #onRequestPermissionsResult(int, String[], int[])
 * @see android.content.Context#checkSelfPermission(String)
 * @deprecated use
 * {@link #registerForActivityResult(androidx.activity.result.contract.ActivityResultContract,androidx.activity.result.ActivityResultCallback)} passing
 * in a {@link androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions RequestMultiplePermissions} object for the {@link androidx.activity.result.contract.ActivityResultContract ActivityResultContract} and
 * handling the result in the {@link androidx.activity.result.ActivityResultCallback#onActivityResult(java.lang.Object) callback}.
 */

@Deprecated
public final void requestPermissions(@androidx.annotation.NonNull java.lang.String[] permissions, int requestCode) { throw new RuntimeException("Stub!"); }

/**
 * Callback for the result from requesting permissions. This method
 * is invoked for every call on {@link #requestPermissions(java.lang.String[],int)}.
 * <p>
 * <strong>Note:</strong> It is possible that the permissions request interaction
 * with the user is interrupted. In this case you will receive empty permissions
 * and results arrays which should be treated as a cancellation.
 * </p>
 *
 * @param requestCode The request code passed in {@link #requestPermissions(java.lang.String[],int)}.
 * @param permissions The requested permissions. Never null.
 * @param grantResults The grant results for the corresponding permissions
 *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
 *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
 *
 * @see #requestPermissions(String[], int)
 *
 * @deprecated use
 * {@link #registerForActivityResult(androidx.activity.result.contract.ActivityResultContract,androidx.activity.result.ActivityResultCallback)} passing
 * in a {@link androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions RequestMultiplePermissions} object for the {@link androidx.activity.result.contract.ActivityResultContract ActivityResultContract} and
 * handling the result in the {@link androidx.activity.result.ActivityResultCallback#onActivityResult(java.lang.Object) callback}.
 */

@Deprecated
public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull java.lang.String[] permissions, @androidx.annotation.NonNull int[] grantResults) { throw new RuntimeException("Stub!"); }

/**
 * Gets whether you should show UI with rationale before requesting a permission.
 *
 * @param permission A permission your app wants to request.
 * @return Whether you should show permission rationale UI.
 *
 * @see android.content.Context#checkSelfPermission(String)
 * @see #requestPermissions(String[], int)
 * @see #onRequestPermissionsResult(int, String[], int[])
 */

public boolean shouldShowRequestPermissionRationale(@androidx.annotation.NonNull java.lang.String permission) { throw new RuntimeException("Stub!"); }

/**
 * Returns the LayoutInflater used to inflate Views of this Fragment. The default
 * implementation will throw an exception if the Fragment is not attached.
 *
 * @param savedInstanceState If the fragment is being re-created from
 * a previous saved state, this is the state.
 * @return The LayoutInflater used to inflate Views of this Fragment.
 */

@androidx.annotation.NonNull
public android.view.LayoutInflater onGetLayoutInflater(@androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Returns the cached LayoutInflater used to inflate Views of this Fragment. If
 * {@link #onGetLayoutInflater(android.os.Bundle)} has not been called {@link #onGetLayoutInflater(android.os.Bundle)}
 * will be called with a {@code null} argument and that value will be cached.
 * <p>
 * The cached LayoutInflater will be replaced immediately prior to
 * {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)} and cleared immediately after
 * {@link #onDetach()}.
 *
 * @return The LayoutInflater used to inflate Views of this Fragment.
 */

@androidx.annotation.NonNull
public final android.view.LayoutInflater getLayoutInflater() { throw new RuntimeException("Stub!"); }

/**
 * Called when a fragment is being created as part of a view layout
 * inflation, typically from setting the content view of an activity.  This
 * may be called immediately after the fragment is created from a
 * {@link androidx.fragment.app.FragmentContainerView FragmentContainerView} in a layout file.  Note this is <em>before</em>
 * the fragment's {@link #onAttach(android.content.Context)} has been called; all you should
 * do here is parse the attributes and save them away.
 *
 * <p>This is called <em>the first time</em> the fragment is inflated. If it is
 * being inflated into a new instance with saved state, this method will not be
 * called a second time for the restored state fragment.</p>
 *
 * <p>Here is a typical implementation of a fragment that can take parameters
 * both through attributes supplied here as well from {@link #getArguments()}:</p>
 *
 * {@sample frameworks/support/samples/Support4Demos/src/main/java/com/example/android/supportv4/app/FragmentArgumentsSupport.java
 *      fragment}
 *
 * <p>Note that parsing the XML attributes uses a "styleable" resource.  The
 * declaration for the styleable used here is:</p>
 *
 * {@sample frameworks/support/samples/Support4Demos/src/main/res/values/attrs.xml fragment_arguments}
 *
 * <p>The fragment can then be declared within its activity's content layout
 * through a tag like this:</p>
 *
 * {@sample frameworks/support/samples/Support4Demos/src/main/res/layout/fragment_arguments_support.xml from_attributes}
 *
 * <p>This fragment can also be created dynamically from arguments given
 * at runtime in the arguments Bundle; here is an example of doing so at
 * creation of the containing activity:</p>
 *
 * {@sample frameworks/support/samples/Support4Demos/src/main/java/com/example/android/supportv4/app/FragmentArgumentsSupport.java
 *      create}
 *
 * @param context The Activity that is inflating this fragment.
 * @param attrs The attributes at the tag where the fragment is
 * being created.
 * @param savedInstanceState If the fragment is being re-created from
 * a previous saved state, this is the state.
 */

public void onInflate(@androidx.annotation.NonNull android.content.Context context, @androidx.annotation.NonNull android.util.AttributeSet attrs, @androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Called when a fragment is being created as part of a view layout
 * inflation, typically from setting the content view of an activity.
 *
 * @deprecated See {@link #onInflate(android.content.Context,android.util.AttributeSet,android.os.Bundle)}.
 */

@Deprecated
public void onInflate(@androidx.annotation.NonNull android.app.Activity activity, @androidx.annotation.NonNull android.util.AttributeSet attrs, @androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Called when a fragment is attached as a child of this fragment.
 *
 * <p>This is called after the attached fragment's <code>onAttach</code> and before
 * the attached fragment's <code>onCreate</code> if the fragment has not yet had a previous
 * call to <code>onCreate</code>.</p>
 *
 * @param childFragment child fragment being attached
 *
 * @deprecated The responsibility for listening for fragments being attached has been moved
 * to FragmentManager. You can add a listener to
 * {@link #getChildFragmentManager()} the child FragmentManager} by calling
 * {@link androidx.fragment.app.FragmentManager#addFragmentOnAttachListener(androidx.fragment.app.FragmentOnAttachListener) FragmentManager#addFragmentOnAttachListener(FragmentOnAttachListener)}
 *  in {@link #onAttach(android.content.Context)} to get callbacks when a child fragment is attached.
 */

@Deprecated
public void onAttachFragment(@androidx.annotation.NonNull androidx.fragment.app.Fragment childFragment) { throw new RuntimeException("Stub!"); }

/**
 * Called when a fragment is first attached to its context.
 * {@link #onCreate(android.os.Bundle)} will be called after this.
 */

public void onAttach(@androidx.annotation.NonNull android.content.Context context) { throw new RuntimeException("Stub!"); }

/**
 * Called when a fragment is first attached to its activity.
 * {@link #onCreate(android.os.Bundle)} will be called after this.
 *
 * @deprecated See {@link #onAttach(android.content.Context)}.
 */

@Deprecated
public void onAttach(@androidx.annotation.NonNull android.app.Activity activity) { throw new RuntimeException("Stub!"); }

/**
 * Called when a fragment loads an animation. Note that if
 * {@link androidx.fragment.app.FragmentTransaction#setCustomAnimations(int,int) FragmentTransaction#setCustomAnimations(int, int)} was called with
 * {@link android.animation.Animator Animator} resources instead of {@link android.view.animation.Animation Animation} resources, {@code nextAnim}
 * will be an animator resource.
 *
 * @param transit The value set in {@link androidx.fragment.app.FragmentTransaction#setTransition(int) FragmentTransaction#setTransition(int)} or 0 if not
 *                set.
 * @param enter {@code true} when the fragment is added/attached/shown or {@code false} when
 *              the fragment is removed/detached/hidden.
 * @param nextAnim The resource set in
 *                 {@link androidx.fragment.app.FragmentTransaction#setCustomAnimations(int,int) FragmentTransaction#setCustomAnimations(int, int)},
 *                 {@link androidx.fragment.app.FragmentTransaction#setCustomAnimations(int,int,int,int) FragmentTransaction#setCustomAnimations(int, int, int, int)}, or
 *                 0 if neither was called. The value will depend on the current operation.
 */

@androidx.annotation.Nullable
public android.view.animation.Animation onCreateAnimation(int transit, boolean enter, int nextAnim) { throw new RuntimeException("Stub!"); }

/**
 * Called when a fragment loads an animator. This will be called when
 * {@link #onCreateAnimation(int,boolean,int)} returns null. Note that if
 * {@link androidx.fragment.app.FragmentTransaction#setCustomAnimations(int,int) FragmentTransaction#setCustomAnimations(int, int)} was called with
 * {@link android.view.animation.Animation Animation} resources instead of {@link android.animation.Animator Animator} resources, {@code nextAnim}
 * will be an animation resource.
 *
 * @param transit The value set in {@link androidx.fragment.app.FragmentTransaction#setTransition(int) FragmentTransaction#setTransition(int)} or 0 if not
 *                set.
 * @param enter {@code true} when the fragment is added/attached/shown or {@code false} when
 *              the fragment is removed/detached/hidden.
 * @param nextAnim The resource set in
 *                 {@link androidx.fragment.app.FragmentTransaction#setCustomAnimations(int,int) FragmentTransaction#setCustomAnimations(int, int)},
 *                 {@link androidx.fragment.app.FragmentTransaction#setCustomAnimations(int,int,int,int) FragmentTransaction#setCustomAnimations(int, int, int, int)}, or
 *                 0 if neither was called. The value will depend on the current operation.
 */

@androidx.annotation.Nullable
public android.animation.Animator onCreateAnimator(int transit, boolean enter, int nextAnim) { throw new RuntimeException("Stub!"); }

/**
 * Called to do initial creation of a fragment.  This is called after
 * {@link #onAttach(android.app.Activity)} and before
 * {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)}.
 *
 * <p>Note that this can be called while the fragment's activity is
 * still in the process of being created.  As such, you can not rely
 * on things like the activity's content view hierarchy being initialized
 * at this point.  If you want to do work once the activity itself is
 * created, add a {@link androidx.lifecycle.LifecycleObserver} on the
 * activity's Lifecycle, removing it when it receives the
 * {@link androidx.lifecycle.Lifecycle.State#CREATED Lifecycle.State#CREATED} callback.
 *
 * <p>Any restored child fragments will be created before the base
 * <code>Fragment.onCreate</code> method returns.</p>
 *
 * @param savedInstanceState If the fragment is being re-created from
 * a previous saved state, this is the state.
 */

public void onCreate(@androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Called to have the fragment instantiate its user interface view.
 * This is optional, and non-graphical fragments can return null. This will be called between
 * {@link #onCreate(android.os.Bundle)} and {@link #onViewCreated(android.view.View,android.os.Bundle)}.
 * <p>A default View can be returned by calling {@link #Fragment(int)} in your
 * constructor. Otherwise, this method returns null.
 *
 * <p>It is recommended to <strong>only</strong> inflate the layout in this method and move
 * logic that operates on the returned View to {@link #onViewCreated(android.view.View,android.os.Bundle)}.
 *
 * <p>If you return a View from here, you will later be called in
 * {@link #onDestroyView} when the view is being released.
 *
 * @param inflater The LayoutInflater object that can be used to inflate
 * any views in the fragment,
 * @param container If non-null, this is the parent view that the fragment's
 * UI should be attached to.  The fragment should not add the view itself,
 * but this can be used to generate the LayoutParams of the view.
 * @param savedInstanceState If non-null, this fragment is being re-constructed
 * from a previous saved state as given here.
 *
 * @return Return the View for the fragment's UI, or null.
 */

@androidx.annotation.Nullable
public android.view.View onCreateView(@androidx.annotation.NonNull android.view.LayoutInflater inflater, @androidx.annotation.Nullable android.view.ViewGroup container, @androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Called immediately after {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)}
 * has returned, but before any saved state has been restored in to the view.
 * This gives subclasses a chance to initialize themselves once
 * they know their view hierarchy has been completely created.  The fragment's
 * view hierarchy is not however attached to its parent at this point.
 * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)}.
 * @param savedInstanceState If non-null, this fragment is being re-constructed
 * from a previous saved state as given here.
 */

public void onViewCreated(@androidx.annotation.NonNull android.view.View view, @androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Get the root view for the fragment's layout (the one returned by {@link #onCreateView}),
 * if provided.
 *
 * @return The fragment's root view, or null if it has no layout.
 */

@androidx.annotation.Nullable
public android.view.View getView() { throw new RuntimeException("Stub!"); }

/**
 * Get the root view for the fragment's layout (the one returned by {@link #onCreateView}).
 *
 * @throws java.lang.IllegalStateException if no view was returned by {@link #onCreateView}.
 * @see #getView()
 */

@androidx.annotation.NonNull
public final android.view.View requireView() { throw new RuntimeException("Stub!"); }

/**
 * Called when the fragment's activity has been created and this
 * fragment's view hierarchy instantiated.  It can be used to do final
 * initialization once these pieces are in place, such as retrieving
 * views or restoring state.  It is also useful for fragments that use
 * {@link #setRetainInstance(boolean)} to retain their instance,
 * as this callback tells the fragment when it is fully associated with
 * the new activity instance.  This is called after {@link #onCreateView}
 * and before {@link #onViewStateRestored(android.os.Bundle)}.
 *
 * @param savedInstanceState If the fragment is being re-created from
 * a previous saved state, this is the state.
 *
 * @deprecated use {@link #onViewCreated(android.view.View,android.os.Bundle)} for code touching
 * the Fragment's view and {@link #onCreate(android.os.Bundle)} for other initialization.
 * To get a callback specifically when a Fragment activity's
 * {@link android.app.Activity#onCreate(android.os.Bundle) Activity#onCreate(Bundle)} is called, register a
 * {@link androidx.lifecycle.LifecycleObserver} on the Activity's
 * {@link androidx.lifecycle.Lifecycle Lifecycle} in {@link #onAttach(android.content.Context)}, removing it when it receives the
 * {@link androidx.lifecycle.Lifecycle.State#CREATED Lifecycle.State#CREATED} callback.
 */

@Deprecated
public void onActivityCreated(@androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Called when all saved state has been restored into the view hierarchy
 * of the fragment.  This can be used to do initialization based on saved
 * state that you are letting the view hierarchy track itself, such as
 * whether check box widgets are currently checked.  This is called
 * after {@link #onViewCreated(android.view.View,android.os.Bundle)} and before {@link #onStart()}.
 *
 * @param savedInstanceState If the fragment is being re-created from
 * a previous saved state, this is the state.
 */

public void onViewStateRestored(@androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Called when the Fragment is visible to the user.  This is generally
 * tied to {@link android.app.Activity#onStart() Activity.onStart} of the containing
 * Activity's lifecycle.
 */

public void onStart() { throw new RuntimeException("Stub!"); }

/**
 * Called when the fragment is visible to the user and actively running.
 * This is generally
 * tied to {@link android.app.Activity#onResume() Activity.onResume} of the containing
 * Activity's lifecycle.
 */

public void onResume() { throw new RuntimeException("Stub!"); }

/**
 * Called to ask the fragment to save its current dynamic state, so it
 * can later be reconstructed in a new instance if its process is
 * restarted.  If a new instance of the fragment later needs to be
 * created, the data you place in the Bundle here will be available
 * in the Bundle given to {@link #onCreate(android.os.Bundle)},
 * {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)}, and
 * {@link #onViewCreated(android.view.View,android.os.Bundle)}.
 *
 * <p>This corresponds to {@link android.app.Activity#onSaveInstanceState(android.os.Bundle)  Activity.onSaveInstanceState(Bundle)} and most of the discussion there
 * applies here as well.  Note however: <em>this method may be called
 * at any time before {@link #onDestroy()}</em>.  There are many situations
 * where a fragment may be mostly torn down (such as when placed on the
 * back stack with no UI showing), but its state will not be saved until
 * its owning activity actually needs to save its state.
 *
 * @param outState Bundle in which to place your saved state.
 */

public void onSaveInstanceState(@androidx.annotation.NonNull android.os.Bundle outState) { throw new RuntimeException("Stub!"); }

/**
 * Called when the Fragment's activity changes from fullscreen mode to multi-window mode and
 * visa-versa. This is generally tied to {@link android.app.Activity#onMultiWindowModeChanged Activity#onMultiWindowModeChanged} of the
 * containing Activity.
 *
 * @param isInMultiWindowMode True if the activity is in multi-window mode.
 */

public void onMultiWindowModeChanged(boolean isInMultiWindowMode) { throw new RuntimeException("Stub!"); }

/**
 * Called by the system when the activity changes to and from picture-in-picture mode. This is
 * generally tied to {@link android.app.Activity#onPictureInPictureModeChanged Activity#onPictureInPictureModeChanged} of the containing Activity.
 *
 * @param isInPictureInPictureMode True if the activity is in picture-in-picture mode.
 */

public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) { throw new RuntimeException("Stub!"); }

public void onConfigurationChanged(@androidx.annotation.NonNull android.content.res.Configuration newConfig) { throw new RuntimeException("Stub!"); }

/**
 * Callback for when the primary navigation state of this Fragment has changed. This can be
 * the result of the {@link #getParentFragmentManager()}  containing FragmentManager} having its
 * primary navigation fragment changed via
 * {@link androidx.fragment.app.FragmentTransaction#setPrimaryNavigationFragment} or due to
 * the primary navigation fragment changing in a parent FragmentManager.
 *
 * @param isPrimaryNavigationFragment True if and only if this Fragment and any
 * {@link #getParentFragment() parent fragment} is set as the primary navigation fragment
 * via {@link androidx.fragment.app.FragmentTransaction#setPrimaryNavigationFragment}.
 */

public void onPrimaryNavigationFragmentChanged(boolean isPrimaryNavigationFragment) { throw new RuntimeException("Stub!"); }

/**
 * Called when the Fragment is no longer resumed.  This is generally
 * tied to {@link android.app.Activity#onPause() Activity.onPause} of the containing
 * Activity's lifecycle.
 */

public void onPause() { throw new RuntimeException("Stub!"); }

/**
 * Called when the Fragment is no longer started.  This is generally
 * tied to {@link android.app.Activity#onStop() Activity.onStop} of the containing
 * Activity's lifecycle.
 */

public void onStop() { throw new RuntimeException("Stub!"); }

public void onLowMemory() { throw new RuntimeException("Stub!"); }

/**
 * Called when the view previously created by {@link #onCreateView} has
 * been detached from the fragment.  The next time the fragment needs
 * to be displayed, a new view will be created.  This is called
 * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
 * <em>regardless</em> of whether {@link #onCreateView} returned a
 * non-null view.  Internally it is called after the view's state has
 * been saved but before it has been removed from its parent.
 */

public void onDestroyView() { throw new RuntimeException("Stub!"); }

/**
 * Called when the fragment is no longer in use.  This is called
 * after {@link #onStop()} and before {@link #onDetach()}.
 */

public void onDestroy() { throw new RuntimeException("Stub!"); }

/**
 * Called when the fragment is no longer attached to its activity.  This
 * is called after {@link #onDestroy()}.
 */

public void onDetach() { throw new RuntimeException("Stub!"); }

/**
 * Initialize the contents of the Fragment host's standard options menu.  You
 * should place your menu items in to <var>menu</var>.  For this method
 * to be called, you must have first called {@link #setHasOptionsMenu}.  See
 * {@link android.app.Activity#onCreateOptionsMenu(android.view.Menu) Activity.onCreateOptionsMenu}
 * for more information.
 *
 * @param menu The options menu in which you place your items.
 *
 * @see #setHasOptionsMenu
 * @see #onPrepareOptionsMenu
 * @see #onOptionsItemSelected
 */

public void onCreateOptionsMenu(@androidx.annotation.NonNull android.view.Menu menu, @androidx.annotation.NonNull android.view.MenuInflater inflater) { throw new RuntimeException("Stub!"); }

/**
 * Prepare the Fragment host's standard options menu to be displayed.  This is
 * called right before the menu is shown, every time it is shown.  You can
 * use this method to efficiently enable/disable items or otherwise
 * dynamically modify the contents.  See
 * {@link android.app.Activity#onPrepareOptionsMenu(android.view.Menu) Activity.onPrepareOptionsMenu}
 * for more information.
 *
 * @param menu The options menu as last shown or first initialized by
 *             onCreateOptionsMenu().
 *
 * @see #setHasOptionsMenu
 * @see #onCreateOptionsMenu
 */

public void onPrepareOptionsMenu(@androidx.annotation.NonNull android.view.Menu menu) { throw new RuntimeException("Stub!"); }

/**
 * Called when this fragment's option menu items are no longer being
 * included in the overall options menu.  Receiving this call means that
 * the menu needed to be rebuilt, but this fragment's items were not
 * included in the newly built menu (its {@link #onCreateOptionsMenu(android.view.Menu,android.view.MenuInflater)}
 * was not called).
 */

public void onDestroyOptionsMenu() { throw new RuntimeException("Stub!"); }

/**
 * This hook is called whenever an item in your options menu is selected.
 * The default implementation simply returns false to have the normal
 * processing happen (calling the item's Runnable or sending a message to
 * its Handler as appropriate).  You can use this method for any items
 * for which you would like to do processing without those other
 * facilities.
 *
 * <p>Derived classes should call through to the base class for it to
 * perform the default menu handling.
 *
 * @param item The menu item that was selected.
 *
 * @return boolean Return false to allow normal menu processing to
 *         proceed, true to consume it here.
 *
 * @see #onCreateOptionsMenu
 */

public boolean onOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) { throw new RuntimeException("Stub!"); }

/**
 * This hook is called whenever the options menu is being closed (either by the user canceling
 * the menu with the back/menu button, or when an item is selected).
 *
 * @param menu The options menu as last shown or first initialized by
 *             onCreateOptionsMenu().
 */

public void onOptionsMenuClosed(@androidx.annotation.NonNull android.view.Menu menu) { throw new RuntimeException("Stub!"); }

/**
 * Called when a context menu for the {@code view} is about to be shown.
 * Unlike {@link #onCreateOptionsMenu}, this will be called every
 * time the context menu is about to be shown and should be populated for
 * the view (or item inside the view for {@link android.widget.AdapterView AdapterView} subclasses,
 * this can be found in the {@code menuInfo})).
 * <p>
 * Use {@link #onContextItemSelected(android.view.MenuItem)} to know when an
 * item has been selected.
 * <p>
 * The default implementation calls up to
 * {@link android.app.Activity#onCreateContextMenu Activity.onCreateContextMenu}, though
 * you can not call this implementation if you don't want that behavior.
 * <p>
 * It is not safe to hold onto the context menu after this method returns.
 * {@inheritDoc}
 */

public void onCreateContextMenu(@androidx.annotation.NonNull android.view.ContextMenu menu, @androidx.annotation.NonNull android.view.View v, @androidx.annotation.Nullable android.view.ContextMenu.ContextMenuInfo menuInfo) { throw new RuntimeException("Stub!"); }

/**
 * Registers a context menu to be shown for the given view (multiple views
 * can show the context menu). This method will set the
 * {@link android.view.View.OnCreateContextMenuListener OnCreateContextMenuListener} on the view to this fragment, so
 * {@link #onCreateContextMenu(android.view.ContextMenu,android.view.View,android.view.ContextMenu.ContextMenuInfo)} will be
 * called when it is time to show the context menu.
 *
 * @see #unregisterForContextMenu(View)
 * @param view The view that should show a context menu.
 */

public void registerForContextMenu(@androidx.annotation.NonNull android.view.View view) { throw new RuntimeException("Stub!"); }

/**
 * Prevents a context menu to be shown for the given view. This method will
 * remove the {@link android.view.View.OnCreateContextMenuListener OnCreateContextMenuListener} on the view.
 *
 * @see #registerForContextMenu(View)
 * @param view The view that should stop showing a context menu.
 */

public void unregisterForContextMenu(@androidx.annotation.NonNull android.view.View view) { throw new RuntimeException("Stub!"); }

/**
 * This hook is called whenever an item in a context menu is selected. The
 * default implementation simply returns false to have the normal processing
 * happen (calling the item's Runnable or sending a message to its Handler
 * as appropriate). You can use this method for any items for which you
 * would like to do processing without those other facilities.
 * <p>
 * Use {@link android.view.MenuItem#getMenuInfo() MenuItem#getMenuInfo()} to get extra information set by the
 * View that added this menu item.
 * <p>
 * Derived classes should call through to the base class for it to perform
 * the default menu handling.
 *
 * @param item The context menu item that was selected.
 * @return boolean Return false to allow normal context menu processing to
 *         proceed, true to consume it here.
 */

public boolean onContextItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) { throw new RuntimeException("Stub!"); }

/**
 * When custom transitions are used with Fragments, the enter transition callback
 * is called when this Fragment is attached or detached when not popping the back stack.
 *
 * @param callback Used to manipulate the shared element transitions on this Fragment
 *                 when added not as a pop from the back stack.
 */

public void setEnterSharedElementCallback(@androidx.annotation.Nullable androidx.core.app.SharedElementCallback callback) { throw new RuntimeException("Stub!"); }

/**
 * When custom transitions are used with Fragments, the exit transition callback
 * is called when this Fragment is attached or detached when popping the back stack.
 *
 * @param callback Used to manipulate the shared element transitions on this Fragment
 *                 when added as a pop from the back stack.
 */

public void setExitSharedElementCallback(@androidx.annotation.Nullable androidx.core.app.SharedElementCallback callback) { throw new RuntimeException("Stub!"); }

/**
 * Sets the Transition that will be used to move Views into the initial scene. The entering
 * Views will be those that are regular Views or ViewGroups that have
 * {@link android.view.ViewGroup#isTransitionGroup ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
 * {@link android.transition.Visibility} as entering is governed by changing visibility from
 * {@link android.view.View#INVISIBLE View#INVISIBLE} to {@link android.view.View#VISIBLE View#VISIBLE}. If <code>transition</code> is null,
 * entering Views will remain unaffected.
 *
 * @param transition The Transition to use to move Views into the initial Scene.
 *         <code>transition</code> must be an
 *         {@link android.transition.Transition} or
 *         {@link androidx.transition.Transition}.
 */

public void setEnterTransition(@androidx.annotation.Nullable java.lang.Object transition) { throw new RuntimeException("Stub!"); }

/**
 * Returns the Transition that will be used to move Views into the initial scene. The entering
 * Views will be those that are regular Views or ViewGroups that have
 * {@link android.view.ViewGroup#isTransitionGroup ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
 * {@link android.transition.Visibility} as entering is governed by changing visibility from
 * {@link android.view.View#INVISIBLE View#INVISIBLE} to {@link android.view.View#VISIBLE View#VISIBLE}.
 *
 * @return the Transition to use to move Views into the initial Scene.
 */

@androidx.annotation.Nullable
public java.lang.Object getEnterTransition() { throw new RuntimeException("Stub!"); }

/**
 * Sets the Transition that will be used to move Views out of the scene when the Fragment is
 * preparing to be removed, hidden, or detached because of popping the back stack. The exiting
 * Views will be those that are regular Views or ViewGroups that have
 * {@link android.view.ViewGroup#isTransitionGroup ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
 * {@link android.transition.Visibility} as entering is governed by changing visibility from
 * {@link android.view.View#VISIBLE View#VISIBLE} to {@link android.view.View#INVISIBLE View#INVISIBLE}. If <code>transition</code> is null,
 * entering Views will remain unaffected. If nothing is set, the default will be to
 * use the same value as set in {@link #setEnterTransition(java.lang.Object)}.
 *
 * @param transition The Transition to use to move Views out of the Scene when the Fragment
 *         is preparing to close due to popping the back stack. <code>transition</code> must be
 *         an {@link android.transition.Transition} or
 *         {@link androidx.transition.Transition}.
 */

public void setReturnTransition(@androidx.annotation.Nullable java.lang.Object transition) { throw new RuntimeException("Stub!"); }

/**
 * Returns the Transition that will be used to move Views out of the scene when the Fragment is
 * preparing to be removed, hidden, or detached because of popping the back stack. The exiting
 * Views will be those that are regular Views or ViewGroups that have
 * {@link android.view.ViewGroup#isTransitionGroup ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
 * {@link android.transition.Visibility} as entering is governed by changing visibility from
 * {@link android.view.View#VISIBLE View#VISIBLE} to {@link android.view.View#INVISIBLE View#INVISIBLE}. If nothing is set, the default will be to use
 * the same transition as {@link #getEnterTransition()}.
 *
 * @return the Transition to use to move Views out of the Scene when the Fragment
 *         is preparing to close due to popping the back stack.
 */

@androidx.annotation.Nullable
public java.lang.Object getReturnTransition() { throw new RuntimeException("Stub!"); }

/**
 * Sets the Transition that will be used to move Views out of the scene when the
 * fragment is removed, hidden, or detached when not popping the back stack.
 * The exiting Views will be those that are regular Views or ViewGroups that
 * have {@link android.view.ViewGroup#isTransitionGroup ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
 * {@link android.transition.Visibility} as exiting is governed by changing visibility
 * from {@link android.view.View#VISIBLE View#VISIBLE} to {@link android.view.View#INVISIBLE View#INVISIBLE}. If transition is null, the views will
 * remain unaffected.
 *
 * @param transition The Transition to use to move Views out of the Scene when the Fragment
 *          is being closed not due to popping the back stack. <code>transition</code>
 *          must be an
 *          {@link android.transition.Transition} or
 *          {@link androidx.transition.Transition}.
 */

public void setExitTransition(@androidx.annotation.Nullable java.lang.Object transition) { throw new RuntimeException("Stub!"); }

/**
 * Returns the Transition that will be used to move Views out of the scene when the
 * fragment is removed, hidden, or detached when not popping the back stack.
 * The exiting Views will be those that are regular Views or ViewGroups that
 * have {@link android.view.ViewGroup#isTransitionGroup ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
 * {@link android.transition.Visibility} as exiting is governed by changing visibility
 * from {@link android.view.View#VISIBLE View#VISIBLE} to {@link android.view.View#INVISIBLE View#INVISIBLE}. If transition is null, the views will
 * remain unaffected.
 *
 * @return the Transition to use to move Views out of the Scene when the Fragment
 *         is being closed not due to popping the back stack.
 */

@androidx.annotation.Nullable
public java.lang.Object getExitTransition() { throw new RuntimeException("Stub!"); }

/**
 * Sets the Transition that will be used to move Views in to the scene when returning due
 * to popping a back stack. The entering Views will be those that are regular Views
 * or ViewGroups that have {@link android.view.ViewGroup#isTransitionGroup ViewGroup#isTransitionGroup} return true. Typical Transitions
 * will extend {@link android.transition.Visibility} as exiting is governed by changing
 * visibility from {@link android.view.View#VISIBLE View#VISIBLE} to {@link android.view.View#INVISIBLE View#INVISIBLE}. If transition is null,
 * the views will remain unaffected. If nothing is set, the default will be to use the same
 * transition as {@link #getExitTransition()}.
 *
 * @param transition The Transition to use to move Views into the scene when reentering from a
 *          previously-started Activity due to popping the back stack. <code>transition</code>
 *          must be an
 *          {@link android.transition.Transition} or
 *          {@link androidx.transition.Transition}.
 */

public void setReenterTransition(@androidx.annotation.Nullable java.lang.Object transition) { throw new RuntimeException("Stub!"); }

/**
 * Returns the Transition that will be used to move Views in to the scene when returning due
 * to popping a back stack. The entering Views will be those that are regular Views
 * or ViewGroups that have {@link android.view.ViewGroup#isTransitionGroup ViewGroup#isTransitionGroup} return true. Typical Transitions
 * will extend {@link android.transition.Visibility} as exiting is governed by changing
 * visibility from {@link android.view.View#VISIBLE View#VISIBLE} to {@link android.view.View#INVISIBLE View#INVISIBLE}. If nothing is set, the
 * default will be to use the same transition as {@link #getExitTransition()}.
 *
 * @return the Transition to use to move Views into the scene when reentering from a
 *                   previously-started Activity due to popping the back stack.
 */

@androidx.annotation.Nullable
public java.lang.Object getReenterTransition() { throw new RuntimeException("Stub!"); }

/**
 * Sets the Transition that will be used for shared elements transferred into the content
 * Scene. Typical Transitions will affect size and location, such as
 * {@link android.transition.ChangeBounds}. A null
 * value will cause transferred shared elements to blink to the final position.
 *
 * @param transition The Transition to use for shared elements transferred into the content
 *          Scene.  <code>transition</code> must be an
 *          {@link android.transition.Transition android.transition.Transition} or
 *          {@link androidx.transition.Transition androidx.transition.Transition}.
 */

public void setSharedElementEnterTransition(@androidx.annotation.Nullable java.lang.Object transition) { throw new RuntimeException("Stub!"); }

/**
 * Returns the Transition that will be used for shared elements transferred into the content
 * Scene. Typical Transitions will affect size and location, such as
 * {@link android.transition.ChangeBounds}. A null
 * value will cause transferred shared elements to blink to the final position.
 *
 * @return The Transition to use for shared elements transferred into the content
 *                   Scene.
 */

@androidx.annotation.Nullable
public java.lang.Object getSharedElementEnterTransition() { throw new RuntimeException("Stub!"); }

/**
 * Sets the Transition that will be used for shared elements transferred back during a
 * pop of the back stack. This Transition acts in the leaving Fragment.
 * Typical Transitions will affect size and location, such as
 * {@link android.transition.ChangeBounds}. A null
 * value will cause transferred shared elements to blink to the final position.
 * If no value is set, the default will be to use the same value as
 * {@link #setSharedElementEnterTransition(java.lang.Object)}.
 *
 * @param transition The Transition to use for shared elements transferred out of the content
 *          Scene. <code>transition</code> must be an
 *          {@link android.transition.Transition android.transition.Transition} or
 *          {@link androidx.transition.Transition androidx.transition.Transition}.
 */

public void setSharedElementReturnTransition(@androidx.annotation.Nullable java.lang.Object transition) { throw new RuntimeException("Stub!"); }

/**
 * Return the Transition that will be used for shared elements transferred back during a
 * pop of the back stack. This Transition acts in the leaving Fragment.
 * Typical Transitions will affect size and location, such as
 * {@link android.transition.ChangeBounds}. A null
 * value will cause transferred shared elements to blink to the final position.
 * If no value is set, the default will be to use the same value as
 * {@link #setSharedElementEnterTransition(java.lang.Object)}.
 *
 * @return The Transition to use for shared elements transferred out of the content
 *                   Scene.
 */

@androidx.annotation.Nullable
public java.lang.Object getSharedElementReturnTransition() { throw new RuntimeException("Stub!"); }

/**
 * Sets whether the the exit transition and enter transition overlap or not.
 * When true, the enter transition will start as soon as possible. When false, the
 * enter transition will wait until the exit transition completes before starting.
 *
 * @param allow true to start the enter transition when possible or false to
 *              wait until the exiting transition completes.
 */

public void setAllowEnterTransitionOverlap(boolean allow) { throw new RuntimeException("Stub!"); }

/**
 * Returns whether the the exit transition and enter transition overlap or not.
 * When true, the enter transition will start as soon as possible. When false, the
 * enter transition will wait until the exit transition completes before starting.
 *
 * @return true when the enter transition should start as soon as possible or false to
 * when it should wait until the exiting transition completes.
 */

public boolean getAllowEnterTransitionOverlap() { throw new RuntimeException("Stub!"); }

/**
 * Sets whether the the return transition and reenter transition overlap or not.
 * When true, the reenter transition will start as soon as possible. When false, the
 * reenter transition will wait until the return transition completes before starting.
 *
 * @param allow true to start the reenter transition when possible or false to wait until the
 *              return transition completes.
 */

public void setAllowReturnTransitionOverlap(boolean allow) { throw new RuntimeException("Stub!"); }

/**
 * Returns whether the the return transition and reenter transition overlap or not.
 * When true, the reenter transition will start as soon as possible. When false, the
 * reenter transition will wait until the return transition completes before starting.
 *
 * @return true to start the reenter transition when possible or false to wait until the
 *         return transition completes.
 */

public boolean getAllowReturnTransitionOverlap() { throw new RuntimeException("Stub!"); }

/**
 * Postpone the entering Fragment transition until {@link #startPostponedEnterTransition()}
 * or {@link androidx.fragment.app.FragmentManager#executePendingTransactions() FragmentManager#executePendingTransactions()} has been called.
 * <p>
 * This method gives the Fragment the ability to delay Fragment animations
 * until all data is loaded. Until then, the added, shown, and
 * attached Fragments will be INVISIBLE and removed, hidden, and detached Fragments won't
 * be have their Views removed. The transaction runs when all postponed added Fragments in the
 * transaction have called {@link #startPostponedEnterTransition()}.
 * <p>
 * This method should be called before being added to the FragmentTransaction or
 * in {@link #onCreate(android.os.Bundle)}, {@link #onAttach(android.content.Context)}, or
 * {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)}}.
 * {@link #startPostponedEnterTransition()} must be called to allow the Fragment to
 * start the transitions.
 * <p>
 * When a FragmentTransaction is started that may affect a postponed FragmentTransaction,
 * based on which containers are in their operations, the postponed FragmentTransaction
 * will have its start triggered. The early triggering may result in faulty or nonexistent
 * animations in the postponed transaction. FragmentTransactions that operate only on
 * independent containers will not interfere with each other's postponement.
 * <p>
 * Calling postponeEnterTransition on Fragments with a null View will not postpone the
 * transition. Likewise, postponement only works if
 * {@link androidx.fragment.app.FragmentTransaction#setReorderingAllowed(boolean) FragmentTransaction reordering} is
 * enabled if you have called {@link androidx.fragment.app.FragmentManager#enableNewStateManager(boolean) FragmentManager#enableNewStateManager(boolean)} with
 * <code>false</code>.
 *
 * @see android.app.Activity#postponeEnterTransition()
 * @see androidx.fragment.app.FragmentTransaction#setReorderingAllowed(boolean)
 */

public void postponeEnterTransition() { throw new RuntimeException("Stub!"); }

/**
 * Postpone the entering Fragment transition for a given amount of time and then call
 * {@link #startPostponedEnterTransition()}.
 * <p>
 * This method gives the Fragment the ability to delay Fragment animations for a given amount
 * of time. Until then, the added, shown, and attached Fragments will be INVISIBLE and removed,
 * hidden, and detached Fragments won't be have their Views removed. The transaction runs when
 * all postponed added Fragments in the transaction have called
 * {@link #startPostponedEnterTransition()}.
 * <p>
 * This method should be called before being added to the FragmentTransaction or
 * in {@link #onCreate(android.os.Bundle)}, {@link #onAttach(android.content.Context)}, or
 * {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)}}.
 * <p>
 * When a FragmentTransaction is started that may affect a postponed FragmentTransaction,
 * based on which containers are in their operations, the postponed FragmentTransaction
 * will have its start triggered. The early triggering may result in faulty or nonexistent
 * animations in the postponed transaction. FragmentTransactions that operate only on
 * independent containers will not interfere with each other's postponement.
 * <p>
 * Calling postponeEnterTransition on Fragments with a null View will not postpone the
 * transition. Likewise, postponement only works if
 * {@link androidx.fragment.app.FragmentTransaction#setReorderingAllowed(boolean) FragmentTransaction reordering} is
 * enabled if you have called {@link androidx.fragment.app.FragmentManager#enableNewStateManager(boolean) FragmentManager#enableNewStateManager(boolean)} with
 * <code>false</code>.
 *
 * @param duration The length of the delay in {@code timeUnit} units
 * @param timeUnit The units of time for {@code duration}
 * @see android.app.Activity#postponeEnterTransition()
 * @see androidx.fragment.app.FragmentTransaction#setReorderingAllowed(boolean)
 */

public final void postponeEnterTransition(long duration, @androidx.annotation.NonNull java.util.concurrent.TimeUnit timeUnit) { throw new RuntimeException("Stub!"); }

/**
 * Begin postponed transitions after {@link #postponeEnterTransition()} was called.
 * If postponeEnterTransition() was called, you must call startPostponedEnterTransition()
 * or {@link androidx.fragment.app.FragmentManager#executePendingTransactions() FragmentManager#executePendingTransactions()} to complete the FragmentTransaction.
 * If postponement was interrupted with {@link androidx.fragment.app.FragmentManager#executePendingTransactions() FragmentManager#executePendingTransactions()},
 * before {@code startPostponedEnterTransition()}, animations may not run or may execute
 * improperly.
 *
 * @see android.app.Activity#startPostponedEnterTransition()
 */

public void startPostponedEnterTransition() { throw new RuntimeException("Stub!"); }

/**
 * Print the Fragments's state into the given stream.
 *
 * @param prefix Text to print at the front of each line.
 * @param fd The raw file descriptor that the dump is being sent to.
 * @param writer The PrintWriter to which you should dump your state.  This will be
 * closed for you after you return.
 * @param args additional arguments to the dump request.
 */

public void dump(@androidx.annotation.NonNull java.lang.String prefix, @androidx.annotation.Nullable java.io.FileDescriptor fd, @androidx.annotation.NonNull java.io.PrintWriter writer, @androidx.annotation.Nullable java.lang.String[] args) { throw new RuntimeException("Stub!"); }

/**
 * {@inheritDoc}
 *
 * <p>
 * If the host of this fragment is an {@link androidx.activity.result.ActivityResultRegistryOwner ActivityResultRegistryOwner} the
 * {@link androidx.activity.result.ActivityResultRegistry ActivityResultRegistry} of the host will be used. Otherwise, this will use the
 * registry of the Fragment's Activity.
 */

@androidx.annotation.NonNull
public final <I, O> androidx.activity.result.ActivityResultLauncher<I> registerForActivityResult(@androidx.annotation.NonNull androidx.activity.result.contract.ActivityResultContract<I,O> contract, @androidx.annotation.NonNull androidx.activity.result.ActivityResultCallback<O> callback) { throw new RuntimeException("Stub!"); }

@androidx.annotation.NonNull
public final <I, O> androidx.activity.result.ActivityResultLauncher<I> registerForActivityResult(@androidx.annotation.NonNull androidx.activity.result.contract.ActivityResultContract<I,O> contract, @androidx.annotation.NonNull androidx.activity.result.ActivityResultRegistry registry, @androidx.annotation.NonNull androidx.activity.result.ActivityResultCallback<O> callback) { throw new RuntimeException("Stub!"); }
/**
 * Thrown by {@link androidx.fragment.app.FragmentFactory#instantiate(java.lang.ClassLoader,java.lang.String) FragmentFactory#instantiate(ClassLoader, String)} when
 * there is an instantiation failure.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static class InstantiationException extends java.lang.RuntimeException {

public InstantiationException(@androidx.annotation.NonNull java.lang.String msg, @androidx.annotation.Nullable java.lang.Exception cause) { throw new RuntimeException("Stub!"); }
}

/**
 * State information that has been retrieved from a fragment instance
 * through {@link androidx.fragment.app.FragmentManager#saveFragmentInstanceState(androidx.fragment.app.Fragment)  FragmentManager.saveFragmentInstanceState}.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static class SavedState implements android.os.Parcelable {

SavedState() { throw new RuntimeException("Stub!"); }

public int describeContents() { throw new RuntimeException("Stub!"); }

public void writeToParcel(@androidx.annotation.NonNull android.os.Parcel dest, int flags) { throw new RuntimeException("Stub!"); }

@androidx.annotation.NonNull public static final android.os.Parcelable.Creator<androidx.fragment.app.Fragment.SavedState> CREATOR;
static { CREATOR = null; }
}

}

