
package com.zhf.autoscrollview;

import java.util.ArrayList;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

public class WebViewAndListViewActivity extends Activity {
	
	static final String TAG ="zhf";
	
	AutoScrollView mAutoPairScrollView;
	
	RelativeLayout mHeaderLayout;
	
	
	/**
	 * 播放需要用到的View
	 */
	private View mPlayerView;

	/**
	 * 核心播放器控制对象
	 */
	//private IAutohomePlayer mAutoHomePlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_and_list_auto);

        mAutoPairScrollView = (AutoScrollView) findViewById(R.id.container);
        final WebView webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return super.shouldOverrideUrlLoading(view, url);
            }
        });
        //webView.loadUrl("http://www.angeldevil.me");
        String url = "http://m.autohome.com.cn/news/201512/883422.html#pvareaid=100260";
        url = "http://cont.app.autohome.com.cn/autov5.2.0/content/news/videopage-a2-pm2-v5.2.0-vid72894-night0-showpage1-fs0-cw1440.json";
        webView.loadUrl(url);
        // webView.loadData("Test</br>Test</br>Test</br>Test</br>Test</br>Test", "text/html", "utf-8");

        final ListView list = (ListView) findViewById(R.id.listview);
        int count = 40;
        ArrayList<String> data = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            data.add("Text " + i);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_expandable_list_item_1, data);
       /* TextView header = new TextView(this);
        header.setText("Header");
        list.addHeaderView(header);*/
        list.setAdapter(adapter);
        
        mHeaderLayout = (RelativeLayout) findViewById(R.id.headerview);
        
        
        if (null != mPlayerView) {
			// 移除播放器
        	mHeaderLayout.removeView(mPlayerView);
		}
        
        /*String videoUrl = "http://m8.play.vp.autohome.com.cn/flvs/FF91E122A113F07F/2016-01-14/D31091BB019BC2AE-100.mp4";
        mAutoHomePlayer = AutoHomePlayerFactory.getInstants().createPlayer(getApplicationContext(), PlayerType.LETV_PLAYER, videoUrl);
		mPlayerView = mAutoHomePlayer.getView();
		mHeaderLayout.addView(mPlayerView);
		mPlayerView.setBackgroundColor(Color.BLACK);
		
		mAutoHomePlayer.setVideoPath(videoUrl);
       */
        
        //mAutoPairScrollView.mListView.setVisibility(View.GONE);
        
    }
    
    public void onClickHead(View v){
    	mAutoPairScrollView.scrollToHeader();
    }
    public void onClickWebView(View v){
    	mAutoPairScrollView.scrollToWebView();
    }
    public void onClickListView(View v){
    	mAutoPairScrollView.scrollToListView();
    }
    public void onHideShowListView(View v){
    	if(mAutoPairScrollView.mListView.getVisibility() == View.VISIBLE)
    		mAutoPairScrollView.mListView.setVisibility(View.GONE);
    	else
    		mAutoPairScrollView.mListView.setVisibility(View.VISIBLE);
    }
    
   /* 
    @Override
	protected void onPause() {
		super.onPause();
		if (null != mAutoHomePlayer && mAutoHomePlayer.isPlaying()) {
			if (null != mAutoHomePlayer) {
				mAutoHomePlayer.pause();
			}
		}
	}*/
    
    
    

}
