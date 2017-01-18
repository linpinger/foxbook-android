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

import com.linpinger.novel.NV;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.ToolAndroid;
import com.linpinger.tool.ToolBookJava;
/*
Activity_PageList : msg IS_GETQIDIANURL -> Runnable:GetQidianURL : 选项: 快搜:qidian : aSearchBookOnQiDian, bookname, bookurl
Activity_PageList : 预览按钮 : aSearchBookOnNet, bookname, bookurl

Activity_QuickSearch : 选项菜单:sogou : BookName, searchEngine
Activity_QuickSearch : 选项菜单:bing  : BookName, searchEngine
Activity_QuickSearch : 选项菜单:yahoo : BookName, searchEngine
*/
public class Activity_SearchBook extends Activity {

	private int ittAction = 0 ; // 传入的数据
	private long mExitTime ;
	private WebView wv;
	private EditText et;
	private ImageButton btn_search;
	private Button btn_pre ;
	
	SharedPreferences settings;
	private String book_name = "";
	private String book_url = "";
	
	private final int IS_GETQIDIANURL = 8;
	
	private static Handler handler;
	
	private static final int ItemA1 = Menu.FIRST;
	private static final int ItemA2 = Menu.FIRST + 1;

	public class GetQidianURLFromBookName implements Runnable {
		String bookname ;
		public GetQidianURLFromBookName(String inBookName) {
			this.bookname = inBookName;
		}
		@Override
		public void run() {
			String json = ToolBookJava.downhtml(new SiteQiDian().getSearchURL_Android7(this.bookname), "utf-8");
			List<Map<String, Object>> qds = new SiteQiDian().getSearchBookList_Android7(json);
			if ( qds.get(0).get(NV.BookName).toString().equalsIgnoreCase(this.bookname) ) { // 第一个结果就是目标书
				book_url = qds.get(0).get(NV.BookURL).toString();
			}

			Message msg = Message.obtain();
			msg.what = IS_GETQIDIANURL;
			msg.obj = book_url;
			handler.sendMessage(msg);
		}			
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true); // 标题栏中添加返回图标
//		getActionBar().setDisplayShowHomeEnabled(false); // 隐藏程序图标
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		mExitTime = System.currentTimeMillis();
		
		showHomeUp();

		et = (EditText) findViewById(R.id.editText1); // bookname
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
							intentQD.putExtra(AC.action, AC.aSearchBookOnQiDian);
							intentQD.putExtra(NV.BookURL, lcQidianURL);
							intentQD.putExtra(NV.BookName, book_name);
							startActivity(intentQD);
						} else {
							foxtip("在起点上未搜索到该书名");
						}
						break;
				}
				return false;
			}
		});

		btn_pre.setOnClickListener(new OnClickListener() { // 预览按钮
			public void onClick(View v) {
				book_url = wv.getUrl();
				if ( null != book_url ) {
					setTitle(book_name + " <" + book_url + ">");
					Intent intent = new Intent(Activity_SearchBook.this, Activity_PageList.class);
					intent.putExtra(AC.action, AC.aSearchBookOnSite);
					intent.putExtra(NV.BookURL, book_url);
					intent.putExtra(NV.BookName, book_name);
					startActivity(intent);
				} else { //什么时候会是null？
					setTitle("错误: 当前页面地址为空");
				}
			}
		});

		// 获取传入的数据
		Intent itt = getIntent();
		ittAction = itt.getIntExtra(AC.action, 0);
		switch (ittAction) {
		case AC.aListQDPages: // 搜索起点
			book_name = itt.getStringExtra(NV.BookName);
			et.setText(book_name);
			(new Thread(new GetQidianURLFromBookName(book_name))).start() ;
			break;
		}
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
			(new Thread(new GetQidianURLFromBookName(book_name))).start() ;
			break;
		case R.id.sm_QuickSearchSouGou: // 快搜:搜狗
			book_name = et.getText().toString();
			Intent intent = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			intent.putExtra(NV.BookName, book_name);
			intent.putExtra(AC.searchEngine, AC.SE_SOGOU);
			startActivity(intent);
			break;
		case R.id.sm_QuickSearchBing: // 快搜:Bing
			book_name = et.getText().toString();
			Intent itb = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			itb.putExtra(NV.BookName, book_name);
			itb.putExtra(AC.searchEngine, AC.SE_BING);
			startActivity(itb);
			break;
		case R.id.sm_QuickSearchYahoo: // 快搜:雅虎
			book_name = et.getText().toString();
			Intent ityh = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			ityh.putExtra(NV.BookName, book_name);
			ityh.putExtra(AC.searchEngine, AC.SE_YAHOO);
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
		case R.id.sm_downQDEpub:
			this.funcDownQDEbook();
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
			funcDownQDEbook();
			break;
		}
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if ( v == this.btn_pre ) {
			menu.setHeaderTitle("操作");
			menu.add(9, ItemA1, 9, "复制网址到剪贴板"); // int groupId, int itemId, int order, CharSequence title
			menu.add(9, ItemA2, 9, "下载起点Epub");
		}
	}
	
	private String funcCopyURL() {
		String ua = wv.getUrl();
		ToolAndroid.setClipText(ua, this);
		foxtip("剪贴板:\n" + ua);
		return ua;
	}
	
	private void funcDownQDEbook() {
		String ub = wv.getUrl();
		if (ub.contains(".qidian.com/")) {
			String qidianID = new SiteQiDian().getBookID_FromURL(ub);
			ToolAndroid.download("http://download.qidian.com/epub/" + qidianID + ".epub", qidianID + ".epub", this);
			foxtip("开始下载: " + qidianID + ".epub");
		} else {
			foxtip("非起点URL:\n" + ub);
		}
	}
	
	private void funcOpenTopQD(boolean isMobile) {
		wv.getSettings().setJavaScriptEnabled(true) ; // 允许JS
		if ( isMobile ) {
			wv.loadUrl("http://m.qidian.com/rank/male");
		} else {
			wv.loadUrl("http://r.qidian.com");
		}
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}
