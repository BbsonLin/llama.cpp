package com.example.llama

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun InferenceSettingsPanel(
    settingsState: InferenceSettingsState,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inference Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Collapsible content
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Accelerator Selection
                AcceleratorSelector(
                    selectedAccelerator = settingsState.accelerator,
                    onAcceleratorChanged = settingsState::updateAccelerator
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // GPU Layers (only show when GPU is selected)
                if (settingsState.accelerator == AcceleratorType.GPU) {
                    GpuLayersSlider(
                        nGpuLayers = settingsState.nGpuLayers,
                        onNGpuLayersChanged = settingsState::updateNGpuLayers
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Max Tokens
                IntSliderSetting(
                    label = "Max Tokens",
                    value = settingsState.maxTokens,
                    onValueChange = settingsState::updateMaxTokens,
                    valueRange = 50..2048,
                    steps = 39 // (2048-50)/50 steps
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Temperature
                FloatSliderSetting(
                    label = "Temperature",
                    value = settingsState.temperature,
                    onValueChange = settingsState::updateTemperature,
                    valueRange = 0.1f..2.0f,
                    steps = 18, // 0.1 increments
                    format = "%.1f"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Top-K
                IntSliderSetting(
                    label = "Top-K",
                    value = settingsState.topK,
                    onValueChange = settingsState::updateTopK,
                    valueRange = 1..100,
                    steps = 19 // 5-unit steps
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Top-P
                FloatSliderSetting(
                    label = "Top-P",
                    value = settingsState.topP,
                    onValueChange = settingsState::updateTopP,
                    valueRange = 0.1f..1.0f,
                    steps = 8, // 0.1 increments
                    format = "%.1f"
                )
            }
        }
    }
}

@Composable
private fun AcceleratorSelector(
    selectedAccelerator: AcceleratorType,
    onAcceleratorChanged: (AcceleratorType) -> Unit
) {
    Column {
        Text(
            text = "Accelerator",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AcceleratorType.values().forEach { accelerator ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAcceleratorChanged(accelerator) }
                ) {
                    RadioButton(
                        selected = selectedAccelerator == accelerator,
                        onClick = { onAcceleratorChanged(accelerator) }
                    )
                    Text(
                        text = accelerator.displayName,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GpuLayersSlider(
    nGpuLayers: Int,
    onNGpuLayersChanged: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "GPU Layers",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = if (nGpuLayers == 0) "Auto" else nGpuLayers.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = nGpuLayers.toFloat(),
            onValueChange = { onNGpuLayersChanged(it.roundToInt()) },
            valueRange = 0f..99f,
            steps = 10,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Text(
            text = "0 = Auto detect, 99 = All layers on GPU",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun IntSliderSetting(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    steps: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = steps,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun FloatSliderSetting(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: String = "%.2f"
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = format.format(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
} 