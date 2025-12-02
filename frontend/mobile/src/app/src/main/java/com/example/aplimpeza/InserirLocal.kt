package com.example.aplimpeza

import com.google.gson.annotations.SerializedName

data class InserirLocal (
    val longitude : Double,
    val latitude: Double,

    @SerializedName("url_foto")

    val urlFoto: String,

    val descricao: String?
)