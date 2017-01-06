package com.linpinger.foxbook;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.tool.Ext_ListActivity_4Eink;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

// Activity_ShowPage4Eink : 单击列表, 显示内容   onListItemClick

public class Activity_EBook_Viewer extends Ext_ListActivity_4Eink {
	
	public final int ZIP = 26 ;        // 普通zip文件
	public final int ZIP1024 = 1024 ;  // 1024 html 打包的zip
	public final int EPUB = 500 ;      // 普通 epub文件 处理方式待定
	public final int EPUBFOXMAKE = 506; //我生成的 epub
	public final int EPUBQIDIAN = 517; // 起点 epub
	public final int TXT = 200 ;       // 普通txt
	public final int TXTQIDIAN = 217 ; // 起点txt
	
	private int eBookType = ZIP ;  // 处理的zip/epub类型
	
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	SimpleAdapter adapter;
	private NovelManager nm;

	private File eBookFile ;

	SharedPreferences settings;

	private void renderListView() { // 刷新LV
		adapter = new SimpleAdapter(this, data,
				R.layout.lv_item_pagelist, new String[] { NV.PageName, NV.Size },
				new int[] { R.id.tvName, R.id.tvCount });
		lv_pagelist.setAdapter(adapter);
	}
	

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(settings.getBoolean("isClickHomeExit", false));  // 标题栏中添加返回图标
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // 入口
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ebook_viewer);
		showHomeUp();
		
		lv_pagelist = getListView();
		
		// 获取传入的文件路径
		Intent itt = getIntent();
		eBookFile = new File(itt.getData().getPath());  // 从intent获取txt/zip/epub路径

		if ( eBookFile.getName().toLowerCase().endsWith(".epub"))
			eBookType = EPUB ;
		if ( eBookFile.getName().toLowerCase().endsWith(".zip"))
			eBookType = ZIP ;
		if ( eBookFile.getName().toLowerCase().endsWith(".txt"))
			eBookType = TXT ;
		
		this.nm = new NovelManager(eBookFile);
		((FoxApp)this.getApplication()).nm = this.nm;

		setTitle(nm.getBookInfo(0).get(NV.BookName).toString());
		data = nm.getPageList(0);
		renderListView();  // 处理好data后再刷新列表

	} // onCreate end

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Map<String, Object> page = (HashMap<String, Object>) data.get(position);

		// 跳到阅读页
		Intent itt  = new Intent(Activity_EBook_Viewer.this, Activity_ShowPage4Eink.class);
		switch (eBookType) {
		case ZIP:
		case ZIP1024:
			String zipItemName = page.get(NV.PageName).toString();
			setTitle(zipItemName);
			if ( zipItemName.endsWith(".html") | zipItemName.endsWith(".htm") ) {
				itt.putExtra(NV.PageFullURL, nm.getBookInfo(0).get(NV.BookURL) + "@" + zipItemName);
		        itt.putExtra(AC.action, AC.aShowPageInZip1024);
				startActivity(itt);
			} else {
				foxtip("暂不支持这种格式的直接查看");
			}
			break;
		case EPUB:
		case EPUBQIDIAN:
		case EPUBFOXMAKE:
		case TXT:
		case TXTQIDIAN:
			setTitle(page.get(NV.PageName).toString());
			itt.putExtra(AC.action, AC.aShowPageInMem);
			itt.putExtra(NV.BookIDX, (Integer)page.get(NV.BookIDX)) ;
			itt.putExtra(NV.PageIDX, (Integer)page.get(NV.PageIDX)) ;
			startActivity(itt);
			break;
		default:
			break;
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.ebook_viewer, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
			case R.id.action_gbk2utf8:
				if ( eBookType == TXT || eBookType == TXTQIDIAN)
					menu.getItem(i).setVisible(true) ; // 显示
				else
					menu.getItem(i).setVisible(false) ; // 非txt隐藏
				break;
			}
		}
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case android.R.id.home:
			this.finish();
			System.exit(0);
			break;
		case R.id.action_save_exit:
			if ( ! settings.getBoolean("isSaveAsFML", true) )
				nm.setSaveFormat(NovelManager.SQLITE3);
			nm.close();
			this.finish();
			System.exit(0);
			break;
		case R.id.jumplist_tobottom:
			lv_pagelist.setSelection(adapter.getCount() - 1);
			setItemPos4Eink(adapter.getCount() - 1);
			break;
		case R.id.jumplist_totop:
			lv_pagelist.setSelection(0);
			setItemPos4Eink(); // 滚动位置放到头部
			break;
		case R.id.jumplist_tomiddle:
			int midPos = adapter.getCount() / 2 - 1 ;
			lv_pagelist.setSelection(midPos);
			setItemPos4Eink(midPos);
			break;
		case R.id.action_gbk2utf8:  // Txt GBK->UTF-8
			nm.exportAsTxt(new File(eBookFile.getPath().replace(".txt", "") + "_UTF8.txt"));
			this.finish();
			System.exit(0);
			break;
		case R.id.settingUI:
			startActivity(new Intent(Activity_EBook_Viewer.this, Activity_Setting.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}
