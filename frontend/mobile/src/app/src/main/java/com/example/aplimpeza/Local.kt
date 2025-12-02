package com.example.aplimpeza
import com.google.gson.annotations.SerializedName

data class Local(
    val id: Int,
    val descricao: String?,

    @SerializedName("url_foto")
    val urlFoto: String,
    val status: String,
    @SerializedName("data_reporte")
    val dataReporte: String,

    val latitude: Double,
    val longitude: Double
)