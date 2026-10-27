package com.studentlauncher.data

/**
 * Choices offered by Settings: up to three whole rows for the current column count, plus
 * [current] when it is not one of them.
 *
 * Columns and the app limit are independent settings. Changing the column count used to rewrite
 * the limit to the next whole row, which looked like a second setting resetting itself. Keeping
 * [current] in the list means the selected value survives a column change and the user can still
 * pick a whole row afterwards.
 */
fun homeLimitOptions(columns: Int, current: Int = 0): List<Int> {
    val c = columns.coerceIn(3, 5)
    return ((1..3).map { it * c } + current)
        .filter { it in 1..16 }
        .distinct()
        .sorted()
}
