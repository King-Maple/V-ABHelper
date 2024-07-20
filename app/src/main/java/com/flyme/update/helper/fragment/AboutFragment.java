package com.flyme.update.helper.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.flyme.update.helper.R;
import com.flyme.update.helper.utils.AndroidInfo;
import com.flyme.update.helper.widget.TouchFeedback;


public class AboutFragment extends Fragment implements TouchFeedback.OnFeedBackListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_about, container, false);
        TouchFeedback touchFeedback = TouchFeedback.newInstance(getContext());
        //初始化标题
        AndroidInfo androidInfo = new AndroidInfo(getContext());
        inflate.<TextView>findViewById(R.id.about_app_info).setText(getContext().getString(R.string.app_name) + "  V " + androidInfo.getVerName());
        inflate.<TextView>findViewById(R.id.tv_author).setText("你好，我是King丶枫岚");
        inflate.<TextView>findViewById(R.id.tv_email).setText("i@yowal.cn");
        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(R.id.group));
        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(R.id.email));
        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(R.id.developer));
        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(R.id.xiaoqian));
        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(R.id.meizu));
        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(R.id.lumyuan));
        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(R.id.yege));
        return inflate;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.group) {
            String qqUrl = "https://www.yowal.cn";
            Intent qqIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(qqUrl));
            qqIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(qqIntent);
        } else if (id == R.id.email) {
            String[] mailto = { "i@yowal.cn" };
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            String emailBody = "";
            sendIntent.setType("message/rfc822");
            sendIntent.putExtra(Intent.EXTRA_EMAIL, mailto);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "");
            sendIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
            startActivity(sendIntent);
        } else if (id == R.id.developer) {
            Intent coolapkIntent = new Intent();
            coolapkIntent.setClassName("com.coolapk.market", "com.coolapk.market.view.AppLinkActivity");
            coolapkIntent.setAction("android.intent.action.VIEW");
            coolapkIntent.setData(Uri.parse("http://www.coolapk.com/u/1010404"));
            startActivity(coolapkIntent);
        } else if (id == R.id.xiaoqian) {
            Intent coolapkIntent = new Intent();
            coolapkIntent.setClassName("com.coolapk.market", "com.coolapk.market.view.AppLinkActivity");
            coolapkIntent.setAction("android.intent.action.VIEW");
            coolapkIntent.setData(Uri.parse("http://www.coolapk.com/u/621284"));
            startActivity(coolapkIntent);
        } else if (id == R.id.meizu) {
            Intent coolapkIntent = new Intent();
            coolapkIntent.setClassName("com.coolapk.market", "com.coolapk.market.view.AppLinkActivity");
            coolapkIntent.setAction("android.intent.action.VIEW");
            coolapkIntent.setData(Uri.parse("http://www.coolapk.com/u/1449279"));
            startActivity(coolapkIntent);
        } else if (id == R.id.lumyuan) {
            Intent coolapkIntent = new Intent();
            coolapkIntent.setClassName("com.coolapk.market", "com.coolapk.market.view.AppLinkActivity");
            coolapkIntent.setAction("android.intent.action.VIEW");
            coolapkIntent.setData(Uri.parse("http://www.coolapk.com/u/2073264"));
            startActivity(coolapkIntent);
        } else if (id == R.id.yege) {
            Intent coolapkIntent = new Intent();
            coolapkIntent.setClassName("com.coolapk.market", "com.coolapk.market.view.AppLinkActivity");
            coolapkIntent.setAction("android.intent.action.VIEW");
            coolapkIntent.setData(Uri.parse("http://www.coolapk.com/u/377020"));
            startActivity(coolapkIntent);
        }

    }

    @Override
    public void onLongClick(View view) {

    }
}
