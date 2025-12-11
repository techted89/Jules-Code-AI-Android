package com.example.julesapp.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Data Classes
data class ApiKeyRequest(val key: String)
data class Project(val id: String, val name: String, val repo: String?, val branch: String?, val status: String)
data class CreateProjectRequest(val name: String, val repo: String, val branch: String)
data class ChatRequest(val projectId: String, val message: String)
data class ChatResponse(val message: String, val status: String, val progress: Float, val logs: List<String>)
data class StatusResponse(val status: String, val progress: Float, val logs: List<String>)

// API Interface
interface JulesApiService {
    @POST("/api/key")
    suspend fun setApiKey(@Body request: ApiKeyRequest): Result<Unit>

    @GET("/projects")
    suspend fun getProjects(): List<Project>

    @POST("/projects")
    suspend fun createProject(@Body request: CreateProjectRequest): Project

    @POST("/chat")
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse
    
    @GET("/status")
    suspend fun getStatus(@Query("projectId") projectId: String): StatusResponse
}

// Authentication Interceptor
class AuthInterceptor : Interceptor {
    private var apiKey: String = ""

    fun setApiKey(key: String) {
        this.apiKey = key
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
        if (apiKey.isNotEmpty()) {
            builder.header("Authorization", "Bearer $apiKey")
        }
        return chain.proceed(builder.build())
    }
}

// Singleton for API Access
object RetrofitInstance {
    private const val BASE_URL = "https://jules.google.com/api/" // Assumed base URL
    
    val authInterceptor = AuthInterceptor()
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: JulesApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JulesApiService::class.java)
    }
}
