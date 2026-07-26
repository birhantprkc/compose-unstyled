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
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.withoutEventHandling
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.math.roundToInt

@Stable
class DrawerSnapPoint(
  val identifier: String,
  val calculateVisibleSize: (containerSize: Dp, panelSize: Dp) -> Dp,
) {
  companion object {
    val Open: DrawerSnapPoint =
      DrawerSnapPoint("open") { _, panelSize -> panelSize }

    val Closed: DrawerSnapPoint = DrawerSnapPoint("closed") { _, _ -> 0.dp }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as DrawerSnapPoint

    return identifier == other.identifier
  }

  override fun hashCode(): Int {
    return identifier.hashCode()
  }

  override fun toString(): String {
    return "DrawerSnapPoint(identifier='$identifier')"
  }
}

@JvmInline
value class DrawerSide internal constructor(private val value: String) {
  companion object {
    val Top = DrawerSide("top")
    val Bottom = DrawerSide("bottom")
    val Start = DrawerSide("start")
    val End = DrawerSide("end")
  }

  override fun toString(): String {
    return value
  }
}

@Composable
fun rememberDrawerState(
  initialSnapPoint: DrawerSnapPoint = DrawerSnapPoint.Closed,
  snapPoints: () -> List<DrawerSnapPoint> = {
    listOf(DrawerSnapPoint.Closed, DrawerSnapPoint.Open)
  },
): DrawerState {
  val density = LocalDensity.current
  val scope = rememberCoroutineScope()
  val currentSnapPoints = snapPoints()
  val state = remember(scope, density) {
    DrawerState(
      initialSnapPoint = initialSnapPoint,
      snapPoints = currentSnapPoints,
      coroutineScope = scope,
      density = density,
    )
  }
  SideEffect {
    state.updateSnapPoints(currentSnapPoints)
  }
  return state
}

class DrawerState internal constructor(
  initialSnapPoint: DrawerSnapPoint,
  snapPoints: List<DrawerSnapPoint>,
  private val coroutineScope: CoroutineScope,
  private val density: Density,
) {
  init {
    checkValidSnapPoints(snapPoints)
    check(snapPoints.contains(initialSnapPoint)) {
      "The initialSnapPoint ${initialSnapPoint.identifier} was not part of the included " +
        "snapPoints while creating the drawer's state."
    }
  }

  internal var pendingSnapPointChange: Job? = null
  private var pendingTargetSnapPoint: DrawerSnapPoint? by mutableStateOf(null)
  internal var containerSizePx by mutableStateOf(Float.NaN)
  internal var panelSizePx by mutableStateOf(Float.NaN)
  internal var isAnchoredToMinEdge by mutableStateOf(false)
  internal var isDragging by mutableStateOf(false)

  internal val anchoredDraggableState = AnchoredDraggableState(initialValue = initialSnapPoint)

  private var innerSnapPoints: List<DrawerSnapPoint> by mutableStateOf(snapPoints)

  val snapPoints: List<DrawerSnapPoint>
    get() {
      return innerSnapPoints
    }

  internal fun updateSnapPoints(value: List<DrawerSnapPoint>) {
    checkValidSnapPoints(value)
    val currentOffset = anchoredDraggableState.offset
    val requestedTarget = if (currentOffset.isNaN()) {
      anchoredDraggableState.settledValue
    } else {
      targetSnapPoint
    }

    innerSnapPoints = value
    val closestTarget = if (currentOffset.isNaN().not() && value.contains(requestedTarget).not()) {
      createAnchors().closestAnchor(currentOffset) ?: value.first()
    } else {
      null
    }

    if (closestTarget == null) {
      updateAnchors()
      return
    }

    pendingTargetSnapPoint = closestTarget
    pendingSnapPointChange?.cancel()
    pendingSnapPointChange = coroutineScope.launch {
      try {
        anchoredDraggableState.anchoredDrag {
          updateAnchors(newTarget = closestTarget)
        }
        anchoredDraggableState.animateTo(closestTarget)
      } finally {
        if (pendingTargetSnapPoint == closestTarget) {
          pendingTargetSnapPoint = null
        }
      }
    }
  }

  val currentSnapPoint: DrawerSnapPoint
    get() {
      return anchoredDraggableState.settledValue
    }

  private val derivedTargetSnapPoint: DrawerSnapPoint by derivedStateOf {
    anchoredDraggableState.targetValue
  }

  var targetSnapPoint: DrawerSnapPoint
    get() = pendingTargetSnapPoint ?: derivedTargetSnapPoint
    set(value) {
      coroutineScope.launch {
        animateTo(value)
      }
    }

  val isIdle: Boolean by derivedStateOf {
    val currentPosition =
      anchoredDraggableState.anchors.positionOf(anchoredDraggableState.settledValue)
    val currentOffset = anchoredDraggableState.offset
    val isSettledAtCurrentSnapPoint =
      currentOffset.isNaN() ||
        currentPosition.isNaN() ||
        abs(currentOffset - currentPosition) < 0.5f

    pendingTargetSnapPoint == null &&
      isDragging.not() &&
      anchoredDraggableState.isAnimationRunning.not() &&
      isSettledAtCurrentSnapPoint
  }

  val offset: Float by derivedStateOf {
    visiblePanelSizePx()
  }

  fun progress(from: DrawerSnapPoint, to: DrawerSnapPoint): Float {
    return anchoredDraggableState.progress(from, to)
  }

  suspend fun animateTo(value: DrawerSnapPoint) {
    check(innerSnapPoints.contains(value)) {
      "Tried to set currentSnapPoint to an unknown snapPoint with identifier " +
        "${value.identifier}. Make sure that the snapPoint is passed to the list of " +
        "snapPoints when instantiating the drawer's state."
    }

    pendingTargetSnapPoint = value
    try {
      pendingSnapPointChange?.cancel()
      awaitAnchors()
      anchoredDraggableState.animateTo(value)
    } finally {
      if (pendingTargetSnapPoint == value) {
        pendingTargetSnapPoint = null
      }
    }
  }

  fun jumpTo(value: DrawerSnapPoint) {
    check(innerSnapPoints.contains(value)) {
      "Tried to set currentSnapPoint to an unknown snapPoint with identifier " +
        "${value.identifier}. Make sure that the snapPoint is passed to the list of " +
        "snapPoints when instantiating the drawer's state."
    }
    if (
      anchoredDraggableState.settledValue == value &&
      anchoredDraggableState.targetValue == value
    ) {
      return
    }

    pendingTargetSnapPoint = value
    pendingSnapPointChange?.cancel()
    pendingSnapPointChange = coroutineScope.launch {
      try {
        awaitAnchors()
        anchoredDraggableState.snapTo(value)
      } finally {
        if (pendingTargetSnapPoint == value) {
          pendingTargetSnapPoint = null
        }
      }
    }
  }

  internal fun updateContainerSize(
    measuredSizePx: Float,
    isAnchoredToMinEdge: Boolean,
  ) {
    if (
      containerSizePx == measuredSizePx &&
      this.isAnchoredToMinEdge == isAnchoredToMinEdge
    ) {
      return
    }

    containerSizePx = measuredSizePx
    this.isAnchoredToMinEdge = isAnchoredToMinEdge
    updateAnchors()
  }

  internal fun updatePanelSize(measuredSizePx: Float) {
    if (panelSizePx == measuredSizePx) return

    panelSizePx = measuredSizePx
    updateAnchors()
  }

  internal fun visiblePanelSizePx(): Float {
    if (containerSizePx.isNaN()) return 0f
    if (anchoredDraggableState.offset.isNaN()) return 0f

    val visiblePanelSizePx = if (isAnchoredToMinEdge) {
      anchoredDraggableState.offset
    } else {
      containerSizePx - anchoredDraggableState.offset
    }
    return visiblePanelSizePx.coerceIn(0f, containerSizePx)
  }

  internal fun targetVisiblePanelSizePx(): Float {
    return visibleSizeFor(targetSnapPoint)
  }

  private fun visibleSizeFor(snapPoint: DrawerSnapPoint): Float {
    if (containerSizePx.isNaN()) return Float.NaN

    return with(density) {
      val containerSize = containerSizePx.toDp()
      val panelSize = panelSizePx
        .takeIf { it.isNaN().not() }
        ?.toDp()
        ?: containerSize
      snapPoint.calculateVisibleSize(containerSize, panelSize)
        .takeIf { it.isSpecified }
        ?.coerceIn(0.dp, containerSize)
        ?.toPx()
        ?: 0f
    }
  }

  internal fun panelMainAxisOffsetPx(
    side: DrawerSide,
    viewportMainAxisSizePx: Float,
  ): Float {
    if (anchoredDraggableState.offset.isNaN()) {
      return if (side.isLeadingEdge) {
        0f
      } else {
        viewportMainAxisSizePx + hiddenTrailingEdgeOffsetPx(visiblePanelSizePx = 0f)
      }
    }

    return if (side.isLeadingEdge) {
      visiblePanelSizePx() - panelSizePx
    } else {
      val visiblePanelSizePx = visiblePanelSizePx()
      (viewportMainAxisSizePx - visiblePanelSizePx).coerceIn(0f, viewportMainAxisSizePx) +
        hiddenTrailingEdgeOffsetPx(visiblePanelSizePx)
    }
  }

  private fun hiddenTrailingEdgeOffsetPx(visiblePanelSizePx: Float): Float {
    if (visiblePanelSizePx > 0.5f) return 0f
    if (isIdle.not()) return 0f

    return panelSizePx.takeIf { it.isNaN().not() } ?: 0f
  }

  private fun updateAnchors(newTarget: DrawerSnapPoint? = null) {
    if (containerSizePx.isNaN() || panelSizePx.isNaN()) return

    val anchors = createAnchors()
    val requestedTarget = if (anchoredDraggableState.offset.isNaN()) {
      anchoredDraggableState.settledValue
    } else {
      targetSnapPoint
    }
    val resolvedTarget = newTarget
      ?: requestedTarget.takeIf { innerSnapPoints.contains(it) }
      ?: DrawerSnapPoint.Closed.takeIf { innerSnapPoints.contains(it) }
      ?: innerSnapPoints.first()
    anchoredDraggableState.updateAnchors(anchors, newTarget = resolvedTarget)
  }

  private fun createAnchors(): DraggableAnchors<DrawerSnapPoint> {
    return DraggableAnchors {
      with(density) {
        innerSnapPoints.forEach { snapPoint ->
          val containerSize = containerSizePx.toDp()
          val panelSize = panelSizePx.toDp()
          val snapPointSize = snapPoint.calculateVisibleSize(containerSize, panelSize)
            .takeIf { it.isSpecified }
            ?.coerceIn(0.dp, minOf(containerSize, panelSize))
            ?: 0.dp
          val offset = if (isAnchoredToMinEdge) {
            snapPointSize.toPx()
          } else {
            (containerSize - snapPointSize).toPx()
          }
          snapPoint at offset
        }
      }
    }
  }

  private suspend fun awaitAnchors() {
    if (anchoredDraggableState.offset.isNaN().not()) return

    snapshotFlow { anchoredDraggableState.offset.isNaN().not() }.first { it }
  }

  private fun checkValidSnapPoints(snapPoints: List<DrawerSnapPoint>) {
    check(snapPoints.isNotEmpty()) {
      "Tried to create a drawer without any snapPoints. Make sure to pass at least one " +
        "snapPoint when creating the drawer's state."
    }

    val duplicates =
      snapPoints.groupBy { it.identifier }.filter { it.value.size > 1 }.map { it.key }
    check(duplicates.isEmpty()) {
      "SnapPoint identifiers need to be unique, but you passed the following snapPoints " +
        "multiple times: ${duplicates.joinToString { it }}."
    }
  }
}

class DrawerScope internal constructor(
  internal val drawerState: DrawerState,
  internal val side: DrawerSide,
  internal val enabled: Boolean,
)

class DrawerViewportScope internal constructor()

class DrawerPanelScope internal constructor()

data class DrawerProperties(
  val dismissOnNavigateBack: Boolean = true,
)

private class DrawerContext(
  internal val state: DrawerState? = null,
  internal val side: DrawerSide = DrawerSide.Bottom,
  enabled: Boolean = true,
  internal val interactionSource: MutableInteractionSource? = null,
) {
  internal var enabled by mutableStateOf(enabled)
}

private val LocalDrawerContext: ProvidableCompositionLocal<DrawerContext> =
  compositionLocalOf { DrawerContext() }

@Composable
fun UnstyledDrawer(
  state: DrawerState,
  modifier: Modifier = Modifier,
  side: DrawerSide = DrawerSide.Start,
  enabled: Boolean = true,
  properties: DrawerProperties = DrawerProperties(),
  content: @Composable DrawerScope.() -> Unit,
) {
  if (
    enabled &&
    properties.dismissOnNavigateBack &&
    state.snapPoints.contains(DrawerSnapPoint.Closed) &&
    (
      state.currentSnapPoint != DrawerSnapPoint.Closed ||
        state.targetSnapPoint != DrawerSnapPoint.Closed
      )
  ) {
    EscapeHandler {
      state.targetSnapPoint = DrawerSnapPoint.Closed
    }
  }

  Box(modifier) {
    val drawerScope = remember(state, side, enabled) {
      DrawerScope(
        drawerState = state,
        side = side,
        enabled = enabled,
      )
    }
    drawerScope.content()
  }
}

@Composable
fun DrawerScope.Viewport(
  modifier: Modifier = Modifier,
  content: @Composable DrawerViewportScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val context = remember(drawerState, side, interactionSource) {
    DrawerContext(
      state = drawerState,
      side = side,
      enabled = enabled,
      interactionSource = interactionSource,
    )
  }
  SideEffect {
    context.enabled = enabled
  }
  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      when (interaction) {
        is androidx.compose.foundation.interaction.DragInteraction.Start ->
          drawerState.isDragging = true
        is androidx.compose.foundation.interaction.DragInteraction.Stop,
        is androidx.compose.foundation.interaction.DragInteraction.Cancel,
        -> drawerState.isDragging = false
      }
    }
  }
  Layout(
    modifier = modifier,
    content = {
      CompositionLocalProvider(LocalDrawerContext provides context) {
        DrawerViewportScope().content()
      }
    },
  ) { measurables, constraints ->
    val childConstraints = constraints.copy(
      minWidth = 0,
      minHeight = 0,
    )
    val placeables = measurables.map { measurable ->
      measurable.measure(childConstraints)
    }

    val width = maxOf(
      constraints.minWidth,
      placeables.maxOfOrNull { placeable -> placeable.width } ?: 0,
    ).coerceIn(constraints.minWidth, constraints.maxWidth)
    val height = maxOf(
      constraints.minHeight,
      placeables.maxOfOrNull { placeable -> placeable.height } ?: 0,
    ).coerceIn(constraints.minHeight, constraints.maxHeight)
    val side = context.side
    val containerMainAxisSizePx = if (side.isHorizontal) {
      width.toFloat()
    } else {
      height.toFloat()
    }
    drawerState.updateContainerSize(
      measuredSizePx = containerMainAxisSizePx,
      isAnchoredToMinEdge = side.isLeadingEdge,
    )

    layout(width, height) {
      val panelMainAxisOffset =
        drawerState.panelMainAxisOffsetPx(side, containerMainAxisSizePx).roundToInt()
      val trailingEdgeOverflow = drawerState.trailingEdgeOverflowPx(containerMainAxisSizePx)
        .roundToInt()
      placeables.forEach { placeable ->
        if (side.isHorizontal) {
          val coercionOffset = placeable.mainAxisCoercionOffset(side)
          val x = if (side.isLeadingEdge) {
            drawerState.visiblePanelSizePx().roundToInt() - placeable.width
          } else {
            panelMainAxisOffset - maxOf(coercionOffset, trailingEdgeOverflow)
          }
          placeable.placeRelative(x, 0)
        } else {
          val coercionOffset = placeable.mainAxisCoercionOffset(side)
          val y = if (side.isLeadingEdge) {
            drawerState.visiblePanelSizePx().roundToInt() - placeable.height
          } else {
            panelMainAxisOffset - maxOf(coercionOffset, trailingEdgeOverflow)
          }
          placeable.placeRelative(0, y)
        }
      }
    }
  }
}

@Composable
fun DrawerViewportScope.Panel(
  modifier: Modifier = Modifier,
  overscrollEffect: OverscrollEffect? = null,
  content: @Composable DrawerPanelScope.() -> Unit,
) {
  val context = LocalDrawerContext.current
  val state = context.state
  val side = context.side
  val orientation = if (side.isHorizontal) {
    Orientation.Horizontal
  } else {
    Orientation.Vertical
  }
  val panelOverscrollEffect = remember(overscrollEffect) {
    overscrollEffect?.withoutVisualEffect()
  }
  val panelOverscrollVisualEffect = remember(overscrollEffect) {
    overscrollEffect?.withoutEventHandling()
  }
  val panelContainerMainAxisSize = state?.containerSizePx
    ?.takeIf { it.isNaN().not() }
    ?.roundToInt()
  Layout(
    modifier = buildModifier {
      if (panelOverscrollVisualEffect != null) {
        add(Modifier.overscroll(panelOverscrollVisualEffect))
      }
    }.then(
      buildModifier {
        if (
          state != null &&
          context.enabled &&
          (state.snapPoints.size > 1 || panelOverscrollEffect != null)
        ) {
          add(
            Modifier
              .anchoredDraggable(
                state = state.anchoredDraggableState,
                orientation = orientation,
                enabled = context.enabled,
                interactionSource = context.interactionSource,
                overscrollEffect = panelOverscrollEffect,
              )
              .nestedScroll(
                remember(state.anchoredDraggableState, orientation, side) {
                  ConsumeSwipeWithinDrawerBoundsNestedScrollConnection(
                    drawerState = state.anchoredDraggableState,
                    orientation = orientation,
                    side = side,
                  )
                },
              ),
          )
        }
      },
    ),
    content = {
      PanelContentLayout(
        modifier = modifier,
        side = side,
        containerMainAxisSize = panelContainerMainAxisSize,
        targetSnapPoint = state?.targetSnapPoint,
        targetVisibleMainAxisSize = state?.targetVisiblePanelSizePx()
          ?.takeIf { it.isNaN().not() }
          ?.roundToInt(),
        onMainAxisSizeMeasured = { measuredSize ->
          state?.updatePanelSize(measuredSize.toFloat())
        },
      ) {
        DrawerPanelScope().content()
      }
    },
  ) { measurables, constraints ->
    val containerMainAxisSize = state?.containerSizePx
      ?.takeIf { it.isNaN().not() }
      ?.roundToInt()
    val canUseContainerMax = containerMainAxisSize != null &&
      state.panelSizePx.isNaN().not() &&
      state.panelSizePx <= containerMainAxisSize
    val contentConstraints = if (side.isHorizontal) {
      val maxWidth = constraints.maxWidth.takeIf { maxWidth ->
        maxWidth != Constraints.Infinity
      } ?: containerMainAxisSize.takeIf { canUseContainerMax }
        ?: Constraints.Infinity
      constraints.copy(minWidth = 0, maxWidth = maxWidth)
    } else {
      val maxHeight = constraints.maxHeight.takeIf { maxHeight ->
        maxHeight != Constraints.Infinity
      } ?: containerMainAxisSize.takeIf { canUseContainerMax }
        ?: Constraints.Infinity
      constraints.copy(minHeight = 0, maxHeight = maxHeight)
    }
    val placeables = measurables.map { measurable ->
      measurable.measure(contentConstraints)
    }
    val coercedPanelMainAxisSize = placeables.maxOfOrNull { placeable ->
      placeable.mainAxisSize(side)
    } ?: 0
    val panelMainAxisSize = state?.panelSizePx
      ?.takeIf { it.isNaN().not() }
      ?.roundToInt()
      ?: coercedPanelMainAxisSize
    state?.updatePanelSize(panelMainAxisSize.toFloat())

    val width = if (side.isHorizontal) {
      maxOf(
        panelMainAxisSize,
        placeables.maxOfOrNull { it.width } ?: 0,
      )
    } else {
      maxOf(
        constraints.minWidth,
        placeables.maxOfOrNull { it.width } ?: 0,
      )
    }
    val height = if (side.isHorizontal) {
      maxOf(
        constraints.minHeight,
        placeables.maxOfOrNull { it.height } ?: 0,
      )
    } else {
      maxOf(
        panelMainAxisSize,
        placeables.maxOfOrNull { it.height } ?: 0,
      )
    }

    layout(width, height) {
      placeables.forEach { placeable ->
        val x = if (side.isHorizontal && side.isLeadingEdge.not()) {
          width - placeable.measuredWidth
        } else {
          0
        }
        val y = if (side.isHorizontal.not() && side.isLeadingEdge.not()) {
          height - placeable.height
        } else {
          0
        }
        placeable.placeRelative(x, y)
      }
    }
  }
}

@Composable
private fun PanelContentLayout(
  modifier: Modifier,
  side: DrawerSide,
  containerMainAxisSize: Int?,
  targetSnapPoint: DrawerSnapPoint?,
  targetVisibleMainAxisSize: Int?,
  onMainAxisSizeMeasured: (Int) -> Unit,
  content: @Composable () -> Unit,
) {
  Layout(
    modifier = modifier,
    content = content,
  ) { measurables, constraints ->
    val mainAxisMinSize: Int
    val mainAxisMaxSize: Int
    if (side.isHorizontal) {
      mainAxisMinSize = constraints.minWidth
      mainAxisMaxSize = constraints.maxWidth
    } else {
      mainAxisMinSize = constraints.minHeight
      mainAxisMaxSize = constraints.maxHeight
    }
    val fixedMainAxisSize = mainAxisMinSize == mainAxisMaxSize
    val explicitlyConstrainedMainAxis = containerMainAxisSize != null &&
      mainAxisMaxSize != Constraints.Infinity &&
      mainAxisMaxSize < containerMainAxisSize
    val shouldBoundToVisibleSize = containerMainAxisSize != null &&
      targetSnapPoint != DrawerSnapPoint.Open &&
      targetVisibleMainAxisSize != null &&
      targetVisibleMainAxisSize > 0 &&
      targetVisibleMainAxisSize < containerMainAxisSize
    val childConstraints = if (fixedMainAxisSize) {
      constraints
    } else if (containerMainAxisSize == null && mainAxisMaxSize != Constraints.Infinity) {
      constraints
    } else if (explicitlyConstrainedMainAxis) {
      constraints
    } else if (shouldBoundToVisibleSize && side.isHorizontal) {
      constraints.copy(
        minWidth = 0,
        maxWidth = targetVisibleMainAxisSize,
      )
    } else if (shouldBoundToVisibleSize) {
      constraints.copy(
        minHeight = 0,
        maxHeight = targetVisibleMainAxisSize,
      )
    } else if (side.isHorizontal) {
      constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
    } else {
      constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
    }
    val effectiveChildConstraints = if (
      side.isHorizontal &&
      childConstraints.maxWidth == Constraints.Infinity
    ) {
      childConstraints.copy(
        maxWidth = containerMainAxisSize
          ?: constraints.maxWidth.takeIf { it != Constraints.Infinity }
          ?: Constraints.Infinity,
      )
    } else if (side.isHorizontal.not() && childConstraints.maxHeight == Constraints.Infinity) {
      childConstraints.copy(
        maxHeight = containerMainAxisSize
          ?: constraints.maxHeight.takeIf { it != Constraints.Infinity }
          ?: Constraints.Infinity,
      )
    } else {
      childConstraints
    }
    val placeables = measurables.map { measurable ->
      measurable.measure(effectiveChildConstraints)
    }
    val width = if (side.isHorizontal && fixedMainAxisSize.not()) {
      placeables.maxOfOrNull { placeable -> placeable.measuredWidth } ?: constraints.minWidth
    } else {
      maxOf(
        constraints.minWidth,
        placeables.maxOfOrNull { placeable -> placeable.width } ?: 0,
      )
    }
    val height = if (side.isHorizontal.not() && fixedMainAxisSize.not()) {
      placeables.maxOfOrNull { placeable -> placeable.measuredHeight } ?: constraints.minHeight
    } else {
      maxOf(
        constraints.minHeight,
        placeables.maxOfOrNull { placeable -> placeable.height } ?: 0,
      )
    }
    onMainAxisSizeMeasured(
      if (side.isHorizontal) {
        width
      } else {
        height
      },
    )

    layout(width, height) {
      placeables.forEach { placeable ->
        placeable.placeRelative(0, 0)
      }
    }
  }
}

private fun Placeable.mainAxisSize(side: DrawerSide): Int {
  return if (side.isHorizontal) {
    measuredWidth
  } else {
    measuredHeight
  }
}

private fun Placeable.mainAxisCoercionOffset(side: DrawerSide): Int {
  // Compose centers placeables that exceed parent constraints.
  // Trailing-edge drawers need to cancel that center offset to stay edge-aligned.
  return if (side.isHorizontal) {
    measuredWidth - width
  } else {
    measuredHeight - height
  }.coerceAtLeast(0) / 2
}

private fun DrawerState.trailingEdgeOverflowPx(viewportMainAxisSizePx: Float): Float {
  if (isAnchoredToMinEdge) return 0f

  val panelOverflow = panelSizePx - viewportMainAxisSizePx
  if (panelOverflow <= 0f) return 0f

  return if (abs(visiblePanelSizePx() - viewportMainAxisSizePx) < 0.5f) {
    panelOverflow
  } else {
    0f
  }
}

private fun ConsumeSwipeWithinDrawerBoundsNestedScrollConnection(
  drawerState: AnchoredDraggableState<DrawerSnapPoint>,
  orientation: Orientation,
  side: DrawerSide,
): NestedScrollConnection = object : NestedScrollConnection {
  override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
    val delta = available.toFloat(orientation)
    return if (source == NestedScrollSource.UserInput && side.isOpeningDelta(delta)) {
      drawerState.dispatchRawDelta(delta).toOffset(orientation)
    } else {
      Offset.Zero
    }
  }

  override fun onPostScroll(
    consumed: Offset,
    available: Offset,
    source: NestedScrollSource,
  ): Offset {
    return if (source == NestedScrollSource.UserInput) {
      drawerState.dispatchRawDelta(available.toFloat(orientation)).toOffset(orientation)
    } else {
      Offset.Zero
    }
  }

  override suspend fun onPreFling(available: Velocity): Velocity {
    val velocity = available.toFloat(orientation)
    val currentOffset = drawerState.requireOffset()
    val minAnchor = drawerState.anchors.minPosition()
    val maxAnchor = drawerState.anchors.maxPosition()
    val canMoveTowardOpen = if (side.isLeadingEdge) {
      currentOffset < maxAnchor
    } else {
      currentOffset > minAnchor
    }
    return if (side.isOpeningDelta(velocity) && canMoveTowardOpen) {
      drawerState.settle(AnchoredDraggableDefaults.SnapAnimationSpec)
      available
    } else {
      Velocity.Zero
    }
  }

  override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
    drawerState.settle(AnchoredDraggableDefaults.SnapAnimationSpec)
    return available
  }
}

private fun DrawerSide.isOpeningDelta(delta: Float): Boolean {
  return if (isLeadingEdge) {
    delta > 0
  } else {
    delta < 0
  }
}

private fun Float.toOffset(orientation: Orientation): Offset {
  return Offset(
    x = if (orientation == Orientation.Horizontal) this else 0f,
    y = if (orientation == Orientation.Vertical) this else 0f,
  )
}

@JvmName("velocityToFloat")
private fun Velocity.toFloat(orientation: Orientation): Float {
  return if (orientation == Orientation.Horizontal) x else y
}

@JvmName("offsetToFloat")
private fun Offset.toFloat(orientation: Orientation): Float {
  return if (orientation == Orientation.Horizontal) x else y
}

private val DrawerSide.isHorizontal: Boolean
  get() {
    return this == DrawerSide.Start || this == DrawerSide.End
  }

private val DrawerSide.isLeadingEdge: Boolean
  get() {
    return this == DrawerSide.Top || this == DrawerSide.Start
  }
