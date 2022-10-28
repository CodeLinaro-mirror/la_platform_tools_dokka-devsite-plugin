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

package com.google.devsite.components.testing

import com.google.devsite.TypeSummaryItem
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.Link
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.TableRowSummaryItem
import kotlinx.html.TR
import kotlinx.html.unsafe

internal object NoopTableRowTypeSummaryItem : TableRowSummaryItem<TypeSummary, SymbolSummary<*>> {
    override val data: TableRowSummaryItem.Params<TypeSummary, SymbolSummary<*>>
        get() = throw NotImplementedError()

    override fun render(into: TR) = into.run {
        unsafe { +"<noop/>" }
    }
}

@Suppress("UNCHECKED_CAST")
internal val NoopTableRowTypeSummaryItemF =
    NoopTableRowTypeSummaryItem as TypeSummaryItem<FunctionSignature>
@Suppress("UNCHECKED_CAST")
internal val NoopTableRowTypeSummaryItemLD =
    NoopTableRowTypeSummaryItem as TableRowSummaryItem<Link, DescriptionComponent>
