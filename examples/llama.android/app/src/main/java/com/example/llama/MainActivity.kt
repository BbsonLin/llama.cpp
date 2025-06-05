package com.example.llama

import android.Manifest
import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.Settings
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.example.llama.ui.theme.LlamaAndroidTheme
import java.io.File

class MainActivity(
    activityManager: ActivityManager? = null,
    downloadManager: DownloadManager? = null,
    clipboardManager: ClipboardManager? = null,
): ComponentActivity() {
    private val tag: String? = this::class.simpleName

    private val activityManager by lazy { activityManager ?: getSystemService<ActivityManager>()!! }
    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy { clipboardManager ?: getSystemService<ClipboardManager>()!! }

    private val viewModel: MainViewModel by viewModels()

    // Permission launcher for external storage access
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            viewModel.log("Storage permissions granted")
            // Check if we still need MANAGE_EXTERNAL_STORAGE for Android 11+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                requestManageExternalStoragePermission()
            }
        } else {
            viewModel.log("Storage permissions denied - scanning will be limited to app directory")
        }
    }

    // For Android 11+ MANAGE_EXTERNAL_STORAGE permission
    private val manageExternalStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                viewModel.log("All files access permission granted")
            } else {
                viewModel.log("All files access permission denied - limited file access")
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        // For Android 13+ (API 33+), we need different permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_* permissions instead of READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+ but below 13
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            // If basic permissions are granted, check for MANAGE_EXTERNAL_STORAGE on Android 11+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                requestManageExternalStoragePermission()
            }
        }
    }

    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            viewModel.log("Requesting all files access permission for broader file scanning...")
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                manageExternalStorageLauncher.launch(intent)
            } catch (e: Exception) {
                viewModel.log("Error requesting all files access: ${e.message}")
                // Fallback to general manage all files settings
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageExternalStorageLauncher.launch(intent)
            }
        }
    }

    private fun hasStoragePermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ prefers MANAGE_EXTERNAL_STORAGE for full access
                Environment.isExternalStorageManager() || 
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            else -> true // Permissions granted by default on older Android versions
        }
    }

    // Get a MemoryInfo object for the device's current memory status.
    private fun availableMemory(): ActivityManager.MemoryInfo {
        return ActivityManager.MemoryInfo().also { memoryInfo ->
            activityManager.getMemoryInfo(memoryInfo)
        }
    }

    private fun scanForGgufFiles(): List<File> {
        val ggufFiles = mutableListOf<File>()

        // Always scan external files directory (app-specific - no permissions needed)
        val extFilesDir = getExternalFilesDir(null)
        extFilesDir?.let { dir ->
            ggufFiles.addAll(findGgufFiles(dir))
        }

        // Only scan public directories if we have permissions
        if (hasStoragePermissions()) {
            viewModel.log("Storage permissions available - scanning public directories")
            
            // Scan Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir?.exists() == true) {
                ggufFiles.addAll(findGgufFiles(downloadsDir))
            }

            // Scan Documents directory
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (documentsDir?.exists() == true) {
                ggufFiles.addAll(findGgufFiles(documentsDir))
            }

            // For Android 11+ with MANAGE_EXTERNAL_STORAGE, we can scan broader areas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                val externalStorage = Environment.getExternalStorageDirectory()
                if (externalStorage?.exists() == true) {
                    // Only scan common folders to avoid overwhelming results
                    val commonFolders = listOf("Download", "Documents", "Models", "AI")
                    commonFolders.forEach { folderName ->
                        val folder = File(externalStorage, folderName)
                        if (folder.exists()) {
                            ggufFiles.addAll(findGgufFiles(folder))
                        }
                    }
                }
            }
        } else {
            viewModel.log("Storage permissions denied - scanning will be limited to app directory")
            viewModel.log("To access files in Downloads/Documents, please grant storage permissions")
        }

        return ggufFiles.distinctBy { it.absolutePath }
    }

    private fun findGgufFiles(directory: File): List<File> {
        val ggufFiles = mutableListOf<File>()

        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isDirectory -> {
                        // Recursively search subdirectories (limited depth to avoid deep recursion)
                        if (file.absolutePath.split(File.separator).size < 10) {
                            ggufFiles.addAll(findGgufFiles(file))
                        }
                    }
                    file.isFile && file.name.lowercase().endsWith(".gguf") -> {
                        ggufFiles.add(file)
                    }
                }
            }
        } catch (e: SecurityException) {
            viewModel.log("Permission denied accessing: ${directory.absolutePath}")
        } catch (e: Exception) {
            viewModel.log("Error scanning directory ${directory.absolutePath}: ${e.message}")
        }

        return ggufFiles
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions for external storage access
        checkAndRequestPermissions()

        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        val free = Formatter.formatFileSize(this, availableMemory().availMem)
        val total = Formatter.formatFileSize(this, availableMemory().totalMem)

        viewModel.log("Current memory: $free / $total")
        viewModel.log("Downloads directory: ${getExternalFilesDir(null)}")

        val extFilesDir = getExternalFilesDir(null)

        val models = listOf(
            Downloadable(
                "Phi-2 7B (Q4_0, 1.6 GiB)",
                Uri.parse("https://huggingface.co/ggml-org/models/resolve/main/phi-2/ggml-model-q4_0.gguf?download=true"),
                File(extFilesDir, "phi-2-q4_0.gguf"),
            ),
            Downloadable(
                "TinyLlama 1.1B (f16, 2.2 GiB)",
                Uri.parse("https://huggingface.co/ggml-org/models/resolve/main/tinyllama-1.1b/ggml-model-f16.gguf?download=true"),
                File(extFilesDir, "tinyllama-1.1-f16.gguf"),
            ),
            Downloadable(
                "Phi 2 DPO (Q3_K_M, 1.48 GiB)",
                Uri.parse("https://huggingface.co/TheBloke/phi-2-dpo-GGUF/resolve/main/phi-2-dpo.Q3_K_M.gguf?download=true"),
                File(extFilesDir, "phi-2-dpo.Q3_K_M.gguf")
            ),
        )

        setContent {
            LlamaAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainCompose(
                        viewModel,
                        clipboardManager,
                        downloadManager,
                        models,
                        onScanGgufFiles = { scanForGgufFiles() },
                        onRequestPermissions = { checkAndRequestPermissions() },
                        hasStoragePermissions = { hasStoragePermissions() }
                    )
                }

            }
        }
    }
}

@Composable
fun MainCompose(
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    dm: DownloadManager,
    models: List<Downloadable>,
    onScanGgufFiles: () -> List<File>,
    onRequestPermissions: () -> Unit,
    hasStoragePermissions: () -> Boolean
) {
    var discoveredGgufFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadedModelPath by remember { mutableStateOf<String?>(null) }
    var isGgufSectionExpanded by remember { mutableStateOf(true) }
    var isSettingsExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp)
    ) {
        // Chat messages area - limited height
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(bottom = 8.dp)
        ) {
            val lazyScrollState = rememberLazyListState()
            LazyColumn(state = lazyScrollState) {
                items(viewModel.messages) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                        modifier = Modifier.padding(16.dp, 4.dp)
                    )
                }
            }
        }

        // Inference Settings Panel
        InferenceSettingsPanel(
            settingsState = viewModel.inferenceSettings,
            isExpanded = isSettingsExpanded,
            onToggleExpanded = { isSettingsExpanded = !isSettingsExpanded },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Input field
        OutlinedTextField(
            value = viewModel.message,
            onValueChange = { viewModel.updateMessage(it) },
            label = { Text("Message") },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Action buttons
        Row(modifier = Modifier.padding(16.dp, 8.dp)) {
            Button({ viewModel.send() }) { Text("Send") }
            Button({ viewModel.bench(8, 4, 1) }) { Text("Bench") }
            Button({ viewModel.clear() }) { Text("Clear") }
            Button({
                viewModel.messages.joinToString("\n").let {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", it))
                }
            }) { Text("Copy") }
        }

        // Show currently loaded model info
        loadedModelPath?.let { path ->
            Text(
                "Currently Loaded: ${File(path).name}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp)
            )
        }

        // Collapsible GGUF Files Section
        Column {
            // Header with expand/collapse functionality
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isGgufSectionExpanded = !isGgufSectionExpanded }
                    .padding(16.dp, 16.dp, 16.dp, 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GGUF Files Manager",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isGgufSectionExpanded) "▼" else "▶",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Collapsible content
            if (isGgufSectionExpanded) {
                Column {
                    // Instruction text for GGUF files
                    Text(
                        "To use your own GGUF files:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 4.dp)
                    )
                    Text(
                        "• Place .gguf files in Downloads, Documents, or any folder\n" +
                        "• Grant storage permission when prompted\n" +
                        "• Tap 'Scan for GGUF Files' to find them",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp)
                    )

                    // Permission request button (show if permissions not granted)
                    if (!hasStoragePermissions()) {
                        Button(
                            onClick = { onRequestPermissions() },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text("Grant Storage Permissions")
                        }
                        Text(
                            "Storage permissions are required to scan files outside the app directory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp)
                        )
                    }

                    // Scan for existing GGUF files button
                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Button(
                            onClick = {
                                discoveredGgufFiles = onScanGgufFiles()
                                if (discoveredGgufFiles.isEmpty()) {
                                    viewModel.log("No GGUF files found. Place .gguf files in Downloads, Documents, or any accessible folder.")
                                } else {
                                    viewModel.log("Found ${discoveredGgufFiles.size} GGUF files on device")
                                    discoveredGgufFiles.forEach { file ->
                                        val sizeInMB = file.length() / (1024 * 1024)
                                        viewModel.log("Found: ${file.name} (${sizeInMB} MB) at ${file.parent}")
                                    }
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Text(if (isLoading) "Loading Model..." else "Scan for GGUF Files")
                        }

                        // Add unload button if model is loaded
                        if (loadedModelPath != null) {
                            Button(
                                onClick = {
                                    viewModel.unloadModel()
                                    loadedModelPath = null
                                    isLoading = false
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Unload Model")
                            }
                        }
                    }

                    // Display discovered GGUF files
                    if (discoveredGgufFiles.isNotEmpty()) {
                        Text(
                            "Pre-downloaded GGUF Files (${discoveredGgufFiles.size} found):",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp)
                        )

                        Column {
                            discoveredGgufFiles.forEach { file ->
                                val sizeInMB = file.length() / (1024 * 1024)
                                val fileName = file.name.let {
                                    if (it.length > 25) "${it.take(22)}..." else it
                                }
                                val isCurrentlyLoaded = loadedModelPath == file.absolutePath

                                Button(
                                    onClick = {
                                        if (!isCurrentlyLoaded) {
                                            isLoading = true
                                            viewModel.log("Loading pre-downloaded model: ${file.name}")
                                            viewModel.log("From: ${file.absolutePath}")
                                            viewModel.loadModelWithCallback(file.absolutePath) { success ->
                                                isLoading = false
                                                if (success) {
                                                    loadedModelPath = file.absolutePath
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && !isCurrentlyLoaded,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        when {
                                            isCurrentlyLoaded -> "✓ Loaded: $fileName (${sizeInMB} MB)"
                                            isLoading -> "Loading..."
                                            else -> "Load $fileName (${sizeInMB} MB)"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Original downloadable models
        Text(
            "Download New Models:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp)
        )

        Column {
            for (model in models) {
                Downloadable.Button(viewModel, dm, model)
            }
        }
    }
}

