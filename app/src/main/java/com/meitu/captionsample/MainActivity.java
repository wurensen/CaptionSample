package com.meitu.captionsample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private FrameLayout captionLayoutContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        captionLayoutContainer = getView(R.id.captionlayout_container);
    }

    private <T extends View> T getView(int id) {
        return (T) findViewById(id);
    }
}
