package com.bzh.dytt.db

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import android.util.Log
import androidx.work.*
import com.bzh.dytt.vo.MovieDetail
import com.bzh.dytt.workers.FetchVideoDetailWorker

@Database(
        entities = [MovieDetail::class],
        version = 5
)
abstract class DyttDB : RoomDatabase() {

    abstract fun movieDetailDao(): MovieDetailDAO

    companion object {

        const val TAG = "DyttDB"

        @VisibleForTesting
        val DATABASE_NAME = "dytt.db"

        @Volatile
        private var instance: DyttDB? = null

        fun getInstance(context: Context): DyttDB {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        // Create and pre-populate the database. See this article for more details:
        // https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1#4785
        private fun buildDatabase(context: Context): DyttDB {
            return Room.databaseBuilder(context, DyttDB::class.java, DyttDB.DATABASE_NAME)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Log.d(TAG, "onCreate() called with: db = [$db]")
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)

                            val constraints = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                Constraints.Builder()
                                        .setRequiresCharging(true)
                                        .setRequiresDeviceIdle(true)
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .setRequiresBatteryNotLow(true)
                                        .setRequiresStorageNotLow(true)
                                        .build()
                            } else {
                                Constraints.Builder()
                                        .setRequiresCharging(true)
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .setRequiresBatteryNotLow(true)
                                        .setRequiresStorageNotLow(true)
                                        .build()
                            }

                            val workRequest = OneTimeWorkRequest
                                    .Builder(FetchVideoDetailWorker::class.java)
                                    .setConstraints(constraints)
                                    .build()

                            WorkManager.getInstance().enqueue(workRequest)

                            Log.d(TAG, "onOpen() called with: db = [$db]")
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
        }
    }

}