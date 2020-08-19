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
import androidx.lifecycle.Lifecycle;
import android.view.View;

/**
 * Static library support version of the framework's {@link android.app.FragmentTransaction}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework SDK
 * documentation for a class overview.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class FragmentTransaction {

/**
 * @deprecated You should not instantiate a FragmentTransaction except via
 * {@link androidx.fragment.app.FragmentManager#beginTransaction() FragmentManager#beginTransaction()}.
 */

@Deprecated
public FragmentTransaction() { throw new RuntimeException("Stub!"); }

/**
 * Calls {@link #add(int,java.lang.Class,android.os.Bundle,java.lang.String)} with a 0 containerViewId.
 */

public final androidx.fragment.app.FragmentTransaction add(java.lang.Class<? extends androidx.fragment.app.Fragment> fragmentClass, android.os.Bundle args, java.lang.String tag) { throw new RuntimeException("Stub!"); }

/**
 * Calls {@link #add(int,androidx.fragment.app.Fragment,java.lang.String)} with a 0 containerViewId.
 */

public androidx.fragment.app.FragmentTransaction add(androidx.fragment.app.Fragment fragment, java.lang.String tag) { throw new RuntimeException("Stub!"); }

/**
 * Calls {@link #add(int,java.lang.Class,android.os.Bundle,java.lang.String)} with a null tag.
 */

public final androidx.fragment.app.FragmentTransaction add(int containerViewId, java.lang.Class<? extends androidx.fragment.app.Fragment> fragmentClass, android.os.Bundle args) { throw new RuntimeException("Stub!"); }

/**
 * Calls {@link #add(int,androidx.fragment.app.Fragment,java.lang.String)} with a null tag.
 */

public androidx.fragment.app.FragmentTransaction add(int containerViewId, androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Add a fragment to the activity state.  This fragment may optionally
 * also have its view (if {@link androidx.fragment.app.Fragment#onCreateView Fragment#onCreateView}
 * returns non-null) into a container view of the activity.
 *
 * @param containerViewId Optional identifier of the container this fragment is
 * to be placed in.  If 0, it will not be placed in a container.
 * @param fragmentClass The fragment to be added, created via the
 * {@link androidx.fragment.app.FragmentManager#getFragmentFactory() FragmentManager#getFragmentFactory()}.
 * @param args Optional arguments to be set on the fragment.
 * @param tag Optional tag name for the fragment, to later retrieve the
 * fragment with {@link androidx.fragment.app.FragmentManager#findFragmentByTag(java.lang.String) FragmentManager#findFragmentByTag(String)}.
 *
 * @return Returns the same FragmentTransaction instance.
 */

public final androidx.fragment.app.FragmentTransaction add(int containerViewId, java.lang.Class<? extends androidx.fragment.app.Fragment> fragmentClass, android.os.Bundle args, java.lang.String tag) { throw new RuntimeException("Stub!"); }

/**
 * Add a fragment to the activity state.  This fragment may optionally
 * also have its view (if {@link androidx.fragment.app.Fragment#onCreateView Fragment#onCreateView}
 * returns non-null) into a container view of the activity.
 *
 * @param containerViewId Optional identifier of the container this fragment is
 * to be placed in.  If 0, it will not be placed in a container.
 * @param fragment The fragment to be added.  This fragment must not already
 * be added to the activity.
 * @param tag Optional tag name for the fragment, to later retrieve the
 * fragment with {@link androidx.fragment.app.FragmentManager#findFragmentByTag(java.lang.String) FragmentManager#findFragmentByTag(String)}.
 *
 * @return Returns the same FragmentTransaction instance.
 */

public androidx.fragment.app.FragmentTransaction add(int containerViewId, androidx.fragment.app.Fragment fragment, java.lang.String tag) { throw new RuntimeException("Stub!"); }

/**
 * Calls {@link #replace(int,java.lang.Class,android.os.Bundle,java.lang.String)} with a null tag.
 */

public final androidx.fragment.app.FragmentTransaction replace(int containerViewId, java.lang.Class<? extends androidx.fragment.app.Fragment> fragmentClass, android.os.Bundle args) { throw new RuntimeException("Stub!"); }

/**
 * Calls {@link #replace(int,androidx.fragment.app.Fragment,java.lang.String)} with a null tag.
 */

public androidx.fragment.app.FragmentTransaction replace(int containerViewId, androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Replace an existing fragment that was added to a container.  This is
 * essentially the same as calling {@link #remove(androidx.fragment.app.Fragment)} for all
 * currently added fragments that were added with the same containerViewId
 * and then {@link #add(int,androidx.fragment.app.Fragment,java.lang.String)} with the same arguments
 * given here.
 *
 * @param containerViewId Identifier of the container whose fragment(s) are
 * to be replaced.
 * @param fragmentClass The new fragment to place in the container, created via the
 * {@link androidx.fragment.app.FragmentManager#getFragmentFactory() FragmentManager#getFragmentFactory()}.
 * @param args Optional arguments to be set on the fragment.
 * @param tag Optional tag name for the fragment, to later retrieve the
 * fragment with {@link androidx.fragment.app.FragmentManager#findFragmentByTag(java.lang.String) FragmentManager#findFragmentByTag(String)}.
 *
 * @return Returns the same FragmentTransaction instance.
 */

public final androidx.fragment.app.FragmentTransaction replace(int containerViewId, java.lang.Class<? extends androidx.fragment.app.Fragment> fragmentClass, android.os.Bundle args, java.lang.String tag) { throw new RuntimeException("Stub!"); }

/**
 * Replace an existing fragment that was added to a container.  This is
 * essentially the same as calling {@link #remove(androidx.fragment.app.Fragment)} for all
 * currently added fragments that were added with the same containerViewId
 * and then {@link #add(int,androidx.fragment.app.Fragment,java.lang.String)} with the same arguments
 * given here.
 *
 * @param containerViewId Identifier of the container whose fragment(s) are
 * to be replaced.
 * @param fragment The new fragment to place in the container.
 * @param tag Optional tag name for the fragment, to later retrieve the
 * fragment with {@link androidx.fragment.app.FragmentManager#findFragmentByTag(java.lang.String) FragmentManager#findFragmentByTag(String)}.
 *
 * @return Returns the same FragmentTransaction instance.
 */

public androidx.fragment.app.FragmentTransaction replace(int containerViewId, androidx.fragment.app.Fragment fragment, java.lang.String tag) { throw new RuntimeException("Stub!"); }

/**
 * Remove an existing fragment.  If it was added to a container, its view
 * is also removed from that container.
 *
 * @param fragment The fragment to be removed.
 *
 * @return Returns the same FragmentTransaction instance.
 */

public androidx.fragment.app.FragmentTransaction remove(androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Hides an existing fragment.  This is only relevant for fragments whose
 * views have been added to a container, as this will cause the view to
 * be hidden.
 *
 * @param fragment The fragment to be hidden.
 *
 * @return Returns the same FragmentTransaction instance.
 */

public androidx.fragment.app.FragmentTransaction hide(androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Shows a previously hidden fragment.  This is only relevant for fragments whose
 * views have been added to a container, as this will cause the view to
 * be shown.
 *
 * @param fragment The fragment to be shown.
 *
 * @return Returns the same FragmentTransaction instance.
 */

public androidx.fragment.app.FragmentTransaction show(androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Detach the given fragment from the UI.  This is the same state as
 * when it is put on the back stack: the fragment is removed from
 * the UI, however its state is still being actively managed by the
 * fragment manager.  When going into this state its view hierarchy
 * is destroyed.
 *
 * @param fragment The fragment to be detached.
 *
 * @return Returns the same FragmentTransaction instance.
 */

public androidx.fragment.app.FragmentTransaction detach(androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Re-attach a fragment after it had previously been detached from
 * the UI with {@link #detach(androidx.fragment.app.Fragment)}.  This
 * causes its view hierarchy to be re-created, attached to the UI,
 * and displayed.
 *
 * @param fragment The fragment to be attached.
 *
 * @return Returns the same FragmentTransaction instance.
 */

public androidx.fragment.app.FragmentTransaction attach(androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Set a currently active fragment in this FragmentManager as the primary navigation fragment.
 *
 * <p>The primary navigation fragment's
 * {@link androidx.fragment.app.Fragment#getChildFragmentManager() Fragment#getChildFragmentManager()} will be called first
 * to process delegated navigation actions such as {@link androidx.fragment.app.FragmentManager#popBackStack() FragmentManager#popBackStack()}
 * if no ID or transaction name is provided to pop to. Navigation operations outside of the
 * fragment system may choose to delegate those actions to the primary navigation fragment
 * as returned by {@link androidx.fragment.app.FragmentManager#getPrimaryNavigationFragment() FragmentManager#getPrimaryNavigationFragment()}.</p>
 *
 * <p>The fragment provided must currently be added to the FragmentManager to be set as
 * a primary navigation fragment, or previously added as part of this transaction.</p>
 *
 * @param fragment the fragment to set as the primary navigation fragment
 * @return the same FragmentTransaction instance
 */

public androidx.fragment.app.FragmentTransaction setPrimaryNavigationFragment(androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Set a ceiling for the state of an active fragment in this FragmentManager. If fragment is
 * already above the received state, it will be forced down to the correct state.
 *
 * <p>The fragment provided must currently be added to the FragmentManager to have it's
 * Lifecycle state capped, or previously added as part of this transaction. If the
 * {@link androidx.lifecycle.Lifecycle.State#INITIALIZED Lifecycle.State#INITIALIZED} is passed in as the {@link androidx.lifecycle.Lifecycle.State Lifecycle.State} and the
 * provided fragment has already moved beyond {@link androidx.lifecycle.Lifecycle.State#INITIALIZED Lifecycle.State#INITIALIZED}, an
 * {@link java.lang.IllegalArgumentException IllegalArgumentException} will be thrown.</p>
 *
 * @param fragment the fragment to have it's state capped.
 * @param state the ceiling state for the fragment.
 * @return the same FragmentTransaction instance
 */

public androidx.fragment.app.FragmentTransaction setMaxLifecycle(androidx.fragment.app.Fragment fragment, androidx.lifecycle.Lifecycle.State state) { throw new RuntimeException("Stub!"); }

/**
 * @return <code>true</code> if this transaction contains no operations,
 * <code>false</code> otherwise.
 */

public boolean isEmpty() { throw new RuntimeException("Stub!"); }

/**
 * Set specific animation resources to run for the fragments that are
 * entering and exiting in this transaction. These animations will not be
 * played when popping the back stack.
 *
 * <p>This method applies the custom animations to all future fragment operations; previous
 * operations are unaffected. Fragment operations in the same {@link androidx.fragment.app.FragmentTransaction FragmentTransaction} can
 * set different animations by calling this method prior to each operation, e.g:
 *
 * <pre class="prettyprint">
 *  fragmentManager.beingTransaction()
 *      .setCustomAnimations(enter1, exit1)
 *      .add(MyFragmentClass, args, tag1) // this fragment gets the first animations
 *      .setCustomAnimations(enter2, exit2)
 *      .add(MyFragmentClass, args, tag2) // this fragment gets the second animations
 *      .commit()
 * </pre>
 *
 * @param enter An animation or animator resource ID used for the enter animation on the
 *              view of the fragment being added or attached.
 * @param exit An animation or animator resource ID used for the exit animation on the
 *             view of the fragment being removed or detached.
 */

public androidx.fragment.app.FragmentTransaction setCustomAnimations(int enter, int exit) { throw new RuntimeException("Stub!"); }

/**
 * Set specific animation resources to run for the fragments that are
 * entering and exiting in this transaction. The <code>popEnter</code>
 * and <code>popExit</code> animations will be played for enter/exit
 * operations specifically when popping the back stack.
 *
 * <p>This method applies the custom animations to all future fragment operations; previous
 * operations are unaffected. Fragment operations in the same {@link androidx.fragment.app.FragmentTransaction FragmentTransaction} can
 * set different animations by calling this method prior to each operation, e.g:
 *
 * <pre class="prettyprint">
 *  fragmentManager.beingTransaction()
 *      .setCustomAnimations(enter1, exit1, popEnter1, popExit1)
 *      .add(MyFragmentClass, args, tag1) // this fragment gets the first animations
 *      .setCustomAnimations(enter2, exit2, popEnter2, popExit2)
 *      .add(MyFragmentClass, args, tag2) // this fragment gets the second animations
 *      .commit()
 * </pre>
 *
 * @param enter An animation or animator resource ID used for the enter animation on the
 *              view of the fragment being added or attached.
 * @param exit An animation or animator resource ID used for the exit animation on the
 *             view of the fragment being removed or detached.
 * @param popEnter An animation or animator resource ID used for the enter animation on the
 *                 view of the fragment being readded or reattached caused by
 *                 {@link androidx.fragment.app.FragmentManager#popBackStack() FragmentManager#popBackStack()} or similar methods.
 * @param popExit An animation or animator resource ID used for the enter animation on the
 *                view of the fragment being removed or detached caused by
 *                {@link androidx.fragment.app.FragmentManager#popBackStack() FragmentManager#popBackStack()} or similar methods.
 */

public androidx.fragment.app.FragmentTransaction setCustomAnimations(int enter, int exit, int popEnter, int popExit) { throw new RuntimeException("Stub!"); }

/**
 * Used with custom Transitions to map a View from a removed or hidden
 * Fragment to a View from a shown or added Fragment.
 * <var>sharedElement</var> must have a unique transitionName in the View hierarchy.
 *
 * @param sharedElement A View in a disappearing Fragment to match with a View in an
 *                      appearing Fragment.
 * @param name The transitionName for a View in an appearing Fragment to match to the shared
 *             element.
 * @see androidx.fragment.app.Fragment#setSharedElementReturnTransition(Object)
 * @see androidx.fragment.app.Fragment#setSharedElementEnterTransition(Object)
 */

public androidx.fragment.app.FragmentTransaction addSharedElement(android.view.View sharedElement, java.lang.String name) { throw new RuntimeException("Stub!"); }

/**
 * Select a standard transition animation for this transaction.  May be
 * one of {@link #TRANSIT_NONE}, {@link #TRANSIT_FRAGMENT_OPEN},
 * {@link #TRANSIT_FRAGMENT_CLOSE}, or {@link #TRANSIT_FRAGMENT_FADE}.

 * @param transition Value is {@link androidx.fragment.app.FragmentTransaction#TRANSIT_NONE}, {@link androidx.fragment.app.FragmentTransaction#TRANSIT_FRAGMENT_OPEN}, {@link androidx.fragment.app.FragmentTransaction#TRANSIT_FRAGMENT_CLOSE}, or {@link androidx.fragment.app.FragmentTransaction#TRANSIT_FRAGMENT_FADE}
 */

public androidx.fragment.app.FragmentTransaction setTransition(int transition) { throw new RuntimeException("Stub!"); }

/**
 * Set a custom style resource that will be used for resolving transit
 * animations.
 *
 * @deprecated The desired functionality never worked correctly. This should not be used.
 */

@Deprecated
public androidx.fragment.app.FragmentTransaction setTransitionStyle(int styleRes) { throw new RuntimeException("Stub!"); }

/**
 * Add this transaction to the back stack.  This means that the transaction
 * will be remembered after it is committed, and will reverse its operation
 * when later popped off the stack.
 * <p>
 * {@link #setReorderingAllowed(boolean)} must be set to <code>true</code>
 * in the same transaction as addToBackStack() to allow the pop of that
 * transaction to be reordered.
 *
 * @param name An optional name for this back stack state, or null.
 */

public androidx.fragment.app.FragmentTransaction addToBackStack(java.lang.String name) { throw new RuntimeException("Stub!"); }

/**
 * Returns true if this FragmentTransaction is allowed to be added to the back
 * stack. If this method would return false, {@link #addToBackStack(java.lang.String)}
 * will throw {@link java.lang.IllegalStateException IllegalStateException}.
 *
 * @return True if {@link #addToBackStack(java.lang.String)} is permitted on this transaction.
 */

public boolean isAddToBackStackAllowed() { throw new RuntimeException("Stub!"); }

/**
 * Disallow calls to {@link #addToBackStack(java.lang.String)}. Any future calls to
 * addToBackStack will throw {@link java.lang.IllegalStateException IllegalStateException}. If addToBackStack
 * has already been called, this method will throw IllegalStateException.
 */

public androidx.fragment.app.FragmentTransaction disallowAddToBackStack() { throw new RuntimeException("Stub!"); }

/**
 * Set the full title to show as a bread crumb when this transaction
 * is on the back stack.
 *
 * @param res A string resource containing the title.
 * @deprecated Store breadcrumb titles separately from fragment transactions. For
 * example, by using an <code>android:label</code> on a fragment in a navigation graph.
 */

@Deprecated
public androidx.fragment.app.FragmentTransaction setBreadCrumbTitle(int res) { throw new RuntimeException("Stub!"); }

/**
 * Like {@link #setBreadCrumbTitle(int)} but taking a raw string; this
 * method is <em>not</em> recommended, as the string can not be changed
 * later if the locale changes.
 * @deprecated Store breadcrumb titles separately from fragment transactions. For
 * example, by using an <code>android:label</code> on a fragment in a navigation graph.
 */

@Deprecated
public androidx.fragment.app.FragmentTransaction setBreadCrumbTitle(java.lang.CharSequence text) { throw new RuntimeException("Stub!"); }

/**
 * Set the short title to show as a bread crumb when this transaction
 * is on the back stack.
 *
 * @param res A string resource containing the title.
 * @deprecated Store breadcrumb short titles separately from fragment transactions. For
 * example, by using an <code>android:label</code> on a fragment in a navigation graph.
 */

@Deprecated
public androidx.fragment.app.FragmentTransaction setBreadCrumbShortTitle(int res) { throw new RuntimeException("Stub!"); }

/**
 * Like {@link #setBreadCrumbShortTitle(int)} but taking a raw string; this
 * method is <em>not</em> recommended, as the string can not be changed
 * later if the locale changes.
 * @deprecated Store breadcrumb short titles separately from fragment transactions. For
 * example, by using an <code>android:label</code> on a fragment in a navigation graph.
 */

@Deprecated
public androidx.fragment.app.FragmentTransaction setBreadCrumbShortTitle(java.lang.CharSequence text) { throw new RuntimeException("Stub!"); }

/**
 * Sets whether or not to allow optimizing operations within and across
 * transactions. This will remove redundant operations, eliminating
 * operations that cancel. For example, if two transactions are executed
 * together, one that adds a fragment A and the next replaces it with fragment B,
 * the operations will cancel and only fragment B will be added. That means that
 * fragment A may not go through the creation/destruction lifecycle.
 * <p>
 * The side effect of removing redundant operations is that fragments may have state changes
 * out of the expected order. For example, one transaction adds fragment A,
 * a second adds fragment B, then a third removes fragment A. Without removing the redundant
 * operations, fragment B could expect that while it is being created, fragment A will also
 * exist because fragment A will be removed after fragment B was added.
 * With removing redundant operations, fragment B cannot expect fragment A to exist when
 * it has been created because fragment A's add/remove will be optimized out.
 * <p>
 * It can also reorder the state changes of Fragments to allow for better Transitions.
 * Added Fragments may have {@link androidx.fragment.app.Fragment#onCreate(android.os.Bundle) Fragment#onCreate(Bundle)} called before replaced
 * Fragments have {@link androidx.fragment.app.Fragment#onDestroy() Fragment#onDestroy()} called.
 * <p>
 * {@link androidx.fragment.app.Fragment#postponeEnterTransition() Fragment#postponeEnterTransition()} requires {@code setReorderingAllowed(true)}.
 * <p>
 * The default is {@code false}.
 *
 * @param reorderingAllowed {@code true} to enable optimizing out redundant operations
 *                          or {@code false} to disable optimizing out redundant
 *                          operations on this transaction.
 */

public androidx.fragment.app.FragmentTransaction setReorderingAllowed(boolean reorderingAllowed) { throw new RuntimeException("Stub!"); }

/**
 * @deprecated This has been renamed {@link #setReorderingAllowed(boolean)}.
 */

@Deprecated
public androidx.fragment.app.FragmentTransaction setAllowOptimization(boolean allowOptimization) { throw new RuntimeException("Stub!"); }

/**
 * Add a Runnable to this transaction that will be run after this transaction has
 * been committed. If fragment transactions are {@link #setReorderingAllowed(boolean) optimized}
 * this may be after other subsequent fragment operations have also taken place, or operations
 * in this transaction may have been optimized out due to the presence of a subsequent
 * fragment transaction in the batch.
 *
 * <p>If a transaction is committed using {@link #commitAllowingStateLoss()} this runnable
 * may be executed when the FragmentManager is in a state where new transactions may not
 * be committed without allowing state loss.</p>
 *
 * <p><code>runOnCommit</code> may not be used with transactions
 * {@link #addToBackStack(java.lang.String) added to the back stack} as Runnables cannot be persisted
 * with back stack state. {@link java.lang.IllegalStateException IllegalStateException} will be thrown if
 * {@link #addToBackStack(java.lang.String)} has been previously called for this transaction
 * or if it is called after a call to <code>runOnCommit</code>.</p>
 *
 * @param runnable Runnable to add
 * @return this FragmentTransaction
 * @throws java.lang.IllegalStateException if {@link #addToBackStack(java.lang.String)} has been called
 */

public androidx.fragment.app.FragmentTransaction runOnCommit(java.lang.Runnable runnable) { throw new RuntimeException("Stub!"); }

/**
 * Schedules a commit of this transaction.  The commit does
 * not happen immediately; it will be scheduled as work on the main thread
 * to be done the next time that thread is ready.
 *
 * <p class="note">A transaction can only be committed with this method
 * prior to its containing activity saving its state.  If the commit is
 * attempted after that point, an exception will be thrown.  This is
 * because the state after the commit can be lost if the activity needs to
 * be restored from its state.  See {@link #commitAllowingStateLoss()} for
 * situations where it may be okay to lose the commit.</p>
 *
 * @return Returns the identifier of this transaction's back stack entry,
 * if {@link #addToBackStack(java.lang.String)} had been called.  Otherwise, returns
 * a negative number.
 */

public abstract int commit();

/**
 * Like {@link #commit} but allows the commit to be executed after an
 * activity's state is saved.  This is dangerous because the commit can
 * be lost if the activity needs to later be restored from its state, so
 * this should only be used for cases where it is okay for the UI state
 * to change unexpectedly on the user.
 */

public abstract int commitAllowingStateLoss();

/**
 * Commits this transaction synchronously. Any added fragments will be
 * initialized and brought completely to the lifecycle state of their host
 * and any removed fragments will be torn down accordingly before this
 * call returns. Committing a transaction in this way allows fragments
 * to be added as dedicated, encapsulated components that monitor the
 * lifecycle state of their host while providing firmer ordering guarantees
 * around when those fragments are fully initialized and ready. Fragments
 * that manage views will have those views created and attached.
 *
 * <p>Calling <code>commitNow</code> is preferable to calling
 * {@link #commit()} followed by {@link androidx.fragment.app.FragmentManager#executePendingTransactions() FragmentManager#executePendingTransactions()}
 * as the latter will have the side effect of attempting to commit <em>all</em>
 * currently pending transactions whether that is the desired behavior
 * or not.</p>
 *
 * <p>Transactions committed in this way may not be added to the
 * FragmentManager's back stack, as doing so would break other expected
 * ordering guarantees for other asynchronously committed transactions.
 * This method will throw {@link java.lang.IllegalStateException IllegalStateException} if the transaction
 * previously requested to be added to the back stack with
 * {@link #addToBackStack(java.lang.String)}.</p>
 *
 * <p class="note">A transaction can only be committed with this method
 * prior to its containing activity saving its state.  If the commit is
 * attempted after that point, an exception will be thrown.  This is
 * because the state after the commit can be lost if the activity needs to
 * be restored from its state.  See {@link #commitAllowingStateLoss()} for
 * situations where it may be okay to lose the commit.</p>
 */

public abstract void commitNow();

/**
 * Like {@link #commitNow} but allows the commit to be executed after an
 * activity's state is saved.  This is dangerous because the commit can
 * be lost if the activity needs to later be restored from its state, so
 * this should only be used for cases where it is okay for the UI state
 * to change unexpectedly on the user.
 */

public abstract void commitNowAllowingStateLoss();

/**
 * Bit mask that is set for all enter transitions.
 */

public static final int TRANSIT_ENTER_MASK = 4096; // 0x1000

/**
 * Bit mask that is set for all exit transitions.
 */

public static final int TRANSIT_EXIT_MASK = 8192; // 0x2000

/** Fragment is being removed from the stack */

public static final int TRANSIT_FRAGMENT_CLOSE = 8194; // 0x2002

/** Fragment should simply fade in or out; that is, no strong navigation associated
 * with it except that it is appearing or disappearing for some reason. */

public static final int TRANSIT_FRAGMENT_FADE = 4099; // 0x1003

/** Fragment is being added onto the stack */

public static final int TRANSIT_FRAGMENT_OPEN = 4097; // 0x1001

/** No animation for transition. */

public static final int TRANSIT_NONE = 0; // 0x0

/** Not set up for a transition. */

public static final int TRANSIT_UNSET = -1; // 0xffffffff
}

