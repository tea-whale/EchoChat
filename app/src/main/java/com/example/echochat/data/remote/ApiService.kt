package com.example.echochat.data.remote

import com.example.echochat.data.remote.dto.ChatRequest
import com.example.echochat.data.remote.dto.ChatResponse
import com.example.echochat.data.remote.dto.OpenAIModelsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {
    @GET
    suspend fun getModels(
        @Url url: String,
        @Header("Authorization") auth: String
    ): OpenAIModelsResponse

    @POST
    suspend fun chatCompletions(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): ChatResponse
}
