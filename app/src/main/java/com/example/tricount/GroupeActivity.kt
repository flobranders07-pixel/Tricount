package com.example.tricount

import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class GroupeActivity : AppCompatActivity() {

    private var groupIndex = -1
    private lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groupe)

        // Bouton retour personnalisé (pas de barre mauve)
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // Récupération du groupe
        groupIndex = intent.getIntExtra("groupIndex", -1)
        if (groupIndex == -1) {
            finish()
            return
        }
        group = DataHolder.groups[groupIndex]

        val title = findViewById<TextView>(R.id.groupTitle)
        val listView = findViewById<ListView>(R.id.expensesListView)
        val btnAdd = findViewById<Button>(R.id.btnAddExpense)
        val summaryText = findViewById<TextView>(R.id.summaryText)

        title.text = group.name

        updateExpensesList(listView, summaryText)

        btnAdd.setOnClickListener {
            showAddExpenseDialog(listView, summaryText)
        }
    }

    private fun updateExpensesList(listView: ListView, summaryText: TextView) {
        val expensesCopy = group.expenses.toMutableList()

        val adapter = ExpenseAdapter(
            this,
            expensesCopy,
            onDelete = { expenseToDelete ->
                group.expenses.remove(expenseToDelete)
                updateExpensesList(listView, summaryText)

                Toast.makeText(this, "Dépense supprimée", Toast.LENGTH_SHORT).show()

                val sharedPref = getSharedPreferences("tricount_data", MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("group_${groupIndex}_data", serializeGroup(group))
                editor.apply()
            },
            onUpdate = {
                updateExpensesList(listView, summaryText)

                val sharedPref = getSharedPreferences("tricount_data", MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("group_${groupIndex}_data", serializeGroup(group))
                editor.apply()
            },
            participants = group.participants
        )


        listView.adapter = adapter

        val perPerson = DoubleArray(group.participants.size) { 0.0 }
        for (e in group.expenses) {
            val share = e.amount / group.participants.size
            for (i in perPerson.indices) perPerson[i] += share
            perPerson[e.payerIndex] -= e.amount
        }

        val sb = StringBuilder()
        for (i in group.participants.indices) {
            sb.append("${group.participants[i]} : ${"%.2f".format(perPerson[i])}€\n")
        }

        val transfers = calculateTransfers(perPerson)
        val fullSummary = sb.toString() + "\n" + transfers.joinToString("\n")
        summaryText.text = fullSummary
    }

    private fun calculateTransfers(perPerson: DoubleArray): List<String> {
        val result = mutableListOf<String>()
        val names = group.participants.toList()

        val creditors = mutableListOf<Pair<Int, Double>>()
        val debtors = mutableListOf<Pair<Int, Double>>()

        for (i in perPerson.indices) {
            val balance = perPerson[i]
            if (balance > 0.01) creditors.add(i to balance)
            else if (balance < -0.01) debtors.add(i to -balance)
        }

        var c = 0
        var d = 0
        while (c < creditors.size && d < debtors.size) {
            val (ci, cAmount) = creditors[c]
            val (di, dAmount) = debtors[d]

            val transfer = minOf(cAmount, dAmount)
            result.add("${names[ci]} doit ${"%.2f".format(transfer)}€ à ${names[di]}")

            creditors[c] = ci to (cAmount - transfer)
            debtors[d] = di to (dAmount - transfer)

            if (creditors[c].second < 0.01) c++
            if (debtors[d].second < 0.01) d++
        }

        return result
    }

    private fun serializeGroup(group: Group): String {
        val sb = StringBuilder()
        sb.append(group.name).append("|")
        sb.append(group.participants.joinToString(",")).append("|")
        sb.append(group.expenses.joinToString(";") {
            "${it.name},${it.amount},${it.payerIndex}"
        })
        return sb.toString()
    }

    private fun showAddExpenseDialog(listView: ListView, summaryText: TextView) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ajouter une dépense")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 10)

        val nameInput = EditText(this)
        nameInput.hint = "Nom de la dépense"
        layout.addView(nameInput)

        val amountInput = EditText(this)
        amountInput.hint = "Montant (€)"
        amountInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        layout.addView(amountInput)

        val payerSpinner = Spinner(this)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, group.participants)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        payerSpinner.adapter = spinnerAdapter
        layout.addView(payerSpinner)

        builder.setView(layout)

        builder.setPositiveButton("Ajouter") { _, _ ->
            val name = nameInput.text.toString().trim()
            val amountText = amountInput.text.toString().trim()
            val payerIndex = payerSpinner.selectedItemPosition

            if (name.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(this, "Remplis tous les champs", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            group.expenses.add(Expense(name, amount, payerIndex))
            updateExpensesList(listView, summaryText)

            val sharedPref = getSharedPreferences("tricount_data", MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString("group_${groupIndex}_data", serializeGroup(group))
            editor.apply()
        }

        builder.setNegativeButton("Annuler", null)
        builder.show()
    }
}





