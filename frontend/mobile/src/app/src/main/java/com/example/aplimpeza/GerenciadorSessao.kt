package com.example.aplimpeza

import android.content.Context
import android.content.SharedPreferences

class GerenciadorSessao(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    companion object{
        const val PREFS_NAME = "app_limpeza_prefs"
        const val USER_TOKEN = "user_token"
    }

    fun saveAuthToken(token: String, tipo: String?) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }
    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }
    fun clearData() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}