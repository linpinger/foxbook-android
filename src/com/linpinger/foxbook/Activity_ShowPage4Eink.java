package com.linpinger.foxbook;

import java.io.File;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

	private String bookname = "";
	private String allpagescount = "0" ;

	SharedPreferences settings;
	SharedPreferences.Editor editor;
	private String myBGcolor = "default" ;  // 背景:默认羊皮纸
	private boolean isMapUpKey = false; // 是否映射上翻为下翻键
	private float fontsize = 36.0f; // 字体大小
	private float paddingMultip  = 0.5f ; // 页边距 = 字体大小 * paddingMultip
	private float lineSpaceingMultip = 1.5f ; // 行间距倍数
	private boolean isProcessLongOneLine = true; // 处理超长单行 > 4K

	private long tLastPushEinkButton ;

	private final int IS_REFRESH = 5 ;

	private int SE_TYPE = 1; // 搜索引擎
	
	private File ZIPFILE ;


	private class FoxTextView extends Ext_View_FoxTextView {

		public FoxTextView(Context context) {
			super(context);
		}

		private int setPrevOrNextText(boolean isNextPage) {
			String strNoMoreTip ;
			String whereStrA;
			String whereStrB;
			String addSQL = " and content is not null ";

			if ( foxfrom == SITES.FROM_ZIP )
				addSQL = "" ;

			if ( isNextPage ) {
				strNoMoreTip = "亲，没有下一页了";
				whereStrA = "id > " + pageid + " and bookid = " + bookid + addSQL + " limit 1" ;
				whereStrB = "page.bookid=book.id and bookid > " + bookid + addSQL + " order by bookid, id limit 1";
			} else {
				strNoMoreTip = "亲，没有上一页了";
				whereStrA = "id < " + pageid + " and bookid = " + bookid + addSQL + " order by id desc limit 1" ;
				whereStrB = "page.bookid=book.id and bookid < " + bookid + addSQL + " order by bookid desc, id desc limit 1";
			}

			if ( 0 == pageid ) {
				foxtip("亲，ID 为 0");
				return -1;
			}
			

			Map<String,String> pp ;
			pp = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where " + whereStrA); // 本书
			if ( null == pp.get("id") ) {
				pp = oDB.getOneRow("select page.id as id, page.bookid as bid, page.name as name, page.url as url, page.content as content, book.name as bnn from book, page where " + whereStrB);
				if ( null == pp.get("name") ) {
					foxtip(strNoMoreTip);
					return -2;
				}
				bookname = pp.get("bnn");
			}
			pageid = Integer.valueOf(pp.get("id"));
			bookid = Integer.valueOf(pp.get("bid"));
			pagename = pp.get("name");
			pagetext = pp.get("content");

			if ( foxfrom == SITES.FROM_ZIP ) {
				String html = FoxZipReader.getUtf8TextFromZip(ZIPFILE, pp.get("url"));
				if ( html.contains("\"tpc_content\"") ) {
					HashMap<String, Object> cc = ToolBookJava.getPage1024(html);
					pagetext = cc.get("content").toString();
					pagename = cc.get("title").toString();
				}
			}

			pagetext = ProcessLongOneLine(pagetext);
			mv.setText(pagename, "　　" + pagetext.replace("\n", "\n　　"), bookname + "   " + pageid + " / " + allpagescount);
			return 0;
		}

		@Override
		public int setPrevText() {
			return setPrevOrNextText(false); // 上一
		}

		@Override
		public int setNextText() {
			return setPrevOrNextText(true); // 下一
		}
	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isFullScreen", false) )
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

		this.setTheme(android.R.style.Theme_Holo_Light_NoActionBar); // 无ActionBar

		super.onCreate(savedInstanceState);
//		Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 180000); // 设置超时时间 3分钟
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 永远亮着
        myBGcolor = settings.getString("myBGcolor", myBGcolor);
        editor = settings.edit(); // 获取设置
		setBGcolor(myBGcolor);

		isProcessLongOneLine = settings.getBoolean("isProcessLongOneLine", isProcessLongOneLine);

		mv = new FoxTextView(this); // 自定义View
		mv.setBodyBold(settings.getBoolean("isBodyBold", false));

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

        fontsize = settings.getFloat("fontsize", (float)ToolAndroid.sp2px(this, 18.5f)); // 字体大小
		mv.setFontSize(fontsize);

		paddingMultip = settings.getFloat("paddingMultip", paddingMultip);
		mv.setPadding(String.valueOf(paddingMultip) + "f");

		lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
		mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");

		Intent itt = getIntent();
		foxfrom = itt.getIntExtra("iam", 0);       // 必需 表明数据从哪来的
		pagename = itt.getStringExtra("chapter_name");
		pageurl = itt.getStringExtra("chapter_url");
		SE_TYPE = itt.getIntExtra("searchengine", 1) ; // 给出搜索引擎类型

		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				String sText = (String)msg.obj;
				if ( msg.what == IS_REFRESH ) {
					if ( sText.length() < 9 ) {
						mv.setText("错误", "　　啊噢，可能处理的时候出现问题了哦\n\nURL: " + pageurl + "\nPageName: " + pagename + "\nContent:" + sText);
					} else {
						pagetext = ProcessLongOneLine(pagetext);
						mv.setText(pagename, "　　" + sText.replace("\n", "\n　　"));
					}
					mv.postInvalidate();
				}
			}
		};

		final Runnable down_page = new Runnable() {
			@Override
			public void run() {
				String text = "";
				switch(SE_TYPE) {
				case SITES.SE_QIDIAN_MOBILE:
					text = ToolBookJava.downhtml(pageurl, "GBK");
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

		switch (foxfrom) {
		case SITES.FROM_DB: // DB
			pageid =  itt.getIntExtra("chapter_id", 0);
			Map<String,String> infox = oDB.getOneRow("select page.bookid as bid, page.Content as cc, page.Name as naa, book.name as bnn from book,page where page.bookid=book.id and page.id = " + pageid ); // + " and page.Content is not null");
			bookid = Integer.valueOf(infox.get("bid")); // 翻页使用
			pagetext = infox.get("cc") ;
			pagename = infox.get("naa") ;
			bookname = infox.get("bnn") ;

			if ( null == pagetext | pagetext.length() < 5  )
				pagetext = "本章节内容还没下载，请回到列表，更新本书或本章节" ;
			allpagescount = oDB.getOneCell("select count(id) from page");
			pagetext = ProcessLongOneLine(pagetext);
			mv.setText(pagename, "　　" + pagetext.replace("\n", "\n　　"), bookname + "   " + pageid + " / " + allpagescount);
			break;
		case SITES.FROM_NET: // NET
			new Thread(down_page).start();
			break;
		case SITES.FROM_ZIP: // ZIP文件
			pageid =  itt.getIntExtra("chapter_id", 0);
			bookid = Integer.valueOf(oDB.getOneCell("select bookid from page where id = " + pageid )); // 翻页使用
			allpagescount = oDB.getOneCell("select count(id) from page");
			
	    	Matcher mat = Pattern.compile("(?i)^zip://([^@]*?)@([^@]*)$").matcher(pageurl);
	    	String zipRelPath = "";
	    	String zipItemName = "";
	    	while (mat.find()) {
	    		zipRelPath = mat.group(1) ;
	    		zipItemName = mat.group(2) ;
	    	}
	    	ZIPFILE = new File(oDB.getDBFile().getParent() + "/" + zipRelPath);
	    	String html = FoxZipReader.getUtf8TextFromZip(ZIPFILE, zipItemName);
			if ( html.contains("\"tpc_content\"") ) {
				HashMap<String, Object> cc = ToolBookJava.getPage1024(html);
				pagetext = cc.get("content").toString();
				pagename = cc.get("title").toString();
			}
			mv.setText(pagename, "　　" + pagetext.replace("\n", "\n　　"), bookname + "   " + pageid + " / " + allpagescount);
			break;
		default:
			break;
		} // switch 结束

	} // oncreate 结束


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if ( v == mv ) { // menu.setHeaderTitle("菜单名");
			getMenuInflater().inflate(R.menu.showpage4eink, menu);
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.show_prev:
			mv.setPrevText(); // 上一章
			mv.postInvalidate();
			break;
		case R.id.show_next:
			mv.setNextText() ; // 下一章
			mv.postInvalidate();
			break;
		case R.id.sp_set_size_up: // 增大字体
			fontsize += 0.5f ;
			mv.setFontSize(fontsize);
			mv.postInvalidate();
			editor.putFloat("fontsize", fontsize);
			editor.commit();
			foxtip("字体大小: " + fontsize);
			break;
		case R.id.sp_set_size_down: // 减小字体
			fontsize -= 0.5f ;
			mv.setFontSize(fontsize);
			mv.postInvalidate();
			editor.putFloat("fontsize", fontsize);
			editor.commit();
			foxtip("字体大小: " + fontsize);
			break;
		case R.id.paddingup:  // 增大页边距
			paddingMultip += 0.1f ;
			mv.setPadding(String.valueOf(paddingMultip) + "f");
			mv.postInvalidate();
			editor.putFloat("paddingMultip", paddingMultip);
			editor.commit();
			foxtip("页边距: " + paddingMultip);
			break;
		case R.id.paddingdown:  // 减小页边距
			paddingMultip -= 0.1f ;
			mv.setPadding(String.valueOf(paddingMultip) + "f");
			mv.postInvalidate();
			editor.putFloat("paddingMultip", paddingMultip);
			editor.commit();
			foxtip("页边距: " + paddingMultip);
			break;
		case R.id.sp_set_linespace_up: // 加 行间距
			lineSpaceingMultip += 0.05f ;
			mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");
			mv.postInvalidate();
			editor.putFloat("lineSpaceingMultip", lineSpaceingMultip);
			editor.commit();
			foxtip("行间距: " + lineSpaceingMultip);
			break;
		case R.id.sp_set_linespace_down: // 减 行间距
			lineSpaceingMultip -= 0.05f ;
			mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");
			mv.postInvalidate();
			editor.putFloat("lineSpaceingMultip", lineSpaceingMultip);
			editor.commit();
			foxtip("行间距: " + lineSpaceingMultip);
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
		case R.id.setting:
			startActivity(new Intent(Activity_ShowPage4Eink.this, Activity_Setting.class));
			break;
		case R.id.userfont:
			String nowFontPath = settings.getString("selectfont", "/sdcard/fonts/foxfont.ttf") ;
			if ( ! mv.setUserFontPath(nowFontPath) )
				foxtip("字体不存在:\n" + nowFontPath);
			else
				mv.postInvalidate();
			break;
		case R.id.selectFont:
			Intent itt = new Intent(Activity_ShowPage4Eink.this, Activity_FileChooser.class);
			itt.putExtra("dir", "/sdcard/fonts/");
			startActivityForResult(itt, 9);
			break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 9:  // 响应文件选择器的选择
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				// 判断文件名后缀
				String newFont = new File(uri.getPath()).getAbsolutePath();
				String nowPATH = newFont.toLowerCase() ;
				if ( nowPATH.endsWith(".ttf") | nowPATH.endsWith(".ttc") | nowPATH.endsWith(".otf") ) {
					editor.putString("selectfont", newFont);
					editor.commit();
					foxtip(newFont);
					mv.setUserFontPath(newFont);
					mv.postInvalidate();
				} else {
					foxtip("要选择后缀为.ttf/.ttc/.otf的字体文件");
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int kc = event.getKeyCode() ;

		// 莫名其妙的按一个按钮出现两个keyCode
		if ( ( event.getAction() == KeyEvent.ACTION_UP ) & ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) ) {
			if ( System.currentTimeMillis() - tLastPushEinkButton < 1000 ) { // 莫名其妙的会多按，也是醉了
				tLastPushEinkButton = System.currentTimeMillis();
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

	private String ProcessLongOneLine(String iText) {
		if ( ! isProcessLongOneLine )
			return iText ;

//		long sTime = System.currentTimeMillis();

		int sLen = iText.length(); // 字符数
		int lineCount = 0 ; // 行数
		try {
			LineNumberReader lnr = new LineNumberReader(new StringReader(iText));
			while ( null != lnr.readLine() )
				++ lineCount;
		} catch ( Exception e ) {
			e.toString();
		}
		if ( ( sLen / lineCount ) > 200 ) { // 小于200(也许可以定到130)意味着换行符够多，处理的时候不会太卡，大于阈值200就得处理得到足够多的换行符
//			Log.e("XX", "Cal : " + sLen + " / " +  lineCount + " >= 200 ");
			iText = iText.replace("。", "。\n");
			iText = iText.replace("\n\n", "\n");
		}

//		Log.e("TT", "Time=" + ( System.currentTimeMillis() - sTime ) ); 
		return iText;
	}

}
