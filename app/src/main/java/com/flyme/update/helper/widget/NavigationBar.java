package com.flyme.update.helper.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.flyme.update.helper.R;
import com.flyme.update.helper.utils.AndroidInfo;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class NavigationBar extends LinearLayout{

    private static ViewPager pager;
    private static String[] itemTitles;
    private static int[] itemIds;
    private static List<TabLayout.Tab> tabs = new ArrayList<>();
    private OnClickPositionListener positionListener;

    public NavigationBar(Context context) {
        this(context, null);
    }

    public NavigationBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavigationBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        positionListener = new OnClickPositionListener() {
            @Override
            public void onChanged(View view, int position) {

            }
        };
    }

    public void bindData(ViewPager viewPager, String[] titles, int[] ids) {
        pager = viewPager;
        itemIds = ids;
        itemTitles = titles;
        init(getContext());
    }

    public void bindData(String[] titles, int[] ids) {
        itemIds = ids;
        itemTitles = titles;
        init(getContext());
    }

    private TabLayout tabLayout;
    private void init(Context context){
        setOrientation(VERTICAL);
        View rootView = View.inflate(context, R.layout.fragment_navigation, null);
        tabLayout = rootView.findViewById(R.id.tabLayout);
        LinearLayout box = rootView.findViewById(R.id.navigation_box);
        if (pager != null) tabLayout.setupWithViewPager(pager);
        for (int i = 0; i < itemTitles.length; i++) {
            View item = View.inflate(getContext(), R.layout.layout_navigation_item, null);
            ImageView icon = item.findViewById(R.id.item_icon);
            TextView title = item.findViewById(R.id.item_title);
            icon.setImageResource(itemIds[i]);
            title.setText(itemTitles[i]);
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.weight = 1f;
            item.setLayoutParams(layoutParams);
            TabLayout.Tab tab = tabLayout.newTab();
            tabs.add(tab);
            tabLayout.addTab(tab);
            clickViews.add(new Item(0, item));
            int finalI = i;
            item.setOnClickListener((view)->{
                if (pager != null) pager.setCurrentItem(finalI);
                else tabLayout.selectTab(tabs.get(finalI), true);
                positionListener.onChanged(view, finalI);
            });
            if (i == 2){
                {
                    ObjectAnimator scaleX = ObjectAnimator.ofFloat(icon, "scaleX", 1.15f);
                    scaleX.setDuration(0);
                    scaleX.start();
                }
                {
                    ObjectAnimator scaleY = ObjectAnimator.ofFloat(icon, "scaleY", 1.15f);
                    scaleY.setDuration(0);
                    scaleY.start();
                }
            }
            box.addView(item);
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setCurrentItem(tab.getPosition());
                positionListener.onChanged(box.getChildAt(tab.getPosition()), tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        postDelayed(()->{
            initView();
            setCurrentItem(mPosition);
        },0L);

        addView(rootView);

        AndroidInfo androidInfo = new AndroidInfo(context);
        int width = androidInfo.width();
        int height = androidInfo.height();

        if ((16 / 9) > (height / width)) tabLayout.setVisibility(View.GONE);
    }

    private final List<Item> clickViews = new ArrayList<>();

    public void initView(){
        for (int i = 1; i < clickViews.size(); i++) {
            defaultedView(i);
        }
    }

    private int mPosition = 0;
    public void setCurrentItem(int position){
        postDelayed(()->{
            defaulted(mPosition);
            selected(position);
            mPosition = position;
        }, 0L);
    }

    private final int duration = 750;
    private void selected(int position){
        Item item = clickViews.get(position);
        View view = item.getItem();
        View box = view.findViewById(R.id.shadow_layout);
        ImageView icon = view.findViewById(R.id.item_icon);
        TextView title = view.findViewById(R.id.item_title);

        title.setTextColor(getColor(R.color.indicator_color));
        icon.setColorFilter(getColor(R.color.indicator_color));
        ObjectAnimator alpha = ObjectAnimator.ofFloat(box, "alpha", 0.8f);
        alpha.setDuration(duration);
        alpha.start();

        ObjectAnimator alpha1 = ObjectAnimator.ofFloat(title, "alpha", 1);
        alpha1.setDuration(duration);
        alpha1.start();

        {
            ObjectAnimator translationY = ObjectAnimator.ofFloat(title, "translationY", item.getTranslationX(), 0);
            translationY.setDuration(duration);
            translationY.addUpdateListener(animation -> {
                float f = (float) animation.getAnimatedValue();
                item.setTranslationX(f);
            });
            translationY.setInterpolator(new AnticipateOvershootInterpolator());
            translationY.start();
        }

        {
            ObjectAnimator translationY = ObjectAnimator.ofFloat(icon, "translationY", item.getTranslationX(), 0);
            translationY.setDuration(duration);
            translationY.addUpdateListener(animation -> {
                float f = (float) animation.getAnimatedValue();
                item.setTranslationX(f);
            });
            translationY.setInterpolator(new AnticipateOvershootInterpolator());
            translationY.start();
        }
    }

    private void defaulted(int position){
        Item item = clickViews.get(position);
        View view = item.getItem();
        View box = view.findViewById(R.id.shadow_layout);
        ImageView icon = view.findViewById(R.id.item_icon);
        TextView title = view.findViewById(R.id.item_title);

        title.setTextColor(getColor(R.color.indicator_default));
        icon.setColorFilter(getColor(R.color.indicator_default));
        ObjectAnimator alpha = ObjectAnimator.ofFloat(box, "alpha", 0);
        alpha.setDuration(duration);
        alpha.start();

        ObjectAnimator alpha1 = ObjectAnimator.ofFloat(title, "alpha", 0);
        alpha1.setDuration(duration);
        alpha1.start();

        {
            ObjectAnimator translationY = ObjectAnimator.ofFloat(title, "translationY", item.getTranslationX(), title.getHeight() * 0.5f);
            translationY.setDuration(duration);
            translationY.addUpdateListener(animation -> {
                float f = (float) animation.getAnimatedValue();
                item.setTranslationX(f);
            });
            translationY.setInterpolator(new AnticipateOvershootInterpolator());
            translationY.start();
        }

        {
            ObjectAnimator translationY = ObjectAnimator.ofFloat(icon, "translationY", item.getTranslationX(), title.getHeight() * 0.5f);
            translationY.setDuration(duration);
            translationY.addUpdateListener(animation -> {
                float f = (float) animation.getAnimatedValue();
                item.setTranslationX(f);
            });
            translationY.setInterpolator(new AnticipateOvershootInterpolator());
            translationY.start();
        }
    }

    private void defaultedView(int position){
        Item item = clickViews.get(position);
        View view = item.getItem();
        View box = view.findViewById(R.id.shadow_layout);
        ImageView icon = view.findViewById(R.id.item_icon);
        TextView title = view.findViewById(R.id.item_title);

        title.setTextColor(getColor(R.color.indicator_default));
        icon.setColorFilter(getColor(R.color.indicator_default));
        ObjectAnimator alpha = ObjectAnimator.ofFloat(box, "alpha", 0);
        alpha.setDuration(0);
        alpha.start();

        ObjectAnimator alpha1 = ObjectAnimator.ofFloat(title, "alpha", 0);
        alpha1.setDuration(0);
        alpha1.start();

        {
            ObjectAnimator translationY = ObjectAnimator.ofFloat(title, "translationY", item.getTranslationX(), title.getHeight() * 0.5f);
            translationY.setDuration(0);
            translationY.addUpdateListener(animation -> {
                float f = (float) animation.getAnimatedValue();
                item.setTranslationX(f);
            });
            translationY.setInterpolator(new AnticipateOvershootInterpolator());
            translationY.start();
        }

        {
            ObjectAnimator translationY = ObjectAnimator.ofFloat(icon, "translationY", item.getTranslationX(), title.getHeight() * 0.5f);
            translationY.setDuration(0);
            translationY.addUpdateListener(animation -> {
                float f = (float) animation.getAnimatedValue();
                item.setTranslationX(f);
            });
            translationY.setInterpolator(new AnticipateOvershootInterpolator());
            translationY.start();
        }
    }

    private int getColor(int id){
        return Color.parseColor(getContext().getString(id));
    }

    public OnClickPositionListener getPositionListener() {
        return positionListener;
    }

    public void setPositionListener(OnClickPositionListener positionListener) {
        this.positionListener = positionListener;
    }

    private static class Item {
        private float translationX;
        private View item;

        public Item(float translationX, View item) {
            this.translationX = translationX;
            this.item = item;
        }

        public float getTranslationX() {
            return translationX;
        }

        public void setTranslationX(float translationX) {
            this.translationX = translationX;
        }

        public View getItem() {
            return item;
        }

        public void setItem(View item) {
            this.item = item;
        }
    }

    public static interface OnClickPositionListener{
        void onChanged(View view, int position);
    }
}
