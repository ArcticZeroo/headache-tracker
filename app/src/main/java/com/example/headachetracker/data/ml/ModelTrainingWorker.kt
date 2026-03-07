package com.example.headachetracker.data.ml

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ModelTrainingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val predictionEngine: PredictionEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            predictionEngine.trainAndStore()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "model_training"
    }
}
