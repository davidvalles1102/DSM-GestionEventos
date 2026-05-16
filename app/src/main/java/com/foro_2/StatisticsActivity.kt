package com.foro_2

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityStatisticsBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

class StatisticsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var eventsListener: ListenerRegistration? = null
    private var attendancesListener: ListenerRegistration? = null
    private var currentRole: String = "usuario"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configurar barras del sistema después de setContentView
        SystemUIHelper.setupSystemBars(this)
        
        firebaseAuth = FirebaseAuth.getInstance()
        
        loadUserRole()
    }
    
    private fun loadUserRole() {
        val user = firebaseAuth.currentUser ?: return
        
        FirestoreUtil.getUserRole(user.uid,
            onSuccess = { role ->
                currentRole = role ?: Roles.USUARIO
                loadStatistics()
            },
            onFailure = {
                currentRole = "usuario"
                loadStatistics()
            }
        )
    }
    
    private fun loadStatistics() {
        val user = firebaseAuth.currentUser ?: return
        
        if (currentRole == Roles.ORGANIZADOR) {
            // Estadísticas del organizador
            eventsListener = FirestoreUtil.listenToOrganizerEvents(user.uid) { events ->
                binding.textViewTotalEvents.text = "Total eventos creados: ${events.size}"
                loadOrganizerStats(events)
            }
        } else {
            // Estadísticas del usuario
            FirestoreUtil.getUserAttendedEvents(user.uid,
                onSuccess = { eventIds ->
                    loadUserStats(eventIds)
                },
                onFailure = {
                    Toast.makeText(this, "Error al cargar estadísticas", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
    
    private fun loadUserStats(eventIds: List<String>) {
        // Cargar todos los eventos para obtener fechas
        eventsListener = FirestoreUtil.listenToAllEvents { allEvents ->
            val attendedEvents = allEvents.filter { eventIds.contains(it.id) }
            val notAttendedEvents = allEvents.filter { !eventIds.contains(it.id) && it.timestamp < System.currentTimeMillis() }
            
            binding.textViewTotalAttended.text = "Eventos asistidos: ${attendedEvents.size}"
            
            // Gráfica circular
            val pieEntries = listOf(
                PieEntry(attendedEvents.size.toFloat(), "Asistí"),
                PieEntry(notAttendedEvents.size.toFloat(), "No asistí")
            )
            
            val pieDataSet = PieDataSet(pieEntries, "").apply {
                colors = listOf(
                    getColor(R.color.chart_green),
                    getColor(R.color.chart_red)
                )
                valueTextSize = 14f
                valueTextColor = Color.WHITE
            }
            
            binding.pieChart.apply {
                data = PieData(pieDataSet)
                description.isEnabled = false
                setCenterText("Asistencias")
                setCenterTextSize(16f)
                animateY(1000)
                invalidate()
            }
            
            // Gráfica de barras por mes
            val eventsByMonth = attendedEvents.groupBy {
                val calendar = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                calendar.get(Calendar.MONTH)
            }
            
            val barEntries = (0..11).map { month ->
                BarEntry(month.toFloat(), eventsByMonth[month]?.size?.toFloat() ?: 0f)
            }
            
            val barDataSet = BarDataSet(barEntries, "Eventos por mes").apply {
                color = getColor(R.color.chart_blue)
                valueTextSize = 12f
            }
            
            binding.barChart.apply {
                data = BarData(barDataSet)
                description.isEnabled = false
                animateY(1000)
                invalidate()
            }
        }
    }
    
    private fun loadOrganizerStats(events: List<Event>) {
        // Para organizadores, mostrar estadísticas de sus eventos
        val totalAttendances = events.size // Simplificado
        
        binding.textViewTotalAttended.text = "Total de eventos: ${events.size}"
        
        // Gráfica de barras por mes
        val eventsByMonth = events.groupBy {
            val calendar = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            calendar.get(Calendar.MONTH)
        }
        
        val barEntries = (0..11).map { month ->
            BarEntry(month.toFloat(), eventsByMonth[month]?.size?.toFloat() ?: 0f)
        }
        
        val barDataSet = BarDataSet(barEntries, "Eventos creados por mes").apply {
            color = getColor(R.color.chart_blue)
            valueTextSize = 12f
        }
        
        binding.barChart.apply {
            data = BarData(barDataSet)
            description.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }
    
    override fun onPause() {
        super.onPause()
        eventsListener?.remove()
        attendancesListener?.remove()
    }
}

