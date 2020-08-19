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

import androidx.viewpager.widget.PagerAdapter;
import androidx.lifecycle.Lifecycle;

/**
 * Implementation of {@link androidx.viewpager.widget.PagerAdapter PagerAdapter} that
 * represents each page as a {@link androidx.fragment.app.Fragment Fragment} that is persistently
 * kept in the fragment manager as long as the user can return to the page.
 *
 * <p>This version of the pager is best for use when there are a handful of
 * typically more static fragments to be paged through, such as a set of tabs.
 * The fragment of each page the user visits will be kept in memory, though its
 * view hierarchy may be destroyed when not visible.  This can result in using
 * a significant amount of memory since fragment instances can hold on to an
 * arbitrary amount of state.  For larger sets of pages, consider
 * {@link androidx.fragment.app.FragmentStatePagerAdapter FragmentStatePagerAdapter}.
 *
 * <p>When using FragmentPagerAdapter the host ViewPager must have a
 * valid ID set.</p>
 *
 * <p>Subclasses only need to implement {@link #getItem(int)}
 * and {@link #getCount()} to have a working adapter.
 *
 * <p>Here is an example implementation of a pager containing fragments of
 * lists:
 *
 * {@sample frameworks/support/samples/Support4Demos/src/main/java/com/example/android/supportv4/app/FragmentPagerSupport.java
 *      complete}
 *
 * <p>The <code>R.layout.fragment_pager</code> resource of the top-level fragment is:
 *
 * {@sample frameworks/support/samples/Support4Demos/src/main/res/layout/fragment_pager.xml
 *      complete}
 *
 * <p>The <code>R.layout.fragment_pager_list</code> resource containing each
 * individual fragment's layout is:
 *
 * {@sample frameworks/support/samples/Support4Demos/src/main/res/layout/fragment_pager_list.xml
 *      complete}
 *
 * @deprecated Switch to {@link androidx.viewpager2.widget.ViewPager2} and use
 * {@link androidx.viewpager2.adapter.FragmentStateAdapter} instead.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
@Deprecated
public abstract class FragmentPagerAdapter extends androidx.viewpager.widget.PagerAdapter {

/**
 * Constructor for {@link androidx.fragment.app.FragmentPagerAdapter FragmentPagerAdapter} that sets the fragment manager for the adapter.
 * This is the equivalent of calling {@link #FragmentPagerAdapter(androidx.fragment.app.FragmentManager,int)} and
 * passing in {@link #BEHAVIOR_SET_USER_VISIBLE_HINT}.
 *
 * <p>Fragments will have {@link androidx.fragment.app.Fragment#setUserVisibleHint(boolean) Fragment#setUserVisibleHint(boolean)} called whenever the
 * current Fragment changes.</p>
 *
 * @param fm fragment manager that will interact with this adapter
 * @deprecated use {@link #FragmentPagerAdapter(androidx.fragment.app.FragmentManager,int)} with
 * {@link #BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT}
 */

@Deprecated
public FragmentPagerAdapter(androidx.fragment.app.FragmentManager fm) { throw new RuntimeException("Stub!"); }

/**
 * Constructor for {@link androidx.fragment.app.FragmentPagerAdapter FragmentPagerAdapter}.
 *
 * If {@link #BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT} is passed in, then only the current
 * Fragment is in the {@link androidx.lifecycle.Lifecycle.State#RESUMED Lifecycle.State#RESUMED} state. All other fragments are capped at
 * {@link androidx.lifecycle.Lifecycle.State#STARTED Lifecycle.State#STARTED}. If {@link #BEHAVIOR_SET_USER_VISIBLE_HINT} is passed, all
 * fragments are in the {@link androidx.lifecycle.Lifecycle.State#RESUMED Lifecycle.State#RESUMED} state and there will be callbacks to
 * {@link androidx.fragment.app.Fragment#setUserVisibleHint(boolean) Fragment#setUserVisibleHint(boolean)}.
 *
 * @param fm fragment manager that will interact with this adapter
 * @param behavior determines if only current fragments are in a resumed state

 * Value is {@link androidx.fragment.app.FragmentPagerAdapter#BEHAVIOR_SET_USER_VISIBLE_HINT}, or {@link androidx.fragment.app.FragmentPagerAdapter#BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT}
 */

public FragmentPagerAdapter(androidx.fragment.app.FragmentManager fm, int behavior) { throw new RuntimeException("Stub!"); }

/**
 * Return the Fragment associated with a specified position.
 */

public abstract androidx.fragment.app.Fragment getItem(int position);

public void startUpdate(android.view.ViewGroup container) { throw new RuntimeException("Stub!"); }

public java.lang.Object instantiateItem(android.view.ViewGroup container, int position) { throw new RuntimeException("Stub!"); }

public void destroyItem(android.view.ViewGroup container, int position, java.lang.Object object) { throw new RuntimeException("Stub!"); }

public void setPrimaryItem(android.view.ViewGroup container, int position, java.lang.Object object) { throw new RuntimeException("Stub!"); }

public void finishUpdate(android.view.ViewGroup container) { throw new RuntimeException("Stub!"); }

public boolean isViewFromObject(android.view.View view, java.lang.Object object) { throw new RuntimeException("Stub!"); }

public android.os.Parcelable saveState() { throw new RuntimeException("Stub!"); }

public void restoreState(android.os.Parcelable state, java.lang.ClassLoader loader) { throw new RuntimeException("Stub!"); }

/**
 * Return a unique identifier for the item at the given position.
 *
 * <p>The default implementation returns the given position.
 * Subclasses should override this method if the positions of items can change.</p>
 *
 * @param position Position within this adapter
 * @return Unique identifier for the item at position
 */

public long getItemId(int position) { throw new RuntimeException("Stub!"); }

/**
 * Indicates that only the current fragment will be in the {@link androidx.lifecycle.Lifecycle.State#RESUMED Lifecycle.State#RESUMED}
 * state. All other Fragments are capped at {@link androidx.lifecycle.Lifecycle.State#STARTED Lifecycle.State#STARTED}.
 *
 * @see #FragmentPagerAdapter(FragmentManager, int)
 */

public static final int BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT = 1; // 0x1

/**
 * Indicates that {@link androidx.fragment.app.Fragment#setUserVisibleHint(boolean) Fragment#setUserVisibleHint(boolean)} will be called when the current
 * fragment changes.
 *
 * @deprecated This behavior relies on the deprecated
 * {@link androidx.fragment.app.Fragment#setUserVisibleHint(boolean) Fragment#setUserVisibleHint(boolean)} API. Use
 * {@link #BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT} to switch to its replacement,
 * {@link androidx.fragment.app.FragmentTransaction#setMaxLifecycle FragmentTransaction#setMaxLifecycle}.
 * @see #FragmentPagerAdapter(FragmentManager, int)
 */

@Deprecated public static final int BEHAVIOR_SET_USER_VISIBLE_HINT = 0; // 0x0
}

