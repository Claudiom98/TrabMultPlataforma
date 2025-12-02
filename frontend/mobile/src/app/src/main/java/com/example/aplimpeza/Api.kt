package com.example.aplimpeza

import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.http.PUT
import retrofit2.http.Path

interface Api{

    @GET("locais/pendentes")
    suspend fun getLocaisPendentes(): List<Local>

    @POST("inserir")
    suspend fun reportarLocal(@Body novoLocal: InserirLocal): Local

    @PUT("locais/{id}/limpar")
    suspend fun limpar(@Path("id") localId: Int): Response<Unit>

    @POST("usuarios/login")
    suspend fun login(@Body request: Login): Response<Autenticacao>

    @POST("usuarios/cadastro")
    suspend fun registrar(@Body request: Registro): Response<Autenticacao>
}