package com.example.aplimpeza

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import android.location.Location
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import kotlin.coroutines.resume
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.google.firebase.Firebase
import retrofit2.HttpException

class MainActivity : AppCompatActivity() {
    private lateinit var txtStatus: TextView
    private lateinit var btnBuscarLocais: Button
    private lateinit var btnReportar: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var etDescricao: TextInputEditText

    private lateinit var localAdapter: MostrarLocais
    private val listaDeLocais = mutableListOf<Local>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var ultimaFoto: Uri? = null
    private var localizacaoAtual: Location? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>

    private lateinit var storage: FirebaseStorage
    private val PERMISSOES_REQ = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){ permissoes: Map<String,Boolean> ->
        if(permissoes.all{it.value==true}){
            lifecycleScope.launch {
                //obterLocalizacaoeAbrirCamera()
            }

        } else{
            Log.w("Permissoes","Permissao negada!!")
            Toast.makeText(this,"Permissões de Localização e Câmera são necessárias",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        txtStatus = findViewById(R.id.txtStatus)
        btnBuscarLocais = findViewById(R.id.btnBuscarLocais)
        btnReportar = findViewById(R.id.btnReportar)
        recyclerView = findViewById(R.id.recyclerViewLocais)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        storage = Firebase.storage
        etDescricao = findViewById(R.id.etDescricao)

        setupRecyclerView()
        iCameraLauncher()


        btnBuscarLocais.setOnClickListener{
            locaisPendentes()
        }

        btnReportar.setOnClickListener {
            novoLocal()
        }

    }

    private fun setupRecyclerView() {
        localAdapter = MostrarLocais(listaDeLocais,{local -> abrirMapa(local)},
            {local -> marcarComoLimpo(local)})

        recyclerView.adapter = localAdapter

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun iCameraLauncher(){
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { isSuccess: Boolean ->
            if (isSuccess) {
                Log.d("FluxoReporte", "Foto tirada com sucesso! URI: $ultimaFoto")

                val fotoUri = ultimaFoto
                val loc = localizacaoAtual

                if (fotoUri != null && loc != null) {
                    lifecycleScope.launch {
                        uploadFoto(fotoUri, loc)
                    }

                } else {
                    Log.e("FluxoReporte", "Erro: URI da foto ou localização está nula após a foto.")
                    txtStatus.text = "Erro ao guardar foto ou localização."
                }

            } else {
                Log.w("FluxoReporte", "Utilizador cancelou a câmara.")
                txtStatus.text = "Reporte cancelado."
            }
        }
    }
    private suspend fun uploadFoto(fotoUri: Uri,loc: Location){
        txtStatus.text = "Foto OK! A fazer upload para o Firebase..."
        Log.d("FluxoReporte", "Passo 3: A iniciar upload da foto $fotoUri. Local: ${loc.latitude}")
        try {
            val nomeFicheiro = "reporte_${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child("reportes/$nomeFicheiro")


            storageRef.putFile(fotoUri).await()


            val downloadUrl = storageRef.downloadUrl.await()

            Log.d("FluxoReporte", "Upload com sucesso! URL: $downloadUrl")


            enviarReporteParaServidor(downloadUrl, loc)

        } catch (e: Exception) {
            Log.e("FluxoReporte", "Falha no upload para o Firebase", e)
            txtStatus.text = "Erro ao fazer upload da foto: ${e.message}"
        }
    }
    private fun enviarReporteParaServidor(downloadUrl: Uri, localizacao: Location) {
        txtStatus.text = "Upload OK! A enviar reporte para o servidor..."
        Log.d("FluxoReporte", "Passo 4: A enviar para o servidor Node.js.")
        var textoDescricao = etDescricao.text.toString()

        if (textoDescricao.isEmpty()) {
            textoDescricao = "Sem descrição fornecida."
        }
        val novoLocal = InserirLocal(
            latitude = localizacao.latitude,
            longitude = localizacao.longitude,
            urlFoto = downloadUrl.toString(),
            descricao = textoDescricao
        )

        lifecycleScope.launch {
            try {
                val localCriado = Cliente.getInstance(this@MainActivity).reportarLocal(novoLocal)

                txtStatus.text = "Reporte Concluído! ID: ${localCriado.id}"
                Log.d("FluxoReporte", "Reporte enviado com sucesso! $localCriado")

                etDescricao.setText("")
                locaisPendentes()

                localizacaoAtual = null
                ultimaFoto = null

            } catch (e: Exception) {
                if (e is HttpException) {
                    val code = e.code()

                    if (code == 403 || code == 401) {

                        Toast.makeText(this@MainActivity, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()

                        fazerLogout()
                        return@launch
                    }
                }
                Log.e("FluxoReporte", "Falha ao enviar para o servidor Node.js", e)
                txtStatus.text = "Erro ao enviar reporte: ${e.message}"
            }
        }
    }
    private fun locaisPendentes(){
        txtStatus.text = "A buscar locais pendentes..."
        lifecycleScope.launch {
            try {
                val locais = Cliente.getInstance(this@MainActivity).getLocaisPendentes()

                if (locais.isNotEmpty()) {

                    listaDeLocais.clear()
                    listaDeLocais.addAll(locais)
                    localAdapter.notifyDataSetChanged()

                    txtStatus.text = "Mostrando ${locais.size} locais."
                    Log.d("API_SUCESSO_GET", "Dados recebidos: $locais")
                } else {
                    txtStatus.text = "Nenhum local pendente encontrado."
                    listaDeLocais.clear()
                    localAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                if (e is HttpException) {
                    val code = e.code()

                    if (code == 403 || code == 401) {
                        Toast.makeText(this@MainActivity, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()

                        fazerLogout()
                        return@launch
                    }
                }
                txtStatus.text = "Erro ao buscar dados: ${e.message}"
                Log.e("API_ERRO_GET", "Falha na chamada de rede", e)
            }
        }
    }

    private fun todasPermissoes(): Boolean{
        return PERMISSOES_REQ.all {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun abrirCamera() {
        txtStatus.text = "GPS OK! A abrir a câmara..."
        Log.d("FluxoReporte", "Passo 2: A preparar para abrir a câmara.")

        val uri = criarPastaImagem()
        if (uri == null) {
            Log.e("FluxoReporte", "Não foi possível criar a URI do ficheiro.")
            txtStatus.text = "Erro: Não foi possível preparar a câmera."
            return
        }
        this.ultimaFoto = uri
        cameraLauncher.launch(uri)
    }

    private fun criarPastaImagem(): Uri?{
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val nomeFicheiro = "JPEG_${timestamp}.jpg"

        val diretorioStorage = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)

        return try {
            val ficheiroImagem = File(diretorioStorage, nomeFicheiro)

            val authority = "${applicationContext.packageName}.provider"
            FileProvider.getUriForFile(this, authority, ficheiroImagem)

        } catch (e: Exception) {
            Log.e("FileProvider", "Erro ao criar URI do ficheiro", e)
            null
        }
    }

    private suspend fun getLocalAtual(): Location?{
        val temPermissao = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!temPermissao) {
            Log.w("GPS", "getLocalAtual chamada sem permissão.")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->

                    Log.d("GPS", "Sucesso no callback. Local: $location")
                    continuation.resume(location)
                }
                .addOnFailureListener { e ->

                    Log.e("GPS", "Falha no callback.", e)
                    continuation.resume(null)
                }

        }
    }

    private suspend fun localeFoto(){
        try {
            txtStatus.text = "A obter localização GPS... "
            Log.d("FluxoReporte", "A obter localização... ")

            val localizacao = getLocalAtual()

            if(localizacao != null){
                this.localizacaoAtual = localizacao
                Log.d("FluxoReporte", "Localização obtida: Lon ${localizacao.longitude}, Lat ${localizacao.latitude}")
                abrirCamera()
            } else {
                Log.e("FluxoReporte", "localizacao NULL")
                txtStatus.text = "Erro: Não foi possível obter a localização."
                Toast.makeText(this, "Verifique se o GPS está ligado.", Toast.LENGTH_LONG).show()
            }


        }catch (e: Exception){
            Log.e("FluxoReporte", "Exceção ao obter localização", e)
            txtStatus.text = "Erro: ${e.message}"
        }
        txtStatus.text = "Permissões OK! A obter GPS..."
        Log.d("FluxoReporte", "Passo 1: Obter Localização e depois abrir câmera.")
    }
    private fun novoLocal(){
        if (todasPermissoes()) {
            Log.d("Permissoes", "App já tem as permissões.")
            lifecycleScope.launch {
                localeFoto()
            }

        } else {
            Log.i("Permissoes", "A pedir permissões...")
            requestPermissionsLauncher.launch(PERMISSOES_REQ)
        }
    }

    private fun marcarComoLimpo(local: Local) {
        txtStatus.text = "A limpar o local ${local.id}..."

        lifecycleScope.launch {
            try {
                val response = Cliente.getInstance(this@MainActivity).limpar(local.id)

                if (response.isSuccessful) {
                    localAdapter.removerLocal(local)

                    txtStatus.text = "Local ${local.id} limpo com sucesso!"
                    Log.d("API_SUCESSO_PUT", "Local ${local.id} limpo.")
                    Toast.makeText(this@MainActivity, "Limpo!", Toast.LENGTH_SHORT).show()

                } else {
                    txtStatus.text = "Erro da API ao limpar: ${response.code()}"
                    Log.e("API_ERRO_PUT", "API respondeu com erro: ${response.code()}")
                }

            } catch (e: Exception) {

                if (e is HttpException) {
                    val code = e.code()

                    if (code == 403 || code == 401) {

                        Toast.makeText(this@MainActivity, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()

                        fazerLogout()
                        return@launch
                    }
                }
                txtStatus.text = "Erro de rede ao limpar: ${e.message}"
                Log.e("API_ERRO_PUT", "Falha na chamada de rede", e)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun fazerLogout() {
        val sessionManager = GerenciadorSessao(this)
        sessionManager.clearData()

        val intent = Intent(this, LoginAtividade::class.java)


        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                fazerLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun abrirMapa(local: Local) {
        try {

            val uri = Uri.parse("geo:${local.latitude},${local.longitude}?q=${local.latitude},${local.longitude}(Local Sujo)")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)

            startActivity(mapIntent)

        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao abrir mapa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}