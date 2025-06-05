# 推理設定功能說明

## 新增功能概述

我們已經為 Android llama.cpp 應用程式新增了完整的推理參數控制功能，包括：

### 📱 UI 控制項
- **Max Tokens**: 控制生成的最大token數量 (50-2048)
- **Temperature**: 控制輸出的隨機性 (0.1-2.0)
- **Top-K**: 限制候選token數量 (1-100)
- **Top-P**: 累積機率閾值 (0.1-1.0)
- **Accelerator**: 選擇 CPU 或 GPU 推理
- **GPU Layers**: 設定GPU層數 (僅在GPU模式下顯示)

### 🏗️ 程式架構

#### 新增檔案
1. `InferenceSettings.kt` - 資料類別和狀態管理
2. `InferenceSettingsUI.kt` - UI 組件
3. `INFERENCE_SETTINGS_README.md` - 說明文件

#### 修改檔案
1. `MainActivity.kt` - 集成設定面板
2. `MainViewModel.kt` - 狀態管理和參數傳遞
3. `LLamaAndroid.kt` - 支援新參數
4. `llama-android.cpp` - 底層實現

### ⚙️ 參數說明

#### Max Tokens
- **範圍**: 50-2048
- **功能**: 控制模型生成回應的最大長度
- **建議**: 短回應用256，長回應用1024

#### Temperature
- **範圍**: 0.1-2.0
- **功能**: 控制輸出的創造性/隨機性
- **建議**: 
  - 0.1-0.3: 保守、一致的回應
  - 0.7-0.9: 平衡的回應
  - 1.2-2.0: 創意、多樣的回應

#### Top-K
- **範圍**: 1-100
- **功能**: 限制每次選擇的候選token數量
- **建議**: 40是常用值，較小值更保守

#### Top-P (Nucleus Sampling)
- **範圍**: 0.1-1.0
- **功能**: 基於累積機率的token選擇
- **建議**: 0.9是常用值，與Temperature搭配使用

#### GPU加速
- **CPU Only**: 純CPU推理，相容性最佳
- **GPU Accelerated**: 利用GPU加速，需要支援的設備

### 🔧 GPU支援狀況

#### Adreno GPU (Qualcomm)
- ✅ **完全支援** (透過OpenCL)
- 適用晶片: Snapdragon 8 Gen 1/2/3/Elite
- 編譯參數: `-DGGML_OPENCL_USE_ADRENO_KERNELS=ON`

#### Mali GPU (ARM)
- ✅ **部分支援** (透過Vulkan)
- 適用型號: Mali-G52 MP2, Mali-G78等
- 編譯參數: `-DGGML_VULKAN=ON`

### 📋 使用方式

1. **開啟設定面板**: 點擊"Inference Settings"展開設定選項
2. **選擇加速器**: 
   - 選擇"CPU Only"進行CPU推理
   - 選擇"GPU Accelerated"啟用GPU加速
3. **調整參數**: 使用滑桿調整各項推理參數
4. **載入模型**: 設定完成後載入.gguf模型檔案
5. **開始對話**: 發送訊息時會使用當前設定的參數

### 🚀 效能建議

#### 手機設備
- **高階設備** (8GB+ RAM): 可嘗試GPU加速 + 中等參數
- **中階設備** (6GB RAM): 建議CPU推理 + 較小max_tokens
- **入門設備** (4GB RAM): CPU推理 + 保守參數設定

#### 參數組合建議
```
創意寫作: temp=1.2, top_k=50, top_p=0.9, tokens=1024
日常對話: temp=0.8, top_k=40, top_p=0.9, tokens=512  
技術問答: temp=0.3, top_k=20, top_p=0.7, tokens=768
```

### 🔧 開發者注意事項

#### 待完成功能
- [ ] 實際GPU層數參數傳遞到native層
- [ ] 自定義採樣器參數的完整實現
- [ ] 模型載入時的GPU後端選擇
- [ ] 效能監控和GPU使用率顯示

#### 編譯需求
```bash
# Adreno GPU支援
cmake -DGGML_OPENCL=ON \
      -DGGML_OPENCL_USE_ADRENO_KERNELS=ON \
      -DGGML_OPENCL_EMBED_KERNELS=ON

# Mali GPU支援  
cmake -DGGML_VULKAN=ON \
      -DGGML_VULKAN_MEMORY_DEBUG=OFF
```

### 📝 更新日誌

#### v1.0
- ✅ 新增UI控制面板
- ✅ 基本參數傳遞框架
- ✅ CPU/GPU選擇器
- ✅ 可折疊式設定介面
- ⚠️ GPU後端編譯需要額外配置

---

如有問題或建議，請參考[llama.cpp官方文檔](https://github.com/ggerganov/llama.cpp)。 