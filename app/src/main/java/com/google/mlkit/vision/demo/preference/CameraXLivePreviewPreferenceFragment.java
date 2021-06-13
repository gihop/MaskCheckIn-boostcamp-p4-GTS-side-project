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

package com.google.mlkit.vision.demo.preference;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build.VERSION_CODES;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import android.util.Log;
import android.util.Size;
import androidx.annotation.StringRes;
import androidx.appcompat.view.menu.ListMenuPresenter;
import androidx.camera.core.CameraSelector;
import com.google.mlkit.vision.demo.R;
import java.util.Arrays;
import java.util.List;

/** Configures CameraX live preview demo settings. */
@RequiresApi(VERSION_CODES.LOLLIPOP)
public class CameraXLivePreviewPreferenceFragment extends LivePreviewPreferenceFragment {
  @Nullable
  public static CameraCharacteristics getCameraCharacteristics(
      Context context, Integer lensFacing) {
    CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    try {
      List<String> cameraList = Arrays.asList(cameraManager.getCameraIdList());
      for (String availableCameraId : cameraList) {
        CameraCharacteristics availableCameraCharacteristics =
            cameraManager.getCameraCharacteristics(availableCameraId);
        Integer availableLensFacing =
            availableCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
        if (availableLensFacing == null) {
          continue;
        }
        if (availableLensFacing.equals(lensFacing)) {
          return availableCameraCharacteristics;
        }
      }
    } catch (CameraAccessException e) {
      // Accessing camera ID info got error
    }
    return null;
  }
}
