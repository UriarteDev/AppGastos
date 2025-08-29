package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.example.myapplication.R
import com.example.myapplication.db.entities.Movimiento

class AddSaldoActivity : AppCompatActivity() {
    private val viewModel: MovimientoViewModel by viewModels()
    private var usuarioId: Long = 1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_saldo)

        // Retrieve usuarioId from Intent
        usuarioId = intent.getLongExtra("usuarioId", 1L)

        val edtSaldo = findViewById<EditText>(R.id.edtSaldo)
        val edtGasto = findViewById<EditText>(R.id.edtGastosFijos)
        val edtAhorro = findViewById<EditText>(R.id.edtAhorro)

        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        btnGuardar.setOnClickListener {
            val saldo = edtSaldo.text.toString().toFloatOrNull() ?: 0f
            val gasto = edtGasto.text.toString().toFloatOrNull() ?: 0f
            val ahorro = edtAhorro.text.toString().toFloatOrNull() ?: 0f

            // Pass usuarioId correctly to Movimiento
            if (saldo > 0f) viewModel.insertarMovimiento(Movimiento(tipo = "INGRESO", monto = saldo, usuarioId = usuarioId))
            if (gasto > 0f) viewModel.insertarMovimiento(Movimiento(tipo = "GASTO_FIJO", monto = gasto, usuarioId = usuarioId))
            if (ahorro > 0f) viewModel.insertarMovimiento(Movimiento(tipo = "AHORRO", monto = ahorro, usuarioId = usuarioId))

            val resultIntent = Intent().apply {
                putExtra("saldo", saldo)
                putExtra("gastosFijos", gasto)
                putExtra("ahorro", ahorro)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}