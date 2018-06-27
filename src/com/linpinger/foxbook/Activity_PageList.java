package com.linpinger.foxbook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.NovelSite;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.ToolAndroid;
import com.linpinger.tool.ToolBookJava;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

// Activity_ShowPage4Eink : 单击列表，显示内容 aShowPageInMem|aShowPageOnNet, bookIDX, pageIDX, [pageName, pageFullUrl]
// Activity_BookInfo : 添加书籍
public class Activity_PageList extends Activity {
	private NovelManager nm;

	SharedPreferences settings;
	private List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
	private ListView lv ;
	private TextView info;
	private int posLongClick = -1 ;
	private float clickX = 0 ; // 用来确定点击横坐标以实现不同LV不同区域点击效果
	private boolean isOnLine = true; // 是否在线

	SimpleAdapter adapter;
	private Handler handler;

	private static int IS_UPDATEPAGE = 88;
	private static int IS_RenderListView = 5;

	private int ittAction = 0 ; // 传入的数据
	private int bookIDX = -1 ;
	private String searchBookName = "";
	private String searchBookURL = "";

	@Override
		public void onCreate(Bundle savedInstanceState) { // 入口
//			requestWindowFeature(Window.FEATURE_NO_TITLE);
//			this.setTheme(android.R.style.Theme_Holo_Light_NoActionBar); // 无ActionBar
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_pagelist);

			settings = PreferenceManager.getDefaultSharedPreferences(this);
			lv = (ListView)this.findViewById(R.id.testLV); // 获取LV
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

			if ( ! ToolAndroid.isEink() ) {
				lv.setBackgroundColor(Color.parseColor("#EEFAEE"));
			}
			lv.setOnTouchListener(new OnTouchListener(){
				@Override
				public boolean onTouch(View v, MotionEvent arg1) {
					clickX = arg1.getX(); // 点击横坐标
					return false;
				}
			});
			init_LV_item_Click();
			init_LV_item_Long_click(); // 初始化 长击 条目 的行为
	
			this.nm = ((FoxApp)this.getApplication()).nm;
			init_handler() ; // 初始化一个handler 用于处理后台线程的消息
	
			// 获取传入的数据
			Intent itt = getIntent();
			ittAction = itt.getIntExtra(AC.action, AC.aListBookPages);
			bookIDX = itt.getIntExtra(NV.BookIDX, -1);
	switch (ittAction) {
			case AC.aListBookPages:
				this.findViewById(R.id.btnAddBook).setVisibility(View.GONE); // 隐藏按钮
				isOnLine = false ;
				data = nm.getBookPageList( bookIDX ); // 获取页面列表
				Map<String, Object> info = nm.getBookInfo(bookIDX);
	
				foxtipL(info.get(NV.BookName) + " : " + info.get(NV.BookURL));
				break;
			case AC.aListAllPages:
				this.findViewById(R.id.btnAddBook).setVisibility(View.GONE);
				this.findViewById(R.id.btnCleanBookND).setVisibility(View.GONE);
				isOnLine = false ;
				data = nm.getPageList(1);
				foxtipL("共 " + String.valueOf(data.size()) + " 章");
				break;
			case AC.aListLess1KPages:
				this.findViewById(R.id.btnAddBook).setVisibility(View.GONE);
				this.findViewById(R.id.btnCleanBookND).setVisibility(View.GONE);
				isOnLine = false ;
				data = nm.getPageList(999);
				foxtipL("共 " + String.valueOf(data.size()) + " 章");
				break;
			case AC.aListSitePages:
				this.findViewById(R.id.btnAddBook).setVisibility(View.GONE);
				new Thread(new DownTOC( nm.getBookInfo(bookIDX).get(NV.BookURL).toString() )).start(); // 在线查看目录
				foxtipL("在线看: " + nm.getBookInfo(bookIDX).get(NV.BookName).toString() );
				break;
			case AC.aListQDPages:
				this.findViewById(R.id.btnCleanBook).setVisibility(View.GONE);
				this.searchBookName = nm.getBookInfo(bookIDX).get(NV.BookName).toString();
				this.searchBookURL = itt.getStringExtra(NV.TmpString) ;
				new Thread(new DownTOC(this.searchBookURL)).start(); // 查看起点
				foxtipL("起点: " + this.searchBookName );
				break;
			case AC.aSearchBookOnQiDian:
			case AC.aSearchBookOnSite:
				this.findViewById(R.id.btnCleanBook).setVisibility(View.GONE);
				this.findViewById(R.id.btnCleanBookND).setVisibility(View.GONE);
				this.searchBookName = itt.getStringExtra(NV.BookName);
				this.searchBookURL = itt.getStringExtra(NV.BookURL);
				new Thread(new DownTOC( this.searchBookURL )).start(); // 搜索 一般TOC
				foxtipL("搜索: " + this.searchBookName );
				break;
			default:
				break;
	}
			renderListView();
		} // onCreate end

	void init_LV_item_Click() {
		lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> av, View v, int position, long id) { // 初始化 单击 条目 的行为

				if ( settings.getBoolean("isClickItemRightAct", true) && ! isOnLine ) {
					if ( clickX > lv.getWidth() * 0.8 ) { // 右边1/5处弹出菜单
						posLongClick = position;
						lvItemLongClickDialog();
						return ;
					}
				}

			Map<String, Object> page = (HashMap<String, Object>) av.getItemAtPosition(position);

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
				itt.putExtra(NV.PageFullURL, ToolBookJava.getFullURL( searchBookURL, page.get(NV.PageURL).toString()) );
				break;
			default:
				break;
			}
			System.out.println("APL: Action=" + itt.getIntExtra(AC.action, 0)
					+ " bookIDX=" + itt.getIntExtra(NV.BookIDX, -1)
					+ " pageIDX=" + itt.getIntExtra(NV.PageIDX, -1));
			startActivity(itt);
		}
		}); // LV item click end
	}

	private void lvItemLongClickDialog() { // 长击LV条目弹出的对话框
		if ( isOnLine ) {
			foxtipL("在线不显示menu的哟");
			return ; // 在线的就不显示menu
		}

		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("操作:" + data.get(posLongClick).get(NV.PageName).toString());
		final ArrayList<String> menu = new ArrayList<String>();
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
		ListAdapter listdapter = new ArrayAdapter<String>(lv.getContext(), android.R.layout.simple_list_item_1, menu);

		builder.setAdapter(listdapter, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int which) {
				Map<String, Object> page = data.get(posLongClick);
				final String lcName = page.get(NV.PageName).toString(); // final 便于线程中使用
				final int bookIDX = (Integer) page.get(NV.BookIDX);
				final int pageIDX = (Integer) page.get(NV.PageIDX);
				foxtipL(lcName + " : " + page.get(NV.PageURL));

				String itemName = menu.get(which).toString();
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
						foxtipL("正在更新: " + lcName);
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

//						setItemPos4Eink(); // 滚动位置放到头部
				renderListView();
			}
		});
		builder.create().show();
	}

	private void init_LV_item_Long_click() { // 初始化 长击 条目 的行为
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> adptv, View view, int position, long id) {
				posLongClick = position;
				lvItemLongClickDialog();
				return true;
			}
		});
	}

	public void onBtnClick(View v) {
		switch ( v.getId() ) {
		case R.id.btnAddBook:
			if ( ittAction == AC.aListQDPages |ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite ) {
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
					foxtipL("信息不完整@新增 : " + this.searchBookName + " <" + this.searchBookURL + ">");
				}
			} else {
				foxtipL("骚年，当前添加功能不可用哟"); // 当是搜索时隐藏添加按钮
			}
			break;
		case R.id.btnCleanBook:
			if ( ittAction == AC.aListQDPages |ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite ) {
				foxtipL("骚年，当前是网络模式，删除功能不可用哟"); // 当是网络时隐藏删除按钮
				break;
			}
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
		case R.id.btnCleanBookND:
			if ( ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite
			| ittAction == AC.aListAllPages | ittAction == AC.aListLess1KPages ) {
				foxtipL("骚年，当前是网络模式，删除功能不可用哟"); // 当是网络时隐藏删除按钮
				break;
			}
			if ( ittAction == AC.aListBookPages )
				nm.clearBook(bookIDX, false);
			foxtip("已删除所有");
			onBackPressed();
			break;
		case R.id.btnToLVBottom:
			ToolAndroid.jump2ListViewPos(lv, -66) ;
			break;
		case R.id.btnToLVTop:
			ToolAndroid.jump2ListViewPos(lv, -99) ;
			break;
		}
	}

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
			lv.setAdapter(adapter);
			break;
		case AC.aListSitePages:
		case AC.aListQDPages:
		case AC.aSearchBookOnQiDian:
		case AC.aSearchBookOnSite:
			adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_1,
					new String[] { NV.PageName }, new int[] { android.R.id.text1 });
			lv.setAdapter(adapter);
			lv.setSelection(adapter.getCount() - 1); // 网络列表跳到尾部
			break;
		default:
			break;
		}
	}

	private void init_handler() { // 初始化一个handler 用于处理后台线程的消息
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if ( msg.what == IS_UPDATEPAGE ) // 更新章节完毕
					foxtipL("更新完毕 : " + (String)msg.obj );
				if ( msg.what == IS_RenderListView ) // 下载目录完毕
					renderListView();
			}
		};
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
	public void onBackPressed() { // 返回键被按
		setResult(RESULT_OK);
		finish();
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
	private void foxtipL(String sinfo) {
		info.setText(sinfo);
//		info.setVisibility(View.VISIBLE);
	}
}
