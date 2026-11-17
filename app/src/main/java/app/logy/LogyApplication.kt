package app.logy

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.logy.data.LogyDatabase

class LogyApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, LogyDatabase::class.java, "logy.db")
            .addMigrations(MIGRATION_1_2)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("INSERT OR IGNORE INTO categories (`name`, `color`) VALUES ('General', 4287339766)")
                }
            })
            .build()
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (`name` TEXT NOT NULL, `color` INTEGER NOT NULL, PRIMARY KEY(`name`))")
                db.execSQL("INSERT OR IGNORE INTO categories (`name`, `color`) VALUES ('General', 4287339766)")
                db.execSQL("INSERT OR IGNORE INTO categories (`name`, `color`) SELECT DISTINCT category, color FROM events WHERE category != ''")
                db.execSQL("ALTER TABLE entries ADD COLUMN duration_minutes INTEGER")
            }
        }
    }
}
