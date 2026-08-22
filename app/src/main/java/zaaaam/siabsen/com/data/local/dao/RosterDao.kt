package zaaaam.siabsen.com.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import zaaaam.siabsen.com.data.local.entity.AcademicYearEntity
import zaaaam.siabsen.com.data.local.entity.ClassEntity
import zaaaam.siabsen.com.data.local.entity.Role
import zaaaam.siabsen.com.data.local.entity.StudentEntity
import zaaaam.siabsen.com.data.local.entity.SubjectEntity
import zaaaam.siabsen.com.data.local.entity.TeacherEntity
import zaaaam.siabsen.com.data.local.entity.UserEntity

data class StudentRow(
    @Embedded val student: StudentEntity,
    val className: String?,
)

data class TeacherRow(
    @Embedded val teacher: TeacherEntity,
    val homeroomClassName: String?,
)

data class ClassRow(
    @Embedded val clazz: ClassEntity,
    val homeroomTeacherName: String?,
    val academicYearLabel: String?,
    val studentCount: Int,
)

@Dao
interface RosterDao {

    // ---------- Users ----------
    @Query("SELECT * FROM users WHERE active = 1 ORDER BY role, displayName")
    fun observeActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY role, displayName")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun userByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun userById(id: Long): UserEntity?

    @Upsert suspend fun upsertUser(user: UserEntity): Long

    @Query("UPDATE users SET pinHash = :pinHash WHERE id = :id")
    suspend fun updatePin(id: Long, pinHash: String?)

    @Query("UPDATE users SET active = :active WHERE id = :id")
    suspend fun setUserActive(id: Long, active: Boolean)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Long)

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsers(users: List<UserEntity>)

    // ---------- Students ----------
    @Transaction
    @Query(
        """SELECT s.*, c.name AS className FROM students s
           LEFT JOIN classes c ON c.id = s.classId
           WHERE s.active = 1 ORDER BY c.name, s.name"""
    )
    fun observeStudents(): Flow<List<StudentRow>>

    @Transaction
    @Query(
        """SELECT s.*, c.name AS className FROM students s
           LEFT JOIN classes c ON c.id = s.classId
           WHERE s.active = 1 AND s.classId = :classId ORDER BY s.name"""
    )
    fun observeStudentsOfClass(classId: Long): Flow<List<StudentRow>>

    @Transaction
    @Query(
        """SELECT s.*, c.name AS className FROM students s
           LEFT JOIN classes c ON c.id = s.classId
           WHERE s.id = :id"""
    )
    suspend fun studentById(id: String): StudentRow?

    @Transaction
    @Query(
        """SELECT s.*, c.name AS className FROM students s
           LEFT JOIN classes c ON c.id = s.classId
           WHERE s.active = 1 AND (s.name LIKE '%' || :q || '%' OR s.id LIKE '%' || :q || '%' OR IFNULL(s.nisn,'') LIKE '%' || :q || '%')
           ORDER BY s.name LIMIT 30"""
    )
    suspend fun searchStudents(q: String): List<StudentRow>

    @Query("SELECT * FROM students WHERE classId = :classId AND active = 1 ORDER BY name")
    suspend fun studentsOfClass(classId: Long): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun studentRaw(id: String): StudentEntity?

    @Upsert suspend fun upsertStudent(s: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Query("UPDATE students SET active = 0 WHERE id = :id")
    suspend fun deactivateStudent(id: String)

    @Query("DELETE FROM students")
    suspend fun clearStudents()

    // ---------- Teachers ----------
    @Transaction
    @Query(
        """SELECT t.*, c.name AS homeroomClassName FROM teachers t
           LEFT JOIN classes c ON c.homeroomTeacherId = t.id
           WHERE t.active = 1 ORDER BY t.name"""
    )
    fun observeTeachers(): Flow<List<TeacherRow>>

    @Query("SELECT * FROM teachers WHERE id = :id LIMIT 1")
    suspend fun teacherById(id: Long): TeacherEntity?

    @Upsert suspend fun upsertTeacher(t: TeacherEntity): Long

    @Query("UPDATE teachers SET active = 0 WHERE id = :id")
    suspend fun deactivateTeacher(id: Long)

    @Query("DELETE FROM teachers")
    suspend fun clearTeachers()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTeachers(teachers: List<TeacherEntity>)

    // ---------- Classes ----------
    @Transaction
    @Query(
        """SELECT c.*, t.name AS homeroomTeacherName, ay.name || ' - ' || ay.semester AS academicYearLabel,
                  (SELECT COUNT(*) FROM students s WHERE s.classId = c.id AND s.active = 1) AS studentCount
           FROM classes c
           LEFT JOIN teachers t ON t.id = c.homeroomTeacherId
           LEFT JOIN academic_years ay ON ay.id = c.academicYearId
           WHERE c.active = 1 ORDER BY c.name"""
    )
    fun observeClasses(): Flow<List<ClassRow>>

    @Query("SELECT * FROM classes WHERE active = 1 ORDER BY name")
    fun observeClassList(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
    suspend fun classById(id: Long): ClassEntity?

    @Query("SELECT * FROM classes WHERE name = :name LIMIT 1")
    suspend fun classByName(name: String): ClassEntity?

    @Upsert suspend fun upsertClass(c: ClassEntity): Long

    @Query("UPDATE classes SET active = 0 WHERE id = :id")
    suspend fun deactivateClass(id: Long)

    @Query("DELETE FROM classes")
    suspend fun clearClasses()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClasses(classes: List<ClassEntity>)

    // ---------- Subjects ----------
    @Query("SELECT * FROM subjects ORDER BY name")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun subjectById(id: Long): SubjectEntity?

    @Upsert suspend fun upsertSubject(s: SubjectEntity): Long

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)

    @Query("DELETE FROM subjects")
    suspend fun clearSubjects()

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun countSubjects(): Int

    @Query("SELECT role FROM users WHERE id = :userId LIMIT 1")
    suspend fun roleOf(userId: Long): Role?

    @Query("SELECT * FROM users WHERE active = 1 AND role = 'STUDENT' AND linkedStudentId IS NOT NULL")
    suspend fun activeStudentUsersOnce(): List<UserEntity>
}
