# SiAbsen 📱

Aplikasi absensi sekolah **offline-first** dengan Kotlin + Jetpack Compose.

## Fitur

- **Multi-role**: Siswa, Guru, Wali Kelas, Admin
- **Absensi manual oleh guru** + **QR Code dinamis** (token HMAC berputar, one-time use, anti foto-ulang)
- Check-in / check-out mandiri dengan jendela waktu & status otomatis (Hadir/Terlambat)
- **Pengajuan izin/sakit** + lampiran bukti + alur persetujuan guru
- Riwayat absensi berbentuk **kalender bulanan** per siswa
- Statistik kehadiran (mingguan/bulanan/semester) + rekap kelas
- **Early warning** siswa dengan kehadiran rendah (threshold bisa diatur)
- **Koreksi absensi** wajib menyertai alasan → tercatat di tabel koreksi + **audit log**
- Kelola master data: siswa, guru, kelas, mapel, tahun ajaran, kalender sekolah
- **Import siswa via CSV**, export laporan **CSV/PDF**
- **Backup & restore** seluruh database (JSON, integrity check, mode replace/merge)
- App lock PIN + biometrik
- Notifikasi lokal (reminder belum absen)

## Teknologi

| Layer | Tech |
|---|---|
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| DB | Room (offline-first, semua data di device) |
| Settings | DataStore Preferences |
| Background | WorkManager (reminder periodik) |
| QR | ZXing core (generate) + zxing-android-embedded (scan) |
| Keamanan | PBKDF2 untuk PIN, BiometricPrompt, audit log |
| CI | GitHub Actions |

## Akun demo (seed otomatis saat pertama kali)

| Username | Role | PIN |
|---|---|---|
| admin | Admin | 123456 |
| guru | Guru | 123456 |
| wali | Wali Kelas | 123456 |
| siswa | Siswa (Andi Wijaya) | 123456 |

> Ganti PIN lewat menu Akun setelah login.

## Build via GitHub Actions

1. Push repo ini ke GitHub.
2. Buka tab **Actions** → pilih workflow **Android CI** → jalankan.
3. Unduh artifact `siabsen-debug-apk` atau `siabsen-release-unsigned-apk`.

Build lokal (butuh JDK 17 + Android SDK):

```bash
./gradlew :app:assembleDebug
```

## Struktur

```
app/src/main/java/zaaaam/siabsen/com/
├── data/
│   ├── local/       # Room: entities, DAOs, database
│   ├── repository/  # Business logic
│   ├── backup/      # Backup/restore JSON
│   └── export/      # CSV & PDF
├── security/        # PinHasher (PBKDF2), SessionManager, AuditLogger
├── qr/              # QrCodec (HMAC rotating token), QrImage
├── notification/    # Notifier
├── work/            # ReminderWorker
├── di/              # Hilt modules
└── ui/
    ├── navigation/  # RootNavHost, routes per role
    ├── components/  # Komponen bersama
    └── feature/     # auth, lock, student, guru, admin, shared
```

## Catatan Desain

- Attendance tidak disimpan sebagai `student.status` semata — setiap record terikat pada
  `AttendanceSession` (harian/per mapel) sehingga struktur tetap benar saat fitur berkembang.
- QR berisi `sessionId`, window waktu, dan token HMAC — bukan teks statis yang mudah difoto.
- Siswa tidak bisa mengubah status absensinya sendiri; koreksi hanya lewat guru/wali kelas dan selalu masuk audit log.
