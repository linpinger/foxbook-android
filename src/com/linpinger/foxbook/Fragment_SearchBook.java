package com.linpinger.foxbook;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import java.lang.String;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import com.linpinger.misc.BackHandledFragment;
import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.FoxHTTP;
import com.linpinger.tool.ToolAndroid;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Fragment_SearchBook extends BackHandledFragment {

	public static Fragment_SearchBook newInstance(NovelManager novelMgr) {
		Fragment_SearchBook fc = new Fragment_SearchBook();
		fc.nm = novelMgr;
		return fc;
	}
//	public static Fragment_SearchBook newInstance(NovelManager novelMgr, String bookName) {
//		Fragment_SearchBook fc = new Fragment_SearchBook();
//		fc.nm = novelMgr;
//		Bundle bd = new Bundle();
//		bd.putInt(AC.action, AC.aListQDPages);
//		bd.putString(NV.BookName, bookName);
//		fc.setArguments(bd);
//		return fc;
//	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ctx = container.getContext();
		View v = inflater.inflate(R.layout.fragment_search, container, false); // 这个false很重要，不然会崩溃

		init_views(v);
		init_handler();
		init_button_actions();

		setWebView(); // 在当前webview里面跳转
		loadDefaultHTML(); // 载入默认内容

		// 获取传入的数据
//		Bundle itt = getArguments();
//		if ( itt != null ) {
//			ittAction = itt.getInt(AC.action, 0);
//			switch (ittAction) {
//			case AC.aListQDPages: // 搜索起点
//				book_name = itt.getString(NV.BookName);
//				et.setText(book_name);
//				(new Thread(new GetQidianURLFromBookName(book_name))).start() ;
//				break;
//			}
//		}

		if ( ToolAndroid.getClipText(ctx).length() > 1 ) {
			fucClickButtonSearch(); // 搜索剪贴板中的内容
		}

		return v;
	} // onCreate end

	void init_button_actions() {
		btn_finish.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		btn_finish.setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				back();
				return true;
			}
		});
		// ----
		btn_search.setOnClickListener(new OnClickListener() { // 点击按钮搜索 // 需要转换编码
			public void onClick(View v) {
				fucClickButtonSearch();
			}
		});
		btn_search.setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				createPopupMenuSearch(v) ; // 搜索/浏览：弹出菜单
				return true;
			}
		});
		// ----
		btn_pre.setOnClickListener(new OnClickListener() { // 预览按钮
			public void onClick(View v) {
				book_url = wv.getUrl();
				if ( null != book_url ) {
					foxtip(book_name + " <" + book_url + ">");
					ToolAndroid.setClipText(tmpClipBoardText, ctx);

					Bundle bd = new Bundle();

					bd.putInt(AC.action, AC.aSearchBookOnSite);
					bd.putString(NV.BookURL, book_url);
					bd.putString(NV.BookName, book_name);

					startFragment( Fragment_PageList.newInstance(nm, bd) );
				} else { //什么时候会是null？
					foxtip("错误: 当前页面地址为空");
				}
			}
		});
		btn_pre.setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				createPopupMenuPreview(v) ; // 预览/快搜:弹出菜单
				return true;
			}
		});
		// ----
		btn_other.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				funcAddBookFromClip();
			}
		});
		btn_other.setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				createPopupMenuOther(v) ; // 剪贴板/其他: 弹出菜单
				return true;
			}
		});
		
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void createPopupMenuSearch(View view) { // 搜索/浏览：弹出菜单
		PopupMenu popW = new PopupMenu(ctx, view);
		Menu m = popW.getMenu();
	
		m.add("下载起点Epub");
		m.add("使用说明");
		m.add("设置: 允许JS");
		m.add("设置: 不允许JS");
		m.add("设置: 桌面UA");
		m.add("设置: 手机UA");
		m.add("复制site: xxx.com");
		m.add("复制当前网址");
		m.add("打开: 起点排行");
		m.add("打开: 百度");
	
		popW.show();
		popW.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem mi) {
				String mt = mi.getTitle().toString();
				if ( mt.equalsIgnoreCase("复制site: xxx.com") ) {
					copyCurrentMainSiteStr();
				} else if ( mt.equalsIgnoreCase("复制当前网址") ) {
					funcCopyURL();
				} else if ( mt.equalsIgnoreCase("打开: 百度") ) {
					wv.loadUrl("https://www.baidu.com");
				} else if ( mt.equalsIgnoreCase("打开: 起点排行") ) {
					wv.loadUrl("http://r.qidian.com");
				} else if ( mt.equalsIgnoreCase("设置: 允许JS") ) {
					wv.getSettings().setJavaScriptEnabled(true) ; // 允许JS
					wv.reload();
				} else if ( mt.equalsIgnoreCase("设置: 不允许JS") ) {
					wv.getSettings().setJavaScriptEnabled(false) ; // 不允许JS
					wv.reload();
				} else if ( mt.equalsIgnoreCase("设置: 桌面UA") ) {
					setUserAgent("desktop");
					wv.reload();
				} else if ( mt.equalsIgnoreCase("设置: 手机UA") ) {
					setUserAgent("mobile");
					wv.reload();
				} else if ( mt.equalsIgnoreCase("下载起点Epub") ) {
					funcDownQDEbook();
				} else if ( mt.equalsIgnoreCase("使用说明") ) {
					loadDefaultHTML(); // 载入默认内容
				}
				return true;
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void createPopupMenuPreview(View view) { // 预览/快搜:弹出菜单
		PopupMenu popW = new PopupMenu(ctx, view);
		Menu m = popW.getMenu();
	
		m.add("快速搜索:雅虎");
		m.add("快速搜索:搜狗");
		m.add("快速搜索:起点");
		m.add("快速搜索:Bing");
		m.add("快速搜索: meegoq");
	
		popW.show();
		popW.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem mi) {
				String mt = mi.getTitle().toString();
				book_name = et.getText().toString();

				if ( mt.equalsIgnoreCase("快速搜索:起点") ) {
					(new Thread(new GetQidianURLFromBookName(book_name))).start() ;
				} else if ( mt.equalsIgnoreCase("快速搜索:Bing") ) {
					startFragment( Fragment_QuickSearch.newInstance(nm, AC.SE_BING, book_name) );
				} else if ( mt.equalsIgnoreCase("快速搜索:搜狗") ) {
					startFragment( Fragment_QuickSearch.newInstance(nm, AC.SE_SOGOU, book_name) );
				} else if ( mt.equalsIgnoreCase("快速搜索:雅虎") ) {
					startFragment( Fragment_QuickSearch.newInstance(nm, AC.SE_YAHOO, book_name) );
				} else if ( mt.equalsIgnoreCase("快速搜索: meegoq") ) {
					startFragment( Fragment_QuickSearch.newInstance(nm, AC.SE_MEEGOQ, book_name) );
				}
				return true;
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void createPopupMenuOther(View view) { // 剪贴板/其他: 弹出菜单
		PopupMenu popW = new PopupMenu(ctx, view);
		Menu m = popW.getMenu();
	
		m.add("复制临时全书信息");
	
		popW.show();
		popW.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem mi) {
				String mt = mi.getTitle().toString();
				if ( mt.equalsIgnoreCase("复制临时全书信息") ) {
					ToolAndroid.setClipText(tmpClipBoardText, ctx);
					foxtip("剪贴板:\n" + tmpClipBoardText);
				}
				return true;
			}
		});
	}

	public class GetQidianURLFromBookName implements Runnable {
		String bookname ;
		public GetQidianURLFromBookName(String inBookName) {
			this.bookname = inBookName;
		}
		@Override
		public void run() {
			SiteQiDian qd = new SiteQiDian();
			String json = new FoxHTTP( qd.getSearchURL_Android7(this.bookname) ).getHTML("UTF-8") ;
			List<Map<String, Object>> qds = qd.getSearchBookList_Android7(json);
			if ( qds.get(0).get(NV.BookName).toString().equalsIgnoreCase(this.bookname) ) { // 第一个结果就是目标书
				book_url = qds.get(0).get(NV.BookURL).toString();
			}

			Message msg = Message.obtain();
			msg.what = IS_GETQIDIANURL;
			msg.obj = book_url;
			handler.sendMessage(msg);
		}			
	}

	private String funcCopyURL() {
		String ua = wv.getUrl();
		ToolAndroid.setClipText(ua, ctx);
		foxtip("剪贴板:\n" + ua);
		return ua;
	}

	private void funcDownQDEbook() {
		String ub = wv.getUrl();
		if (ub.contains(".qidian.com/")) {
			String qidianID = new SiteQiDian().getBookID_FromURL(ub);
			final String eBookURL = "http://download.qidian.com/epub/" + qidianID + ".epub";
			final String SAVEPATH= "/sdcard/99_sync/" + qidianID + ".epub";

			ToolAndroid.download("http://download.qidian.com/epub/" + qidianID + ".epub", qidianID + ".epub", ctx);
			foxtip("开始下载: " + qidianID + ".epub");

			(new Thread() { public void run() {
			   	// 2019-11-17: 下载起点epub需要加该HTTP头字段
				long fLen = new FoxHTTP(eBookURL).setHead("Accept-Encoding", "gzip, deflate").saveFile(SAVEPATH);

				Message msg = Message.obtain();
				msg.what = IS_SHOWTIP;
				msg.obj = SAVEPATH + " 下载完毕，大小: " + fLen ;
				handler.sendMessage(msg);
			}}).start();
			
		} else {
			foxtip("非起点URL:\n" + ub);
		}
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(ctx, sinfo, Toast.LENGTH_SHORT).show();
	}

	public void copyCurrentMainSiteStr() {
		String mainBookURL = nm.getBookInfo(0).get(NV.BookURL).toString();
		String urlHost = "";
		try {
			URL uu = new URL(mainBookURL);
			urlHost = uu.getHost();
			if ( urlHost.startsWith("www.") ) {
				urlHost = urlHost.replace("www.", "");
			} else if ( urlHost.startsWith("m.") ) {
				urlHost = urlHost.replace("m.", "");
			} else if ( urlHost.contains(".qidian.com") ) {
				urlHost = "qidian.com";
			}
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		String siteStr = " site:" + urlHost ;
		ToolAndroid.setClipText(siteStr, ctx);
		foxtip("剪贴板:\n" + siteStr);
	}

	void fucClickButtonSearch() { // 点击搜索按钮
		book_name = et.getText().toString();
		if ( book_name.length() == 0 ) { // 当未输入书名，粘贴剪贴板
			tmpClipBoardText = ToolAndroid.getClipText(ctx);
			if ( tmpClipBoardText.contains("FoxBook>") ) {
				foxtip("剪贴板中包含FoxBook>\n可以按+号按钮来快速添加书哟");
				String xx[] = tmpClipBoardText.split(">");
				book_name = xx[1];
				et.setText(book_name);
			} else {
				foxtip("剪贴板中的内容格式不对哟\n先粘贴到搜索栏好了\n长按本按钮有惊喜哟");
				book_name = tmpClipBoardText;
				et.setText(book_name);
			}
		}
		try {
			wv.loadUrl("http://cn.bing.com/search?q=" + URLEncoder.encode(book_name, "UTF-8"));
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	void setUserAgent(String iType) {
//		String oldUA = wv.getSettings().getUserAgentString();

		String newUA = "foobar";
		if ( iType.equalsIgnoreCase("mobile") ) {
			newUA = "Mozilla/5.0 (Linux; Android 7.1.2; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/66.0.3359.158 Mobile Safari/537.36";
		} else if ( iType.equalsIgnoreCase("desktop") ) {
			newUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Safari/537.36";
		}
		wv.getSettings().setUserAgentString(newUA);
	}

	void funcAddBookFromClip() {
		String nowfbs = ToolAndroid.getClipText(ctx);
		if ( ! nowfbs.contains("FoxBook>") ) {
			foxtip("剪贴板中的内容格式不对哟\n长按本按钮有惊喜哟");
			return;
		}
		String xx[] = nowfbs.split(">");
		if ( "" != xx[1] && "" != xx[4] ) { // name, url
			int nBookIDX = -1 ;
			nBookIDX = nm.addBook(xx[1], xx[4], xx[3]); // 新增，并获取返回bookidx
			if ( nBookIDX < 0 )
				return ;
			if ( "" != xx[2] ) { // 作者
				Map<String, Object> nowBookInfo = nm.getBookInfo(nBookIDX);
				nowBookInfo.put(NV.BookAuthor, xx[2]);
				nm.setBookInfo(nowBookInfo, nBookIDX);
			}
			
			startFragment( Fragment_BookInfo.newInstance(nm, nBookIDX) );
			back();
		} else {
			foxtip("信息不完整，不包含名字和地址\n" + nowfbs);
		}
	}

	void setWebView() {
		setUserAgent("desktop");
		wv.setWebViewClient(new WebViewClient() { // 在当前webview里面跳转
			public boolean shouldOverrideUrlLoading(WebView wb, String url) {
				wb.loadUrl(url);
				return true;
			}
		});
	}

	void loadDefaultHTML() { // 载入默认内容
			String html = "<!DOCTYPE html>\n<html>\n<head>\t<META http-equiv=Content-Type content=\"text/html; charset=utf-8\">\n<title>萌萌哒说明</title>\n</head>\n<body bgcolor=\"#eefaee\">\n<center><h2>使用说明:</h2></center>\n\n<h3>使用搜索引擎搜索:</h3>\n<ul>\n<li>[输入要搜索的书名，]按搜索按钮，然后在这里会显示搜索引擎结果[长按搜索按钮，可以弹出菜单]</li>\n<li>点击链接直到目录页，然后按按钮“圈+”</li>\n</ul>\n\n<h3>使用快速搜索:</h3>\n<ul>\n<li>输入要搜索的书名</li>\n<li>长按“圈+”，在出来的菜单中选择一个搜索即可</li>\n</ul>\n\n<h3>添加剪贴板中书籍:</h3>\n<ul>\n<li>前提是剪贴板里已经有书籍信息</li>\n<li>按“+”按钮即可添加</li>\n<li>长按“+”，有其他功能</li>\n</ul>\n\n<p>　如果出现列表正常的话，按加号添加书，之后按保存按钮</p>\n<p>　然后回到主界面即可看到新添加的书</p>\n\n</body>\n</html>" ;
			wv.loadData(html, "text/html; charset=UTF-8", null);
	//		wv.loadUrl("about:blank");
	//		wv.loadDataWithBaseURL("https://linpinger.github.io/?s=FoxBook_Android", "用法说明:", "text/html", "utf-8", "");
	}

	void init_handler() {
		handler = new Handler(new Handler.Callback() {
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
					case IS_GETQIDIANURL:
						String lcQidianURL = (String)msg.obj;
						if ( 0 != lcQidianURL.length() ) {
							Bundle bd = new Bundle();

							bd.putInt(AC.action, AC.aSearchBookOnQiDian);
							bd.putString(NV.BookURL, lcQidianURL);
							bd.putString(NV.BookName, book_name);

							startFragment( Fragment_PageList.newInstance(nm, bd) );
						} else {
							foxtip("在起点上未搜索到该书名");
						}
						break;
					case IS_SHOWTIP:
						foxtip( (String)msg.obj );
						break;
				}
				return false;
			}
		});
	}
	void init_views(View v) {
		wv   = (WebView)  v.findViewById(R.id.webView1);
		btn_finish = (Button) v.findViewById(R.id.btnFinish);
		et   = (EditText) v.findViewById(R.id.editText1); // bookname
		btn_search = (Button) v.findViewById(R.id.button1);
		btn_pre    = (Button) v.findViewById(R.id.button2);
		btn_other  = (Button) v.findViewById(R.id.button3);
	}

	private NovelManager nm;

	private String book_name = "";
	private String book_url = "";

	private String tmpClipBoardText = "";
//	private int ittAction = 0 ; // 传入的数据

	private Context ctx;
	private WebView wv;
	private Button btn_finish;
	private EditText et;
	private Button btn_search;
	private Button btn_pre ;
	private Button btn_other ;

	private final int IS_GETQIDIANURL = 8;
	private final int IS_SHOWTIP = 1;

	private static Handler handler;

	private OnFinishListener lsn;
	public Fragment setOnFinishListener(OnFinishListener ofl) {
		lsn = ofl;
		return this;
	}
	@Override
	public void onDestroy() {
		if ( lsn != null) {
			lsn.OnFinish();
		}
		super.onDestroy();
	}

	@Override
	public boolean onBackPressed() {
		if ( wv.canGoBack() ) {
			wv.goBack(); // goBack()表示返回webView的上一页面
		} else {
			back();
		}
		return true;
	}


}
