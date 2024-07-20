package com.flyme.update.helper.activity;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flyme.update.helper.App;
import com.flyme.update.helper.R;
import com.kongzue.dialogx.dialogs.PopTip;

public class LogActivity extends BaseActivity {

    private void defaultOkHandler(final TextView logView, View okButton) {
        logView.setText(getIntent().getStringExtra("CashStr"));
        okButton.setOnClickListener(v -> {
            String log = logView.getText().toString();
            copyLog2Author(log);
        });
    }

    private void copyLog2Author(String log) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(log);
        String str = cm.getText().toString();
        if (log.equals(str)) {
            PopTip.build().setMessage("复制成功！赶快把神秘代码给作者吧！").iconSuccess().show();
        } else {
            PopTip.build().setMessage("复制失败！").iconError().show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        LinearLayout status_bar = findViewById(R.id.status_bar);
        LinearLayout.LayoutParams Params = (LinearLayout.LayoutParams) status_bar.getLayoutParams();
        Params.height = App.StatusBarHeight;
        status_bar.setLayoutParams(Params);
        defaultOkHandler(findViewById(R.id.tv_log), findViewById(R.id.bt_copy));
    }
}
