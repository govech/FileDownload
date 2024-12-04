package com.example.filedownload

fun DownloadTask.toEntity(): DownloadTaskEntity = DownloadTaskEntity(
    id = this.id,
    url = this.url,
    fileName = this.fileName,
    downloadedSize = this.downloadedSize,
    totalSize = this.totalSize,
    status = this.status.name
)


fun DownloadTaskEntity.toTask(): DownloadTask = DownloadTask(
    id = this.id,
    url = this.url,
    fileName = this.fileName,
    downloadedSize = this.downloadedSize,
    totalSize = this.totalSize,
    status = DownloadStatus.valueOf(this.status)
)