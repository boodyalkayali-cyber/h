package com.drx.casefiles.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class Repo(private val ctx: Context, val dao: CaseDao) {
    /** Survives screen changes; used for writes that must not be cancelled. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dir = File(ctx.filesDir, "media").apply { mkdirs() }

    fun file(name: String): File = File(dir, name)

    fun newImageName(): String = "${UUID.randomUUID()}.jpg"

    suspend fun addMedia(m: Media) {
        dao.insertMedia(m)
        dao.touchPatient(m.patientId, System.currentTimeMillis())
    }

    suspend fun importImages(uris: List<Uri>, template: Media) = withContext(Dispatchers.IO) {
        uris.forEach { uri ->
            val name = newImageName()
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                file(name).outputStream().use { out -> input.copyTo(out) }
            }
            if (file(name).exists()) {
                addMedia(template.copy(id = 0, path = name, takenAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun deleteMedia(items: List<Media>) = withContext(Dispatchers.IO) {
        dao.deleteMediaByIds(items.map { it.id })
        items.forEach { file(it.path).delete() }
    }

    suspend fun deleteVisit(v: Visit) = withContext(Dispatchers.IO) {
        dao.mediaOfVisit(v.id).forEach { file(it.path).delete() }
        dao.deleteVisit(v)
    }

    suspend fun deletePatient(p: Patient) = withContext(Dispatchers.IO) {
        dao.mediaOfPatient(p.id).forEach { file(it.path).delete() }
        dao.deletePatient(p)
    }
}
