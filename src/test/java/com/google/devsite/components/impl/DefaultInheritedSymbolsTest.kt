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

package com.google.devsite.components.impl

import com.google.common.truth.Truth
import com.google.devsite.components.Link
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.InheritedSymbolsList
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableTitle
import com.google.devsite.components.table.TwoPaneSummaryItem
import com.google.devsite.components.testing.NoopLink
import com.google.devsite.components.testing.NoopTwoPaneTypeSummaryItem
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import org.junit.Test

class DefaultInheritedSymbolsTest {

    @Test
    fun `Inherited symbols table renders correctly `() {
        val inheritedSymbols = HashMap<Link,
            SummaryList<TwoPaneSummaryItem<TypeSummary, SymbolSummary>>>()
        inheritedSymbols[NoopLink("aClass")] = DefaultSummaryList(
            SummaryList.Params(
                items = listOf(NoopTwoPaneTypeSummaryItem, NoopTwoPaneTypeSummaryItem)
            )
        )

        val component = DefaultInheritedSymbols(

            InheritedSymbolsList.Params(
                header = DefaultTableTitle(
                    TableTitle.Params("Inherited Methods", big = true)
                ),
                inheritedSymbolSummaries = inheritedSymbols
            )

        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        Truth.assertThat(output).isEqualTo(
            """
                <body>
                  <div class="devsite-table-wrapper">
                    <table class="responsive" id="inhmethods">
                      <thead>
                        <tr>
                          <th colspan="100%"><h3>Inherited Methods</h3></th>
                        </tr>
                      </thead>
                      <tbody class="list">
                        <tr>
                          <td><devsite-expandable><span class="expand-control">From class aClass</span>
                            <div class="devsite-table-wrapper">
                              <table class="responsive">
                                <tbody class="list">
                                  <tr><noop/></tr>
                                  <tr><noop/></tr>
                                </tbody>
                              </table>
                            </div>
                </devsite-expandable>          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </body>
            """.trimIndent()
        )
    }

    @Test
    fun `Empty inherited symbols table renders correctly `() {

        val component = DefaultInheritedSymbols(
            InheritedSymbolsList.Params(
                header = DefaultTableTitle(
                    TableTitle.Params("Inherited Methods", big = true)
                ),
                inheritedSymbolSummaries = HashMap()
            )

        )

        val output = createHTML().body {
            component.render(this)
        }.trim()

        // language=html
        Truth.assertThat(output).isEqualTo(
            """
                <body></body>
            """.trimIndent()
        )
    }
}
