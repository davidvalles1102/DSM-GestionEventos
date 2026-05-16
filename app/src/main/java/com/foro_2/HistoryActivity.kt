package com.foro_2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var textViewEmpty: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var historyAdapter: HistoryEventAdapter
    private var eventsListener: ListenerRegistration? = null
    private var attendedEventIds: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        SystemUIHelper.setupSystemBars(this)

        recyclerView = findViewById(R.id.recyclerViewHistory)
        textViewEmpty = findViewById(R.id.textViewEmpty)
        auth = FirebaseAuth.getInstance()

        historyAdapter = HistoryEventAdapter(emptyList()) { event ->
            val intent = Intent(this, EventDetailsActivity::class.java)
            intent.putExtra("eventId", event.id)
            startActivity(intent)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }

        findViewById<Button>(R.id.btnBackToHome)?.setOnClickListener {
            startActivity(Intent(this, EventsListActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAttendedEvents()
    }

    override fun onPause() {
        super.onPause()
        eventsListener?.remove()
        eventsListener = null
    }

    private fun loadAttendedEvents() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        FirestoreUtil.getUserAttendedEvents(user.uid,
            onSuccess = { eventIds ->
                attendedEventIds = eventIds
                loadAllEvents()
            },
            onFailure = {
                Toast.makeText(this, "Error al cargar historial", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadAllEvents() {
        eventsListener?.remove()
        eventsListener = FirestoreUtil.listenToAllEvents { events ->
            val now = System.currentTimeMillis()
            val pastAttendedEvents = events
                .filter { attendedEventIds.contains(it.id) && it.timestamp < now }
                .sortedByDescending { it.timestamp }

            historyAdapter.updateEvents(pastAttendedEvents)
            textViewEmpty.visibility = if (pastAttendedEvents.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (pastAttendedEvents.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
