package com.ifttt.location;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;
import com.ifttt.connect.R;
import com.ifttt.connect.ui.BaseConnectButtonTest;
import com.ifttt.connect.ui.ConnectButton;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public final class TestActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Configuration config = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(
                this, config);

        ConnectButton button = new ConnectButton(this);
        button.setId(R.id.ifttt_connect_button_test);
        button.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        setContentView(button);
    }
}
