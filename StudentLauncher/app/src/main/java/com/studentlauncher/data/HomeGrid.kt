package com.studentlauncher.data

/**
 * Home app limit is always a whole number of grid rows. Changing columns keeps the current
 * number of rows (rounded up) instead of leaving a ragged last row.
 */
fun snapHomeLimit(limit: Int, columns: Int): Int {
    val c = columns.coerceIn(3, 5)
    val rows = ((limit.coerceAtLeast(1) + c - 1) / c).coerceIn(1, 3)
    return (rows * c).coerceIn(1, 16)
}

/** Choices offered by Settings: up to three rows of the current column count. */
fun homeLimitOptions(columns: Int): List<Int> {
    val c = columns.coerceIn(3, 5)
    return (1..3).map { it * c }.filter { it <= 16 }
}
