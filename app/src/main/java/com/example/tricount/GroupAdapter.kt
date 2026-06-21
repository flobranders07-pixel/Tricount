package com.example.tricount

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class GroupAdapter( // traducteur entre la liste de groupes et l'affichage dans ListView
    private val context: Context, // l’écran où je suis
    private val groups: MutableList<Group>, // la liste des groupes
    private val onDelete: (Int) -> Unit //  une fonction à appeler quand on supprime un groupe
) : ArrayAdapter<Group>(context, 0, groups) { // Cet adapter va afficher des objets de type Group

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.group_item, parent, false)

        val groupNameText = view.findViewById<TextView>(R.id.groupNameText)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)
        val editButton = view.findViewById<Button>(R.id.editButton)

        groupNameText.text = groups[position].name

        deleteButton.setOnClickListener {
            onDelete(position)
            Toast.makeText(context, "Groupe supprimé", Toast.LENGTH_SHORT).show()
        }

        editButton.setOnClickListener {
            val editText = EditText(context)
            editText.setText(groups[position].name)

            AlertDialog.Builder(context)
                .setTitle("Modifier le groupe")
                .setView(editText)
                .setPositiveButton("Enregistrer") { _, _ ->
                    val newName = editText.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        groups[position].name = newName
                        notifyDataSetChanged()
                        Toast.makeText(context, "Nom modifié", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        return view
    }
}

