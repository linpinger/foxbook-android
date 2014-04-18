package com.linpinger.foxbook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class Activity_PageList extends ListActivity {
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	
	SimpleAdapter adapter;
	private Handler handler;

	private static int IS_UPDATEPAGE = 88;
	private static int IS_DOWNTOC = 5;
	private static int FROM_DB = 1 ;
	private static int FROM_NET = 2 ; 
	private int foxfrom = 0; // 1=DB, 2=search
	private String bookurl = "";
	private String bookname = "";
	private String html = "";
 	private boolean bShowAll = false;
	private int bookid = 0 ;
	private String lcURL, lcName;
	private Integer lcID;
	private int longclickpos = 0;

	public class DownTOC implements Runnable { // 后台线程下载网页
		@Override
		public void run() {
			String html = FoxBookLib.downhtml(bookurl);
	        Message msg = Message.obtain();
	        msg.what = IS_DOWNTOC;
	        msg.obj = html;
	        handler.sendMessage(msg);
		}
	}

	private void renderListView() { // 刷新LV
		adapter = new SimpleAdapter(this, data,
				android.R.layout.simple_list_item_1, new String[] { "name" },
				new int[] { android.R.id.text1 });
		lv_pagelist.setAdapter(adapter);
	}

	private void init_LV_item_click() { // 初始化 单击 条目 的行为
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpid = (Integer) chapinfo.get("id");

				// setTitle(parent.getItemAtPosition(position).toString());
				Intent intent = new Intent(Activity_PageList.this,
						Activity_ShowPage.class);
				intent.putExtra("iam", foxfrom);
				intent.putExtra("chapter_id", tmpid);
				intent.putExtra("chapter_name", tmpname);
				intent.putExtra("chapter_url", FoxBookLib.getFullURL(bookurl, tmpurl));
				startActivity(intent);
			}
		};
		lv_pagelist.setOnItemClickListener(listener);
	}
	
	private void init_LV_item_Long_click() { // 初始化 长击 条目 的行为
		final Builder builder = new AlertDialog.Builder(this);
		OnItemLongClickListener longlistener = new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Map<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				longclickpos = position ; // base 0

				if ( foxfrom == FROM_NET ) { // 从网络下载临时的条目没有ID
					foxtip("从网络下载临时的条目没有ID");
					return true;
				}

				lcURL = (String) chapinfol.get("url");
				lcName = (String) chapinfol.get("name");
				lcID = (Integer) chapinfol.get("id");

				setTitle(lcName + " : " + lcURL);

				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("操作:" + lcName);
				builder.setItems(new String[] { "删除本章", "删除本章并不写入Dellist", "删除本章及以上", "删除本章及以上并不写入Dellist", "删除本章及以下", "删除本章及以下并不写入Dellist", "更新本章" },
						new DialogInterface.OnClickListener() {
							@TargetApi(Build.VERSION_CODES.HONEYCOMB)
							public void onClick(DialogInterface dialog,	 int which) {
								switch (which) {
								case 0:
									FoxDB.delete_Pages(lcID, true);
									data.remove(longclickpos); // 位置可能不太靠谱
									adapter.notifyDataSetChanged();
									foxtip("已删除并记录: " + lcName);
									break;
								case 1:
									FoxDB.delete_Pages(lcID, false);
									data.remove(longclickpos);
									adapter.notifyDataSetChanged();
									foxtip("已删除: " + lcName);
									break;
								case 2:
									FoxDB.delete_nowupdown_Pages(lcID, true, true);
									for ( int i = 0; i<=longclickpos; ++i) {
										data.remove(0);
									}
									adapter.notifyDataSetChanged();
									foxtip("已删除并记录: <= " + lcName);
									break;
								case 3:
									FoxDB.delete_nowupdown_Pages(lcID, true, false);
									for ( int i = 0; i<=longclickpos; ++i) {
										data.remove(0);
									}
									adapter.notifyDataSetChanged();
									foxtip("已删除: <= " + lcName);
									break;
								case 4:
									FoxDB.delete_nowupdown_Pages(lcID, false, true);
									int datasiza = data.size();
									for ( int i = longclickpos; i<datasiza; ++i) {
										data.remove(longclickpos);
									}
									adapter.notifyDataSetChanged();
									foxtip("已删除并记录: >= " + lcName);
									break;
								case 5:
									FoxDB.delete_nowupdown_Pages(lcID, false, false);
									int datasizb = data.size();
									for ( int i = longclickpos; i<datasizb; ++i) {
										data.remove(longclickpos);
									}
									foxtip("已删除: >= " + lcName);
									adapter.notifyDataSetChanged();
									break;
								case 6:  // 更新章节
									setTitle("正在更新: " + lcName);
									(new Thread(){
										public void run(){
											FoxBookLib.updatepage(lcID);
									        handler.sendEmptyMessage(IS_UPDATEPAGE);
										}
									}).start();
									break;
								}
							}
						});
				builder.create().show();
				return true;
			}

		};
		lv_pagelist.setOnItemLongClickListener(longlistener);
	}
	
	private void init_handler() { // 初始化一个handler 用于处理后台线程的消息
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if ( msg.what == IS_UPDATEPAGE ) { // 更新章节完毕
					setTitle("节更新完毕 : " + lcName);
				}
				if ( msg.what == IS_DOWNTOC ) { // 下载目录完毕
					html = (String)msg.obj;
					if ( bShowAll ) {
						data = FoxBookLib.tocHref(html, 0);
					} else {
						data = FoxBookLib.tocHref(html, 16);
					}
					renderListView();
				}
			}
		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) { // 入口
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pagelist);

		lv_pagelist = getListView();

		// 获取传入的数据
		Intent itt = getIntent();
		foxfrom = itt.getIntExtra("iam", 0); // 必需 表明数据从哪来的
		bookurl = itt.getStringExtra("bookurl"); // 必需
		bookname = itt.getStringExtra("bookname"); // 必需
		bShowAll = itt.getBooleanExtra("bShowAll", false);

		setTitle(bookname + " : " + bookurl);

		init_handler() ; // 初始化一个handler 用于处理后台线程的消息
 
		if ( FROM_NET == foxfrom ) {
			html = itt.getStringExtra("html");
			if (null == html) { // 没传入自己下载
				html = "";
				new Thread(new DownTOC()).start();
			}
			data = FoxBookLib.tocHref(html, 16);
		}
		if ( FROM_DB == foxfrom) { // DB
			bookid = itt.getIntExtra("bookid", 0);
			data = FoxDB.getPageList("where bookid=" + bookid); // 获取页面列表
		}

		renderListView();

		init_LV_item_click() ; // 初始化 单击 条目 的行为
		init_LV_item_Long_click() ; // 初始化 长击 条目 的行为
	}

	public boolean onCreateOptionsMenu(Menu menu) { // 菜单初始化
		menu.add(0, 1, 1, "删除所有章节");
		menu.add(0, 2, 2, "删除所有章节且不修改DelList");
		menu.add(0, 3, 3, "添加本书");
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应菜单
		switch (item.getItemId()) {
		case 1:
			FoxDB.delete_Book_All_Pages(bookid, true);
			foxtip("已删除并更新记录");
			setResult(RESULT_OK, (new Intent()).setAction("已清空某书并写入已删除列表"));
			finish();
			break;
		case 2:
			FoxDB.delete_Book_All_Pages(bookid, false);
			foxtip("已删除");
			setResult(RESULT_OK, (new Intent()).setAction("已清空某书并没有写入已删除列表"));
			finish();
			break;
		case 3:
			if ( FROM_NET == foxfrom ) {
				if ( null != bookurl && "" != bookname ) {
					int nBookID = 0 ;
					// 新增入数据库，并获取返回bookid
					nBookID = FoxDB.insertbook(bookname, bookurl);
					if ( nBookID < 1 )
						break ;
					Intent itti = new Intent(Activity_PageList.this, Activity_BookInfo.class);
					itti.putExtra("bookid", nBookID);
					startActivity(itti);
					setResult(RESULT_OK, (new Intent()).setAction("返回列表"));
					finish();
				} else {
					setTitle("信息不完整@新增 : " + bookname + " <" + bookurl + ">");
				}
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public boolean onKeyDown(int keyCoder, KeyEvent event) { // 按键响应
		if (keyCoder == KeyEvent.KEYCODE_BACK) {
			setResult(RESULT_OK, (new Intent()).setAction("返回列表"));
			finish();
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}
