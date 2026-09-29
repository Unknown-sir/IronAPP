package com.ironpanel.app.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface PanelApi {
    @GET
    suspend fun appJson(@Url url: String): AppSnapshot

    @GET
    suspend fun status(@Url url: String): StatusSnapshot
}

object ApiFactory {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Base URL is a placeholder: every call uses an absolute @Url.
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://localhost/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: PanelApi = retrofit.create(PanelApi::class.java)
}
