package com.example.project.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM user_table WHERE user_name = :userName AND password = :password")
    suspend fun getUser(userName: String, password: String): User?
}
