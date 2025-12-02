package com.example.aplimpeza

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import android.content.Context
import java.util.concurrent.TimeUnit


class Cliente{
    companion object{
        private const val URL = "http://31.97.241.54:3000/"
        private var apiService: Api? = null

        fun getInstance(context: Context): Api {
            if(apiService==null){
                val gerenciador = GerenciadorSessao(context)
                val cliente = OkHttpClient.Builder().addInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()

                    val token = gerenciador.fetchAuthToken()
                    if (token != null) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }

                    chain.proceed(requestBuilder.build())
                }.connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .client(cliente)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                apiService = retrofit.create(Api::class.java)
            }
            return  apiService!!
        }
    }
}