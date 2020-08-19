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

/**
 * Special TabHost that allows the use of {@link androidx.fragment.app.Fragment Fragment} objects for
 * its tab content.  When placing this in a view hierarchy, after inflating
 * the hierarchy you must call {@link #setup(android.content.Context,androidx.fragment.app.FragmentManager,int)}
 * to complete the initialization of the tab host.
 *
 * @deprecated Use <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
@Deprecated
public class FragmentTabHost extends android.widget.TabHost implements android.widget.TabHost.OnTabChangeListener {

/**
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
public FragmentTabHost(android.content.Context context) { super((android.content.Context)null); throw new RuntimeException("Stub!"); }

/**
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
public FragmentTabHost(android.content.Context context, android.util.AttributeSet attrs) { super((android.content.Context)null); throw new RuntimeException("Stub!"); }

/**
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
public void setup() { throw new RuntimeException("Stub!"); }

/**
 * Set up the FragmentTabHost to use the given FragmentManager
 *
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
public void setup(android.content.Context context, androidx.fragment.app.FragmentManager manager) { throw new RuntimeException("Stub!"); }

/**
 * Set up the FragmentTabHost to use the given FragmentManager
 *
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
public void setup(android.content.Context context, androidx.fragment.app.FragmentManager manager, int containerId) { throw new RuntimeException("Stub!"); }

/**
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
public void setOnTabChangedListener(android.widget.TabHost.OnTabChangeListener l) { throw new RuntimeException("Stub!"); }

/**
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
public void addTab(android.widget.TabHost.TabSpec tabSpec, java.lang.Class<?> clss, android.os.Bundle args) { throw new RuntimeException("Stub!"); }

/**
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
protected void onAttachedToWindow() { throw new RuntimeException("Stub!"); }

/**
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
protected void onDetachedFromWindow() { throw new RuntimeException("Stub!"); }

/**
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
protected android.os.Parcelable onSaveInstanceState() { throw new RuntimeException("Stub!"); }

/**
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
protected void onRestoreInstanceState(android.os.Parcelable state) { throw new RuntimeException("Stub!"); }

/**
 * @deprecated Use
 * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
 *  TabLayout and ViewPager</a> instead.
 */

@Deprecated
public void onTabChanged(java.lang.String tabId) { throw new RuntimeException("Stub!"); }
}

