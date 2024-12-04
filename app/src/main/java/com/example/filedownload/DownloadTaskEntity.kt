package com.example.filedownload

import androidx.room.*

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val downloadedSize: Long,
    val totalSize: Long,
    val status: String // 下载状态
)