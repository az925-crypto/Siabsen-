package zaaaam.siabsen.com.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import zaaaam.siabsen.com.data.local.SiabsenDatabase
import zaaaam.siabsen.com.data.local.dao.AcademicDao
import zaaaam.siabsen.com.data.local.dao.AttendanceDao
import zaaaam.siabsen.com.data.local.dao.AuditDao
import zaaaam.siabsen.com.data.local.dao.AnnouncementDao
import zaaaam.siabsen.com.data.local.dao.BackupDao
import zaaaam.siabsen.com.data.local.dao.LeaveDao
import zaaaam.siabsen.com.data.local.dao.QrDao
import zaaaam.siabsen.com.data.local.dao.RosterDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SiabsenDatabase =
        Room.databaseBuilder(context, SiabsenDatabase::class.java, SiabsenDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun rosterDao(db: SiabsenDatabase): RosterDao = db.rosterDao()
    @Provides fun academicDao(db: SiabsenDatabase): AcademicDao = db.academicDao()
    @Provides fun attendanceDao(db: SiabsenDatabase): AttendanceDao = db.attendanceDao()
    @Provides fun leaveDao(db: SiabsenDatabase): LeaveDao = db.leaveDao()
    @Provides fun auditDao(db: SiabsenDatabase): AuditDao = db.auditDao()
    @Provides fun qrDao(db: SiabsenDatabase): QrDao = db.qrDao()
    @Provides fun backupDao(db: SiabsenDatabase): BackupDao = db.backupDao()
    @Provides fun announcementDao(db: SiabsenDatabase): AnnouncementDao = db.announcementDao()
}
