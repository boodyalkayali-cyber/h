@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.drx.casefiles.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.drx.casefiles.R
import com.drx.casefiles.data.Media
import com.drx.casefiles.data.Repo
import com.drx.casefiles.data.Stage
import com.drx.casefiles.data.sideShort
import com.drx.casefiles.data.stageText
import com.drx.casefiles.data.viewText
import kotlin.math.abs

val HealthyGreen = Color(0xFF2E9E5B)

// ---------------------------------------------------------------- zoom

class ZoomState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }
}

/**
 * Pinch / double-tap zoom and pan. When [onSwipeUp] / [onSwipeDown] are given, a vertical
 * one-finger swipe at scale 1 triggers them; horizontal swipes are left to the parent pager.
 */
@Composable
fun ZoomableImage(
    model: Any,
    state: ZoomState,
    modifier: Modifier = Modifier,
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val up by rememberUpdatedState(onSwipeUp)
    val down by rememberUpdatedState(onSwipeDown)
    Box(
        modifier
            .onSizeChanged { boxSize = it }
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (state.scale > 1f) state.reset() else state.scale = 2.5f
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var multi = false
                    var dx = 0f
                    var dy = 0f
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1) multi = true
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        if (multi || state.scale > 1f) {
                            val ns = (state.scale * zoom).coerceIn(1f, 6f)
                            state.scale = ns
                            if (ns > 1f) {
                                val maxX = boxSize.width * (ns - 1f) / 2f
                                val maxY = boxSize.height * (ns - 1f) / 2f
                                val o = state.offset + pan
                                state.offset = Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
                            } else {
                                state.offset = Offset.Zero
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else {
                            dx += pan.x
                            dy += pan.y
                            if ((up != null || down != null) && abs(dy) > 24f && abs(dy) > abs(dx) * 1.5f) {
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    if (!multi && state.scale <= 1f && abs(dy) > 140f && abs(dy) > abs(dx) * 1.5f) {
                        if (dy < 0f) up?.invoke() else down?.invoke()
                    }
                }
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offset.x
                    translationY = state.offset.y
                }
        )
    }
}

// ---------------------------------------------------------------- thumbnails and grid

@Composable
fun Tag(text: String, modifier: Modifier = Modifier, bg: Color = Color.Black.copy(alpha = 0.6f)) {
    Text(
        text,
        color = Color.White,
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

@Composable
fun Thumb(
    m: Media,
    repo: Repo,
    selected: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: () -> Unit,
    onLong: () -> Unit
) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.DarkGray)
            .combinedClickable(onClick = onClick, onLongClick = onLong)
    ) {
        AsyncImage(
            model = repo.file(m.path),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (m.healthy) {
            Tag(stringResource(R.string.healthy_short), Modifier.align(Alignment.TopStart).padding(3.dp), HealthyGreen)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val sv = listOf(sideShort(m.side), viewText(m.viewPos)).filter { it.isNotBlank() }.joinToString(" ")
            if (sv.isNotBlank()) Tag(sv)
            if (m.stage.isNotBlank()) Tag(stageText(m.stage))
            if (!label.isNullOrBlank()) Tag(label)
        }
        if (selected) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Check, null, tint = Color.White) }
        }
    }
}

private sealed interface Cell {
    data class Item(val m: Media) : Cell
    data class More(val n: Int) : Cell
}

/** Grid inside a scrolling column. With [maxRows] the last tile becomes a "+N more" tile. */
@Composable
fun MediaGrid(
    items: List<Media>,
    repo: Repo,
    selected: Set<Long>,
    maxRows: Int?,
    onExpand: () -> Unit,
    onClick: (Media) -> Unit,
    onLong: (Media) -> Unit,
    columns: Int = 3
) {
    val cap = if (maxRows != null) maxRows * columns else null
    val cells: List<Cell> =
        if (cap != null && items.size > cap) {
            items.take(cap - 1).map { Cell.Item(it) } + Cell.More(items.size - (cap - 1))
        } else {
            items.map { Cell.Item(it) }
        }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { c ->
                    when (c) {
                        is Cell.Item -> Thumb(
                            c.m, repo, c.m.id in selected, Modifier.weight(1f),
                            onClick = { onClick(c.m) }, onLong = { onLong(c.m) }
                        )
                        is Cell.More -> Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .combinedClickable(onClick = onExpand),
                            contentAlignment = Alignment.Center
                        ) { Text(stringResource(R.string.more_fmt, c.n), style = MaterialTheme.typography.titleSmall) }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ---------------------------------------------------------------- dialogs and fields

@Composable
fun ConfirmDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun TextInputDialog(title: String, label: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var v by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(v, { v = it }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { if (v.isNotBlank()) onConfirm(v.trim()) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Options are (key, label). With [customHint] the user may also type a free-text key. */
@Composable
fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    customHint: String? = null,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var custom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (key, label) ->
                    TextButton(onClick = { onPick(key) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
                if (customHint != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            custom, { custom = it }, singleLine = true,
                            label = { Text(customHint) }, modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { if (custom.isNotBlank()) onPick(custom.trim()) }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun stageOptions(): List<Pair<String, String>> =
    listOf("" to stringResource(R.string.stage_none)) + Stage.values().map { it.name to stringResource(it.label) }

/** Text field where Enter moves to the next field instead of adding a new line. */
@Composable
fun FormField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    last: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    val fm = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = { v ->
            val typedEnter = v.length == value.length + 1 && v.contains('\n') && v.replace("\n", "") == value
            if (typedEnter) {
                if (last) fm.clearFocus() else fm.moveFocus(FocusDirection.Next)
            } else {
                onChange(v)
            }
        },
        label = { Text(label) },
        maxLines = 5,
        keyboardOptions = KeyboardOptions(imeAction = if (last) ImeAction.Done else ImeAction.Next, keyboardType = keyboard),
        keyboardActions = KeyboardActions(
            onNext = { fm.moveFocus(FocusDirection.Next) },
            onDone = { fm.clearFocus() }
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun <T> ChipRow(
    items: List<T>,
    isSelected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onClick: (T) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            FilterChip(selected = isSelected(item), onClick = { onClick(item) }, label = { Text(label(item)) })
        }
    }
}
