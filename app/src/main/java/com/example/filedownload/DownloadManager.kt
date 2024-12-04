package com.example.filedownload

import android.util.Log
import okhttp3.ResponseBody
import retrofit2.Call
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.concurrent.Executors

class DownloadManager(
    private val dao: DownloadTaskDao,
    private val downloadService: DownloadService
) {

    private val executor = Executors.newFixedThreadPool(4)

    /**
     * 用于管理所有下载任务的状态和信息。我们使用任务的 id 来唯一标识和管理任务。
     */
    private val tasks = mutableMapOf<String, DownloadTask>()

    // 存储正在进行的下载请求
    private val ongoingDownloads = mutableMapOf<String, Call<ResponseBody>>()

    // 添加任务
    fun addTask(task: DownloadTask) {
        tasks[task.id] = task
        dao.insertTask(task.toEntity())
        startTask(task)
    }

    // 暂停任务
    fun pauseTask(taskId: String) {
        tasks[taskId]?.let {
            it.status = DownloadStatus.PAUSED
            dao.updateTask(it.toEntity())// 更新任务状态到数据库
            ongoingDownloads[taskId]?.cancel()// 取消正在进行的下载请求
            ongoingDownloads.remove(taskId)// 移除正在进行的下载请求

        }
    }

    // 恢复任务
    fun resumeTask(taskId: String) {
        tasks[taskId]?.let { task ->
            if (task.status == DownloadStatus.PAUSED) {
                task.status = DownloadStatus.PENDING
                dao.updateTask(task.toEntity())
                startTask(task)
            }
        }
    }

    // 删除任务
    fun removeTask(taskId: String) {
        tasks[taskId]?.let { task ->
            // 如果任务正在下载，先停止下载
            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PENDING) {
                // 取消正在进行的下载请求
                ongoingDownloads[taskId]?.cancel() // 停止下载
                ongoingDownloads.remove(taskId) // 从 ongoingDownloads 中移除
            }

            // 删除文件（可选，视需求而定）
            val downloadDir = DownloadConfig.downloadDirectory
            val file = File(downloadDir, task.fileName)
            if (file.exists()) {
                file.delete() // 删除文件
            }

            // 从任务列表中移除任务
            tasks.remove(taskId)

            // 从数据库中删除任务或更新任务状态为已删除
            task.status = DownloadStatus.FAILED  // 可以选择更新为失败状态，也可以标记为删除
            dao.updateTask(task.toEntity()) // 更新数据库

            // 清理正在进行的下载任务
            ongoingDownloads.remove(taskId) // 清理 ongoingDownloads 中的任务记录
        }
    }

    // 开始单个任务
    private fun startTask(task: DownloadTask) {

        executor.submit {
            if (task.status != DownloadStatus.PENDING) return@submit
            task.status = DownloadStatus.DOWNLOADING

            val downloadDir = DownloadConfig.downloadDirectory
            if (downloadDir == null || (!downloadDir.exists() && !downloadDir.mkdirs())) {
                task.status = DownloadStatus.FAILED
                dao.updateTask(task.toEntity())
                return@submit
            }

            val file = File(downloadDir, task.fileName)
            // 确保文件夹存在
            if (file.parentFile?.exists() != true) {
                file.parentFile?.mkdirs()
            }

            val call = downloadService.downloadFile("bytes=${task.downloadedSize}-", task.url)
            ongoingDownloads[task.id] = call// 将当前下载请求保存到ongoingDownloads
            try {
                val response = call.execute()
                if (response.isSuccessful) {
                    val buffer = ByteArray(8192) // 缓冲区大小
                    var bytesRead: Int

                    // 使用 use 函数确保资源在使用后自动关闭
                    response.body()?.byteStream()?.use { inputStream ->
                        RandomAccessFile(file, "rw").use { randomAccessFile ->
                            randomAccessFile.seek(task.downloadedSize) // 设置文件指针位置

                            // 从输入流读取数据并写入文件
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                randomAccessFile.write(buffer, 0, bytesRead)
                                task.downloadedSize += bytesRead

                                // 只在下载块较大时或按需更新数据库
                                if (task.downloadedSize % (2 * 1024 * 1024) == 0.toLong()) { // 每下载 2MB 更新一次数据库
                                    dao.updateTask(task.toEntity())
                                }
                            }
                        }
                    }
                    // 更新数据库，在下载结束后
                    dao.updateTask(task.toEntity())
                    task.status = DownloadStatus.COMPLETED
                } else {
                    task.status = DownloadStatus.FAILED
                }
            } catch (e: Exception) {
                Log.d(DownloadConfig.TAG, "startTask: ${e.message}")
                if (task.status != DownloadStatus.PAUSED) {
                    // 如果任务被暂停，不进入这里。因为如果是暂停任务，使用call.cancel()取下下载线程会被中断也会报错进入这里从而导致下载状态被错误的更新成FAILED
                    task.status = DownloadStatus.FAILED
                }
            } finally {
                dao.updateTask(task.toEntity())
                //当任务下载完成、失败或暂停时，应该将它从 ongoingDownloads 中移除
                ongoingDownloads.remove(task.id)
            }
        }
    }

    // 多线程分块下载
    fun downloadFileInChunks(task: DownloadTask) {
        val totalSize = getFileSize(task.url)
        val chunkSize = 2 * 1024 * 1024 // 每块 2MB


        val downloadDir = DownloadConfig.downloadDirectory
        if (downloadDir == null || (!downloadDir.exists() && !downloadDir.mkdirs())) {
            task.status = DownloadStatus.FAILED
            dao.updateTask(task.toEntity())
            return
        }

        val file = File(downloadDir, task.fileName)

        // 确保文件夹存在
        if (file.parentFile?.exists() == true) {
            file.parentFile?.mkdirs()
        }


        val randomAccessFile = RandomAccessFile(file, "rw")

        var start = 0L
        val chunks = mutableListOf<Pair<Long, Long>>()

        while (start < totalSize) {
            val end = (start + chunkSize).coerceAtMost(totalSize)
            chunks.add(start to end)
            start = end + 1
        }

        chunks.forEach { (start, end) ->
            executor.submit {
                val call = downloadService.downloadFile("bytes=$start-$end", task.url)
                val response = call.execute()
                if (response.isSuccessful) {
                    response.body()?.byteStream()?.use { input ->
                        randomAccessFile.seek(start)
//                        input.copyTo(randomAccessFile)

                        input.copyTo(object : OutputStream() {
                            override fun write(b: Int) {
                                synchronized(randomAccessFile) {
                                    randomAccessFile.write(b)  // 确保在写入时同步访问
                                }
                            }

                            override fun write(b: ByteArray, off: Int, len: Int) {
                                synchronized(randomAccessFile) {
                                    randomAccessFile.write(b, off, len)  // 同样在这里使用同步块
                                }
                            }
                        })


                    }
                }
            }
        }
    }


    // 获取文件大小
    private fun getFileSize(url: String): Long {
        try {
            val call = downloadService.downloadFile("bytes=0-", url)
            val response = call.execute()
            if (response.isSuccessful) {
                val contentRange = response.headers()["Content-Range"]
                val sizeString = contentRange?.split("/")?.get(1) // 提取总大小
                return sizeString?.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }
}
