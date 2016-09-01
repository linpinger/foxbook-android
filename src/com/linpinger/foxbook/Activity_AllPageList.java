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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
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

// 2016-2-17: 本 Activity 只被 BookList 调用
public class Activity_AllPageList extends ListActivity {
	public static FoxMemDB oDB; // 被调用者修改
	
	public static final int SHOW_ALL = 1 ;  // 被其他Activity的调用者调用
	public static final int SHOW_LESS1K = 2 ;
	private int showtypelist = SHOW_ALL ;
	
	SharedPreferences settings;
	private boolean isWhiteActionBar = false; // 是否E-ink设备
	private boolean isUseNewPageView = true; // 使用新的自定义View

	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	SimpleAdapter adapter;
	private Handler handler;
	private static int IS_UPDATEPAGE = 88;

	// 多层类中共享使用
	private String lcURL, lcName;
	private Integer lcID ;
	private int longclickpos = 0;

	private void renderListView() { // 刷新LV
		switch (showtypelist) {
		case SHOW_ALL:
			data = FoxMemDBHelper.getPageList("order by bookid,id", 1, oDB);
			break;
		case SHOW_LESS1K:
			data = FoxMemDBHelper.getPageList("where length(content) < 999 order by bookid,id", oDB);
			break;
		}
		adapter = new SimpleAdapter(this, data,
				R.layout.lv_item_pagelist, new String[] { "name", "count" },
				new int[] { R.id.tvName, R.id.tvCount });
		lv_pagelist.setAdapter(adapter);
	}

	private void init_LV_item_click() { // 初始化 单击 条目 的行为
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				@SuppressWarnings("unchecked")
				Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpid = (Integer) chapinfo.get("id");
				Integer tmpbid = (Integer) chapinfo.get("bookid");
				String bookurl = oDB.getOneCell("select url from book where id=" + tmpbid);

				setTitle(tmpname + " : " + tmpid);
				
				Intent intent;
				isUseNewPageView = settings.getBoolean("isUseNewPageView", isUseNewPageView);
				if ( isUseNewPageView ) {
					intent = new Intent(Activity_AllPageList.this, Activity_ShowPage4Eink.class);
					Activity_ShowPage4Eink.oDB = oDB;
				} else {
					intent = new Intent(Activity_AllPageList.this, Activity_ShowPage.class);
					Activity_ShowPage.oDB = oDB;
				}
				intent.putExtra("iam", SITES.FROM_DB);
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
				@SuppressWarnings("unchecked")
				Map<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				longclickpos = position ;

				lcURL = (String) chapinfol.get("url");
				lcName = (String) chapinfol.get("name");
				lcID = (Integer) chapinfol.get("id");

				setTitle(lcName + " : " + lcURL);

				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("操作:" + lcName);
				builder.setItems(new String[] { "删除本章", "删除本章并不写入Dellist", "删除本章及以上", "删除本章及以下", "更新本章" },
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,  int which) {
								switch (which) {
								case 0:
									FoxMemDBHelper.delete_Pages(lcID, true, oDB);
									foxtip("已删除并记录: " + lcName);
									data.remove(longclickpos); // 位置可能不太靠谱
									adapter.notifyDataSetChanged();
									showItemCountOnTitle();
									break;
								case 1:
									FoxMemDBHelper.delete_Pages(lcID, false, oDB);
									foxtip("已删除: " + lcName);
									data.remove(longclickpos);
									adapter.notifyDataSetChanged();
									showItemCountOnTitle();
									break;
								case 2:
									HashMap<String, Object> nHMa ;
									Integer nIDa;
									for ( int i = 0; i<=longclickpos; ++i) { // 删除数据库记录
										nHMa = (HashMap<String, Object>) data.get(i);
										nIDa = (Integer) nHMa.get("id");
										FoxMemDBHelper.delete_Pages(nIDa, true, oDB);
									}
									for ( int i = 0; i<=longclickpos; ++i) { // 删除数据结构
										data.remove(0);
									}
									adapter.notifyDataSetChanged(); // 通知变更
									showItemCountOnTitle();
									foxtip("已删除并记录: <= " + lcName);
									break;
								case 3:
									HashMap<String, Object> nHMb ;
									Integer nIDb;
									int datasiza = data.size();
									for ( int i = longclickpos; i<datasiza; ++i) { // 删除数据库记录
										nHMb = (HashMap<String, Object>) data.get(i);
										nIDb = (Integer) nHMb.get("id");
										FoxMemDBHelper.delete_Pages(nIDb, true, oDB);
									}
									for ( int i = longclickpos; i<datasiza; ++i) {
										data.remove(longclickpos);
									}
									adapter.notifyDataSetChanged();
									showItemCountOnTitle();
									foxtip("已删除并记录: >= " + lcName);
									break;
								case 4:
									setTitle("正在更新: " + lcName);
									(new Thread(){
										public void run(){
											FoxMemDBHelper.updatepage(lcID, oDB);
									        handler.sendEmptyMessage(IS_UPDATEPAGE);
										}
									}).start();
									break;
								}
								if ( data.size() == 0 ) { // 当记录删除完后，结束本Activity
									exitMe(); // 结束本Activity
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
					setTitle("更新完毕 : " + lcName);
				}
			}
		};
	}
	
	private void showItemCountOnTitle() {
		setTitle("总数: " + String.valueOf(data.size()));
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // 标题栏中添加返回图标
//		getActionBar().setDisplayShowHomeEnabled(false); // 隐藏程序图标
	}		// 响应点击事件在onOptionsItemSelected的switch中加入 android.R.id.home   this.finish();
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // 入口
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isWhiteActionBar = settings.getBoolean("isWhiteActionBar", isWhiteActionBar);
		if ( isWhiteActionBar ) {
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pagelist);
		
		showHomeUp();
		
		showtypelist = getIntent().getIntExtra("apl_showtype", SHOW_ALL); // 通过intent获取数据
		
		lv_pagelist = getListView();

		init_handler() ; // 初始化一个handler 用于处理后台线程的消息
		renderListView();
		showItemCountOnTitle();
		init_LV_item_click() ; // 初始化 单击 条目 的行为
		init_LV_item_Long_click() ; // 初始化 长击 条目 的行为
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case android.R.id.home: // 返回图标
			exitMe();
			break;
		case R.id.allpagelist_delete: // 删除所有章节
			HashMap<String, Object> nHMb ;
			Integer nIDb;
			int datasiza = data.size();
			for ( int i = 0; i<datasiza; ++i) { // 删除数据库记录
				nHMb = (HashMap<String, Object>) data.get(i);
				nIDb = (Integer) nHMb.get("id");
				FoxMemDBHelper.delete_Pages(nIDb, true, oDB);
			}
			FoxMemDBHelper.simplifyAllDelList(oDB); // 精简所有DelList
			for ( int i = 0; i<datasiza; ++i) {
				data.remove(0);
			}
			adapter.notifyDataSetChanged();
			foxtip("已删除所有记录");
			exitMe(); // 结束本Activity
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
		case R.id.apl_finish:
			exitMe();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.allpagelist, menu);
		return true;
	}
	
	public boolean onKeyDown(int keyCoder, KeyEvent event) { // 按键响应
		if (keyCoder == KeyEvent.KEYCODE_BACK) {
			exitMe();
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

	private void exitMe() { // 结束本Activity
		setResult(RESULT_OK, (new Intent()).setAction("返回列表"));
		this.finish();
	}
}
