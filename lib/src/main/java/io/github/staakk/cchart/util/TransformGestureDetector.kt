package io.github.staakk.cchart.util

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

/**
 * Adapted from [androidx.compose.foundation.gestures.detectTransformGestures].
 * Provides zooming direction as positive unit vector instead of rotation.
 */
