package com.example.filedownload

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化默认下载路径
        DownloadConfig.initialize(this)
        // 如果需要自定义路径
        // DownloadConfig.initialize(this, File("/storage/emulated/0/CustomPath"))
    }
}