package com.example.tricount

import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class GroupeActivity : AppCompatActivity() { //  crée l’écran qui affiche un groupe et ses dépenses

    private var groupIndex = -1
    private lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) { //  Fonction appelée quand l’activité démarre
        super.onCreate(savedInstanceState) // Appelle la version parent (obliger)
        setContentView(R.layout.activity_groupe)

        // Bouton retour
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
        group = DataHolder.groups[groupIndex] // récupère le groupe correspondant dans la liste globale

        val title = findViewById<TextView>(R.id.groupTitle)
        val listView = findViewById<ListView>(R.id.expensesListView)
        val btnAdd = findViewById<Button>(R.id.btnAddExpense)
        val summaryText = findViewById<TextView>(R.id.summaryText)

        title.text = group.name

        updateExpensesList(listView, summaryText)

        btnAdd.setOnClickListener { // bouton ajouter une dépense
            showAddExpenseDialog(listView, summaryText)
        }
    }

    private fun updateExpensesList(listView: ListView, summaryText: TextView) {
        val expensesCopy = group.expenses.toMutableList()

        val adapter = ExpenseAdapter( // crée l’adapter qui affichera chaque dépense
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
            participants = group.participants //passe la liste des participants à l’adapter
        )


        listView.adapter = adapter // affiche l’adapter dans la liste


        // Calcul des soldes
        val perPerson = DoubleArray(group.participants.size) { 0.0 } // Tableau des soldes de chaque personne
        for (e in group.expenses) {
            val share = e.amount / group.participants.size
            for (i in perPerson.indices) perPerson[i] += share
            perPerson[e.payerIndex] -= e.amount
        }

        val sb = StringBuilder()
        for (i in group.participants.indices) {
            sb.append("${group.participants[i]} : ${"%.2f".format(perPerson[i])}€\n")
        }

        val transfers = calculateTransfers(perPerson) // calcule qui doit payer qui
        val fullSummary = sb.toString() + "\n" + transfers.joinToString("\n") //  affiches le résumé complet
        summaryText.text = fullSummary
    }

    private fun calculateTransfers(perPerson: DoubleArray): List<String> {
        val result = mutableListOf<String>()
        val names = group.participants.toList()

        val creditors = mutableListOf<Pair<Int, Double>>() // ceux qui doivent recevoir
        val debtors = mutableListOf<Pair<Int, Double>>() // ceux qui doivent de l’argent

        for (i in perPerson.indices) {
            val balance = perPerson[i]
            if (balance > 0.01) creditors.add(i to balance)
            else if (balance < -0.01) debtors.add(i to -balance)
        }

        var c = 0
        var d = 0
        while (c < creditors.size && d < debtors.size) { // Tant qu’il reste des gens à équilibrer
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

    private fun serializeGroup(group: Group): String { // transformer un groupe en texte
        val sb = StringBuilder() //  construis une ligne de texte
        sb.append(group.name).append("|") // Nom du groupe
        sb.append(group.participants.joinToString(",")).append("|")
        sb.append(group.expenses.joinToString(";") {
            "${it.name},${it.amount},${it.payerIndex}"
        })
        return sb.toString() // renvoies la ligne complète
    }

    private fun showAddExpenseDialog(listView: ListView, summaryText: TextView) { // fenêtre pour ajouter une dépense
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

        val payerSpinner = Spinner(this) // Liste déroulante pour choisir qui a payé
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





