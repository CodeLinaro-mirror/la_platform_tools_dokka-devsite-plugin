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

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import dokkatest.simple.TwoKt;

public class Fraggy {

    /**
     * (this is a real function, vastly simplified from the source in Fragment.java)
     *
     * <p>
     * If the host of this fragment is an {@link List<I>} the
     * {@link Map} of the host will be used. Otherwise, this will use the
     * registry of the Fragment's Activity.
     */
    @NonNull
    public final <I, O> List<I> registerForActivityResult(
        @NonNull final Map<I, O> contract,
        @NonNull final List<O> callback) {
        return new ArrayList<I>();
    }
    public int thisShouldShowUp() {return 0;}
    /** @hide */
    public int thisShouldNotShow() {return 0;}
    /** @suppress */
    public int thisShouldShowUpItsJavaHiddenWithTheKotlinMethod() {return 0;}

    /**
     * linklink
     * @see #registerForActivityResult(Map,List)}
     * @see #createType(Comparable, List)
     * @see Two#z
     * @see TwoKt#copyWhenGreater(List, CharSequence)
     */
    public static <Q extends Comparable<Q> & Map<Q, Q>> Q createType(Q blarg, List<Q> gah){
        return null;
    }
}


