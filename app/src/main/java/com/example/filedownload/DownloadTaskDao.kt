package com.example.filedownload

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_tasks")
    fun getAllTasks(): List<DownloadTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: DownloadTaskEntity)

    @Update
    fun updateTask(task: DownloadTaskEntity)

    @Delete
    fun deleteTask(task: DownloadTaskEntity)
}