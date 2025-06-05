package com.example.llama

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class InferenceSettings(
    val maxTokens: Int = 512,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val temperature: Float = 0.8f,
    val accelerator: AcceleratorType = AcceleratorType.CPU,
    val nGpuLayers: Int = 0
)

enum class AcceleratorType(val displayName: String) {
    CPU("CPU Only"),
    GPU("GPU Accelerated")
}

class InferenceSettingsState {
    var maxTokens by mutableStateOf(512)
        private set
    
    var topK by mutableStateOf(40)
        private set
    
    var topP by mutableStateOf(0.9f)
        private set
    
    var temperature by mutableStateOf(0.8f)
        private set
    
    var accelerator by mutableStateOf(AcceleratorType.CPU)
        private set
    
    var nGpuLayers by mutableStateOf(0)
        private set

    fun updateMaxTokens(value: Int) {
        maxTokens = value.coerceIn(1, 2048)
    }

    fun updateTopK(value: Int) {
        topK = value.coerceIn(1, 100)
    }

    fun updateTopP(value: Float) {
        topP = value.coerceIn(0.1f, 1.0f)
    }

    fun updateTemperature(value: Float) {
        temperature = value.coerceIn(0.1f, 2.0f)
    }

    fun updateAccelerator(value: AcceleratorType) {
        accelerator = value
        // Reset GPU layers when switching to CPU
        if (value == AcceleratorType.CPU) {
            nGpuLayers = 0
        } else {
            // Set default GPU layers when switching to GPU
            if (nGpuLayers == 0) {
                nGpuLayers = 32
            }
        }
    }

    fun updateNGpuLayers(value: Int) {
        nGpuLayers = value.coerceIn(0, 99)
    }

    fun toInferenceSettings(): InferenceSettings {
        return InferenceSettings(
            maxTokens = maxTokens,
            topK = topK,
            topP = topP,
            temperature = temperature,
            accelerator = accelerator,
            nGpuLayers = if (accelerator == AcceleratorType.GPU) nGpuLayers else 0
        )
    }
} 