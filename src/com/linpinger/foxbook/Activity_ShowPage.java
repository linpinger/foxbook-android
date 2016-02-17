package com.linpinger.foxbook;

import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_ShowPage extends Activity {
	public static FoxMemDB oDB;

	private int foxfrom = 0 ;  // 1=DB, 2=search 
	private TextView tv ;
	private ScrollView sv;
	private int bookid = 0 ; // 翻页时使用
	private int pageid = 0 ;
	private String pagetext = "暂缺" ;
	private String pagename = "" ;
	private String pageurl = "" ;
	private float cY = 0 ; // 点击textView的坐标
	public static final String FOXSETTING = "FOXSETTING";
	float sp_fontsize = 19; // 字体大小
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	private boolean isNextChapter = true;  // 翻页/翻屏
	
	private final int IS_REFRESH = 5 ;
	
	private int SE_TYPE = 1; // 搜索引擎
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // 标题栏中添加返回图标
		getActionBar().setDisplayShowHomeEnabled(false); // 隐藏程序图标
	}		// 响应点击事件在onOptionsItemSelected的switch中加入 android.R.id.home   this.finish();
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 180000); // 设置超时时间 3分钟
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 永远亮着
		setContentView(R.layout.activity_showpage);
		
		showHomeUp();
		
		tv = (TextView) findViewById(R.id.tv_page);
		sv = (ScrollView) findViewById(R.id.scrollView1);
		
		// 获取设置，并设置字体大小
        settings = getSharedPreferences(FOXSETTING, 0);
        editor = settings.edit();
        isNextChapter = settings.getBoolean("isNextChapter", isNextChapter);
		sp_fontsize = settings.getFloat("ShowPage_FontSize", sp_fontsize);
		tv.setTextSize(sp_fontsize);

		
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
					tv.setText("　　" + sText.replace("\n", "\n　　") + "\n" + pagename);
					setTitle(pagename + " : " + pageurl );
					if ( sText.length() < 9 ) {
						tv.setText("　　啊噢，可能处理的时候出现问题了哦\n\nURL: " + pageurl + "\nPageName: " + pagename + "\nContent:" + sText );
					}
				}
			}
		};

		final Runnable down_page = new Runnable() {
			@Override
			public void run() {
				String text = "";
				switch(SE_TYPE) {
				case FoxBookLib.SE_EASOU : // 处理easou搜索书籍，返回书籍地址
					text = FoxBookLib.downhtml(pageurl, "utf-8");
					text = site_easou.json2Text(text);
					break;
				case FoxBookLib.SE_ZSSQ:
					text = FoxBookLib.downhtml(pageurl, "utf-8");
					text = site_zssq.json2Text(text);
					break;
				case FoxBookLib.SE_QREADER:
					text = site_qreader.qreader_GetContent(pageurl);
					break;
				case FoxBookLib.SE_QIDIAN_MOBILE:
					text = FoxBookLib.downhtml(pageurl, "GBK");
					text = site_qidian.qidian_getTextFromPageJS(text);
					break;
				default:
					text = FoxBookLib.updatepage(-1, pageurl, oDB) ;
				}
				Message msg = Message.obtain();
				msg.what = IS_REFRESH;
				msg.obj = text;
				handler.sendMessage(msg);
			}
		};
		
		if ( FoxBookLib.FROM_DB == foxfrom ){ // DB
			pageid =  itt.getIntExtra("chapter_id", 0);
//			pagetext = FoxDB.getOneCell("select Content from page where id = " + pageid + " and Content is not null" );
			Map<String,String> infox = oDB.getOneRow("select bookid as bid, Content as cc, Name as naa from page where id = " + pageid + " and Content is not null");
			pagetext = infox.get("cc") ;
			pagename = infox.get("naa") ;

			if ( null == pagetext  ) {
				pagetext = "本章节内容还没下载，请回到列表，更新本书或本章节" ;
			} else {
				bookid = Integer.valueOf(infox.get("bid")); // 翻页使用
			}
			tv.setText("　　" + pagetext.replace("\n", "\n　　") + "\n" + pagename);
		} 
		if ( FoxBookLib.FROM_NET == foxfrom ){ // NET
			setTitle("下载中...");
			new Thread(down_page).start();
		}

		tv.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) { // 单击滚屏
				int vy = getWindowManager().getDefaultDisplay().getHeight(); // 屏幕高度
				if ( cY <= vy / 3 ) { // 小于1/3屏 上一页
					sv.smoothScrollBy(0, 30 - sv.getMeasuredHeight());
				} else {
					sv.smoothScrollBy(0, sv.getMeasuredHeight() - 30);
				}
			}
		});
		
		tv.setOnTouchListener(new OnTouchListener(){ // 触摸事件
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
//				if ( arg1.getAction() == MotionEvent.ACTION_DOWN )
				cY = arg1.getRawY(); // 获取的坐标给click使用
//				cX = arg1.getRawX();
				return false;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.showpage, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
				case R.id.is_next_chapter:  // 翻章
					menu.getItem(i).setChecked(isNextChapter);
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
		setTitle(pageid + " : " + pp.get("name") + " : " + pp.get("url") );
		pagetext = pp.get("content");
		tv.setText("　　" + pagetext.replace("\n", "\n　　") + "\n" + pp.get("name"));
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
		setTitle(pageid + " : " + nn.get("name") + " : " + nn.get("url") );
		pagetext = nn.get("content");
		tv.setText("　　" + pagetext.replace("\n", "\n　　") + "\n" + nn.get("name"));
		
//		sv.smoothScrollTo(0, 0);
		sv.scrollTo(0, 0);
	}
	
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
		case R.id.sp_set_size_down: // 减小字体
			--sp_fontsize;
			tv.setTextSize(sp_fontsize);
			editor.putFloat("ShowPage_FontSize", sp_fontsize);
			editor.commit();
			break;
		case R.id.is_next_chapter:  // 默认翻章，取消则翻屏，和点击相同效果
			isNextChapter = ! item.isChecked();
			item.setChecked(isNextChapter);
			editor.putBoolean("isNextChapter", isNextChapter);
			editor.commit();
			if (isNextChapter) {
				foxtip("音量键现在是翻章模式");
			} else {
				foxtip("音量键现在是翻屏模式");
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public boolean onKeyDown(int keyCoder, KeyEvent event) { // 按退出键
		if ( keyCoder == KeyEvent.KEYCODE_VOLUME_UP ) {
			if ( isNextChapter ) {
				showPrevChapter(); // 上一章
			} else {
				sv.smoothScrollBy(0, 30 - sv.getMeasuredHeight());
			}
			return true;
		}
		if ( keyCoder == KeyEvent.KEYCODE_VOLUME_DOWN ) {
			if ( isNextChapter ) {
				showNextChapter() ; // 下一章
			} else {
				sv.smoothScrollBy(0, sv.getMeasuredHeight() - 30);
			}
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}
	
	// 覆盖音量键，避免发出点击声音
	public boolean onKeyUp(int keyCoder, KeyEvent event) { // 按退出键
		if ( keyCoder == KeyEvent.KEYCODE_VOLUME_UP ) {
			return true;
		}
		if ( keyCoder == KeyEvent.KEYCODE_VOLUME_DOWN ) {
			return true;
		}
		return super.onKeyUp(keyCoder, event);
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}
