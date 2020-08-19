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
import android.view.Window;
import android.app.Dialog;
import android.content.DialogInterface;

/**
 * Static library support version of the framework's {@link android.app.DialogFragment}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework SDK
 * documentation for a class overview.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class DialogFragment extends androidx.fragment.app.Fragment implements android.content.DialogInterface.OnCancelListener, android.content.DialogInterface.OnDismissListener {

/**
 * Constructor used by the default {@link androidx.fragment.app.FragmentFactory FragmentFactory}. You must
 * {@link androidx.fragment.app.FragmentManager#setFragmentFactory(androidx.fragment.app.FragmentFactory) FragmentManager#setFragmentFactory(FragmentFactory)}
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
 * its context.
 */

public DialogFragment() { throw new RuntimeException("Stub!"); }

/**
 * Alternate constructor that can be called from your default, no argument constructor to
 * provide a default layout that will be inflated by
 * {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)}.
 *
 * <pre class="prettyprint">
 * class MyDialogFragment extends DialogFragment {
 *   public MyDialogFragment() {
 *     super(R.layout.dialog_fragment_main);
 *   }
 * }
 * </pre>
 *
 * You must
 * {@link androidx.fragment.app.FragmentManager#setFragmentFactory(androidx.fragment.app.FragmentFactory) FragmentManager#setFragmentFactory(FragmentFactory)}
 * if you want to use a non-default constructor to ensure that your constructor is called
 * when the fragment is re-instantiated.
 *
 * @see #DialogFragment()
 * @see #onCreateView(LayoutInflater, ViewGroup, Bundle)
 */

public DialogFragment(int contentLayoutId) { throw new RuntimeException("Stub!"); }

/**
 * Call to customize the basic appearance and behavior of the
 * fragment's dialog.  This can be used for some common dialog behaviors,
 * taking care of selecting flags, theme, and other options for you.  The
 * same effect can be achieve by manually setting Dialog and Window
 * attributes yourself.  Calling this after the fragment's Dialog is
 * created will have no effect.
 *
 * @param style Selects a standard style: may be {@link #STYLE_NORMAL},
 * {@link #STYLE_NO_TITLE}, {@link #STYLE_NO_FRAME}, or
 * {@link #STYLE_NO_INPUT}.
 * Value is {@link androidx.fragment.app.DialogFragment#STYLE_NORMAL}, {@link androidx.fragment.app.DialogFragment#STYLE_NO_TITLE}, {@link androidx.fragment.app.DialogFragment#STYLE_NO_FRAME}, or {@link androidx.fragment.app.DialogFragment#STYLE_NO_INPUT}
 * @param theme Optional custom theme.  If 0, an appropriate theme (based
 * on the style) will be selected for you.
 */

public void setStyle(int style, int theme) { throw new RuntimeException("Stub!"); }

/**
 * Display the dialog, adding the fragment to the given FragmentManager.  This
 * is a convenience for explicitly creating a transaction, adding the
 * fragment to it with the given tag, and {@link androidx.fragment.app.FragmentTransaction#commit() FragmentTransaction#commit()} it.
 * This does <em>not</em> add the transaction to the fragment back stack.  When the fragment
 * is dismissed, a new transaction will be executed to remove it from
 * the activity.
 * @param manager The FragmentManager this fragment will be added to.
 * @param tag The tag for this fragment, as per
 * {@link androidx.fragment.app.FragmentTransaction#add(androidx.fragment.app.Fragment,java.lang.String) FragmentTransaction#add(Fragment, String)}.
 */

public void show(androidx.fragment.app.FragmentManager manager, java.lang.String tag) { throw new RuntimeException("Stub!"); }

/**
 * Display the dialog, adding the fragment using an existing transaction
 * and then {@link androidx.fragment.app.FragmentTransaction#commit() FragmentTransaction#commit()} the transaction.
 * @param transaction An existing transaction in which to add the fragment.
 * @param tag The tag for this fragment, as per
 * {@link androidx.fragment.app.FragmentTransaction#add(androidx.fragment.app.Fragment,java.lang.String) FragmentTransaction#add(Fragment, String)}.
 * @return Returns the identifier of the committed transaction, as per
 * {@link androidx.fragment.app.FragmentTransaction#commit() FragmentTransaction#commit()}.
 */

public int show(androidx.fragment.app.FragmentTransaction transaction, java.lang.String tag) { throw new RuntimeException("Stub!"); }

/**
 * Display the dialog, immediately adding the fragment to the given FragmentManager.  This
 * is a convenience for explicitly creating a transaction, adding the
 * fragment to it with the given tag, and calling {@link androidx.fragment.app.FragmentTransaction#commitNow() FragmentTransaction#commitNow()}.
 * This does <em>not</em> add the transaction to the fragment back stack.  When the fragment
 * is dismissed, a new transaction will be executed to remove it from
 * the activity.
 * @param manager The FragmentManager this fragment will be added to.
 * @param tag The tag for this fragment, as per
 * {@link androidx.fragment.app.FragmentTransaction#add(androidx.fragment.app.Fragment,java.lang.String) FragmentTransaction#add(Fragment, String)}.
 */

public void showNow(androidx.fragment.app.FragmentManager manager, java.lang.String tag) { throw new RuntimeException("Stub!"); }

/**
 * Dismiss the fragment and its dialog.  If the fragment was added to the
 * back stack, all back stack state up to and including this entry will
 * be popped.  Otherwise, a new transaction will be committed to remove
 * the fragment.
 */

public void dismiss() { throw new RuntimeException("Stub!"); }

/**
 * Version of {@link #dismiss()} that uses
 * {@link androidx.fragment.app.FragmentTransaction#commitAllowingStateLoss() FragmentTransaction#commitAllowingStateLoss()}. See linked
 * documentation for further details.
 */

public void dismissAllowingStateLoss() { throw new RuntimeException("Stub!"); }

/**
 * Return the {@link android.app.Dialog Dialog} this fragment is currently controlling.
 *
 * @see #requireDialog()
 */

public android.app.Dialog getDialog() { throw new RuntimeException("Stub!"); }

/**
 * Return the {@link android.app.Dialog Dialog} this fragment is currently controlling.
 *
 * @throws java.lang.IllegalStateException if the Dialog has not yet been created (before
 * {@link #onCreateDialog(android.os.Bundle)}) or has been destroyed (after {@link #onDestroyView()}.
 * @see #getDialog()
 */

public final android.app.Dialog requireDialog() { throw new RuntimeException("Stub!"); }

public int getTheme() { throw new RuntimeException("Stub!"); }

/**
 * Control whether the shown Dialog is cancelable.  Use this instead of
 * directly calling {@link android.app.Dialog#setCancelable(boolean) Dialog#setCancelable(boolean)}, because DialogFragment needs to change
 * its behavior based on this.
 *
 * @param cancelable If true, the dialog is cancelable.  The default
 * is true.
 */

public void setCancelable(boolean cancelable) { throw new RuntimeException("Stub!"); }

/**
 * Return the current value of {@link #setCancelable(boolean)}.
 */

public boolean isCancelable() { throw new RuntimeException("Stub!"); }

/**
 * Controls whether this fragment should be shown in a dialog.  If not
 * set, no Dialog will be created and the fragment's view hierarchy will
 * thus not be added to it.  This allows you to instead use it as a
 * normal fragment (embedded inside of its activity).
 *
 * <p>This is normally set for you based on whether the fragment is
 * associated with a container view ID passed to
 * {@link androidx.fragment.app.FragmentTransaction#add(int,androidx.fragment.app.Fragment) FragmentTransaction#add(int, Fragment)}.
 * If the fragment was added with a container, setShowsDialog will be
 * initialized to false; otherwise, it will be true.
 *
 * <p>If calling this manually, it should be called in {@link #onCreate(android.os.Bundle)}
 * as calling it any later will have no effect.
 *
 * @param showsDialog If true, the fragment will be displayed in a Dialog.
 * If false, no Dialog will be created and the fragment's view hierarchy
 * left undisturbed.
 */

public void setShowsDialog(boolean showsDialog) { throw new RuntimeException("Stub!"); }

/**
 * Return the current value of {@link #setShowsDialog(boolean)}.
 */

public boolean getShowsDialog() { throw new RuntimeException("Stub!"); }

public void onAttach(android.content.Context context) { throw new RuntimeException("Stub!"); }

public void onDetach() { throw new RuntimeException("Stub!"); }

public void onCreate(android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * {@inheritDoc}
 *
 * <p>
 * If this is called from within {@link #onCreateDialog(android.os.Bundle)}, the layout inflater from
 * {@link androidx.fragment.app.Fragment#onGetLayoutInflater(android.os.Bundle) Fragment#onGetLayoutInflater(Bundle)}, without the dialog theme, will be returned.
 */

public android.view.LayoutInflater onGetLayoutInflater(android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Override to build your own custom Dialog container.  This is typically
 * used to show an AlertDialog instead of a generic Dialog; when doing so,
 * {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)} does not need
 * to be implemented since the AlertDialog takes care of its own content.
 *
 * <p>This method will be called after {@link #onCreate(android.os.Bundle)} and
 * immediately before {@link #onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)}.  The
 * default implementation simply instantiates and returns a {@link android.app.Dialog Dialog}
 * class.
 *
 * <p><em>Note: DialogFragment own the {@link android.app.Dialog#setOnCancelListener Dialog#setOnCancelListener} and {@link android.app.Dialog#setOnDismissListener Dialog#setOnDismissListener} callbacks.  You must not set them yourself.</em>
 * To find out about these events, override {@link #onCancel(android.content.DialogInterface)}
 * and {@link #onDismiss(android.content.DialogInterface)}.</p>
 *
 * @param savedInstanceState The last saved instance state of the Fragment,
 * or null if this is a freshly created Fragment.
 *
 * @return Return a new Dialog instance to be displayed by the Fragment.
 */

public android.app.Dialog onCreateDialog(android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

public void onCancel(android.content.DialogInterface dialog) { throw new RuntimeException("Stub!"); }

public void onDismiss(android.content.DialogInterface dialog) { throw new RuntimeException("Stub!"); }

public void onViewStateRestored(android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

public void onStart() { throw new RuntimeException("Stub!"); }

public void onSaveInstanceState(android.os.Bundle outState) { throw new RuntimeException("Stub!"); }

public void onStop() { throw new RuntimeException("Stub!"); }

/**
 * Remove dialog.
 */

public void onDestroyView() { throw new RuntimeException("Stub!"); }

/**
 * Style for {@link #setStyle(int,int)}: a basic,
 * normal dialog.
 */

public static final int STYLE_NORMAL = 0; // 0x0

/**
 * Style for {@link #setStyle(int,int)}: don't draw
 * any frame at all; the view hierarchy returned by {@link #onCreateView}
 * is entirely responsible for drawing the dialog.
 */

public static final int STYLE_NO_FRAME = 2; // 0x2

/**
 * Style for {@link #setStyle(int,int)}: like
 * {@link #STYLE_NO_FRAME}, but also disables all input to the dialog.
 * The user can not touch it, and its window will not receive input focus.
 */

public static final int STYLE_NO_INPUT = 3; // 0x3

/**
 * Style for {@link #setStyle(int,int)}: don't include
 * a title area.
 */

public static final int STYLE_NO_TITLE = 1; // 0x1
}

