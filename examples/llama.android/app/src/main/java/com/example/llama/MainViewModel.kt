package com.example.llama

import android.llama.cpp.LLamaAndroid
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class MainViewModel(private val llamaAndroid: LLamaAndroid = LLamaAndroid.instance()): ViewModel() {
    companion object {
        @JvmStatic
        private val NanosPerSecond = 1_000_000_000.0
    }

    private val tag: String? = this::class.simpleName

    var messages by mutableStateOf(listOf("Initializing..."))
        private set

    var message by mutableStateOf("")
        private set

    // Add inference settings state
    val inferenceSettings = InferenceSettingsState()

    override fun onCleared() {
        super.onCleared()

        viewModelScope.launch {
            try {
                llamaAndroid.unload()
            } catch (exc: IllegalStateException) {
                messages += exc.message!!
            }
        }
    }

    fun send() {
        val text = message
        message = ""

        // Add to messages console.
        messages += text
        messages += ""

        // Get current settings for inference
        val settings = inferenceSettings.toInferenceSettings()
        
        // Log inference settings for debugging
        messages += "Using settings: Tokens=${settings.maxTokens}, Temp=${settings.temperature}, TopK=${settings.topK}, TopP=${settings.topP}, Accel=${settings.accelerator.displayName}"

        viewModelScope.launch {
            llamaAndroid.send(
                message = text,
                formatChat = false,
                maxTokens = settings.maxTokens,
                topK = settings.topK,
                topP = settings.topP,
                temperature = settings.temperature
            )
                .catch { exception ->
                    Log.e(tag, "send() failed", exception)
                    messages += exception.message!!
                }
                .flowOn(Dispatchers.IO)
                .collect { token -> messages += token }
        }
    }

    fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1) {
        viewModelScope.launch {
            try {
                val start = System.nanoTime()
                val warmupResult = llamaAndroid.bench(pp, tg, pl, nr)
                val end = System.nanoTime()

                messages += warmupResult

                val warmup = (end - start).toDouble() / NanosPerSecond
                messages += "Warm up time: $warmup seconds, please wait..."

                if (warmup > 5.0) {
                    messages += "Warm up took too long, aborting benchmark"
                    return@launch
                }

                messages += llamaAndroid.bench(512, 128, 1, 3)
            } catch (exc: IllegalStateException) {
                Log.e(tag, "bench() failed", exc)
                messages += exc.message!!
            }
        }
    }

    fun load(pathToModel: String) {
        viewModelScope.launch {
            try {
                val settings = inferenceSettings.toInferenceSettings()
                llamaAndroid.load(
                    pathToModel = pathToModel,
                    nGpuLayers = settings.nGpuLayers
                )
                messages += "Loaded $pathToModel with ${settings.accelerator.displayName}"
                if (settings.accelerator == AcceleratorType.GPU) {
                    messages += "GPU Layers: ${settings.nGpuLayers}"
                }
            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                messages += exc.message!!
            }
        }
    }

    fun loadModelWithCallback(pathToModel: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val settings = inferenceSettings.toInferenceSettings()
                llamaAndroid.load(
                    pathToModel = pathToModel,
                    nGpuLayers = settings.nGpuLayers
                )
                messages += "Successfully loaded: ${java.io.File(pathToModel).name}"
                messages += "Accelerator: ${settings.accelerator.displayName}"
                if (settings.accelerator == AcceleratorType.GPU) {
                    messages += "GPU Layers: ${settings.nGpuLayers}"
                }
                onComplete(true)
            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                messages += "Failed to load model: ${exc.message}"
                onComplete(false)
            }
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            try {
                llamaAndroid.unload()
                messages += "Model unloaded successfully"
            } catch (exc: IllegalStateException) {
                Log.e(tag, "unload() failed", exc)
                messages += "Failed to unload model: ${exc.message}"
            }
        }
    }

    fun updateMessage(newMessage: String) {
        message = newMessage
    }

    fun clear() {
        messages = listOf()
    }

    fun log(message: String) {
        messages += message
    }
}
