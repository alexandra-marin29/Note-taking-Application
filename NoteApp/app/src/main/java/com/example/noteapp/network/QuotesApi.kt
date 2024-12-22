package com.example.noteapp.network

import com.example.noteapp.model.Quote
import retrofit2.Response
import retrofit2.http.GET

interface QuotesApi {
    @GET("api/random")
    suspend fun getRandomQuote(): Response<List<Quote>>
}
