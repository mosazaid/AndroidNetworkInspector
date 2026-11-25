package com.iiab.networkinspector.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NetworkCallEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NetworkInspectorDatabase : RoomDatabase() {
    abstract fun networkCallDao(): NetworkCallDao
}

