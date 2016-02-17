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
	public static FoxMemDB oDB;
	
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	
	SimpleAdapter adapter;
	private Handler handler;

	private static int IS_UPDATEPAGE = 88;
	private static int IS_DOWNTOC = 5;
	private final int IS_DOWNEASOU = 11;
	private final int IS_DOWNZSSQ = 12;
	private final int IS_DOWNKUAIDU = 13;
	private final int IS_QIDIAN_MOBILE = 16;
	
	private String easou_gid_nid = "";
	private int foxfrom = 0; // 1=DB, 2=search
	private String bookurl = "";
	private String bookname = "";
	private int bookid = 0 ;
	private String lcURL, lcName;
	private Integer lcID;
	private int longclickpos = 0;
	
	private int SE_TYPE = 1; // 搜索引擎
	

	public class DownTOC implements Runnable { // 后台线程下载网页
		@Override
		public void run() {
			Message msg = Message.obtain();
			switch(SE_TYPE) {
			case FoxBookLib.SE_QIDIAN_MOBILE : // 起点手机版目录
				msg.what = IS_QIDIAN_MOBILE;
				msg.obj = FoxBookLib.downhtml(bookurl, "utf-8") ;
				break;
			case FoxBookLib.SE_EASOU : // 处理easou搜索书籍，返回书籍地址
				String sJson = FoxBookLib.downhtml(site_easou.getUrlSE(bookname), "utf-8");
				easou_gid_nid = site_easou.json2IDs(sJson,0);
				bookurl = site_easou.getUrlToc(easou_gid_nid);
				sJson = FoxBookLib.downhtml(bookurl, "utf-8");
				msg.what = IS_DOWNEASOU;
				msg.obj = sJson;
				break;
			case FoxBookLib.SE_ZSSQ:
				msg.what = IS_DOWNZSSQ;
				msg.obj = FoxBookLib.downhtml(bookurl, "utf-8") ;
				break;
			case FoxBookLib.SE_QREADER:
				if ( ! bookurl.contains(".qreader.") ) { // 在booklist上搜索快读
					bookurl = site_qreader.qreader_Search(bookname);
				}
				data = site_qreader.qreader_GetIndex(bookurl, 0, 1); // 0 表示所有
				msg.what = IS_DOWNKUAIDU;
				msg.obj = data;
				break;
			default:
				msg.what = IS_DOWNTOC;
				msg.obj = FoxBookLib.downhtml(bookurl);
				break;
			}
			handler.sendMessage(msg);
		}
	}

	private void renderListView() { // 刷新LV
		if (FoxBookLib.FROM_DB == foxfrom) {
			adapter = new SimpleAdapter(this, data, R.layout.lv_item_pagelist,
					new String[] { "name", "count" }, new int[] { R.id.tvName, R.id.tvCount });
			lv_pagelist.setAdapter(adapter);
		} else {
			adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_1,
					new String[] { "name" }, new int[] { android.R.id.text1 });
			lv_pagelist.setAdapter(adapter);
			lv_pagelist.setSelection(adapter.getCount() - 1); // 网络列表跳到尾部
		}
	}

	private void init_LV_item_click() { // 初始化 单击 条目 的行为
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				@SuppressWarnings("unchecked")
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
				intent.putExtra("searchengine", SE_TYPE);
				Activity_ShowPage.oDB = oDB;
				startActivity(intent);
			}
		};
		lv_pagelist.setOnItemClickListener(listener);
	}
	
	private void init_LV_item_Long_click() { // 初始化 长击 条目 的行为
		final Builder builder = new AlertDialog.Builder(this);
		OnItemLongClickListener longlistener = new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				@SuppressWarnings("unchecked")
				Map<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				longclickpos = position ; // base 0

				if ( foxfrom == FoxBookLib.FROM_NET ) { // 从网络下载临时的条目没有ID
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
									FoxMemDBHelper.delete_Pages(lcID, true, oDB);
									data.remove(longclickpos); // 位置可能不太靠谱
									adapter.notifyDataSetChanged();
									foxtip("已删除并记录: " + lcName);
									break;
								case 1:
									FoxMemDBHelper.delete_Pages(lcID, false, oDB);
									data.remove(longclickpos);
									adapter.notifyDataSetChanged();
									foxtip("已删除: " + lcName);
									break;
								case 2:
									FoxMemDBHelper.delete_nowupdown_Pages(lcID, true, true, oDB);
									for ( int i = 0; i<=longclickpos; ++i) {
										data.remove(0);
									}
									adapter.notifyDataSetChanged();
									foxtip("已删除并记录: <= " + lcName);
									break;
								case 3:
									FoxMemDBHelper.delete_nowupdown_Pages(lcID, true, false, oDB);
									for ( int i = 0; i<=longclickpos; ++i) {
										data.remove(0);
									}
									adapter.notifyDataSetChanged();
									foxtip("已删除: <= " + lcName);
									break;
								case 4:
									FoxMemDBHelper.delete_nowupdown_Pages(lcID, false, true, oDB);
									int datasiza = data.size();
									for ( int i = longclickpos; i<datasiza; ++i) {
										data.remove(longclickpos);
									}
									adapter.notifyDataSetChanged();
									foxtip("已删除并记录: >= " + lcName);
									break;
								case 5:
									FoxMemDBHelper.delete_nowupdown_Pages(lcID, false, false, oDB);
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
											FoxBookLib.updatepage(lcID, oDB);
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
				if ( msg.what == IS_DOWNKUAIDU ) {
					data = (List<Map<String, Object>>)msg.obj;
					renderListView();
					return ;
				}
				String sHTTP = (String)msg.obj;
				if ( msg.what == IS_UPDATEPAGE ) { // 更新章节完毕
					setTitle("更新完毕 : " + lcName);
				}
				if ( msg.what == IS_DOWNEASOU ) { // 处理easou json
					data = site_easou.json2PageList(sHTTP, easou_gid_nid, 0);
					renderListView();
				}
				if ( msg.what == IS_DOWNZSSQ ) {
					data = site_zssq.json2PageList(sHTTP, 0);
					renderListView();
				}
				if ( msg.what == IS_QIDIAN_MOBILE ) {
					data = site_qidian.json2PageList(sHTTP);
					renderListView();
				}
				
				if ( msg.what == IS_DOWNTOC ) { // 下载目录完毕
					data = FoxBookLib.tocHref(sHTTP, 0);
					renderListView();
				}
			}
		};
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // 标题栏中添加返回图标
		getActionBar().setDisplayShowHomeEnabled(false); // 隐藏程序图标
	}		// 响应点击事件在onOptionsItemSelected的switch中加入 android.R.id.home   this.finish();
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // 入口
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pagelist);

		showHomeUp();
		lv_pagelist = getListView();

		// 获取传入的数据
		Intent itt = getIntent();
		foxfrom = itt.getIntExtra("iam", 0); // 必需 表明数据从哪来的
		bookurl = itt.getStringExtra("bookurl"); // 必需
		bookname = itt.getStringExtra("bookname"); // 必需
		SE_TYPE = itt.getIntExtra("searchengine", 1) ; // 给出搜索引擎类型

		setTitle(bookname + " : " + bookurl);

		init_handler() ; // 初始化一个handler 用于处理后台线程的消息
 
		if ( FoxBookLib.FROM_NET == foxfrom ) {
			String html = itt.getStringExtra("html");
			if (null == html) { // 没传入自己下载
				new Thread(new DownTOC()).start();
				html = "";
			}
			data = FoxBookLib.tocHref(html, 0);
		}
		if ( FoxBookLib.FROM_DB == foxfrom) { // DB
			bookid = itt.getIntExtra("bookid", 0);
			data = FoxMemDBHelper.getPageList("where bookid=" + bookid, oDB); // 获取页面列表
		}

		renderListView();

		init_LV_item_click() ; // 初始化 单击 条目 的行为
		init_LV_item_Long_click() ; // 初始化 长击 条目 的行为
	}


	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.pagelist, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
				case R.id.pm_Add:
					if ( FoxBookLib.FROM_DB == foxfrom)  // 当是本地数据库时隐藏添加按钮
						menu.getItem(i).setVisible(false);
					break;
				case R.id.pm_cleanBook:
				case R.id.pm_cleanBookND:
					if ( FoxBookLib.FROM_NET == foxfrom ) // 当是网络时隐藏删除按钮
						menu.getItem(i).setVisible(false);
					break;
			}
		}

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应菜单
		switch (item.getItemId()) {
		case android.R.id.home: // 返回图标
			this.finish();
			break;
		case R.id.pm_cleanBook:
			FoxMemDBHelper.delete_Book_All_Pages(bookid, true, oDB);
			foxtip("已删除并更新记录");
			setResult(RESULT_OK, (new Intent()).setAction("已清空某书并写入已删除列表"));
			finish();
			break;
		case R.id.pm_cleanBookND:
			FoxMemDBHelper.delete_Book_All_Pages(bookid, false, oDB);
			foxtip("已删除");
			setResult(RESULT_OK, (new Intent()).setAction("已清空某书并没有写入已删除列表"));
			finish();
			break;
		case R.id.pm_Add:
			if ( FoxBookLib.FROM_NET == foxfrom ) {
				if ( null != bookurl && "" != bookname ) {
					int nBookID = 0 ;
					// 新增入数据库，并获取返回bookid
					nBookID = FoxMemDBHelper.insertbook(bookname, bookurl, oDB);
					if ( nBookID < 1 )
						break ;
					Intent itti = new Intent(Activity_PageList.this, Activity_BookInfo.class);
					itti.putExtra("bookid", nBookID);
					Activity_BookInfo.oDB = oDB;
					startActivity(itti);
					setResult(RESULT_OK, (new Intent()).setAction("返回列表"));
					finish();
				} else {
					setTitle("信息不完整@新增 : " + bookname + " <" + bookurl + ">");
				}
			}
			break;
		case R.id.jumplist_tobottom:
			lv_pagelist.setSelection(adapter.getCount() - 1);
			break;
		case R.id.jumplist_totop:
			lv_pagelist.setSelection(0);
			break;
		case R.id.jumplist_tomiddle:
			lv_pagelist.setSelection((int)( 0.5 * ( adapter.getCount() - 1 ) ));
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
