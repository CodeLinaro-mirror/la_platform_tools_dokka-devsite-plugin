/*
 * Copyright 2019 The Android Open Source Project
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
import android.animation.LayoutTransition;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.view.View;

/**
 * FragmentContainerView is a customized Layout designed specifically for Fragments. It extends
 * {@link android.widget.FrameLayout FrameLayout}, so it can reliably handle Fragment Transactions, and it also has additional
 * features to coordinate with fragment behavior.
 *
 * <p>FragmentContainerView should be used as the container for Fragments, commonly set in the
 * xml layout of an activity, e.g.: <p>
 *
 * <pre class="prettyprint">
 * &lt;androidx.fragment.app.FragmentContainerView
 *        xmlns:android="http://schemas.android.com/apk/res/android"
 *        xmlns:app="http://schemas.android.com/apk/res-auto"
 *        android:id="@+id/fragment_container_view"
 *        android:layout_width="match_parent"
 *        android:layout_height="match_parent"&gt;
 * &lt;/androidx.fragment.app.FragmentContainerView&gt;
 * </pre>
 *
 * <p> FragmentContainerView can also be used to add a Fragment by using the
 * <code>android:name</code> attribute. FragmentContainerView will perform a one time operation
 * that:
 *
 * <ul>
 * <li>Creates a new instance of the Fragment</li>
 * <li>Calls {@link androidx.fragment.app.Fragment#onInflate(android.content.Context,android.util.AttributeSet,android.os.Bundle) Fragment#onInflate(Context, AttributeSet, Bundle)}</li>
 * <li>Executes a FragmentTransaction to add the Fragment to the appropriate FragmentManager</li>
 * </ul>
 *
 * <p> You can optionally include an <code>android:tag</code> which allows you to use
 * {@link androidx.fragment.app.FragmentManager#findFragmentByTag(java.lang.String) FragmentManager#findFragmentByTag(String)} to retrieve the added Fragment.
 *
 * <pre class="prettyprint">
 * &lt;androidx.fragment.app.FragmentContainerView
 *        xmlns:android="http://schemas.android.com/apk/res/android"
 *        xmlns:app="http://schemas.android.com/apk/res-auto"
 *        android:id="@+id/fragment_container_view"
 *        android:layout_width="match_parent"
 *        android:layout_height="match_parent"
 *        android:name="com.example.MyFragment"
 *        android:tag="my_tag"&gt;
 * &lt;/androidx.fragment.app.FragmentContainerView&gt;
 * </pre>
 *
 * <p>FragmentContainerView should not be used as a replacement for other ViewGroups (FrameLayout,
 * LinearLayout, etc) outside of Fragment use cases.
 *
 * <p>FragmentContainerView will only allow views returned by a Fragment's
 * {@link androidx.fragment.app.Fragment#onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle) Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}. Attempting to add any other
 * view will result in an {@link java.lang.IllegalStateException IllegalStateException}.
 *
 * <p>Layout animations and transitions are disabled for FragmentContainerView for APIs above 17.
 * Otherwise, Animations should be done through
 * {@link androidx.fragment.app.FragmentTransaction#setCustomAnimations(int,int,int,int) FragmentTransaction#setCustomAnimations(int, int, int, int)}. If animateLayoutChanges is
 * set to <code>true</code> or {@link #setLayoutTransition(android.animation.LayoutTransition)} is called directly an
 * {@link java.lang.UnsupportedOperationException UnsupportedOperationException} will be thrown.
 *
 * <p>Fragments using exit animations are drawn before all others for FragmentContainerView. This
 * ensures that exiting Fragments do not appear on top of the view.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class FragmentContainerView extends android.widget.FrameLayout {

public FragmentContainerView(@androidx.annotation.NonNull android.content.Context context) { super((android.content.Context)null); throw new RuntimeException("Stub!"); }

/**
 * Do not call this constructor directly. Doing so will result in an
 * {@link java.lang.UnsupportedOperationException UnsupportedOperationException}.
 */

public FragmentContainerView(@androidx.annotation.NonNull android.content.Context context, @androidx.annotation.Nullable android.util.AttributeSet attrs) { super((android.content.Context)null); throw new RuntimeException("Stub!"); }

/**
 * Do not call this constructor directly. Doing so will result in an
 * {@link java.lang.UnsupportedOperationException UnsupportedOperationException}.
 */

public FragmentContainerView(@androidx.annotation.NonNull android.content.Context context, @androidx.annotation.Nullable android.util.AttributeSet attrs, int defStyleAttr) { super((android.content.Context)null); throw new RuntimeException("Stub!"); }

/**
 * When called, this method throws a {@link java.lang.UnsupportedOperationException UnsupportedOperationException} on APIs above 17.
 * On APIs 17 and below, it calls {@link android.widget.FrameLayout#setLayoutTransition(android.animation.LayoutTransition) FrameLayout#setLayoutTransition(LayoutTransition)}
 * This can be called either explicitly, or implicitly by setting animateLayoutChanges to
 * <code>true</code>.
 *
 * <p>View animations and transitions are disabled for FragmentContainerView for APIs above 17.
 * Use {@link androidx.fragment.app.FragmentTransaction#setCustomAnimations(int,int,int,int) FragmentTransaction#setCustomAnimations(int, int, int, int)} and
 * {@link androidx.fragment.app.FragmentTransaction#setTransition(int) FragmentTransaction#setTransition(int)}.
 *
 * @param transition The LayoutTransition object that will animated changes in layout. A value
 * of <code>null</code> means no transition will run on layout changes.
 * @attr ref android.R.styleable#ViewGroup_animateLayoutChanges
 */

public void setLayoutTransition(@androidx.annotation.Nullable android.animation.LayoutTransition transition) { throw new RuntimeException("Stub!"); }

/**
 * {@inheritDoc}
 *
 * <p>The sys ui flags must be set to enable extending the layout into the window insets.
 * @apiSince 20
 */

@androidx.annotation.NonNull
public android.view.WindowInsets onApplyWindowInsets(@androidx.annotation.NonNull android.view.WindowInsets insets) { throw new RuntimeException("Stub!"); }

protected void dispatchDraw(@androidx.annotation.NonNull android.graphics.Canvas canvas) { throw new RuntimeException("Stub!"); }

protected boolean drawChild(@androidx.annotation.NonNull android.graphics.Canvas canvas, @androidx.annotation.NonNull android.view.View child, long drawingTime) { throw new RuntimeException("Stub!"); }

public void startViewTransition(@androidx.annotation.NonNull android.view.View view) { throw new RuntimeException("Stub!"); }

public void endViewTransition(@androidx.annotation.NonNull android.view.View view) { throw new RuntimeException("Stub!"); }

/**
 * <p>FragmentContainerView will only allow views returned by a Fragment's
 * {@link androidx.fragment.app.Fragment#onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle) Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}. Attempting to add any
 *  other view will result in an {@link java.lang.IllegalStateException IllegalStateException}.
 *
 * {@inheritDoc}
 */

public void addView(@androidx.annotation.NonNull android.view.View child, int index, @androidx.annotation.Nullable android.view.ViewGroup.LayoutParams params) { throw new RuntimeException("Stub!"); }

/**
 * <p>FragmentContainerView will only allow views returned by a Fragment's
 * {@link androidx.fragment.app.Fragment#onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle) Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}. Attempting to add any
 *  other view will result in an {@link java.lang.IllegalStateException IllegalStateException}.
 *
 * {@inheritDoc}
 */

protected boolean addViewInLayout(@androidx.annotation.NonNull android.view.View child, int index, @androidx.annotation.Nullable android.view.ViewGroup.LayoutParams params, boolean preventRequestLayout) { throw new RuntimeException("Stub!"); }

public void removeViewAt(int index) { throw new RuntimeException("Stub!"); }

public void removeViewInLayout(@androidx.annotation.NonNull android.view.View view) { throw new RuntimeException("Stub!"); }

public void removeView(@androidx.annotation.NonNull android.view.View view) { throw new RuntimeException("Stub!"); }

public void removeViews(int start, int count) { throw new RuntimeException("Stub!"); }

public void removeViewsInLayout(int start, int count) { throw new RuntimeException("Stub!"); }

public void removeAllViewsInLayout() { throw new RuntimeException("Stub!"); }

protected void removeDetachedView(@androidx.annotation.NonNull android.view.View child, boolean animate) { throw new RuntimeException("Stub!"); }
}

