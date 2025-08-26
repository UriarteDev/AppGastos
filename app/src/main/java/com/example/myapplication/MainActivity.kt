package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import android.graphics.Color
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pieChart = findViewById(R.id.pieChart)

        // Datos iniciales
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(25f, "Gastos Fijos"))
        entries.add(PieEntry(25f, "Ahorro"))
        entries.add(PieEntry(25f, "Dinero Gastado"))
        entries.add(PieEntry(25f, "Disponible"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            "#FF9800".toColorInt(), // naranja gastos fijos
            "#4CAF50".toColorInt(), // verde ahorro
            "#F44336".toColorInt(), // rojo gastado
            "#2196F3".toColorInt()  // azul disponible
        )

        // Configuración estilo dona
        pieChart.holeRadius = 60f
        pieChart.setHoleColor(Color.WHITE)
        pieChart.centerText = "Ingresos\n0"
        pieChart.setCenterTextSize(16f)

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }
}