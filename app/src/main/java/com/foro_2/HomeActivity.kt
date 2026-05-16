package com.foro_2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityHomeBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var expensesListener: ListenerRegistration? = null
    
    // Variables para estadísticas de gastos
    private var monthlyTotal: Double = 0.0
    private var categoryTotals: Map<String, Double> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        setupUI()
    }

    private fun setupUI() {
        binding.welcomeText.text = "Hola, ${firebaseAuth.currentUser?.email ?: "Usuario"} 👋"

        binding.btnCreateExpense.setOnClickListener {
            startActivity(Intent(this, CreateExpenseActivity::class.java))
        }

        binding.btnViewExpenses.setOnClickListener {
            startActivity(Intent(this, ViewExpensesActivity::class.java))
        }

        binding.btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        
        binding.signOutButton.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadExpensesData()
    }

    override fun onPause() {
        super.onPause()
        expensesListener?.remove()
    }

    private fun loadExpensesData() {
        val user = firebaseAuth.currentUser ?: return

        expensesListener = FirestoreUtil.listenToUserExpenses(user.uid) { expenses ->
            // Calcular estadísticas
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentYear = calendar.get(Calendar.YEAR)

            // Filtrar gastos del mes actual
            val monthlyExpenses = expenses.filter { expense ->
                val parts = expense.date.split("/")
                if (parts.size == 3) {
                    val month = parts[1].toIntOrNull() ?: 0
                    val year = parts[2].toIntOrNull() ?: 0
                    month == currentMonth && year == currentYear
                } else false
            }

            // Calcular total mensual
            monthlyTotal = monthlyExpenses.sumOf { it.amount }
            binding.tvMonthlyTotal.text = String.format("Total Mensual: $%.2f", monthlyTotal)

            // Calcular totales por categoría
            categoryTotals = monthlyExpenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            // Actualizar gráfico
            updateChart(categoryTotals)

            // Mostrar número de gastos del mes
            binding.tvExpenseCount.text = "Gastos este mes: ${monthlyExpenses.size}"
        }
    }

    private fun updateChart(categoryTotals: Map<String, Double>) {
        if (categoryTotals.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        val entries = categoryTotals.map { (category, total) ->
            PieEntry(total.toFloat(), category)
        }

        val colors = listOf(
            getColor(R.color.chart_pink),
            getColor(R.color.chart_light_blue),
            getColor(R.color.chart_yellow),
            getColor(R.color.chart_teal),
            getColor(R.color.chart_purple),
            getColor(R.color.chart_orange),
            getColor(R.color.chart_pink),
            getColor(R.color.chart_gray),
            getColor(R.color.chart_teal)
        )

        val dataSet = PieDataSet(entries, "Gastos por Categoría").apply {
            this.colors = colors
            sliceSpace = 3f
            valueTextSize = 14f
            valueTextColor = Color.WHITE
        }

        binding.pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isRotationEnabled = true
            centerText = "Gastos por\nCategoría"
            setCenterTextSize(16f)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(10f)
            animateY(1000)
            invalidate()
        }
    }
}