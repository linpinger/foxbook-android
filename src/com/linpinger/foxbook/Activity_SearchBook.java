package com.linpinger.foxbook;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import java.io.UnsupportedEncodingException;
import java.lang.String;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

public class Activity_SearchBook extends Activity {
	public static FoxMemDB oDB;
	private long mExitTime ;
	private WebView wv;
	private EditText et;
	private ImageButton btn_search;
	private Button btn_pre ;
	
	SharedPreferences settings;
	private boolean isWhiteActionBar = false; // 白色动作栏

	private String book_name = "";
	private String book_url = "";
	
	private final int IS_GETQIDIANURL = 8;
	private final int IS_DOWNHTML = 5;
	
	private static Handler handler;
	
	private static final int ItemA1 = Menu.FIRST;
	private static final int ItemA2 = Menu.FIRST + 1;

	public class GetQidianURL implements Runnable {
		@Override
		public void run() {
			String json = FoxBookLib.downhtml(site_qidian.qidian_getSearchURL_Mobile(book_name), "utf-8");
            List<Map<String, Object>> qds = site_qidian.json2BookList(json);
            if ( qds.get(0).get("name").toString().equalsIgnoreCase(book_name) ) { // 第一个结果就是目标书
            	book_url = qds.get(0).get("url").toString();
            }

			Message msg = Message.obtain();
			msg.what = IS_GETQIDIANURL;
			msg.obj = book_url;
			handler.sendMessage(msg);
		}			
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // 标题栏中添加返回图标
//		getActionBar().setDisplayShowHomeEnabled(false); // 隐藏程序图标
	}		// 响应点击事件在onOptionsItemSelected的switch中加入 android.R.id.home   this.finish();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isWhiteActionBar = settings.getBoolean("isWhiteActionBar", isWhiteActionBar);
		if ( isWhiteActionBar ) {
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		mExitTime = System.currentTimeMillis();
		
		showHomeUp();
		
		et = (EditText) findViewById(R.id.editText1);
		wv = (WebView) findViewById(R.id.webView1);
		btn_search = (ImageButton) findViewById(R.id.button1);
		btn_pre = (Button) findViewById(R.id.button2);
		this.registerForContextMenu(btn_pre);
		
//		wv.loadUrl("about:blank");
//		wv.loadDataWithBaseURL("http://www.autohotkey.net/~linpinger/index.html?s=FoxBook_Android", "用法说明:", "text/html", "utf-8", "");

		wv.setWebViewClient(new WebViewClient() { // 在当前webview里面跳转
			public boolean shouldOverrideUrlLoading(WebView wb, String url) {
				wb.loadUrl(url);
				return true;
			}
		});
		
		// 说明
//		wv.getSettings().setDefaultTextEncodingName("UTF-8");
		String html = "<!DOCTYPE html>\n<html>\n<head>\t<META http-equiv=Content-Type content=\"text/html; charset=utf-8\">\n<title>萌萌哒说明</title>\n</head>\n<body bgcolor=\"#eefaee\">\n<h2>说明:</h2>\n\n<h3>使用搜索引擎搜索:</h3>\n<ul>\n<li>输入要搜索的书名，按搜索按钮，然后在这里会显示搜索引擎结果</li>\n<li>点击链接直到目录页，然后按按钮“预”</li>\n</ul>\n\n<h3>使用快速搜索:</h3>\n<ul>\n<li>输入要搜索的书名</li>\n<li>按菜单键，在出来的菜单中选择一个搜索即可</li>\n</ul>\n\n<p>　如果出现列表正常的话，按加号添加书，之后按保存按钮</p>\n<p>　然后回到主界面即可看到新添加的书</p>\n\n</body>\n</html>" ;
		wv.loadData(html, "text/html; charset=UTF-8", null);

		btn_search.setOnClickListener(new OnClickListener() { // 点击按钮搜索 // 需要转换编码
			public void onClick(View v) {
				book_name = et.getText().toString();
				if ( book_name.length() == 0 ) { // 当未输入书名，去排行看看
					funcOpenTopQD(true);
					return ;
				}
				try {
					wv.loadUrl("http://cn.bing.com/search?q=" + URLEncoder.encode(book_name, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.toString();
				}

			}

		});

		handler = new Handler(new Handler.Callback() {
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
					case IS_GETQIDIANURL:
						String lcQidianURL = (String)msg.obj;
						if ( 0 != lcQidianURL.length() ) {
							Intent intentQD = new Intent(Activity_SearchBook.this, Activity_PageList.class);
							intentQD.putExtra("iam", SITES.FROM_NET);
							intentQD.putExtra("bookurl", lcQidianURL);
							intentQD.putExtra("bookname", book_name);
							intentQD.putExtra("searchengine", SITES.SE_QIDIAN_MOBILE);
							Activity_PageList.oDB = oDB;
							startActivity(intentQD);
						} else {
							foxtip("在起点上未搜索到该书名");
						}
						break;
					case IS_DOWNHTML:
						String html = (String)msg.obj;

						Intent intent = new Intent(Activity_SearchBook.this, Activity_PageList.class);
						intent.putExtra("iam", SITES.FROM_NET);
						intent.putExtra("bookurl", book_url);
						intent.putExtra("bookname", book_name);
						intent.putExtra("html", html);
						Activity_PageList.oDB = oDB;
						startActivity(intent);
						break;
				}
				return false;
			}
		});

		final Runnable downHTML = new Runnable() {
			@Override
			public void run() {
				String html = "";
				if ( book_url.toLowerCase().contains(".qidian.com/") ) { // 起点地址特别处理
					int qdid = site_qidian.qidian_getBookID_FromURL(book_url);
					if ( qdid == 0 ) { return ; }
					book_url = site_qidian.qidian_getIndexURL_Mobile(qdid);
					html = FoxBookLib.downhtml(book_url, "utf-8");
				} else {
					html = FoxBookLib.downhtml(book_url);
				}
				
				Message msg = Message.obtain();
				msg.what = IS_DOWNHTML;
				msg.obj = html;
				handler.sendMessage(msg);
			}
		};
		


		btn_pre.setOnClickListener(new OnClickListener() { // 预览按钮
			public void onClick(View v) {
				book_url = wv.getUrl();
				if ( null != book_url ) {
					setTitle(book_name + " <" + book_url + ">");
					new Thread(downHTML).start();
				} else { //什么时候会是null？
					book_url = "";
					Intent itt = getIntent();
					book_url = itt.getStringExtra("bookurl");
					if ( null == book_url ) {
						book_url = "";
						setTitle("错误: 当前页面地址为空");
					} else {
						// 这里为毛要 get  bookurl bookname ?
						book_name = itt.getStringExtra("bookname");
						if ( null == book_name ) {
							book_name = "";
						}
						setTitle("蜜汁下载目录 : " + book_name + " <" + book_url + ">");
						new Thread(downHTML).start();
					}
				}
			}
		});
		
	}

	@Override
	public void onBackPressed() { // 返回键被按
		if ((System.currentTimeMillis() - mExitTime) > 2000) {
			if ( wv.canGoBack() ) {
				foxtip("后退中...");
				wv.goBack(); // goBack()表示返回webView的上一页面
			} else {
				foxtip("再按一次退出搜索");
				mExitTime = System.currentTimeMillis();
			}
		} else {
			exitMe();
		}
	}

	private void exitMe() {
		setResult(RESULT_OK);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.search, menu);
		return true;
	}


	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case android.R.id.home: // 返回图标
			exitMe();
			break;
		case R.id.sm_QuickSearchQidian: // 快搜:起点
			book_name = et.getText().toString();
			(new Thread(new GetQidianURL())).start() ;
			break;
		case R.id.sm_QuickSearchSouGou: // 快搜:搜狗
			book_name = et.getText().toString();
			Intent intent = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			intent.putExtra("bookname", book_name);
			intent.putExtra("searchengine", SITES.SE_SOGOU);
			Activity_QuickSearch.oDB = oDB;
			startActivity(intent);
			break;
		case R.id.sm_QuickSearchBing:  // 快搜:Bing
			book_name = et.getText().toString();
			Intent itb = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			itb.putExtra("bookname", book_name);
			itb.putExtra("searchengine", SITES.SE_BING);
			Activity_QuickSearch.oDB = oDB;
			startActivity(itb);
			break;
		case R.id.sm_QuickSearchYahoo:  // 快搜:雅虎
			book_name = et.getText().toString();
			Intent ityh = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			ityh.putExtra("bookname", book_name);
			ityh.putExtra("searchengine", SITES.SE_YAHOO);
			Activity_QuickSearch.oDB = oDB;
			startActivity(ityh);
			break;
		case R.id.link_qidian_mtop:
			funcOpenTopQD(true);
			break;
		case R.id.link_qidian_dtop:
			funcOpenTopQD(false);
			break;
		case R.id.sm_copyURL:
			this.funcCopyURL();
			break;
		case R.id.sm_downQDtxt:
			this.funcDownQDtxt();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ItemA1:
			funcCopyURL();
			break;
		case ItemA2:
			funcDownQDtxt();
			break;
		}
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if ( v == this.btn_pre ) {
			menu.setHeaderTitle("操作");
			menu.add(9, ItemA1, 9, "复制网址到剪贴板"); // int groupId, int itemId, int order, CharSequence title
			menu.add(9, ItemA2, 9, "下载起点Txt");
		}
	}
	
	private String funcCopyURL() {
		String ua = wv.getUrl();
		TOOLS.setcliptext(ua, this);
		foxtip("剪贴板:\n" + ua);
		return ua;
	}
	
	private void funcDownQDtxt() {
		String ub = wv.getUrl();
		if (ub.contains(".qidian.com/")) {
			String qidianID = String.valueOf(site_qidian.qidian_getBookID_FromURL(ub));
			TOOLS.download("http://download.qidian.com/pda/" + qidianID + ".txt", qidianID + ".txt", this);
			foxtip("开始下载: " + qidianID + ".txt");
		} else {
			foxtip("非起点URL:\n" + ub);
		}
	}
	
	private void funcOpenTopQD(boolean isMobile) {
		wv.getSettings().setJavaScriptEnabled(true) ; // 允许JS
		if ( isMobile ) {
			wv.loadUrl("http://m.qidian.com/top.aspx");
		} else {
			wv.loadUrl("http://top.qidian.com");
		}
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}
