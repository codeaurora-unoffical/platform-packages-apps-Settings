/* Copyright (c) 2016,2018 The Linux Foundation. All rights reserved.*/
/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.network;

import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import org.codeaurora.ims.utils.QtiImsExtUtils;

public class CallSettingsPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_CALL_SETTINGS = "call_settings";
    private UserManager mUm;
    private final String ACTION_LAUNCH_CALL_SETTINGS = "org.codeaurora.CALL_SETTINGS";
    String TAG = "CallSettingsPreferenceController";

    public CallSettingsPreferenceController(Context context) {
        super(context);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
        } else{
            removePreference(screen, getPreferenceKey());
        }
    }

    @Override
    public boolean isAvailable() {
        // NOTE: Enables/Diables call settings options based on carrier.
        return QtiImsExtUtils.isCarrierOneSupported() && isPrimaryUser();
    }

    private boolean isPrimaryUser() {
        return mUm.isSystemUser();
    }
    @Override
    public String getPreferenceKey() {
        return KEY_CALL_SETTINGS;
    }
}
