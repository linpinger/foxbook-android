package com.linpinger.foxbook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class Activity_AllPageList extends ListActivity {
	public static FoxMemDB oDB;
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	
	private int howmany=0;
	private int foxfrom = 1; // 1=DB, 2=search
	
	SimpleAdapter adapter;
	
	private String lcURL, lcName;
	private Integer lcID ;
	private int longclickpos = 0;

	private void renderListView() { // 刷新LV
		if ( 0 == howmany ) {
			data = FoxMemDBHelper.getPageList("order by bookid,id", oDB);
		} else {
			data = FoxMemDBHelper.getPageList("order by bookid,id limit "+ howmany, oDB);
		}
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
				Integer tmpbid = (Integer) chapinfo.get("bookid");
				String bookurl = oDB.getOneCell("select url from book where id=" + tmpbid);

				// setTitle(parent.getItemAtPosition(position).toString());
				Intent intent = new Intent(Activity_AllPageList.this,
						Activity_ShowPage.class);
				intent.putExtra("iam", foxfrom);
				intent.putExtra("chapter_id", tmpid);
				intent.putExtra("chapter_name", tmpname);
				intent.putExtra("chapter_url", FoxBookLib.getFullURL(bookurl, tmpurl));
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
				longclickpos = position ;

				lcURL = (String) chapinfol.get("url");
				lcName = (String) chapinfol.get("name");
				lcID = (Integer) chapinfol.get("id");

				setTitle(lcName + " : " + lcURL);

				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("操作:" + lcName);
				builder.setItems(new String[] { "删除本章", "删除本章并不写入Dellist", "删除本章及以上", "删除本章及以下" },
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,  int which) {
								switch (which) {
								case 0:
									FoxMemDBHelper.delete_Pages(lcID, true, oDB);
									foxtip("已删除并记录: " + lcName);
									data.remove(longclickpos); // 位置可能不太靠谱
									adapter.notifyDataSetChanged();
									break;
								case 1:
									FoxMemDBHelper.delete_Pages(lcID, false, oDB);
									foxtip("已删除: " + lcName);
									data.remove(longclickpos);
									adapter.notifyDataSetChanged();
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
									foxtip("已删除并记录: >= " + lcName);
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
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // 入口
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_allpagelist);
		
		
		lv_pagelist = getListView();
		
		// 获取传入的数据
		Intent itt = getIntent();
		howmany = itt.getIntExtra("howmany", 0); // 必需 表明 显示多少条目, 0为所有

		renderListView();
		init_LV_item_click() ; // 初始化 单击 条目 的行为
		init_LV_item_Long_click() ; // 初始化 长击 条目 的行为
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

	private void exitMe() { // 结束本Activity
		setResult(RESULT_OK, (new Intent()).setAction("已清空所有列表"));
		this.finish();
	}
}
