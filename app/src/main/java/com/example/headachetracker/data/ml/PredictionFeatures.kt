package com.example.headachetracker.data.ml

data class PredictionFeatures(
    /** Weather on the target day (D) — from forecast or archive */
    val dayTempMax: Double,
    val dayTempMin: Double,
    val dayPressure: Double,
    val dayRain: Double,
    /** Pressure change from D-1 to D */
    val pressureDelta: Double,
    /** Day D-1 pressure */
    val prevDayPressure: Double,
    /** Sleep that ended the morning of D (night D-1→D), in hours */
    val prevNightSleepHours: Double,
    /** Steps on D-1 (in thousands) */
    val prevDayStepsThousands: Double,
    /** Exercise minutes on D-1 */
    val prevDayExerciseMinutes: Double,
    /** Cyclic day-of-week encoding for day D */
    val dayOfWeekSin: Double,
    val dayOfWeekCos: Double,
    /** Cyclic month encoding for day D */
    val monthSin: Double,
    val monthCos: Double
) {
    fun toDoubleArray(): DoubleArray = doubleArrayOf(
        dayTempMax,
        dayTempMin,
        dayPressure,
        dayRain,
        pressureDelta,
        prevDayPressure,
        prevNightSleepHours,
        prevDayStepsThousands,
        prevDayExerciseMinutes,
        dayOfWeekSin,
        dayOfWeekCos,
        monthSin,
        monthCos
    )

    companion object {
        const val FEATURE_COUNT = 13
    }
}

data class TrainingSample(
    val features: PredictionFeatures,
    val label: Int  // 1 = headache occurred, 0 = no headache
)
