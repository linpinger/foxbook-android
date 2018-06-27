package com.linpinger.foxbook;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.tool.ToolAndroid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

// Activity_ShowPage4Eink : 单击列表, 显示内容 onListItemClick

public class Activity_EBook_Viewer extends Activity {

	public final int ZIP = 26 ;			// 普通zip文件
	public final int ZIP1024 = 1024 ;	// 1024 html 打包的zip
	public final int EPUB = 500 ;		// 普通 epub文件 处理方式待定
	public final int EPUBFOXMAKE = 506;	// 我生成的 epub
	public final int EPUBQIDIAN = 517;	// 起点 epub
	public final int TXT = 200 ;		// 普通txt
	public final int TXTQIDIAN = 217 ;	// 起点txt

	private int eBookType = ZIP ;		// 处理的zip/epub类型

	private List<Map<String, Object>> data;
	private ListView lv ;
	private TextView info;
	SimpleAdapter adapter;
	private NovelManager nm;

	private File eBookFile ;

	SharedPreferences settings;

	private void renderListView() { // 刷新LV
		adapter = new SimpleAdapter(this, data,
				R.layout.lv_item_pagelist, new String[] { NV.PageName, NV.Size },
				new int[] { R.id.tvName, R.id.tvCount });
		lv.setAdapter(adapter);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) { // 入口
		settings = PreferenceManager.getDefaultSharedPreferences(this);
//		this.setTheme(android.R.style.Theme_Holo_Light_DarkActionBar); // tmp: ActionBar
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ebook_viewer);

		lv = (ListView)this.findViewById(R.id.testLV); // 获取LV
		if ( ! ToolAndroid.isEink() ) {
			lv.setBackgroundColor(Color.parseColor("#EEFAEE"));
		}
		info = (TextView)this.findViewById(R.id.testTV);
		info.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		this.findViewById(R.id.btnToLVBottom).setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				ToolAndroid.jump2ListViewPos(lv, -1) ;
				return true;
			}
		});
		this.findViewById(R.id.btnToLVTop).setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				ToolAndroid.jump2ListViewPos(lv, 0) ;
				return true;
			}
		});

		// 获取传入的文件路径
		Intent itt = getIntent();
		eBookFile = new File(itt.getData().getPath()); // 从intent获取txt/zip/epub路径

		if ( eBookFile.getName().toLowerCase().endsWith(".epub"))
			eBookType = EPUB ;
		if ( eBookFile.getName().toLowerCase().endsWith(".zip"))
			eBookType = ZIP ;
		if ( eBookFile.getName().toLowerCase().endsWith(".txt"))
			eBookType = TXT ;

		if ( eBookType == TXT || eBookType == TXTQIDIAN) { // 显示
			this.findViewById(R.id.btnToUTF8).setVisibility(View.VISIBLE);
		}

		this.nm = new NovelManager(eBookFile);
		((FoxApp)this.getApplication()).nm = this.nm;

		foxtipL(nm.getBookInfo(0).get(NV.BookName).toString());
		data = nm.getPageList(0);
		renderListView(); // 处理好data后再刷新列表

		lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> av, View v, int position, long id) { // 初始化 单击 条目 的行为
				Map<String, Object> page = (HashMap<String, Object>) data.get(position);

				// 跳到阅读页
				Intent itt = new Intent(Activity_EBook_Viewer.this, Activity_ShowPage4Eink.class);
				switch (eBookType) {
				case ZIP:
				case ZIP1024:
					String zipItemName = page.get(NV.PageName).toString();
					foxtipL(zipItemName);
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
					foxtipL(page.get(NV.PageName).toString());
					itt.putExtra(AC.action, AC.aShowPageInMem);
					itt.putExtra(NV.BookIDX, (Integer)page.get(NV.BookIDX)) ;
					itt.putExtra(NV.PageIDX, (Integer)page.get(NV.PageIDX)) ;
					startActivity(itt);
					break;
				default:
					break;
				}
			}
		}); // LV item click end
	} // onCreate end

	public void onBtnClick(View v) {
		switch ( v.getId() ) {
		case R.id.btnSaveExit:
			if ( ! settings.getBoolean("isSaveAsFML", true) )
				nm.setSaveFormat(NovelManager.SQLITE3);
			nm.close();
			this.finish();
			System.exit(0);
			break;
		case R.id.btnCopyInfo:
			Map<String, Object> bk = nm.getBookInfo(0);
			String fbs = "FoxBook>" + bk.get(NV.BookName).toString() + ">"
					+ bk.get(NV.BookAuthor).toString() + ">"
					+ bk.get(NV.QDID).toString() + ">"
					+ bk.get(NV.BookURL) + ">" ;
			ToolAndroid.setClipText(fbs, this);
			foxtip("剪贴板: " + fbs);
			break;
		case R.id.btnToLVBottom:
			ToolAndroid.jump2ListViewPos(lv, -66) ;
			break;
		case R.id.btnToLVTop:
			ToolAndroid.jump2ListViewPos(lv, -99) ;
			break;
		case R.id.btnToUTF8:
			if ( eBookType == TXT || eBookType == TXTQIDIAN) { // 显示
				nm.exportAsTxt(new File(eBookFile.getPath().replace(".txt", "") + "_UTF8.txt")); // Txt GBK->UTF-8
				this.finish();
				System.exit(0);
			} else {
				foxtipL("骚年哟，当前不是txt");
			}
			break;
		}
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
	private void foxtipL(String sinfo) {
		info.setText(sinfo);
	}
}
