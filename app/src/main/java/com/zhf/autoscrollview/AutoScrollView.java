
package com.zhf.autoscrollview;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.ListView;
import android.widget.Scroller;

/**
 * 
 * 任意ViewGrop/View + WebView + ListView   可以连续滑动的一个组件
 * 
 * 使用示例参考文章最终页  
 * 
 * Scroller， VelocityTracker，多指触摸 请参考ScrollView
 * 
 * fling 实现思路：事件优先给子View,子View滑动停止时并且到达子View边界取其速率 ，如果fling>0 传给父View，父View再继续fling.
 * WebView滑动到底时，取其速率，大于0传给父ScrollView. ScrollView 继续fling ,当到达ListView完全显示的时候，终止ScrollView fling ,取ScrollView速率传递给ListView继续fling.
 * 
 * @author zhangHaifei
 *
 */
public class AutoScrollView extends ViewGroup {

	static final String TAG = "zhf";
	
    public interface OnScrollListener {
        void onScrollChanged(int l, int t, int oldl, int oldt);
    }

    private static final int DIRECT_BOTTOM = 1;
    private static final int DIRECT_TOP = -1;

    /**
     * Position of the last motion event.
     */
    private int mLastMotionY;
    private boolean mIsBeingDragged;
    private boolean mEnableScroll = true;
    
    
    private int mLastFlingY;
    private boolean mIsFling = false;
    private int FLING_DETECT_INTERVAL = 200;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    private Scroller mScroller;
    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    
    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;
    
    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;

    private OnScrollListener mOnScrollListener;

    public View mHeaderview;
    public MyWebView mWebView;
    public MyListView mListView;
    
    public AutoScrollView(Context context) {
        super(context);
        init();
    }

    public AutoScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOverScrollMode(OVER_SCROLL_NEVER);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        mScroller = new Scroller(getContext());

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        
        mHeaderview = findViewById(R.id.headerview);
        mWebView = (MyWebView) findViewById(R.id.webview);
        mListView = (MyListView) findViewById(R.id.listview);
        
    }

    public void setOnScrollListener(OnScrollListener listener) {
        mOnScrollListener = listener;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	
    	Log.i(TAG, "onLayout  changed="+changed +"   left="+left+"    right="+right +"   top="+top +"   bottom="+bottom);
    	
        final int count = getChildCount();
        final int parentLeft = getPaddingLeft();
        final int parentTop = getPaddingTop();

        int lastBottom = parentTop;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft = parentLeft + lp.leftMargin;
                int childTop = lastBottom + lp.topMargin;
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
                lastBottom = childTop + height + lp.bottomMargin;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	Log.i(TAG, "onMeasure   wSpec="+widthMeasureSpec +"    hSpec="+heightMeasureSpec);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if(child instanceof WebView || child instanceof ListView){
            	measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            }else{
            	
            	measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollChanged(l, t, oldl, oldt);
        }
        super.onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    public void computeScroll() {
        if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            
            int velocity = (int)mScroller.getCurrVelocity();
            Log.i(TAG, "computeScroll oldY="+oldY + "   newY="+y+"   velocity="+velocity);
            
            int dy = adjustScrollY(y - oldY);
            
            do{
            	
            	if(!(isFling() && Math.abs(velocity) > mMinimumVelocity)){
            		//mScroller.abortAnimation();
            		break;
            	}
				if (dy == 0) {
					//mScroller.abortAnimation(); //小米2S上有BUG
					break;
				} else if (dy > 0) {// scroll to bottom
    	        	Log.i(TAG, "computeScroll =========== scroll to bottom");
    	        	if(oldY <= mWebView.getTop() && mWebView.getTop() <= y){ // 
    	        		
    	        		Log.i(TAG, "computeScroll 1111111111111111");
	            		if(!isWebViewShowCompletely() && mWebView.canScrollVertically(DIRECT_BOTTOM)){
	            			scrollToWebView();
	            			
	            			mScroller.abortAnimation();
    	    	        	mWebView.flingScroll(0, velocity / 2);
	            			Log.i(TAG, "computeScroll 22222222222");
	            		}else{
	            			scrollBy(x - oldX, dy);
	            			Log.i(TAG, "computeScroll 333333333333");
	            		}
	    	        	
    	            }else if(mListView.getVisibility() == View.VISIBLE && oldY <= mListView.getTop() && mListView.getTop() <= y ){
    	            	Log.i(TAG, "computeScroll 44444444444");
    	            	if(!isListShowCompletely() && mListView.canScrollVertically(DIRECT_BOTTOM)){
	            			scrollToListView();
	            			Log.i(TAG, "computeScroll 5555555555");
	            		}
    	            	Log.i(TAG, "computeScroll 666666666666");
	    	        	mScroller.abortAnimation();
	    	        	mListView.flingMethod(velocity / 2);
    	            }else{
    	            	Log.i(TAG, "computeScroll 77777777777");
    	            	scrollBy(x - oldX, dy);
    	            }
    	        	
    	        
				} else {// scroll to top
    	        	Log.i(TAG, "computeScroll =========== scroll to top");
    	        	if(oldY >= mWebView.getTop() && mWebView.getTop() >= y){ // 
    	        		
    	        		Log.i(TAG, "computeScroll 1111111111111111");
	            		if(!isWebViewShowCompletely() && mWebView.canScrollVertically(DIRECT_TOP)){
	            			scrollToWebView();
	            			
	            			mScroller.abortAnimation();
    	    	        	mWebView.flingScroll(0, -(velocity / 2));
	            			Log.i(TAG, "computeScroll 22222222222");
	            		}else{
	            			scrollBy(x - oldX, dy);
	            			Log.i(TAG, "computeScroll 333333333333");
	            		}
	    	        	
    	            }/*else if(oldY >= mListView.getTop() && mListView.getTop() >= y ){
    	            	Log.i(TAG, "computeScroll 44444444444");
    	            	if(!isListShowCompletely() && mListView.canScrollVertically(DIRECT_TOP)){
	            			scrollToListView();
	            			Log.i(TAG, "computeScroll 5555555555");
	            		}
    	            	Log.i(TAG, "computeScroll 666666666666");
	    	        	mScroller.abortAnimation();
	    	        	mListView.flingMethod(velocity / 2);
    	            }*/else{
    	            	Log.i(TAG, "computeScroll 77777777777");
    	            	scrollBy(x - oldX, dy);
    	            }
    	        
				}
            	
            }while(false);
            

            if (!awakenScrollBars()) {
                ViewCompat.postInvalidateOnAnimation(this);
            }

        }
        super.computeScroll();
    }

    @Override
    protected int computeVerticalScrollRange() {
    	Log.i(TAG, "computeVerticalScrollRange");
    	
    	View lastView  = getLastVisibleView();
    	if (lastView != null) {
    		return lastView.getBottom();
    	}
       
        return super.computeVerticalScrollRange();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	//Log.i("zhf", "onInterceptTouchEvent "+ev.getActionMasked());
    	
        final int childCount = getChildCount();
        if (childCount < 2) {
            return false;
        }

        View headerView = mHeaderview;
        View webview = mWebView;
        View listview = mListView;

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
        
	        case MotionEvent.ACTION_MOVE:{
	        	
	        	/*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                * Locally do absolute value. mLastMotionY is set to the y value
                * of the down event.
                */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = ev.findPointerIndex(activePointerId);
                if (pointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + activePointerId
                            + " in onInterceptTouchEvent");
                    break;
                }
	        	
	        	final int y = (int) ev.getY(pointerIndex);
                int deltaY = y - mLastMotionY;
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                	
                	 initVelocityTrackerIfNotExists();
                     mVelocityTracker.addMovement(ev);
                     
                     if (deltaY < 0) { // scroll to bottom
 						
                    	 if(!isWebViewShowCompletely() && !isListShowCompletely()){
                    		//webview listview 都没有完全显示
                    		 mIsBeingDragged = true;
                    	 }else{
                    		
                    		 if(!webview.canScrollVertically(DIRECT_BOTTOM) && !isListShowCompletely()){
                    			//webview 到底&& listview又没有完全显示
                    			 mIsBeingDragged = true; 
                    		 }
                    	 }
                    	 
                     } else { //scroll to top
                    	 if(!isWebViewShowCompletely() && !isListShowCompletely()){
                    		 mIsBeingDragged = true;
                    	 }else{
                    		 if(!webview.canScrollVertically(DIRECT_TOP) && !isListShowCompletely()){
                    			 //webviwe 到顶 && listview没有完全显示
                    			 mIsBeingDragged = true;
                    		 }
                    		 if(!listview.canScrollVertically(DIRECT_TOP) && !isWebViewShowCompletely()){
                    			//listview 到顶 && webview没有完全显示
                    			 mIsBeingDragged = true;
                    		 }
                    	 }
                     }
                     
                     mLastMotionY = (int) ev.getY();
                    
                }
                
	        	break;
	        }
            case MotionEvent.ACTION_DOWN: {
                int y = (int) ev.getY();
                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(0);
                
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);

                // 在Fling状态下点击屏幕
                mIsBeingDragged = !mScroller.isFinished();
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
            	/* Release the drag */
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
                break;
            }
        }
        
        debug_event(ev);
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
    	Log.i("zhf", "onTouchEvent "+ev.getActionMasked());
    	
    	if(!mEnableScroll){return false;}
    	
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);
        
        debugPosition();
        
        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                
            	//mIsBeingDragged = !mScroller.isFinished();
				if ((mIsBeingDragged = !mScroller.isFinished())) {
					final ViewParent parent = getParent();
					if (parent != null) {
						parent.requestDisallowInterceptTouchEvent(true);
					}
				}
				 /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                // Remember where the motion event started
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
            	
            	final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }
                
            	final int y = (int) ev.getY(activePointerIndex);
                int delta = y - mLastMotionY;
                if (!mIsBeingDragged && Math.abs(delta) > mTouchSlop) {
                    
                    mIsBeingDragged = true;
                    if (delta > 0) {
                    	delta += mTouchSlop;
                    } else {
                    	delta -= mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {
                    //int y = (int) ev.getY(0);
                    int deltaY = y - mLastMotionY ;
                    
                    //int dy = - deltaY;//位移的值
                    int dy = adjustScrollY(-delta);
                    Log.i(TAG,"onTouchEvent  dy="+dy);
                    mLastMotionY = y;
                    
					if (deltaY < 0) { // scroll to bottom
						if(getScrollY() < mWebView.getTop()){//webview还未完全显示
	                        scrollBy(0, dy);
						}else if(getScrollY() < mListView.getTop()){
							
							if(mWebView.canScrollVertically(DIRECT_BOTTOM)){
								
								if(getScrollY() != mWebView.getTop()){
									scrollToWebView();
								}
								
								Log.i(TAG,"onTouchEvent 111111111 dy="+dy);
								mWebView.scrollBy(0, dy);
							}else{
								scrollBy(0, dy);
								Log.i(TAG,"onTouchEvent 22222222 dy="+dy);
							}
						}else{
							if(mListView.canScrollVertically(DIRECT_BOTTOM)){
								
								if(getScrollY() != mListView.getTop()){
									scrollToListView();
								}
								
								/*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
	                				mListView.scrollListBy(dy);
	                			}else{
	                				mListView.scrollBy(0, dy);
	                			}*/
								mListView.scrollListBy(dy);
								
							}
						}
	
					} else { // scroll to top
						
						int scrollY = getScrollY() + getHeight();
						if(scrollY >= mListView.getBottom()){
							
							if(mListView.canScrollVertically(DIRECT_TOP)){
								
								if(getScrollY() != mListView.getTop()){
									scrollToListView();
								}
								
								/*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
	                			}else{
	                				mListView.scrollBy(0, dy);
	                			}*/
								mListView.scrollListBy(dy);
								
							}else{
								scrollBy(0, dy);
							}
						}else if(scrollY > mListView.getTop() && scrollY < mListView.getBottom()){
							scrollBy(0, dy);
						}else{
							if(mWebView.canScrollVertically(DIRECT_TOP)){
								
								if(getScrollY() != mWebView.getTop()){
									scrollToWebView();
								}
								
								mWebView.scrollBy(0, dy);
							}else{
								if(getScrollY()+dy > 0){
									scrollBy(0, dy);
									///onScrollChanged(getScrollX(), getScrollY(), getScrollX(), oldY);
								}
							}
						}
					}
                    
                    
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
            	
                if (mIsBeingDragged) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) mVelocityTracker.getYVelocity(mActivePointerId);
                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                    	boolean flag = false;
                    	if(isWebViewShowCompletely()){
                    		if(initialVelocity > 0  ){
                    			if(mWebView.canScrollVertically(DIRECT_TOP)){
	                    			mScroller.abortAnimation();
	                    			mWebView.flingScroll(0, -initialVelocity);
	                    			flag = true;
                    			}
                    		}else{
                    			if(mWebView.canScrollVertically(DIRECT_BOTTOM)){
	                    			mScroller.abortAnimation();
	                    			mWebView.flingScroll(0, -initialVelocity);
	                    			flag = true;
                    			}
                    		}
                    	}
                    	
                    	if(!flag){
                    		fling(-initialVelocity);
                    		
                    	}
                    }
                    
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged) {
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                mLastMotionY = (int) ev.getY(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
        }
        
        //debug_event(ev);
        return true;
    }
    
    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = (int) ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }
    
    /**
     * 计算scrollBy() Y值的边界
     * @param delta
     * @return
     */
    private int adjustScrollY(int delta) {
        int dy = 0;
        int distance = Math.abs(delta);
        if (delta > 0) { // Scroll To Bottom
            View lastView = getLastVisibleView();//getChildAt(getChildCount() - 1);
            
            if (lastView != null) {
                int max = lastView.getBottom() - getScrollY() - getHeight();//最后一个View的底部
                //int max = lastView.getBottom() - getScrollY();//最后一个View的底部
                dy = Math.min(max, distance);
            }
        } else if (delta < 0) { // Scroll To Top
            dy = -Math.min(distance, getScrollY());
        }
        return dy;
    }

    public void fling(int velocity) {
    	Log.i(TAG,"fling() velocity="+velocity);
    	/*int minY = 0;
    	int verticalScrollRange ;
    	
		if (getScrollY() < mWebView.getTop()) {
			minY = 0;
			verticalScrollRange = mWebView.getTop();
		} else if (getScrollY() < mListView.getTop()) {
			minY = mWebView.getTop();
			verticalScrollRange = mListView.getTop();
		} else {
			minY = mListView.getTop();
			verticalScrollRange = mListView.getBottom();

		}
    	
    	Log.i(TAG, "*************fling "+velocity +"    verticalRange="+verticalScrollRange);
    	*/
    	
        /*mScroller.fling(getScrollX(), getScrollY(), 0, velocity,
        		0, computeHorizontalScrollRange(), 
        		minY, verticalScrollRange);*/
    	
    	//采用最大值的方式，计算出的fling时间会缩短，滑动明显流畅了，需要自己处理边界
    	mScroller.fling(getScrollX(), getScrollY(), 0, velocity,
        		0, computeHorizontalScrollRange(), 
        		-Integer.MAX_VALUE, Integer.MAX_VALUE);
        ViewCompat.postInvalidateOnAnimation(this);
        
        startDetectFling();
    }

    private void endDrag() {
        mIsBeingDragged = false;
        recycleVelocityTracker();
    }
    
    private boolean touchInView(View child, MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        final int scrollY = getScrollY();
        return !(y < child.getTop() - scrollY
                || y >= child.getBottom() - scrollY
                || x < child.getLeft()
                || x >= child.getRight());
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

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        // 禁用掉此功能，当ChildView是ListView时，ListView会通过此方法禁止ParentView拦截事件，
        // 而且ListView的onTouchEvent永远返回true，结果就是，如果ListView是第二个ChildView，
        // 当ListView拉到顶后父控件无法拦截事件，这样父控件无法继续往上滚动。
        // 如果这是可接受的，打以打开这条语句。
        ///super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }
    
    public void scrollToHeader(){
    	scrollTo(0, mHeaderview.getTop());
    }
    public void scrollToWebView(){
    	scrollTo(0, mWebView.getTop());
    }
    public void scrollToListView(){
    	scrollTo(0, mListView.getTop());
    }
    
    public boolean isWebViewShowCompletely(){
    	return getScrollY() == mWebView.getTop();
    }
    
    public boolean isListShowCompletely(){
    	return getScrollY() == mListView.getTop();
    }
    
    Runnable mScrollDetector = new Runnable() {
        @Override
        public void run() {
            if (mLastFlingY == getScrollY()) {
            	mIsFling = false;//fling 停止
            } else {
                mLastFlingY = getScrollY();
                postDelayed(mScrollDetector, FLING_DETECT_INTERVAL);
            }
        }
    };
    
    private void startDetectFling() {
        mIsFling = true;
        mLastFlingY = getScrollY();
        postDelayed(mScrollDetector, FLING_DETECT_INTERVAL);
    }
    
    public boolean isFling() {
        return mIsFling;
    }
    
    public void setEnableScroll(boolean enable){
    	mEnableScroll = enable;
    }
    
    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }
    
    private void debug_event(MotionEvent ev) {
        
        final int action = ev.getAction();
        String s = "";
        if (action == MotionEvent.ACTION_DOWN) {
            s = "DOWN";
        } else if (action == MotionEvent.ACTION_MOVE) {
            s = "MOVE";
        } else if (action == MotionEvent.ACTION_UP) {
            s = "UP";
        }
        int t_y = (int) ev.getY();
        Log.v(TAG, "touch " + s + " " + t_y + " " +  " " +
                mIsBeingDragged + " " + getScrollY() + " " + getHeight());
        
    }
    
    private View getLastVisibleView(){
    	for (int i = getChildCount(); i > 0; i--) {
        	View lastView = getChildAt(i - 1);
        	if (lastView != null && lastView.getVisibility() == View.VISIBLE) {
        		return lastView;
        	}
		}
    	
    	return null;
    }
    
    void debugPosition(){
    	/*Log.i(TAG,"parent  top="+getTop()+"  bottom="+getBottom());
    	for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			Log.i(TAG,"child("+i+""+")" +"  top="+child.getTop()+"  bottom="+child.getBottom());
		}*/
    	
    }
    
}
