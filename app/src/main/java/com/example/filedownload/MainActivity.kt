package com.example.filedownload

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val urls = listOf(
    "https://pfga0.market.xiaomi.com/download/AppStore/0cc64268241da4a879135b27cff95007397ca36f2",
    "https://pfga0.market.xiaomi.com/download/AppStore/07ecd2fd696384b609bf4803337d2b3e9f394a060",
    "https://pfga0.market.xiaomi.com/download/AppStore/0fd93dd911a334f7ba5633873a8ab3b310b630068"
)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val task = DownloadTask(
            id = urls[0],
            url = urls[0],
            fileName = "largefile.zip",
            downloadedSize = 0,
            totalSize = 0,
            status = DownloadStatus.PENDING
        )
        val dao = AppDatabase.getDatabase(this)
        val downloadManager = DownloadManager(dao.downloadTaskDao(), RetrofitClient.service)

        findViewById<Button>(R.id.btn_download).setOnClickListener {


            lifecycleScope.launch(Dispatchers.IO) {


                // 添加任务
                downloadManager.addTask(task)
            }
        }

        findViewById<Button>(R.id.btn_pause).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                downloadManager.pauseTask(task.id)
            }
        }
        findViewById<Button>(R.id.btn_resume).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                // 恢复任务
                downloadManager.resumeTask(task.id)
            }
        }


         findViewById<Button>(R.id.btn_delete).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                //  删除任务
                downloadManager.removeTask(task.id)
            }
        }







    }

}