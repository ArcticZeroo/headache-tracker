package com.example.headachetracker.data.ml

import com.example.headachetracker.data.local.HeadacheDao
import javax.inject.Inject
import javax.inject.Singleton

const val MIN_TRAINING_ENTRIES = 30

@Singleton
class PredictionEngine @Inject constructor(
    private val headacheDao: HeadacheDao,
    private val featureExtractor: FeatureExtractor,
    private val modelStorage: ModelStorage
) {
    /**
     * Builds training data, trains the model, and persists the result.
     * Returns the trained [ModelState], or null if there is not enough data.
     */
    suspend fun trainAndStore(): ModelState? {
        val entryCount = headacheDao.getEntryCount()
        if (entryCount < MIN_TRAINING_ENTRIES) return null

        val samples = featureExtractor.buildTrainingSamples()
        if (samples.size < MIN_TRAINING_ENTRIES) return null

        val rawFeatureArrays = samples.map { it.features.toDoubleArray() }
        val labels = samples.map { it.label }

        // Replace NaN values with column means before computing normalization stats
        val means = computeMeansWithNanHandling(rawFeatureArrays)
        val imputedArrays = rawFeatureArrays.map { impute(it, means) }

        val stds = LogisticRegression.computeStds(imputedArrays, means)
        val normalizedArrays = imputedArrays.map { LogisticRegression.normalize(it, means, stds) }

        val modelWeights = LogisticRegression.train(normalizedArrays, labels)

        val headacheDays = samples.count { it.label == 1 }
        val state = ModelState(
            weights = modelWeights.weights.toList(),
            bias = modelWeights.bias,
            featureMeans = means.toList(),
            featureStds = stds.toList(),
            trainingSampleCount = samples.size,
            headacheDayCount = headacheDays,
            trainedAtMillis = System.currentTimeMillis()
        )
        modelStorage.save(state)
        return state
    }

    /**
     * Runs inference for a given [features] vector using the stored model.
     * Returns null if no trained model exists.
     */
    fun predict(features: PredictionFeatures): Double? {
        val state = modelStorage.load() ?: return null
        val raw = features.toDoubleArray()
        val means = state.featureMeans.toDoubleArray()
        val stds = state.featureStds.toDoubleArray()
        val imputed = impute(raw, means)
        val normalized = LogisticRegression.normalize(imputed, means, stds)
        return LogisticRegression.predict(normalized, state.weights.toDoubleArray(), state.bias)
    }

    fun loadModel(): ModelState? = modelStorage.load()

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun computeMeansWithNanHandling(arrays: List<DoubleArray>): DoubleArray {
        if (arrays.isEmpty()) return DoubleArray(0)
        val m = arrays[0].size
        return DoubleArray(m) { j ->
            val values = arrays.mapNotNull { row -> row[j].takeUnless { it.isNaN() } }
            if (values.isEmpty()) 0.0 else values.average()
        }
    }

    private fun impute(features: DoubleArray, means: DoubleArray): DoubleArray =
        DoubleArray(features.size) { i ->
            if (features[i].isNaN()) means[i] else features[i]
        }
}
