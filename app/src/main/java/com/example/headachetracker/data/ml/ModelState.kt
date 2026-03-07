package com.example.headachetracker.data.ml

data class ModelState(
    val weights: List<Double>,
    val bias: Double,
    val featureMeans: List<Double>,
    val featureStds: List<Double>,
    val trainingSampleCount: Int,
    val headacheDayCount: Int,
    val trainedAtMillis: Long,
    val modelVersion: Int = 1
)
