package com.drx.casefiles.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.drx.casefiles.R

enum class Category(@StringRes val label: Int) {
    CLINICAL(R.string.cat_clinical),
    XRAY(R.string.cat_xray),
    MRI(R.string.cat_mri),
    CT(R.string.cat_ct),
    ECHO(R.string.cat_echo),
    LAB(R.string.cat_lab),
    REPORT(R.string.cat_report)
}

enum class Side(@StringRes val label: Int) {
    NONE(R.string.side_none),
    L(R.string.side_left),
    R(R.string.side_right)
}

enum class Region(@StringRes val label: Int) {
    SHOULDER(R.string.r_shoulder), ARM(R.string.r_arm), ELBOW(R.string.r_elbow),
    FOREARM(R.string.r_forearm), WRIST(R.string.r_wrist), HAND(R.string.r_hand),
    PELVIS(R.string.r_pelvis), HIP(R.string.r_hip), THIGH(R.string.r_thigh),
    KNEE(R.string.r_knee), LEG(R.string.r_leg), ANKLE(R.string.r_ankle),
    FOOT(R.string.r_foot), SPINE(R.string.r_spine), OTHER(R.string.r_other)
}

enum class Stage(@StringRes val label: Int) {
    BEFORE_REDUCTION(R.string.st_before_reduction),
    AFTER_REDUCTION(R.string.st_after_reduction),
    AFTER_CAST(R.string.st_after_cast),
    PRE_OP(R.string.st_pre_op),
    POST_OP(R.string.st_post_op),
    CONTROL_FIXATION(R.string.st_control_fixation)
}

enum class ViewPos(@StringRes val label: Int) {
    AP(R.string.v_ap), LATERAL(R.string.v_lateral), OBLIQUE(R.string.v_oblique),
    PA(R.string.v_pa), MORTISE(R.string.v_mortise), GRASHEY(R.string.v_grashey),
    YVIEW(R.string.v_yview), AXILLARY(R.string.v_axillary), ROBERTS(R.string.v_roberts),
    SKYLINE(R.string.v_skyline), ANTERIOR(R.string.v_anterior), POSTERIOR(R.string.v_posterior),
    MEDIAL(R.string.v_medial), AXIAL(R.string.v_axial), STRESS(R.string.v_stress)
}

inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }

fun sideShort(side: String): String = when (side) {
    "L" -> "L"
    "R" -> "R"
    else -> ""
}

@Composable
fun stageText(stage: String): String {
    val s = enumOrNull<Stage>(stage)
    return when {
        s != null -> stringResource(s.label)
        stage.isBlank() -> stringResource(R.string.stage_none)
        else -> stage
    }
}

@Composable
fun regionText(region: String): String {
    val r = enumOrNull<Region>(region)
    return if (r != null) stringResource(r.label) else ""
}

@Composable
fun viewText(viewPos: String): String {
    val v = enumOrNull<ViewPos>(viewPos)
    return if (v != null) stringResource(v.label) else ""
}

@Composable
fun categoryText(category: String): String {
    val c = enumOrNull<Category>(category)
    return if (c != null) stringResource(c.label) else category
}

/** Label for every visit, e.g. "Follow-up 2". Follow-ups are numbered per patient by date. */
fun visitLabels(ctx: Context, visits: List<Visit>): Map<Long, String> {
    val out = HashMap<Long, String>()
    visits.groupBy { it.patientId }.values.forEach { list ->
        var n = 0
        list.sortedBy { it.date }.forEach { v ->
            out[v.id] = when (v.kind) {
                "INITIAL" -> ctx.getString(R.string.visit_initial)
                "FOLLOWUP" -> { n++; ctx.getString(R.string.visit_followup_fmt, n) }
                else -> v.title.ifBlank { ctx.getString(R.string.visit_followup) }
            }
        }
    }
    return out
}

/** Days since the first visit of the same patient. */
fun dayNumber(visit: Visit, patientVisits: List<Visit>): Int {
    val start = patientVisits.firstOrNull { it.kind == "INITIAL" }?.date
        ?: patientVisits.minOfOrNull { it.date } ?: visit.date
    return ((visit.date - start) / 86_400_000L).toInt().coerceAtLeast(0)
}
