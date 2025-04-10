package com.example.findcourse

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses")
    suspend fun getAll(): List<AddressEntity>

    @Insert
    suspend fun insert(address: AddressEntity)

    @Delete
    suspend fun delete(address: AddressEntity)
}