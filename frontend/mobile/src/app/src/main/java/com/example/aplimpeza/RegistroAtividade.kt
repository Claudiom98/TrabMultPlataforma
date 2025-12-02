package com.example.aplimpeza

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RegistroAtividade: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registro_activity)

        val etNome = findViewById<EditText>(R.id.etNome)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegistar = findViewById<Button>(R.id.btnRegistar)

        btnRegistar.setOnClickListener {
            val nome = etNome.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()


            if (nome.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val request = Registro(email, password, nome)


                    val response = Cliente.getInstance(this@RegistroAtividade).registrar(request)

                    if (response.isSuccessful) {
                        Toast.makeText(this@RegistroAtividade, "Registo com sucesso!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@RegistroAtividade, LoginAtividade::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@RegistroAtividade, "Erro: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RegistroAtividade, "Erro de rede: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}