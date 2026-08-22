package zaaaam.siabsen.com.data.repository

import kotlinx.coroutines.flow.Flow
import zaaaam.siabsen.com.data.local.dao.ClassRow
import zaaaam.siabsen.com.data.local.dao.RosterDao
import zaaaam.siabsen.com.data.local.dao.StudentRow
import zaaaam.siabsen.com.data.local.dao.TeacherRow
import zaaaam.siabsen.com.data.local.entity.AcademicYearEntity
import zaaaam.siabsen.com.data.local.entity.ClassEntity
import zaaaam.siabsen.com.data.local.entity.StudentEntity
import zaaaam.siabsen.com.data.local.entity.SubjectEntity
import zaaaam.siabsen.com.data.local.entity.TeacherEntity
import zaaaam.siabsen.com.security.AuditLogger
import java.io.BufferedReader
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RosterRepository @Inject constructor(
    private val dao: RosterDao,
    private val audit: AuditLogger,
) {
    // ---------- Students ----------
    fun observeStudents(): Flow<List<StudentRow>> = dao.observeStudents()
    fun observeStudentsOfClass(classId: Long): Flow<List<StudentRow>> = dao.observeStudentsOfClass(classId)

    suspend fun studentById(id: String): StudentRow? = dao.studentById(id)
    suspend fun studentRaw(id: String): StudentEntity? = dao.studentRaw(id)
    suspend fun searchStudents(q: String): List<StudentRow> = dao.searchStudents(q.trim())

    suspend fun saveStudent(s: StudentEntity) {
        val old = dao.studentRaw(s.id)
        dao.upsertStudent(s)
        audit.log(
            if (old == null) "CREATE_STUDENT" else "UPDATE_STUDENT",
            "STUDENT", s.id, "${s.id} ${s.name} kelas=${s.classId}"
        )
    }

    suspend fun deactivateStudent(id: String) {
        dao.deactivateStudent(id)
        audit.log("DEACTIVATE_STUDENT", "STUDENT", id, "")
    }

    suspend fun importStudentsCsv(csv: String): ImportResult {
        val lines = csv.lineSequence().map { it.trim() }.filter { it.isNotBlank() }
        var inserted = 0
        var skipped = 0
        val errors = mutableListOf<String>()
        lines.forEachIndexed { idx, line ->
            if (idx == 0 && line.contains("NIS", ignoreCase = true)) return@forEachIndexed
            val cols = line.split(",", ";").map { it.trim() }
            if (cols.size < 3) { errors.add("Baris ${idx + 1}: format tidak sesuai"); return@forEachIndexed }
            val nis = cols[0]
            val name = cols[1]
            val className = cols[2]
            val nisn = cols.getOrNull(3)?.takeIf { it.isNotBlank() }
            if (nis.isBlank() || name.isBlank()) { skipped++; return@forEachIndexed }
            val classId = ensureClass(className)
            val row = StudentEntity(id = nis, nisn = nisn, name = name, classId = classId)
            val before = dao.studentRaw(nis)
            dao.upsertStudent(row)
            if (before == null) inserted++ else skipped++
        }
        audit.log("IMPORT_STUDENTS_CSV", details = "insert=$inserted skip=$skipped err=${errors.size}")
        return ImportResult(inserted, skipped, errors)
    }

    private suspend fun ensureClass(name: String): Long? {
        if (name.isBlank()) return null
        return dao.classByName(name)?.id ?: dao.upsertClass(ClassEntity(name = name))
    }

    // ---------- Teachers ----------
    fun observeTeachers(): Flow<List<TeacherRow>> = dao.observeTeachers()
    suspend fun teacherById(id: Long): TeacherEntity? = dao.teacherById(id)

    suspend fun saveTeacher(t: TeacherEntity): Long {
        val id = dao.upsertTeacher(t)
        audit.log(if (t.id == 0L) "CREATE_TEACHER" else "UPDATE_TEACHER", "TEACHER", id.toString(), t.name)
        return id
    }

    suspend fun deactivateTeacher(id: Long) {
        dao.deactivateTeacher(id)
        audit.log("DEACTIVATE_TEACHER", "TEACHER", id.toString(), "")
    }

    // ---------- Classes ----------
    fun observeClasses(): Flow<List<ClassRow>> = dao.observeClasses()
    fun observeClassList(): Flow<List<ClassEntity>> = dao.observeClassList()
    suspend fun classById(id: Long): ClassEntity? = dao.classById(id)

    suspend fun saveClass(c: ClassEntity): Long {
        val id = dao.upsertClass(c)
        audit.log(if (c.id == 0L) "CREATE_CLASS" else "UPDATE_CLASS", "CLASS", id.toString(), c.name)
        return id
    }

    suspend fun deactivateClass(id: Long) {
        dao.deactivateClass(id)
        audit.log("DEACTIVATE_CLASS", "CLASS", id.toString(), "")
    }

    // ---------- Subjects ----------
    fun observeSubjects(): Flow<List<SubjectEntity>> = dao.observeSubjects()

    suspend fun saveSubject(s: SubjectEntity): Long {
        val id = dao.upsertSubject(s)
        audit.log(if (s.id == 0L) "CREATE_SUBJECT" else "UPDATE_SUBJECT", "SUBJECT", id.toString(), s.name)
        return id
    }

    suspend fun deleteSubject(id: Long) {
        dao.deleteSubject(id)
        audit.log("DELETE_SUBJECT", "SUBJECT", id.toString(), "")
    }

    data class ImportResult(val inserted: Int, val skipped: Int, val errors: List<String>)
}

/** Util kecil untuk membaca CSV dari Uri */
object CsvReader {
    fun read(context: android.content.Context, uri: android.net.Uri): String =
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(stream.reader(Charsets.UTF_8)).readText()
        } ?: ""
}
