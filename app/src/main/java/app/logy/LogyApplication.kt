package app.logy

import android.app.Application
import androidx.room.Room
import app.logy.data.LogyDatabase

class LogyApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, LogyDatabase::class.java, "logy.db")
            .fallbackToDestructiveMigration(false)
            .build()
    }
}
