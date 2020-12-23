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

import android.os.Bundle;
import android.view.View;
import java.io.PrintWriter;
import android.content.Context;
import androidx.lifecycle.Lifecycle;
import android.view.ViewGroup;
import android.view.LayoutInflater;

/**
 * Static library support version of the framework's {@link android.app.FragmentManager}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework {@link androidx.fragment.app.FragmentManager FragmentManager}
 * documentation for a class overview.
 *
 * <p>Your activity must derive from {@link androidx.fragment.app.FragmentActivity FragmentActivity} to use this. From such an activity,
 * you can acquire the {@link androidx.fragment.app.FragmentManager FragmentManager} by calling
 * {@link androidx.fragment.app.FragmentActivity#getSupportFragmentManager FragmentActivity#getSupportFragmentManager}.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class FragmentManager implements androidx.fragment.app.FragmentResultOwner {

public FragmentManager() { throw new RuntimeException("Stub!"); }

/**
 * Control whether FragmentManager uses the new state manager that is responsible for:
 * <ul>
 *     <li>Moving Fragments through their lifecycle methods</li>
 *     <li>Running animations and transitions</li>
 *     <li>Handling postponed transactions</li>
 * </ul>
 *
 * This must only be changed <strong>before</strong> any fragment transactions are done
 * (i.e., in your <code>Application</code> class or prior to <code>super.onCreate()</code>
 * in every activity with the same value for all activities). Changing it after that point
 * is <strong>not</strong> supported and can result in fragments not moving to their
 * expected state.
 * <p>
 * This is <strong>enabled</strong> by default. Disabling it should only be used in
 * cases where you are debugging a potential regression and as part of
 * <a href="https://issuetracker.google.com/issues/new?component=460964">filing
 * an issue</a> to verify and fix the regression.
 *
 * @param enabled Whether the new state manager should be enabled.
 */

@androidx.fragment.app.FragmentStateManagerControl
public static void enableNewStateManager(boolean enabled) { throw new RuntimeException("Stub!"); }

/**
 * Control whether the framework's internal fragment manager debugging
 * logs are turned on.  If enabled, you will see output in logcat as
 * the framework performs fragment operations.
 * @deprecated FragmentManager now respects {@link android.util.Log#isLoggable(java.lang.String,int) Log#isLoggable(String, int)} for debug
 * logging, allowing you to use <code>adb shell setprop log.tag.FragmentManager VERBOSE</code>.
 * @see android.util.Log#isLoggable(String, int)
 */

@Deprecated
public static void enableDebugLogging(boolean enabled) { throw new RuntimeException("Stub!"); }

/**
 * Start a series of edit operations on the Fragments associated with
 * this FragmentManager.
 *
 * <p>Note: A fragment transaction can only be created/committed prior
 * to an activity saving its state.  If you try to commit a transaction
 * after {@link androidx.fragment.app.FragmentActivity#onSaveInstanceState FragmentActivity#onSaveInstanceState}
 * (and prior to a following {@link androidx.fragment.app.FragmentActivity#onStart FragmentActivity#onStart}
 * or {@link androidx.fragment.app.FragmentActivity#onResume FragmentActivity#onResume}, you will get an error.
 * This is because the framework takes care of saving your current fragments
 * in the state, and if changes are made after the state is saved then they
 * will be lost.</p>
 */

@androidx.annotation.NonNull
public androidx.fragment.app.FragmentTransaction beginTransaction() { throw new RuntimeException("Stub!"); }

/**
 * After a {@link androidx.fragment.app.FragmentTransaction FragmentTransaction} is committed with
 * {@link androidx.fragment.app.FragmentTransaction#commit FragmentTransaction#commit}, it
 * is scheduled to be executed asynchronously on the process's main thread.
 * If you want to immediately executing any such pending operations, you
 * can call this function (only from the main thread) to do so.  Note that
 * all callbacks and other related behavior will be done from within this
 * call, so be careful about where this is called from.
 *
 * <p>If you are committing a single transaction that does not modify the
 * fragment back stack, strongly consider using
 * {@link androidx.fragment.app.FragmentTransaction#commitNow() FragmentTransaction#commitNow()} instead. This can help avoid
 * unwanted side effects when other code in your app has pending committed
 * transactions that expect different timing.</p>
 * <p>
 * This also forces the start of any postponed Transactions where
 * {@link androidx.fragment.app.Fragment#postponeEnterTransition() Fragment#postponeEnterTransition()} has been called.
 *
 * @return Returns true if there were any pending transactions to be
 * executed.
 */

public boolean executePendingTransactions() { throw new RuntimeException("Stub!"); }

/**
 * Pop the top state off the back stack. This function is asynchronous -- it enqueues the
 * request to pop, but the action will not be performed until the application
 * returns to its event loop.
 */

public void popBackStack() { throw new RuntimeException("Stub!"); }

/**
 * Like {@link #popBackStack()}, but performs the operation immediately
 * inside of the call.  This is like calling {@link #executePendingTransactions()}
 * afterwards without forcing the start of postponed Transactions.
 * @return Returns true if there was something popped, else false.
 */

public boolean popBackStackImmediate() { throw new RuntimeException("Stub!"); }

/**
 * Pop the last fragment transition from the manager's fragment
 * back stack.
 * This function is asynchronous -- it enqueues the
 * request to pop, but the action will not be performed until the application
 * returns to its event loop.
 *
 * @param name If non-null, this is the name of a previous back state
 * to look for; if found, all states up to that state will be popped.  The
 * {@link #POP_BACK_STACK_INCLUSIVE} flag can be used to control whether
 * the named state itself is popped. If null, only the top state is popped.
 * @param flags Either 0 or {@link #POP_BACK_STACK_INCLUSIVE}.
 */

public void popBackStack(@androidx.annotation.Nullable java.lang.String name, int flags) { throw new RuntimeException("Stub!"); }

/**
 * Like {@link #popBackStack(java.lang.String,int)}, but performs the operation immediately
 * inside of the call.  This is like calling {@link #executePendingTransactions()}
 * afterwards without forcing the start of postponed Transactions.
 * @return Returns true if there was something popped, else false.
 */

public boolean popBackStackImmediate(@androidx.annotation.Nullable java.lang.String name, int flags) { throw new RuntimeException("Stub!"); }

/**
 * Pop all back stack states up to the one with the given identifier.
 * This function is asynchronous -- it enqueues the
 * request to pop, but the action will not be performed until the application
 * returns to its event loop.
 *
 * @param id Identifier of the stated to be popped. If no identifier exists,
 * false is returned.
 * The identifier is the number returned by
 * {@link androidx.fragment.app.FragmentTransaction#commit() FragmentTransaction#commit()}.  The
 * {@link #POP_BACK_STACK_INCLUSIVE} flag can be used to control whether
 * the named state itself is popped.
 * @param flags Either 0 or {@link #POP_BACK_STACK_INCLUSIVE}.
 */

public void popBackStack(int id, int flags) { throw new RuntimeException("Stub!"); }

/**
 * Like {@link #popBackStack(int,int)}, but performs the operation immediately
 * inside of the call.  This is like calling {@link #executePendingTransactions()}
 * afterwards without forcing the start of postponed Transactions.
 * @return Returns true if there was something popped, else false.
 */

public boolean popBackStackImmediate(int id, int flags) { throw new RuntimeException("Stub!"); }

/**
 * Return the number of entries currently in the back stack.
 */

public int getBackStackEntryCount() { throw new RuntimeException("Stub!"); }

/**
 * Return the BackStackEntry at index <var>index</var> in the back stack;
 * entries start index 0 being the bottom of the stack.
 */

@androidx.annotation.NonNull
public androidx.fragment.app.FragmentManager.BackStackEntry getBackStackEntryAt(int index) { throw new RuntimeException("Stub!"); }

/**
 * Add a new listener for changes to the fragment back stack.
 */

public void addOnBackStackChangedListener(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager.OnBackStackChangedListener listener) { throw new RuntimeException("Stub!"); }

/**
 * Remove a listener that was previously added with
 * {@link #addOnBackStackChangedListener(androidx.fragment.app.FragmentManager.OnBackStackChangedListener)}.
 */

public void removeOnBackStackChangedListener(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager.OnBackStackChangedListener listener) { throw new RuntimeException("Stub!"); }

public final void setFragmentResult(@androidx.annotation.NonNull java.lang.String requestKey, @androidx.annotation.NonNull android.os.Bundle result) { throw new RuntimeException("Stub!"); }

public final void clearFragmentResult(@androidx.annotation.NonNull java.lang.String requestKey) { throw new RuntimeException("Stub!"); }

public final void setFragmentResultListener(@androidx.annotation.NonNull java.lang.String requestKey, @androidx.annotation.NonNull androidx.lifecycle.LifecycleOwner lifecycleOwner, @androidx.annotation.NonNull androidx.fragment.app.FragmentResultListener listener) { throw new RuntimeException("Stub!"); }

public final void clearFragmentResultListener(@androidx.annotation.NonNull java.lang.String requestKey) { throw new RuntimeException("Stub!"); }

/**
 * Put a reference to a fragment in a Bundle.  This Bundle can be
 * persisted as saved state, and when later restoring
 * {@link #getFragment(android.os.Bundle,java.lang.String)} will return the current
 * instance of the same fragment.
 *
 * @param bundle The bundle in which to put the fragment reference.
 * @param key The name of the entry in the bundle.
 * @param fragment The Fragment whose reference is to be stored.
 */

public void putFragment(@androidx.annotation.NonNull android.os.Bundle bundle, @androidx.annotation.NonNull java.lang.String key, @androidx.annotation.NonNull androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Retrieve the current Fragment instance for a reference previously
 * placed with {@link #putFragment(android.os.Bundle,java.lang.String,androidx.fragment.app.Fragment)}.
 *
 * @param bundle The bundle from which to retrieve the fragment reference.
 * @param key The name of the entry in the bundle.
 * @return Returns the current Fragment instance that is associated with
 * the given reference.
 */

@androidx.annotation.Nullable
public androidx.fragment.app.Fragment getFragment(@androidx.annotation.NonNull android.os.Bundle bundle, @androidx.annotation.NonNull java.lang.String key) { throw new RuntimeException("Stub!"); }

/**
 * Find a {@link androidx.fragment.app.Fragment Fragment} associated with the given {@link android.view.View View}.
 *
 * This method will locate the {@link androidx.fragment.app.Fragment Fragment} associated with this view. This is automatically
 * populated for the View returned by {@link androidx.fragment.app.Fragment#onCreateView Fragment#onCreateView} and its children.
 *
 * @param view the view to search from
 * @return the locally scoped {@link androidx.fragment.app.Fragment Fragment} to the given view
 * @throws java.lang.IllegalStateException if the given view does not correspond with a
 * {@link androidx.fragment.app.Fragment Fragment}.
 */

@androidx.annotation.NonNull
public static <F extends androidx.fragment.app.Fragment> F findFragment(@androidx.annotation.NonNull android.view.View view) { throw new RuntimeException("Stub!"); }

/**
 * Get a list of all fragments that are currently added to the FragmentManager.
 * This may include those that are hidden as well as those that are shown.
 * This will not include any fragments only in the back stack, or fragments that
 * are detached or removed.
 * <p>
 * The order of the fragments in the list is the order in which they were
 * added or attached.
 *
 * @return A list of all fragments that are added to the FragmentManager.
 */

@androidx.annotation.NonNull
public java.util.List<androidx.fragment.app.Fragment> getFragments() { throw new RuntimeException("Stub!"); }

/**
 * Save the current instance state of the given Fragment.  This can be
 * used later when creating a new instance of the Fragment and adding
 * it to the fragment manager, to have it create itself to match the
 * current state returned here.  Note that there are limits on how
 * this can be used:
 *
 * <ul>
 * <li>The Fragment must currently be attached to the FragmentManager.
 * <li>A new Fragment created using this saved state must be the same class
 * type as the Fragment it was created from.
 * <li>The saved state can not contain dependencies on other fragments --
 * that is it can't use {@link #putFragment(android.os.Bundle,java.lang.String,androidx.fragment.app.Fragment)} to
 * store a fragment reference because that reference may not be valid when
 * this saved state is later used.  Likewise the Fragment's target and
 * result code are not included in this state.
 * </ul>
 *
 * @param fragment The Fragment whose state is to be saved.
 * @return The generated state.  This will be null if there was no
 * interesting state created by the fragment.
 */

@androidx.annotation.Nullable
public androidx.fragment.app.Fragment.SavedState saveFragmentInstanceState(@androidx.annotation.NonNull androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Returns true if the final {@link android.app.Activity#onDestroy() Activity.onDestroy()}
 * call has been made on the FragmentManager's Activity, so this instance is now dead.
 */

public boolean isDestroyed() { throw new RuntimeException("Stub!"); }

@androidx.annotation.NonNull
public java.lang.String toString() { throw new RuntimeException("Stub!"); }

/**
 * Print the FragmentManager's state into the given stream.
 *
 * @param prefix Text to print at the front of each line.
 * @param fd The raw file descriptor that the dump is being sent to.
 * @param writer A PrintWriter to which the dump is to be set.
 * @param args Additional arguments to the dump request.
 */

public void dump(@androidx.annotation.NonNull java.lang.String prefix, @androidx.annotation.Nullable java.io.FileDescriptor fd, @androidx.annotation.NonNull java.io.PrintWriter writer, @androidx.annotation.Nullable java.lang.String[] args) { throw new RuntimeException("Stub!"); }

/**
 * Finds a fragment that was identified by the given id either when inflated
 * from XML or as the container ID when added in a transaction.  This first
 * searches through fragments that are currently added to the manager's
 * activity; if no such fragment is found, then all fragments currently
 * on the back stack associated with this ID are searched.
 * @return The fragment if found or null otherwise.
 */

@androidx.annotation.Nullable
public androidx.fragment.app.Fragment findFragmentById(int id) { throw new RuntimeException("Stub!"); }

/**
 * Finds a fragment that was identified by the given tag either when inflated
 * from XML or as supplied when added in a transaction.  This first
 * searches through fragments that are currently added to the manager's
 * activity; if no such fragment is found, then all fragments currently
 * on the back stack are searched.
 * <p>
 * If provided a {@code null} tag, this method returns null.
 *
 * @param tag the tag used to search for the fragment
 * @return The fragment if found or null otherwise.
 */

@androidx.annotation.Nullable
public androidx.fragment.app.Fragment findFragmentByTag(@androidx.annotation.Nullable java.lang.String tag) { throw new RuntimeException("Stub!"); }

/**
 * Returns {@code true} if the FragmentManager's state has already been saved
 * by its host. Any operations that would change saved state should not be performed
 * if this method returns true. For example, any popBackStack() method, such as
 * {@link #popBackStackImmediate()} or any FragmentTransaction using
 * {@link androidx.fragment.app.FragmentTransaction#commit() FragmentTransaction#commit()} instead of
 * {@link androidx.fragment.app.FragmentTransaction#commitAllowingStateLoss() FragmentTransaction#commitAllowingStateLoss()} will change
 * the state and will result in an error.
 *
 * @return true if this FragmentManager's state has already been saved by its host
 */

public boolean isStateSaved() { throw new RuntimeException("Stub!"); }

/**
 * Return the currently active primary navigation fragment for this FragmentManager.
 * The primary navigation fragment is set by fragment transactions using
 * {@link androidx.fragment.app.FragmentTransaction#setPrimaryNavigationFragment(androidx.fragment.app.Fragment) FragmentTransaction#setPrimaryNavigationFragment(Fragment)}.
 *
 * <p>The primary navigation fragment's
 * {@link androidx.fragment.app.Fragment#getChildFragmentManager() Fragment#getChildFragmentManager()} will be called first
 * to process delegated navigation actions such as {@link #popBackStack()} if no ID
 * or transaction name is provided to pop to.</p>
 *
 * @return the fragment designated as the primary navigation fragment
 */

@androidx.annotation.Nullable
public androidx.fragment.app.Fragment getPrimaryNavigationFragment() { throw new RuntimeException("Stub!"); }

/**
 * Set a {@link androidx.fragment.app.FragmentFactory FragmentFactory} for this FragmentManager that will be used
 * to create new Fragment instances from this point onward.
 * <p>
 * The {@link androidx.fragment.app.Fragment#getChildFragmentManager() Fragment#getChildFragmentManager()} of all Fragments
 * in this FragmentManager will also use this factory if one is not explicitly set.
 *
 * @param fragmentFactory the factory to use to create new Fragment instances
 * @see #getFragmentFactory()
 */

public void setFragmentFactory(@androidx.annotation.NonNull androidx.fragment.app.FragmentFactory fragmentFactory) { throw new RuntimeException("Stub!"); }

/**
 * Gets the current {@link androidx.fragment.app.FragmentFactory FragmentFactory} used to instantiate new Fragment instances.
 * <p>
 * If no factory has been explicitly set on this FragmentManager via
 * {@link #setFragmentFactory(androidx.fragment.app.FragmentFactory)}, the FragmentFactory of the
 * {@link androidx.fragment.app.Fragment#getParentFragmentManager() Fragment#getParentFragmentManager()} will be returned.
 *
 * @return the current FragmentFactory
 */

@androidx.annotation.NonNull
public androidx.fragment.app.FragmentFactory getFragmentFactory() { throw new RuntimeException("Stub!"); }

/**
 * Registers a {@link androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks FragmentLifecycleCallbacks} to listen to fragment lifecycle events
 * happening in this FragmentManager. All registered callbacks will be automatically
 * unregistered when this FragmentManager is destroyed.
 *
 * @param cb Callbacks to register
 * @param recursive true to automatically register this callback for all child FragmentManagers
 */

public void registerFragmentLifecycleCallbacks(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks cb, boolean recursive) { throw new RuntimeException("Stub!"); }

/**
 * Unregisters a previously registered {@link androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks FragmentLifecycleCallbacks}. If the callback
 * was not previously registered this call has no effect. All registered callbacks will be
 * automatically unregistered when this FragmentManager is destroyed.
 *
 * @param cb Callbacks to unregister
 */

public void unregisterFragmentLifecycleCallbacks(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks cb) { throw new RuntimeException("Stub!"); }

/**
 * Add a {@link androidx.fragment.app.FragmentOnAttachListener FragmentOnAttachListener} that should receive a call to
 * {@link androidx.fragment.app.FragmentOnAttachListener#onAttachFragment(androidx.fragment.app.FragmentManager,androidx.fragment.app.Fragment) FragmentOnAttachListener#onAttachFragment(FragmentManager, Fragment)} when a
 * new Fragment is attached to this FragmentManager.
 *
 * @param listener Listener to add
 */

public void addFragmentOnAttachListener(@androidx.annotation.NonNull androidx.fragment.app.FragmentOnAttachListener listener) { throw new RuntimeException("Stub!"); }

/**
 * Remove a {@link androidx.fragment.app.FragmentOnAttachListener FragmentOnAttachListener} that was previously added via
 * {@link #addFragmentOnAttachListener(androidx.fragment.app.FragmentOnAttachListener)}. It will no longer
 * get called when a new Fragment is attached.
 *
 * @param listener Listener to remove
 */

public void removeFragmentOnAttachListener(@androidx.annotation.NonNull androidx.fragment.app.FragmentOnAttachListener listener) { throw new RuntimeException("Stub!"); }

/**
 * Flag for {@link #popBackStack(java.lang.String,int)}
 * and {@link #popBackStack(int,int)}: If set, and the name or ID of
 * a back stack entry has been supplied, then all matching entries will
 * be consumed until one that doesn't match is found or the bottom of
 * the stack is reached.  Otherwise, all entries up to but not including that entry
 * will be removed.
 */

public static final int POP_BACK_STACK_INCLUSIVE = 1; // 0x1
/**
 * Representation of an entry on the fragment back stack, as created
 * with {@link androidx.fragment.app.FragmentTransaction#addToBackStack(java.lang.String) FragmentTransaction#addToBackStack(String)}.  Entries can later be
 * retrieved with {@link androidx.fragment.app.FragmentManager#getBackStackEntryAt(int) FragmentManager#getBackStackEntryAt(int)}.
 *
 * <p>Note that you should never hold on to a BackStackEntry object;
 * the identifier as returned by {@link #getId} is the only thing that
 * will be persisted across activity instances.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static interface BackStackEntry {

/**
 * Return the unique identifier for the entry.  This is the only
 * representation of the entry that will persist across activity
 * instances.
 */

public int getId();

/**
 * Get the name that was supplied to
 * {@link androidx.fragment.app.FragmentTransaction#addToBackStack(java.lang.String) FragmentTransaction#addToBackStack(String)} when creating this entry.
 */

@androidx.annotation.Nullable
public java.lang.String getName();

/**
 * Return the full bread crumb title resource identifier for the entry,
 * or 0 if it does not have one.
 * @deprecated Store breadcrumb titles separately from back stack entries. For example,
 * by using an <code>android:label</code> on a fragment in a navigation graph.
 */

@Deprecated
public int getBreadCrumbTitleRes();

/**
 * Return the short bread crumb title resource identifier for the entry,
 * or 0 if it does not have one.
 * @deprecated Store breadcrumb short titles separately from back stack entries. For
 * example, by using an <code>android:label</code> on a fragment in a navigation graph.
 */

@Deprecated
public int getBreadCrumbShortTitleRes();

/**
 * Return the full bread crumb title for the entry, or null if it
 * does not have one.
 * @deprecated Store breadcrumb titles separately from back stack entries. For example,
 *          * by using an <code>android:label</code> on a fragment in a navigation graph.
 */

@Deprecated
@androidx.annotation.Nullable
public java.lang.CharSequence getBreadCrumbTitle();

/**
 * Return the short bread crumb title for the entry, or null if it
 * does not have one.
 * @deprecated Store breadcrumb short titles separately from back stack entries. For
 * example, by using an <code>android:label</code> on a fragment in a navigation graph.
 */

@Deprecated
@androidx.annotation.Nullable
public java.lang.CharSequence getBreadCrumbShortTitle();
}

/**
 * Callback interface for listening to fragment state changes that happen
 * within a given FragmentManager.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract static class FragmentLifecycleCallbacks {

public FragmentLifecycleCallbacks() { throw new RuntimeException("Stub!"); }

/**
 * Called right before the fragment's {@link androidx.fragment.app.Fragment#onAttach(android.content.Context) Fragment#onAttach(Context)} method is called.
 * This is a good time to inject any required dependencies or perform other configuration
 * for the fragment before any of the fragment's lifecycle methods are invoked.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 * @param context Context that the Fragment is being attached to
 */

public void onFragmentPreAttached(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f, @androidx.annotation.NonNull android.content.Context context) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has been attached to its host. Its host will have had
 * <code>onAttachFragment</code> called before this call happens.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 * @param context Context that the Fragment was attached to
 */

public void onFragmentAttached(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f, @androidx.annotation.NonNull android.content.Context context) { throw new RuntimeException("Stub!"); }

/**
 * Called right before the fragment's {@link androidx.fragment.app.Fragment#onCreate(android.os.Bundle) Fragment#onCreate(Bundle)} method is called.
 * This is a good time to inject any required dependencies or perform other configuration
 * for the fragment.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 * @param savedInstanceState Saved instance bundle from a previous instance
 */

public void onFragmentPreCreated(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f, @androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has returned from the FragmentManager's call to
 * {@link androidx.fragment.app.Fragment#onCreate(android.os.Bundle) Fragment#onCreate(Bundle)}. This will only happen once for any given
 * fragment instance, though the fragment may be attached and detached multiple times.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 * @param savedInstanceState Saved instance bundle from a previous instance
 */

public void onFragmentCreated(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f, @androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has returned from the FragmentManager's call to
 * {@link androidx.fragment.app.Fragment#onActivityCreated(android.os.Bundle) Fragment#onActivityCreated(Bundle)}. This will only happen once for any given
 * fragment instance, though the fragment may be attached and detached multiple times.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 * @param savedInstanceState Saved instance bundle from a previous instance
 *
 * @deprecated To get a callback specifically when a Fragment activity's
 * {@link android.app.Activity#onCreate(Bundle)} is called, register a
 * {@link androidx.lifecycle.LifecycleObserver} on the Activity's {@link androidx.lifecycle.Lifecycle Lifecycle} in
 * {@link #onFragmentAttached(androidx.fragment.app.FragmentManager,androidx.fragment.app.Fragment,android.content.Context)}, removing it when it
 * receives the {@link androidx.lifecycle.Lifecycle.State#CREATED Lifecycle.State#CREATED} callback.
 */

@Deprecated
public void onFragmentActivityCreated(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f, @androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has returned a non-null view from the FragmentManager's
 * request to {@link androidx.fragment.app.Fragment#onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle) Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}.
 *
 * @param fm Host FragmentManager
 * @param f Fragment that created and owns the view
 * @param v View returned by the fragment
 * @param savedInstanceState Saved instance bundle from a previous instance
 */

public void onFragmentViewCreated(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f, @androidx.annotation.NonNull android.view.View v, @androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has returned from the FragmentManager's call to
 * {@link androidx.fragment.app.Fragment#onStart() Fragment#onStart()}.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 */

public void onFragmentStarted(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has returned from the FragmentManager's call to
 * {@link androidx.fragment.app.Fragment#onResume() Fragment#onResume()}.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 */

public void onFragmentResumed(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has returned from the FragmentManager's call to
 * {@link androidx.fragment.app.Fragment#onPause() Fragment#onPause()}.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 */

public void onFragmentPaused(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has returned from the FragmentManager's call to
 * {@link androidx.fragment.app.Fragment#onStop() Fragment#onStop()}.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 */

public void onFragmentStopped(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has returned from the FragmentManager's call to
 * {@link androidx.fragment.app.Fragment#onSaveInstanceState(android.os.Bundle) Fragment#onSaveInstanceState(Bundle)}.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 * @param outState Saved state bundle for the fragment
 */

public void onFragmentSaveInstanceState(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f, @androidx.annotation.NonNull android.os.Bundle outState) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has returned from the FragmentManager's call to
 * {@link androidx.fragment.app.Fragment#onDestroyView() Fragment#onDestroyView()}.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 */

public void onFragmentViewDestroyed(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has returned from the FragmentManager's call to
 * {@link androidx.fragment.app.Fragment#onDestroy() Fragment#onDestroy()}.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 */

public void onFragmentDestroyed(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f) { throw new RuntimeException("Stub!"); }

/**
 * Called after the fragment has returned from the FragmentManager's call to
 * {@link androidx.fragment.app.Fragment#onDetach() Fragment#onDetach()}.
 *
 * @param fm Host FragmentManager
 * @param f Fragment changing state
 */

public void onFragmentDetached(@androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm, @androidx.annotation.NonNull androidx.fragment.app.Fragment f) { throw new RuntimeException("Stub!"); }
}

/**
 * Interface to watch for changes to the back stack.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static interface OnBackStackChangedListener {

/**
 * Called whenever the contents of the back stack change.
 */

public void onBackStackChanged();
}

}

