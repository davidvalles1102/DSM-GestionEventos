package com.foro_2

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configurar barras del sistema después de setContentView
        SystemUIHelper.setupSystemBars(this)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.alreadyUser.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Toggle para mostrar/ocultar contraseña
        var isPasswordVisible = false
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            
            if (isPasswordVisible) {
                // Mostrar contraseña
                binding.newPass.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            } else {
                // Ocultar contraseña
                binding.newPass.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }
            
            // Mover el cursor al final del texto
            binding.newPass.setSelection(binding.newPass.text.length)
        }

        // Toggle para mostrar/ocultar confirmar contraseña
        var isConfirmPasswordVisible = false
        binding.btnToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            
            if (isConfirmPasswordVisible) {
                // Mostrar contraseña
                binding.etConfirmPassword.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility)
            } else {
                // Ocultar contraseña
                binding.etConfirmPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off)
            }
            
            // Mover el cursor al final del texto
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text.length)
        }

        binding.newRegisterButton.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.newEmail.text.toString()
            val pass = binding.newPass.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()
            
            // Obtener rol seleccionado
            val role = when (binding.radioGroupRole.checkedRadioButtonId) {
                R.id.radioUsuario -> "usuario"
                R.id.radioOrganizador -> "organizador"
                else -> "usuario"
            }

            // Validaciones
            if (name.isEmpty()) {
                Toast.makeText(this, "Ingresa tu nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "¡Campos vacíos no están permitidos!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (pass != confirmPass) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (pass.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.newRegisterButton.isEnabled = false
            binding.progressBar.visibility = android.view.View.VISIBLE
            firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                if (it.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        FirestoreUtil.createUserDocument(
                            userId = user.uid,
                            email = email,
                            role = role,
                            onSuccess = {
                                binding.newRegisterButton.isEnabled = true
                                binding.progressBar.visibility = android.view.View.GONE
                                android.app.AlertDialog.Builder(this)
                                    .setTitle("Registro exitoso")
                                    .setMessage("Tu cuenta ha sido creada correctamente como $role.")
                                    .setPositiveButton("Aceptar") { dialog, _ ->
                                        dialog.dismiss()
                                        startActivity(Intent(this, EventsListActivity::class.java))
                                        finish()
                                    }
                                    .show()
                            },
                            onFailure = { e ->
                                binding.newRegisterButton.isEnabled = true
                                binding.progressBar.visibility = android.view.View.GONE
                                Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } else {
                    binding.newRegisterButton.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, it.exception?.localizedMessage ?: "Error desconocido", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
