/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.DialogInterface;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.widget.Button;
import android.widget.EditText;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class RenameMobileNetworkDialogFragmentTest {
    @Mock
    private TelephonyManager mTelephonyMgr;
    @Mock
    private SubscriptionManager mSubscriptionMgr;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;

    private FragmentActivity mActivity;
    private RenameMobileNetworkDialogFragment mFragment;
    private int mSubscriptionId = 1234;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = spy(Robolectric.buildActivity(FragmentActivity.class).setup().get());

        when(mSubscriptionInfo.getSubscriptionId()).thenReturn(mSubscriptionId);
        when(mSubscriptionInfo.getDisplayName()).thenReturn("test");

        mFragment = spy(RenameMobileNetworkDialogFragment.newInstance(mSubscriptionId));
        doReturn(mTelephonyMgr).when(mFragment).getTelephonyManager(any());
        doReturn(mSubscriptionMgr).when(mFragment).getSubscriptionManager(any());

        final ServiceState serviceState = mock(ServiceState.class);
        when(serviceState.getOperatorAlphaLong()).thenReturn("fake carrier name");
        when(mTelephonyMgr.getServiceStateForSubscriber(mSubscriptionId)).thenReturn(serviceState);
    }

    @Test
    public void dialog_subscriptionMissing_noCrash() {
        final AlertDialog dialog = startDialog();
        final Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(negativeButton).isNotNull();
        negativeButton.performClick();
    }

    @Test
    public void dialog_cancelButtonClicked_setDisplayNameNotCalled() {
        when(mSubscriptionMgr.getActiveSubscriptionInfo(mSubscriptionId)).thenReturn(
                mSubscriptionInfo);
        final AlertDialog dialog = startDialog();
        final EditText nameView = mFragment.getNameView();
        nameView.setText("test2");

        final Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        negativeButton.performClick();

        verify(mSubscriptionMgr, never()).setDisplayName(anyString(), anyInt(), anyInt());
    }

    @Test
    public void dialog_renameButtonClicked_setDisplayNameCalled() {
        when(mSubscriptionMgr.getActiveSubscriptionInfo(mSubscriptionId)).thenReturn(
                mSubscriptionInfo);

        final AlertDialog dialog = startDialog();
        final EditText nameView = mFragment.getNameView();
        nameView.setText("test2");

        final Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.performClick();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mSubscriptionMgr).setDisplayName(captor.capture(), eq(mSubscriptionId),
                eq(SubscriptionManager.NAME_SOURCE_USER_INPUT));
        assertThat(captor.getValue()).isEqualTo("test2");
    }

    /** Helper method to start the dialog */
    private AlertDialog startDialog() {
        mFragment.show(mActivity.getSupportFragmentManager(), null);
        return ShadowAlertDialogCompat.getLatestAlertDialog();
    }
}
