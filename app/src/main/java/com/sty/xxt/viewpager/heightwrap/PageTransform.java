package com.sty.xxt.viewpager.heightwrap;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

public class PageTransform implements ViewPager.PageTransformer {
    private static final String TAG = PageTransform.class.getSimpleName();
    private static final float DEFAULT_MIN_ALPHA = 0.3f;
    private float mMinAlpha = DEFAULT_MIN_ALPHA;
    private static final float DEFAULT_MAX_ROTATE = 15.0f;
    private float mMaxRotate = DEFAULT_MAX_ROTATE;

    // 0~1 1~2 2~3 3~4  -左滑->  -1~0 0~1 1~2 2~3
    @Override
    public void transformPage(@NonNull View page, float position) {
        Log.d(TAG, "position(" + ((TextView)page.findViewById(R.id.tv)).getText().toString().trim() + "): " + position);
        if(position < -1) {
            //透明度
            page.setAlpha(mMinAlpha);
            //旋转
            page.setRotation(mMaxRotate * -1);
            page.setPivotX(page.getWidth());
            page.setPivotY(page.getHeight());
        }else if(position <= 1) {
            if(position < 0) {
                //position 是0到-1的变化，p1+position就是从1到0的变化
                //(p1 - mMinAlpha) * (p1 + position) 就是(p1 - mMinAlpha)到0的变化
                //再加上一个mMinAlpha，就变为1到mMinAlpha的变化
                float factor = mMinAlpha + (1 - mMinAlpha) * (1 + position);
                page.setAlpha(factor);

                page.setRotation(mMaxRotate * position);
                //position为width/2 到width 的变化
                page.setPivotX(page.getWidth() * 0.5f * (1 - position));
                page.setPivotY(page.getHeight());
            }else {
                //minAlpha到1的变化
                float factor = mMinAlpha + (1 - mMinAlpha) * (1 - position);
                page.setAlpha(factor);

                page.setRotation(mMaxRotate * position);
                page.setPivotX(page.getWidth() * 0.5f * (1 - position));
                page.setPivotY(page.getHeight());
            }
        }else {
            page.setAlpha(mMinAlpha);

            page.setRotation(mMaxRotate);
            page.setPivotX(0);
            page.setPivotY(page.getHeight());
        }
    }
}
