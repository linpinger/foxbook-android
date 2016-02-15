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
import android.widget.AdapterView.OnItemClickListener;

public class Activity_DB3_Viewer extends ListActivity  {
	ListView lv_booklist;
	public FoxMemDB oDB  ; // 默认使用MemDB
	List<Map<String, Object>> data;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_booklist);
		
		lv_booklist = getListView(); // 获取LV
		
		// 获取传入的路径并初始化oDB
		Intent itt = getIntent();
		oDB = new FoxMemDB(new File(itt.getData().getPath()), this.getApplicationContext()) ; // 创建内存数据库
		
		refresh_BookList(); // 刷新LV中的数据
		init_LV_item_click(); // 初始化 单击 条目 的行为
	}
	
	private void refresh_BookList() { // 刷新LV中的数据
		data = FoxMemDBHelper.getBookList(oDB); // 获取书籍列表
		SimpleAdapter adapter = new SimpleAdapter(this, data,
				R.layout.lv_item_booklist, new String[] { "name", "count" },
				new int[] { R.id.tvName, R.id.tvCount });
		lv_booklist.setAdapter(adapter);
	}
	
	private void init_LV_item_click() { // 初始化 单击 条目 的行为
		OnItemClickListener listener = new OnItemClickListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
				HashMap<String, Object> chapinfo = (HashMap<String, Object>) arg0.getItemAtPosition(arg2);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpcount = Integer.parseInt((String) chapinfo.get("count"));
				Integer tmpid = (Integer) chapinfo.get("id");
				setTitle(tmpname + " : " + tmpurl);

				if (tmpcount > 0) {
					Intent intent = new Intent(Activity_DB3_Viewer.this, Activity_PageList.class);
					intent.putExtra("iam", FoxBookLib.FROM_DB);
					intent.putExtra("bookurl", tmpurl);
					intent.putExtra("bookname", tmpname);
					intent.putExtra("bookid", tmpid);
					Activity_PageList.oDB = oDB;
					startActivityForResult(intent, 0);
				}
			}
		};
		lv_booklist.setOnItemClickListener(listener);
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
