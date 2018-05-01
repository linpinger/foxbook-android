package com.linpinger.foxbook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.NovelSite;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.Ext_ListActivity_4Eink;
import com.linpinger.tool.ToolBookJava;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

// Activity_ShowPage4Eink : 单击列表，显示内容 aShowPageInMem|aShowPageOnNet, bookIDX, pageIDX, [pageName, pageFullUrl]
// Activity_BookInfo : 添加书籍
public class Activity_PageList extends Ext_ListActivity_4Eink {
	private NovelManager nm;

	SharedPreferences settings;

	private List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
	private ListView lv_pagelist ;
	private int posLongClick = -1 ;
	private boolean isOnLine = true; // 是否在线

	SimpleAdapter adapter;
	private Handler handler;

	private static int IS_UPDATEPAGE = 88;
	private static int IS_RenderListView = 5;

	private int ittAction = 0 ; // 传入的数据
	private int bookIDX = -1 ;
	private String searchBookName = "";
	private String searchBookURL = "";

	public class DownTOC implements Runnable { // 后台线程下载网页
		private String tocURL ;
		public DownTOC(String inURL){
			this.tocURL = inURL;
		}
		@Override
		public void run() {
			if ( tocURL.contains(".if.qidian.com") ) // 在线查看，站点是起点手机时
				ittAction = AC.aListQDPages;
			if ( ittAction == AC.aListQDPages | ittAction == AC.aSearchBookOnQiDian )
				data = new SiteQiDian().getTOC_Android7( ToolBookJava.downhtml(tocURL, "utf-8") );
			if ( ittAction == AC.aListSitePages | ittAction == AC.aSearchBookOnSite )
				data = new NovelSite().getTOC( ToolBookJava.downhtml(tocURL) ); // PageName PageURL
			handler.sendEmptyMessage(IS_RenderListView);
		}
	}

	private void renderListView() { // 刷新LV
		if ( ! isOnLine && data.size() == 0 ) { // 当记录删除完后，结束本Activity
			onBackPressed();
		}
		switch (ittAction) {
		case AC.aListBookPages:
		case AC.aListAllPages:
		case AC.aListLess1KPages:
			adapter = new SimpleAdapter(this, data, R.layout.lv_item_pagelist,
					new String[] { NV.PageName, NV.Size }, new int[] { R.id.tvName, R.id.tvCount });
			lv_pagelist.setAdapter(adapter);
			break;
		case AC.aListSitePages:
		case AC.aListQDPages:
		case AC.aSearchBookOnQiDian:
		case AC.aSearchBookOnSite:
			adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_1,
					new String[] { NV.PageName }, new int[] { android.R.id.text1 });
			lv_pagelist.setAdapter(adapter);
			lv_pagelist.setSelection(adapter.getCount() - 1); // 网络列表跳到尾部
			break;
		default:
			break;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) { // 初始化 单击 条目 的行为
		Map<String, Object> page = (HashMap<String, Object>) l.getItemAtPosition(position);

		Intent itt = new Intent(Activity_PageList.this, Activity_ShowPage4Eink.class);

		switch (ittAction) {
		case AC.aListBookPages:
		case AC.aListAllPages:
		case AC.aListLess1KPages:
			itt.putExtra(AC.action, AC.aShowPageInMem);
			itt.putExtra(NV.BookIDX, (Integer)page.get(NV.BookIDX));
			itt.putExtra(NV.PageIDX, (Integer)page.get(NV.PageIDX));
			if ( ittAction == AC.aListBookPages) {
				String bookURL = nm.getBookInfo(bookIDX).get(NV.BookURL).toString() ;
				if ( bookURL.contains("zip://") )
					itt.putExtra(NV.PageFullURL, bookURL + "@" + page.get(NV.PageURL).toString() ); // 1024DB3
			}
			break;
		case AC.aListSitePages:
			itt.putExtra(AC.action, AC.aShowPageOnNet);
			itt.putExtra(NV.BookIDX, bookIDX);
			itt.putExtra(NV.PageIDX, -1);
			itt.putExtra(NV.PageName, page.get(NV.PageName).toString() );
			itt.putExtra(NV.PageFullURL, ToolBookJava.getFullURL( nm.getBookInfo(bookIDX).get(NV.BookURL).toString(), page.get(NV.PageURL).toString()) );
			break;
		case AC.aListQDPages:
		case AC.aSearchBookOnQiDian:
			itt.putExtra(AC.action, AC.aShowPageOnNet);
			itt.putExtra(NV.BookIDX, bookIDX);
			itt.putExtra(NV.PageIDX, -1);
			itt.putExtra(NV.PageName, page.get(NV.PageName).toString() );
			itt.putExtra(NV.PageFullURL, new SiteQiDian().getContentFullURL_Android7(page.get(NV.PageURL).toString()) );
			break;
		case AC.aSearchBookOnSite:
			itt.putExtra(AC.action, AC.aShowPageOnNet);
			itt.putExtra(NV.BookIDX, bookIDX);
			itt.putExtra(NV.PageIDX, -1);
			itt.putExtra(NV.PageName, page.get(NV.PageName).toString() );
			itt.putExtra(NV.PageFullURL, ToolBookJava.getFullURL( this.searchBookURL, page.get(NV.PageURL).toString()) );
			break;
		default:
			break;
		}
		System.out.println("APL: Action=" + itt.getIntExtra(AC.action, 0)
				+ " bookIDX=" + itt.getIntExtra(NV.BookIDX, -1)
				+ " pageIDX=" + itt.getIntExtra(NV.PageIDX, -1));
		startActivity(itt);
		super.onListItemClick(l, v, position, id);
	}


	private void init_handler() { // 初始化一个handler 用于处理后台线程的消息
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if ( msg.what == IS_UPDATEPAGE ) // 更新章节完毕
					setTitle("更新完毕 : " + (String)msg.obj );
				if ( msg.what == IS_RenderListView ) // 下载目录完毕
					renderListView();
			}
		};
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // 标题栏中添加返回图标
		getActionBar().setDisplayShowHomeEnabled(false); // 隐藏程序图标
	}

	@Override
	public void onCreate(Bundle savedInstanceState) { // 入口
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pagelist);

		showHomeUp();
		lv_pagelist = getListView();
		this.registerForContextMenu(lv_pagelist); // ListView注册上下文菜单

		this.nm = ((FoxApp)this.getApplication()).nm;
		init_handler() ; // 初始化一个handler 用于处理后台线程的消息

		// 获取传入的数据
		Intent itt = getIntent();
		ittAction = itt.getIntExtra(AC.action, AC.aListBookPages);
		bookIDX = itt.getIntExtra(NV.BookIDX, -1);
switch (ittAction) {
		case AC.aListBookPages:
			isOnLine = false ;
			data = nm.getBookPageList( bookIDX ); // 获取页面列表
			Map<String, Object> info = nm.getBookInfo(bookIDX);

			setTitle(info.get(NV.BookName) + " : " + info.get(NV.BookURL));
			break;
		case AC.aListAllPages:
			isOnLine = false ;
			data = nm.getPageList(1);
			setTitle("共 " + String.valueOf(data.size()) + " 章");
			break;
		case AC.aListLess1KPages:
			isOnLine = false ;
			data = nm.getPageList(999);
			setTitle("共 " + String.valueOf(data.size()) + " 章");
			break;
		case AC.aListSitePages:
			new Thread(new DownTOC( nm.getBookInfo(bookIDX).get(NV.BookURL).toString() )).start(); // 在线查看目录
			setTitle("在线看: " + nm.getBookInfo(bookIDX).get(NV.BookName).toString() );
			break;
		case AC.aListQDPages:
			this.searchBookName = nm.getBookInfo(bookIDX).get(NV.BookName).toString();
			this.searchBookURL = itt.getStringExtra(NV.TmpString) ;
			new Thread(new DownTOC(this.searchBookURL)).start(); // 查看起点
			setTitle("起点: " + this.searchBookName );
			break;
		case AC.aSearchBookOnQiDian:
		case AC.aSearchBookOnSite:
			this.searchBookName = itt.getStringExtra(NV.BookName);
			this.searchBookURL = itt.getStringExtra(NV.BookURL);
			new Thread(new DownTOC( this.searchBookURL )).start(); // 搜索 一般TOC
			setTitle("搜索: " + this.searchBookName );
			break;
		default:
			break;
}

		renderListView();
	}


	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.pagelist, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
				case R.id.pm_Add:
					if ( ittAction == AC.aListQDPages |ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite )
						menu.getItem(i).setVisible(true); // 当是搜索时隐藏添加按钮
					else
						menu.getItem(i).setVisible(false);
					break;
				case R.id.pm_cleanBook:
					if ( ittAction == AC.aListQDPages |ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite )
						menu.getItem(i).setVisible(false); // 当是网络时隐藏删除按钮
					else
						menu.getItem(i).setVisible(true);
					break;
				case R.id.pm_cleanBookND:
					if ( ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite
						| ittAction == AC.aListAllPages | ittAction == AC.aListLess1KPages )
						menu.getItem(i).setVisible(false); // 当是网络时隐藏删除按钮
					else
						menu.getItem(i).setVisible(true);
					break;
			}
		}

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应菜单
		switch (item.getItemId()) {
		case android.R.id.home: // 返回图标
			onBackPressed();
			break;
		case R.id.pl_finish:
			onBackPressed();
			break;
		case R.id.pm_cleanBook:
			if ( ittAction == AC.aListAllPages) {
				nm.clearShelf(true);
				nm.simplifyAllDelList(); // 精简所有DelList
			}
			if ( ittAction == AC.aListBookPages )
				nm.clearBook(bookIDX, true);
			if ( ittAction == AC.aListLess1KPages ) { // 倒序一条条删
				for ( int i=data.size()-1; i>=0; i-- )
					nm.clearPage((Integer)data.get(i).get(NV.BookIDX), (Integer)data.get(i).get(NV.PageIDX), true);
			}
			data.clear();
			adapter.notifyDataSetChanged();
			foxtip("已删除所有并更新记录");
			onBackPressed();
			break;
		case R.id.pm_cleanBookND:
			if ( ittAction == AC.aListBookPages )
				nm.clearBook(bookIDX, false);
			foxtip("已删除所有");
			onBackPressed();
			break;
		case R.id.pm_Add:
			if ( "" != this.searchBookURL && "" != this.searchBookName ) {
				int nBookIDX = -1 ;
				nBookIDX = nm.addBook(this.searchBookName, this.searchBookURL, ""); // 新增，并获取返回bookidx
				if ( nBookIDX < 0 )
					break ;

				Intent itti = new Intent(Activity_PageList.this, Activity_BookInfo.class);
				itti.putExtra(NV.BookIDX, nBookIDX);
				startActivity(itti);
				onBackPressed();
			} else {
				setTitle("信息不完整@新增 : " + this.searchBookName + " <" + this.searchBookURL + ">");
			}
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
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
//		if ( bookIDX == -1 ) { // intent传来的bookIDX, 从网络下载临时的条目没有ID
//			foxtip("itent 传入的 bookIDX = -1\n\n从网络下载临时的条目没有ID");
//			return super.onContextItemSelected(item);
//		}

		Map<String, Object> page = data.get(posLongClick);
		final String lcName = page.get(NV.PageName).toString(); // final 便于线程中使用
		final int bookIDX = (Integer) page.get(NV.BookIDX);
		final int pageIDX = (Integer) page.get(NV.PageIDX);

		setTitle(lcName + " : " + page.get(NV.PageURL));

		String itemName = item.getTitle().toString();
		if ( itemName.equalsIgnoreCase("删除本章") ) {
			nm.clearPage(bookIDX, pageIDX, true);
			foxtip("已删除并记录: " + lcName);
		} else if ( itemName.equalsIgnoreCase("删除本章并不写入Dellist") ) {
			nm.clearPage(bookIDX, pageIDX, false);
			foxtip("已删除: " + lcName);
		} else if ( itemName.equalsIgnoreCase("删除本章及以上") ) {
			if ( ittAction == AC.aListBookPages )
				nm.clearBookPages(bookIDX, pageIDX, true, true);
			if ( ittAction == AC.aListAllPages )
				nm.clearShelfPages(bookIDX, pageIDX, true, true);
			foxtip("已删除并记录: <= " + lcName);
		} else if ( itemName.equalsIgnoreCase("删除本章及以上并不写入Dellist") ) {
			if ( ittAction == AC.aListBookPages )
				nm.clearBookPages(bookIDX, pageIDX, true, false);
			if ( ittAction == AC.aListAllPages )
				nm.clearShelfPages(bookIDX, pageIDX, true, false);
			foxtip("已删除: <= " + lcName);
		} else if ( itemName.equalsIgnoreCase("删除本章及以下") ) {
			if ( ittAction == AC.aListBookPages )
				nm.clearBookPages(bookIDX, pageIDX, false, true);
			if ( ittAction == AC.aListAllPages )
				nm.clearShelfPages(bookIDX, pageIDX, false, true);
			foxtip("已删除并记录: >= " + lcName);
		} else if ( itemName.equalsIgnoreCase("删除本章及以下并不写入Dellist") ) {
			if ( ittAction == AC.aListBookPages )
				nm.clearBookPages(bookIDX, pageIDX, false, false);
			if ( ittAction == AC.aListAllPages )
				nm.clearShelfPages(bookIDX, pageIDX, false, false);
			foxtip("已删除: >= " + lcName);
		} else if ( itemName.equalsIgnoreCase("更新本章") ) {
			if ( bookIDX != -1 ) {
				setTitle("正在更新: " + lcName);
				(new Thread(){
					public void run(){
						nm.updatePage(bookIDX, pageIDX);
						updateLocalData(bookIDX) ; // 更新data数据
						Message msg = Message.obtain();
						msg.what = IS_UPDATEPAGE;
						msg.obj = lcName ;
						handler.sendMessage(msg);
					}
				}).start();
			}
		} else if ( itemName.equalsIgnoreCase("编辑本章") ) {
			Intent ittPageInfo = new Intent(Activity_PageList.this,Activity_PageInfo.class);
			ittPageInfo.putExtra(NV.BookIDX, bookIDX);
			ittPageInfo.putExtra(NV.PageIDX, pageIDX);
			startActivity(ittPageInfo);
		} else {
			foxtip("一脸萌圈，还没实现这个菜单呐:\n" + itemName);
		}

		updateLocalData(bookIDX) ; // 更新data数据

		setItemPos4Eink(); // 滚动位置放到头部
		renderListView();

		return super.onContextItemSelected(item);
	}

	void updateLocalData(int inBookIDX) {
		switch (ittAction) { // 更新data数据
		case AC.aListBookPages:
			data = nm.getBookPageList( inBookIDX ); // 获取页面列表
			break;
		case AC.aListAllPages:
			data = nm.getPageList(1);
			break;
		case AC.aListLess1KPages:
			data = nm.getPageList(999);
			break;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		if ( isOnLine ) {
			return ; // 在线的就不显示menu
		}

		posLongClick = ( (AdapterContextMenuInfo) menuInfo ).position; //（position 代表点击列表中第几项出现的上下文菜单）
		menu.setHeaderTitle("操作:" + data.get(posLongClick).get(NV.PageName).toString());

		menu.add("删除本章");
		menu.add("删除本章并不写入Dellist");
		if ( ittAction != AC.aListLess1KPages ) {
		menu.add("删除本章及以上");
		menu.add("删除本章及以上并不写入Dellist");
		menu.add("删除本章及以下");
		menu.add("删除本章及以下并不写入Dellist");
		}
		menu.add("编辑本章");
		menu.add("更新本章");

	}

	@Override
	public void onBackPressed() { // 返回键被按
		setResult(RESULT_OK);
		finish();
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}
