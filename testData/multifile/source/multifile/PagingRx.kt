/*
 * Copyright 2022 The Android Open Source Project
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

@file:JvmName("PagingRx")
@file:JvmMultifileClass
@file:Suppress("NotARealLintIssue")

package multifile

import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.asObservable

/**
 * An [Observable] of [PagingData], which mirrors the stream provided by [Pager.flow], but exposes
 * it as an [Observable].
 */
// Both annotations are needed here see: https://youtrack.jetbrains.com/issue/KT-45227
@ExperimentalCoroutinesApi
val <Key : Any, Value : Any> Pager<Key, Value>.observable: Observable<PagingData<Value>>
    get() = flow
        .conflate()
        .asObservable()
