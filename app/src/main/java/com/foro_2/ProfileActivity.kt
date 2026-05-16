package com.foro_2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.foro_2.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUIHelper.setupSystemBars(this)

        firebaseAuth = FirebaseAuth.getInstance()

        setupUI()
        loadUserData()
        loadStatistics()
    }

    private fun setupUI() {
        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(this, "Edición de perfil próximamente", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadUserData() {
        val user = firebaseAuth.currentUser ?: return

        binding.textViewName.text = user.displayName ?: user.email?.split("@")?.get(0) ?: "Usuario"
        binding.textViewEmail.text = user.email ?: ""

        if (user.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .into(binding.imageViewProfile)
        }

        FirestoreUtil.getUserRole(user.uid,
            onSuccess = { role ->
                binding.textViewAccountType.text = when (role) {
                    Roles.ORGANIZADOR -> "Organizador"
                    else -> "Usuario"
                }
            },
            onFailure = {
                binding.textViewAccountType.text = "Usuario"
            }
        )
    }

    private fun loadStatistics() {
        val user = firebaseAuth.currentUser ?: return

        FirestoreUtil.getUserAttendedEvents(user.uid,
            onSuccess = { eventIds ->
                binding.textViewEventsAttended.text = "Eventos asistidos: ${eventIds.size}"
            },
            onFailure = {
                binding.textViewEventsAttended.text = "Eventos asistidos: 0"
            }
        )

        FirestoreUtil.getUserCommentsCount(user.uid,
            onSuccess = { count ->
                binding.textViewCommentsCount.text = "Comentarios realizados: $count"
            },
            onFailure = {
                binding.textViewCommentsCount.text = "Comentarios realizados: 0"
            }
        )
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                firebaseAuth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
