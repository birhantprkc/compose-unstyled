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
package com.composeunstyled.demo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.DrawerSide
import com.composeunstyled.DrawerSnapPoint
import com.composeunstyled.Panel
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledDrawer
import com.composeunstyled.Viewport
import com.composeunstyled.rememberDrawerState
import kotlin.math.roundToInt

@Composable
fun DrawerDynamicContentSizeDemo() {
  var nextItemNumber by remember { mutableIntStateOf(4) }
  var items by remember {
    mutableStateOf(
      listOf(
        DynamicContentItem(id = 1, label = 1),
        DynamicContentItem(id = 2, label = 2),
        DynamicContentItem(id = 3, label = 3),
      ),
    )
  }
  val itemCount = items.count { item ->
    item.isRemoving.not()
  }
  val itemCountLabel = if (itemCount == 1) {
    "1 Item"
  } else {
    "$itemCount Items"
  }
  val drawerState = rememberDrawerState(
    initialSnapPoint = DrawerSnapPoint.Open,
    snapPoints = {
      listOf(DrawerSnapPoint.Open)
    },
  )

  Column(Modifier.fillMaxSize()) {
    UnstyledDrawer(
      state = drawerState,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .clipToBounds(),
      side = DrawerSide.Bottom,
    ) {
      Viewport(Modifier.fillMaxSize()) {
        Panel(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Color.Black),
        ) {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
          ) {
            itemsIndexed(
              items = items,
              key = { _, item -> item.id },
            ) { index, item ->
              val expansion = remember(item.id) {
                Animatable(
                  initialValue = if (item.animateEnter) {
                    0f
                  } else {
                    1f
                  },
                )
              }

              LaunchedEffect(item.isVisible) {
                expansion.animateTo(
                  targetValue = if (item.isVisible) {
                    1f
                  } else {
                    0f
                  },
                  animationSpec = tween(durationMillis = 600),
                )
                if (item.isRemoving && item.isVisible.not()) {
                  items = items.filterNot { currentItem ->
                    currentItem.id == item.id
                  }
                }
              }

              Column(
                Modifier
                  .fillMaxWidth()
                  .clipToBounds()
                  .expandVerticallyBy(expansion.value),
              ) {
                Column(Modifier.fillMaxWidth()) {
                  if (index == 0) {
                    Spacer(Modifier.height(12.dp))
                  }
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .border(1.dp, Color.Black)
                      .padding(12.dp),
                    contentAlignment = Alignment.CenterStart,
                  ) {
                    BasicText("Item ${item.label}")
                  }
                  Spacer(Modifier.height(12.dp))
                }
              }
            }
          }
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color.Black)
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BasicText(
        text = itemCountLabel,
        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        UnstyledButton(
          onClick = {
            val itemToRemove = items.lastOrNull { item ->
              item.isRemoving.not()
            }
            if (itemToRemove != null) {
              items = items.map { item ->
                if (item.id == itemToRemove.id) {
                  item.copy(isVisible = false, isRemoving = true)
                } else {
                  item
                }
              }
            }
          },
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .heightIn(32.dp)
            .background(Color.White)
            .border(1.dp, Color.Black, RoundedCornerShape(10.dp)),
          contentPadding = PaddingValues(horizontal = 10.dp),
          indication = LocalIndication.current,
        ) {
          BasicText("Remove")
        }

        UnstyledButton(
          onClick = {
            val itemNumber = nextItemNumber
            items = items + DynamicContentItem(
              id = itemNumber,
              label = itemNumber,
              animateEnter = true,
            )
            nextItemNumber += 1
          },
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .heightIn(32.dp)
            .background(Color.White)
            .border(1.dp, Color.Black, RoundedCornerShape(10.dp)),
          contentPadding = PaddingValues(horizontal = 10.dp),
          indication = LocalIndication.current,
        ) {
          BasicText("Add")
        }
      }
    }
  }
}

private data class DynamicContentItem(
  val id: Int,
  val label: Int,
  val isVisible: Boolean = true,
  val isRemoving: Boolean = false,
  val animateEnter: Boolean = false,
)

private fun Modifier.expandVerticallyBy(
  progress: Float,
): Modifier {
  return layout { measurable, constraints ->
    val placeable = measurable.measure(
      constraints.copy(minHeight = 0),
    )
    val animatedHeight = (placeable.height * progress).roundToInt()
    layout(placeable.width, animatedHeight) {
      placeable.placeRelative(0, 0)
    }
  }
}
