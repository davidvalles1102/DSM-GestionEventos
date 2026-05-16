// kotlin
package com.foro_2

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import android.util.Log

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    
    // Activity Result Launcher para Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginActivity", "Result code: ${result.resultCode}")
        Log.d("LoginActivity", "Result data: ${result.data != null}")
        
        // Siempre intentar procesar el intent, incluso si el resultado es CANCELED
        // porque los errores de configuración pueden aparecer como CANCELED
        if (result.data == null) {
            // Solo mostrar cancelado si realmente no hay datos Y el código es CANCELED
            if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show()
                Log.d("LoginActivity", "Usuario canceló el inicio de sesión (sin datos)")
            } else {
                Toast.makeText(this, "Error: No se recibieron datos de Google", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "result.data es null pero result code es ${result.resultCode}")
            }
            return@registerForActivityResult
        }
        
        // Intentar procesar el resultado incluso si el código es CANCELED
        // porque puede contener información de error útil
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        
        // Verificar si la tarea fue exitosa antes de intentar obtener el resultado
        if (task.isSuccessful) {
            try {
                val account = task.result
                val idToken = account?.idToken

                if (idToken != null) {
                    Log.d("LoginActivity", "ID Token recibido correctamente")
                    firebaseAuthWithGoogle(idToken)
                } else {
                    Toast.makeText(this, "Error: token de Google es nulo", Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "idToken nulo en Google Sign-In. Account: ${account?.email}")
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al procesar cuenta de Google: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "Error al procesar cuenta", e)
            }
        } else {
            // La tarea falló, obtener el error específico
            try {
                val exception = task.exception as? ApiException
                if (exception != null) {
                    val errorMessage = when (exception.statusCode) {
                        12501 -> "Inicio de sesión cancelado por el usuario"
                        10 -> {
                            val detailedMsg = "Error de configuración (10). Verifica:\n1. SHA-1 fingerprint agregado en Firebase\n2. Web Client ID correcto en strings.xml\n3. Google Sign-In habilitado en Firebase"
                            Log.e("LoginActivity", detailedMsg, exception)
                            detailedMsg
                        }
                        7 -> "Error de conexión. Verifica tu conexión a internet"
                        8 -> "Error interno de Google. Intenta de nuevo"
                        12500 -> "Error al iniciar sesión. Intenta de nuevo"
                        else -> {
                            val msg = "Google Sign-In falló (código ${exception.statusCode}): ${exception.message}"
                            Log.e("LoginActivity", msg, exception)
                            msg
                        }
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    Log.e("LoginActivity", "Google Sign-In error completo", exception)
                    Log.e("LoginActivity", "Status code: ${exception.statusCode}, Message: ${exception.message}")
                } else {
                    val errorMsg = "Error desconocido: ${task.exception?.message ?: "Sin detalles"}"
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "Error en Google Sign-In (no ApiException)", task.exception)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "Error inesperado al procesar error de Google Sign-In", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configurar barras del sistema después de setContentView
        SystemUIHelper.setupSystemBars(this)

        // Inicializa Firebase explícitamente
        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()

        binding.notUserYet.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Configuración para el inicio de sesión con Google
        try {
            val webClientId = getString(R.string.default_web_client_id).trim()
            Log.d("LoginActivity", "Web Client ID configurado: ${webClientId.take(20)}...")
            
            if (webClientId.isNotEmpty() && webClientId.contains(".apps.googleusercontent.com")) {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                googleSignInClient = GoogleSignIn.getClient(this, gso)
                Log.d("LoginActivity", "Google Sign-In configurado correctamente")
            } else {
                Log.e("LoginActivity", "default_web_client_id inválido o vacío en strings.xml")
                Log.e("LoginActivity", "Asegúrate de usar el Web Client ID de Firebase Console")
                // No mostrar toast aquí para no molestar al usuario si no usa Google Sign-In
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error al configurar Google Sign-In", e)
            // No mostrar toast aquí para no molestar al usuario si no usa Google Sign-In
        }

        // Referencias a los elementos del layout usando binding
        val emailEditText = binding.emailEditText
        val passwordEditText = binding.passwordEditText

        // Acción del botón de login tradicional
        binding.loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "La contraseña no puede estar vacía", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.loginButton.isEnabled = false
            binding.progressBar.visibility = android.view.View.VISIBLE
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                binding.loginButton.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "Inicio de sesión exitoso")
                    val intent = Intent(this, EventsListActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("LoginActivity", "Error en login", task.exception)
                    Toast.makeText(this, task.exception?.localizedMessage ?: "Error desconocido", Toast.LENGTH_SHORT).show()
                }
            }

        }

        // Acción del botón de Google Sign-In (ImageButton)
        binding.googleSignInButton.setOnClickListener {
            if (::googleSignInClient.isInitialized) {
                Log.d("LoginActivity", "Iniciando Google Sign-In...")
                try {
                    // Intentar obtener el intent directamente sin hacer signOut primero
                    // Esto evita problemas si signOut falla
                    val signInIntent = googleSignInClient.signInIntent
                    if (signInIntent != null) {
                        Log.d("LoginActivity", "Lanzando intent de Google Sign-In")
                        googleSignInLauncher.launch(signInIntent)
                    } else {
                        Toast.makeText(this, "Error: No se pudo crear el intent de Google Sign-In", Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "signInIntent es null")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al iniciar Google Sign-In: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "Error al lanzar Google Sign-In", e)
                }
            } else {
                val errorMsg = "Google Sign-In no está configurado. Verifica:\n1. default_web_client_id en strings.xml\n2. Web Client ID de Firebase Console"
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                Log.e("LoginActivity", errorMsg)
            }
        }

        // Acción del botón de Facebook Sign-In
        binding.facebookSignInButton.setOnClickListener {
            Toast.makeText(this, "Inicio de sesión con Facebook próximamente", Toast.LENGTH_SHORT).show()
            // Aquí se puede implementar la autenticación con Facebook cuando esté configurada
        }

        // Toggle para mostrar/ocultar contraseña
        var isPasswordVisible = false
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            
            if (isPasswordVisible) {
                // Mostrar contraseña
                binding.passwordEditText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            } else {
                // Ocultar contraseña
                binding.passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }
            
            // Mover el cursor al final del texto
            binding.passwordEditText.setSelection(binding.passwordEditText.text.length)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        // Crear o actualizar documento de usuario en Firestore
                        FirestoreUtil.createUserDocument(
                            userId = user.uid,
                            email = user.email ?: "",
                            role = Roles.USUARIO,
                            onSuccess = {
                                Log.d("LoginActivity", "Usuario creado/actualizado en Firestore")
                                goToHome()
                            },
                            onFailure = { e ->
                                Log.e("LoginActivity", "Error al crear documento de usuario", e)
                                // Continuar de todas formas, el usuario ya está autenticado
                                goToHome()
                            }
                        )
                    } else {
                        goToHome()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(this, "Error con Firebase: $errorMessage", Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "Firebase signInWithCredential failed", task.exception)
                }
            }
    }

    private fun goToHome() {
        val intent = Intent(this, EventsListActivity::class.java)
        startActivity(intent)
        finish()
    }
}
