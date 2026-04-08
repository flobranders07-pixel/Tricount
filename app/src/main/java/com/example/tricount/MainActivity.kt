package com.example.tricount

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: GroupAdapter
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnCreateGroup = findViewById<Button>(R.id.btnCreateGroup)
        emptyText = findViewById(R.id.emptyText)
        listView = findViewById(R.id.groupsListView)

        // 🔄 Charger les groupes sauvegardés depuis SharedPreferences
        val sharedPref = getSharedPreferences("tricount_data", MODE_PRIVATE)
        for (i in 0..50) {
            val data = sharedPref.getString("group_${i}_data", null)
            if (data != null) {
                val group = deserializeGroup(data)
                if (group != null) {
                    DataHolder.groups.add(group)
                }
            }
        }

        // ✅ Adapter avec suppression
        adapter = GroupAdapter(this, DataHolder.groups) { position ->
            DataHolder.groups.removeAt(position)
            saveGroups()
            refreshList()
        }
        listView.adapter = adapter

        btnCreateGroup.setOnClickListener {
            showCreateGroupDialog()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, GroupeActivity::class.java)
            intent.putExtra("groupIndex", position)
            startActivity(intent)
        }

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        adapter.notifyDataSetChanged()
        emptyText.visibility = if (DataHolder.groups.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showCreateGroupDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Créer un groupe")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 10)

        val inputName = EditText(this)
        inputName.hint = "Nom du groupe"
        layout.addView(inputName)

        val inputParticipants = EditText(this)
        inputParticipants.hint = "Participants (séparés par des virgules)"
        layout.addView(inputParticipants)

        builder.setView(layout)

        builder.setPositiveButton("Créer") { _, _ ->
            val name = inputName.text.toString().trim()
            val participantsText = inputParticipants.text.toString()
            val participants = participantsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()

            if (name.isEmpty() || participants.isEmpty()) {
                Toast.makeText(this, "Donne un nom et au moins 1 participant", Toast.LENGTH_SHORT).show()
            } else {
                DataHolder.groups.add(Group(name, participants))
                saveGroups()
                refreshList()
            }
        }
        builder.setNegativeButton("Annuler", null)
        builder.show()
    }

    private fun saveGroups() {
        val sharedPref = getSharedPreferences("tricount_data", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.clear()

        for ((index, group) in DataHolder.groups.withIndex()) {
            val name = group.name
            val participants = group.participants.joinToString(",")
            val expenses = group.expenses.joinToString(";") {
                "${it.name},${it.amount},${it.payerIndex}"
            }
            val data = "$name|$participants|$expenses"
            editor.putString("group_${index}_data", data)
        }

        editor.apply()
    }

    private fun deserializeGroup(data: String): Group? {
        val parts = data.split("|")
        if (parts.size != 3) return null

        val name = parts[0]
        val participants = parts[1].split(",")
        val expenses = parts[2].split(";").mapNotNull {
            val eParts = it.split(",")
            if (eParts.size != 3) return@mapNotNull null
            val eName = eParts[0]
            val eAmount = eParts[1].toDoubleOrNull() ?: return@mapNotNull null
            val ePayer = eParts[2].toIntOrNull() ?: return@mapNotNull null
            Expense(eName, eAmount, ePayer)
        }

        return Group(name, participants.toMutableList(), expenses.toMutableList())
    }
}


