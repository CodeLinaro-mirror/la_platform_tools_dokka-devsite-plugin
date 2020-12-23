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

import android.widget.ListView;
import android.widget.ListAdapter;

/**
 * Static library support version of the framework's {@link android.app.ListFragment}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework SDK
 * documentation for a class overview.
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class ListFragment extends androidx.fragment.app.Fragment {

public ListFragment() { throw new RuntimeException("Stub!"); }

/**
 * Provide default implementation to return a simple list view.  Subclasses
 * can override to replace with their own layout.  If doing so, the
 * returned view hierarchy <em>must</em> have a ListView whose id
 * is {@link android.R.id#list android.R.id.list} and can optionally
 * have a sibling view id {@link android.R.id#empty android.R.id.empty}
 * that is to be shown when the list is empty.
 *
 * <p>If you are overriding this method with your own custom content,
 * consider including the standard layout {@link android.R.layout#list_content}
 * in your layout file, so that you continue to retain all of the standard
 * behavior of ListFragment.  In particular, this is currently the only
 * way to have the built-in indeterminant progress state be shown.
 */

@androidx.annotation.Nullable
public android.view.View onCreateView(@androidx.annotation.NonNull android.view.LayoutInflater inflater, @androidx.annotation.Nullable android.view.ViewGroup container, @androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Attach to list view once the view hierarchy has been created.
 */

public void onViewCreated(@androidx.annotation.NonNull android.view.View view, @androidx.annotation.Nullable android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Detach from list view.
 */

public void onDestroyView() { throw new RuntimeException("Stub!"); }

/**
 * This method will be called when an item in the list is selected.
 * Subclasses should override. Subclasses can call
 * getListView().getItemAtPosition(position) if they need to access the
 * data associated with the selected item.
 *
 * @param l The ListView where the click happened
 * @param v The view that was clicked within the ListView
 * @param position The position of the view in the list
 * @param id The row id of the item that was clicked
 */

public void onListItemClick(@androidx.annotation.NonNull android.widget.ListView l, @androidx.annotation.NonNull android.view.View v, int position, long id) { throw new RuntimeException("Stub!"); }

/**
 * Provide the cursor for the list view.
 */

public void setListAdapter(@androidx.annotation.Nullable android.widget.ListAdapter adapter) { throw new RuntimeException("Stub!"); }

/**
 * Set the currently selected list item to the specified
 * position with the adapter's data
 *
 * @param position
 */

public void setSelection(int position) { throw new RuntimeException("Stub!"); }

/**
 * Get the position of the currently selected list item.
 */

public int getSelectedItemPosition() { throw new RuntimeException("Stub!"); }

/**
 * Get the cursor row ID of the currently selected list item.
 */

public long getSelectedItemId() { throw new RuntimeException("Stub!"); }

/**
 * Get the fragment's list view widget.
 */

@androidx.annotation.NonNull
public android.widget.ListView getListView() { throw new RuntimeException("Stub!"); }

/**
 * The default content for a ListFragment has a TextView that can
 * be shown when the list is empty.  If you would like to have it
 * shown, call this method to supply the text it should use.
 */

public void setEmptyText(@androidx.annotation.Nullable java.lang.CharSequence text) { throw new RuntimeException("Stub!"); }

/**
 * Control whether the list is being displayed.  You can make it not
 * displayed if you are waiting for the initial data to show in it.  During
 * this time an indeterminant progress indicator will be shown instead.
 *
 * <p>Applications do not normally need to use this themselves.  The default
 * behavior of ListFragment is to start with the list not being shown, only
 * showing it once an adapter is given with {@link #setListAdapter(android.widget.ListAdapter)}.
 * If the list at that point had not been shown, when it does get shown
 * it will be do without the user ever seeing the hidden state.
 *
 * @param shown If true, the list view is shown; if false, the progress
 * indicator.  The initial value is true.
 */

public void setListShown(boolean shown) { throw new RuntimeException("Stub!"); }

/**
 * Like {@link #setListShown(boolean)}, but no animation is used when
 * transitioning from the previous state.
 */

public void setListShownNoAnimation(boolean shown) { throw new RuntimeException("Stub!"); }

/**
 * Get the ListAdapter associated with this fragment's ListView.
 *
 * @see #requireListAdapter()
 */

@androidx.annotation.Nullable
public android.widget.ListAdapter getListAdapter() { throw new RuntimeException("Stub!"); }

/**
 * Get the ListAdapter associated with this fragment's ListView.
 *
 * @throws java.lang.IllegalStateException if no ListAdapter has been set.
 * @see #getListAdapter()
 */

@androidx.annotation.NonNull
public final android.widget.ListAdapter requireListAdapter() { throw new RuntimeException("Stub!"); }
}

