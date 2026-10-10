@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.drx.casefiles.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drx.casefiles.R
import com.drx.casefiles.data.Category
import com.drx.casefiles.data.Media
import com.drx.casefiles.data.Region
import com.drx.casefiles.data.Repo
import com.drx.casefiles.data.Side
import com.drx.casefiles.data.Stage
import com.drx.casefiles.data.ViewPos
import com.drx.casefiles.data.categoryText
import com.drx.casefiles.data.enumOrNull
import com.drx.casefiles.data.regionText
import com.drx.casefiles.data.sideShort
import com.drx.casefiles.data.stageText
import com.drx.casefiles.data.viewText
import com.drx.casefiles.data.visitLabels
import kotlinx.coroutines.launch

@Composable
fun ViewerScreen(initial: List<Media>, startIndex: Int, repo: Repo, nav: Nav) {
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<Media>().also { it.addAll(initial) } }
    val pager = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (initial.size - 1).coerceAtLeast(0)),
        pageCount = { items.size }
    )
    var panel by remember { mutableStateOf(false) }
    var zoomed by remember { mutableStateOf(false) }
    var showStage by remember { mutableStateOf(false) }
    var showView by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    fun update(f: (Media) -> Media) {
        val i = pager.currentPage
        val cur = items.getOrNull(i) ?: return
        val n = f(cur)
        items[i] = n
        scope.launch { repo.dao.updateMedia(n) }
    }

    LaunchedEffect(items.size) { if (items.isEmpty()) nav.pop() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (items.isNotEmpty()) {
            HorizontalPager(
                state = pager,
                userScrollEnabled = !zoomed,
                modifier = Modifier.fillMaxSize(),
                key = { items.getOrNull(it)?.id ?: it }
            ) { page ->
                val m = items.getOrNull(page)
                if (m != null) {
                    val zs = remember(m.id) { ZoomState() }
                    LaunchedEffect(zs.scale, pager.currentPage) {
                        if (pager.currentPage == page) zoomed = zs.scale > 1.01f
                    }
                    ZoomableImage(
                        model = repo.file(m.path),
                        state = zs,
                        modifier = Modifier.fillMaxSize(),
                        onSwipeUp = { panel = true },
                        onSwipeDown = { if (panel) panel = false else nav.pop() }
                    )
                }
            }

            val cur = items.getOrNull(pager.currentPage)
            if (cur != null) {
                FlowRow(
                    Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (cur.tag.isNotBlank()) Tag(cur.tag)
                    val info = listOf(regionText(cur.region), sideShort(cur.side), viewText(cur.viewPos))
                        .filter { it.isNotBlank() }.joinToString(" ")
                    if (info.isNotBlank()) Tag(info)
                    if (cur.stage.isNotBlank()) Tag(stageText(cur.stage))
                    if (cur.healthy) Tag(stringResource(R.string.healthy), bg = HealthyGreen)
                }

                if (!panel && cur.caption.isNotBlank()) {
                    Text(
                        cur.caption,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .navigationBarsPadding()
                            .padding(12.dp)
                    )
                }

                AnimatedVisibility(visible = panel, modifier = Modifier.align(Alignment.BottomCenter)) {
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
                        Column(
                            Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(cur.stage.isNotBlank(), { showStage = true }, { Text(stageText(cur.stage)) })
                                FilterChip(
                                    cur.viewPos.isNotBlank(), { showView = true },
                                    { Text(if (cur.viewPos.isBlank()) stringResource(R.string.view_none) else viewText(cur.viewPos)) }
                                )
                                FilterChip(cur.healthy, { update { it.copy(healthy = !it.healthy) } }, { Text(stringResource(R.string.healthy)) })
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, null) }
                            }
                            OutlinedTextField(
                                value = cur.caption,
                                onValueChange = { v -> update { it.copy(caption = v) } },
                                placeholder = { Text(stringResource(R.string.caption_hint)) },
                                minLines = 3,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStage) {
        ChoiceDialog(
            stringResource(R.string.stage), stageOptions(), stringResource(R.string.custom_stage),
            onPick = { s -> showStage = false; update { it.copy(stage = s) } },
            onDismiss = { showStage = false }
        )
    }
    if (showView) {
        val opts = listOf("" to stringResource(R.string.view_none)) +
            ViewPos.values().map { it.name to stringResource(it.label) }
        ChoiceDialog(
            stringResource(R.string.view_none), opts,
            onPick = { v -> showView = false; update { it.copy(viewPos = v) } },
            onDismiss = { showView = false }
        )
    }
    if (showDelete) {
        ConfirmDialog(
            stringResource(R.string.delete_media_q),
            onConfirm = {
                showDelete = false
                val i = pager.currentPage
                val cur = items.getOrNull(i)
                if (cur != null) {
                    items.removeAt(i)
                    scope.launch { repo.deleteMedia(listOf(cur)) }
                }
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
fun SearchScreen(repo: Repo, nav: Nav) {
    val ctx = LocalContext.current
    val media by remember { repo.dao.allMedia() }.collectAsState(emptyList())
    val patients by remember { repo.dao.patients() }.collectAsState(emptyList())
    val visits by remember { repo.dao.allVisits() }.collectAsState(emptyList())

    var q by remember { mutableStateOf("") }
    var region by remember { mutableStateOf<String?>(null) }
    var side by remember { mutableStateOf<String?>(null) }
    var cat by remember { mutableStateOf<String?>(null) }
    var stage by remember { mutableStateOf<String?>(null) }
    var healthyOnly by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }

    val pMap = remember(patients) { patients.associateBy { it.id } }
    val vLabels = remember(visits) { visitLabels(ctx, visits) }
    val results = remember(media, q, region, side, cat, stage, healthyOnly, pMap) {
        val tokens = q.trim().lowercase().split(" ").filter { it.isNotBlank() }
        media.filter { m ->
            val hay = (m.tag + " " + (pMap[m.patientId]?.name ?: "") + " " + m.caption + " " + m.stage).lowercase()
            tokens.all { hay.contains(it) } &&
                (region == null || m.region == region) &&
                (side == null || m.side == side) &&
                (cat == null || m.category == cat) &&
                (stage == null || m.stage == stage) &&
                (!healthyOnly || m.healthy)
        }
    }
    val selMedia = results.filter { it.id in selected }
    fun labelOf(m: Media) = "${pMap[m.patientId]?.name.orEmpty()} · ${vLabels[m.visitId].orEmpty()}"

    Scaffold(
        topBar = {
            if (selected.isEmpty()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.results_fmt, results.size)) },
                    navigationIcon = { IconButton(onClick = { nav.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                    actions = { IconButton(onClick = { showFilters = !showFilters }) { Text("⚙", fontSize = 20.sp) } }
                )
            } else {
                TopAppBar(
                    title = { Text("${selected.size}") },
                    navigationIcon = { IconButton(onClick = { selected = emptySet() }) { Icon(Icons.Default.Close, null) } },
                    actions = {
                        if (selMedia.size == 2) {
                            IconButton(onClick = {
                                nav.push(Screen.Compare(selMedia[0], selMedia[1], labelOf(selMedia[0]), labelOf(selMedia[1])))
                            }) { Text("↔", fontSize = 20.sp) }
                        }
                    }
                )
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                q, { q = it }, singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                modifier = Modifier.fillMaxWidth().padding(12.dp, 4.dp)
            )
            if (showFilters) {
                Column(
                    Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(stringResource(R.string.region), style = MaterialTheme.typography.labelMedium)
                    ChipRow(
                        listOf<String?>(null) + Region.values().map { it.name },
                        { it == region },
                        { if (it == null) stringResource(R.string.all) else regionText(it) },
                        { region = if (region == it) null else it }
                    )
                    Text(stringResource(R.string.side), style = MaterialTheme.typography.labelMedium)
                    ChipRow(
                        listOf<String?>(null, "L", "R"),
                        { it == side },
                        { if (it == null) stringResource(R.string.all) else it },
                        { side = if (side == it) null else it }
                    )
                    ChipRow(
                        listOf<String?>(null) + Category.values().map { it.name },
                        { it == cat },
                        { if (it == null) stringResource(R.string.all) else categoryText(it) },
                        { cat = if (cat == it) null else it }
                    )
                    Text(stringResource(R.string.stage), style = MaterialTheme.typography.labelMedium)
                    ChipRow(
                        listOf<String?>(null) + Stage.values().map { it.name },
                        { it == stage },
                        { if (it == null) stringResource(R.string.all) else stageText(it) },
                        { stage = if (stage == it) null else it }
                    )
                    FilterChip(healthyOnly, { healthyOnly = !healthyOnly }, { Text(stringResource(R.string.healthy_only)) })
                }
            }
            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_results)) }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(results, key = { it.id }) { m ->
                        Thumb(
                            m, repo, m.id in selected,
                            label = pMap[m.patientId]?.name,
                            onClick = {
                                if (selected.isNotEmpty()) {
                                    selected = if (m.id in selected) selected - m.id else selected + m.id
                                } else {
                                    nav.push(Screen.Viewer(results, results.indexOf(m)))
                                }
                            },
                            onLong = { selected = if (m.id in selected) selected - m.id else selected + m.id }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparePane(m: Media, label: String, zs: ZoomState, repo: Repo, modifier: Modifier) {
    Box(modifier.background(Color(0xFF111111))) {
        ZoomableImage(repo.file(m.path), zs, Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.TopStart).padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Tag(label)
            if (m.healthy) Tag(stringResource(R.string.healthy), bg = HealthyGreen)
            val info = listOf(sideShort(m.side), viewText(m.viewPos)).filter { it.isNotBlank() }.joinToString(" ")
            if (info.isNotBlank()) Tag(info)
            if (m.stage.isNotBlank()) Tag(stageText(m.stage))
            if (m.tag.isNotBlank()) Tag(m.tag)
        }
    }
}

@Composable
fun CompareScreen(a: Media, b: Media, la: String, lb: String, repo: Repo, nav: Nav) {
    var sync by remember { mutableStateOf(true) }
    var vertical by remember { mutableStateOf(false) }
    val shared = remember { ZoomState() }
    val za = remember { ZoomState() }
    val zb = remember { ZoomState() }
    val sa = if (sync) shared else za
    val sb = if (sync) shared else zb

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.pop() }) { Icon(Icons.Default.Close, null, tint = Color.White) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.sync_zoom), color = Color.White)
            Switch(checked = sync, onCheckedChange = { sync = it })
            TextButton(onClick = { vertical = !vertical }) { Text("⇅", color = Color.White, fontSize = 20.sp) }
        }
        if (vertical) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ComparePane(a, la, sa, repo, Modifier.weight(1f).fillMaxWidth())
                ComparePane(b, lb, sb, repo, Modifier.weight(1f).fillMaxWidth())
            }
        } else {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ComparePane(a, la, sa, repo, Modifier.weight(1f).fillMaxSize())
                ComparePane(b, lb, sb, repo, Modifier.weight(1f).fillMaxSize())
            }
        }
    }
}
