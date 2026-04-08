package com.example.tricount

data class Expense(var name: String, var amount: Double, var payerIndex: Int)
data class Group(var name: String, val participants: MutableList<String>, val expenses: MutableList<Expense> = mutableListOf())

object DataHolder {
    // stocke les groupes en mémoire pendant que l'app tourne
    val groups = mutableListOf<Group>()
}
