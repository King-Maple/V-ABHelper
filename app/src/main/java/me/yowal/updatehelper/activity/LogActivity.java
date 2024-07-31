package me.yowal.updatehelper.activity;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import me.yowal.updatehelper.databinding.ActivityLogBinding;
import me.yowal.updatehelper.databinding.ActivityMainBinding;

public class LogActivity extends BaseActivity {

    private ActivityLogBinding binding;

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
            toast("复制成功！赶快把神秘代码给作者吧！");
        } else {
            toast("复制失败！");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolBar.setTitle("恭喜你，到了异次元空间");

        //替换 ToolBar
        setSupportActionBar(binding.toolBar);

        defaultOkHandler(binding.tvLog, binding.btCopy);
    }
}
