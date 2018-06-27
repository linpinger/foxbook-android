package com.linpinger.foxbook;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.tool.ToolAndroid;
import com.linpinger.tool.ToolBookJava;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

// Activity_PageList : 单击列表

public class Activity_QuickSearch extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) { // 入口
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		this.setTheme(android.R.style.Theme_Holo_Light_NoActionBar); // 无ActionBar
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_quicksearch);

		info = (TextView)this.findViewById(R.id.testTV);
		lv = (ListView)this.findViewById(R.id.testLV); // 获取LV
		if ( ! ToolAndroid.isEink() ) {
			lv.setBackgroundColor(Color.parseColor("#EEFAEE"));
		}

		Intent itt = getIntent();
		book_name = itt.getStringExtra(NV.BookName); // 必需
		SE_TYPE = itt.getIntExtra(AC.searchEngine, 1) ;
		if ( SE_TYPE == AC.SE_NONE ) { // 非搜索引擎
			seURL = itt.getStringExtra(NV.BookURL);
			foxtipL("搜索: " + book_name + "  点此返回，普通页面地址:\n" + seURL);
		} else {
			foxtipL("搜索: " + book_name + "  点击这里返回");
		}
		
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

		(new Thread(){
			public void run(){
				if ( SE_TYPE == AC.SE_NONE ) {
					data = ToolBookJava.getSearchEngineHref( ToolBookJava.downhtml(seURL) , "");
				} else {
					data = ToolBookJava.getSearchEngineHref( ToolBookJava.downhtml(seURL) , book_name); // 搜索引擎网页分析放在这里
				}
				handler.sendEmptyMessage(IS_REFRESH);
			}
		}).start();

		init_handler() ; // 初始化一个handler 用于处理后台线程的消息
		initViewAction() ; // 初始化 点击 的行为

	}

	void initViewAction() {

		lv.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent arg1) {
				clickX = arg1.getX(); // 点击横坐标
				return false;
			}
		});
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Map<String, Object> book = (HashMap<String, Object>) parent.getItemAtPosition(position);
				book_url = (String) book.get(NV.BookURL);
				
				if ( clickX > lv.getWidth() * 0.8 ) { // 右边1/5处: 表示进入信息页，需要手工选择链接
					Intent ittR = new Intent(Activity_QuickSearch.this, Activity_QuickSearch.class); // Test打开副本
					ittR.putExtra(AC.action, AC.aSearchBookOnSite);
					ittR.putExtra(NV.BookName, book_name);
					ittR.putExtra(AC.searchEngine, AC.SE_NONE);
					ittR.putExtra(NV.BookURL, ToolBookJava.getFullURL(seURL, book_url));
					startActivity(ittR);
				} else {
					Intent itt = new Intent(Activity_QuickSearch.this, Activity_PageList.class); //
					itt.putExtra(AC.action, AC.aSearchBookOnSite);
					itt.putExtra(NV.BookURL, book_url);
					itt.putExtra(NV.BookName, book_name);
					startActivity(itt);
				}
				foxtipL("搜索: " + book_name + "\n" + book_url);
			}
		});

		info.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

	} // initViewAction end

	private void refreshLVAdapter() {
		adapter = new SimpleAdapter(this, data,
				android.R.layout.simple_list_item_2, new String[] { NV.BookName, NV.BookURL },
				new int[] { android.R.id.text1, android.R.id.text2 });
		lv.setAdapter(adapter);
	}

	private void init_handler() { // 初始化一个handler 用于处理后台线程的消息
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if ( msg.what == IS_REFRESH ) { // 下载完毕
					info.setText( "D." + info.getText() );
					refreshLVAdapter();
				}
			}
		};
	}

	private void foxtipL(String sinfo) {
		info.setText(sinfo);
//		info.setVisibility(View.VISIBLE);
	}

	private float clickX = 0 ; // 用来确定点击横坐标以实现不同LV不同区域点击效果
	private ListView lv ;
	private TextView info;
	SimpleAdapter adapter;
	private List<Map<String, Object>> data;
	private Handler handler;
	private static int IS_REFRESH = 5 ;

	private String book_name = "" ;
	private String book_url = "" ;

	private int SE_TYPE = 1; // 搜索引擎
	private String seURL = "" ;
}
