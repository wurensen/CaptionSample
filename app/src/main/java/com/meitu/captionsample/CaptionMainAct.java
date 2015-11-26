package com.meitu.captionsample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.meitu.captionlayout.CaptionInfo;
import com.meitu.captionlayout.CaptionLayout;
import com.meitu.captionlayout.FlexibleCaptionView;

public class CaptionMainAct extends Activity {

    private CaptionLayout captionLayoutContainer;
    private FlexibleCaptionView captionView1;
    private ImageView imgViewShow;
    private TextView labelExportInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_caption_main);

        initView();
    }

    private void initView() {
        captionLayoutContainer = getView(R.id.captionLayout_container);
        captionView1 = getView(R.id.captionView1);
        imgViewShow = getView(R.id.imgView_show);
        labelExportInfo = getView(R.id.label_export_info);
    }

    private <T extends View> T getView(int id) {
        return (T) findViewById(id);
    }

    public void add(View view) {
        intent2EditCaptionAct(true, null);
    }

    public void edit(View view) {
        FlexibleCaptionView captionView = captionLayoutContainer.getCurrentFocusCaptionView();
        if (captionView != null) {
            intent2EditCaptionAct(false, captionView.getText());
        }
    }

    public void export(View view) {
        FlexibleCaptionView captionView = captionLayoutContainer.getCurrentFocusCaptionView();
        if (captionView != null) {
            CaptionInfo captionInfo = captionView.getCurrentCaption();
            Log.w("", captionInfo.toString());
            imgViewShow.setImageBitmap(captionInfo.bitmap);
            labelExportInfo.setText("locationRect=" + captionInfo.locationRect.toShortString()
                    + ",degree=" + captionInfo.degree);
        } else {
            imgViewShow.setImageBitmap(null);
            labelExportInfo.setText(null);
        }
    }

    private void intent2EditCaptionAct(boolean isAdd, String caption) {
        Intent intent = new Intent(this, AddEditCaptionAct.class);
        intent.putExtra("isAdd", isAdd);
        intent.putExtra("caption", caption);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            boolean isAdd = data.getBooleanExtra("isAdd", true);
            String caption = data.getStringExtra("caption");
            float textSize = data.getIntExtra("textSize", 0);
            int textColor = data.getIntExtra("textColor", Color.BLACK);
            int typeFaceIndex = data.getIntExtra("typeFaceIndex", 0);
            int typeFaceStyleIndex = data.getIntExtra("typeFaceStyleIndex", 0);
            Typeface typeface =
                Typeface.create(AddEditCaptionAct.typefaces[typeFaceIndex],
                    AddEditCaptionAct.typefaceStyle[typeFaceStyleIndex]);
            if (isAdd) {
                addCaption(caption, textSize, textColor, typeface);
            } else {
                updateCaption(caption, textSize, textColor, typeface);
            }
        }
    }

    private void addCaption(String caption, float textSize, int textColor, Typeface typeFace) {
        FlexibleCaptionView addCaptionView = new FlexibleCaptionView(this);
        configCaptionView(addCaptionView, caption, textSize, textColor, typeFace);
        captionLayoutContainer.addCaptionView(addCaptionView);
    }

    private void updateCaption(String caption, float textSize, int textColor, Typeface typeFace) {
        FlexibleCaptionView captionView = captionLayoutContainer.getCurrentFocusCaptionView();
        if (captionView != null) {
            captionView.setText(caption);
            captionView.setTextSizeDp(textSize);
            captionView.setTextColor(textColor);
            captionView.setTextTypeFace(typeFace);
        }
    }

    private void configCaptionView(FlexibleCaptionView captionView, String caption, float textSize, int textColor,
        Typeface typeFace) {
        captionView.setText(caption);
        captionView.setTextColor(textColor);
        captionView.setTextSizePx(textSize);
        captionView.setTextTypeFace(typeFace);
        captionView.setBorderColor(Color.RED);
        captionView.setIconSizeDp(40);
        captionView.setLeftTopIcon(android.R.drawable.ic_delete);
        captionView.setRightTopIcon(android.R.drawable.checkbox_on_background);
        captionView.setRightBottomIcon(android.R.drawable.ic_menu_crop);
    }

}
