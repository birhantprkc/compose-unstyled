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

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.composeunstyled.DrawerSide
import com.composeunstyled.DrawerSnapPoint
import com.composeunstyled.Panel
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledDrawer
import com.composeunstyled.Viewport
import com.composeunstyled.rememberDrawerState

@Composable
fun DrawerNestedScrollDemo() {
  val peek = remember {
    DrawerSnapPoint("peek") { containerSize, _ ->
      containerSize * 0.65f
    }
  }
  val drawerState = rememberDrawerState(
    initialSnapPoint = peek,
    snapPoints = {
      listOf(DrawerSnapPoint.Closed, peek, DrawerSnapPoint.Open)
    },
  )

  UnstyledButton(
    onClick = {
      drawerState.targetSnapPoint = peek
    },
    modifier = Modifier
      .heightIn(32.dp)
      .background(Color.White)
      .border(1.dp, Color.Black),
    contentPadding = PaddingValues(horizontal = 10.dp),
    indication = LocalIndication.current,
  ) {
    BasicText("Open drawer")
  }

  UnstyledDrawer(
    state = drawerState,
    modifier = Modifier.fillMaxSize(),
    side = DrawerSide.Bottom,
  ) {
    Viewport(Modifier.fillMaxSize()) {
      Panel(
        modifier = Modifier
          .dropShadow(
            shape = RectangleShape,
            shadow = Shadow(
              radius = 0.dp,
              color = Color.Black,
              spread = 0.dp,
              offset = DpOffset(8.dp, 8.dp),
              alpha = 0.33f,
            ),
          )
          .background(Color.White)
          .border(1.dp, Color.Black)
          .fillMaxSize()
          .padding(12.dp),
      ) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            BasicText(
              "Messages",
              modifier = Modifier.padding(bottom = 4.dp),
            )
          }
          items((1..32).toList()) { index ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black)
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(Color(0xFFD6D6D6), CircleShape),
              )
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth(if (index % 3 == 0) 0.62f else 0.48f)
                    .height(14.dp)
                    .background(Color(0xFFBDBDBD)),
                )
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Color(0xFFD6D6D6)),
                )
                Box(
                  modifier = Modifier
                    .fillMaxWidth(if (index % 4 == 0) 0.72f else 0.86f)
                    .height(10.dp)
                    .background(Color(0xFFD6D6D6)),
                )
              }
            }
          }
        }
      }
    }
  }
}
