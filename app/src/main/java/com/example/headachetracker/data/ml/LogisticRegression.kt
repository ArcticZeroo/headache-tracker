package com.example.headachetracker.data.ml

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * On-device logistic regression via gradient descent with L2 regularization.
 * All features are expected to be z-score normalized before calling [train] or [predict].
 */
object LogisticRegression {

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z))

    /**
     * Trains the model on the given samples.
     *
     * @param features n_samples × n_features matrix (already normalized)
     * @param labels   binary labels (0 or 1) for each sample
     * @return [ModelWeights] containing weights, bias, and final training loss
     */
    fun train(
        features: List<DoubleArray>,
        labels: List<Int>,
        learningRate: Double = 0.1,
        epochs: Int = 1000,
        lambda: Double = 0.01
    ): ModelWeights {
        require(features.size == labels.size) { "Feature/label size mismatch" }
        require(features.isNotEmpty()) { "Training set must not be empty" }

        val n = features.size
        val m = features[0].size
        val weights = DoubleArray(m) { 0.0 }
        var bias = 0.0

        repeat(epochs) {
            val gradW = DoubleArray(m) { 0.0 }
            var gradB = 0.0

            for (i in 0 until n) {
                val z = dotProduct(weights, features[i]) + bias
                val prediction = sigmoid(z)
                val error = prediction - labels[i]
                for (j in 0 until m) {
                    gradW[j] += error * features[i][j]
                }
                gradB += error
            }

            for (j in 0 until m) {
                // Gradient with L2 regularization (skip bias from regularization)
                weights[j] -= learningRate * (gradW[j] / n + 2.0 * lambda * weights[j])
            }
            bias -= learningRate * (gradB / n)
        }

        // Compute final loss for diagnostics
        var loss = 0.0
        for (i in 0 until n) {
            val z = dotProduct(weights, features[i]) + bias
            val p = sigmoid(z).coerceIn(1e-9, 1.0 - 1e-9)
            loss += -(labels[i] * Math.log(p) + (1 - labels[i]) * Math.log(1.0 - p))
        }
        loss /= n

        return ModelWeights(weights, bias, loss)
    }

    /**
     * Predicts the probability of a headache (1) for a single sample.
     *
     * @param features normalized feature vector (same normalization as training)
     */
    fun predict(features: DoubleArray, weights: DoubleArray, bias: Double): Double {
        return sigmoid(dotProduct(weights, features) + bias)
    }

    // --- Normalization helpers ---

    fun computeMeans(features: List<DoubleArray>): DoubleArray {
        if (features.isEmpty()) return DoubleArray(0)
        val m = features[0].size
        return DoubleArray(m) { j ->
            features.sumOf { it[j] } / features.size
        }
    }

    fun computeStds(features: List<DoubleArray>, means: DoubleArray): DoubleArray {
        if (features.isEmpty()) return DoubleArray(0)
        val m = features[0].size
        return DoubleArray(m) { j ->
            val variance = features.sumOf { x ->
                val diff = x[j] - means[j]
                diff * diff
            } / features.size
            sqrt(variance).let { if (it < 1e-9) 1.0 else it }
        }
    }

    fun normalize(features: DoubleArray, means: DoubleArray, stds: DoubleArray): DoubleArray {
        return DoubleArray(features.size) { j ->
            (features[j] - means[j]) / stds[j]
        }
    }

    private fun dotProduct(a: DoubleArray, b: DoubleArray): Double {
        var sum = 0.0
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }
}

data class ModelWeights(
    val weights: DoubleArray,
    val bias: Double,
    val trainingLoss: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelWeights) return false
        return weights.contentEquals(other.weights) && bias == other.bias
    }

    override fun hashCode(): Int = 31 * weights.contentHashCode() + bias.hashCode()
}
