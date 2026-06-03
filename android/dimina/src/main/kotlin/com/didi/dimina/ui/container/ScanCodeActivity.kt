package com.didi.dimina.ui.container

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * 扫码 Activity
 *
 * 根据 Android 版本使用不同的扫码引擎策略：
 * - Android 9+ (API 28+)：ML Kit Barcode Scanning
 *   （更准确，原生支持 DataMatrix、PDF417 等仓储常用格式）
 * - Android < 9 (API 26-27)：ZXing
 *   （轻量级，无 Google Play Services 依赖，支持所有常见 1D/2D 条码）
 */
class ScanCodeActivity : ComponentActivity() {

    private var isProcessing = false
    private var isScanning = true
    private var continuous = false
    private var canOpenAlbum = false
    private var previewView: PreviewView? = null

    // 连续扫码结果列表（Compose state）
    private val scanResults = mutableStateListOf<ScanResultItem>()

    // UI 配置（通过 Intent 传入，可被 wx.scanCode 参数覆盖）
    private var config = ScanCodeConfig.DEFAULT

    // ML Kit 扫码器（Android 9+）
    private var mlKitScanner: BarcodeScanner? = null
    // ZXing 扫码器（Android < 9 备选方案）
    private var zxingReader: MultiFormatReader? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val albumLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                bitmap?.let { decodeBitmap(it) }
            } else {
                finishWithCancel()
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) albumLauncher.launch("image/*")
            else Toast.makeText(this, "需要相册权限", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        canOpenAlbum = intent.getBooleanExtra("canOpenAlbum", false)
        continuous = intent.getBooleanExtra("continuous", false)
        config = ScanCodeConfig.fromIntent(intent)
        initScanner()

        // 注册返回键处理（兼容 Android 13+）
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (continuous && scanResults.isNotEmpty()) {
                    finishWithBatchResults()
                } else {
                    finishWithCancel()
                }
            }
        })

        setContent {
            ScanCodeScreen(
                config = config,
                canOpenAlbum = canOpenAlbum,
                continuous = continuous,
                scanResults = scanResults,
                onFinishClick = { finishWithBatchResults() },
                onAlbumClick = { checkAlbumPermission() },
                onBackPress = {
                    if (continuous && scanResults.isNotEmpty()) {
                        finishWithBatchResults()
                    } else {
                        finishWithCancel()
                    }
                },
                onPreviewViewCreated = { pv ->
                    previewView = pv
                    startCamera()
                },
            )
        }
    }

    private fun initScanner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9+ → ML Kit
            mlKitScanner = BarcodeScanning.getClient()
        } else {
            // Android < 9 → ZXing
            zxingReader = MultiFormatReader().apply {
                val hints = HashMap<com.google.zxing.DecodeHintType, Any>()
                hints[com.google.zxing.DecodeHintType.POSSIBLE_FORMATS] = listOf(
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    com.google.zxing.BarcodeFormat.CODE_128,
                    com.google.zxing.BarcodeFormat.CODE_39,
                    com.google.zxing.BarcodeFormat.CODE_93,
                    com.google.zxing.BarcodeFormat.EAN_13,
                    com.google.zxing.BarcodeFormat.EAN_8,
                    com.google.zxing.BarcodeFormat.UPC_A,
                    com.google.zxing.BarcodeFormat.UPC_E,
                    com.google.zxing.BarcodeFormat.CODABAR,
                    com.google.zxing.BarcodeFormat.ITF,
                    com.google.zxing.BarcodeFormat.DATA_MATRIX,
                    com.google.zxing.BarcodeFormat.PDF_417,
                )
                hints[com.google.zxing.DecodeHintType.TRY_HARDER] = true
                setHints(hints)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(960, 1280))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isProcessing && isScanning) {
                            processFrame(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }
            // 将 Preview 连接到 PreviewView 的输出 surface
            previewView?.let { pv ->
                preview.surfaceProvider = pv.surfaceProvider
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            } catch (e: Exception) {
                finishWithResult(null, "failed: camera error - ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        isProcessing = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            processWithMLKit(imageProxy)
        } else {
            processWithZXing(imageProxy)
        }
    }

    /**
     * ML Kit 解码 — Android 9+
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
    private fun processWithMLKit(imageProxy: ImageProxy) {
        @Suppress("DEPRECATION")
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            mlKitScanner?.process(inputImage)
                ?.addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank()) {
                            imageProxy.close()
                            if (continuous) {
                                // 连续模式：添加结果到列表，继续扫码
                                scanResults.add(ScanResultItem(rawValue, barcode.formatToScanType()))
                                isProcessing = false
                            } else {
                                isScanning = false
                                finishWithResult(rawValue, barcode.formatToScanType())
                            }
                            return@addOnSuccessListener
                        }
                    }
                    isProcessing = false
                    imageProxy.close()
                }
                ?.addOnFailureListener {
                    isProcessing = false
                    imageProxy.close()
                }
        } else {
            isProcessing = false
            imageProxy.close()
        }
    }

    /**
     * ZXing 解码 — Android < 9
     */
    private fun processWithZXing(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        if (bitmap == null) {
            isProcessing = false
            imageProxy.close()
            return
        }
        try {
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = zxingReader?.decodeWithState(binaryBitmap)
            if (result != null && !result.text.isNullOrBlank()) {
                imageProxy.close()
                if (continuous) {
                    scanResults.add(ScanResultItem(result.text, result.barcodeFormat.toScanType()))
                    isProcessing = false
                } else {
                    isScanning = false
                    finishWithResult(result.text, result.barcodeFormat.toScanType())
                }
                return
            }
        } catch (_: Exception) { }
        isProcessing = false
        imageProxy.close()
    }

    /**
     * 从相册图片解码
     */
    private fun decodeBitmap(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            mlKitScanner?.process(inputImage)
                ?.addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes[0]
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank()) {
                            finishWithResult(rawValue, barcode.formatToScanType())
                            return@addOnSuccessListener
                        }
                    }
                    Toast.makeText(this, "未识别到条码", Toast.LENGTH_SHORT).show()
                }
                ?.addOnFailureListener {
                    Toast.makeText(this, "识别失败", Toast.LENGTH_SHORT).show()
                }
        } else {
            try {
                val intArray = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val result = zxingReader?.decodeWithState(binaryBitmap)
                if (result != null && !result.text.isNullOrBlank()) {
                    finishWithResult(result.text, result.barcodeFormat.toScanType())
                } else {
                    Toast.makeText(this, "未识别到条码", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this, "识别失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAlbumPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            albumLauncher.launch("image/*")
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun finishWithResult(text: String?, scanType: String?) {
        Intent().apply {
            if (text != null) {
                putExtra("result", text)
                putExtra("scanType", scanType ?: "UNKNOWN")
                putExtra("charSet", "utf-8")
                putExtra("errMsg", "scanCode:ok")
            } else {
                putExtra("errMsg", "scanCode:fail")
            }
            setResult(RESULT_OK, this)
        }
        finish()
    }

    /**
     * 连续扫码模式下，将批量结果通过 Intent 返回
     */
    private fun finishWithBatchResults() {
        val resultsArray = org.json.JSONArray()
        scanResults.forEach { item ->
            resultsArray.put(org.json.JSONObject().apply {
                put("result", item.text)
                put("scanType", item.scanType)
                put("charSet", "utf-8")
            })
        }
        Intent().apply {
            putExtra("result", resultsArray.toString())
            putExtra("scanType", "BATCH")
            putExtra("charSet", "utf-8")
            putExtra("errMsg", "scanCode:ok")
            putExtra("batch", true)
            setResult(RESULT_OK, this)
        }
        finish()
    }

    private fun finishWithCancel() {
        Intent().apply {
            putExtra("errMsg", "scanCode:fail cancel")
            setResult(RESULT_CANCELED, this)
        }
        finish()
    }



    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        mlKitScanner?.close()
    }

    // ===== Barcode Format 转 scanType 字符串 =====

    private fun Barcode.formatToScanType(): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        else -> "UNKNOWN"
    }

    private fun com.google.zxing.BarcodeFormat.toScanType(): String = when (this) {
        com.google.zxing.BarcodeFormat.QR_CODE -> "QR_CODE"
        com.google.zxing.BarcodeFormat.CODE_128 -> "CODE_128"
        com.google.zxing.BarcodeFormat.CODE_39 -> "CODE_39"
        com.google.zxing.BarcodeFormat.CODE_93 -> "CODE_93"
        com.google.zxing.BarcodeFormat.CODABAR -> "CODABAR"
        com.google.zxing.BarcodeFormat.DATA_MATRIX -> "DATA_MATRIX"
        com.google.zxing.BarcodeFormat.EAN_13 -> "EAN_13"
        com.google.zxing.BarcodeFormat.EAN_8 -> "EAN_8"
        com.google.zxing.BarcodeFormat.ITF -> "ITF"
        com.google.zxing.BarcodeFormat.PDF_417 -> "PDF417"
        com.google.zxing.BarcodeFormat.UPC_A -> "UPC_A"
        com.google.zxing.BarcodeFormat.UPC_E -> "UPC_E"
        com.google.zxing.BarcodeFormat.AZTEC -> "AZTEC"
        else -> "UNKNOWN"
    }

    /**
     * ImageProxy 转 Bitmap（供 ZXing 使用）
     */
    private fun ImageProxy.toBitmap(): Bitmap? {
        val buffer = planes[0].buffer ?: return null
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val yuvImage = YuvImage(bytes, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val jpegData = out.toByteArray()
        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
    }
}

// ========================
// Compose UI
// ========================

/**
 * 扫码界面 — Compose 构建，所有 UI 文案和颜色均可通过 config 参数定制
 */
@Composable
private fun ScanCodeScreen(
    config: ScanCodeConfig,
    canOpenAlbum: Boolean,
    continuous: Boolean = false,
    scanResults: List<ScanResultItem> = emptyList(),
    onFinishClick: () -> Unit = {},
    onAlbumClick: () -> Unit,
    onBackPress: () -> Unit,
    onPreviewViewCreated: (PreviewView) -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // CameraX PreviewView
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { pv ->
                    onPreviewViewCreated(pv)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 扫描框覆盖层 + 结果列表
        Column(modifier = Modifier.fillMaxSize()) {
            // 上半部分：相机预览 + 扫描框
            Box(modifier = Modifier.weight(1f)) {
                // 扫描框 Canvas（与 PreviewView 重叠）
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val frameW = w * config.frameWidthRatio.coerceIn(0.2f, 0.95f)
                    val frameH = frameW * config.frameAspectRatio.coerceIn(0.3f, 1.5f)
                    val left = (w - frameW) / 2f
                    val top = (h - frameH) / 2f * config.frameVerticalOffset.coerceIn(0.3f, 1.7f)
                    val right = left + frameW
                    val bottom = top + frameH

                    drawRect(config.maskColor, Offset(0f, 0f), Size(w, top))
                    drawRect(config.maskColor, Offset(0f, bottom), Size(w, h - bottom))
                    drawRect(config.maskColor, Offset(0f, top), Size(left, frameH))
                    drawRect(config.maskColor, Offset(right, top), Size(w - right, frameH))

                    drawRoundRect(
                        color = config.frameColor,
                        topLeft = Offset(left, top),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(8f),
                        style = Stroke(width = 3f)
                    )

                    val cl = 40f
                    drawLine(config.cornerColor, Offset(left, top), Offset(left + cl, top), strokeWidth = 6f)
                    drawLine(config.cornerColor, Offset(left, top), Offset(left, top + cl), strokeWidth = 6f)
                    drawLine(config.cornerColor, Offset(right, top), Offset(right - cl, top), strokeWidth = 6f)
                    drawLine(config.cornerColor, Offset(right, top), Offset(right, top + cl), strokeWidth = 6f)
                    drawLine(config.cornerColor, Offset(left, bottom), Offset(left + cl, bottom), strokeWidth = 6f)
                    drawLine(config.cornerColor, Offset(left, bottom), Offset(left, bottom - cl), strokeWidth = 6f)
                    drawLine(config.cornerColor, Offset(right, bottom), Offset(right - cl, bottom), strokeWidth = 6f)
                    drawLine(config.cornerColor, Offset(right, bottom), Offset(right, bottom - cl), strokeWidth = 6f)
                }

                // 顶部标题
                Text(
                    text = config.title,
                    color = config.titleColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp)
                )

                // 返回
                Text(
                    text = config.backText,
                    color = config.backTextColor,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 48.dp)
                        .clickable { onBackPress() }
                )
            }

            // 下半部分：扫码结果列表
            if (continuous) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A2E))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // 连续扫码状态提示
                    val count = scanResults.size
                    Text(
                        text = if (count > 0)
                            config.continuousHint.replace("%d", count.toString())
                        else config.hint,
                        color = config.hintColor,
                        fontSize = 13.sp,
                    )

                    // 结果列表（最多显示5条，其余可滚动）
                    if (count > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(scanResults.takeLast(20).reversed()) { item ->
                                ResultRow(item, config.cornerColor)
                            }
                        }
                    }

                    // 相册选图 + 完成按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (canOpenAlbum) {
                            Text(
                                text = config.albumText,
                                color = config.albumTextColor,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onAlbumClick() }
                                    .align(Alignment.CenterVertically)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = onFinishClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = config.cornerColor,
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Text(
                                text = config.finishText.replace("%d", count.toString()),
                                fontSize = 14.sp,
                                color = Color.White,
                            )
                        }
                    }
                }
            } else {
                // 非连续模式：底部提示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC1A1A2E))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = config.hint,
                        color = config.hintColor,
                        fontSize = 14.sp,
                    )
                }

                // 相册选图
                if (canOpenAlbum) {
                    Text(
                        text = config.albumText,
                        color = config.albumTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 60.dp)
                            .clickable { onAlbumClick() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultRow(item: ScanResultItem, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x33FFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 序号圆点
        Box(
            modifier = Modifier
                .background(accentColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = item.scanType.takeWhile { it != '_' }.ifEmpty { "?" },
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.text,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 单次扫码结果
 */
data class ScanResultItem(val text: String, val scanType: String, val timestamp: Long = System.currentTimeMillis())

/**
 * 扫码界面 UI 配置
 *
 * 可通过 wx.scanCode 的参数自定义，例如：
 * ```js
 * wx.scanCode({
 *   title: '仓储扫码',
 *   hint: '请对准条码',
 *   cornerColor: '#FF5722',
 *   albumText: '从相册选取条码图片',
 * })
 * ```
 *
 * @property title 顶部标题文字（默认 "扫码"）
 * @property titleColor 标题颜色
 * @property hint 底部提示文字（默认 "将条码/二维码放入框内自动扫描"）
 * @property hintColor 提示文字颜色
 * @property frameColor 扫描框边框颜色
 * @property cornerColor 四角色彩
 * @property maskColor 遮罩颜色
 * @property albumText 相册选图按钮文字（默认 "从相册选择"）
 * @property albumTextColor 相册按钮文字颜色
 * @property backText 返回按钮文字（默认 "← 返回"）
 * @property backTextColor 返回按钮文字颜色
 */
data class ScanCodeConfig(
    val title: String,
    val titleColor: Color,
    val hint: String,
    val hintColor: Color,
    val frameColor: Color,
    val cornerColor: Color,
    val maskColor: Color,
    val albumText: String,
    val albumTextColor: Color,
    val backText: String,
    val backTextColor: Color,
    val continuous: Boolean = false,
    val continuousHint: String = "已识别 %d 个，继续扫码中…",
    val finishText: String = "完成 (%d)",
    /** 扫描框宽度占屏幕宽度的比例 0.0~1.0，默认 0.75 */
    val frameWidthRatio: Float = 0.75f,
    /** 扫描框高宽比，默认 0.7（即高度为宽度的 70%） */
    val frameAspectRatio: Float = 0.7f,
    /** 扫描框垂直位置偏移系数，1.0=正中间，<1.0=偏上，>1.0=偏下，默认 0.8 */
    val frameVerticalOffset: Float = 0.8f,
) {
    companion object {
        /** 默认配置 */
        val DEFAULT = ScanCodeConfig(
            title = "扫码",
            titleColor = Color.White,
            hint = "将条码/二维码放入框内自动扫描",
            hintColor = Color.White.copy(alpha = 0.8f),
            frameColor = Color.White,
            cornerColor = Color(0xFF00C853),
            maskColor = Color(0x80000000),
            albumText = "从相册选择",
            albumTextColor = Color(0xFF00C853),
            backText = "← 返回",
            backTextColor = Color.White,
        )

        /** 从 Intent 解析，使用默认值兜底 */
        fun fromIntent(intent: Intent): ScanCodeConfig {
            return ScanCodeConfig(
                title = intent.getStringExtra("cfg_title") ?: DEFAULT.title,
                titleColor = parseColor(intent.getStringExtra("cfg_titleColor"), DEFAULT.titleColor),
                hint = intent.getStringExtra("cfg_hint") ?: DEFAULT.hint,
                hintColor = parseColor(intent.getStringExtra("cfg_hintColor"), DEFAULT.hintColor),
                frameColor = parseColor(intent.getStringExtra("cfg_frameColor"), DEFAULT.frameColor),
                cornerColor = parseColor(intent.getStringExtra("cfg_cornerColor"), DEFAULT.cornerColor),
                maskColor = DEFAULT.maskColor,
                albumText = intent.getStringExtra("cfg_albumText") ?: DEFAULT.albumText,
                albumTextColor = parseColor(intent.getStringExtra("cfg_albumTextColor"), DEFAULT.albumTextColor),
                backText = intent.getStringExtra("cfg_backText") ?: DEFAULT.backText,
                backTextColor = parseColor(intent.getStringExtra("cfg_backTextColor"), DEFAULT.backTextColor),
                frameWidthRatio = intent.getFloatExtra("cfg_frameWidthRatio", DEFAULT.frameWidthRatio),
                frameAspectRatio = intent.getFloatExtra("cfg_frameAspectRatio", DEFAULT.frameAspectRatio),
                frameVerticalOffset = intent.getFloatExtra("cfg_frameVerticalOffset", DEFAULT.frameVerticalOffset),
            )
        }

        /** 将 UI 配置写入 Intent */
        fun putToIntent(intent: Intent, config: ScanCodeConfig) {
            intent.putExtra("cfg_title", config.title)
            intent.putExtra("cfg_titleColor", config.titleColor.toHexString())
            intent.putExtra("cfg_hint", config.hint)
            intent.putExtra("cfg_hintColor", config.hintColor.toHexString())
            intent.putExtra("cfg_frameColor", config.frameColor.toHexString())
            intent.putExtra("cfg_cornerColor", config.cornerColor.toHexString())
            intent.putExtra("cfg_albumText", config.albumText)
            intent.putExtra("cfg_albumTextColor", config.albumTextColor.toHexString())
            intent.putExtra("cfg_backText", config.backText)
            intent.putExtra("cfg_backTextColor", config.backTextColor.toHexString())
            intent.putExtra("continuous", config.continuous)
            if (config.continuousHint.isNotEmpty()) intent.putExtra("cfg_continuousHint", config.continuousHint)
            if (config.finishText.isNotEmpty()) intent.putExtra("cfg_finishText", config.finishText)
            intent.putExtra("cfg_frameWidthRatio", config.frameWidthRatio)
            intent.putExtra("cfg_frameAspectRatio", config.frameAspectRatio)
            intent.putExtra("cfg_frameVerticalOffset", config.frameVerticalOffset)
        }

        /**
         * 从 JSONObject 解析 UI 配置
         * 与 wx.scanCode 的参数映射 —— 在 ScanApi 中调用
         */
        fun fromJson(params: JSONObject?): ScanCodeConfig {
            if (params == null) return DEFAULT
            val continuous = params.optBoolean("continuous", false)
            return ScanCodeConfig(
                title = params.optString("title", DEFAULT.title),
                titleColor = parseColor(params.optString("titleColor"), DEFAULT.titleColor),
                hint = if (continuous) params.optString("hint",
                    "连续扫码中，识别后自动继续") else params.optString("hint", DEFAULT.hint),
                hintColor = parseColor(params.optString("hintColor"), DEFAULT.hintColor).copy(alpha = 0.8f),
                frameColor = parseColor(params.optString("frameColor"), DEFAULT.frameColor),
                cornerColor = parseColor(params.optString("cornerColor"), DEFAULT.cornerColor),
                maskColor = DEFAULT.maskColor,
                albumText = params.optString("albumText", DEFAULT.albumText),
                albumTextColor = parseColor(params.optString("albumTextColor"), DEFAULT.albumTextColor),
                backText = params.optString("backText", DEFAULT.backText),
                backTextColor = parseColor(params.optString("backTextColor"), DEFAULT.backTextColor),
                continuous = continuous,
                continuousHint = params.optString("continuousHint", "已识别 %d 个，继续扫码中…"),
                finishText = params.optString("finishText", "完成 (%d)"),
                frameWidthRatio = params.optDouble("frameWidthRatio", DEFAULT.frameWidthRatio.toDouble()).toFloat(),
                frameAspectRatio = params.optDouble("frameAspectRatio", DEFAULT.frameAspectRatio.toDouble()).toFloat(),
                frameVerticalOffset = params.optDouble("frameVerticalOffset", DEFAULT.frameVerticalOffset.toDouble()).toFloat(),
            )
        }

        /** 解析 #RRGGBB / #AARRGGBB 颜色字符串 */
        private fun parseColor(hex: String?, fallback: Color): Color {
            if (hex == null) return fallback
            return try {
                val clean = hex.removePrefix("#")
                val argb = when (clean.length) {
                    6 -> (0xFF000000 or clean.toLong(16))
                    8 -> clean.toLong(16)
                    else -> return fallback
                }
                Color(argb.toInt())
            } catch (_: Exception) { fallback }
        }
    }
}

/** Compose Color 转 #AARRGGBB 字符串 */
private fun Color.toHexString(): String {
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    val a = (alpha * 255).toInt()
    return String.format("#%02X%02X%02X%02X", a, r, g, b)
}
