package me.yowal.updatehelper.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;

import androidx.annotation.NonNull;

public class HtmlUtils {

    private static void setLinkClickable(Context context, SpannableStringBuilder clickableHtml, URLSpan urlSpan) {
        int start = clickableHtml.getSpanStart(urlSpan);
        int end = clickableHtml.getSpanEnd(urlSpan);
        int flags = clickableHtml.getSpanFlags(urlSpan);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                if (urlSpan.getURL()!= null) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.setData(Uri.parse(urlSpan.getURL()));
                    context.startActivity(intent);
                }
            }
        };
        clickableHtml.setSpan(clickableSpan, start, end, flags);
    }

    public static CharSequence toClickableHtml(Context context, String text) {
        Spanned spannedHtml = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
        SpannableStringBuilder clickableHtmlBuilder = new SpannableStringBuilder(spannedHtml);
        URLSpan[] urls = clickableHtmlBuilder.getSpans(0, spannedHtml.length(), URLSpan.class);
        for (URLSpan span : urls){
            setLinkClickable(context, clickableHtmlBuilder, span);
        }
        return clickableHtmlBuilder;
    }

}
