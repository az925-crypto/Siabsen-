package zaaaam.siabsen.com.ui.feature.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.backup.BackupRepository
import zaaaam.siabsen.com.data.local.dao.ClassRow
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.data.local.entity.AcademicYearEntity
import zaaaam.siabsen.com.data.local.entity.CalendarDayType
import zaaaam.siabsen.com.data.local.entity.ClassEntity
import zaaaam.siabsen.com.data.local.entity.StudentEntity
import zaaaam.siabsen.com.data.local.entity.SubjectEntity
import zaaaam.siabsen.com.data.local.entity.TeacherEntity
import zaaaam.siabsen.com.data.repository.AttendanceRepository
import zaaaam.siabsen.com.data.repository.RosterRepository
import zaaaam.siabsen.com.data.repository.SchoolSettings
import zaaaam.siabsen.com.data.repository.SettingsRepository
import zaaaam.siabsen.com.security.PinHasher
import java.time.LocalDate
import javax.inject.Inject

/** ViewModel gabungan untuk master data admin */
@HiltViewModel
class RosterVm @Inject constructor(
    private val roster: RosterRepository,
) : ViewModel() {

    val students = roster.observeStudents().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val teachers = roster.observeTeachers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val classes = roster.observeClasses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val subjects = roster.observeSubjects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- Student ----------
    fun saveStudent(s: StudentEntity) = viewModelScope.launch { roster.saveStudent(s) }
    fun deleteStudent(id: String) = viewModelScope.launch { roster.deactivateStudent(id) }

    suspend fun importCsv(text: String): RosterRepository.ImportResult = roster.importStudentsCsv(text)

    private val _importMsg = MutableStateFlow<String?>(null)
    val importMsg: StateFlow<String?> = _importMsg

    fun importCsvAsync(text: String) {
        viewModelScope.launch {
            val res = roster.importStudentsCsv(text)
            _importMsg.value = "Import: ${res.inserted} baru, ${res.skipped} dilewati, ${res.errors.size} error"
        }
    }

    // ---------- Teacher ----------
    fun saveTeacher(t: TeacherEntity) = viewModelScope.launch { roster.saveTeacher(t) }
    fun deleteTeacher(id: Long) = viewModelScope.launch { roster.deactivateTeacher(id) }

    // ---------- Class ----------
    fun saveClass(c: ClassEntity) = viewModelScope.launch { roster.saveClass(c) }
    fun deleteClass(id: Long) = viewModelScope.launch { roster.deactivateClass(id) }

    // ---------- Subject ----------
    fun saveSubject(s: SubjectEntity) = viewModelScope.launch { roster.saveSubject(s) }
    fun deleteSubject(id: Long) = viewModelScope.launch { roster.deleteSubject(id) }
}

@HiltViewModel
class AcademicVm @Inject constructor(
    private val academic: zaaaam.siabsen.com.data.repository.AcademicRepository,
) : ViewModel() {

    val years = academic.observeYears().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val calendar = academic.observeCalendar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveYear(y: AcademicYearEntity) = viewModelScope.launch { academic.saveYear(y) }
    fun activate(id: Long) = viewModelScope.launch { academic.activateYear(id) }
    fun deleteYear(id: Long) = viewModelScope.launch { academic.deleteYear(id) }

    fun setDay(date: LocalDate, type: CalendarDayType, note: String?) =
        viewModelScope.launch { academic.setCalendarDay(date, type, note) }
}

@HiltViewModel
class SettingsVm @Inject constructor(
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val settings = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SchoolSettings())

    val saved = MutableStateFlow(false)

    fun save(next: SchoolSettings) {
        viewModelScope.launch {
            settingsRepo.save { next }
            saved.value = true
        }
    }
}

@HiltViewModel
class BackupVm @Inject constructor(
    private val backupRepo: BackupRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data class Done(val message: String) : UiState
    }

    val state = MutableStateFlow<UiState>(UiState.Idle)
    fun suggestedName(): String = backupRepo.suggestedName()

    fun createBackup(target: Uri?) {
        if (target == null) return
        viewModelScope.launch {
            state.value = runCatching { backupRepo.createBackup(target) }
                .fold({ UiState.Done("Backup tersimpan ($it)") }, { UiState.Done("Gagal: ${it.message}") })
        }
    }

    fun restore(source: Uri?, replace: Boolean) {
        if (source == null) return
        viewModelScope.launch {
            when (val res = backupRepo.restore(source, replace)) {
                is BackupRepository.RestoreResult.Success ->
                    state.value = UiState.Done("Restore sukses: ${res.rows} baris")
                is BackupRepository.RestoreResult.Failure ->
                    state.value = UiState.Done("Gagal: ${res.reason}")
            }
        }
    }
}
