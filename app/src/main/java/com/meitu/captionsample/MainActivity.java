package com.meitu.captionsample;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.meitu.captionlayout.CaptionInfo;
import com.meitu.captionlayout.CaptionLayout;
import com.meitu.captionlayout.FlexibleCaptionView;

public class MainActivity extends Activity {

    private CaptionLayout captionLayoutContainer;
    private FlexibleCaptionView captionView1;
    private ImageView imgViewShow;
    private EditText ediTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        captionLayoutContainer = getView(R.id.captionLayout_container);
        captionView1 = getView(R.id.captionView1);
        imgViewShow = getView(R.id.imgView_show);
        ediTxt = getView(R.id.ediTxt);
    }

    private void configCaptionView(FlexibleCaptionView captionView) {
        captionView.setText("通过代码添加的字幕控件");
        captionView.setTextColor(Color.RED);
        captionView.setTextSizeDp(20);
        captionView.setTextPaddingDp(35);
        captionView.setBorderColor(Color.BLUE);
        captionView.setIconSizeDp(40);
        captionView.setLeftTopIcon(android.R.drawable.ic_delete);
        captionView.setRightTopIcon(android.R.drawable.checkbox_on_background);
        captionView.setRightBottomIcon(android.R.drawable.ic_menu_crop);
    }

    private <T extends View> T getView(int id) {
        return (T) findViewById(id);
    }

    public void add(View view) {
        FlexibleCaptionView captionView = captionLayoutContainer.getCurrentFocusCaptionView();
        if (captionView != null) {
            String content = ediTxt.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                return;
            }
            captionView.setText(content);
        } else {
            FlexibleCaptionView thirdCaptionView = new FlexibleCaptionView(this);
            configCaptionView(thirdCaptionView);
            captionLayoutContainer.addCaptionView(thirdCaptionView);
        }
    }

    public void export(View view) {
        FlexibleCaptionView captionView = captionLayoutContainer.getCurrentFocusCaptionView();
        if (captionView != null) {
            CaptionInfo captionInfo = captionView.getCurrentCaption();
            Log.w("", captionInfo.toString());
            imgViewShow.setImageBitmap(captionInfo.bitmap);
        }
    }
}
