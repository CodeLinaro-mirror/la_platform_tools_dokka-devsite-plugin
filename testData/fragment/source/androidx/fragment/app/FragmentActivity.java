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
import android.view.Window;
import android.view.View;
import java.io.PrintWriter;
import androidx.loader.app.LoaderManager;
import androidx.lifecycle.LifecycleOwner;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import androidx.activity.result.contract.ActivityResultContract;
import android.content.IntentSender;
import androidx.activity.result.ActivityResultCallback;

/**
 * Base class for activities that want to use the support-based
 * {@link androidx.fragment.app.Fragment Fragment}.
 *
 * <p>Known limitations:</p>
 * <ul>
 * <li> <p>When using the <code>&lt;fragment></code> tag, this implementation can not
 * use the parent view's ID as the new fragment's ID.  You must explicitly
 * specify an ID (or tag) in the <code>&lt;fragment></code>.</p>
 * </ul>
 */

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class FragmentActivity extends androidx.activity.ComponentActivity implements androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback, androidx.core.app.ActivityCompat.RequestPermissionsRequestCodeValidator {

/**
 * Default constructor for FragmentActivity. All Activities must have a default constructor
 * for API 27 and lower devices or when using the default
 * {@link android.app.AppComponentFactory}.
 */

public FragmentActivity() { throw new RuntimeException("Stub!"); }

/**
 * Alternate constructor that can be used to provide a default layout
 * that will be inflated as part of <code>super.onCreate(savedInstanceState)</code>.
 *
 * <p>This should generally be called from your constructor that takes no parameters,
 * as is required for API 27 and lower or when using the default
 * {@link android.app.AppComponentFactory}.
 *
 * @see #FragmentActivity()
 */

public FragmentActivity(int contentLayoutId) { throw new RuntimeException("Stub!"); }

protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) { throw new RuntimeException("Stub!"); }

/**
 * Reverses the Activity Scene entry Transition and triggers the calling Activity
 * to reverse its exit Transition. When the exit Transition completes,
 * {@link #finish()} is called. If no entry Transition was used, finish() is called
 * immediately and the Activity exit Transition is run.
 *
 * <p>On Android 4.4 or lower, this method only finishes the Activity with no
 * special exit transition.</p>
 */

public void supportFinishAfterTransition() { throw new RuntimeException("Stub!"); }

/**
 * When {@link android.app.ActivityOptions#makeSceneTransitionAnimation(Activity,
 * android.view.View, String)} was used to start an Activity, <var>callback</var>
 * will be called to handle shared elements on the <i>launched</i> Activity. This requires
 * {@link android.view.Window#FEATURE_CONTENT_TRANSITIONS Window#FEATURE_CONTENT_TRANSITIONS}.
 *
 * @param callback Used to manipulate shared element transitions on the launched Activity.
 */

public void setEnterSharedElementCallback(androidx.core.app.SharedElementCallback callback) { throw new RuntimeException("Stub!"); }

/**
 * When {@link android.app.ActivityOptions#makeSceneTransitionAnimation(Activity,
 * android.view.View, String)} was used to start an Activity, <var>listener</var>
 * will be called to handle shared elements on the <i>launching</i> Activity. Most
 * calls will only come when returning from the started Activity.
 * This requires {@link android.view.Window#FEATURE_CONTENT_TRANSITIONS Window#FEATURE_CONTENT_TRANSITIONS}.
 *
 * @param listener Used to manipulate shared element transitions on the launching Activity.
 */

public void setExitSharedElementCallback(androidx.core.app.SharedElementCallback listener) { throw new RuntimeException("Stub!"); }

/**
 * Support library version of {@link android.app.Activity#postponeEnterTransition()} that works
 * only on API 21 and later.
 */

public void supportPostponeEnterTransition() { throw new RuntimeException("Stub!"); }

/**
 * Support library version of {@link android.app.Activity#startPostponedEnterTransition()}
 * that only works with API 21 and later.
 */

public void supportStartPostponedEnterTransition() { throw new RuntimeException("Stub!"); }

/**
 * {@inheritDoc}
 *
 * <p><strong>Note:</strong> If you override this method you must call
 * <code>super.onMultiWindowModeChanged</code> to correctly dispatch the event
 * to support fragments attached to this activity.</p>
 *
 * @param isInMultiWindowMode True if the activity is in multi-window mode.
 */

public void onMultiWindowModeChanged(boolean isInMultiWindowMode) { throw new RuntimeException("Stub!"); }

/**
 * {@inheritDoc}
 *
 * <p><strong>Note:</strong> If you override this method you must call
 * <code>super.onPictureInPictureModeChanged</code> to correctly dispatch the event
 * to support fragments attached to this activity.</p>
 *
 * @param isInPictureInPictureMode True if the activity is in picture-in-picture mode.
 */

public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) { throw new RuntimeException("Stub!"); }

/**
 * Dispatch configuration change to all fragments.
 */

public void onConfigurationChanged(android.content.res.Configuration newConfig) { throw new RuntimeException("Stub!"); }

/**
 * Perform initialization of all fragments.
 */

protected void onCreate(android.os.Bundle savedInstanceState) { throw new RuntimeException("Stub!"); }

/**
 * Dispatch to Fragment.onCreateOptionsMenu().
 */

public boolean onCreatePanelMenu(int featureId, android.view.Menu menu) { throw new RuntimeException("Stub!"); }

public android.view.View onCreateView(android.view.View parent, java.lang.String name, android.content.Context context, android.util.AttributeSet attrs) { throw new RuntimeException("Stub!"); }

public android.view.View onCreateView(java.lang.String name, android.content.Context context, android.util.AttributeSet attrs) { throw new RuntimeException("Stub!"); }

/**
 * Destroy all fragments.
 */

protected void onDestroy() { throw new RuntimeException("Stub!"); }

/**
 * Dispatch onLowMemory() to all fragments.
 */

public void onLowMemory() { throw new RuntimeException("Stub!"); }

/**
 * Dispatch context and options menu to fragments.
 */

public boolean onMenuItemSelected(int featureId, android.view.MenuItem item) { throw new RuntimeException("Stub!"); }

/**
 * Call onOptionsMenuClosed() on fragments.
 */

public void onPanelClosed(int featureId, android.view.Menu menu) { throw new RuntimeException("Stub!"); }

/**
 * Dispatch onPause() to fragments.
 */

protected void onPause() { throw new RuntimeException("Stub!"); }

/**
 * Handle onNewIntent() to inform the fragment manager that the
 * state is not saved.  If you are handling new intents and may be
 * making changes to the fragment state, you want to be sure to call
 * through to the super-class here first.  Otherwise, if your state
 * is saved but the activity is not stopped, you could get an
 * onNewIntent() call which happens before onResume() and trying to
 * perform fragment operations at that point will throw IllegalStateException
 * because the fragment manager thinks the state is still saved.
 */

protected void onNewIntent(android.content.Intent intent) { throw new RuntimeException("Stub!"); }

/**
 * Hook in to note that fragment state is no longer saved.
 */

public void onStateNotSaved() { throw new RuntimeException("Stub!"); }

/**
 * Dispatch onResume() to fragments.  Note that for better inter-operation
 * with older versions of the platform, at the point of this call the
 * fragments attached to the activity are <em>not</em> resumed.
 */

protected void onResume() { throw new RuntimeException("Stub!"); }

/**
 * Dispatch onResume() to fragments.
 */

protected void onPostResume() { throw new RuntimeException("Stub!"); }

/**
 * This is the fragment-orientated version of {@link #onResume()} that you
 * can override to perform operations in the Activity at the same point
 * where its fragments are resumed.  Be sure to always call through to
 * the super-class.
 */

protected void onResumeFragments() { throw new RuntimeException("Stub!"); }

/**
 * Dispatch onPrepareOptionsMenu() to fragments.
 */

public boolean onPreparePanel(int featureId, android.view.View view, android.view.Menu menu) { throw new RuntimeException("Stub!"); }

/**
 * Dispatch onStart() to all fragments.
 */

protected void onStart() { throw new RuntimeException("Stub!"); }

/**
 * Dispatch onStop() to all fragments.
 */

protected void onStop() { throw new RuntimeException("Stub!"); }

/**
 * Support library version of {@link android.app.Activity#invalidateOptionsMenu Activity#invalidateOptionsMenu}.
 *
 * <p>Invalidate the activity's options menu. This will cause relevant presentations
 * of the menu to fully update via calls to onCreateOptionsMenu and
 * onPrepareOptionsMenu the next time the menu is requested.
 *
 * @deprecated Call {@link android.app.Activity#invalidateOptionsMenu Activity#invalidateOptionsMenu} directly.
 */

@Deprecated
public void supportInvalidateOptionsMenu() { throw new RuntimeException("Stub!"); }

/**
 * Print the Activity's state into the given stream.  This gets invoked if
 * you run "adb shell dumpsys activity <activity_component_name>".
 *
 * @param prefix Desired prefix to prepend at each line of output.
 * @param fd The raw file descriptor that the dump is being sent to.
 * @param writer The PrintWriter to which you should dump your state.  This will be
 * closed for you after you return.
 * @param args additional arguments to the dump request.
 */

public void dump(java.lang.String prefix, java.io.FileDescriptor fd, java.io.PrintWriter writer, java.lang.String[] args) { throw new RuntimeException("Stub!"); }

/**
 * Called when a fragment is attached to the activity.
 *
 * <p>This is called after the attached fragment's <code>onAttach</code> and before
 * the attached fragment's <code>onCreate</code> if the fragment has not yet had a previous
 * call to <code>onCreate</code>.</p>
 *
 * @deprecated The responsibility for listening for fragments being attached has been moved
 * to FragmentManager. You can add a listener to
 * {@link #getSupportFragmentManager() this Activity's FragmentManager} by calling
 * {@link androidx.fragment.app.FragmentManager#addFragmentOnAttachListener(androidx.fragment.app.FragmentOnAttachListener) FragmentManager#addFragmentOnAttachListener(FragmentOnAttachListener)}
 * in your constructor to get callbacks when a fragment is attached directly to
 * the activity's FragmentManager.
 */

@Deprecated
public void onAttachFragment(androidx.fragment.app.Fragment fragment) { throw new RuntimeException("Stub!"); }

/**
 * Return the FragmentManager for interacting with fragments associated
 * with this activity.
 */

public androidx.fragment.app.FragmentManager getSupportFragmentManager() { throw new RuntimeException("Stub!"); }

/**
 * @deprecated Use
 * {@link androidx.loader.app.LoaderManager#getInstance(androidx.lifecycle.LifecycleOwner) LoaderManager#getInstance(LifecycleOwner)}.
 */

@Deprecated
public androidx.loader.app.LoaderManager getSupportLoaderManager() { throw new RuntimeException("Stub!"); }

/**
 * @deprecated there are no longer any restrictions on permissions requestCodes.
 */

@Deprecated
public final void validateRequestPermissionsRequestCode(int requestCode) { throw new RuntimeException("Stub!"); }

public void onRequestPermissionsResult(int requestCode, java.lang.String[] permissions, int[] grantResults) { throw new RuntimeException("Stub!"); }

/**
 * Called by Fragment.startActivityForResult() to implement its behavior.
 *
 * @param fragment the Fragment to start the activity from.
 * @param intent The intent to start.
 * @param requestCode The request code to be returned in
 * {@link androidx.fragment.app.Fragment#onActivityResult(int,int,android.content.Intent) Fragment#onActivityResult(int, int, Intent)} when the activity exits. Must be
 *                    between 0 and 65535 to be considered valid. If given requestCode is
 *                    greater than 65535, an IllegalArgumentException would be thrown.
 */

public void startActivityFromFragment(androidx.fragment.app.Fragment fragment, android.content.Intent intent, int requestCode) { throw new RuntimeException("Stub!"); }

/**
 * Called by Fragment.startActivityForResult() to implement its behavior.
 *
 * @param fragment the Fragment to start the activity from.
 * @param intent The intent to start.
 * @param requestCode The request code to be returned in
 * {@link androidx.fragment.app.Fragment#onActivityResult(int,int,android.content.Intent) Fragment#onActivityResult(int, int, Intent)} when the activity exits. Must be
 *                    between 0 and 65535 to be considered valid. If given requestCode is
 *                    greater than 65535, an IllegalArgumentException would be thrown.
 * @param options Additional options for how the Activity should be started. See
 * {@link android.content.Context#startActivity(android.content.Intent,android.os.Bundle) Context#startActivity(Intent, Bundle)} for more details. This value may be null.
 */

public void startActivityFromFragment(androidx.fragment.app.Fragment fragment, android.content.Intent intent, int requestCode, android.os.Bundle options) { throw new RuntimeException("Stub!"); }

/**
 * Called by Fragment.startIntentSenderForResult() to implement its behavior.
 *
 * @param fragment the Fragment to start the intent sender from.
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
 * @throws android.content.IntentSender.SendIntentException if the call fails to execute.
 *
 * @deprecated Fragments should use
 * {@link androidx.fragment.app.Fragment#registerForActivityResult(androidx.activity.result.contract.ActivityResultContract,androidx.activity.result.ActivityResultCallback) Fragment#registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
 * with the {@link androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult StartIntentSenderForResult} contract. This method will still be called when
 * Fragments call the deprecated <code>startIntentSenderForResult()</code> method.
 */

@Deprecated
public void startIntentSenderFromFragment(androidx.fragment.app.Fragment fragment, android.content.IntentSender intent, int requestCode, android.content.Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, android.os.Bundle options) throws android.content.IntentSender.SendIntentException { throw new RuntimeException("Stub!"); }
}

