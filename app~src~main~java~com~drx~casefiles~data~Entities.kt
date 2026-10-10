package com.drx.casefiles.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val age: String = "",
    val residence: String = "",
    val phone: String = "",
    val complaint: String = "",
    val pmh: String = "",
    val psh: String = "",
    val drugs: String = "",
    val allergy: String = "",
    val family: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** kind: INITIAL (the medical history box), FOLLOWUP (auto numbered by date) or CUSTOM (uses title). */
@Entity(
    tableName = "visits",
    foreignKeys = [ForeignKey(entity = Patient::class, parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("patientId")]
)
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val kind: String = "FOLLOWUP",
    val title: String = "",
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "media",
    foreignKeys = [ForeignKey(entity = Visit::class, parentColumns = ["id"], childColumns = ["visitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("visitId"), Index("patientId")]
)
data class Media(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visitId: Long,
    val patientId: Long,
    val path: String,
    val category: String = "CLINICAL",
    val region: String = "OTHER",
    val side: String = "NONE",
    val viewPos: String = "",
    val stage: String = "",
    val tag: String = "",
    val healthy: Boolean = false,
    val caption: String = "",
    val takenAt: Long = System.currentTimeMillis()
)
