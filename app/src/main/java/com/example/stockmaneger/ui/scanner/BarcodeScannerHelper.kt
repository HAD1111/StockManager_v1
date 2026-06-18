package com.example.stockmaneger.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Reusable barcode scanner — attach it to any Fragment (Stock, Sales,
 * and later a Cart/Checkout screen) that needs to turn a camera scan
 * into a String.
 *
 * Future POS readiness: this class holds no "session" or "cart" state.
 * Each call to scan() is independent, so a future checkout flow can call
 * scan() repeatedly (scan → add to cart → scan again) without any change
 * here — only the onResult callback's body needs to change.
 *
 * IMPORTANT — must be constructed in onCreate(), not in onViewCreated()
 * or inside a dialog function. registerForActivityResult() has to run
 * before the Fragment reaches STARTED, and onCreate() is the safest,
 * earliest point where requireContext() is also already available.
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *       super.onCreate(savedInstanceState)
 *       barcodeScanner = BarcodeScannerHelper(this, requireContext()) { result ->
 *           // result is the scanned String, or null if the user cancelled
 *       }
 *   }
 */
class BarcodeScannerHelper(
    caller: ActivityResultCaller,
    private val context: Context,
    private val onResult: (String?) -> Unit
) {

    private val scanLauncher = caller.registerForActivityResult(ScanContract()) { result ->
        onResult(result.contents) // null when the user backs out of the camera
    }

    private val permissionLauncher = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchScanner()
        } else {
            Toast.makeText(context, "Camera permission is required to scan barcodes", Toast.LENGTH_SHORT).show()
        }
    }

    /** Starts a scan. Requests camera permission automatically if needed. */
    fun scan() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) launchScanner() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            setPrompt("Scan a product barcode")
            setBeepEnabled(true)
            setOrientationLocked(true)
        }
        scanLauncher.launch(options)
    }
}