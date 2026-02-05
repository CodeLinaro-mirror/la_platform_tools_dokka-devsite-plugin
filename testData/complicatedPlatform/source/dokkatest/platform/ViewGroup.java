/*
 * Copyright 2026 The Android Open Source Project
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

package dokkatest.platform;

import java.lang.annotation.*;
import dokkatest.platform.ViewDebug;
import dokkatest.platform.InspectableProperty;
import dokkatest.platform.InspectableProperty.EnumEntry;

public class ViewGroup {
    /**
     * Gets the descendant focusability of this view group.  The descendant
     * focusability defines the relationship between this view group and its
     * descendants when looking for a view to take focus in
     * {@link #requestFocus(int, android.graphics.Rect)}.
     *
     * @return one of {@link #FOCUS_BEFORE_DESCENDANTS}, {@link #FOCUS_AFTER_DESCENDANTS},
     *   {@link #FOCUS_BLOCK_DESCENDANTS}.
     */
    @ViewDebug.ExportedProperty(category = "focus", mapping = {
            @ViewDebug.IntToString(from = FOCUS_BEFORE_DESCENDANTS, to = "FOCUS_BEFORE_DESCENDANTS"),
            @ViewDebug.IntToString(from = FOCUS_AFTER_DESCENDANTS, to = "FOCUS_AFTER_DESCENDANTS"),
            @ViewDebug.IntToString(from = FOCUS_BLOCK_DESCENDANTS, to = "FOCUS_BLOCK_DESCENDANTS")
    })
    @InspectableProperty(enumMapping = {
            @EnumEntry(value = FOCUS_BEFORE_DESCENDANTS, name = "beforeDescendants"),
            @EnumEntry(value = FOCUS_AFTER_DESCENDANTS, name = "afterDescendants"),
            @EnumEntry(value = FOCUS_BLOCK_DESCENDANTS, name = "blocksDescendants")
    })
    public int getDescendantFocusability() {
        return 0;
    }

    /**
     * This view will get focus before any of its descendants.
     */
    public static final int FOCUS_BEFORE_DESCENDANTS = 0x20000;

    /**
     * This view will get focus only if none of its descendants want it.
     */
    public static final int FOCUS_AFTER_DESCENDANTS = 0x40000;

    /**
     * This view will block any of its descendants from getting focus, even
     * if they are focusable.
     */
    public static final int FOCUS_BLOCK_DESCENDANTS = 0x60000;
}
