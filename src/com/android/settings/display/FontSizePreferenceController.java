/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.os.RemoteException;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.WarnedPreference;
import com.android.settings.accessibility.ToggleFontSizePreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.display.ScreenZoomPreference;
import com.android.settingslib.core.AbstractPreferenceController;

public class FontSizePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, WarnedPreference.OnPreferenceValueChangeListener,
        WarnedPreference.OnPreferenceClickListener{

    String TAG = "FontSizePreferenceController";
    /** If there is no setting in the provider, use this. */
    public static final String KEY_IS_CHECKED = "is_checked";
    public static final String FILE_FONT_WARING = "font_waring";

    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_FONT_SIZE_MODE = "font_size_mode";

    private static final String FONT_SIZE_MINIMUM = "0.95";
    private static final String FONT_SIZE_SMALL = "1.0";
    private static final String FONT_SIZE_MEDIUM = "1.05";
    private static final String FONT_SIZE_LARGE = "1.15";
    private static final String FONT_SIZE_VERYLARGE = "1.30";

    private static final int DLG_FONTSIZE_CHANGE_WARNING = 2;

    private ScreenZoomPreference mScreenZoomPref;
    private WarnedPreference mDialogPref;

    private boolean isRJILMode;

    private Preference mFontSizePref;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    private final Configuration mCurConfig = new Configuration();
    public FontSizePreferenceController(Context context) {
        super(context);

        mSharedPreferences = mContext.getSharedPreferences(FILE_FONT_WARING,
                Activity.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();

        isRJILMode = mContext.getResources().getBoolean(R.bool.show_font_size_config);
        Log.d(TAG, "Constructor - RJIL Flag "+isRJILMode);

    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        if (isRJILMode) {
            Log.d(TAG, "displayPreference - RJIL");
            removePreference(screen, KEY_FONT_SIZE);
            mDialogPref = (WarnedPreference) screen.findPreference(KEY_FONT_SIZE_MODE);
            mDialogPref.setPreferenceValueChangeListener(this);
            mDialogPref.setOnPreferenceClickListener(this);
        } else {
            Log.d(TAG, "displayPreference - Non RJIL");
            removePreference(screen, KEY_FONT_SIZE_MODE);
            mFontSizePref = screen.findPreference(KEY_FONT_SIZE);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_FONT_SIZE;
    }

    @Override
    public void updateState(Preference preference) {
        Log.d(TAG, "updateState");
        if (isRJILMode) {
            readFontSizePreference(mDialogPref);
            Log.d(TAG, "updateState - RJIL");
            return;
        }

        final float currentScale = Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.FONT_SCALE, 1.0f);
        final Resources res = mContext.getResources();
        final String[] entries = res.getStringArray(R.array.entries_font_size);
        final String[] strEntryValues = res.getStringArray(R.array.entryvalues_font_size);
        final int index = ToggleFontSizePreferenceFragment.fontSizeValueToIndex(currentScale,
                strEntryValues);
        Log.d(TAG, "updateState - Non RJIL" +entries[index]);
        preference.setSummary(entries[index]);
    }

    Dialog fontDialog = null;
    public Dialog showDialog(int dialogId) {
        if (dialogId == DLG_FONTSIZE_CHANGE_WARNING) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            final View dialog_view = LayoutInflater.from(mContext).
                    inflate(R.layout.dialog_fontwaring, null);
            builder.setView(dialog_view);
            final CheckBox cb_showagain = (CheckBox) dialog_view.findViewById(R.id.showagain);
            TextView ok_message = (TextView) dialog_view.findViewById(R.id.ok_message);
            ok_message.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mEditor.putBoolean(KEY_IS_CHECKED, cb_showagain.isChecked());
                    mEditor.commit();
                    writeFontSizePreference(FONT_SIZE_VERYLARGE);
                    removeDialog(DLG_FONTSIZE_CHANGE_WARNING);
                    mDialogPref.waringDialogOk();
                }
            });

            return builder.create();
        }
        return null;
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            if (objValue != null) {
                mCurConfig.fontScale = Float.parseFloat(objValue.toString());
                ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);

                if (mContext == null) return;

                final ContentResolver resolver = mContext.getContentResolver();
                Settings.System.putFloat(resolver,
                        Settings.System.FONT_SCALE, Float.parseFloat(objValue.toString()));
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    protected void removeDialog(int dialogId) {
        if(dialogId == DLG_FONTSIZE_CHANGE_WARNING && fontDialog != null){
            fontDialog.dismiss();
            fontDialog = null;
        }
    }

    @Override
    public void onPreferenceClick(Preference preference) {
        if (preference == mDialogPref) {
            if (isRJILMode) {
                mDialogPref.showDialog(null);
                if (mDialogPref.getDialog() != null) {
                    mDialogPref.getDialog().show();
                }
            }
        }
    }

    public void readFontSizePreference(WarnedPreference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to retrieve font size", e);
        }
        Log.i(TAG, "readFontSizePreference : "+pref.getWarnedPreferenceSummary());
        pref.setSummary(pref.getWarnedPreferenceSummary());
    }

    @Override
    public void onPreferenceValueChange(Preference preference, Object newValue) {
        Log.w(TAG, "onPreferenceValueChange");
        final String rb_textValue = newValue.toString();
        final Resources res = mContext.getResources();
        if (res.getString(R.string.choose_font_VeryLarge).equals(rb_textValue)) {
            if (!mSharedPreferences.getBoolean(KEY_IS_CHECKED, false)) {
                if (mDialogPref.getDialog() != null && mDialogPref.getDialog().isShowing()) {
                    fontDialog = showDialog(DLG_FONTSIZE_CHANGE_WARNING);
                    fontDialog.show();
                }
            } else {
                writeFontSizePreference(FONT_SIZE_VERYLARGE);
            }
        } else if (res.getString(R.string.choose_font_Large).equals(rb_textValue)) {
            writeFontSizePreference(FONT_SIZE_LARGE);
        } else if (res.getString(R.string.choose_font_Medium).equals(rb_textValue)) {
            writeFontSizePreference(FONT_SIZE_MEDIUM);
        } else if (res.getString(R.string.choose_font_Small).equals(rb_textValue)) {
            writeFontSizePreference(FONT_SIZE_SMALL);
        } else if (res.getString(R.string.choose_font_Minimum).equals(rb_textValue)) {
            writeFontSizePreference(FONT_SIZE_MINIMUM);
        }
    }
}