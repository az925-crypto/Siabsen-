package zaaaam.siabsen.com.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "siabsen_settings")

data class SchoolSettings(
    val schoolName: String = "SMA Negeri 1 Contoh",
    val schoolAddress: String = "",
    /** jam masuk dibuka, format HH:mm */
    val checkInStart: String = "05:30",
    /** batas tepat waktu HH:mm */
    val onTimeUntil: String = "06:45",
    /** batas terlambat HH:mm; setelah itu dianggap tidak hadir */
    val lateUntil: String = "07:30",
    val checkOutFrom: String = "15:00",
    /** 1=Senin .. 7=Minggu */
    val schoolDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val qrEnabled: Boolean = true,
    val qrRotationSeconds: Int = 30,
    val qrValidityMinutes: Int = 15,
    val locationCheckEnabled: Boolean = false,
    val schoolLatitude: Double = 0.0,
    val schoolLongitude: Double = 0.0,
    val radiusMeters: Int = 150,
    val wifiCheckEnabled: Boolean = false,
    val wifiSsid: String = "",
    /** threshold early warning */
    val warnThresholdPercent: Int = 90,
    val criticalThresholdPercent: Int = 80,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = true,
    val autoLockMinutes: Int = 5,
    val reminderHour: Int = 7,
    val reminderMinute: Int = 0,
    val logoPath: String = "",
    val deviceBindingEnabled: Boolean = false,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object K {
        val SCHOOL_NAME = stringPreferencesKey("school_name")
        val SCHOOL_ADDRESS = stringPreferencesKey("school_address")
        val CHECK_IN_START = stringPreferencesKey("check_in_start")
        val ON_TIME_UNTIL = stringPreferencesKey("on_time_until")
        val LATE_UNTIL = stringPreferencesKey("late_until")
        val CHECK_OUT_FROM = stringPreferencesKey("check_out_from")
        val SCHOOL_DAYS = stringPreferencesKey("school_days")
        val QR_ENABLED = booleanPreferencesKey("qr_enabled")
        val QR_ROTATION = intPreferencesKey("qr_rotation_seconds")
        val QR_VALIDITY = intPreferencesKey("qr_validity_minutes")
        val LOC_ENABLED = booleanPreferencesKey("location_check_enabled")
        val SCHOOL_LAT = doublePreferencesKey("school_lat")
        val SCHOOL_LNG = doublePreferencesKey("school_lng")
        val RADIUS = intPreferencesKey("radius_meters")
        val WIFI_ENABLED = booleanPreferencesKey("wifi_check_enabled")
        val WIFI_SSID = stringPreferencesKey("wifi_ssid")
        val WARN_THRESHOLD = intPreferencesKey("warn_threshold_percent")
        val CRITICAL_THRESHOLD = intPreferencesKey("critical_threshold_percent")
        val APP_LOCK = booleanPreferencesKey("app_lock_enabled")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val AUTO_LOCK_MIN = intPreferencesKey("auto_lock_minutes")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MIN = intPreferencesKey("reminder_minute")
        val LAST_USER_ID = longPreferencesKey("last_user_id")
        val LOGO_PATH = stringPreferencesKey("logo_path")
        val DEVICE_BIND = booleanPreferencesKey("device_binding_enabled")
    }

    val settings: Flow<SchoolSettings> = context.dataStore.data.map { p ->
        SchoolSettings(
            schoolName = p[K.SCHOOL_NAME] ?: "SMA Negeri 1 Contoh",
            schoolAddress = p[K.SCHOOL_ADDRESS] ?: "",
            checkInStart = p[K.CHECK_IN_START] ?: "05:30",
            onTimeUntil = p[K.ON_TIME_UNTIL] ?: "06:45",
            lateUntil = p[K.LATE_UNTIL] ?: "07:30",
            checkOutFrom = p[K.CHECK_OUT_FROM] ?: "15:00",
            schoolDays = p[K.SCHOOL_DAYS]?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: setOf(1, 2, 3, 4, 5),
            qrEnabled = p[K.QR_ENABLED] ?: true,
            qrRotationSeconds = p[K.QR_ROTATION] ?: 30,
            qrValidityMinutes = p[K.QR_VALIDITY] ?: 15,
            locationCheckEnabled = p[K.LOC_ENABLED] ?: false,
            schoolLatitude = p[K.SCHOOL_LAT] ?: 0.0,
            schoolLongitude = p[K.SCHOOL_LNG] ?: 0.0,
            radiusMeters = p[K.RADIUS] ?: 150,
            wifiCheckEnabled = p[K.WIFI_ENABLED] ?: false,
            wifiSsid = p[K.WIFI_SSID] ?: "",
            warnThresholdPercent = p[K.WARN_THRESHOLD] ?: 90,
            criticalThresholdPercent = p[K.CRITICAL_THRESHOLD] ?: 80,
            appLockEnabled = p[K.APP_LOCK] ?: false,
            biometricEnabled = p[K.BIOMETRIC] ?: true,
            autoLockMinutes = p[K.AUTO_LOCK_MIN] ?: 5,
            reminderHour = p[K.REMINDER_HOUR] ?: 7,
            reminderMinute = p[K.REMINDER_MIN] ?: 0,
            logoPath = p[K.LOGO_PATH] ?: "",
            deviceBindingEnabled = p[K.DEVICE_BIND] ?: false,
        )
    }

    suspend fun current(): SchoolSettings = settings.first()

    suspend fun save(transform: suspend (SchoolSettings) -> SchoolSettings) {
        val cur = current()
        val next = transform(cur)
        context.dataStore.edit { p ->
            p[K.SCHOOL_NAME] = next.schoolName
            p[K.SCHOOL_ADDRESS] = next.schoolAddress
            p[K.CHECK_IN_START] = next.checkInStart
            p[K.ON_TIME_UNTIL] = next.onTimeUntil
            p[K.LATE_UNTIL] = next.lateUntil
            p[K.CHECK_OUT_FROM] = next.checkOutFrom
            p[K.SCHOOL_DAYS] = next.schoolDays.joinToString(",")
            p[K.QR_ENABLED] = next.qrEnabled
            p[K.QR_ROTATION] = next.qrRotationSeconds
            p[K.QR_VALIDITY] = next.qrValidityMinutes
            p[K.LOC_ENABLED] = next.locationCheckEnabled
            p[K.SCHOOL_LAT] = next.schoolLatitude
            p[K.SCHOOL_LNG] = next.schoolLongitude
            p[K.RADIUS] = next.radiusMeters
            p[K.WIFI_ENABLED] = next.wifiCheckEnabled
            p[K.WIFI_SSID] = next.wifiSsid
            p[K.WARN_THRESHOLD] = next.warnThresholdPercent
            p[K.CRITICAL_THRESHOLD] = next.criticalThresholdPercent
            p[K.APP_LOCK] = next.appLockEnabled
            p[K.BIOMETRIC] = next.biometricEnabled
            p[K.AUTO_LOCK_MIN] = next.autoLockMinutes
            p[K.REMINDER_HOUR] = next.reminderHour
            p[K.REMINDER_MIN] = next.reminderMinute
            p[K.LOGO_PATH] = next.logoPath
            p[K.DEVICE_BIND] = next.deviceBindingEnabled
        }
    }

    /** Key-value generik untuk dedup notifikasi & device binding */
    suspend fun kvPut(key: String, value: String) {
        context.dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    suspend fun kvGet(key: String): String? =
        context.dataStore.data.first()[stringPreferencesKey(key)]


    suspend fun saveLastUser(id: Long) {
        context.dataStore.edit { it[K.LAST_USER_ID] = id }
    }

    suspend fun lastUserId(): Long? {
        val v = context.dataStore.data.first()[K.LAST_USER_ID] ?: return null
        return if (v == 0L) null else v
    }

    suspend fun clearLastUser() {
        context.dataStore.edit { it[K.LAST_USER_ID] = 0L }
    }
}
