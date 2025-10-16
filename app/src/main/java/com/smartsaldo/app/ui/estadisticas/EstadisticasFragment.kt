package com.smartsaldo.app.ui.estadisticas

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.chip.Chip
import com.smartsaldo.app.data.local.entities.TipoTransaccion
import com.smartsaldo.app.databinding.FragmentEstadisticasBinding
import com.smartsaldo.app.ui.home.TransaccionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class EstadisticasFragment : Fragment() {

    private var _binding: FragmentEstadisticasBinding? = null
    private val binding get() = _binding!!

    private val transaccionViewModel: TransaccionViewModel by activityViewModels()

    private var periodoSeleccionado = Periodo.MENSUAL

    enum class Periodo {
        SEMANAL, QUINCENAL, MENSUAL, ANUAL
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEstadisticasBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPeriodoChips()
        cargarEstadisticas(Periodo.MENSUAL)
    }

    private fun setupPeriodoChips() {
        binding.apply {
            chipSemanal.setOnClickListener {
                periodoSeleccionado = Periodo.SEMANAL
                cargarEstadisticas(Periodo.SEMANAL)
                actualizarChipsSeleccion(chipSemanal)
            }
            chipQuincenal.setOnClickListener {
                periodoSeleccionado = Periodo.QUINCENAL
                cargarEstadisticas(Periodo.QUINCENAL)
                actualizarChipsSeleccion(chipQuincenal)
            }
            chipMensual.setOnClickListener {
                periodoSeleccionado = Periodo.MENSUAL
                cargarEstadisticas(Periodo.MENSUAL)
                actualizarChipsSeleccion(chipMensual)
            }
            chipAnual.setOnClickListener {
                periodoSeleccionado = Periodo.ANUAL
                cargarEstadisticas(Periodo.ANUAL)
                actualizarChipsSeleccion(chipAnual)
            }
        }
    }

    private fun actualizarChipsSeleccion(chipSeleccionado: Chip) {
        binding.apply {
            chipSemanal.isChecked = chipSeleccionado == chipSemanal
            chipQuincenal.isChecked = chipSeleccionado == chipQuincenal
            chipMensual.isChecked = chipSeleccionado == chipMensual
            chipAnual.isChecked = chipSeleccionado == chipAnual
        }
    }

    private fun cargarEstadisticas(periodo: Periodo) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (periodo) {
                Periodo.SEMANAL -> mostrarEstadisticasPorDias(7, "Últimos 7 días")
                Periodo.QUINCENAL -> mostrarEstadisticasPorDias(15, "Últimos 15 días")
                Periodo.MENSUAL -> mostrarEstadisticasPorMeses(12, "Últimos 12 meses")
                Periodo.ANUAL -> mostrarEstadisticasPorAños(5, "Últimos 5 años")
            }
        }
    }

    private fun mostrarEstadisticasPorDias(dias: Int, titulo: String) {
        val ingresos = mutableListOf<BarEntry>()
        val gastos = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        // Obtener todas las transacciones del usuario
        viewLifecycleOwner.lifecycleScope.launch {
            transaccionViewModel.transacciones.collect { todasLasTransacciones ->
                val ahora = Calendar.getInstance()
                val hace_dias = Calendar.getInstance()
                hace_dias.add(Calendar.DAY_OF_YEAR, -(dias - 1))

                // Limpiar listas
                ingresos.clear()
                gastos.clear()
                labels.clear()

                repeat(dias) { index ->
                    val fechaActual = Calendar.getInstance()
                    fechaActual.set(hace_dias.get(Calendar.YEAR), hace_dias.get(Calendar.MONTH), hace_dias.get(
                        Calendar.DAY_OF_MONTH))
                    fechaActual.add(Calendar.DAY_OF_YEAR, index)

                    val inicioDelDia = fechaActual.apply { set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
                    val finDelDia = fechaActual.apply { set(Calendar.HOUR_OF_DAY, 23) }.timeInMillis

                    val transaccionesDelDia = todasLasTransacciones.filter {
                        it.transaccion.fecha in inicioDelDia..finDelDia
                    }

                    val totalIngresos = transaccionesDelDia
                        .filter { it.transaccion.tipo == TipoTransaccion.INGRESO }
                        .sumOf { it.transaccion.monto }

                    val totalGastos = transaccionesDelDia
                        .filter { it.transaccion.tipo == TipoTransaccion.GASTO }
                        .sumOf { it.transaccion.monto }

                    ingresos.add(BarEntry(index.toFloat(), totalIngresos.toFloat()))
                    gastos.add(BarEntry(index.toFloat(), totalGastos.toFloat()))
                    labels.add(fechaActual.get(Calendar.DAY_OF_MONTH).toString())
                }

                mostrarGraficoBarras(ingresos, gastos, labels, titulo)
            }
        }
    }

    private fun mostrarEstadisticasPorMeses(cantidad: Int, titulo: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            transaccionViewModel.transacciones.collect { todasLasTransacciones ->
                val ingresos = mutableListOf<BarEntry>()
                val gastos = mutableListOf<BarEntry>()
                val labels = mutableListOf<String>()

                var fechaActual = Calendar.getInstance()
                fechaActual.add(Calendar.MONTH, -(cantidad - 1))

                repeat(cantidad) { index ->
                    val year = fechaActual.get(Calendar.YEAR)
                    val month = fechaActual.get(Calendar.MONTH)

                    val inicioDelMes = Calendar.getInstance().apply {
                        set(year, month, 1, 0, 0, 0)
                    }.timeInMillis

                    val finDelMes = Calendar.getInstance().apply {
                        set(year, month + 1, 0, 23, 59, 59)
                    }.timeInMillis

                    val transaccionesDelMes = todasLasTransacciones.filter {
                        it.transaccion.fecha in inicioDelMes..finDelMes
                    }

                    val totalIngresos = transaccionesDelMes
                        .filter { it.transaccion.tipo == TipoTransaccion.INGRESO }
                        .sumOf { it.transaccion.monto }

                    val totalGastos = transaccionesDelMes
                        .filter { it.transaccion.tipo == TipoTransaccion.GASTO }
                        .sumOf { it.transaccion.monto }

                    ingresos.add(BarEntry(index.toFloat(), totalIngresos.toFloat()))
                    gastos.add(BarEntry(index.toFloat(), totalGastos.toFloat()))
                    labels.add(getMesNombre(month))

                    fechaActual.add(Calendar.MONTH, 1)
                }

                mostrarGraficoBarras(ingresos, gastos, labels, titulo)
            }
        }
    }

    private fun mostrarEstadisticasPorAños(cantidad: Int, titulo: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            transaccionViewModel.transacciones.collect { todasLasTransacciones ->
                val ingresos = mutableListOf<BarEntry>()
                val gastos = mutableListOf<BarEntry>()
                val labels = mutableListOf<String>()

                var anoActual = Calendar.getInstance().get(Calendar.YEAR) - (cantidad - 1)

                repeat(cantidad) { index ->
                    val year = anoActual + index

                    val inicioDelAno = Calendar.getInstance().apply {
                        set(year, 0, 1, 0, 0, 0)
                    }.timeInMillis

                    val finDelAno = Calendar.getInstance().apply {
                        set(year, 11, 31, 23, 59, 59)
                    }.timeInMillis

                    val transaccionesDelAno = todasLasTransacciones.filter {
                        it.transaccion.fecha in inicioDelAno..finDelAno
                    }

                    val totalIngresos = transaccionesDelAno
                        .filter { it.transaccion.tipo == TipoTransaccion.INGRESO }
                        .sumOf { it.transaccion.monto }

                    val totalGastos = transaccionesDelAno
                        .filter { it.transaccion.tipo == TipoTransaccion.GASTO }
                        .sumOf { it.transaccion.monto }

                    ingresos.add(BarEntry(index.toFloat(), totalIngresos.toFloat()))
                    gastos.add(BarEntry(index.toFloat(), totalGastos.toFloat()))
                    labels.add(year.toString())
                }

                mostrarGraficoBarras(ingresos, gastos, labels, titulo)
            }
        }
    }

    private fun mostrarGraficoBarras(
        ingresos: List<BarEntry>,
        gastos: List<BarEntry>,
        labels: List<String>,
        titulo: String
    ) {
        binding.apply {
            tvTituloEstadistica.text = titulo

            val ingresosDataSet = BarDataSet(ingresos, "Ingresos").apply {
                color = Color.parseColor("#4CAF50")
                valueTextColor = Color.BLACK
                valueTextSize = 9f
            }

            val gastosDataSet = BarDataSet(gastos, "Gastos").apply {
                color = Color.parseColor("#F44336")
                valueTextColor = Color.BLACK
                valueTextSize = 9f
            }

            val barData = BarData(ingresosDataSet, gastosDataSet)
            barData.barWidth = 0.35f

            barChart.apply {
                data = barData
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.granularity = 1f
                xAxis.isGranularityEnabled = true
                xAxis.labelCount = minOf(labels.size, 6)
                axisLeft.axisMinimum = 0f
                axisRight.isEnabled = false
                legend.isEnabled = true
                description.isEnabled = false
                animateY(800)
                invalidate()
            }

            // Mostrar resumen
            val totalIngresos = ingresos.sumOf { it.y.toDouble() }
            val totalGastos = gastos.sumOf { it.y.toDouble() }
            val saldo = totalIngresos - totalGastos

            tvTotalIngresos.text = "Ingresos: S/ ${String.format("%.2f", totalIngresos)}"
            tvTotalGastos.text = "Gastos: S/ ${String.format("%.2f", totalGastos)}"
            tvSaldoTotal.text = "Saldo: S/ ${String.format("%.2f", saldo)}"

            tvSaldoTotal.setTextColor(
                if (saldo >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
            )
        }
    }

    private fun getMesNombre(mes: Int): String {
        return when (mes) {
            0 -> "Ene"
            1 -> "Feb"
            2 -> "Mar"
            3 -> "Abr"
            4 -> "May"
            5 -> "Jun"
            6 -> "Jul"
            7 -> "Ago"
            8 -> "Sep"
            9 -> "Oct"
            10 -> "Nov"
            11 -> "Dic"
            else -> ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}