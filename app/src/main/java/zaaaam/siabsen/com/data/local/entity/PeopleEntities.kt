package zaaaam.siabsen.com.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val pinHash: String? = null,
    val role: Role,
    val linkedStudentId: String? = null,
    val linkedTeacherId: Long? = null,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "students",
    indices = [Index(value = ["nis"], unique = true)]
)
data class StudentEntity(
    @PrimaryKey val id: String,          // NIS sebagai ID
    val nisn: String? = null,
    val name: String,
    val gender: String? = null,          // L / P
    val classId: Long? = null,
    val photoPath: String? = null,
    val active: Boolean = true,
)

@Serializable
@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nip: String? = null,
    val name: String,
    val phone: String? = null,
    val homeroomClassId: Long? = null,   // wali kelas dari kelas ini
    val active: Boolean = true,
)

@Serializable
@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val homeroomTeacherId: Long? = null,
    val academicYearId: Long? = null,
    val active: Boolean = true,
)

@Serializable
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String? = null,
)
