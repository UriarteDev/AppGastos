package com.smartsaldo.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.smartsaldo.app.R
import com.smartsaldo.app.db.entities.Movimiento

class AddGastoActivity : AppCompatActivity() {

    private val movimientoViewModel: MovimientoViewModel by viewModels()
    private var usuarioId: Long = 0L  // lo recibiremos desde el intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_gasto)

        // Recibir el usuarioId que mandes desde MainActivity
        usuarioId = intent.getLongExtra("usuarioId", 0L)

        val etNombreGasto = findViewById<EditText>(R.id.etNombreGasto)
        val etPrecioGasto = findViewById<EditText>(R.id.etPrecioGasto)
        val btnGuardarGasto = findViewById<Button>(R.id.btnGuardarGasto)

        btnGuardarGasto.setOnClickListener {
            val nombre = etNombreGasto.text.toString().trim()
            val montoTexto = etPrecioGasto.text.toString().trim()

            if (nombre.isEmpty() || montoTexto.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val monto = montoTexto.toFloatOrNull()
            if (monto == null || monto <= 0f) {
                Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val movimiento = Movimiento(
                nombre = nombre,
                monto = monto,
                tipo = "GASTO",
                usuarioId = usuarioId
            )

            movimientoViewModel.insertarMovimiento(movimiento)

            Toast.makeText(this, "Gasto añadido", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}