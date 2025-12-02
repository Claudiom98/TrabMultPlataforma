package com.example.aplimpeza

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MostrarLocais(
    private val locais: MutableList<Local>,
    private val onMapaClick: (local: Local) -> Unit,
    private val onLimparClick: (local: Local) -> Unit
): RecyclerView.Adapter<MostrarLocais.ViewHolder>(){
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDescricao: TextView = view.findViewById(R.id.txtDescricaoItem)
        val txtDataReporte: TextView = view.findViewById(R.id.txtDataReporteItem)
        val btnLimpar: Button = view.findViewById(R.id.btnLimparItem)
        val imgFoto: ImageView =view.findViewById(R.id.imgFotoLocal)
        val btnMapa: Button = view.findViewById(R.id.btnMapaItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.locais_view, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val local = locais[position]

        holder.txtDescricao.text = local.descricao ?: "Sem descrição"

        holder.txtDataReporte.text = "Reportado em: ${local.dataReporte.substringBefore("T")}"
        if (local.urlFoto.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(local.urlFoto)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(holder.imgFoto)
        } else {

            holder.imgFoto.setImageResource(android.R.drawable.ic_menu_camera)
        }


        holder.btnLimpar.setOnClickListener {
            onLimparClick(local)
        }

        holder.btnMapa.setOnClickListener {
            onMapaClick(local)
        }
    }
    override fun getItemCount(): Int {
        return locais.size
    }
    fun removerLocal(local: Local) {

        val position = locais.indexOf(local)
        if (position > -1) {
            locais.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
