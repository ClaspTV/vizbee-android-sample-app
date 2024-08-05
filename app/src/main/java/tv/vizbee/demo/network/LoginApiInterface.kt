package tv.vizbee.demo.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import tv.vizbee.demo.model.LoginRequest
import tv.vizbee.demo.model.LoginResponse
import tv.vizbee.demo.model.RegCodeStatusRequest

interface LoginApiInterface {
    @POST("v1/signin")
    fun signIn(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @POST("v1/signout")
    fun signOut(
        @Header("Authorization") authToken: String,
    ): Call<Any>

    @POST("v1/accountregcode/{regCode}/status")
    fun updateRegCodeStatus(
        @Path("regCode") regCode: String,
        @Header("Authorization") authToken: String,
        @Body regCodeStatusRequest: RegCodeStatusRequest
    ): Call<Any>
}