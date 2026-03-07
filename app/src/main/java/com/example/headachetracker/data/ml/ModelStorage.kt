package com.example.headachetracker.data.ml

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "headache_prediction_model"
private const val KEY_MODEL_JSON = "model_state_json"

@Singleton
class ModelStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val adapter by lazy {
        moshi.adapter(ModelState::class.java)
    }

    fun save(state: ModelState) {
        val json = adapter.toJson(state)
        prefs.edit().putString(KEY_MODEL_JSON, json).apply()
    }

    fun load(): ModelState? {
        val json = prefs.getString(KEY_MODEL_JSON, null) ?: return null
        return try {
            adapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_MODEL_JSON).apply()
    }
}
