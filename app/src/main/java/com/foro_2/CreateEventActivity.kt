package com.foro_2

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityCreateEventBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class CreateEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEventBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var imageUri: Uri? = null
    private var eventId: String? = null // Para edición
    private var currentRole: String = "usuario"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configurar barras del sistema después de setContentView
        SystemUIHelper.setupSystemBars(this)

        firebaseAuth = FirebaseAuth.getInstance()

        // Verificar rol del usuario
        val user = firebaseAuth.currentUser
        if (user != null) {
            FirestoreUtil.getUserRole(user.uid,
                onSuccess = { role ->
                    currentRole = role ?: Roles.USUARIO
                    if (currentRole != Roles.ORGANIZADOR && eventId == null) {
                        // Si no es organizador y está creando (no editando), cerrar la actividad
                        Toast.makeText(this, "Solo los organizadores pueden crear eventos", Toast.LENGTH_SHORT).show()
                        finish()
                        return@getUserRole
                    }
                    initializeActivity()
                },
                onFailure = {
                    Log.e("CreateEventActivity", "Error al obtener rol: ${it.message}")
                    Toast.makeText(this, "Error al verificar permisos", Toast.LENGTH_SHORT).show()
                    finish()
                }
            )
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeActivity() {
        // Verificar si es edición
        eventId = intent.getStringExtra("eventId")
        if (eventId != null) {
            loadEventData(eventId!!)
            binding.btnSaveEvent.text = "Actualizar evento"
        }

        setupUI()
    }

    private fun setupUI() {
        // Botón seleccionar imagen
        binding.btnSelectImage.setOnClickListener {
            // Por ahora, solo placeholder. Se puede implementar selección de imagen real
            Toast.makeText(this, "Selección de imagen próximamente", Toast.LENGTH_SHORT).show()
        }

        // Botón seleccionar fecha
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Botón seleccionar hora
        binding.etTime.setOnClickListener {
            showTimePicker()
        }

        // Botón guardar
        binding.btnSaveEvent.setOnClickListener {
            saveEvent()
        }

        // Botón cancelar
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDay = String.format("%02d", selectedDay)
            val formattedMonth = String.format("%02d", selectedMonth + 1)
            binding.etDate.setText("$formattedDay/$formattedMonth/$selectedYear")
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedHour = String.format("%02d", selectedHour)
            val formattedMinute = String.format("%02d", selectedMinute)
            binding.etTime.setText("$formattedHour:$formattedMinute")
        }, hour, minute, true).show()
    }

    private fun saveEvent() {
        val user = firebaseAuth.currentUser ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val title = binding.etTitle.text.toString()
        val description = binding.etDescription.text.toString()
        val date = binding.etDate.text.toString()
        val time = binding.etTime.text.toString()
        val location = binding.etLocation.text.toString()

        // Validaciones
        if (title.isEmpty()) {
            Toast.makeText(this, "Ingresa el título del evento", Toast.LENGTH_SHORT).show()
            return
        }

        if (date.isEmpty()) {
            Toast.makeText(this, "Selecciona una fecha", Toast.LENGTH_SHORT).show()
            return
        }

        if (time.isEmpty()) {
            Toast.makeText(this, "Selecciona una hora", Toast.LENGTH_SHORT).show()
            return
        }

        if (location.isEmpty()) {
            Toast.makeText(this, "Ingresa la ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        val event = if (eventId != null) {
            // Edición
            Event(
                id = eventId!!,
                organizerId = user.uid,
                title = title,
                description = description,
                date = date,
                time = time,
                location = location,
                imageUrl = "", // Por ahora sin imagen
                timestamp = parseEventDate(date, time),
                createdAt = System.currentTimeMillis()
            )
        } else {
            // Creación
            Event(
                organizerId = user.uid,
                title = title,
                description = description,
                date = date,
                time = time,
                location = location,
                imageUrl = "",
                timestamp = parseEventDate(date, time),
                createdAt = System.currentTimeMillis()
            )
        }

        binding.btnSaveEvent.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE
        if (eventId != null) {
            FirestoreUtil.updateEvent(event,
                onSuccess = {
                    binding.btnSaveEvent.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Evento actualizado exitosamente", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = {
                    binding.btnSaveEvent.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            if (currentRole != Roles.ORGANIZADOR) {
                binding.btnSaveEvent.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Solo los organizadores pueden crear eventos", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("CreateEventActivity", "Intentando crear evento: $title")
            FirestoreUtil.createEvent(event,
                onSuccess = {
                    binding.btnSaveEvent.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    Log.d("CreateEventActivity", "Evento creado exitosamente")
                    Toast.makeText(this, "Evento creado exitosamente", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = { error ->
                    binding.btnSaveEvent.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    Log.e("CreateEventActivity", "Error al crear evento: ${error.message}", error)
                    Toast.makeText(this, "Error al crear evento: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun parseEventDate(date: String, time: String): Long {
        try {
            val parts = date.split("/")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt() - 1
                val year = parts[2].toInt()

                val timeParts = time.split(":")
                val hour = if (timeParts.size >= 1) timeParts[0].toInt() else 0
                val minute = if (timeParts.size >= 2) timeParts[1].toInt() else 0

                val calendar = Calendar.getInstance()
                calendar.set(year, month, day, hour, minute, 0)
                return calendar.timeInMillis
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return System.currentTimeMillis()
    }

    private fun loadEventData(eventId: String) {
        FirestoreUtil.getEvent(eventId,
            onSuccess = { event ->
                if (event != null) {
                    binding.etTitle.setText(event.title)
                    binding.etDescription.setText(event.description)
                    binding.etDate.setText(event.date)
                    binding.etTime.setText(event.time)
                    binding.etLocation.setText(event.location)
                }
            },
            onFailure = {
                Toast.makeText(this, "Error al cargar evento", Toast.LENGTH_SHORT).show()
            }
        )
    }
}