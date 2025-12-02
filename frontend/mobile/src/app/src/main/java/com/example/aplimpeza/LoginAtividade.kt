package com.example.aplimpeza

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginAtividade: AppCompatActivity() {
    private lateinit var sessionManager: GerenciadorSessao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_atividade)

        sessionManager = GerenciadorSessao(this)

        if (sessionManager.fetchAuthToken() != null) {
            irParaMain()
        }

        val etEmail = findViewById<EditText>(R.id.etLoginEmail)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtRegistar = findViewById<TextView>(R.id.txtIrParaRegisto)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha email e password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val request = Login(email, password)
                    val response = Cliente.getInstance(this@LoginAtividade).login(request)

                    if (response.isSuccessful && response.body() != null) {
                        val authResponse = response.body()!!


                        if (authResponse.token != null) {

                            sessionManager.saveAuthToken(authResponse.token, null)

                            irParaMain()
                        }
                    } else {
                        Toast.makeText(this@LoginAtividade, "Login falhou: verifique os dados", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginAtividade, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Link para Registar
        txtRegistar.setOnClickListener {
            startActivity(Intent(this, RegistroAtividade::class.java))
        }
    }

    private fun irParaMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}