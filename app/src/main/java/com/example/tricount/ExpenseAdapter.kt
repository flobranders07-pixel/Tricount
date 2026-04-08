package com.example.tricount

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Toast

class ExpenseAdapter(
    private val context: Context,
    private val expenses: MutableList<Expense>,
    private val onDelete: (Expense) -> Unit,
    private val onUpdate: () -> Unit,
    private val participants: List<String>   // ✔️ AJOUT
) : BaseAdapter() {

    override fun getCount(): Int = expenses.size
    override fun getItem(position: Int): Expense = expenses[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.expense_item, parent, false)

        val expense = expenses[position]

        val nameText = view.findViewById<TextView>(R.id.expenseName)
        val amountText = view.findViewById<TextView>(R.id.expenseAmount)
        val deleteButton = view.findViewById<Button>(R.id.deleteExpenseButton)
        val editButton = view.findViewById<Button>(R.id.editExpenseButton)

        nameText.text = expense.name
        amountText.text = "${expense.amount}€"

        deleteButton.setOnClickListener {
            onDelete(expense)
        }

        editButton.setOnClickListener {

            // ✔️ Création du layout du dialog
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(40, 20, 40, 10)

            // ✔️ Champ nom
            val nameInput = EditText(context)
            nameInput.setText(expense.name)
            nameInput.hint = "Nom"
            layout.addView(nameInput)

            // ✔️ Champ montant
            val amountInput = EditText(context)
            amountInput.setText(expense.amount.toString())
            amountInput.hint = "Montant"
            amountInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layout.addView(amountInput)

            // ✔️ Spinner pour choisir le payeur
            val payerSpinner = Spinner(context)
            val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, participants)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            payerSpinner.adapter = spinnerAdapter
            payerSpinner.setSelection(expense.payerIndex)
            layout.addView(payerSpinner)

            // ✔️ Création du dialog
            AlertDialog.Builder(context)
                .setTitle("Modifier la dépense")
                .setView(layout)
                .setPositiveButton("Enregistrer") { _, _ ->
                    val newName = nameInput.text.toString().trim()
                    val newAmount = amountInput.text.toString().toDoubleOrNull() ?: expense.amount
                    val newPayerIndex = payerSpinner.selectedItemPosition

                    if (newName.isNotEmpty()) {
                        expense.name = newName
                        expense.amount = newAmount
                        expense.payerIndex = newPayerIndex   // ✔️ Mise à jour du payeur

                        notifyDataSetChanged()
                        onUpdate()   // ✔️ Recalcul des soldes + sauvegarde

                        Toast.makeText(context, "Dépense modifiée", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        return view
    }
}






