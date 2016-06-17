
package com.zhf.autoscrollview;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.ListView;

/**
 * 配合AutoScrollView使用，滑动到边界时执行是否调用 父fling
 * 
 * @author zhangHaiFei
 *
 */
public class MyListView extends ListView implements ListView.OnScrollListener{

	static final String TAG = "MyListView";
    
    private int mScrollState;
    
    public MyListView(Context context) {
		super(context);
		init(context);
	}
	
	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public MyListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private void init(Context context) {
		setOnScrollListener(this);
	}
   
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //Log.d(TAG, "onInterceptTouchEvent" + ev.getAction());
        return super.onInterceptTouchEvent(ev);
    }

    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //Log.d(TAG, "onTouchEvent" + ev.getAction());
        return super.onTouchEvent(ev);
    }
   
    
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		mScrollState = scrollState;
	}
	
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		//Log.i(TAG, "onScroll firstVisibleItem="+firstVisibleItem+"  visibleItemCount="+visibleItemCount);
		
		if( firstVisibleItem == 0 && mScrollState!=SCROLL_STATE_IDLE){
			View firstVisibleChild = getChildAt(0);
			//Log.i(TAG, "firstVisibleChild.getTop()"+firstVisibleChild.getTop());
            if (firstVisibleChild != null && firstVisibleChild.getTop() == 0) {
            	
				int velocity = (int) getCurrVelocityY();
				velocity = -velocity;
				if (Math.abs(velocity) > 1 && velocity < 0) {
					((AutoScrollView) getParent()).fling(velocity);
					Log.i(TAG, "onScroll pull to top velocity" + velocity);
				}
            	
            }
		}
	}
	
	
	/**
	 * 执行ListView的fling方法
	 * @param velocityY
	 */
	public void flingMethod( int velocityY){
		Log.i(TAG, "MyListView fling "+velocityY);
		
		if(Build.VERSION.SDK_INT >=21){
			this.fling(velocityY);
		}else{
			//通过反射调用 
			setFriction(ViewConfiguration.getScrollFriction());//调用后 mFlingRunnable 才会被初始化
			try {
				Class<?> superClass = this.getClass().getSuperclass().getSuperclass();
				Field field = superClass.getDeclaredField("mFlingRunnable");
				field.setAccessible(true);
				AbsListView superInst = (AbsListView)this;
				Object obj = field.get(superInst);
				if(obj != null){
					Method method = obj.getClass().getDeclaredMethod("start",int.class);
					method.setAccessible(true);
					method.invoke(obj, velocityY);
				}
			} catch (Exception e){
				//e.printStackTrace();
			}
		}
		
	}
	/**
	 * 获取当前滑动速率
	 * @return
	 */
	public float getCurrVelocityY(){
		
		float velocity = 0;//默认
		
		try {
			Class<?> superClass = this.getClass().getSuperclass().getSuperclass();
			Field field = superClass.getDeclaredField("mFlingRunnable");
			field.setAccessible(true);
			AbsListView superInst = (AbsListView)this;
			Object obj = field.get(superInst);
			if(obj != null){
				Field fieldScroller = obj.getClass().getDeclaredField("mScroller");
				fieldScroller.setAccessible(true);
				Object scrollObj = fieldScroller.get(obj);
				Method method = scrollObj.getClass().getDeclaredMethod("getCurrVelocity");
				method.setAccessible(true);
				
				velocity = Float.valueOf(method.invoke(scrollObj)+"");
				
			}
		} catch (Exception e){
			//e.printStackTrace();
		}
	
		return velocity;
	}
	
	public void scrollListBy(int y) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
			super.scrollListBy(y);
		}else{
			try {
				Method method  = this.getClass().getSuperclass().getSuperclass().getDeclaredMethod("trackMotionScroll", int.class, int.class);
				method.setAccessible(true);
				boolean r= Boolean.valueOf(method.invoke(this,-y,-y)+""); // 效果并不理想，在api15上测试，和执行scrollBy()效果一样
			
			} catch (Exception e) {
				//e.printStackTrace();
			} 
			
		}
	}
	
}
