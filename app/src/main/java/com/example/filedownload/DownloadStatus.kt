package com.example.filedownload

enum class DownloadStatus {

    //表示下载任务已经创建，但尚未开始。例如，任务可能正在等待排队
    PENDING,

    //表示下载正在进行中。
    DOWNLOADING,

    //表示下载已被暂停，可能是由于用户操作或其他原因（比如网络中断）。
    PAUSED,

    //表示下载已成功完成。
    COMPLETED,

    //表示下载失败，可能是由于网络问题、文件不存在或权限不足等。
    FAILED

}