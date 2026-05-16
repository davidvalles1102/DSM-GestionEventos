package com.foro_2

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.foro_2.databinding.ActivityEventsListBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class EventsListActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEventsListBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var eventsListener: ListenerRegistration? = null
    private var currentRole: String = "usuario"
    private var allEvents: List<Event> = emptyList()
    private var filteredEvents: List<Event> = emptyList()
    private lateinit var eventsAdapter: EventsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityEventsListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configurar barras del sistema después de setContentView
        SystemUIHelper.setupSystemBars(this)
        
        firebaseAuth = FirebaseAuth.getInstance()
        
        setupToolbar()
        setupDrawer()
        setupRecyclerView()
        setupFloatingActionButton()
        setupFilters()
        loadUserRole()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
        supportActionBar?.title = "Eventos"
    }
    
    private fun setupDrawer() {
        binding.drawerLayout.setDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: android.view.View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: android.view.View) {}
            override fun onDrawerClosed(drawerView: android.view.View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
        
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Ya estamos en home
                    binding.drawerLayout.closeDrawer(binding.navView)
                    true
                }
                R.id.nav_create_event -> {
                    if (currentRole == Roles.ORGANIZADOR) {
                        startActivity(Intent(this, CreateEventActivity::class.java))
                    } else {
                        Toast.makeText(this, "Solo los organizadores pueden crear eventos", Toast.LENGTH_SHORT).show()
                    }
                    binding.drawerLayout.closeDrawer(binding.navView)
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    binding.drawerLayout.closeDrawer(binding.navView)
                    true
                }
                R.id.nav_statistics -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
                    binding.drawerLayout.closeDrawer(binding.navView)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    binding.drawerLayout.closeDrawer(binding.navView)
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Ajustes próximamente", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawer(binding.navView)
                    true
                }
                R.id.nav_logout -> {
                    firebaseAuth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapter(
            events = emptyList(),
            isOrganizer = false,
            onEventClick = { event ->
                val intent = Intent(this, EventDetailsActivity::class.java)
                intent.putExtra("eventId", event.id)
                startActivity(intent)
            },
            onEditClick = { event ->
                val intent = Intent(this, CreateEventActivity::class.java)
                intent.putExtra("eventId", event.id)
                startActivity(intent)
            }
        )
        
        binding.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(this@EventsListActivity)
            adapter = eventsAdapter
        }
    }
    
    private fun setupFloatingActionButton() {
        binding.fabCreateEvent.setOnClickListener {
            if (currentRole == Roles.ORGANIZADOR) {
                startActivity(Intent(this, CreateEventActivity::class.java))
            } else {
                Toast.makeText(this, "Solo los organizadores pueden crear eventos", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupFilters() {
        binding.btnFilterToday.setOnClickListener {
            filterEvents("today")
        }
        
        binding.btnFilterWeek.setOnClickListener {
            filterEvents("week")
        }
        
        binding.btnFilterAll.setOnClickListener {
            filterEvents("all")
        }
    }
    
    private fun filterEvents(filter: String) {
        val calendar = java.util.Calendar.getInstance()
        val today = calendar.timeInMillis
        
        filteredEvents = when (filter) {
            "today" -> {
                allEvents.filter { event ->
                    val eventDate = parseEventDate(event.date, event.time)
                    val sameDay = isSameDay(eventDate, today)
                    sameDay && eventDate >= today
                }
            }
            "week" -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 7)
                val weekLater = calendar.timeInMillis
                allEvents.filter { event ->
                    val eventDate = parseEventDate(event.date, event.time)
                    eventDate >= today && eventDate <= weekLater
                }
            }
            else -> allEvents.filter { event ->
                parseEventDate(event.date, event.time) >= today
            }
        }

        eventsAdapter.updateEvents(filteredEvents)
        updateEmptyState(filteredEvents.isEmpty())
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
                
                val calendar = java.util.Calendar.getInstance()
                calendar.set(year, month, day, hour, minute, 0)
                return calendar.timeInMillis
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }
    
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
    
    private fun loadUserRole() {
        val user = firebaseAuth.currentUser ?: return
        
        FirestoreUtil.getUserRole(
            userId = user.uid,
            onSuccess = { role ->
                currentRole = role ?: Roles.USUARIO
                updateUIForRole()
                loadEvents()
            },
            onFailure = {
                currentRole = "usuario"
                updateUIForRole()
                loadEvents()
            }
        )
    }
    
    private fun updateUIForRole() {
        if (currentRole == Roles.ORGANIZADOR) {
            binding.fabCreateEvent.show()
            binding.navView.menu.findItem(R.id.nav_create_event).isVisible = true
        } else {
            binding.fabCreateEvent.hide()
            binding.navView.menu.findItem(R.id.nav_create_event).isVisible = false
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.textViewEmpty.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerViewEvents.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun loadEvents() {
        val user = firebaseAuth.currentUser ?: return
        
        if (currentRole == Roles.ORGANIZADOR) {
            // Organizador ve solo sus eventos
            eventsListener = FirestoreUtil.listenToOrganizerEvents(user.uid) { events ->
                allEvents = events
                filterEvents("all")
            }
        } else {
            // Usuario ve todos los eventos
            eventsListener = FirestoreUtil.listenToAllEvents { events ->
                allEvents = events
                filterEvents("all")
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_events_list, menu)
        
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                val result = if (newText.isNullOrEmpty()) {
                    filteredEvents
                } else {
                    filteredEvents.filter {
                        it.title.contains(newText, ignoreCase = true) ||
                        it.description.contains(newText, ignoreCase = true) ||
                        it.location.contains(newText, ignoreCase = true)
                    }
                }
                eventsAdapter.updateEvents(result)
                updateEmptyState(result.isEmpty())
                return true
            }
        })
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(binding.navView)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        eventsListener?.remove()
        loadEvents()
    }

    override fun onPause() {
        super.onPause()
        eventsListener?.remove()
        eventsListener = null
    }
}

