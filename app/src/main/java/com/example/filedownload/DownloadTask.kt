package com.example.filedownload



data class DownloadTask(
    val id: String,
    val url: String,
    val fileName: String,
    var downloadedSize: Long,
    var totalSize: Long,
    var status: DownloadStatus




)