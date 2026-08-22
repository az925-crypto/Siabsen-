package zaaaam.siabsen.com.ui.feature.guru

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.backup.BackupRepository
import zaaaam.siabsen.com.data.export.ExportRepository
import zaaaam.siabsen.com.data.local.dao.ClassRow
import zaaaam.siabsen.com.data.local.dao.RecordRow
import zaaaam.siabsen.com.data.local.dao.SessionRow
import zaaaam.siabsen.com.data.local.dao.StudentRow
import zaaaam.siabsen.com.data.local.entity.AttendanceMethod
import zaaaam.siabsen.com.data.local.entity.AttendanceRecordEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.data.local.entity.QrBroadcastEntity
import zaaaam.siabsen.com.data.local.entity.SessionType
import zaaaam.siabsen.com.data.repository.AttendanceRepository
import zaaaam.siabsen.com.data.repository.CheckResult
import zaaaam.siabsen.com.data.repository.LeaveRepository
import zaaaam.siabsen.com.data.repository.RosterRepository
import zaaaam.siabsen.com.data.repository.SettingsRepository
import zaaaam.siabsen.com.qr.QrCodec
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TeacherDashboardVm @Inject constructor(
    attendance: AttendanceRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val todaySessions: StateFlow<List<SessionRow>> =
        attendance.observeSessionsBetween(
            LocalDate.now().toEpochDay(), LocalDate.now().toEpochDay(),
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _insights = MutableStateFlow<List<String>>(emptyList())
    val insights: StateFlow<List<String>> = _insights

    init {
        viewModelScope.launch {
            _insights.value = attendance.insightForClass(null)
        }
    }

    fun refreshInsights() {
        viewModelScope.launch { _insights.value = attendance.insightForClass(null) }
    }
}

@HiltViewModel
class ClassListVm @Inject constructor(roster: RosterRepository) : ViewModel() {
    val classes: StateFlow<List<ClassRow>> = roster.observeClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TakeAttendanceVm @Inject constructor(
    private val roster: RosterRepository,
    private val attendance: AttendanceRepository,
    private val leaveRepo: LeaveRepository,
    private val settingsRepo: SettingsRepository,
    val session: zaaaam.siabsen.com.security.SessionManager,
) : ViewModel() {

    val classId = MutableStateFlow(0L)
    val sessionId = MutableStateFlow<String?>(null)

    val students = classId.flatMapLatest { cid ->
        if (cid == 0L) flowOf(emptyList<StudentRow>()) else roster.observeStudentsOfClass(cid)
    }

    val records = sessionId.flatMapLatest { sid ->
        if (sid == null) flowOf(emptyList<RecordRow>())
        else attendance.observeRecordsOfSession(sid)
    }

    data class Ui(
        val students: List<StudentRow> = emptyList(),
        val records: List<RecordRow> = emptyList(),
        val className: String? = null,
    )

    private val classNameState = MutableStateFlow<String?>(null)

    val ui: StateFlow<Ui> = combine(students, records, classNameState) { s, r, cn -> Ui(s, r, cn) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Ui())

    fun initFor(cid: Long) {
        if (classId.value == cid) return
        classId.value = cid
        viewModelScope.launch {
            classNameState.value = roster.classById(cid)?.name
            // pakai sesi harian yang sudah ada hari ini, kalau tidak buat baru
            val ses = attendance.ensureDailySession(cid, LocalDate.now())
            sessionId.value = ses?.id
        }
    }

    /** Tap cepat: set status langsung (jika belum ada record). */
    fun quickMark(student: StudentRow, status: AttendanceStatus, onError: (String) -> Unit) {
        val sid = sessionId.value ?: return onError("Sesi belum siap")
        viewModelScope.launch {
            when (val res = attendance.mark(sid, student.student.id, status, AttendanceMethod.MANUAL)) {
                is CheckResult.Failure -> onError(res.reason)
                else -> Unit
            }
        }
    }

    /** Koreksi dengan alasan wajib. */
    fun correct(student: StudentRow, newStatus: AttendanceStatus, reason: String, onDone: (String) -> Unit, onError: (String) -> Unit) {
        val sid = sessionId.value ?: return onError("Sesi belum siap")
        viewModelScope.launch {
            val existing = records.value.firstOrNull { it.record.studentId == student.student.id }
            when {
                existing != null ->
                    when (val res = attendance.correct(existing.record, newStatus, reason)) {
                        is CheckResult.Success -> onDone(res.message)
                        is CheckResult.Failure -> onError(res.reason)
                    }
                else ->
                    when (val res = attendance.mark(sid, student.student.id, newStatus, AttendanceMethod.MANUAL, correctionReason = reason)) {
                        is CheckResult.Success -> onDone(res.message)
                        is CheckResult.Failure -> onError(res.reason)
                    }
            }
        }
    }

    fun closeSession(onDone: () -> Unit) {
        val sid = sessionId.value ?: return
        viewModelScope.launch {
            attendance.closeSession(sid)
            onDone()
        }
    }

    /** Mulai QR broadcast untuk sesi ini. Return sessionId bila sukses. */
    fun startQr(onReady: () -> Unit, onError: (String) -> Unit) {
        val sid = sessionId.value ?: return onError("Sesi belum siap")
        viewModelScope.launch {
            val s = settingsRepo.current()
            if (!s.qrEnabled) return@launch onError("QR dinonaktifkan admin")
            val secret = QrCodec.randomSecret()
            val expires = System.currentTimeMillis() + s.qrValidityMinutes * 60_000L
            attendance.startBroadcast(sid, secret, s.qrRotationSeconds, expires)
            onReady()
        }
    }
}

@HiltViewModel
class QrBroadcastVm @Inject constructor(
    private val attendance: AttendanceRepository,
    private val qrDaoFlow: zaaaam.siabsen.com.data.local.dao.QrDao,
) : ViewModel() {

    data class BroadcastUi(
        val active: Boolean = false,
        val rotationSeconds: Int = 30,
        val expiresAt: Long = 0,
        val secret: String = "",
        val sessionId: String = "",
    )

    val sessionId = MutableStateFlow("")

    val broadcast: StateFlow<BroadcastUi> = sessionId.flatMapLatest { sid ->
        if (sid.isBlank()) flowOf(null) else qrDaoFlow.observeBySession(sid)
    }.combine(MutableStateFlow(Unit)) { b, _ ->
        b?.let { BroadcastUi(it.active, it.rotationSeconds, it.expiresAt, it.secret, it.sessionId) } ?: BroadcastUi()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BroadcastUi())

    /** payload QR yang berputar tiap window */
    private val tick = MutableStateFlow(0L)
    init {
        viewModelScope.launch {
            while (true) {
                tick.value = System.currentTimeMillis()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    data class QrRender(val payload: String, val secondsLeftInWindow: Int, val totalLeftSeconds: Long)

    val render: StateFlow<QrRender?> = combine(tick, broadcast) { now, bc ->
        if (!bc.active || bc.sessionId.isBlank()) null
        else {
            val window = QrCodec().currentWindow(now, bc.rotationSeconds)
            val payload = QrCodec().buildPayload(bc.sessionId, bc.secret, window)
            val secInWindow = ((now / 1000) % bc.rotationSeconds).toInt()
            QrRender(payload, bc.rotationSeconds - secInWindow, (bc.expiresAt - now) / 1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val presentCount = sessionId.flatMapLatest { sid ->
        if (sid.isBlank()) flowOf(0)
        else attendance.observeRecordsOfSession(sid).combine(flowOf(1)) { recs, _ ->
            recs.count { it.record.status == AttendanceStatus.PRESENT || it.record.status == AttendanceStatus.LATE }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun stop() {
        viewModelScope.launch {
            qrDaoFlow.deactivate(sessionId.value)
        }
    }

    fun initFor(sid: String) { sessionId.value = sid }
}
