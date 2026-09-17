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

    @POST("auth/login/")
    suspend fun loginWithPassword(@Body req: LoginRequestDto): AuthResponseDto

    @POST("auth/logout/")
    suspend fun logout(): Response<Unit>

    @GET("auth/me/")
    suspend fun getMe(): AuthResponseDto

    @POST("auth/register/")
    suspend fun register(@Body req: RegisterRequestDto): AuthResponseDto

    @POST("auth/verify/")
    suspend fun verifyEmail(@Body req: VerifyCodeRequestDto): AuthResponseDto

    @POST("auth/resend-code/")
    suspend fun resendCode(@Body req: ResendCodeRequestDto): AuthResponseDto

    @POST("auth/password/change/")
    suspend fun changePassword(@Body req: PasswordChangeRequestDto): AuthResponseDto

    @POST("auth/password/reset/")
    suspend fun resetPassword(@Body req: PasswordResetRequestDto): AuthResponseDto

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
