package com.foro_2

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.foro_2.databinding.ActivityCommentsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class CommentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommentsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var eventId: String? = null
    private var commentsListener: ListenerRegistration? = null
    private lateinit var commentsAdapter: CommentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUIHelper.setupSystemBars(this)

        firebaseAuth = FirebaseAuth.getInstance()
        eventId = intent.getStringExtra("eventId")

        if (eventId == null) {
            Toast.makeText(this, "Error: Evento no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        setupAddCommentButton()
        loadComments()
    }

    private fun setupRecyclerView() {
        commentsAdapter = CommentsAdapter(emptyList())
        binding.recyclerViewComments.apply {
            layoutManager = LinearLayoutManager(this@CommentsActivity)
            adapter = commentsAdapter
        }
    }

    private fun setupAddCommentButton() {
        binding.btnAddComment.setOnClickListener {
            showAddCommentDialog()
        }
    }

    private fun showAddCommentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_comment, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val etComment = dialogView.findViewById<EditText>(R.id.etComment)
        val btnSend = dialogView.findViewById<Button>(R.id.btnSend)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Agregar comentario")
            .setView(dialogView)
            .create()

        btnSend.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val text = etComment.text.toString().trim()

            if (text.isEmpty()) {
                Toast.makeText(this, "Ingresa un comentario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rating == 0) {
                Toast.makeText(this, "Selecciona una calificación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = firebaseAuth.currentUser ?: run {
                Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            val comment = Comment(
                userId = user.uid,
                eventId = eventId!!,
                userName = user.displayName ?: user.email?.split("@")?.get(0) ?: "Usuario",
                userPhotoUrl = user.photoUrl?.toString() ?: "",
                text = text,
                rating = rating
            )

            FirestoreUtil.addComment(comment,
                onSuccess = {
                    Toast.makeText(this, "Comentario agregado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                },
                onFailure = {
                    btnSend.isEnabled = true
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun loadComments() {
        commentsListener = FirestoreUtil.listenToEventComments(eventId!!) { comments ->
            commentsAdapter.updateComments(comments)
        }
    }

    override fun onPause() {
        super.onPause()
        commentsListener?.remove()
        commentsListener = null
    }
}
