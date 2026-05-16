package com.foro_2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityEventDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class EventDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEventDetailsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var eventId: String? = null
    private var currentEvent: Event? = null
    private var attendanceStatus: String? = null
    private var commentsListener: ListenerRegistration? = null
    private var currentRole: String = "usuario"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configurar barras del sistema después de setContentView
        SystemUIHelper.setupSystemBars(this)
        
        firebaseAuth = FirebaseAuth.getInstance()
        eventId = intent.getStringExtra("eventId")
        
        if (eventId == null) {
            Toast.makeText(this, "Error: Evento no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupUI()
        loadUserRole()
        loadEvent()
        checkAttendance()
        loadComments()
        loadAttendeesCount()
    }
    
    private fun setupUI() {
        binding.btnConfirmAttendance.setOnClickListener {
            confirmAttendance()
        }
        
        binding.btnCancelAttendance.setOnClickListener {
            cancelAttendance()
        }
        
        binding.btnComments.setOnClickListener {
            val intent = Intent(this, CommentsActivity::class.java)
            intent.putExtra("eventId", eventId)
            startActivity(intent)
        }
        
        binding.btnShare.setOnClickListener {
            shareEvent()
        }
    }
    
    private fun loadUserRole() {
        val user = firebaseAuth.currentUser ?: return
        
        FirestoreUtil.getUserRole(user.uid,
            onSuccess = { role ->
                currentRole = role ?: Roles.USUARIO
                updateUIForRole()
            },
            onFailure = {
                currentRole = "usuario"
                updateUIForRole()
            }
        )
    }
    
    private fun updateUIForRole() {
        if (currentRole == Roles.ORGANIZADOR && currentEvent?.organizerId == firebaseAuth.currentUser?.uid) {
            binding.btnEditEvent.visibility = android.view.View.VISIBLE
            binding.btnEditEvent.setOnClickListener {
                val intent = Intent(this, CreateEventActivity::class.java)
                intent.putExtra("eventId", eventId)
                startActivity(intent)
            }
        } else {
            binding.btnEditEvent.visibility = android.view.View.GONE
        }
    }
    
    private fun loadEvent() {
        FirestoreUtil.getEvent(eventId!!,
            onSuccess = { event ->
                if (event != null) {
                    currentEvent = event
                    binding.textViewTitle.text = event.title
                    binding.textViewDate.text = "${event.date} ${event.time}"
                    binding.textViewLocation.text = event.location
                    binding.textViewDescription.text = event.description
                    updateUIForRole()
                } else {
                    Toast.makeText(this, "Evento no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            onFailure = {
                Toast.makeText(this, "Error al cargar evento: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun checkAttendance() {
        val user = firebaseAuth.currentUser ?: return
        
        FirestoreUtil.checkUserAttendance(user.uid, eventId!!,
            onSuccess = { status ->
                attendanceStatus = status
                updateAttendanceButtons()
            },
            onFailure = {
                attendanceStatus = null
                updateAttendanceButtons()
            }
        )
    }
    
    private fun updateAttendanceButtons() {
        if (attendanceStatus == AttendanceStatus.CONFIRMED) {
            binding.btnConfirmAttendance.visibility = android.view.View.GONE
            binding.btnCancelAttendance.visibility = android.view.View.VISIBLE
        } else {
            binding.btnConfirmAttendance.visibility = android.view.View.VISIBLE
            binding.btnCancelAttendance.visibility = android.view.View.GONE
        }
    }
    
    private fun confirmAttendance() {
        val user = firebaseAuth.currentUser ?: return
        
        val attendance = Attendance(
            userId = user.uid,
            eventId = eventId!!,
            status = AttendanceStatus.CONFIRMED
        )
        
        FirestoreUtil.confirmAttendance(attendance,
            onSuccess = {
                Toast.makeText(this, "Asistencia confirmada", Toast.LENGTH_SHORT).show()
                checkAttendance()
                loadAttendeesCount()
            },
            onFailure = {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun cancelAttendance() {
        val user = firebaseAuth.currentUser ?: return
        
        FirestoreUtil.cancelAttendance(user.uid, eventId!!,
            onSuccess = {
                Toast.makeText(this, "Asistencia cancelada", Toast.LENGTH_SHORT).show()
                checkAttendance()
                loadAttendeesCount()
            },
            onFailure = {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun loadAttendeesCount() {
        FirestoreUtil.getConfirmedAttendeesCount(eventId!!,
            onSuccess = { count ->
                binding.textViewAttendeesCount.text = "$count asistentes confirmados"
            },
            onFailure = {
                binding.textViewAttendeesCount.text = "0 asistentes confirmados"
            }
        )
    }
    
    private fun loadComments() {
        commentsListener = FirestoreUtil.listenToEventComments(eventId!!) { comments ->
            if (comments.isNotEmpty()) {
                val previewComments = comments.take(2)
                binding.textViewCommentsPreview.text = previewComments.joinToString("\n") { 
                    "${it.userName}: ${it.text}"
                }
            } else {
                binding.textViewCommentsPreview.text = "No hay comentarios aún"
            }
        }
    }
    
    private fun shareEvent() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        val eventTitle = currentEvent?.title ?: "Evento"
        val eventDate = currentEvent?.date ?: ""
        val eventLocation = currentEvent?.location ?: ""
        shareIntent.putExtra(Intent.EXTRA_TEXT, "$eventTitle\nFecha: $eventDate\nUbicación: $eventLocation")
        startActivity(Intent.createChooser(shareIntent, "Compartir evento"))
    }
    
    override fun onPause() {
        super.onPause()
        commentsListener?.remove()
        commentsListener = null
    }
}

