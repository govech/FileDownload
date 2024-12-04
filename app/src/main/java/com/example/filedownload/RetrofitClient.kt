package com.example.filedownload

import okhttp3.OkHttpClient

object RetrofitClient : BaseRetrofitClient() {

    val service by lazy { createService<DownloadService>(DownloadService.BASE_URL) }


    /**
     * 支持动态 Header 如果需要动态设置请求头，可以通过子类扩展 handleBuilder
     */
    override fun handleBuilder(builder: OkHttpClient.Builder) = Unit
}