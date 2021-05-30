/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.kotlin

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.demo.CameraXViewModel
import com.google.mlkit.vision.demo.GraphicOverlay
import com.google.mlkit.vision.demo.R
import com.google.mlkit.vision.demo.VisionImageProcessor
import com.google.mlkit.vision.demo.kotlin.labeldetector.LabelDetectorProcessor
import com.google.mlkit.vision.demo.kotlin.labeldetector.LabelGraphic
import com.google.mlkit.vision.demo.preference.PreferenceUtils
import com.google.mlkit.vision.demo.preference.SettingsActivity
import com.google.mlkit.vision.demo.preference.SettingsActivity.LaunchSource
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import java.util.ArrayList

/** Live preview demo app for ML Kit APIs using CameraX.  */
@KeepName
@RequiresApi(VERSION_CODES.LOLLIPOP)
class CameraXLivePreviewActivity :
  AppCompatActivity(),
  ActivityCompat.OnRequestPermissionsResultCallback,
  OnItemSelectedListener,
  CompoundButton.OnCheckedChangeListener {

  private var previewView: PreviewView? = null
  private var graphicOverlay: GraphicOverlay? = null
  private var cameraProvider: ProcessCameraProvider? = null
  private var previewUseCase: Preview? = null
  private var analysisUseCase: ImageAnalysis? = null
  private var imageProcessor: VisionImageProcessor? = null
  private var needUpdateGraphicOverlayImageSourceInfo = false
  private var selectedModel = MASK_V1
  private var lensFacing = CameraSelector.LENS_FACING_BACK
  private var cameraSelector: CameraSelector? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "onCreate")
    if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
      Toast.makeText(
        applicationContext,
        "CameraX is only supported on SDK version >=21. Current SDK version is " +
          VERSION.SDK_INT,
        Toast.LENGTH_LONG
      )
        .show()
      return
    }
    if (savedInstanceState != null) {
      selectedModel =
        savedInstanceState.getString(
          STATE_SELECTED_MODEL,
          MASK_V1
        )
    }
    cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    setContentView(R.layout.activity_vision_camerax_live_preview)
    previewView = findViewById(R.id.preview_view)
    if (previewView == null) {
      Log.d(TAG, "previewView is null")
    }
    graphicOverlay = findViewById(R.id.graphic_overlay)
    if (graphicOverlay == null) {
      Log.d(TAG, "graphicOverlay is null")
    }
    val spinner = findViewById<Spinner>(R.id.spinner)
    val options: MutableList<String> = ArrayList()
    options.add(MASK_V1)
    options.add(MASK_V2)
    options.add(MASK_V3)

    // Creating adapter for spinner
    val dataAdapter =
      ArrayAdapter(this, R.layout.spinner_style, options)
    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    // attaching data adapter to spinner
    spinner.adapter = dataAdapter
    spinner.onItemSelectedListener = this
    val facingSwitch =
      findViewById<ToggleButton>(R.id.facing_switch)
    facingSwitch.setOnCheckedChangeListener(this)
    ViewModelProvider(
      this,
      ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )
      .get(CameraXViewModel::class.java)
      .processCameraProvider
      .observe(
        this,
        Observer { provider: ProcessCameraProvider? ->
          cameraProvider = provider
          if (allPermissionsGranted()) {
            bindAllCameraUseCases(false)
          }
        }
      )
    val settingsButton = findViewById<ImageView>(R.id.settings_button)
    settingsButton.setOnClickListener {
      val intent =
        Intent(applicationContext, SettingsActivity::class.java)
      intent.putExtra(
        SettingsActivity.EXTRA_LAUNCH_SOURCE,
        LaunchSource.CAMERAX_LIVE_PREVIEW
      )
      startActivity(intent)
    }

    val inferenceButton = findViewById<Button>(R.id.inference_button)
    inferenceButton.setOnClickListener {
      if (allPermissionsGranted()) {
        graphicOverlay?.clear()
        bindAnalysisUseCase()
        Thread{
          Thread.sleep(3000)
          runOnUiThread {
            graphicOverlay?.clear()
          }
        }.start()
      }
    }

    if (!allPermissionsGranted()) {
      getRuntimePermissions()
    }
  }

  override fun onSaveInstanceState(bundle: Bundle) {
    super.onSaveInstanceState(bundle)
    bundle.putString(STATE_SELECTED_MODEL, selectedModel)
  }

  @Synchronized
  override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
    // An item was selected. You can retrieve the selected item using
    // parent.getItemAtPosition(pos)
    selectedModel = parent?.getItemAtPosition(pos).toString()
    Log.d(TAG, "Selected model: $selectedModel")
    //bindAnalysisUseCase()
  }

  override fun onNothingSelected(parent: AdapterView<*>?) {
    // Do nothing.
  }

  override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
    if (cameraProvider == null) {
      return
    }
    val newLensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
      CameraSelector.LENS_FACING_BACK
    } else {
      CameraSelector.LENS_FACING_FRONT
    }
    val newCameraSelector =
      CameraSelector.Builder().requireLensFacing(newLensFacing).build()
    try {
      if (cameraProvider!!.hasCamera(newCameraSelector)) {
        Log.d(TAG, "Set facing to " + newLensFacing)
        lensFacing = newLensFacing
        cameraSelector = newCameraSelector
        bindAllCameraUseCases(false)
        return
      }
    } catch (e: CameraInfoUnavailableException) {
      // Falls through
    }
    Toast.makeText(
      applicationContext, "This device does not have lens with facing: $newLensFacing",
      Toast.LENGTH_SHORT
    )
      .show()
  }

  public override fun onResume() {
    super.onResume()
    bindAllCameraUseCases(false)
  }

  override fun onPause() {
    super.onPause()

    imageProcessor?.run {
      this.stop()
    }
  }

  public override fun onDestroy() {
    super.onDestroy()
    imageProcessor?.run {
      this.stop()
    }
  }

  private fun bindAllCameraUseCases(clicked: Boolean) {
    if (cameraProvider != null) {
      // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
      cameraProvider!!.unbindAll()
      bindPreviewUseCase()
    }
  }

  private fun bindPreviewUseCase() {
    if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
      return
    }
    if (cameraProvider == null) {
      return
    }
    if (previewUseCase != null) {
      cameraProvider!!.unbind(previewUseCase)
    }
    val builder = Preview.Builder()
    val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
    if (targetResolution != null) {
      builder.setTargetResolution(targetResolution)
    }
    previewUseCase = builder.build()
    previewUseCase!!.setSurfaceProvider(previewView!!.getSurfaceProvider())
    cameraProvider!!.bindToLifecycle(/* lifecycleOwner= */this, cameraSelector!!, previewUseCase)
  }

  private fun bindAnalysisUseCase() {
    if (cameraProvider == null) {
      return
    }
    if (analysisUseCase != null) {
      cameraProvider!!.unbind(analysisUseCase)
    }
    if (imageProcessor != null) {
      imageProcessor!!.stop()
    }
    imageProcessor = try {
      when (selectedModel) {
        MASK_V1 -> {
          Log.i(
            TAG,
            "Using Mask V1 Detector Processor"
          )
          val localClassifier = LocalModel.Builder()
            .setAssetFilePath("custom_models/mask_v1.tflite")
            .build()
          val customImageLabelerOptions =
            CustomImageLabelerOptions.Builder(localClassifier).build()
          LabelDetectorProcessor(
            this, customImageLabelerOptions
          )
        }
        MASK_V2 -> {
          Log.i(
                  TAG,
                  "Using Mask V1 Detector Processor"
          )
          val localClassifier = LocalModel.Builder()
                  .setAssetFilePath("custom_models/mask_v2.tflite")
                  .build()
          val customImageLabelerOptions =
                  CustomImageLabelerOptions.Builder(localClassifier).build()
          LabelDetectorProcessor(
                  this, customImageLabelerOptions
          )
        }
        MASK_V3 -> {
          Log.i(
                  TAG,
                  "Using Mask V1 Detector Processor"
          )
          val localClassifier = LocalModel.Builder()
                  .setAssetFilePath("custom_models/mask_v3.tflite")
                  .build()
          val customImageLabelerOptions =
                  CustomImageLabelerOptions.Builder(localClassifier).build()
          LabelDetectorProcessor(
                  this, customImageLabelerOptions
          )
        }
        else -> throw IllegalStateException("Invalid model name")
      }
    } catch (e: Exception) {
      Log.e(
        TAG,
        "Can not create image processor: $selectedModel",
        e
      )
      Toast.makeText(
        applicationContext,
        "Can not create image processor: " + e.localizedMessage,
        Toast.LENGTH_LONG
      )
        .show()
      return
    }

    val builder = ImageAnalysis.Builder()
    val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
    if (targetResolution != null) {
      builder.setTargetResolution(targetResolution)
    }
    analysisUseCase = builder.build()

    needUpdateGraphicOverlayImageSourceInfo = true

    analysisUseCase?.setAnalyzer(
      // imageProcessor.processImageProxy will use another thread to run the detection underneath,
      // thus we can just runs the analyzer itself on main thread.
      ContextCompat.getMainExecutor(this),
      ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
        if (needUpdateGraphicOverlayImageSourceInfo) {
          val isImageFlipped =
            lensFacing == CameraSelector.LENS_FACING_FRONT
          val rotationDegrees =
            imageProxy.imageInfo.rotationDegrees
          if (rotationDegrees == 0 || rotationDegrees == 180) {
            graphicOverlay!!.setImageSourceInfo(
              imageProxy.width, imageProxy.height, isImageFlipped
            )
          } else {
            graphicOverlay!!.setImageSourceInfo(
              imageProxy.height, imageProxy.width, isImageFlipped
            )
          }
          needUpdateGraphicOverlayImageSourceInfo = false
        }
        try {
          imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
          if (VERSION.SDK_INT >= VERSION_CODES.O){
            vibrateImageDetected()
          }
        } catch (e: MlKitException) {
          Log.e(
            TAG,
            "Failed to process image. Error: " + e.localizedMessage
          )
          Toast.makeText(
            applicationContext,
            e.localizedMessage,
            Toast.LENGTH_SHORT
          )
            .show()
        }
      }
    )
    cameraProvider!!.bindToLifecycle( /* lifecycleOwner= */this, cameraSelector!!, analysisUseCase)
  }

  @RequiresApi(VERSION_CODES.O)
  private fun vibrateImageDetected(){
    val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(VibrationEffect.createOneShot(300, 50))
  }

  private fun getRequiredPermissions(): Array<String?> {
    return try {
      val info = this.packageManager
              .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
      val ps = info.requestedPermissions
      if (ps != null && ps.isNotEmpty()) {
        ps
      } else {
        arrayOfNulls(0)
      }
    } catch (e: Exception) {
      arrayOfNulls(0)
    }
  }

  private fun allPermissionsGranted(): Boolean {
    for (permission in getRequiredPermissions()) {
      permission?.let {
        if (!isPermissionGranted(this, it)) {
          return false
        }
      }
    }
    return true
  }

  private fun getRuntimePermissions() {
    val allNeededPermissions = ArrayList<String>()
    for (permission in getRequiredPermissions()) {
      permission?.let {
        if (!isPermissionGranted(this, it)) {
          allNeededPermissions.add(permission)
        }
      }
    }

    if (allNeededPermissions.isNotEmpty()) {
      ActivityCompat.requestPermissions(
              this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS
      )
    }
  }

  private fun isPermissionGranted(context: Context, permission: String): Boolean {
    if (ContextCompat.checkSelfPermission(context, permission)
            == PackageManager.PERMISSION_GRANTED
    ) {
      Log.i(TAG, "Permission granted: $permission")
      return true
    }
    Log.i(TAG, "Permission NOT granted: $permission")
    return false
  }

  companion object {
    private const val TAG = "CameraXLivePreview"
    private const val PERMISSION_REQUESTS = 1
    private const val MASK_V1 = "Mask Model Version 1"
    private const val MASK_V2 = "Mask Model Version 2"
    private const val MASK_V3 = "Mask Model Version 3"

    private const val STATE_SELECTED_MODEL = "selected_model"

    private fun isPermissionGranted(
      context: Context,
      permission: String?
    ): Boolean {
      if (ContextCompat.checkSelfPermission(context, permission!!)
        == PackageManager.PERMISSION_GRANTED
      ) {
        Log.i(TAG, "Permission granted: $permission")
        return true
      }
      Log.i(TAG, "Permission NOT granted: $permission")
      return false
    }
  }
}
