package com.linpinger.foxbook;

import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Activity_ShowPage4Eink extends Activity {
	public static FoxMemDB oDB;
	
	FoxTextView mv;
	private float cX = 0 ; // 点击View的坐标
	private float cY = 0 ; // 点击View的坐标

	
	private int foxfrom = 0 ;  // 1=DB, 2=search 
	private int bookid = 0 ; // 翻页时使用
	private int pageid = 0 ;
	private String pagetext = "暂缺" ;
	private String pagename = "" ;
	private String pageurl = "" ;
	
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	private String myBGcolor = "default" ;  // 背景:默认羊皮纸
	private boolean isMapUpKey = false; // 是否映射上翻为下翻键
	private boolean isFullScreen = false; // 下次是否全屏
	private float sp_fontsize = 18; // 字体大小
	private float lineSpaceingMultip = 1.5f ; // 行间距倍数

	private long tLastPushEinkButton ;
	private String lastTitle="";

	private final int IS_REFRESH = 5 ;
	
	private int SE_TYPE = 1; // 搜索引擎
	
	
	private class FoxTextView extends View_FoxTextView {

		public FoxTextView(Context context) {
			super(context);
		}

		@Override
		public int setPrevText() {
			if ( 0 == pageid ) {
				foxtip("亲，ID 为 0");
				return -1;
			}
			Map<String,String> pp ;
			pp = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where id < " + pageid + " and bookid = " + bookid + " and content is not null order by id desc limit 1"); // 本书内的上一章
			if ( null == pp.get("id") ) {
				pp = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where bookid < " + bookid + " and content is not null order by bookid desc, id desc limit 1"); // 上一本书的最后一章
				if ( null == pp.get("name") ) {
					foxtip("亲，没有上一页了");
					return -2;
				}
			}
			pageid = Integer.valueOf(pp.get("id"));
			bookid = Integer.valueOf(pp.get("bid"));
			lastTitle = pageid + " : " + pp.get("name") + " : " + pp.get("url");
			setTitle(lastTitle);
			pagetext = pp.get("content");
			mv.setText(pp.get("name"), "　　" + pagetext.replace("\n", "\n　　"), pageid);
			return 0;
		}

		@Override
		public int setNextText() {
			if ( 0 == pageid ) {
				foxtip("亲，ID 为 0");
				return -1 ;
			}
			Map<String,String> nn;
			nn = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where id > " + pageid + " and bookid = " + bookid + " and content is not null limit 1"); // 本书内的下一章
			if ( null == nn.get("id") ) {
				nn = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where bookid > " + bookid + " and content is not null order by bookid, id limit 1");
				if ( null == nn.get("name") ) {
					foxtip("亲，没有下一页了");
					return -2 ;
				}
			}
			
			pageid = Integer.valueOf(nn.get("id"));
			bookid = Integer.valueOf(nn.get("bid"));
			lastTitle = pageid + " : " + nn.get("name") + " : " + nn.get("url");
			setTitle(lastTitle);
			pagetext = nn.get("content");
			mv.setText(nn.get("name"), "　　" + pagetext.replace("\n", "\n　　"), pageid);
			return 0;
		}
	}
	

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isFullScreen = settings.getBoolean("isFullScreen", isFullScreen);
		if ( isFullScreen )
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

		this.setTheme(android.R.style.Theme_Holo_Light_NoActionBar); // 无ActionBar

		super.onCreate(savedInstanceState);
//		Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 180000); // 设置超时时间 3分钟
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 永远亮着
        myBGcolor = settings.getString("myBGcolor", myBGcolor);
        editor = settings.edit(); // 获取设置
		setBGcolor(myBGcolor);
		
		mv = new FoxTextView(this); // 自定义View
		setContentView(mv);
		this.registerForContextMenu(mv); // 绑定上下文菜单
		mv.setOnTouchListener(new OnTouchListener(){ // 触摸事件
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
//				if ( arg1.getAction() == MotionEvent.ACTION_DOWN )
				cX = arg1.getX(); // 获取的坐标给click使用
				cY = arg1.getY(); // getRawX  getRawY
				return false;
			}
		});
		mv.setOnClickListener(new OnClickListener(){  // 单击事件
			@Override
			public void onClick(View arg0) {
//				foxtip("c=" + cX + ":" + cY + "/" + arg0.getWidth() + ":" + arg0.getHeight() + "\n" + arg0.toString());
				int vy = arg0.getHeight();
				if ( cY <= vy / 3 ) { // 小于1/3屏高
					int vx = arg0.getWidth(); // 屏幕宽度
					if ( cX >= vx * 0.6666 ) { // 右上角1/3宽处
						showMenu();
					} else if (cX <= vx * 0.333) { // 左上角
						foxExit();
					} else {
						mv.clickPrev();
					}
				} else {
					mv.clickNext();
				}
			}
		});
		
		tLastPushEinkButton = System.currentTimeMillis();

        isMapUpKey = settings.getBoolean("isMapUpKey", isMapUpKey);
		sp_fontsize = settings.getFloat("ShowPage_FontSize", sp_fontsize);
		mv.setFontSizeSP(String.valueOf(sp_fontsize) + "f");
		
		lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
		mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");
				
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
						mv.setText("错误", "　　啊噢，可能处理的时候出现问题了哦\n\nURL: " + pageurl + "\nPageName: " + pagename + "\nContent:" + sText , 0);
					} else {
						mv.setText(pagename, "　　" + sText.replace("\n", "\n　　"), 0);
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
			mv.setText(pagename, "　　" + pagetext.replace("\n", "\n　　"), pageid);
			
		} 
		if ( SITES.FROM_NET == foxfrom ){ // NET
			setTitle("下载中...");
			new Thread(down_page).start();
		}
		
	} // oncreate 结束


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if ( v == mv ) {
			// menu.setHeaderTitle("菜单名");
			getMenuInflater().inflate(R.menu.showpage4eink, menu);
			int itemcount = menu.size();
			for ( int i=0; i< itemcount; i++){
				switch (menu.getItem(i).getItemId()) {
					case R.id.is_nextfullscreen:
						menu.getItem(i).setChecked(isFullScreen);
						break;
					case R.id.is_mapupkey:
						menu.getItem(i).setChecked(isMapUpKey);
						break;
				}
			}

		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.show_prev:
			mv.setPrevText(); // 上一章
			mv.setInfoR();
			mv.postInvalidate();
			break;
		case R.id.show_next:
			mv.setNextText() ; // 下一章
			mv.setInfoR();
			mv.postInvalidate();
			break;
		case R.id.sp_set_size_up: // 增大字体
			++sp_fontsize;
			mv.setFontSizeSP(String.valueOf(sp_fontsize) + "f");
			editor.putFloat("ShowPage_FontSize", sp_fontsize);
			editor.commit();
			mv.postInvalidate();
			break;
		case R.id.sp_set_size_down: // 减小字体
			--sp_fontsize;
			mv.setFontSizeSP(String.valueOf(sp_fontsize) + "f");
			editor.putFloat("ShowPage_FontSize", sp_fontsize);
			editor.commit();
			mv.postInvalidate();
			break;
		case R.id.userfont:
			String nowFontPath = settings.getString("selectfont", "/sdcard/fonts/foxfont.ttf") ;
			if ( ! mv.setUserFontPath(nowFontPath) )
				foxtip("字体不存在:\n" + nowFontPath);
			else
				mv.postInvalidate();
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
			lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
			lineSpaceingMultip += 0.1 ;
			mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");
			editor.putFloat("lineSpaceingMultip", lineSpaceingMultip);
			editor.commit();
			mv.postInvalidate();
			break;
		case R.id.sp_set_linespace_down:
			lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
			lineSpaceingMultip -= 0.1 ;
			mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");
			editor.putFloat("lineSpaceingMultip", lineSpaceingMultip);
			editor.commit();
			mv.postInvalidate();
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
			foxExit();
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
		return true;
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
//				if ( isCloseSmoothScroll )
					return true ;
			}
			// 2016-8-15: BOOX C67ML Carta 左右翻页健对应: KEYCODE_PAGE_UP = 92, KEYCODE_PAGE_DOWN = 93
			if ( ! isMapUpKey ) {
				if ( KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc ) {
					mv.clickPrev();
					tLastPushEinkButton = System.currentTimeMillis();
					return true;
				}
			}
			mv.clickNext();
			tLastPushEinkButton = System.currentTimeMillis();
			return true;
		}
		if ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) {
			return true;
		}
		if ( KeyEvent.KEYCODE_MENU == kc & event.getAction() == KeyEvent.ACTION_UP ) {
			this.showMenu();
		}
		return super.dispatchKeyEvent(event);
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
	private void foxExit() {
		this.finish();
	}
	private void showMenu() {
		this.openContextMenu(mv);
	}
	private void setBGcolor(String bgcolor) {
		myBGcolor = bgcolor ;
		editor.putString("myBGcolor", myBGcolor);
		editor.commit();
		
		if ( myBGcolor.equalsIgnoreCase("white") )
			getWindow().setBackgroundDrawableResource(R.color.qd_mapp_bg_white); // 白色背景
		if ( myBGcolor.equalsIgnoreCase("green") )
			getWindow().setBackgroundDrawableResource(R.color.qd_mapp_bg_green); // 绿色
		if ( myBGcolor.equalsIgnoreCase("default") )
			getWindow().setBackgroundDrawableResource(R.drawable.parchment_paper); // 绘制两次
		if ( myBGcolor.equalsIgnoreCase("gray") )
			getWindow().setBackgroundDrawableResource(R.color.qd_mapp_bg_grey); // 灰色
	}
	
}
