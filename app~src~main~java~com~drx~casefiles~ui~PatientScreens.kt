@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.drx.casefiles.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.drx.casefiles.R
import com.drx.casefiles.data.Media
import com.drx.casefiles.data.Patient
import com.drx.casefiles.data.Repo
import com.drx.casefiles.data.Visit
import com.drx.casefiles.data.dayNumber
import com.drx.casefiles.data.visitLabels
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(repo: Repo, nav: Nav) {
    val scope = rememberCoroutineScope()
    val patients by remember { repo.dao.patients() }.collectAsState(emptyList())
    val media by remember { repo.dao.allMedia() }.collectAsState(emptyList())
    var q by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Patient?>(null) }

    val thumbs = remember(media) { media.groupBy { it.patientId }.mapValues { it.value.first() } }
    val filtered = patients.filter {
        q.isBlank() || it.name.contains(q, true) || it.phone.contains(q, true) || it.complaint.contains(q, true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { nav.push(Screen.Search) }) { Icon(Icons.Default.Search, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null) }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                q, { q = it }, singleLine = true,
                placeholder = { Text(stringResource(R.string.search_patients)) },
                modifier = Modifier.fillMaxWidth().padding(12.dp, 4.dp)
            )
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_patients), textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
                }
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { nav.push(Screen.PatientDetail(p.id)) },
                                    onLongClick = { toDelete = p }
                                )
                                .padding(12.dp, 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                val t = thumbs[p.id]
                                if (t != null) {
                                    AsyncImage(repo.file(t.path), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(p.name, style = MaterialTheme.typography.titleMedium)
                                if (p.complaint.isNotBlank()) {
                                    Text(p.complaint, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAdd) {
        TextInputDialog(
            stringResource(R.string.new_patient), stringResource(R.string.patient_name),
            onConfirm = { name ->
                showAdd = false
                scope.launch {
                    val id = repo.dao.insertPatient(Patient(name = name))
                    repo.dao.insertVisit(Visit(patientId = id, kind = "INITIAL"))
                    nav.push(Screen.PatientDetail(id))
                }
            },
            onDismiss = { showAdd = false }
        )
    }
    toDelete?.let { p ->
        ConfirmDialog(
            stringResource(R.string.delete_patient_q),
            onConfirm = { toDelete = null; scope.launch { repo.deletePatient(p) } },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
fun PatientScreen(id: Long, repo: Repo, nav: Nav) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbPatient by remember(id) { repo.dao.patient(id) }.collectAsState(null)
    val visits by remember(id) { repo.dao.visits(id) }.collectAsState(emptyList())
    val media by remember(id) { repo.dao.mediaOfPatientFlow(id) }.collectAsState(emptyList())

    var f by remember { mutableStateOf<Patient?>(null) }
    var dirty by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Visit?>(null) }

    LaunchedEffect(dbPatient) { if (f == null && dbPatient != null) f = dbPatient }
    LaunchedEffect(f) {
        if (dirty) {
            delay(600)
            f?.let { repo.dao.updatePatient(it.copy(updatedAt = System.currentTimeMillis())) }
            dirty = false
        }
    }
    val latest by rememberUpdatedState(f)
    val latestDirty by rememberUpdatedState(dirty)
    DisposableEffect(Unit) {
        onDispose {
            if (latestDirty) latest?.let { repo.appScope.launch { repo.dao.updatePatient(it) } }
        }
    }
    fun edit(n: Patient) {
        f = n
        dirty = true
    }

    val labels = remember(visits) { visitLabels(ctx, visits) }
    val dateFmt = remember { DateFormat.getDateInstance() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(f?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { nav.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { pad ->
        val p = f
        if (p == null) {
            Box(Modifier.padding(pad).fillMaxSize())
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormField(stringResource(R.string.patient_name), p.name, Modifier.weight(2f)) { edit(p.copy(name = it)) }
                        FormField(stringResource(R.string.age), p.age, Modifier.weight(1f)) { edit(p.copy(age = it)) }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormField(stringResource(R.string.residence), p.residence, Modifier.weight(1f)) { edit(p.copy(residence = it)) }
                        FormField(stringResource(R.string.phone), p.phone, Modifier.weight(1f)) { edit(p.copy(phone = it)) }
                    }
                }
                item { Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                item { FormField(stringResource(R.string.complaint), p.complaint) { edit(p.copy(complaint = it)) } }
                item { FormField(stringResource(R.string.pmh), p.pmh) { edit(p.copy(pmh = it)) } }
                item { FormField(stringResource(R.string.psh), p.psh) { edit(p.copy(psh = it)) } }
                item { FormField(stringResource(R.string.drugs), p.drugs) { edit(p.copy(drugs = it)) } }
                item { FormField(stringResource(R.string.allergy), p.allergy) { edit(p.copy(allergy = it)) } }
                item { FormField(stringResource(R.string.family), p.family) { edit(p.copy(family = it)) } }
                item { FormField(stringResource(R.string.description), p.description, last = true) { edit(p.copy(description = it)) } }
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.visits), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null) }
                    }
                }
                items(visits, key = { it.id }) { v ->
                    val vm: List<Media> = media.filter { it.visitId == v.id }
                    VisitBox(
                        v = v,
                        label = labels[v.id].orEmpty(),
                        day = stringResource(R.string.day_fmt, dayNumber(v, visits)),
                        date = dateFmt.format(Date(v.date)),
                        thumbs = vm.takeLast(4).reversed(),
                        repo = repo,
                        onClick = { nav.push(Screen.VisitDetail(v.id)) },
                        onLong = { if (v.kind != "INITIAL") toDelete = v }
                    )
                }
            }
        }
    }

    if (showAdd) {
        ChoiceDialog(
            title = stringResource(R.string.add_visit),
            options = listOf("FOLLOWUP" to stringResource(R.string.visit_followup)),
            customHint = stringResource(R.string.visit_name_hint),
            onPick = { key ->
                showAdd = false
                scope.launch {
                    val v = if (key == "FOLLOWUP") Visit(patientId = id, kind = "FOLLOWUP")
                    else Visit(patientId = id, kind = "CUSTOM", title = key)
                    val vid = repo.dao.insertVisit(v)
                    nav.push(Screen.VisitDetail(vid))
                }
            },
            onDismiss = { showAdd = false }
        )
    }
    toDelete?.let { v ->
        ConfirmDialog(
            stringResource(R.string.delete_visit_q),
            onConfirm = { toDelete = null; scope.launch { repo.deleteVisit(v) } },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
private fun VisitBox(
    v: Visit,
    label: String,
    day: String,
    date: String,
    thumbs: List<Media>,
    repo: Repo,
    onClick: () -> Unit,
    onLong: () -> Unit
) {
    Card(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLong)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("$day · $date", style = MaterialTheme.typography.bodySmall)
            }
            if (v.notes.isNotBlank()) {
                Text(v.notes, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (thumbs.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    thumbs.forEach { m ->
                        AsyncImage(
                            repo.file(m.path), null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }
    }
}
