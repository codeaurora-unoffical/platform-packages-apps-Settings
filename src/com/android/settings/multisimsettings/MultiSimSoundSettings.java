/*
 * Copyright (c) 2012-2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *    * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *    * Neither the name of The Linux Foundation nor the names of its
 *      contributors may be used to endorse or promote products derived
 *      from this software without specific prior written permission.

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

package com.android.settings.multisimsettings;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import android.media.RingtoneManager;
import android.net.Uri;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;

import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.System;

import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.MSimConstants;
import com.android.settings.DefaultRingtonePreference;
import com.android.settings.R;

public class MultiSimSoundSettings extends PreferenceActivity {
    private String LOG_TAG = "MultiSimSoundSettings";
    private static final String KEY_RINGSTONE = "ringtone";
    private int[] mRingtones = {
            RingtoneManager.TYPE_RINGTONE, RingtoneManager.TYPE_RINGTONE_2
    };

    private DefaultRingtonePreference mRingtonePref;
    private int mSubscription;

    private Runnable mRingtoneLookupRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRingtonePref != null) {
                Context context = MultiSimSoundSettings.this;
                Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context,
                        mRingtones[mSubscription]);
                CharSequence summary = context
                        .getString(com.android.internal.R.string.ringtone_unknown);
                CharSequence ringtoneSummary;
                if (ringtoneUri == null) {
                    // silent ringtone
                    summary = context.getString(com.android.internal.R.string.ringtone_silent);
                } else {
                    // Fetch the ringtone title from the media provider
                    try {
                        Cursor cursor = context.getContentResolver().query(ringtoneUri,
                                new String[] { MediaStore.Audio.Media.TITLE }, null, null, null);
                        if (cursor != null) {
                            if (cursor.moveToFirst()) {
                                summary = cursor.getString(0);
                            }
                            cursor.close();
                        }
                    } catch (SQLiteException sqle) {
                        // Unknown title for the ringtone
                    }
                    CharSequence ringtoneUnknown = context
                            .getString(com.android.internal.R.string.ringtone_unknown);
                    if (summary.equals(ringtoneUnknown)) {
                        CharSequence defaultRingtone = resetRingtoneToDefault(context);
                        if (defaultRingtone != null) {
                            summary = defaultRingtone;
                        }
                    }
                }
                mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SUMMARY, summary));
            }
        }

        private CharSequence resetRingtoneToDefault(Context context) {
            CharSequence summary = null;
            Cursor c = null;
            try {
                String defaultRingtoneFilename = SystemProperties.get("ro.config."
                        + Settings.System.RINGTONE);
                c = context
                        .getContentResolver()
                        .acquireProvider("media")
                        .query(context.getPackageName(),
                                MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                                new String[] {
                                        MediaStore.Audio.Media._ID,
                                        MediaStore.Audio.Media.TITLE
                                },
                                MediaStore.Audio.Media.DISPLAY_NAME + "=?",
                                new String[] {
                                    defaultRingtoneFilename
                                },
                                null, null);
                if (c != null) {
                    if (c.getCount() > 0 && c.moveToFirst()) {
                        int rowId = c.getInt(0);
                        summary = c.getString(1);
                        Uri defaultRingtoneUri = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.INTERNAL_CONTENT_URI, rowId);
                        Settings.System.putString(
                                context.getContentResolver(),
                                Settings.System.RINGTONE,
                                defaultRingtoneUri.toString());
                    }
                    c.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            return summary;
        }
    };

    BroadcastReceiver mMediaScanDoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                new Thread(mRingtoneLookupRunnable).start();
            }
        }
    };

    private static final int MSG_UPDATE_SUMMARY = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_SUMMARY) {
                String summary = (String) msg.obj;
                if (mRingtonePref != null) {
                    mRingtonePref.setSummary(summary);
                }
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.multi_sim_sound_settings);
        mRingtonePref = (DefaultRingtonePreference) findPreference(KEY_RINGSTONE);
        mSubscription = this.getIntent().getIntExtra(MSimConstants.SUBSCRIPTION_KEY,
                MSimConstants.SUB1);
        mRingtonePref.setRingtoneType(mRingtones[mSubscription]);
        // Register ACTION_MEDIA_SCANNER_FINISHED intent here, to refresh the ringtone's summary.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        registerReceiver(mMediaScanDoneReceiver, intentFilter);
    }

    protected void onResume() {
        super.onResume();
        mRingtonePref.setEnabled(isSubActivated());
        new Thread(mRingtoneLookupRunnable).start();
    }

    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mMediaScanDoneReceiver);
    }

    // Determine the current card slot is available.
    private boolean isSubActivated() {
        //take sim state ready as actived state
        return  TelephonyManager.SIM_STATE_READY ==
                MSimTelephonyManager.getDefault().getSimState(mSubscription);
    }

}
