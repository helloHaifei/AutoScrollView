package com.zhf.autoscrollview;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * 配合AutoScrollView使用，滑动到边界时执行是否调用 父fling
 * 
 * TODO 获取WebView的速率还不够准确，目前是监听手指滑动时的速率，再乘以一个小数得出来的。
 *      准确的速率应该是滑动到边界时取WebView未消费完的速率。
 * 
 * @author zhangHaiFei
 *
 */
public class MyWebView extends WebView{
	
	static final String TAG = "MyWebView";
	
	private VelocityTracker mVelocityTracker = null; 
	
	private int mVelocity;//滑动WebView的速率，不能代表WebView滑动停止时的当前速率
	private final float VELOCITY_UNIT = 0.4f;//
	
	private OnScrollChangedCallback mOnScrollChangedCallback;
	 
    public MyWebView(final Context context) {
        super(context);
    }
 
    public MyWebView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }
 
    public MyWebView(final Context context, final AttributeSet attrs,
                             final int defStyle) {
        super(context, attrs, defStyle);
    }
 
    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl,
                                   final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
 
		// WebView的总高度
		float webViewContentHeight = getContentHeight() * getScale();
		// WebView的现高度
		float webViewCurrentHeight = (getHeight() + getScrollY());
		Log.i(TAG, "ContentHeight=" + webViewContentHeight
				+ "     CurrentHeight=" + webViewCurrentHeight
				+ "     getScrollY="+getScrollY());
		if ((webViewContentHeight - webViewCurrentHeight) == 0) {
			Log.i(TAG, "**WebView Scroll to Bottom **");
			int velocity = (int) (mVelocity * VELOCITY_UNIT);
			velocity = -velocity;
        	if(Math.abs(velocity) > 1 && velocity > 0){
            	if(((AutoScrollView)getParent()).isFling() == false){ // fix bug 执行2次
            		((AutoScrollView)getParent()).fling(velocity);
            	}
            	
            	Log.i(TAG, "onScroll pull to top velocity "+velocity);
        	}
		}else if(getScrollY() == 0){
			Log.i(TAG, "**WebView Scroll to Top **");
			int velocity = (int) (mVelocity * VELOCITY_UNIT);
			velocity = -velocity;
        	if(Math.abs(velocity) > 1 && velocity < 0){
            	if(((AutoScrollView)getParent()).isFling() == false){ // fix bug 执行2次
            		((AutoScrollView)getParent()).fling(velocity);
            	}
            	
            	Log.i(TAG, "onScroll pull to bottom velocity "+velocity);
        	}
		}

        if (mOnScrollChangedCallback != null) {
            mOnScrollChangedCallback.onScroll(l - oldl, t - oldt);
        }
    }
 
    public OnScrollChangedCallback getOnScrollChangedCallback() {
        return mOnScrollChangedCallback;
    }
 
    public void setOnScrollChangedCallback(
            final OnScrollChangedCallback onScrollChangedCallback) {
        mOnScrollChangedCallback = onScrollChangedCallback;
    }
 
    /**
     * Impliment in the activity/fragment/view that you want to listen to the webview
     */
    public static interface OnScrollChangedCallback {
        public void onScroll(int dx, int dy);
    }
    
    @Override
    public WebSettings getSettings() {
    	// 不要删除此方法  编译时需要
    	return super.getSettings();
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	Log.d(TAG, "onInterceptTouchEvent" + ev.getAction());
    	return super.onInterceptTouchEvent(ev);
    }
    
    @Override    
    public boolean onTouchEvent(MotionEvent event){   
    	Log.d(TAG, " onTouchEvent" + event.getAction());
        int action = event.getAction();    
        switch(action){    
        case MotionEvent.ACTION_DOWN:    
			initOrResetVelocityTracker();
			mVelocityTracker.addMovement(event);
			mVelocity = 0;
			break; 
        case MotionEvent.ACTION_MOVE:    
        	mVelocityTracker.addMovement(event);    
        	mVelocity = 0;
            break;    
        case MotionEvent.ACTION_UP: 
        	mVelocityTracker.computeCurrentVelocity(1000);
        	mVelocity = (int) mVelocityTracker.getYVelocity();
        	
        	recycleVelocityTracker();
        	break;
        case MotionEvent.ACTION_CANCEL: 
        	recycleVelocityTracker();   
            break;    
        }    
        return super.onTouchEvent(event);    
    }    
    
    /**
	 * 获取当前滑动速率
	 * @return
	 */
	public float getCurrVelocityY(){
		
		float velocity = 0;//默认
		
		try {
			Field fieldScroller = this.getClass().getSuperclass().getDeclaredField("mScroller");
			fieldScroller.setAccessible(true);
			Object scrollObj = fieldScroller.get(this);
			Method method = scrollObj.getClass().getDeclaredMethod("getCurrVelocity");
			method.setAccessible(true);
			
			velocity = Float.valueOf(method.invoke(scrollObj)+"");
				
		} catch (Exception e){
			e.printStackTrace();
		}
	
		return velocity;
	}

	private void initOrResetVelocityTracker() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		} else {
			mVelocityTracker.clear();
		}
	}

	private void initVelocityTrackerIfNotExists() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
	}

	private void recycleVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

}
