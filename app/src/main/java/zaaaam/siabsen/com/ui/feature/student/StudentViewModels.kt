package zaaaam.siabsen.com.ui.feature.student

import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.local.dao.ScheduleRow
import zaaaam.siabsen.com.data.local.dao.StudentRow
import zaaaam.siabsen.com.data.local.entity.AttendanceMethod
import zaaaam.siabsen.com.data.local.entity.AttendanceRecordEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.data.local.entity.LeaveRequestEntity
import zaaaam.siabsen.com.data.local.entity.LeaveType
import zaaaam.siabsen.com.data.repository.AttendanceRepository
import zaaaam.siabsen.com.data.repository.CheckResult
import zaaaam.siabsen.com.data.repository.LeaveRepository
import zaaaam.siabsen.com.data.repository.RosterRepository
import zaaaam.siabsen.com.data.repository.SettingsRepository
import zaaaam.siabsen.com.qr.QrCodec
import zaaaam.siabsen.com.security.SessionManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import androidx.compose.ui.platform.LocalContext

data class StudentHomeUi(
    val loading: Boolean = true,
    val name: String = "",
    val className: String? = null,
    val todayStatus: AttendanceStatus? = null,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val percent: Int = 0,
    val monthCounts: Map<AttendanceStatus, Int> = emptyMap(),
    val schedule: List<ScheduleRow> = emptyList(),
    val schoolName: String = "",
)

@HiltViewModel
class StudentHomeVm @Inject constructor(
    private val roster: RosterRepository,
    private val attendance: AttendanceRepository,
    private val academic: zaaaam.siabsen.com.data.repository.AcademicRepository,
    private val settingsRepo: SettingsRepository,
    val session: SessionManager,
) : ViewModel() {

    private val sid: String? get() = session.linkedStudentId

    private val refresh = MutableStateFlow(0)
    private val base = MutableStateFlow(StudentHomeUi())

    val ui: StateFlow<StudentHomeUi> = combine(base, refresh) { b, _ -> b }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudentHomeUi())

    init { load() }

    fun load() {
        viewModelScope.launch {
            val id = sid ?: run { base.value = base.value.copy(loading = false); return@launch }
            val row: StudentRow? = roster.studentById(id)
            val today = attendance.dailyRecordOfToday(id)
            val month = YearMonth.now()
            val from = month.atDay(1)
            var total = 0
            var attended = 0
            val counts = mutableMapOf<AttendanceStatus, Int>()
            // hitung bulan berjalan dari flow sekali (one-shot via first())
            attendance.observeStudentStatusCounts(id, from, LocalDate.now())
                .firstOrNull()?.forEach {
                    counts[it.status] = it.cnt
                    total += it.cnt
                    if (!it.status.countsAsAbsent) attended += it.cnt
                }
            val dow = LocalDate.now().dayOfWeek
            val scheduleFlow = row?.student?.classId?.let { cid -> academic.scheduleForClass(cid, dow) }
            val sched: List<ScheduleRow> = scheduleFlow?.firstOrNull() ?: emptyList()
            base.value = StudentHomeUi(
                loading = false,
                name = row?.student?.name ?: session.currentUserName,
                className = row?.className,
                todayStatus = today?.status,
                checkIn = today?.checkInTime,
                checkOut = today?.checkOutTime,
                percent = if (total == 0) 0 else ((attended * 100.0) / total).toInt(),
                monthCounts = counts,
                schedule = sched,
                schoolName = settingsRepo.current().schoolName,
            )
        }
    }

    fun checkOut(onDone: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val id = sid ?: return@launch onError("Akun tidak terhubung siswa")
            when (val res = attendance.selfCheckOut(id)) {
                is CheckResult.Success -> { load(); onDone(res.message) }
                is CheckResult.Failure -> onError(res.reason)
            }
        }
    }

    fun selfCheckIn(onDone: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val id = sid ?: return@launch onError("Akun tidak terhubung siswa")
            when (val res = attendance.selfCheckIn(id, AttendanceMethod.SELF_CHECKIN, deviceId())) {
                is CheckResult.Success -> { load(); onDone(res.message) }
                is CheckResult.Failure -> onError(res.reason)
            }
        }
    }

    fun deviceId(): String? =
        session.currentUserId?.let { "u$it" }
}

@HiltViewModel
class ScanVm @Inject constructor(
    private val attendance: AttendanceRepository,
    private val settingsRepo: SettingsRepository,
    val session: SessionManager,
) : ViewModel() {

    val result = MutableStateFlow<String?>(null)
    val success = MutableStateFlow<Boolean?>(null)
    val locationRequired = MutableStateFlow(false)

    fun onPayload(payload: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val id = session.linkedStudentId
            if (id == null) { fail("Akun tidak terhubung siswa"); return@launch }
            when (val res = attendance.qrCheckIn(id, payload, "u${session.currentUserId}")) {
                is CheckResult.Success -> { result.value = res.message; success.value = true; onDone() }
                is CheckResult.Failure -> fail(res.reason)
            }
        }
    }

    fun manualCheckIn(onDone: () -> Unit) {
        viewModelScope.launch {
            val id = session.linkedStudentId
            if (id == null) { fail("Akun tidak terhubung siswa"); return@launch }
            when (val res = attendance.selfCheckIn(id, AttendanceMethod.SELF_CHECKIN, "u${session.currentUserId}")) {
                is CheckResult.Success -> { result.value = res.message; success.value = true; onDone() }
                is CheckResult.Failure -> fail(res.reason)
            }
        }
    }

    fun checkOut(onDone: () -> Unit) {
        viewModelScope.launch {
            val id = session.linkedStudentId
            if (id == null) { fail("Akun tidak terhubung siswa"); return@launch }
            when (val res = attendance.selfCheckOut(id)) {
                is CheckResult.Success -> { result.value = res.message; success.value = true; onDone() }
                is CheckResult.Failure -> fail(res.reason)
            }
        }
    }

    fun reset() { result.value = null; success.value = null }

    private fun fail(reason: String) {
        result.value = reason
        success.value = false
    }

    suspend fun qrEnabled(): Boolean = settingsRepo.current().qrEnabled
}
