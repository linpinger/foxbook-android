package com.linpinger.foxbook;

import java.io.File;
import java.util.Map;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Activity_ShowPage extends Activity {
	public static FoxMemDB oDB;

	private int foxfrom = 0 ;  // 1=DB, 2=search 
	private TextView tv ;
	private ScrollView sv;
	private LinearLayout rootLayout ;
	private int bookid = 0 ; // 翻页时使用
	private int pageid = 0 ;
	private String pagetext = "暂缺" ;
	private String pagename = "" ;
	private String pageurl = "" ;
	private float cX = 0 ; // 点击textView的坐标
	private float cY = 0 ; // 点击textView的坐标
	
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	private boolean isWhiteActionBar = false; // 白色动作栏
	private String myBGcolor = "default" ;  // 背景:默认羊皮纸
	private boolean isMapUpKey = false; // 是否映射上翻为下翻键
	private boolean isFullScreen = false; // 下次是否全屏
	private boolean isCloseSmoothScroll = false; // 关闭平滑滚动
	private boolean isShowScrollBar = false; // 是否显示滚动条/自动隐藏
	private boolean bHideActionBar = false ;
	private float sp_fontsize = 18.5f; // 字体大小
	private float lineSpaceingMultip = 1.3f ; // 行间距倍数
	

	private long tLastPushEinkButton ;
	private String lastTitle="";

	private final int IS_REFRESH = 5 ;
	
	private int SE_TYPE = 1; // 搜索引擎
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // 标题栏中添加返回图标
		getActionBar().setDisplayShowHomeEnabled(false); // 隐藏程序图标
	}		// 响应点击事件在onOptionsItemSelected的switch中加入 android.R.id.home   this.finish();
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void changeLineSpaceing(float mu) {
		float spEx = tv.getLineSpacingExtra();
		float spMu = tv.getLineSpacingMultiplier();
		lineSpaceingMultip = spMu + mu ;
		editor.putFloat("lineSpaceingMultip", lineSpaceingMultip);
		editor.commit();
		tv.setLineSpacing(spEx, lineSpaceingMultip);
		foxtip("当前行间距: " + spEx + "  倍数: " + lineSpaceingMultip);
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void setLineSpaceing() {
		tv.setLineSpacing(tv.getLineSpacingExtra(), lineSpaceingMultip);
	}

	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isWhiteActionBar = settings.getBoolean("isWhiteActionBar", isWhiteActionBar);
		if ( isWhiteActionBar ) {
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);
		}
		
		super.onCreate(savedInstanceState);
		
		isFullScreen = settings.getBoolean("isFullScreen", isFullScreen);
		if ( isFullScreen ) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
//		Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 180000); // 设置超时时间 3分钟
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 永远亮着
		setContentView(R.layout.activity_showpage);
		
		showHomeUp();
		
		bHideActionBar = settings.getBoolean("bHideActionBar", bHideActionBar);
		if ( bHideActionBar ) {
			getActionBar().hide();
		}
		
		rootLayout = (LinearLayout) findViewById(R.id.activity_showpage_root_layout);
		tv = (TextView) findViewById(R.id.tv_page);
		sv = (ScrollView) findViewById(R.id.scrollView1);

		isShowScrollBar = settings.getBoolean("isShowScrollBar", isShowScrollBar);
		if ( isShowScrollBar ) {
			sv.setScrollbarFadingEnabled(false); // 一直显示滚动条，非自动淡出
		}

		tLastPushEinkButton = System.currentTimeMillis();

		// 获取设置，并设置字体大小
        
        editor = settings.edit();
        
        isMapUpKey = settings.getBoolean("isMapUpKey", isMapUpKey);
        isCloseSmoothScroll = settings.getBoolean("isCloseSmoothScroll", isCloseSmoothScroll);
        myBGcolor = settings.getString("myBGcolor", myBGcolor);
        setBGcolor(myBGcolor);
		sp_fontsize = settings.getFloat("ShowPage_FontSize", sp_fontsize);
		tv.setTextSize(sp_fontsize);
		
		lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
		setLineSpaceing();
				
		Intent itt = getIntent();
		foxfrom = itt.getIntExtra("iam", 0);       // 必需 表明数据从哪来的
		pagename = itt.getStringExtra("chapter_name");
		pageurl = itt.getStringExtra("chapter_url");
		SE_TYPE = itt.getIntExtra("searchengine", 1) ; // 给出搜索引擎类型

		setTitle(pagename + " : " + pageurl );

		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				String sText = (String)msg.obj;
				if ( msg.what == IS_REFRESH ) {
					setTitle(pagename + " : " + pageurl );
					if ( sText.length() < 9 ) {
						tv.setText("　　啊噢，可能处理的时候出现问题了哦\n\nURL: " + pageurl + "\nPageName: " + pagename + "\nContent:" + sText );
					} else {
						if ( bHideActionBar ) {
							tv.setText(pagename + "\n\n　　" + sText.replace("\n", "\n　　") + "\n" + pagename);
						} else {
							tv.setText("　　" + sText.replace("\n", "\n　　") + "\n" + pagename);
						}
						
					}
				}
			}
		};

		final Runnable down_page = new Runnable() {
			@Override
			public void run() {
				String text = "";
				switch(SE_TYPE) {
				case SITES.SE_QIDIAN_MOBILE:
					text = FoxBookLib.downhtml(pageurl, "GBK");
					text = site_qidian.qidian_getTextFromPageJS(text);
					break;
				default:
					text = FoxMemDBHelper.updatepage(-1, pageurl, oDB) ;
				}
				Message msg = Message.obtain();
				msg.what = IS_REFRESH;
				msg.obj = text;
				handler.sendMessage(msg);
			}
		};
		
		if ( SITES.FROM_DB == foxfrom ){ // DB
			pageid =  itt.getIntExtra("chapter_id", 0);
			Map<String,String> infox = oDB.getOneRow("select bookid as bid, Content as cc, Name as naa from page where id = " + pageid + " and Content is not null");
			pagetext = infox.get("cc") ;
			pagename = infox.get("naa") ;

			if ( null == pagetext  ) {
				pagetext = "本章节内容还没下载，请回到列表，更新本书或本章节" ;
			} else {
				bookid = Integer.valueOf(infox.get("bid")); // 翻页使用
			}
			if ( bHideActionBar ) {
				tv.setText(pagename + "\n\n　　" + pagetext.replace("\n", "\n　　") + "\n" + pagename);
			} else {
				tv.setText("　　" + pagetext.replace("\n", "\n　　") + "\n" + pagename);
			}
			
		} 
		if ( SITES.FROM_NET == foxfrom ){ // NET
			setTitle("下载中...");
			new Thread(down_page).start();
		}

		tv.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) { // 单击滚屏
				int vy = getWindowManager().getDefaultDisplay().getHeight(); // 屏幕高度
				if ( cY <= vy / 3 ) { // 小于1/3屏高
					int vx = getWindowManager().getDefaultDisplay().getWidth(); // 屏幕宽度
					if ( cX >= vx * 0.6666 ) { // 右上角1/3宽处
						hideShowActionBar();
					} else {
						clickPrev();
					}
				} else {
					clickNext();
				}
			}
		});
		
		tv.setOnTouchListener(new OnTouchListener(){ // 触摸事件
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
//				if ( arg1.getAction() == MotionEvent.ACTION_DOWN )
				cX = arg1.getRawX(); // 获取的坐标给click使用
				cY = arg1.getRawY(); // 获取的坐标给click使用
				return false;
			}
		});
		
	}

	private void hideShowActionBar() {
		bHideActionBar = ! bHideActionBar ;
		if ( bHideActionBar ) {
			getActionBar().hide();
		} else {
			if ( "" == lastTitle ) {
				lastTitle = this.getTitle().toString();
			}
			String aNow = (new java.text.SimpleDateFormat("HH:mm")).format(new java.util.Date()) ;
			this.setTitle(aNow + " " + lastTitle);
			getActionBar().show();
		}
		editor.putBoolean("bHideActionBar", bHideActionBar);
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.showpage, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
				case R.id.is_nextfullscreen:
					menu.getItem(i).setChecked(isFullScreen);
					break;
				case R.id.is_closesmoothscroll:
					menu.getItem(i).setChecked(isCloseSmoothScroll);
					break;
				case R.id.is_nextshowscrollbar:
					menu.getItem(i).setChecked(isShowScrollBar);
					break;
				case R.id.is_mapupkey:
					menu.getItem(i).setChecked(isMapUpKey);
					break;
			}
		}
		return true;
	}

	private void showPrevChapter() { // 上一章
		if ( 0 == pageid ) {
			foxtip("亲，ID 为 0");
			return ;
		}
		Map<String,String> pp ;
		pp = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where id < " + pageid + " and bookid = " + bookid + " and content is not null order by id desc limit 1"); // 本书内的上一章
		if ( null == pp.get("id") ) {
			pp = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where bookid < " + bookid + " and content is not null order by bookid desc, id limit 1");
			if ( null == pp.get("name") ) {
				foxtip("亲，没有上一页了");
				return ;
			}
		}
		pageid = Integer.valueOf(pp.get("id"));
		bookid = Integer.valueOf(pp.get("bid"));
		lastTitle = pageid + " : " + pp.get("name") + " : " + pp.get("url");
		setTitle(lastTitle);
		pagetext = pp.get("content");
		if ( bHideActionBar ) {
			tv.setText(pp.get("name") + "\n\n　　" + pagetext.replace("\n", "\n　　") + "\n" + pp.get("name"));
		} else {
			tv.setText("　　" + pagetext.replace("\n", "\n　　") + "\n" + pp.get("name"));
		}
//		sv.smoothScrollTo(0, 0);
		sv.scrollTo(0, 0);
	}
	
	private void showNextChapter() { // 下一章
		if ( 0 == pageid ) {
			foxtip("亲，ID 为 0");
			return ;
		}
		Map<String,String> nn;
		nn = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where id > " + pageid + " and bookid = " + bookid + " and content is not null limit 1"); // 本书内的下一章
		if ( null == nn.get("id") ) {
			nn = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where bookid > " + bookid + " and content is not null order by bookid, id limit 1");
			if ( null == nn.get("name") ) {
				foxtip("亲，没有下一页了");
				return ;
			}
		}
		
		pageid = Integer.valueOf(nn.get("id"));
		bookid = Integer.valueOf(nn.get("bid"));
		lastTitle = pageid + " : " + nn.get("name") + " : " + nn.get("url");
		setTitle(lastTitle);
		pagetext = nn.get("content");
		if ( bHideActionBar ) {
			tv.setText(nn.get("name") + "\n\n　　" + pagetext.replace("\n", "\n　　") + "\n" + nn.get("name"));
		} else {
			tv.setText("　　" + pagetext.replace("\n", "\n　　") + "\n" + nn.get("name"));
		}
		
//		sv.smoothScrollTo(0, 0);
		sv.scrollTo(0, 0);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case android.R.id.home: // 返回图标
			this.finish();
			break;
		case R.id.show_prev:
			showPrevChapter(); // 上一章
			break;
		case R.id.show_next:
			showNextChapter() ; // 下一章
			break;
		case R.id.sp_set_size_up: // 增大字体
			++sp_fontsize;
			tv.setTextSize(sp_fontsize);
			editor.putFloat("ShowPage_FontSize", sp_fontsize);
			editor.commit();
			break;
		case R.id.userfont:
			File font1 = new File(settings.getString("selectfont", "/sdcard/fonts/foxfont.ttf"));
			if ( font1.exists() ) {
				tv.setTypeface(Typeface.createFromFile(font1));
			} else {
				foxtip("字体不存在:\n" + font1.getAbsolutePath());
				font1.getParentFile().mkdirs();
			}
			break;
		case R.id.hideactionbar:
			hideShowActionBar();
			break;
		case R.id.bg_color1:
			setBGcolor("green");
			break;
		case R.id.bg_color2:
			setBGcolor("gray");
			break;
		case R.id.bg_color3:
			setBGcolor("white");
			break;
		case R.id.bg_color9: // 设置背景
			setBGcolor("default");
			break;
		case R.id.sp_set_linespace_up:
			changeLineSpaceing(0.05f);
			break;
		case R.id.sp_set_linespace_down:
			changeLineSpaceing(-0.05f);
			break;
			
		case R.id.sp_set_size_down: // 减小字体
			--sp_fontsize;
			tv.setTextSize(sp_fontsize);
			editor.putFloat("ShowPage_FontSize", sp_fontsize);
			editor.commit();
			break;
		case R.id.is_nextfullscreen:
			isFullScreen = ! item.isChecked();
			item.setChecked(isFullScreen);
			editor.putBoolean("isFullScreen", isFullScreen);
			editor.commit();
			if (isFullScreen) {
				foxtip("下次是全屏模式，现在退出");
			} else {
				foxtip("下次不是全屏模式，现在退出");
			}
			this.finish();
			break;
		case R.id.is_closesmoothscroll:
			isCloseSmoothScroll = ! item.isChecked();
			item.setChecked(isCloseSmoothScroll);
			editor.putBoolean("isCloseSmoothScroll", isCloseSmoothScroll);
			editor.commit();
			if (isCloseSmoothScroll) {
				foxtip("取消平滑滚动");
			} else {
				foxtip("默认平滑滚动");
			}			
			break;
		case R.id.is_nextshowscrollbar:
			isShowScrollBar = ! item.isChecked();
			item.setChecked(isShowScrollBar);
			editor.putBoolean("isShowScrollBar", isShowScrollBar);
			editor.commit();
			if (isShowScrollBar) {
				sv.setScrollbarFadingEnabled(false);
				foxtip("滚动条一直显示");
			} else {
				sv.setScrollbarFadingEnabled(true);
				foxtip("滚动条自动淡出");
			}
			break;
		case R.id.is_mapupkey:
			isMapUpKey = ! item.isChecked();
			item.setChecked(isMapUpKey);
			editor.putBoolean("isMapUpKey", isMapUpKey);
			editor.commit();
			if (isMapUpKey) {
				foxtip("现在是上翻键功能为下翻");
			} else {
				foxtip("现在恢复上翻键功能");
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void setBGcolor(String bgcolor) {
		if ( bgcolor.equalsIgnoreCase("default") ) {
			rootLayout.setBackgroundResource(R.drawable.parchment_paper);
		}
		if ( bgcolor.equalsIgnoreCase("green") ) {
			rootLayout.setBackgroundResource(R.color.qd_mapp_bg_green);
		}
		if ( bgcolor.equalsIgnoreCase("gray") ) {
			rootLayout.setBackgroundResource(R.color.qd_mapp_bg_grey);
		}
		if ( bgcolor.equalsIgnoreCase("white") ) {
			rootLayout.setBackgroundResource(R.color.qd_mapp_bg_white);
		}
		editor.putString("myBGcolor", bgcolor);
		editor.commit();
	}

	private void clickPrev() {
		if (sv.getScrollY() == 0) { // 在顶部前翻章
			showPrevChapter(); // 上一章
		} else {
			if ( isCloseSmoothScroll ) {
				sv.scrollBy(0, 30 - sv.getMeasuredHeight());
			} else {
				sv.smoothScrollBy(0, 30 - sv.getMeasuredHeight());
			}
		}
	}
	private void clickNext() {
		if (sv.getScrollY() == (tv.getHeight() - sv.getHeight())) { // 到底部后翻页
			showNextChapter() ; // 下一章
		} else {
			if ( isCloseSmoothScroll ) {
				sv.scrollBy(0, sv.getMeasuredHeight() - 30);
			} else {
				sv.smoothScrollBy(0, sv.getMeasuredHeight() - 30);
			}
		}
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int kc = event.getKeyCode() ;

// foxtip("key: " + kc + " isMapUpKey: " + isMapUpKey + "\nEvent: " + event.toString());

		// 莫名其妙的按一个按钮出现两个keyCode
		if ( ( event.getAction() == KeyEvent.ACTION_UP ) & ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) ) {
			if ( System.currentTimeMillis() - tLastPushEinkButton < 1000 ) { // 莫名其妙的会多按，也是醉了
				tLastPushEinkButton = System.currentTimeMillis();
//				foxtip("短\n\n\n\n\n\nXXXXX");
				if ( isCloseSmoothScroll )
					return true ;
			}
			// 2016-8-15: BOOX C67ML Carta 左右翻页健对应: KEYCODE_PAGE_UP = 92, KEYCODE_PAGE_DOWN = 93
			if ( ! isMapUpKey ) {
				if ( KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc ) {
					clickPrev();
					tLastPushEinkButton = System.currentTimeMillis();
					return true;
				}
			}
			clickNext();
			tLastPushEinkButton = System.currentTimeMillis();
			return true;
		}
		if ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) {
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

/*

	public boolean onKeyDown(int keyCoder, KeyEvent event) {
//		foxtip("按键D: " + keyCoder + "\n事件: " + event.toString() );
		if ( KeyEvent.KEYCODE_VOLUME_UP == keyCoder ) {
			clickPrev();
			return true;
		}
		if ( KeyEvent.KEYCODE_VOLUME_DOWN == keyCoder ) {
			clickNext();
			return true;
		}
		if ( KeyEvent.KEYCODE_PAGE_DOWN == keyCoder | KeyEvent.KEYCODE_PAGE_UP == keyCoder ) {
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}
	
	public boolean onKeyUp(int keyCoder, KeyEvent event) {
//		foxtip("按键U: " + keyCoder + "\n事件: " + event.toString() );
		if ( KeyEvent.KEYCODE_PAGE_DOWN == keyCoder | KeyEvent.KEYCODE_PAGE_UP == keyCoder ) {
			return true;
		}
		if ( KeyEvent.KEYCODE_VOLUME_UP == keyCoder | KeyEvent.KEYCODE_VOLUME_DOWN == keyCoder ) {
			return true;
		}
		return super.onKeyUp(keyCoder, event);
	}
*/

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}
