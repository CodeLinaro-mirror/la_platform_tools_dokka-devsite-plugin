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

package dokkatest.sampleAnnotation.dokkatest.samples

import java.lang.RuntimeException

/**
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently selected
 * tab. A TabRow places its tabs evenly spaced along the entire row, with each tab taking up an
 * equal amount of space. See [ScrollableTabRow] for a tab row that does not enforce equal size, and
 * allows scrolling to tabs that do not fit on screen.
 *
 * A simple example with text tabs looks like:
 *
 * @sample androidx.compose.material.samples.TextTabs
 *
 * You can also provide your own custom tab, such as:
 *
 * @sample androidx.compose.material.samples.FancyTabs
 *
 * Where the custom tab itself could look like:
 *
 * @sample androidx.compose.material.samples.FancyTab
 *
 * As well as customizing the tab, you can also provide a custom [indicator], to customize the
 * indicator displayed for a tab. [indicator] will be placed to fill the entire TabRow, so it should
 * internally take care of sizing and positioning the indicator to match changes to
 * [selectedTabIndex].
 *
 * For example, given an indicator that draws a rounded rectangle near the edges of the [Tab]:
 *
 * @sample androidx.compose.material.samples.FancyIndicator
 *
 * We can reuse [TabRowDefaults.tabIndicatorOffset] and just provide this indicator, as we aren't
 * changing how the size and position of the indicator changes between tabs:
 *
 * @sample androidx.compose.material.samples.FancyIndicatorTabs
 *
 * You may also want to use a custom transition, to allow you to dynamically change the appearance
 * of the indicator as it animates between tabs, such as changing its color or size. [indicator] is
 * stacked on top of the entire TabRow, so you just need to provide a custom transition that
 * animates the offset of the indicator from the start of the TabRow. For example, take the
 * following example that uses a transition to animate the offset, width, and color of the same
 * FancyIndicator from before, also adding a physics based 'spring' effect to the indicator in the
 * direction of motion:
 *
 * @sample androidx.compose.material.samples.FancyAnimatedIndicator
 *
 * We can now just pass this indicator directly to TabRow:
 *
 * @sample androidx.compose.material.samples.FancyIndicatorContainerTabs
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier optional [Modifier] for this TabRow
 * @param backgroundColor The background color for the TabRow. Use [Color.Transparent] to have no
 *   color.
 * @param contentColor The preferred content color provided by this TabRow to its children. Defaults
 *   to either the matching content color for [backgroundColor], or if [backgroundColor] is not a
 *   color from the theme, this will keep the same value set above this TabRow.
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.Indicator], using a [TabRowDefaults.tabIndicatorOffset] modifier to
 *   animate its position. Note that this indicator will be forced to fill up the entire TabRow, so
 *   you should use [TabRowDefaults.tabIndicatorOffset] or similar to animate the actual drawn
 *   indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the TabRow. This provides a layer of
 *   separation between the TabRow and the content displayed underneath.
 * @param tabs the tabs inside this TabRow. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the TabRow, each taking up equal
 *   space.
 */
fun TabRow(
    selectedTabIndex: Int,
    modifier: String = "Modifier",
    backgroundColor: String = "MaterialTheme.colors.primarySurface",
    contentColor: String = "contentColorFor(backgroundColor)",
    indicator: (tabPositions: List<String>) -> Unit = { tabPositions ->
        listOf(listOf(tabPositions[selectedTabIndex]))
    },
    divider: () -> Unit = { listOf<Unit>() },
    tabs: () -> Unit,
) {
    throw RuntimeException(
        "Very unimplemented stub; $modifier, $backgroundColor, $contentColor," +
            indicator.toString() +
            divider.toString() +
            tabs.toString()
    )
}
