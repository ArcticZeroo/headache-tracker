package com.example.headachetracker.data.correlation

import kotlin.math.sqrt

data class CorrelationResult(
    val factorName: String,
    val coefficient: Double,
    val sampleSize: Int,
    val interpretation: String
)

object CorrelationEngine {

    const val MIN_SAMPLE_SIZE = 10

    /**
     * Computes Pearson correlation coefficient between two equal-length lists.
     * Returns null if there are fewer than [MIN_SAMPLE_SIZE] data points
     * or if either variable has zero variance.
     */
    fun pearson(x: List<Double>, y: List<Double>): Double? {
        if (x.size != y.size || x.size < MIN_SAMPLE_SIZE) return null

        val n = x.size
        val meanX = x.average()
        val meanY = y.average()

        var numerator = 0.0
        var denomX = 0.0
        var denomY = 0.0

        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            numerator += dx * dy
            denomX += dx * dx
            denomY += dy * dy
        }

        if (denomX == 0.0 || denomY == 0.0) return null
        return numerator / (sqrt(denomX) * sqrt(denomY))
    }

    fun interpret(factorName: String, coefficient: Double, sampleSize: Int): CorrelationResult {
        val strength = kotlin.math.abs(coefficient)
        val direction = if (coefficient > 0) "positive" else "negative"

        val interpretation = when {
            strength < 0.1 -> "No meaningful correlation with $factorName"
            strength < 0.3 -> "Weak $direction correlation with $factorName"
            strength < 0.5 -> "Moderate $direction correlation with $factorName"
            strength < 0.7 -> "Strong $direction correlation with $factorName"
            else -> "Very strong $direction correlation with $factorName"
        }

        return CorrelationResult(
            factorName = factorName,
            coefficient = coefficient,
            sampleSize = sampleSize,
            interpretation = interpretation
        )
    }
}
