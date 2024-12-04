package com.example.filedownload

import android.content.Context
import android.os.Environment
import java.io.File

object DownloadConfig {
    const val TAG = "DownloadManager"



    // 默认路径
    var downloadDirectory: File? = null

    fun initialize(context: Context, customDirectory: File? = null) {
        downloadDirectory = customDirectory ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        downloadDirectory?.let {
            if (!it.exists()) it.mkdirs() // 确保目录存在
        }
    }
}


