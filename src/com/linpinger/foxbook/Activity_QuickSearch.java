package com.linpinger.foxbook;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.tool.Ext_ListActivity_4Eink;
import com.linpinger.tool.ToolBookJava;

import android.annotation.TargetApi;
// import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

// Activity_PageList : 单击列表

public class Activity_QuickSearch extends Ext_ListActivity_4Eink {
	private ListView lv_sitelist ;
	SimpleAdapter adapter;
	private List<Map<String, Object>> data;
	private Handler handler;
	private static int IS_REFRESH = 5 ;

	SharedPreferences settings;

	private String book_name = "" ;
	private String book_url = "" ;

	private int SE_TYPE = 1; // 搜索引擎

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // 标题栏中添加返回图标
//		getActionBar().setDisplayShowHomeEnabled(false); // 隐藏程序图标
	}		// 响应点击事件在onOptionsItemSelected的switch中加入 android.R.id.home this.finish();

	@Override
	public void onCreate(Bundle savedInstanceState) { // 入口
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_quicksearch);
		showHomeUp();

		lv_sitelist = getListView();

		Intent itt = getIntent();
		book_name = itt.getStringExtra(NV.BookName); // 必需
		SE_TYPE = itt.getIntExtra(AC.searchEngine, 1) ;

		setTitle("搜索: " + book_name);

		data = new ArrayList<Map<String, Object>>();
		refreshLVAdapter();

		init_LV_item_click() ; // 初始化 单击 条目 的行为

		String seURL = "" ;
		try {
			switch (SE_TYPE) { // 1:sogou 2:yahoo 3:bing
			case AC.SE_SOGOU:
				seURL = "http://www.sogou.com/web?query=" + URLEncoder.encode(book_name, "GB2312") + "&num=50" ;
				break;
			case AC.SE_YAHOO:
				seURL = "http://search.yahoo.com/search?n=40&p=" + URLEncoder.encode(book_name, "UTF-8") ;
				break;
			case AC.SE_BING:
				seURL = "http://cn.bing.com/search?q=" + URLEncoder.encode(book_name, "UTF-8") ;
				break;
			}
		} catch (Exception e) {
			System.err.println(e.toString());
		}

		init_handler() ; // 初始化一个handler 用于处理后台线程的消息

		final String thURL = seURL ;
		(new Thread(){
			public void run(){
				data = ToolBookJava.getSearchEngineHref( ToolBookJava.downhtml(thURL) , book_name); // 搜索引擎网页分析放在这里
				handler.sendEmptyMessage(IS_REFRESH);
			}
		}).start();

	}

	private void refreshLVAdapter() {
		adapter = new SimpleAdapter(this, data,
				android.R.layout.simple_list_item_2, new String[] { NV.BookName, NV.BookURL },
				new int[] { android.R.id.text1, android.R.id.text2 });
		lv_sitelist.setAdapter(adapter);
	}

	private void init_LV_item_click() { // 初始化 单击 条目 的行为
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Map<String, Object> book = (HashMap<String, Object>) parent.getItemAtPosition(position);
				book_url = (String) book.get(NV.BookURL);
				Intent itt = new Intent(Activity_QuickSearch.this, Activity_PageList.class);
				itt.putExtra(AC.action, AC.aSearchBookOnSite);
				itt.putExtra(NV.BookURL, book_url);
				itt.putExtra(NV.BookName, book_name);
				startActivity(itt);
			}
		};
		lv_sitelist.setOnItemClickListener(listener);
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case android.R.id.home: // 返回图标
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void init_handler() { // 初始化一个handler 用于处理后台线程的消息
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if ( msg.what == IS_REFRESH ) { // 下载完毕
					refreshLVAdapter();
				}
			}
		};
	}

}
