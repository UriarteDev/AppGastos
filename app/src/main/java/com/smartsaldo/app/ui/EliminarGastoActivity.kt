package com.smartsaldo.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartsaldo.app.R

class EliminarGastoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GastoAdapter
    private lateinit var viewModel: MovimientoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eliminar_gasto)

        recyclerView = findViewById(R.id.recyclerGastos)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = GastoAdapter { gasto ->
            viewModel.eliminarMovimiento(gasto)
            Toast.makeText(this, "Gasto eliminado ✅", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this).get(MovimientoViewModel::class.java)

        // Cargar solo los gastos del usuarioId = 1 (ejemplo)
        viewModel.cargarMovimientos(1)

        viewModel.gastos.observe(this) { lista ->
            adapter.submitList(lista)
        }
    }
}