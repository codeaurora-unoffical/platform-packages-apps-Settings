/*
 * Copyright (c) 2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.android.settings.wifi;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.text.BidiFormatter;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class WifiPassPointPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    private String TAG = WifiPassPointPreferenceController.class.getSimpleName();

    private static final String KEY_ENABLE_HS2_REL1 = "enable_hs2_rel1";
    private static final String IS_USER_DISABLE_HS2_REL1 = "is_user_disable_hs2_rel1";

    private final Fragment mFragment;
    private final WifiManager mWifiManager;
    private SwitchPreference mEnableHs2Rel1;

    public WifiPassPointPreferenceController(Context context, Lifecycle lifecycle,
                                             Fragment fragment, WifiManager wifiManager) {
        super(context);
        mFragment =  fragment;
        mWifiManager = wifiManager;
        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return mWifiManager.isWifiEnabled();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ENABLE_HS2_REL1;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mEnableHs2Rel1 = (SwitchPreference) screen.findPreference(KEY_ENABLE_HS2_REL1);
    }

    @Override
    public void onResume() {
        if (mContext.getResources().getBoolean(R.bool.config_wifi_hotspot2_enabled_Rel1)) {
            mFragment.getActivity().getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.WIFI_HOTSPOT2_REL1_ENABLED), false,
                    mPasspointObserver);
        }
    }

    @Override
    public void onPause() {
        if (mContext.getResources().getBoolean(R.bool.config_wifi_hotspot2_enabled_Rel1)) {
            mFragment.getActivity().getContentResolver().unregisterContentObserver(mPasspointObserver);
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }

        final SwitchPreference enablePassPoint = (SwitchPreference) preference;

        if (mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_wifi_hotspot2_enabled) &&
                mContext.getResources().getBoolean(R.bool.config_wifi_hotspot2_enabled_Rel1)) {

            enablePassPoint.setChecked(Settings.Global.getInt(
                    mFragment.getActivity().getContentResolver(),
                    Settings.Global.WIFI_HOTSPOT2_REL1_ENABLED, 0) == 1);
        }
    }

    private ContentObserver mPasspointObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {

            if (mEnableHs2Rel1 != null) {
                mEnableHs2Rel1.setChecked(Settings.Global.getInt(
                        mFragment.getActivity().getContentResolver(),
                        Settings.Global.WIFI_HOTSPOT2_REL1_ENABLED, 0) == 1);
            }
        }
    };

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return false;
        }

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_HOTSPOT2_REL1_ENABLED,
                ((SwitchPreference) preference).isChecked() ? 1 : 0);
        Settings.Global.putInt(mContext.getContentResolver(),
                IS_USER_DISABLE_HS2_REL1,
                ((SwitchPreference) preference).isChecked() ? 1 : 0);
        Intent i = new Intent("com.android.settings.action.USER_TAP_PASSPOINT");
        mFragment.getActivity().sendBroadcast(i);
        return true;
    }
}
