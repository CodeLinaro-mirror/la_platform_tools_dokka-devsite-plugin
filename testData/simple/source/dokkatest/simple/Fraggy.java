/*
 * Copyright 2020 The Android Open Source Project
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

package dokkatest.simple;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

class Fraggy {

    /**
     * (this is a real function, vastly simplified from the source in Fragment.java)
     *
     * <p>
     * If the host of this fragment is an {@link List<I>} the
     * {@link Map} of the host will be used. Otherwise, this will use the
     * registry of the Fragment's Activity.
     */
    @NotNull
    public final <I, O> List<I> registerForActivityResult(
        @NotNull final Map<I, O> contract,
        @NotNull final List<O> callback) {
        return new ArrayList<I>();
    }
}
