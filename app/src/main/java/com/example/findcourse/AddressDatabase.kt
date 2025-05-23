package com.example.findcourse

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AddressEntity::class], version = 3)
abstract class AddressDatabase : RoomDatabase() {
    abstract fun addressDao():AddressDao
}
