# 自适应高度的`ViewPager`承载轮播`Banner`

[TOC]

本文实现了自适应高度的`ViewPager`承载轮播`banner`。

## 一、问题起源

使用原生的`ViewPager`时，高度默认会占满全屏：  

![image](https://github.com/tianyalu/XxtViewPagerHeightWrap/raw/master/show/show1.jpg)  

如何能实现自适应高度的`ViewPager`呢？  

![image](https://github.com/tianyalu/XxtViewPagerHeightWrap/raw/master/show/show2.jpg)  

## 二、原因分析

`ViewPager`的布局`xml`文件如下：  

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:layout_width="match_parent"
    android:layout_height="120dp"
    android:background="@color/colorAccent"
    android:gravity="center">
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:id="@+id/tv"
        android:textSize="20sp"
        android:text="tag"
        android:layout_gravity="center"
        android:gravity="center"/>
</LinearLayout>
```

根布局设置高度不生效，猜想`ViewPager`的测量有问题。

查看`ViewPager$onMeasure()`源码（`Android API 29`）如下：  

```java
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
  setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
                       getDefaultSize(0, heightMeasureSpec));

  final int measuredWidth = getMeasuredWidth();
  final int maxGutterSize = measuredWidth / 10;
  mGutterSize = Math.min(maxGutterSize, mDefaultGutterSize);

  // Children are just made to fill our space.
  int childWidthSize = measuredWidth - getPaddingLeft() - getPaddingRight();
  int childHeightSize = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

  /*
   * Make sure all children have been properly measured. Decor views first.
   * Right now we cheat and make this less complicated by assuming decor
   * views won't intersect. We will pin to edges based on gravity.
   */
	int size = getChildCount();
  //...

  mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);
  mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.EXACTLY);

  // Make sure we have created all fragments that we need to have shown.
  mInLayout = true;
  populate();
  mInLayout = false;

  // Page views next.
  size = getChildCount();
  for (int i = 0; i < size; ++i) {
    final View child = getChildAt(i);
    if (child.getVisibility() != GONE) {
			//...
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      if (lp == null || !lp.isDecor) {
        final int widthSpec = MeasureSpec.makeMeasureSpec(
          (int) (childWidthSize * lp.widthFactor), MeasureSpec.EXACTLY);
        child.measure(widthSpec, mChildHeightMeasureSpec);
      }
    }
  }
}
```

从源码上看是先设置了`setMeasuredDimension()`，然后才对子`View`进行测量，所以导致子`View`根布局设置的高度不生效。

## 三、解决方案

### 3.1 方案一

在子`View`根布局下再嵌套一层根布局，此方案虽能解决问题，但无故增加了页面层级，不推荐。

### 3.2 方案二

自定义自己的`MyViewPager`继承自`ViewPager`，并重新`onMeasure()`方法，先测量一次子`View`，获得子布局最高高度，并以此计算`heightMeasureSpec`，然后调用父类的`onMeasure()`方法。

```java
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
  int height = 0;
  for (int i = 0; i < getChildCount(); i++) {
    View child = getChildAt(i);
    ViewGroup.LayoutParams lp = child.getLayoutParams();
    int childWidthSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
    int childHeightSpec = getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
    child.measure(childWidthSpec, childHeightSpec);

    int h = child.getMeasuredHeight();
    if(h > height) {
      height = h;
    }
  }
  heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
  super.onMeasure(widthMeasureSpec, heightMeasureSpec);
}
```

### 3.3 另一个问题

如果以上两种方案未生效，则需要查看`ViewPager`的适配器，注意添加子`View`时的`inflate()`方法：  

```java
@NonNull
@Override
public Object instantiateItem(@NonNull ViewGroup container, int position) {
  position = position % mImages.size();
	//View view = LayoutInflater.from(mContext).inflate(R.layout.linear_item, null); //错误
  //注意这里
  View view = LayoutInflater.from(mContext).inflate(R.layout.linear_item, container, false);  
  TextView textView = view.findViewById(R.id.tv);
  textView.setText(position + " ");
  textView.setBackgroundResource(mImages.get(position));
  container.addView(view);
  return view;
}
```

详情参考：

## 四、其它（页面切换效果）

页面切换效果代码如下：  

```java
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
```

