/*
 * Copyright (c) 2026 Composable Horizons
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.composeunstyled

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import kotlin.math.roundToInt
import kotlin.test.Test

class DrawerTest {

  @Test
  fun defaultStateStartsClosed() = runComposeUiTest {
    setContent {
      DefaultDrawerLayout()
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(viewportBounds.width.roundToInt()).isEqualTo(100)
    assertThat(viewportBounds.height.roundToInt()).isEqualTo(100)
    assertThat(panelBounds.top.roundToInt()).isEqualTo(
      viewportBounds.bottom.roundToInt() + panelBounds.height.roundToInt(),
    )
    assertThat(panelBounds.height.roundToInt()).isEqualTo(100)
  }

  @Test
  fun openSnapPointUsesPanelSize() = runComposeUiTest {
    setContent {
      DrawerLayout(
        initialSnapPoint = DrawerSnapPoint.Open,
        contentHeight = 60,
      )
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(40)
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(panelBounds.height.roundToInt()).isEqualTo(60)
  }

  @Test
  fun openSnapPointCapsVisibleSizeToTheViewport() = runComposeUiTest {
    setContent {
      DrawerLayout(
        initialSnapPoint = DrawerSnapPoint.Open,
        contentHeight = 160,
      )
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(0)
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(panelBounds.height.roundToInt()).isEqualTo(viewportBounds.height.roundToInt())
  }

  @Test
  fun startOpenSnapPointUsesPanelSize() = runComposeUiTest {
    setContent {
      StartDrawerLayout(
        initialSnapPoint = DrawerSnapPoint.Open,
        contentWidth = 60,
      )
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(viewportBounds.left.roundToInt())
    assertThat(panelBounds.right.roundToInt()).isEqualTo(60)
    assertThat(panelBounds.width.roundToInt()).isEqualTo(60)
    assertThat(panelBounds.height.roundToInt()).isEqualTo(viewportBounds.height.roundToInt())
  }

  @Test
  fun startStateStartsClosed() = runComposeUiTest {
    setContent {
      StartDrawerLayout()
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.right.roundToInt()).isEqualTo(viewportBounds.left.roundToInt())
    assertThat(panelBounds.width.roundToInt()).isEqualTo(100)
  }

  @Test
  fun startClosedContentIsNotDisplayed() = runComposeUiTest {
    setContent {
      StartDrawerLayout()
    }

    waitForIdle()

    onNodeWithTag("content").assertIsNotDisplayed()
  }

  @Test
  fun startPeekSnapPointUsesViewportWidth() = runComposeUiTest {
    setContent {
      StartDrawerLayout(
        initialSnapPoint = Peek,
        snapPoints = listOf(DrawerSnapPoint.Closed, Peek),
        contentWidth = 160,
      )
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(0)
    assertThat(panelBounds.right.roundToInt()).isEqualTo(120)
    assertThat(panelBounds.width.roundToInt()).isEqualTo(120)
    assertThat(panelBounds.height.roundToInt()).isEqualTo(viewportBounds.height.roundToInt())
  }

  @Test
  fun endOpenSnapPointUsesPanelSize() = runComposeUiTest {
    setContent {
      EdgeDrawerLayout(
        side = DrawerSide.End,
        initialSnapPoint = DrawerSnapPoint.Open,
        contentWidth = 60,
      )
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(
      viewportBounds.right.roundToInt() - 60,
    )
    assertThat(panelBounds.right.roundToInt()).isEqualTo(viewportBounds.right.roundToInt())
    assertThat(panelBounds.width.roundToInt()).isEqualTo(60)
    assertThat(panelBounds.height.roundToInt()).isEqualTo(viewportBounds.height.roundToInt())
  }

  @Test
  fun topOpenSnapPointUsesPanelSize() = runComposeUiTest {
    setContent {
      EdgeDrawerLayout(
        side = DrawerSide.Top,
        initialSnapPoint = DrawerSnapPoint.Open,
        contentHeight = 60,
      )
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(viewportBounds.top.roundToInt())
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(60)
    assertThat(panelBounds.width.roundToInt()).isEqualTo(viewportBounds.width.roundToInt())
    assertThat(panelBounds.height.roundToInt()).isEqualTo(60)
  }

  @Test
  fun startPositionUsesLeftEdgeInLtr() = runComposeUiTest {
    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        EdgeDrawerLayout(
          side = DrawerSide.Start,
          initialSnapPoint = DrawerSnapPoint.Open,
          contentWidth = 60,
        )
      }
    }

    waitForIdle()

    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(0)
    assertThat(panelBounds.right.roundToInt()).isEqualTo(60)
  }

  @Test
  fun startPositionUsesRightEdgeInRtl() = runComposeUiTest {
    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        EdgeDrawerLayout(
          side = DrawerSide.Start,
          initialSnapPoint = DrawerSnapPoint.Open,
          contentWidth = 60,
        )
      }
    }

    waitForIdle()

    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(140)
    assertThat(panelBounds.right.roundToInt()).isEqualTo(200)
  }

  @Test
  fun endPositionUsesRightEdgeInLtr() = runComposeUiTest {
    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        EdgeDrawerLayout(
          side = DrawerSide.End,
          initialSnapPoint = DrawerSnapPoint.Open,
          contentWidth = 60,
        )
      }
    }

    waitForIdle()

    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(140)
    assertThat(panelBounds.right.roundToInt()).isEqualTo(200)
  }

  @Test
  fun endPositionUsesLeftEdgeInRtl() = runComposeUiTest {
    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        EdgeDrawerLayout(
          side = DrawerSide.End,
          initialSnapPoint = DrawerSnapPoint.Open,
          contentWidth = 60,
        )
      }
    }

    waitForIdle()

    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(0)
    assertThat(panelBounds.right.roundToInt()).isEqualTo(60)
  }

  @Test
  fun startDrawerClosesTowardLeftInLtr() = runComposeUiTest {
    lateinit var state: DrawerState

    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        StartDrawerLayout(
          initialSnapPoint = DrawerSnapPoint.Open,
          onState = { state = it },
        )
      }
    }

    waitForIdle()

    onNodeWithTag("panel").performTouchInput {
      swipe(start = centerRight, end = centerLeft)
    }
    waitForIdle()

    assertThat(state.currentSnapPoint).isEqualTo(DrawerSnapPoint.Closed)
  }

  @Test
  fun startDrawerMovesImmediatelyWhenContentIsWiderThanPanelConstraints() = runComposeUiTest {
    lateinit var state: DrawerState

    setContent {
      ConstrainedStartDrawerLayout(
        initialSnapPoint = DrawerSnapPoint.Open,
        onState = { state = it },
      )
    }

    waitForIdle()

    assertThat(onNodeWithTag("panel").boundsInRoot().width.roundToInt()).isEqualTo(100)

    runOnIdle {
      state.anchoredDraggableState.dispatchRawDelta(-50f)
    }
    waitForIdle()

    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(-50)
    assertThat(panelBounds.right.roundToInt()).isEqualTo(50)
    assertThat(panelBounds.width.roundToInt()).isEqualTo(100)
  }

  @Test
  fun startDrawerClosesTowardRightInRtl() = runComposeUiTest {
    lateinit var state: DrawerState

    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        StartDrawerLayout(
          initialSnapPoint = DrawerSnapPoint.Open,
          onState = { state = it },
        )
      }
    }

    waitForIdle()

    onNodeWithTag("panel").performTouchInput {
      swipe(start = centerLeft, end = centerRight)
    }
    waitForIdle()

    assertThat(state.currentSnapPoint).isEqualTo(DrawerSnapPoint.Closed)
  }

  @Test
  fun endDrawerClosesTowardRightInLtr() = runComposeUiTest {
    lateinit var state: DrawerState

    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        EdgeDrawerLayout(
          side = DrawerSide.End,
          initialSnapPoint = DrawerSnapPoint.Open,
          onState = { state = it },
        )
      }
    }

    waitForIdle()

    onNodeWithTag("panel").performTouchInput {
      swipe(start = centerLeft, end = centerRight)
    }
    waitForIdle()

    assertThat(state.currentSnapPoint).isEqualTo(DrawerSnapPoint.Closed)
  }

  @Test
  fun endDrawerIsPlacedAtRightEdgeWithConstrainedPanelAndOversizedContent() = runComposeUiTest {
    setContent {
      ConstrainedEndDrawerLayout()
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(
      viewportBounds.right.roundToInt() - 100,
    )
    assertThat(panelBounds.right.roundToInt()).isEqualTo(viewportBounds.right.roundToInt())
    assertThat(panelBounds.width.roundToInt()).isEqualTo(100)
  }

  @Test
  fun startDrawerContentCanFillExplicitPanelWidth() = runComposeUiTest {
    setContent {
      FixedWidthStartDrawerLayout(initialSnapPoint = DrawerSnapPoint.Open)
    }

    waitForIdle()

    assertThat(onNodeWithTag("panel").boundsInRoot().width.roundToInt()).isEqualTo(100)
    assertThat(onNodeWithTag("content").boundsInRoot().width.roundToInt()).isEqualTo(100)
  }

  @Test
  fun startDrawerWithExplicitPanelWidthHidesContentWhenClosed() = runComposeUiTest {
    setContent {
      FixedWidthStartDrawerLayout(initialSnapPoint = DrawerSnapPoint.Closed)
    }

    waitForIdle()

    onNodeWithTag("content").assertIsNotDisplayed()
  }

  @Test
  fun endDrawerWithFullSizePanelIsFullyPastViewportWhenClosed() = runComposeUiTest {
    setContent {
      val state = rememberDrawerState(
        initialSnapPoint = DrawerSnapPoint.Closed,
      )
      UnstyledDrawer(
        state = state,
        side = DrawerSide.End,
        modifier = Modifier.size(100.dp),
      ) {
        Viewport(
          modifier = Modifier
            .size(100.dp)
            .testTag("viewport"),
        ) {
          Panel(
            modifier = Modifier
              .fillMaxSize()
              .testTag("panel"),
          ) {
            Box(Modifier.size(20.dp))
          }
        }
      }
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(
      viewportBounds.right.roundToInt() + panelBounds.width.roundToInt(),
    )
  }

  @Test
  fun bottomDrawerContentCanFillExplicitPanelHeight() = runComposeUiTest {
    setContent {
      FixedHeightBottomDrawerLayout()
    }

    waitForIdle()

    assertThat(onNodeWithTag("panel").boundsInRoot().height.roundToInt()).isEqualTo(100)
    assertThat(onNodeWithTag("content").boundsInRoot().height.roundToInt()).isEqualTo(100)
  }

  @Test
  fun bottomDrawerWithFullSizePanelIsFullyPastViewportWhenClosed() = runComposeUiTest {
    setContent {
      val state = rememberDrawerState(
        initialSnapPoint = DrawerSnapPoint.Closed,
      )
      UnstyledDrawer(
        state = state,
        side = DrawerSide.Bottom,
        modifier = Modifier.size(100.dp),
      ) {
        Viewport(
          modifier = Modifier
            .size(100.dp)
            .testTag("viewport"),
        ) {
          Panel(
            modifier = Modifier
              .fillMaxSize()
              .testTag("panel"),
          ) {
            Box(Modifier.size(20.dp))
          }
        }
      }
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(
      viewportBounds.bottom.roundToInt() + panelBounds.height.roundToInt(),
    )
  }

  @Test
  fun bottomDrawerOpeningFromClosedMovesContinuously() = runComposeUiTest {
    lateinit var state: DrawerState

    setContent {
      state = rememberDrawerState(
        initialSnapPoint = DrawerSnapPoint.Closed,
      )
      UnstyledDrawer(
        state = state,
        side = DrawerSide.Bottom,
        modifier = Modifier.size(100.dp),
      ) {
        Viewport(
          modifier = Modifier
            .size(100.dp)
            .testTag("viewport"),
        ) {
          Panel(
            modifier = Modifier
              .fillMaxSize()
              .testTag("panel"),
          ) {
            Box(Modifier.size(20.dp))
          }
        }
      }
    }

    waitForIdle()
    mainClock.autoAdvance = false

    try {
      runOnIdle {
        state.targetSnapPoint = DrawerSnapPoint.Open
      }
      mainClock.advanceTimeBy(100)
      waitForIdle()

      val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
      val panelBounds = onNodeWithTag("panel").boundsInRoot()

      assertThat(panelBounds.top).isLessThan(viewportBounds.bottom)
      assertThat(panelBounds.top).isGreaterThan(viewportBounds.top)
    } finally {
      mainClock.autoAdvance = true
    }
  }

  @Test
  fun bottomDrawerPanelCanFillViewportHeight() = runComposeUiTest {
    setContent {
      val state = rememberDrawerState(
        initialSnapPoint = DrawerSnapPoint.Open,
      )
      UnstyledDrawer(
        state = state,
        side = DrawerSide.Bottom,
        modifier = Modifier.size(100.dp),
      ) {
        Viewport(
          modifier = Modifier
            .size(100.dp)
            .testTag("viewport"),
        ) {
          Panel(
            modifier = Modifier
              .fillMaxSize()
              .testTag("panel"),
          ) {
            Box(Modifier.size(20.dp))
          }
        }
      }
    }

    waitUntil {
      onNodeWithTag("panel").boundsInRoot().height.roundToInt() == 100
    }

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(viewportBounds.top.roundToInt())
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(panelBounds.width.roundToInt()).isEqualTo(viewportBounds.width.roundToInt())
    assertThat(panelBounds.height.roundToInt()).isEqualTo(viewportBounds.height.roundToInt())
  }

  @Test
  fun bottomDrawerStaysFullyOpenWhenPanelBecomesViewportHeight() = runComposeUiTest {
    val ninetyPercent = DrawerSnapPoint("90%") { containerSize, _ ->
      containerSize * 0.9f
    }
    var fillPanel by mutableStateOf(false)

    setContent {
      val state = rememberDrawerState(
        initialSnapPoint = DrawerSnapPoint.Open,
        snapPoints = {
          listOf(DrawerSnapPoint.Open, ninetyPercent, DrawerSnapPoint.Closed)
        },
      )
      UnstyledDrawer(
        state = state,
        side = DrawerSide.Bottom,
        modifier = Modifier.size(100.dp),
      ) {
        Viewport(
          modifier = Modifier
            .size(100.dp)
            .testTag("viewport"),
        ) {
          Panel(
            modifier = if (fillPanel) {
              Modifier
                .fillMaxSize()
                .testTag("panel")
            } else {
              Modifier
                .width(100.dp)
                .height(40.dp)
                .testTag("panel")
            },
          ) {
            Box(Modifier.size(20.dp))
          }
        }
      }
    }

    waitForIdle()

    fillPanel = true

    waitUntil {
      onNodeWithTag("panel").boundsInRoot().height.roundToInt() == 100
    }

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(viewportBounds.top.roundToInt())
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(panelBounds.height.roundToInt()).isEqualTo(viewportBounds.height.roundToInt())
  }

  @Test
  fun bottomDrawerKeepsScrollableColumnWithoutFixedSizeBoundedToVisibleHeight() = runComposeUiTest {
    var itemCount by mutableStateOf(0)

    setContent {
      Box(
        Modifier
          .requiredSize(400.dp)
          .testTag("root"),
      ) {
        val state = rememberDrawerState(
          initialSnapPoint = DrawerSnapPoint.Open,
          snapPoints = {
            listOf(DrawerSnapPoint.Open)
          },
        )

        UnstyledDrawer(
          state = state,
          side = DrawerSide.Bottom,
          modifier = Modifier.fillMaxSize(),
        ) {
          Viewport(
            modifier = Modifier
              .fillMaxSize()
              .testTag("viewport"),
          ) {
            Panel(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("panel"),
            ) {
              Column(
                Modifier
                  .fillMaxWidth()
                  .verticalScroll(rememberScrollState())
                  .testTag("scrollable_content"),
              ) {
                repeat(itemCount) { index ->
                  BasicText(
                    text = "item_$index",
                    modifier = Modifier
                      .testTag("item_$index")
                      .fillMaxWidth()
                      .height(100.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    runOnIdle {
      itemCount = 8
    }

    waitUntil {
      onNodeWithTag("panel").boundsInRoot().height.roundToInt() == 400
    }

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()
    val scrollableContentBounds = onNodeWithTag("scrollable_content").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(viewportBounds.top.roundToInt())
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(
      scrollableContentBounds.height.roundToInt(),
    ).isEqualTo(viewportBounds.height.roundToInt())

    onNodeWithTag("item_7").performScrollTo()
    onNodeWithTag("item_7").assertIsDisplayed()
  }

  @Test
  fun bottomDrawerKeepsLazyColumnWithoutFixedSizeBoundedToVisibleHeight() = runComposeUiTest {
    var itemCount by mutableStateOf(0)

    setContent {
      Box(
        Modifier
          .requiredSize(400.dp)
          .testTag("root"),
      ) {
        val state = rememberDrawerState(
          initialSnapPoint = DrawerSnapPoint.Open,
          snapPoints = {
            listOf(DrawerSnapPoint.Open)
          },
        )

        UnstyledDrawer(
          state = state,
          side = DrawerSide.Bottom,
          modifier = Modifier.fillMaxSize(),
        ) {
          Viewport(
            modifier = Modifier
              .fillMaxSize()
              .testTag("viewport"),
          ) {
            Panel(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("panel"),
            ) {
              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("lazy_content"),
              ) {
                items(itemCount) { index ->
                  BasicText(
                    text = "item_$index",
                    modifier = Modifier
                      .testTag("item_$index")
                      .fillMaxWidth()
                      .height(100.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    runOnIdle {
      itemCount = 8
    }

    waitUntil {
      onNodeWithTag("panel").boundsInRoot().height.roundToInt() == 400
    }

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()
    val lazyContentBounds = onNodeWithTag("lazy_content").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(viewportBounds.top.roundToInt())
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(lazyContentBounds.height.roundToInt()).isEqualTo(viewportBounds.height.roundToInt())

    onNodeWithTag("lazy_content").performScrollToIndex(7)
    onNodeWithTag("item_7").assertIsDisplayed()
  }

  @Test
  fun bottomDrawerPartiallyShowsViewportAtNinetyPercentSnapPoint() = runComposeUiTest {
    val ninetyPercent = DrawerSnapPoint("90%") { containerSize, _ ->
      containerSize * 0.9f
    }

    setContent {
      val state = rememberDrawerState(
        initialSnapPoint = ninetyPercent,
        snapPoints = {
          listOf(DrawerSnapPoint.Open, ninetyPercent, DrawerSnapPoint.Closed)
        },
      )
      UnstyledDrawer(
        state = state,
        side = DrawerSide.Bottom,
        modifier = Modifier.size(100.dp),
      ) {
        Viewport(
          modifier = Modifier
            .size(100.dp)
            .testTag("viewport"),
        ) {
          Panel(
            modifier = Modifier
              .fillMaxSize()
              .testTag("panel"),
          ) {
            Box(Modifier.size(20.dp))
          }
        }
      }
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(
      viewportBounds.top.roundToInt() + 10,
    )
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(
      viewportBounds.bottom.roundToInt() + 10,
    )
    assertThat(panelBounds.height.roundToInt()).isEqualTo(viewportBounds.height.roundToInt())
  }

  @Test
  fun endDrawerClosesTowardLeftInRtl() = runComposeUiTest {
    lateinit var state: DrawerState

    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        EdgeDrawerLayout(
          side = DrawerSide.End,
          initialSnapPoint = DrawerSnapPoint.Open,
          onState = { state = it },
        )
      }
    }

    waitForIdle()

    onNodeWithTag("panel").performTouchInput {
      swipe(start = centerRight, end = centerLeft)
    }
    waitForIdle()

    assertThat(state.currentSnapPoint).isEqualTo(DrawerSnapPoint.Closed)
  }

  @Test
  fun topDrawerClosesTowardTop() = runComposeUiTest {
    lateinit var state: DrawerState

    setContent {
      EdgeDrawerLayout(
        side = DrawerSide.Top,
        initialSnapPoint = DrawerSnapPoint.Open,
        onState = { state = it },
      )
    }

    waitForIdle()

    onNodeWithTag("panel").performTouchInput {
      swipe(start = bottomCenter, end = topCenter)
    }
    waitForIdle()

    assertThat(state.currentSnapPoint).isEqualTo(DrawerSnapPoint.Closed)
  }

  @Test
  fun peekPanelMovesPartiallyOffscreenFromTheViewportBottom() = runComposeUiTest {
    setContent {
      DrawerLayout(
        initialSnapPoint = Peek,
        snapPoints = listOf(DrawerSnapPoint.Closed, Peek),
      )
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(40)
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(panelBounds.height.roundToInt()).isEqualTo(60)
  }

  @Test
  fun oversizedContentMovesWithThePanel() = runComposeUiTest {
    setContent {
      DrawerLayout(
        initialSnapPoint = Peek,
        snapPoints = listOf(DrawerSnapPoint.Closed, Peek),
      )
    }

    waitForIdle()

    val panelBounds = onNodeWithTag("panel").boundsInRoot()
    val contentBottomBounds = onNodeWithTag("contentBottom", useUnmergedTree = true).boundsInRoot()

    assertThat(contentBottomBounds.bottom.roundToInt())
      .isEqualTo(panelBounds.bottom.roundToInt())
    assertThat(contentBottomBounds.top.roundToInt()).isEqualTo(90)
  }

  @Test
  fun bottomCustomSnapPointIsCappedToPanelSize() = runComposeUiTest {
    setContent {
      DrawerLayout(
        initialSnapPoint = Peek,
        snapPoints = listOf(DrawerSnapPoint.Closed, Peek),
        contentHeight = 40,
      )
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(60)
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(panelBounds.height.roundToInt()).isEqualTo(40)
  }

  @Test
  fun topCustomSnapPointIsCappedToPanelSize() = runComposeUiTest {
    setContent {
      EdgeDrawerLayout(
        side = DrawerSide.Top,
        initialSnapPoint = Peek,
        snapPoints = listOf(DrawerSnapPoint.Closed, Peek),
        contentHeight = 40,
      )
    }

    waitForIdle()

    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(0)
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(40)
    assertThat(panelBounds.height.roundToInt()).isEqualTo(40)
  }

  @Test
  fun startCustomSnapPointIsCappedToPanelSize() = runComposeUiTest {
    setContent {
      EdgeDrawerLayout(
        side = DrawerSide.Start,
        initialSnapPoint = Peek,
        snapPoints = listOf(DrawerSnapPoint.Closed, Peek),
        contentWidth = 40,
      )
    }

    waitForIdle()

    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(0)
    assertThat(panelBounds.right.roundToInt()).isEqualTo(40)
    assertThat(panelBounds.width.roundToInt()).isEqualTo(40)
  }

  @Test
  fun startCustomSnapPointIsCappedToPanelSizeInRtl() = runComposeUiTest {
    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        EdgeDrawerLayout(
          side = DrawerSide.Start,
          initialSnapPoint = Peek,
          snapPoints = listOf(DrawerSnapPoint.Closed, Peek),
          contentWidth = 40,
        )
      }
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(
      viewportBounds.right.roundToInt() - 40,
    )
    assertThat(panelBounds.right.roundToInt()).isEqualTo(viewportBounds.right.roundToInt())
    assertThat(panelBounds.width.roundToInt()).isEqualTo(40)
  }

  @Test
  fun endCustomSnapPointIsCappedToPanelSize() = runComposeUiTest {
    setContent {
      EdgeDrawerLayout(
        side = DrawerSide.End,
        initialSnapPoint = Peek,
        snapPoints = listOf(DrawerSnapPoint.Closed, Peek),
        contentWidth = 40,
      )
    }

    waitForIdle()

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(
      viewportBounds.right.roundToInt() - 40,
    )
    assertThat(panelBounds.right.roundToInt()).isEqualTo(viewportBounds.right.roundToInt())
    assertThat(panelBounds.width.roundToInt()).isEqualTo(40)
  }

  @Test
  fun endCustomSnapPointIsCappedToPanelSizeInRtl() = runComposeUiTest {
    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        EdgeDrawerLayout(
          side = DrawerSide.End,
          initialSnapPoint = Peek,
          snapPoints = listOf(DrawerSnapPoint.Closed, Peek),
          contentWidth = 40,
        )
      }
    }

    waitForIdle()

    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.left.roundToInt()).isEqualTo(0)
    assertThat(panelBounds.right.roundToInt()).isEqualTo(40)
    assertThat(panelBounds.width.roundToInt()).isEqualTo(40)
  }

  @Test
  fun settingTargetSnapPointMovesToCustomSnapPoint() = runComposeUiTest {
    lateinit var state: DrawerState

    setContent {
      DrawerLayout(
        snapPoints = listOf(DrawerSnapPoint.Closed, Peek, DrawerSnapPoint.Open),
        onState = { state = it },
      )
    }

    waitForIdle()

    state.targetSnapPoint = Peek

    waitUntil {
      state.isIdle && state.currentSnapPoint == Peek
    }

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(40)
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(panelBounds.height.roundToInt()).isEqualTo(60)
  }

  @Test
  fun jumpToMovesToCustomSnapPoint() = runComposeUiTest {
    lateinit var state: DrawerState

    setContent {
      DrawerLayout(
        snapPoints = listOf(DrawerSnapPoint.Closed, Peek, DrawerSnapPoint.Open),
        onState = { state = it },
      )
    }

    waitForIdle()

    state.jumpTo(Peek)

    waitUntil {
      state.isIdle && state.currentSnapPoint == Peek
    }

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(40)
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(panelBounds.height.roundToInt()).isEqualTo(60)
  }

  @Test
  fun customSnapPointUpdatesWhenContentSizeChanges() = runComposeUiTest {
    var contentHeight by mutableStateOf(40)

    setContent {
      DrawerLayout(
        initialSnapPoint = DrawerSnapPoint.Open,
        contentHeight = contentHeight,
      )
    }

    waitForIdle()

    assertThat(onNodeWithTag("panel").boundsInRoot().height.roundToInt()).isEqualTo(40)

    contentHeight = 80

    waitUntil {
      onNodeWithTag("panel").boundsInRoot().height.roundToInt() == 80
    }

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(20)
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(panelBounds.height.roundToInt()).isEqualTo(80)
  }

  @Test
  fun customSnapPointUpdatesWhenSnapPointLambdaChanges() = runComposeUiTest {
    lateinit var state: DrawerState
    var includePeek by mutableStateOf(false)

    setContent {
      state = rememberDrawerState(
        snapPoints = {
          if (includePeek) {
            listOf(DrawerSnapPoint.Closed, Peek, DrawerSnapPoint.Open)
          } else {
            listOf(DrawerSnapPoint.Closed, DrawerSnapPoint.Open)
          }
        },
      )

      DrawerLayoutContent(state = state)
    }

    waitForIdle()

    assertThat(state.snapPoints).isEqualTo(listOf(DrawerSnapPoint.Closed, DrawerSnapPoint.Open))

    runOnIdle {
      includePeek = true
    }

    waitUntil {
      state.snapPoints == listOf(DrawerSnapPoint.Closed, Peek, DrawerSnapPoint.Open)
    }
  }

  @Test
  fun customSnapPointCanBeTargetedAfterSnapPointsListChanges() = runComposeUiTest {
    lateinit var state: DrawerState
    var includePeek by mutableStateOf(false)

    setContent {
      val snapPoints = if (includePeek) {
        listOf(DrawerSnapPoint.Closed, Peek, DrawerSnapPoint.Open)
      } else {
        listOf(DrawerSnapPoint.Closed, DrawerSnapPoint.Open)
      }
      DrawerLayout(
        snapPoints = snapPoints,
        onState = { state = it },
      )
    }

    waitForIdle()

    runOnIdle {
      includePeek = true
    }

    waitUntil {
      state.snapPoints.contains(Peek)
    }

    state.targetSnapPoint = Peek

    waitUntil {
      state.isIdle && state.currentSnapPoint == Peek
    }

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()

    assertThat(panelBounds.top.roundToInt()).isEqualTo(40)
    assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
    assertThat(panelBounds.height.roundToInt()).isEqualTo(60)
  }

  @Test
  fun drawerAnimatesToClosestSnapPointWhenCurrentSnapPointIsRemoved() = runComposeUiTest {
    lateinit var state: DrawerState
    var snapPoints by mutableStateOf(listOf(DrawerSnapPoint.Closed, Peek, DrawerSnapPoint.Open))

    setContent {
      DrawerLayout(
        initialSnapPoint = Peek,
        snapPoints = snapPoints,
        onState = { state = it },
      )
    }

    waitForIdle()
    mainClock.autoAdvance = false

    try {
      runOnIdle {
        snapPoints = listOf(DrawerSnapPoint.Closed, DrawerSnapPoint.Open)
      }
      mainClock.advanceTimeByFrame()

      waitForIdle()

      assertThat(state.targetSnapPoint).isEqualTo(DrawerSnapPoint.Open)
      assertThat(onNodeWithTag("panel").boundsInRoot().top.roundToInt()).isEqualTo(40)

      mainClock.advanceTimeBy(1_000)
      waitForIdle()

      val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
      val panelBounds = onNodeWithTag("panel").boundsInRoot()

      assertThat(state.currentSnapPoint).isEqualTo(DrawerSnapPoint.Open)
      assertThat(panelBounds.top.roundToInt()).isEqualTo(viewportBounds.top.roundToInt())
      assertThat(panelBounds.bottom.roundToInt()).isEqualTo(viewportBounds.bottom.roundToInt())
      assertThat(panelBounds.height.roundToInt()).isEqualTo(100)
    } finally {
      mainClock.autoAdvance = true
    }
  }

  @Test
  fun panelDispatchesExcessDragToOverscrollEffect() = runComposeUiTest {
    val overscrollEffect = RecordingOverscrollEffect()

    setContent {
      DrawerLayout(
        initialSnapPoint = DrawerSnapPoint.Open,
        overscrollEffect = overscrollEffect,
      )
    }

    waitForIdle()

    onNodeWithTag("panel").performTouchInput {
      swipe(start = center, end = topCenter)
    }
    waitForIdle()

    assertThat(overscrollEffect.totalOverscrollY).isLessThan(0f)
  }

  @Test
  fun lockedPanelDispatchesExcessDragToOverscrollEffect() = runComposeUiTest {
    val overscrollEffect = RecordingOverscrollEffect()

    setContent {
      DrawerLayout(
        initialSnapPoint = DrawerSnapPoint.Open,
        snapPoints = listOf(DrawerSnapPoint.Open),
        overscrollEffect = overscrollEffect,
      )
    }

    waitForIdle()

    onNodeWithTag("panel").performTouchInput {
      swipe(start = center, end = topCenter)
    }
    waitForIdle()

    assertThat(overscrollEffect.totalOverscrollY).isLessThan(0f)
  }

  @Test
  fun bottomDrawerExpandsBeforeScrollingLazyContent() = runComposeUiTest {
    val peek = DrawerSnapPoint("peek") { containerSize, _ ->
      containerSize * 0.5f
    }
    lateinit var state: DrawerState

    setContent {
      state = rememberDrawerState(
        initialSnapPoint = peek,
        snapPoints = {
          listOf(peek, DrawerSnapPoint.Open)
        },
      )

      UnstyledDrawer(
        state = state,
        side = DrawerSide.Bottom,
        modifier = Modifier.size(100.dp),
      ) {
        Viewport(
          modifier = Modifier
            .size(100.dp)
            .testTag("viewport"),
        ) {
          Panel(
            modifier = Modifier
              .fillMaxSize()
              .testTag("panel"),
          ) {
            LazyColumn(
              modifier = Modifier
                .fillMaxSize()
                .testTag("lazy_content"),
            ) {
              items(20) { index ->
                Box(
                  Modifier
                    .testTag("item_$index")
                    .fillMaxWidth()
                    .height(30.dp),
                )
              }
            }
          }
        }
      }
    }

    waitForIdle()

    onNodeWithTag("lazy_content").performTouchInput {
      swipeUp()
    }
    waitForIdle()

    assertThat(state.currentSnapPoint).isEqualTo(DrawerSnapPoint.Open)
  }

  @Test
  fun scrollableColumnExpandsToVisibleBottomDrawerHeight() = runComposeUiTest {
    val peek = DrawerSnapPoint("peek") { containerSize, _ ->
      containerSize * 0.5f
    }

    setContent {
      Box(
        Modifier
          .requiredSize(400.dp)
          .testTag("root"),
      ) {
        val state = rememberDrawerState(
          initialSnapPoint = peek,
          snapPoints = {
            listOf(peek, DrawerSnapPoint.Open)
          },
        )

        UnstyledDrawer(
          state = state,
          side = DrawerSide.Bottom,
          modifier = Modifier.fillMaxSize(),
        ) {
          Viewport(
            modifier = Modifier
              .fillMaxSize()
              .testTag("viewport"),
          ) {
            Panel(Modifier.testTag("panel")) {
              Column(
                Modifier
                  .testTag("scrollable_content")
                  .verticalScroll(rememberScrollState()),
              ) {
                repeat(6) { index ->
                  BasicText(
                    text = "item_$index",
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(100.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    waitUntilExactlyOneExists(hasTestTag("viewport"))

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()
    val scrollableContentBounds = onNodeWithTag("scrollable_content").boundsInRoot()
    val visibleDrawerHeight = viewportBounds.bottom - panelBounds.top

    assertThat(visibleDrawerHeight).isEqualTo(viewportBounds.height / 2f)
    assertThat(scrollableContentBounds.height).isEqualTo(visibleDrawerHeight)
  }

  @Test
  fun scrollableColumnExpandsToVisibleTopDrawerHeight() = runComposeUiTest {
    val peek = DrawerSnapPoint("peek") { containerSize, _ ->
      containerSize * 0.5f
    }

    setContent {
      Box(
        Modifier
          .requiredSize(400.dp)
          .testTag("root"),
      ) {
        val state = rememberDrawerState(
          initialSnapPoint = peek,
          snapPoints = {
            listOf(peek, DrawerSnapPoint.Open)
          },
        )

        UnstyledDrawer(
          state = state,
          side = DrawerSide.Top,
          modifier = Modifier.fillMaxSize(),
        ) {
          Viewport(
            modifier = Modifier
              .fillMaxSize()
              .testTag("viewport"),
          ) {
            Panel(Modifier.testTag("panel")) {
              Column(
                Modifier
                  .testTag("scrollable_content")
                  .verticalScroll(rememberScrollState()),
              ) {
                repeat(6) { index ->
                  BasicText(
                    text = "item_$index",
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(100.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    waitUntilExactlyOneExists(hasTestTag("viewport"))

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()
    val scrollableContentBounds = onNodeWithTag("scrollable_content").boundsInRoot()
    val visibleDrawerHeight = panelBounds.bottom - viewportBounds.top

    assertThat(visibleDrawerHeight).isEqualTo(viewportBounds.height / 2f)
    assertThat(scrollableContentBounds.height).isEqualTo(visibleDrawerHeight)
  }

  @Test
  fun scrollableRowExpandsToVisibleStartDrawerWidth() = runComposeUiTest {
    val peek = DrawerSnapPoint("peek") { containerSize, _ ->
      containerSize * 0.5f
    }

    setContent {
      Box(
        Modifier
          .requiredSize(400.dp)
          .testTag("root"),
      ) {
        val state = rememberDrawerState(
          initialSnapPoint = peek,
          snapPoints = {
            listOf(peek, DrawerSnapPoint.Open)
          },
        )

        UnstyledDrawer(
          state = state,
          side = DrawerSide.Start,
          modifier = Modifier.fillMaxSize(),
        ) {
          Viewport(
            modifier = Modifier
              .fillMaxSize()
              .testTag("viewport"),
          ) {
            Panel(Modifier.testTag("panel")) {
              Row(
                Modifier
                  .testTag("scrollable_content")
                  .horizontalScroll(rememberScrollState()),
              ) {
                repeat(6) { index ->
                  BasicText(
                    text = "item_$index",
                    modifier = Modifier
                      .fillMaxHeight()
                      .width(100.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    waitUntilExactlyOneExists(hasTestTag("viewport"))

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()
    val scrollableContentBounds = onNodeWithTag("scrollable_content").boundsInRoot()
    val visibleDrawerWidth = panelBounds.right - viewportBounds.left

    assertThat(visibleDrawerWidth).isEqualTo(viewportBounds.width / 2f)
    assertThat(scrollableContentBounds.width).isEqualTo(visibleDrawerWidth)
  }

  @Test
  fun scrollableRowExpandsToVisibleEndDrawerWidth() = runComposeUiTest {
    val peek = DrawerSnapPoint("peek") { containerSize, _ ->
      containerSize * 0.5f
    }

    setContent {
      Box(
        Modifier
          .requiredSize(400.dp)
          .testTag("root"),
      ) {
        val state = rememberDrawerState(
          initialSnapPoint = peek,
          snapPoints = {
            listOf(peek, DrawerSnapPoint.Open)
          },
        )

        UnstyledDrawer(
          state = state,
          side = DrawerSide.End,
          modifier = Modifier.fillMaxSize(),
        ) {
          Viewport(
            modifier = Modifier
              .fillMaxSize()
              .testTag("viewport"),
          ) {
            Panel(Modifier.testTag("panel")) {
              Row(
                Modifier
                  .testTag("scrollable_content")
                  .horizontalScroll(rememberScrollState()),
              ) {
                repeat(6) { index ->
                  BasicText(
                    text = "item_$index",
                    modifier = Modifier
                      .fillMaxHeight()
                      .width(100.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    waitUntilExactlyOneExists(hasTestTag("viewport"))

    val viewportBounds = onNodeWithTag("viewport").boundsInRoot()
    val panelBounds = onNodeWithTag("panel").boundsInRoot()
    val scrollableContentBounds = onNodeWithTag("scrollable_content").boundsInRoot()
    val visibleDrawerWidth = viewportBounds.right - panelBounds.left

    assertThat(visibleDrawerWidth).isEqualTo(viewportBounds.width / 2f)
    assertThat(scrollableContentBounds.width).isEqualTo(visibleDrawerWidth)
  }

  @Test
  fun scrollableColumnWithoutFixedHeightDoesNotClipLastItem() = runComposeUiTest {
    val expanded = DrawerSnapPoint("expanded") { containerSize, _ ->
      containerSize * 0.7f
    }

    setContent {
      Box(
        Modifier
          .requiredSize(400.dp)
          .testTag("root"),
      ) {
        val state = rememberDrawerState(
          initialSnapPoint = expanded,
          snapPoints = {
            listOf(DrawerSnapPoint.Closed, expanded)
          },
        )

        UnstyledDrawer(
          state = state,
          side = DrawerSide.Bottom,
          modifier = Modifier.fillMaxSize(),
        ) {
          Viewport(Modifier.fillMaxSize()) {
            Panel {
              Column(
                Modifier
                  .testTag("scrollable_content")
                  .verticalScroll(rememberScrollState()),
              ) {
                repeat(10) { index ->
                  BasicText(
                    text = "item_$index",
                    modifier = Modifier
                      .testTag("item_$index")
                      .fillMaxWidth()
                      .height(100.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    waitUntilExactlyOneExists(hasTestTag("root"))

    onNodeWithTag("item_9").performScrollTo()

    val rootBounds = onNodeWithTag("root").boundsInRoot()
    val lastItemBounds = onNodeWithTag("item_9").boundsInRoot()

    assertThat(lastItemBounds.bottom <= rootBounds.bottom).isEqualTo(true)
  }

  private fun SemanticsNodeInteraction.boundsInRoot(): Rect {
    return fetchSemanticsNode().boundsInRoot
  }
}

private val Peek = DrawerSnapPoint("peek") { containerSize, _ ->
  containerSize * 0.6f
}

@Composable
private fun DefaultDrawerLayout() {
  DrawerLayoutContent(state = rememberDrawerState())
}

@Composable
private fun DrawerLayout(
  initialSnapPoint: DrawerSnapPoint = DrawerSnapPoint.Closed,
  snapPoints: List<DrawerSnapPoint> = listOf(DrawerSnapPoint.Closed, DrawerSnapPoint.Open),
  contentHeight: Int = 100,
  overscrollEffect: OverscrollEffect? = null,
  onState: (DrawerState) -> Unit = {},
) {
  val state = rememberDrawerState(
    initialSnapPoint = initialSnapPoint,
    snapPoints = { snapPoints },
  )
  onState(state)

  DrawerLayoutContent(
    state = state,
    contentHeight = contentHeight,
    overscrollEffect = overscrollEffect,
  )
}

@Composable
private fun StartDrawerLayout(
  initialSnapPoint: DrawerSnapPoint = DrawerSnapPoint.Closed,
  snapPoints: List<DrawerSnapPoint> = listOf(DrawerSnapPoint.Closed, DrawerSnapPoint.Open),
  contentWidth: Int = 100,
  onState: (DrawerState) -> Unit = {},
) {
  EdgeDrawerLayout(
    side = DrawerSide.Start,
    initialSnapPoint = initialSnapPoint,
    snapPoints = snapPoints,
    contentWidth = contentWidth,
    onState = onState,
  )
}

@Composable
private fun ConstrainedStartDrawerLayout(
  initialSnapPoint: DrawerSnapPoint = DrawerSnapPoint.Closed,
  onState: (DrawerState) -> Unit = {},
) {
  val state = rememberDrawerState(
    initialSnapPoint = initialSnapPoint,
  )
  onState(state)

  UnstyledDrawer(
    state = state,
    side = DrawerSide.Start,
    modifier = Modifier.width(500.dp).height(100.dp),
  ) {
    Viewport(
      modifier = Modifier
        .width(500.dp)
        .height(100.dp)
        .testTag("viewport"),
    ) {
      Panel(
        modifier = Modifier
          .height(100.dp)
          .widthIn(max = 100.dp)
          .testTag("panel"),
      ) {
        Box(
          Modifier
            .testTag("content")
            .width(300.dp)
            .height(100.dp),
        )
      }
    }
  }
}

@Composable
private fun ConstrainedEndDrawerLayout() {
  val state = rememberDrawerState(
    initialSnapPoint = DrawerSnapPoint.Open,
  )

  UnstyledDrawer(
    state = state,
    side = DrawerSide.End,
    modifier = Modifier.width(500.dp).height(100.dp),
  ) {
    Viewport(
      modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)
        .testTag("viewport"),
    ) {
      Panel(
        modifier = Modifier
          .height(100.dp)
          .widthIn(max = 100.dp)
          .testTag("panel"),
      ) {
        Box(
          Modifier
            .testTag("content")
            .width(300.dp)
            .height(100.dp),
        )
      }
    }
  }
}

@Composable
private fun FixedWidthStartDrawerLayout(
  initialSnapPoint: DrawerSnapPoint,
) {
  val state = rememberDrawerState(
    initialSnapPoint = initialSnapPoint,
  )

  UnstyledDrawer(
    state = state,
    side = DrawerSide.Start,
    modifier = Modifier.width(200.dp).height(100.dp),
  ) {
    Viewport(
      modifier = Modifier
        .width(200.dp)
        .height(100.dp),
    ) {
      Panel(
        modifier = Modifier
          .width(100.dp)
          .height(100.dp)
          .testTag("panel"),
      ) {
        Box(
          Modifier
            .fillMaxWidth()
            .height(100.dp)
            .testTag("content"),
        )
      }
    }
  }
}

@Composable
private fun FixedHeightBottomDrawerLayout() {
  val state = rememberDrawerState(
    initialSnapPoint = DrawerSnapPoint.Open,
  )

  UnstyledDrawer(
    state = state,
    side = DrawerSide.Bottom,
    modifier = Modifier.width(100.dp).height(200.dp),
  ) {
    Viewport(
      modifier = Modifier
        .width(100.dp)
        .height(200.dp),
    ) {
      Panel(
        modifier = Modifier
          .width(100.dp)
          .height(100.dp)
          .testTag("panel"),
      ) {
        Box(
          Modifier
            .width(100.dp)
            .fillMaxHeight()
            .testTag("content"),
        )
      }
    }
  }
}

@Composable
private fun EdgeDrawerLayout(
  side: DrawerSide,
  initialSnapPoint: DrawerSnapPoint = DrawerSnapPoint.Closed,
  snapPoints: List<DrawerSnapPoint> = listOf(DrawerSnapPoint.Closed, DrawerSnapPoint.Open),
  contentWidth: Int = 100,
  contentHeight: Int = 100,
  onState: (DrawerState) -> Unit = {},
) {
  val isHorizontal = side == DrawerSide.Start || side == DrawerSide.End
  val viewportWidth = if (isHorizontal) {
    200.dp
  } else {
    100.dp
  }
  val viewportHeight = 100.dp
  val panelModifier = if (side == DrawerSide.Top || side == DrawerSide.Bottom) {
    Modifier.width(100.dp)
  } else {
    Modifier.height(viewportHeight)
  }
  val state = rememberDrawerState(
    initialSnapPoint = initialSnapPoint,
    snapPoints = { snapPoints },
  )
  onState(state)

  UnstyledDrawer(
    state = state,
    side = side,
    modifier = Modifier.width(viewportWidth).height(viewportHeight),
  ) {
    Viewport(
      modifier = Modifier
        .width(viewportWidth)
        .height(viewportHeight)
        .testTag("viewport"),
    ) {
      Panel(
        modifier = panelModifier
          .testTag("panel"),
      ) {
        Box(
          Modifier
            .testTag("content")
            .width(contentWidth.dp)
            .height(contentHeight.dp),
        )
      }
    }
  }
}

@Composable
private fun DrawerLayoutContent(
  state: DrawerState,
  contentHeight: Int = 100,
  overscrollEffect: OverscrollEffect? = null,
) {
  UnstyledDrawer(
    state = state,
    side = DrawerSide.Bottom,
    modifier = Modifier.size(100.dp),
  ) {
    Viewport(
      modifier = Modifier
        .size(100.dp)
        .testTag("viewport"),
    ) {
      Panel(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("panel"),
        overscrollEffect = overscrollEffect,
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight.dp),
        ) {
          Box(
            modifier = Modifier
              .width(100.dp)
              .height(contentHeight.dp),
            contentAlignment = Alignment.BottomStart,
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .testTag("contentBottom"),
            )
          }
        }
      }
    }
  }
}

private class RecordingOverscrollEffect : OverscrollEffect {
  var totalOverscrollY: Float = 0f
    private set

  override fun applyToScroll(
    delta: Offset,
    source: NestedScrollSource,
    performScroll: (Offset) -> Offset,
  ): Offset {
    val consumed = performScroll(delta)
    totalOverscrollY += delta.y - consumed.y
    return consumed
  }

  override suspend fun applyToFling(
    velocity: Velocity,
    performFling: suspend (Velocity) -> Velocity,
  ) {
    performFling(velocity)
  }

  override val isInProgress: Boolean
    get() = totalOverscrollY != 0f
}
