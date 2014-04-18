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
			data = FoxDB.getPageList("order by bookid,id");
		} else {
			data = FoxDB.getPageList("order by bookid,id limit "+ howmany);
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
				String bookurl = FoxDB.getOneCell("select url from book where id=" + tmpbid);

				// setTitle(parent.getItemAtPosition(position).toString());
				Intent intent = new Intent(Activity_AllPageList.this,
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
				@SuppressWarnings("unchecked")
				Map<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				longclickpos = position ;

				lcURL = (String) chapinfol.get("url");
				lcName = (String) chapinfol.get("name");
				lcID = (Integer) chapinfol.get("id");

				setTitle(lcName + " : " + lcURL);

				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("操作:" + lcName);
				builder.setItems(new String[] { "删除本章", "删除本章并不写入Dellist" },
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,  int which) {
								switch (which) {
								case 0:
									FoxDB.delete_Pages(lcID, true);
									Toast.makeText(getApplicationContext(), "已删除并记录: " + lcName, Toast.LENGTH_SHORT).show();
									data.remove(longclickpos); // 位置可能不太靠谱
									adapter.notifyDataSetChanged();
									break;
								case 1:
									FoxDB.delete_Pages(lcID, false);
									Toast.makeText(getApplicationContext(), "已删除: " + lcName, Toast.LENGTH_SHORT).show();
									data.remove(longclickpos);
									adapter.notifyDataSetChanged();
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

}
