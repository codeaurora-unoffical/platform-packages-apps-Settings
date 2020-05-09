/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification;

import static com.android.settings.notification.SettingPref.TYPE_SYSTEM;

import android.content.Context;
import android.provider.Settings.System;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class CallConnectedTonePreferenceController extends SettingPrefController {

    private static final String KEY_CALL_CONNECTED_TONES = "call_connected_tones";

    public CallConnectedTonePreferenceController(Context context, SettingsPreferenceFragment parent,
            Lifecycle lifecycle) {
        super(context, parent, lifecycle);

        int defaultOn =  mContext.getResources().getInteger(R.integer.
                config_default_tone_after_connected);
        mPreference = new SettingPref(
            TYPE_SYSTEM, KEY_CALL_CONNECTED_TONES, System.CALL_CONNECTED_TONE_ENABLED,
            defaultOn) {
            @Override
            public boolean isApplicable(Context context) {
                return context.getResources().getBoolean(R.bool.config_show_connect_tone_ui);
            }
        };
    }

}
