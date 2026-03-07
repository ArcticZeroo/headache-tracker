package com.example.headachetracker.data.correlation

import kotlin.math.sqrt

data class CorrelationResult(
    val factorName: String,
    val coefficient: Double,
    val sampleSize: Int,
    val interpretation: String,
    val percentageDifference: Double? = null
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

    /**
     * Computes the percentage difference in average pain between days with
     * above-median vs below-median values of a factor.
     */
    fun computePercentageDiff(painValues: List<Double>, factorValues: List<Double>): Double? {
        if (painValues.size < MIN_SAMPLE_SIZE) return null
        val median = factorValues.sorted()[factorValues.size / 2]
        val highPain = painValues.filterIndexed { i, _ -> factorValues[i] >= median }
        val lowPain = painValues.filterIndexed { i, _ -> factorValues[i] < median }
        if (lowPain.isEmpty() || highPain.isEmpty()) return null
        val lowAvg = lowPain.average()
        if (lowAvg == 0.0) return null
        return ((highPain.average() - lowAvg) / lowAvg) * 100.0
    }

    fun interpret(
        factorName: String,
        coefficient: Double,
        sampleSize: Int,
        percentageDifference: Double? = null
    ): CorrelationResult {
        val strength = kotlin.math.abs(coefficient)
        val pctAbs = percentageDifference?.let { kotlin.math.abs(it).toInt() }

        val interpretation = when {
            strength < 0.1 -> "No clear link between $factorName and your pain"
            else -> {
                val qualifier = when {
                    strength < 0.3 -> "slightly"
                    strength < 0.5 -> "noticeably"
                    else -> "significantly"
                }
                val pctText = if (pctAbs != null && pctAbs > 0) " (~${pctAbs}%)" else ""
                if (coefficient > 0) {
                    "Your pain tends to be $qualifier worse on days with more $factorName$pctText"
                } else {
                    "Your pain tends to be $qualifier better on days with more $factorName$pctText"
                }
            }
        }

        return CorrelationResult(
            factorName = factorName,
            coefficient = coefficient,
            sampleSize = sampleSize,
            interpretation = interpretation,
            percentageDifference = percentageDifference
        )
    }
}
