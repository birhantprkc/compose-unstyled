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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.DrawerSide
import com.composeunstyled.DrawerSnapPoint
import com.composeunstyled.Panel
import com.composeunstyled.UnstyledDrawer
import com.composeunstyled.Viewport
import com.composeunstyled.rememberDrawerState

@Composable
fun DrawerOverscrollEffectDemo() {
  val drawerState = rememberDrawerState(
    initialSnapPoint = DrawerSnapPoint.Open,
    snapPoints = { listOf(DrawerSnapPoint.Open) },
  )

  UnstyledDrawer(
    state = drawerState,
    modifier = Modifier.fillMaxSize(),
    side = DrawerSide.Bottom,
  ) {
    Viewport(Modifier.fillMaxSize()) {
      Panel(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White)
          .border(1.dp, Color.Black),
        overscrollEffect = rememberElasticOverscrollEffect(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          BasicText("Try dragging me upwards", style = TextStyle(fontSize = 22.sp))
          BasicText("The drawer is locked in place with an overscroll effect")
        }
      }
    }
  }
}
