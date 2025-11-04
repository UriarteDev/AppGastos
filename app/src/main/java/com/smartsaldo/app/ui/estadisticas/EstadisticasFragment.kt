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
import com.smartsaldo.app.R
import com.smartsaldo.app.data.local.entities.TipoTransaccion
import com.smartsaldo.app.databinding.FragmentEstadisticasBinding
import com.smartsaldo.app.ui.home.TransaccionViewModel
import com.smartsaldo.app.utils.CurrencyHelper
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
                Periodo.SEMANAL -> mostrarEstadisticasPorDias(7, getString(R.string.ultimos_7_dias))
                Periodo.QUINCENAL -> mostrarEstadisticasPorDias(15, getString(R.string.ultimos_15_dias))
                Periodo.MENSUAL -> mostrarEstadisticasPorMeses(12, getString(R.string.ultimos_12_meses))
                Periodo.ANUAL -> mostrarEstadisticasPorAños(5, getString(R.string.ultimos_5_anos))
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

            val ingresosDataSet = BarDataSet(ingresos, getString(R.string.ingresos)).apply {
                color = Color.parseColor("#4CAF50")
                valueTextColor = Color.BLACK
                valueTextSize = 9f
            }

            val gastosDataSet = BarDataSet(gastos, getString(R.string.gastos)).apply {
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

            tvTotalIngresos.text = getString(
                R.string.ingresos_monto,
                CurrencyHelper.formatAmount(requireContext(), totalIngresos)
            )
            tvTotalGastos.text = getString(
                R.string.gastos_monto,
                CurrencyHelper.formatAmount(requireContext(), totalGastos)
            )
            tvSaldoTotal.text = getString(
                R.string.saldo_monto,
                CurrencyHelper.formatAmount(requireContext(), saldo)
            )

            tvSaldoTotal.setTextColor(
                if (saldo >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
            )
        }
    }

    private fun getMesNombre(mes: Int): String {
        return when (mes) {
            0 -> getString(R.string.ene)
            1 -> getString(R.string.feb)
            2 -> getString(R.string.mar)
            3 -> getString(R.string.abr)
            4 -> getString(R.string.may)
            5 -> getString(R.string.jun)
            6 -> getString(R.string.jul)
            7 -> getString(R.string.ago)
            8 -> getString(R.string.sep)
            9 -> getString(R.string.oct)
            10 -> getString(R.string.nov)
            11 -> getString(R.string.dic)
            else -> ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}