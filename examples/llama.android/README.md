# LLaMA Android App

An Android application for running Large Language Models using the llama.cpp backend.

## Features

- **Download and Run Models**: Download popular GGUF models directly in the app
- **Load Pre-downloaded Models**: Scan and load GGUF files you've already downloaded
- **Chat Interface**: Interactive chat with the loaded model
- **Benchmarking**: Performance testing for loaded models
- **Memory Monitoring**: Display current device memory usage

## Using Pre-downloaded GGUF Files

### Step 1: Obtain GGUF Files
Download GGUF format models from sources like:
- [Hugging Face](https://huggingface.co/models?library=gguf)
- [TheBloke's Models](https://huggingface.co/TheBloke)
- Convert your own models using llama.cpp tools

### Step 2: Place Files on Device
Transfer your `.gguf` files to your Android device using any of these methods:

**Option A: USB Transfer**
1. Connect your device to a computer via USB
2. Copy `.gguf` files to one of these folders:
   - `Downloads/`
   - `Documents/`
   - Any accessible folder on the device

**Option B: Cloud Storage**
1. Upload files to Google Drive, Dropbox, etc.
2. Download them on your Android device
3. Files will typically save to `Downloads/`

**Option C: Direct Download**
1. Use a browser or download manager on Android
2. Download `.gguf` files directly to the device

### Step 3: Load in App
1. Open the LLaMA Android app
2. Grant storage permissions when prompted
3. Tap "Scan for GGUF Files"
4. Select your model from the discovered files list
5. Wait for the model to load
6. Start chatting!

## Supported Locations

The app scans these directories for GGUF files:
- App's private external files directory (always accessible)
- Public Downloads folder (requires storage permission)
- Public Documents folder (requires storage permission)
- External storage root (requires storage permission)

## Model Recommendations

For optimal performance on mobile devices:
- **Small models (1-3B parameters)**: Good for basic conversations
- **Quantized models (Q4_0, Q4_K_M)**: Balance of quality and speed
- **Avoid large models (>7B)**: May cause out-of-memory errors

## Permissions

The app requires these permissions:
- `READ_EXTERNAL_STORAGE`: To scan for pre-downloaded GGUF files
- `WRITE_EXTERNAL_STORAGE`: To download new models
- `INTERNET`: To download models from the internet

## Troubleshooting

**No files found after scanning:**
- Ensure files have `.gguf` extension
- Check that storage permission is granted
- Try placing files in the Downloads folder

**Model fails to load:**
- Check available RAM (displayed in app)
- Try a smaller or more quantized model
- Ensure the file isn't corrupted

**Permission denied:**
- Grant storage permissions in Android Settings
- On Android 11+, you may need to enable "All files access"

## Building

This app is built using:
- Android Studio
- Jetpack Compose for UI
- llama.cpp for model inference
- Kotlin for the main application code

## Contributing

Contributions welcome! Please ensure:
- Code follows existing style
- New features include appropriate documentation
- Test on multiple Android versions when possible
