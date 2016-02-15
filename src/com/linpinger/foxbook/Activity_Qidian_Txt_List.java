package com.linpinger.foxbook;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class Activity_Qidian_Txt_List extends ListActivity {
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	SimpleAdapter adapter;
	public FoxMemDB oDB  ; // 默认使用MemDB
	
	private void renderListView() { // 刷新LV
		adapter = new SimpleAdapter(this, data,
				android.R.layout.simple_list_item_1, new String[] { "name" },
				new int[] { android.R.id.text1 });
		lv_pagelist.setAdapter(adapter);
	}
	
	private void init_LV_item_click() { // 初始化 单击 条目 的行为
		OnItemClickListener listener = new OnItemClickListener() {
			@SuppressWarnings("unchecked")
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpid = (Integer) chapinfo.get("id");

				Intent intent = new Intent(Activity_Qidian_Txt_List.this, Activity_ShowPage.class);
				intent.putExtra("iam", FoxBookLib.FROM_DB); // from DB
				intent.putExtra("chapter_id", tmpid);
				intent.putExtra("chapter_name", tmpname);
				intent.putExtra("chapter_url", tmpurl);
				intent.putExtra("searchengine", FoxBookLib.SE_BING); // SE
				Activity_ShowPage.oDB = oDB;
				startActivity(intent);
			}
		};
		lv_pagelist.setOnItemClickListener(listener);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // 入口
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qidian_txt_list);
		lv_pagelist = getListView();
		
		// 获取传入的文件路径
		Intent itt = getIntent();
		String txtPath = itt.getData().getPath(); // 从intent获取txt路径
		oDB = new FoxMemDB(new File(txtPath.replace(".txt", "") + ".db3"), this.getApplicationContext()) ; // 创建内存数据库
		String BookName = FoxMemDBHelper.importQidianTxt(txtPath, oDB); //导入txt到数据库
			
		foxtip("处理:" + txtPath);
		setTitle(BookName);
		
		data = FoxMemDBHelper.getPageList("", oDB); // 获取页面列表
				
		renderListView();  // 处理好data后再刷新列表
		init_LV_item_click() ; // 初始化 单击 条目 的行为
	}
	
	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.db3_txt_viewer, menu);
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case R.id.action_save_exit:
			oDB.closeMemDB();
			this.finish();
			System.exit(0);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
