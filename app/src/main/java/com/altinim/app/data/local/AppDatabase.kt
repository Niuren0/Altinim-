package com.altinim.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// exportSchema = true: her versiyon değişikliğinde Room, o anki şemayı
// app/schemas/.../<version>.json olarak dışa aktarır (ksp room.schemaLocation
// build.gradle.kts'te ayarlı). Bu dosyalar git'e commit edilmeli — Room,
// autoMigration üretirken eski/yeni şema JSON'larını karşılaştırıyor.
@Database(entities = [GoldEntry::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun goldEntryDao(): GoldEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "altinim.db"
                )
                    .build().also { INSTANCE = it }
            }
        }
    }
}