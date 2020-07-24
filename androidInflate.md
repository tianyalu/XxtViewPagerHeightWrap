# `Android` 之 `inflate()` 方法总结

[TOC]

## 一、引言

```java
inflater.inflate(R.layout.layout_inflate_test,null);
inflater.inflate(R.layout.layout_inflate_test, root,false);
inflater.inflate(R.layout.layout_inflate_test, root,true);
```

做`Android`这么久，经常会看到上面三个方法，只知道这是通过布局资源`id`解析`xml`文件并返回`View`用的，但具体什么时候该用哪种参数的方法，还是懵懵懂懂。  

以下从源码角度分析一下`inflate()`方法。

## 二、源码分析

### 2.1 源码跟踪

先看一下`inflate()`涉及的相关源码（以Android API 29为例）：

```java
public View inflate(@LayoutRes int resource, @Nullable ViewGroup root) {
  return inflate(resource, root, root != null);
}

public View inflate(XmlPullParser parser, @Nullable ViewGroup root) {
  return inflate(parser, root, root != null);
}

public View inflate(@LayoutRes int resource, @Nullable ViewGroup root, boolean attachToRoot) {
  final Resources res = getContext().getResources();
	//...

  View view = tryInflatePrecompiled(resource, res, root, attachToRoot);
  if (view != null) {
    return view;
  }
  XmlResourceParser parser = res.getLayout(resource);
  try {
    return inflate(parser, root, attachToRoot);
  } finally {
    parser.close();
  }
}

public View inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot) {
  synchronized (mConstructorArgs) {
    Trace.traceBegin(Trace.TRACE_TAG_VIEW, "inflate");

    final Context inflaterContext = mContext;
    final AttributeSet attrs = Xml.asAttributeSet(parser);
    Context lastContext = (Context) mConstructorArgs[0];
    mConstructorArgs[0] = inflaterContext;
    View result = root;

    try {
      advanceToRootNode(parser);
      final String name = parser.getName();
			//...
      if (TAG_MERGE.equals(name)) {
        if (root == null || !attachToRoot) {
          throw new InflateException("<merge /> can be used only with a valid "
                                     + "ViewGroup root and attachToRoot=true");
        }

        rInflate(parser, root, inflaterContext, attrs, false);
      } else {
        // Temp is the root view that was found in the xml
        final View temp = createViewFromTag(root, name, inflaterContext, attrs);

        ViewGroup.LayoutParams params = null;

        if (root != null) {
					//...
          // Create layout params that match root, if supplied
          params = root.generateLayoutParams(attrs);
          if (!attachToRoot) {
            // Set the layout params for temp if we are not
            // attaching. (If we are, we use addView, below)
            temp.setLayoutParams(params);
          }
        }
				//...
        
        // Inflate all children under temp against its context.
        rInflateChildren(parser, temp, attrs, true);
				//...

        // We are supposed to attach all the views we found (int temp)
        // to root. Do that now.
        if (root != null && attachToRoot) {
          root.addView(temp, params);
        }

        // Decide whether to return the root that was passed in or the
        // top view found in xml.
        if (root == null || !attachToRoot) {
          result = temp;
        }
      }

    } catch (XmlPullParserException e) {
      final InflateException ie = new InflateException(e.getMessage(), e);
      ie.setStackTrace(EMPTY_STACK_TRACE);
      throw ie;
    } catch (Exception e) {
      final InflateException ie = new InflateException(
        getParserStateDescription(inflaterContext, attrs)
        + ": " + e.getMessage(), e);
      ie.setStackTrace(EMPTY_STACK_TRACE);
      throw ie;
    } finally {
      // Don't retain static reference on context.
      mConstructorArgs[0] = lastContext;
      mConstructorArgs[1] = null;

      Trace.traceEnd(Trace.TRACE_TAG_VIEW);
    }

    return result;
  }
}
```

以上虽然有四个重载方法，但仔细观察，发现万剑归宗，最后的调用都指向一个方法：`inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot)`,我们只需要跟进这个方法便可。  

![image](https://github.com/tianyalu/XxtViewPagerHeightWrap/raw/master/show/android_Inflate_invoke_relation.png)  

> 从上图可以看出以上四个方法总体上可以分为两类：第一类的第一个参数为布局资源`id`；第二类第一个参数为`xml`文件解析器`XmlPullParser`。
> 第一类调用第二类的关键代码如下：  
> ```java
> XmlResourceParser parser = res.getLayout(resource);
> try {
>     return inflate(parser, root, attachToRoot);
> } finally {
>     parser.close();
> }
> ```
> 

跟进`inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot)`方法：  

```java
final Context inflaterContext = mContext;
final AttributeSet attrs = Xml.asAttributeSet(parser);
Context lastContext = (Context) mConstructorArgs[0];
mConstructorArgs[0] = inflaterContext;
View result = root;
```

这段代码主要通过`XmlPuulPaser`获取`xml`的属性集，以及初始化一些数据，把`root`参数（可能为`null`）赋值给返回结果。继续分析：  

```java
advanceToRootNode(parser); 
//       ↓（实现）
private void advanceToRootNode(XmlPullParser parser)
  throws InflateException, IOException, XmlPullParserException {
  // Look for the root node.
  int type;
  while ((type = parser.next()) != XmlPullParser.START_TAG &&
         type != XmlPullParser.END_DOCUMENT) {
    // Empty
  }

  if (type != XmlPullParser.START_TAG) {
    throw new InflateException(parser.getPositionDescription()
                               + ": No start tag found!");
  }
}
```

这里的代码含义是遍历寻找根节点，继续分析：  

```java
final String name = parser.getName();
if (TAG_MERGE.equals(name)) {
  if (root == null || !attachToRoot) {
    throw new InflateException("<merge /> can be used only with a valid "
                               + "ViewGroup root and attachToRoot=true");
  }

  rInflate(parser, root, inflaterContext, attrs, false);
} 
```

当根节点的标签是`merge`时，如果`root`为`null`或者`attachToRoot`为`false`会直接抛异常，也就是当根标签为`merge`的时候必须使用`inflater.inflate(R.layout.layout_inflate_test, root,true);`这种形式，不然会报错，你可以自己试验一下。实验结果：

![image](https://github.com/tianyalu/XxtViewPagerHeightWrap/raw/master/show/merge_tag_exception.webp)  

然后看`rInflate(parser, root, inflaterContext, attrs, false);`方法的实现：  

```java
void rInflate(XmlPullParser parser, View parent, Context context,
              AttributeSet attrs, boolean finishInflate) throws XmlPullParserException, IOException {

  final int depth = parser.getDepth();
  int type;
  boolean pendingRequestFocus = false;

  while (((type = parser.next()) != XmlPullParser.END_TAG ||
          parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

    if (type != XmlPullParser.START_TAG) {
      continue;
    }

    final String name = parser.getName();

    if (TAG_REQUEST_FOCUS.equals(name)) {
      pendingRequestFocus = true;
      consumeChildElements(parser);
    } else if (TAG_TAG.equals(name)) {
      parseViewTag(parser, parent, attrs);
    } else if (TAG_INCLUDE.equals(name)) {
      if (parser.getDepth() == 0) {
        throw new InflateException("<include /> cannot be the root element");
      }
      parseInclude(parser, context, parent, attrs);
    } else if (TAG_MERGE.equals(name)) {
      throw new InflateException("<merge /> must be the root element");
    } else {
      final View view = createViewFromTag(parent, name, context, attrs);
      final ViewGroup viewGroup = (ViewGroup) parent;
      final ViewGroup.LayoutParams params = viewGroup.generateLayoutParams(attrs);
      rInflateChildren(parser, view, attrs, true);
      viewGroup.addView(view, params);
    }
  }

  if (pendingRequestFocus) {
    parent.restoreDefaultFocus();
  }

  if (finishInflate) {
    //个人理解：这里的应该是控制递归调用结束的一个非常关键的因素（即非`ViewGroup`类型的view不会再被递归）
    parent.onFinishInflate();
  }
}
```

该方法递归遍历`xml`文件，根据标签`Tag`的名称通过`createViewFromTag(parent, name, context, attrs);`方法并利用反射创建子`View`，并添加到`ViewGroup parent`上。然后继续分析：  

```java
else {
  // Temp is the root view that was found in the xml
  final View temp = createViewFromTag(root, name, inflaterContext, attrs);

  ViewGroup.LayoutParams params = null;

  if (root != null) {
		//...
    // Create layout params that match root, if supplied
    params = root.generateLayoutParams(attrs);
    if (!attachToRoot) {
      // Set the layout params for temp if we are not
      // attaching. (If we are, we use addView, below)
      temp.setLayoutParams(params);
    }
  }
  //...

  // Inflate all children under temp against its context.
  rInflateChildren(parser, temp, attrs, true);
  //...

  // We are supposed to attach all the views we found (int temp)
  // to root. Do that now.
  if (root != null && attachToRoot) {
    root.addView(temp, params);
  }

  // Decide whether to return the root that was passed in or the
  // top view found in xml.
  if (root == null || !attachToRoot) {
    result = temp;
  }
}
```

之前的`merge`是特殊情况，常见的是`else`中的情况，首先会通过`createViewFromTag(root, name, inflaterContext, attrs);`方法生成一个叫`temp`的`root view`，下面的代码是关键：  

```java
if (root != null) {
  //...
  // Create layout params that match root, if supplied
  params = root.generateLayoutParams(attrs);
  if (!attachToRoot) {
    // Set the layout params for temp if we are not
    // attaching. (If we are, we use addView, below)
    temp.setLayoutParams(params);
  }
}
```

如果`root != null`的话，根据`root`创建相应的`LayoutParams params`；如果`attachToRoot` 为 `false`的话，就把这个布局参数设置给`temp`，再继续分析：  

```java
// Inflate all children under temp against its context.
rInflateChildren(parser, temp, attrs, true);
//...

// We are supposed to attach all the views we found (int temp)
// to root. Do that now.
if (root != null && attachToRoot) {
  root.addView(temp, params);
}

// Decide whether to return the root that was passed in or the
// top view found in xml.
if (root == null || !attachToRoot) {
  result = temp;
}
```

首先是递归解析子布局并生成相应的`View`，如果`root != null && attachToRoot`的话，就把`temp`以`root`的布局参数`params`添加到`root`布局中；如果`root == null` 或者 `attachToRoot`为`false`，则忽略`root`的布局参数信息，直接将`temp`赋值给`result`并最终返回该结果。

### 2.2 结论

引用郭神的结论：  

| resourceId | Root   | attachToRoot | 效果                                                         |
| ---------- | ------ | ------------ | ------------------------------------------------------------ |
| 存在       | null   | 设置无意义   | `attachToRoot`失去意义                                       |
| 存在       | !=null | true         | 会给加载的布局文件指定一个父布局`root`                       |
| 存在       | !=null | false        | 则会将布局文件最外层的所有layout属性进行设置，当该view被添加到父view当中时，这些layout属性会自动生效 |
| 存在       | !=null | 未设置(true) | 会给加载的布局文件指定一个父布局`root`                       |

补充说明：

> 1. 如果`root`不为`null`，布局文件最外层的`layout`关于`LayoutParams`设置的属性和其他属性都会被保留下来，`attachToRoot`设为`true`，则会给加载的布局文件的指定一个父布局，我们不需要自己再`addView`，否则会报错;  
> 2. `attachToRoot`设为`false`，需要我们自己`addView`，`root`为`null`时，被加载的布局`LayoutParams`的属性会被改变，但是其它属性例如背景颜色什么的会被保留。

本文参考：[Android inflate方法总结](https://www.jianshu.com/p/83438249ae91)  

