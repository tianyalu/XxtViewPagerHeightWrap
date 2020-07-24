package com.sty.xxt.viewpager.heightwrap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class MyPagerAdapter extends PagerAdapter {
    private List<Integer> mImages;
    private Context mContext;

    public MyPagerAdapter(List<Integer> mImages, Context mContext) {
        this.mImages = mImages;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        position = position % mImages.size();
//        View view = LayoutInflater.from(mContext).inflate(R.layout.linear_item, null);
        View view = LayoutInflater.from(mContext).inflate(R.layout.linear_item, container, false);  //注意这里
        TextView textView = view.findViewById(R.id.tv);
        textView.setText(position + " ");
        textView.setBackgroundResource(mImages.get(position));
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}
