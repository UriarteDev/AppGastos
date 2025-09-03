package com.smartsaldo.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.smartsaldo.app.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import androidx.activity.viewModels
import com.smartsaldo.app.db.entities.Movimiento

class MainActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private val viewModel: MovimientoViewModel by viewModels()
    private val usuarioId = 1L // ejemplo, usuario fijo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pieChart = findViewById(R.id.pieChart)

        val btnAddSaldo = findViewById<Button>(R.id.btnAddSaldo)
        btnAddSaldo.setOnClickListener {
            val intent = Intent(this, AddSaldoActivity::class.java)
            intent.putExtra("usuarioId", usuarioId)
            startActivityForResult(intent, 1001)
        }

        val btnAddGasto: Button = findViewById(R.id.btnAddGasto)
        btnAddGasto.setOnClickListener {
            val intent = Intent(this, AddGastoActivity::class.java)
            intent.putExtra("usuarioId", usuarioId) // pasar el usuario actual
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnEliminarGasto).setOnClickListener {
            val intent = Intent(this, EliminarGastoActivity::class.java)
            startActivity(intent)
        }


        viewModel.cargarMovimientos(usuarioId)

        // observar movimientos
        viewModel.movimientos.observe(this) { lista ->
            val listaUsuario = lista.filter { it.usuarioId == usuarioId }
            actualizarGrafico(listaUsuario)
        }
    }

    private fun actualizarGrafico(listaUsuario: List<Movimiento>) {
        // Obtener el último saldo ingresado (tipo INGRESO)
        val ultimoSaldo = listaUsuario
            .filter { it.tipo == "INGRESO" }
            .maxByOrNull { it.fecha }?.monto?.toFloat() ?: 0f

        val gastosFijos = listaUsuario.filter { it.tipo == "GASTO_FIJO" }.sumOf { it.monto.toDouble() }.toFloat()
        val ahorros = listaUsuario.filter { it.tipo == "AHORRO" }.sumOf { it.monto.toDouble() }.toFloat()
        val gastos = listaUsuario.filter { it.tipo == "GASTO" }.sumOf { it.monto.toDouble() }.toFloat()
        val disponible = ultimoSaldo - gastosFijos - ahorros - gastos

        val entries = ArrayList<PieEntry>()
        if (gastosFijos > 0f) entries.add(PieEntry(gastosFijos, "Gastos Fijos"))
        if (ahorros > 0f) entries.add(PieEntry(ahorros, "Ahorros"))
        if (gastos > 0f) entries.add(PieEntry(gastos, "Gastos"))
        if (disponible > 0f) entries.add(PieEntry(disponible, "Dinero disponible"))

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("Añade saldo para ver el grafico")
            return
        }

        val dataSet = PieDataSet(entries, "Finanzas")
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS.toList())
        dataSet.valueTextSize = 15f   // en "sp" (default es 13f)
        val pieData = PieData(dataSet)
        pieChart.centerText = "Saldo = $ultimoSaldo"
        pieChart.data = pieData
        pieChart.invalidate()
        pieChart.legend.isEnabled = false
        pieChart.description.isEnabled = false
    }
}
