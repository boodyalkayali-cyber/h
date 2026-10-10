package com.drx.casefiles.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Query("SELECT * FROM patients ORDER BY updatedAt DESC")
    fun patients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE id = :id")
    fun patient(id: Long): Flow<Patient?>

    @Insert
    suspend fun insertPatient(p: Patient): Long

    @Update
    suspend fun updatePatient(p: Patient)

    @Delete
    suspend fun deletePatient(p: Patient)

    @Query("SELECT * FROM visits ORDER BY date")
    fun allVisits(): Flow<List<Visit>>

    @Query("SELECT * FROM visits WHERE patientId = :pid ORDER BY date")
    fun visits(pid: Long): Flow<List<Visit>>

    @Query("SELECT * FROM visits WHERE id = :id")
    fun visit(id: Long): Flow<Visit?>

    @Insert
    suspend fun insertVisit(v: Visit): Long

    @Delete
    suspend fun deleteVisit(v: Visit)

    @Query("UPDATE visits SET notes = :notes WHERE id = :id")
    suspend fun setVisitNotes(id: Long, notes: String)

    @Query("UPDATE visits SET date = :date WHERE id = :id")
    suspend fun setVisitDate(id: Long, date: Long)

    @Query("SELECT * FROM media WHERE visitId = :vid ORDER BY takenAt")
    fun media(vid: Long): Flow<List<Media>>

    @Query("SELECT * FROM media ORDER BY takenAt DESC")
    fun allMedia(): Flow<List<Media>>

    @Query("SELECT * FROM media WHERE patientId = :pid ORDER BY takenAt")
    fun mediaOfPatientFlow(pid: Long): Flow<List<Media>>

    @Query("SELECT * FROM media WHERE patientId = :pid")
    suspend fun mediaOfPatient(pid: Long): List<Media>

    @Query("SELECT * FROM media WHERE visitId = :vid")
    suspend fun mediaOfVisit(vid: Long): List<Media>

    @Insert
    suspend fun insertMedia(m: Media): Long

    @Update
    suspend fun updateMedia(m: Media)

    @Query("DELETE FROM media WHERE id IN (:ids)")
    suspend fun deleteMediaByIds(ids: List<Long>)

    @Query("UPDATE media SET visitId = :visitId, patientId = :patientId WHERE id IN (:ids)")
    suspend fun moveMedia(ids: List<Long>, visitId: Long, patientId: Long)

    @Query("UPDATE media SET stage = :stage WHERE id IN (:ids)")
    suspend fun setStage(ids: List<Long>, stage: String)

    @Query("UPDATE media SET healthy = :healthy WHERE id IN (:ids)")
    suspend fun setHealthy(ids: List<Long>, healthy: Boolean)

    @Query("UPDATE patients SET updatedAt = :t WHERE id = :id")
    suspend fun touchPatient(id: Long, t: Long)
}
