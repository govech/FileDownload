package com.example.filedownload

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url

interface DownloadService {
    @GET
    @Streaming
    fun downloadFile(
        @Header("Range") range: String, // 支持断点续传
        @Url fileUrl: String
    ): Call<ResponseBody>


    companion object {
        const val BASE_URL = "https://wanandroid.com/"
    }
}
