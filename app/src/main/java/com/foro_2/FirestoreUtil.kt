package com.foro_2

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

// FirestoreUtil object for events and attendance management
object FirestoreUtil {
    private val db = FirebaseFirestore.getInstance()
    private val expensesCollection = db.collection("expenses")
    private val usersCollection = db.collection("users")
    private val historyCollection = db.collection("history")
    
    // Nuevas colecciones para eventos
    private val eventsCollection = db.collection("events")
    private val attendancesCollection = db.collection("attendances")
    private val commentsCollection = db.collection("comments")

    // 1. Function to create/update the user document in Firestore
    fun createUserDocument(
        userId: String,
        email: String,
        role: String = Roles.USUARIO,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userMap = hashMapOf(
            "email" to email,
            "role" to role
        )

        usersCollection.document(userId)
            .set(userMap, SetOptions.merge()) // Use merge to avoid overwriting existing data
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // 2. Function to get the user's role
    fun getUserRole(userId: String, onSuccess: (String?) -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    onSuccess(role)
                } else {
                    onSuccess(null) // User document doesn't exist
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }


    // Función para escuchar cambios en los gastos del usuario
    fun listenToUserExpenses(userId: String, onExpensesChanged: (List<Expense>) -> Unit): ListenerRegistration {
        return expensesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreUtil", "Listen failed.", error)
                    return@addSnapshotListener
                }

                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    Expense(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        name = doc.getString("name") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        category = doc.getString("category") ?: "",
                        date = doc.getString("date") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()

                onExpensesChanged(expenses)
            }
    }

    // Función para agregar un gasto
    fun addExpense(expense: Expense, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val docRef = expensesCollection.document()
        val expenseWithId = expense.copy(id = docRef.id)
        docRef.set(expenseWithId.toMap())
            .addOnSuccessListener {
                // Registrar en el historial
                addToHistory(
                    userId = expense.userId,
                    action = "ADD",
                    expenseName = expense.name,
                    amount = expense.amount,
                    category = expense.category,
                    date = expense.date
                )
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Función para eliminar un gasto
    fun deleteExpense(expenseId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Primero obtenemos los datos del gasto antes de eliminarlo
        expensesCollection.document(expenseId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userId = document.getString("userId") ?: ""
                    val name = document.getString("name") ?: ""
                    val amount = document.getDouble("amount") ?: 0.0
                    val category = document.getString("category") ?: ""
                    val date = document.getString("date") ?: ""

                    // Eliminar el gasto
                    expensesCollection.document(expenseId)
                        .delete()
                        .addOnSuccessListener {
                            // Registrar en el historial
                            addToHistory(
                                userId = userId,
                                action = "DELETE",
                                expenseName = name,
                                amount = amount,
                                category = category,
                                date = date
                            )
                            onSuccess()
                        }
                        .addOnFailureListener { onFailure(it) }
                } else {
                    onFailure(Exception("Gasto no encontrado"))
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Función para actualizar un gasto
    fun updateExpense(expense: Expense, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        expensesCollection.document(expense.id)
            .set(expense.toMap())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Función para obtener el total mensual de gastos
    fun getMonthlyTotal(userId: String, year: Int, month: Int, onSuccess: (Double) -> Unit, onFailure: (Exception) -> Unit) {
        expensesCollection
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val total = snapshot.documents.mapNotNull { doc ->
                    val date = doc.getString("date") ?: return@mapNotNull null
                    val amount = doc.getDouble("amount") ?: 0.0
                    
                    // Parsear fecha (formato esperado: "dd/MM/yyyy")
                    val parts = date.split("/")
                    if (parts.size == 3) {
                        val expenseMonth = parts[1].toIntOrNull() ?: 0
                        val expenseYear = parts[2].toIntOrNull() ?: 0
                        if (expenseMonth == month && expenseYear == year) amount else null
                    } else null
                }.sum()
                onSuccess(total)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // Función para obtener gastos filtrados por categoría
    fun getExpensesByCategory(userId: String, category: String, onSuccess: (List<Expense>) -> Unit, onFailure: (Exception) -> Unit) {
        expensesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { snapshot ->
                val expenses = snapshot.documents.mapNotNull { doc ->
                    Expense(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        name = doc.getString("name") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        category = doc.getString("category") ?: "",
                        date = doc.getString("date") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }
                onSuccess(expenses)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // Función privada para agregar al historial
    private fun addToHistory(
        userId: String,
        action: String,
        expenseName: String,
        amount: Double,
        category: String,
        date: String
    ) {
        Log.d("FirestoreUtil", "Intentando agregar al historial: action=$action, user=$userId, expense=$expenseName")
        
        val historyEntry = HistoryEntry(
            userId = userId,
            action = action,
            expenseName = expenseName,
            amount = amount,
            category = category,
            date = date,
            timestamp = System.currentTimeMillis()
        )
        
        val docRef = historyCollection.document()
        historyEntry.id = docRef.id
        
        Log.d("FirestoreUtil", "Guardando historial con ID: ${docRef.id}")
        
        docRef.set(historyEntry.toMap())
            .addOnSuccessListener {
                Log.d("FirestoreUtil", "✅ Historial guardado exitosamente: ${historyEntry.toMap()}")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreUtil", "❌ Error al guardar historial: ${e.message}", e)
            }
    }

    // Función para escuchar el historial del usuario
    fun listenToUserHistory(userId: String, onHistoryChanged: (List<HistoryEntry>) -> Unit): ListenerRegistration {
        Log.d("FirestoreUtil", "📡 Configurando listener de historial para userId: $userId")
        
        return historyCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreUtil", "❌ Error en listener de historial: ${error.message}", error)
                    return@addSnapshotListener
                }

                Log.d("FirestoreUtil", "📥 Snapshot recibido, documentos: ${snapshot?.documents?.size ?: 0}")
                
                snapshot?.documents?.forEach { doc ->
                    Log.d("FirestoreUtil", "Documento: ${doc.id} -> ${doc.data}")
                }

                val history = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        HistoryEntry(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            action = doc.getString("action") ?: "",
                            expenseName = doc.getString("expenseName") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            category = doc.getString("category") ?: "",
                            date = doc.getString("date") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) {
                        Log.e("FirestoreUtil", "Error al parsear documento: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d("FirestoreUtil", "✅ Historial procesado: ${history.size} entradas")
                onHistoryChanged(history)
            }
    }
    
    // ========== FUNCIONES PARA EVENTOS ==========
    
    // Crear evento (solo organizadores)
    fun createEvent(event: Event, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val docRef = eventsCollection.document()
        val eventWithId = event.copy(id = docRef.id)
        docRef.set(eventWithId.toMap())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
    
    // Actualizar evento
    fun updateEvent(event: Event, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        eventsCollection.document(event.id)
            .set(event.toMap())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
    
    // Eliminar evento
    fun deleteEvent(eventId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        eventsCollection.document(eventId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
    
    // Obtener evento por ID
    fun getEvent(eventId: String, onSuccess: (Event?) -> Unit, onFailure: (Exception) -> Unit) {
        eventsCollection.document(eventId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val event = Event(
                        id = document.id,
                        organizerId = document.getString("organizerId") ?: "",
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        date = document.getString("date") ?: "",
                        time = document.getString("time") ?: "",
                        location = document.getString("location") ?: "",
                        imageUrl = document.getString("imageUrl") ?: "",
                        timestamp = document.getLong("timestamp") ?: 0L,
                        createdAt = document.getLong("createdAt") ?: 0L
                    )
                    onSuccess(event)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
    
    // Escuchar todos los eventos
    fun listenToAllEvents(onEventsChanged: (List<Event>) -> Unit): ListenerRegistration {
        return eventsCollection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreUtil", "Listen to events failed.", error)
                    return@addSnapshotListener
                }
                
                val events = snapshot?.documents?.mapNotNull { doc ->
                    Event(
                        id = doc.id,
                        organizerId = doc.getString("organizerId") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        location = doc.getString("location") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                
                onEventsChanged(events)
            }
    }
    
    // Escuchar eventos de un organizador
    fun listenToOrganizerEvents(organizerId: String, onEventsChanged: (List<Event>) -> Unit): ListenerRegistration {
        return eventsCollection
            .whereEqualTo("organizerId", organizerId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreUtil", "Listen to organizer events failed.", error)
                    return@addSnapshotListener
                }
                
                val events = snapshot?.documents?.mapNotNull { doc ->
                    Event(
                        id = doc.id,
                        organizerId = doc.getString("organizerId") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        location = doc.getString("location") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                
                onEventsChanged(events)
            }
    }
    
    // ========== FUNCIONES PARA ASISTENCIAS ==========
    
    // Confirmar asistencia
    fun confirmAttendance(attendance: Attendance, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Verificar si ya existe una asistencia para este usuario y evento
        attendancesCollection
            .whereEqualTo("userId", attendance.userId)
            .whereEqualTo("eventId", attendance.eventId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // No existe, crear nueva
                    val docRef = attendancesCollection.document()
                    val attendanceWithId = attendance.copy(id = docRef.id, status = AttendanceStatus.CONFIRMED)
                    docRef.set(attendanceWithId.toMap())
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                } else {
                    // Existe, actualizar
                    val doc = snapshot.documents[0]
                    doc.reference.update("status", AttendanceStatus.CONFIRMED, "timestamp", System.currentTimeMillis())
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
    
    // Cancelar asistencia
    fun cancelAttendance(userId: String, eventId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        attendancesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    doc.reference.update("status", AttendanceStatus.CANCELLED, "timestamp", System.currentTimeMillis())
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                } else {
                    onFailure(Exception("Asistencia no encontrada"))
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
    
    // Verificar si el usuario confirmó asistencia
    fun checkUserAttendance(userId: String, eventId: String, onSuccess: (String?) -> Unit, onFailure: (Exception) -> Unit) {
        attendancesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val status = snapshot.documents[0].getString("status")
                    onSuccess(status)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
    
    // Obtener número de asistentes confirmados
    fun getConfirmedAttendeesCount(eventId: String, onSuccess: (Int) -> Unit, onFailure: (Exception) -> Unit) {
        attendancesCollection
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("status", AttendanceStatus.CONFIRMED)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.size())
            }
            .addOnFailureListener { onFailure(it) }
    }
    
    // Escuchar asistencias de un evento
    fun listenToEventAttendances(eventId: String, onAttendancesChanged: (List<Attendance>) -> Unit): ListenerRegistration {
        return attendancesCollection
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("status", AttendanceStatus.CONFIRMED)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreUtil", "Listen to attendances failed.", error)
                    return@addSnapshotListener
                }
                
                val attendances = snapshot?.documents?.mapNotNull { doc ->
                    Attendance(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        eventId = doc.getString("eventId") ?: "",
                        status = doc.getString("status") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                
                onAttendancesChanged(attendances)
            }
    }
    
    // Obtener eventos asistidos por un usuario
    fun getUserAttendedEvents(userId: String, onSuccess: (List<String>) -> Unit, onFailure: (Exception) -> Unit) {
        attendancesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", AttendanceStatus.CONFIRMED)
            .get()
            .addOnSuccessListener { snapshot ->
                val eventIds = snapshot.documents.mapNotNull { it.getString("eventId") }
                onSuccess(eventIds)
            }
            .addOnFailureListener { onFailure(it) }
    }
    
    // ========== FUNCIONES PARA COMENTARIOS ==========
    
    // Agregar comentario
    fun addComment(comment: Comment, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val docRef = commentsCollection.document()
        val commentWithId = comment.copy(id = docRef.id)
        docRef.set(commentWithId.toMap())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
    
    // Escuchar comentarios de un evento
    fun listenToEventComments(eventId: String, onCommentsChanged: (List<Comment>) -> Unit): ListenerRegistration {
        return commentsCollection
            .whereEqualTo("eventId", eventId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreUtil", "Listen to comments failed.", error)
                    return@addSnapshotListener
                }
                
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    Comment(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        eventId = doc.getString("eventId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        userPhotoUrl = doc.getString("userPhotoUrl") ?: "",
                        text = doc.getString("text") ?: "",
                        rating = doc.getLong("rating")?.toInt() ?: 0,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                
                onCommentsChanged(comments)
            }
    }
    
    // Contar comentarios de un usuario
    fun getUserCommentsCount(userId: String, onSuccess: (Int) -> Unit, onFailure: (Exception) -> Unit) {
        commentsCollection
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { onSuccess(it.size()) }
            .addOnFailureListener { onFailure(it) }
    }

    // Obtener promedio de calificaciones de un evento
    fun getEventAverageRating(eventId: String, onSuccess: (Double) -> Unit, onFailure: (Exception) -> Unit) {
        commentsCollection
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onSuccess(0.0)
                } else {
                    val ratings = snapshot.documents.mapNotNull { 
                        it.getLong("rating")?.toDouble() 
                    }
                    val average = if (ratings.isNotEmpty()) ratings.average() else 0.0
                    onSuccess(average)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
}