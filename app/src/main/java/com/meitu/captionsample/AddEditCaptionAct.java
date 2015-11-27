package com.meitu.captionsample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

/**
 * 新增编辑字幕
 * Created by meitu on 2015/11/26.
 */
public class AddEditCaptionAct extends Activity {

    public static Typeface[] typefaces = new Typeface[] {Typeface.DEFAULT, Typeface.SANS_SERIF, Typeface.SERIF,
        Typeface.MONOSPACE};
    public static int[] typefaceStyle = new int[] {Typeface.NORMAL, Typeface.BOLD, Typeface.ITALIC,
        Typeface.BOLD_ITALIC};

    public static EditText ediTxtCaption;
    private RadioGroup rdoGroupColor, rdoGroupTypeFace, rdoGroupStyle;

    boolean isAdd;
    float textSize;
    int textColor = Color.BLACK;
    int typeFaceIndex = 0;
    int typeFaceStyleIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_add_edit_caption);
        initView();
        registerMonitor();
        initData();
    }

    private void initData() {
        isAdd = getIntent().getBooleanExtra("isAdd", true);
        if (!isAdd) {
            String caption = getIntent().getStringExtra("caption");
            ediTxtCaption.setText(caption);
            ediTxtCaption.setSelection(caption.length());
        }
    }

    private void initView() {
        ediTxtCaption = getView(R.id.edit_caption);
        rdoGroupColor = getView(R.id.rdo_group_color);
        rdoGroupTypeFace = getView(R.id.rdo_group_typeface);
        rdoGroupStyle = getView(R.id.rdo_group_style);
    }

    private void registerMonitor() {
        rdoGroupColor.setOnCheckedChangeListener(checkedChangeListener);
        rdoGroupTypeFace.setOnCheckedChangeListener(checkedChangeListener);
        rdoGroupStyle.setOnCheckedChangeListener(checkedChangeListener);
    }

    private RadioGroup.OnCheckedChangeListener checkedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (group.getId()) {
                case R.id.rdo_group_color:
                    setCheckedColor(checkedId);
                    break;
                case R.id.rdo_group_typeface:
                    setCheckedTypeFace(checkedId);
                    break;
                case R.id.rdo_group_style:
                    setCheckedTypeFaceStyle(checkedId);
                    break;
            }
        }
    };

    public void setCheckedColor(int checkedId) {
        switch (checkedId) {
            case R.id.rdo_btn_color_black:
                textColor = Color.BLACK;
                break;
            case R.id.rdo_btn_color_blue:
                textColor = Color.BLUE;
                break;
            case R.id.rdo_btn_color_red:
                textColor = Color.RED;
                break;
            case R.id.rdo_btn_color_yellow:
                textColor = Color.YELLOW;
                break;
        }
        ediTxtCaption.setTextColor(textColor);
    }

    public void setCheckedTypeFace(int checkedId) {
        switch (checkedId) {
            case R.id.rdo_btn_typeface_normal:
                typeFaceIndex = 0;
                break;
            case R.id.rdo_btn_typeface_sans:
                typeFaceIndex = 1;
                break;
            case R.id.rdo_btn_typeface_serif:
                typeFaceIndex = 2;
                break;
            case R.id.rdo_btn_typeface_monospace:
                typeFaceIndex = 3;
                break;
        }
        ediTxtCaption.setTypeface(typefaces[typeFaceIndex], typefaceStyle[typeFaceStyleIndex]);
    }

    public void setCheckedTypeFaceStyle(int checkedId) {
        switch (checkedId) {
            case R.id.rdo_btn_style_normal:
                typeFaceStyleIndex = 0;
                break;
            case R.id.rdo_btn_style_bold:
                typeFaceStyleIndex = 1;
                break;
            case R.id.rdo_btn_style_italic:
                typeFaceStyleIndex = 2;
                break;
            case R.id.rdo_btn_style_bold_italic:
                typeFaceStyleIndex = 3;
                break;
        }
        ediTxtCaption.setTypeface(typefaces[typeFaceIndex], typefaceStyle[typeFaceStyleIndex]);
    }

    public void finish(View view) {
        CharSequence caption = ediTxtCaption.getText().toString();
        if (!TextUtils.isEmpty(caption)) {
            textSize = ediTxtCaption.getTextSize();
            Intent data = new Intent();
            data.putExtra("", ediTxtCaption.getText());
            data.putExtra("isAdd", isAdd);
            data.putExtra("caption", caption);
            data.putExtra("textSize", textSize);
            data.putExtra("textColor", textColor);
            data.putExtra("typeFaceIndex", typeFaceIndex);
            data.putExtra("typeFaceStyleIndex", typeFaceStyleIndex);
            setResult(RESULT_OK, data);
            finish();
        }
    }

    private <T extends View> T getView(int id) {
        return (T) findViewById(id);
    }
}
