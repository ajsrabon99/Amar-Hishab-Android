package com.ajyra.amarhishab.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AmarHishabApiService {

    @POST("auth/exchange/")
    suspend fun exchangeAuthCode(@Body req: AuthExchangeRequestDto): AuthExchangeResponseDto

    @POST("auth/login/")
    suspend fun loginWithPassword(@Body req: LoginRequestDto): AuthResponseDto

    @POST("auth/register/")
    suspend fun register(@Body req: RegisterRequestDto): AuthResponseDto

    @POST("auth/google/")
    suspend fun googleLogin(@Body req: GoogleAuthRequestDto): AuthResponseDto

    @POST("auth/logout/")
    suspend fun logout(): Response<Unit>

    @GET("dashboard/")
    suspend fun getDashboard(): DashboardResponseDto

    @GET("transactions/")
    suspend fun getTransactions(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("account") account: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int? = null
    ): List<TransactionDto>

    @POST("transactions/")
    suspend fun createTransaction(@Body req: CreateTransactionRequestDto): TransactionDto

    @PUT("transactions/{id}/")
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body req: CreateTransactionRequestDto
    ): TransactionDto

    @DELETE("transactions/{id}/")
    suspend fun deleteTransaction(@Path("id") id: String): Response<Unit>

    @POST("transfers/")
    suspend fun transferMoney(@Body req: TransferRequestDto): TransactionDto

    @GET("app/version/")
    suspend fun checkAppVersion(): AppVersionResponseDto
}
