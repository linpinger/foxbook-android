package com.linpinger.foxbook;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class Activity_QuickSearch extends ListActivity {
	public static FoxMemDB oDB;
	private ListView lv_sitelist ;
	SimpleAdapter adapter;
	private List<Map<String, Object>> data;
	private Handler handler;
	private static int IS_REFRESH = 5 ;
	
	private String book_name = "" ;
	private String book_url = "" ;
	
	private int SE_TYPE = 1; // 搜索引擎
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // 标题栏中添加返回图标
//		getActionBar().setDisplayShowHomeEnabled(false); // 隐藏程序图标
	}		// 响应点击事件在onOptionsItemSelected的switch中加入 android.R.id.home   this.finish();
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // 入口
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_quicksearch);
		
		lv_sitelist = getListView();
		
		Intent itt = getIntent();
		book_name = itt.getStringExtra("bookname"); // 必需
		SE_TYPE = itt.getIntExtra("searchengine", 1) ;
		
		setTitle("搜索: " + book_name);
		
		data = new ArrayList<Map<String, Object>>(10);
		renderListView();
		
		init_handler() ; // 初始化一个handler 用于处理后台线程的消息
		
		init_LV_item_click() ; // 初始化 单击 条目 的行为
		
		String seURL = "" ;
		try {
			switch (SE_TYPE) { // 1:sogou 2:yahoo 3:bing  11:easou 12:追书神器
			case SITES.SE_SOGOU:
				seURL = "http://www.sogou.com/web?query=" + URLEncoder.encode(book_name, "GB2312") + "&num=50" ;
				break;
			case SITES.SE_YAHOO:
				seURL = "http://search.yahoo.com/search?n=40&p=" + URLEncoder.encode(book_name, "UTF-8") ;
				break;
			case SITES.SE_BING:
				seURL = "http://cn.bing.com/search?q=" + URLEncoder.encode(book_name, "UTF-8") ;
				break;
			case SITES.SE_EASOU:
				seURL = site_easou.getUrlSE(book_name);
				break;
			case SITES.SE_ZSSQ:
				seURL = site_zssq.getUrlSE(book_name);
				break;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		new Thread(new DownTOC(seURL)).start();
	}
	
	private void init_LV_item_click() { // 初始化 单击 条目 的行为
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
				book_url = (String) chapinfo.get("url");
				Intent intent = new Intent(Activity_QuickSearch.this, Activity_PageList.class);
				intent.putExtra("iam", SITES.FROM_NET);
				intent.putExtra("bookurl", book_url);
				intent.putExtra("bookname", book_name);
				intent.putExtra("searchengine", SE_TYPE);
				Activity_PageList.oDB = oDB;
				startActivity(intent);
			}
		};
		lv_sitelist.setOnItemClickListener(listener);
	}

	
	public class DownTOC implements Runnable { // 后台线程下载网页
		private String bookurl = "";
		
		public DownTOC(String inbookurl){
			this.bookurl = inbookurl;
		}
		@Override
		public void run() {
			Message msg = Message.obtain();
			switch(SE_TYPE) {
			case SITES.SE_EASOU:
				String sJson = FoxBookLib.downhtml(this.bookurl, "utf-8");
				bookurl = site_easou.getUrlSL(site_easou.json2IDs(sJson, 1));
				msg.what = IS_REFRESH;
				msg.obj = FoxBookLib.downhtml(bookurl, "utf-8");
				break;
			case SITES.SE_ZSSQ :
				String json = FoxBookLib.downhtml(this.bookurl, "utf-8");
				bookurl = site_zssq.getUrlSL(site_zssq.json2BookID(json));
				msg.what = IS_REFRESH;
				msg.obj = FoxBookLib.downhtml(bookurl, "utf-8");
				break;
			default :
				msg.what = IS_REFRESH;
				msg.obj = FoxBookLib.downhtml(this.bookurl);
			}
			handler.sendMessage(msg);
		}
	}

	private void renderListView() { // 刷新LV
		adapter = new SimpleAdapter(this, data,
				android.R.layout.simple_list_item_2, new String[] { "name", "url" },
				new int[] { android.R.id.text1, android.R.id.text2 });
		lv_sitelist.setAdapter(adapter);
	}
	
	private void init_handler() { // 初始化一个handler 用于处理后台线程的消息
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if ( msg.what == IS_REFRESH ) { // 下载完毕
					String sHTTP = (String)msg.obj;				
					switch(SE_TYPE) {
					case SITES.SE_EASOU:
						data = site_easou.json2SiteList(sHTTP) ;
						break;
					case SITES.SE_ZSSQ :
						data = site_zssq.json2SiteList(sHTTP) ;
						break;
					default :
						data = FoxBookLib.getSearchEngineHref(sHTTP, book_name); // 搜索引擎网页分析放在这里
					}
					renderListView();
				}
			}
		};
	}

/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.search, menu);
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case R.id.sm_QuickSearchSouGou: // 快搜:搜狗
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
*/


}
