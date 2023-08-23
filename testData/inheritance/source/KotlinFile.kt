/*
 * Copyright 2021 The Android Open Source Project
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

package dokkatest.inheritance

import com.google.common.truth.Ordered
import kotlin.collections.MutableMap

/** For verifying that methods can be inherited from externally-defined interfaces. */
interface KotlinInterface: Comparable<KotlinInterface>, Ordered {
    fun aDefaultMethod() = 5
    fun aNonImplementedMethod(): Int
}

/** For verifying that methods can be inherited from internally-defined interfaces. */
abstract class KotlinAbstractClass: KotlinInterface {
    fun aNonAbstractMethod() = 5
    abstract fun anAbstractMethod(): Int
}

open class KotlinSuperClass: KotlinAbstractClass() {
    /**
     * kotlinSuperClassFunction docs
     * @param foo KotlinSuperClassFooDocs
     */
    fun kotlinSuperclassFunction(foo: String) = "KotlinSuperClass"
    fun importTest(): JavaSuperClass? = null

    override fun aNonImplementedMethod() = -5
    override fun compareTo(other: KotlinInterface): Int {
        TODO("Not yet implemented")
    }

    override fun inOrder() {
        TODO("Not yet implemented")
    }

    override fun anAbstractMethod() = -5
}

open class KotlinSubClass : JavaSuperClass() {
    /**
     * kotlinSubClassFunction docs
     * @param bar KotlinSubClassBarDocs
     */
    fun kotlinSubclassFunction(bar: String) = "KotlinSubClass"
}

class KotlinLeafClass : JavaSubClass() {
    /**
     * kotlinSuperLeafFunction docs
     * @param baz KotlinLeafClassBazDocs
     */
    fun kotlinLeafClassFunction(baz: String) = "KotlinLeafClass"
}

/** For testing if externally-defined interface default method remove(key, value) is inherited. */
class KotlinMap(override val size: Int,
                override val entries: MutableSet<MutableMap.MutableEntry<String, String>>,
                override val keys: MutableSet<String>,
                override val values: MutableCollection<String>
) : MutableMap<String, String> {
    override fun containsKey(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: String): String? {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun put(key: String, value: String): String? {
        TODO("Not yet implemented")
    }

    override fun putAll(from: Map<out String, String>) {
        TODO("Not yet implemented")
    }

    override fun remove(key: String): String? {
        TODO("Not yet implemented")
    }

}
