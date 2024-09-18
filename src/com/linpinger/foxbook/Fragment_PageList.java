package com.linpinger.foxbook;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.misc.BackHandledFragment;
import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.NovelSite;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.FoxHTTP;
import com.linpinger.tool.ToolAndroid;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Fragment_PageList extends BackHandledFragment {
	private NovelManager nm;

	SharedPreferences settings;
	private List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
	private ListView lv ;
	private TextView tv;
	private int posLongClick = -1 ;
	private float clickX = 0 ; // 用来确定点击横坐标以实现不同LV不同区域点击效果
	private boolean isOnLine = true; // 是否在线

	SimpleAdapter adapter;


	private int ittAction = 0 ; // 传入的数据
	private int bookIDX = -1 ;
	private String searchBookName = "";
	private String searchBookURL = "";

	public static Fragment_PageList newInstance(NovelManager novelMgr, Bundle bd) {
		Fragment_PageList frgmt = new Fragment_PageList();
		frgmt.nm = novelMgr;
		frgmt.setArguments(bd);
		return frgmt;
	}
	public static Fragment_PageList newInstance(NovelManager novelMgr, int action) {
		Fragment_PageList frgmt = new Fragment_PageList();
		frgmt.nm = novelMgr;
		Bundle bd = new Bundle();
		bd.putInt(AC.action, action);
		frgmt.setArguments(bd);
		return frgmt;
	}
	public static Fragment_PageList newInstance(NovelManager novelMgr, int action, int bookIDX) {
		Fragment_PageList frgmt = new Fragment_PageList();
		frgmt.nm = novelMgr;
		Bundle bd = new Bundle();
		bd.putInt(AC.action, action);
		bd.putInt(NV.BookIDX, bookIDX);
		frgmt.setArguments(bd);
		return frgmt;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ctx = container.getContext();
		View v = inflater.inflate(R.layout.fragment_pagelist, container, false); // 这个false很重要，不然会崩溃

		settings = PreferenceManager.getDefaultSharedPreferences(ctx);
		lv = (ListView)v.findViewById(R.id.testLV); // 获取LV
		tv = (TextView)v.findViewById(R.id.testTV);
		v.findViewById(R.id.btnAddBook).setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				ToolAndroid.setClipText(searchBookURL, ctx);
				foxtip("网址已复制到剪贴板\n" + searchBookURL);
				return true;
			}
		});
		v.findViewById(R.id.btnToLVBottom).setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				ToolAndroid.jump2ListViewPos(lv, -1) ;
				return true;
			}
		});
		v.findViewById(R.id.btnToLVTop).setOnLongClickListener(new OnLongClickListener(){
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

		if ( nm == null ) {
			foxtip("警告: nm == null");
		}
		// 获取传入的数据
		Bundle bd = getArguments();
		ittAction = bd.getInt(AC.action, AC.aListBookPages);
		bookIDX = bd.getInt(NV.BookIDX, -1);
		switch (ittAction) {
		case AC.aListBookPages:
			v.findViewById(R.id.btnAddBook).setVisibility(View.GONE); // 隐藏按钮
			isOnLine = false ;
			data = nm.getBookPageList( bookIDX ); // 获取页面列表
			handler.sendEmptyMessage(IS_RenderListView);
			Map<String, Object> info = nm.getBookInfo(bookIDX);

			foxtipL(info.get(NV.BookName) + " : " + info.get(NV.BookURL));
			break;
		case AC.aListAllPages:
			v.findViewById(R.id.btnAddBook).setVisibility(View.GONE);
			v.findViewById(R.id.btnCleanBookND).setVisibility(View.GONE);
			isOnLine = false ;
			data = nm.getPageList(1);
			handler.sendEmptyMessage(IS_RenderListView);
			foxtipL(nm.getShelfFile().getName() + " 共 " + String.valueOf(data.size()) + " 章");
			break;
		case AC.aListLess1KPages:
			v.findViewById(R.id.btnAddBook).setVisibility(View.GONE);
			v.findViewById(R.id.btnCleanBookND).setVisibility(View.GONE);
			isOnLine = false ;
			data = nm.getPageList(999);
			handler.sendEmptyMessage(IS_RenderListView);
			foxtipL(nm.getShelfFile().getName() + " 共 " + String.valueOf(data.size()) + " 章");
			break;
		case AC.aListMore1KPages:
			v.findViewById(R.id.btnAddBook).setVisibility(View.GONE);
			v.findViewById(R.id.btnCleanBookND).setVisibility(View.GONE);
			isOnLine = false ;
			data = nm.getPageList(1001);
			handler.sendEmptyMessage(IS_RenderListView);
			foxtipL(nm.getShelfFile().getName() + " 共 " + String.valueOf(data.size()) + " 章");
			break;
		case AC.aListSitePages:
			v.findViewById(R.id.btnAddBook).setVisibility(View.GONE);
			new Thread(new DownTOC( nm.getBookInfo(bookIDX).get(NV.BookURL).toString() )).start(); // 在线查看目录
			foxtipL("在线看: " + nm.getBookInfo(bookIDX).get(NV.BookName).toString() );
			break;
		case AC.aListQDPages:
			v.findViewById(R.id.btnCleanBook).setVisibility(View.GONE);
			this.searchBookName = nm.getBookInfo(bookIDX).get(NV.BookName).toString();
			this.searchBookURL = bd.getString(NV.TmpString) ;
			new Thread(new DownTOC(this.searchBookURL)).start(); // 查看起点
			foxtipL("起点: " + this.searchBookName );
			break;
		case AC.aSearchBookOnQiDian:
		case AC.aSearchBookOnSite:
			v.findViewById(R.id.btnCleanBook).setVisibility(View.GONE);
			v.findViewById(R.id.btnCleanBookND).setVisibility(View.GONE);
			this.searchBookName = bd.getString(NV.BookName);
			this.searchBookURL = bd.getString(NV.BookURL);
			new Thread(new DownTOC( this.searchBookURL )).start(); // 搜索 一般TOC
			foxtipL("搜索: " + this.searchBookName + " : " + searchBookURL);
			break;
		default:
			break;
		}

		onViewClickListener cl = new onViewClickListener();
		tv.setOnClickListener(cl);
		v.findViewById(R.id.btnAddBook).setOnClickListener(cl);
		v.findViewById(R.id.btnCleanBook).setOnClickListener(cl);
		v.findViewById(R.id.btnCleanBookND).setOnClickListener(cl);
		v.findViewById(R.id.btnToLVBottom).setOnClickListener(cl);
		v.findViewById(R.id.btnToLVTop).setOnClickListener(cl);

		return v;
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

				Bundle arg = new Bundle();

				switch (ittAction) {
				case AC.aListBookPages:
				case AC.aListAllPages:
				case AC.aListLess1KPages:
				case AC.aListMore1KPages:
					arg.putInt(AC.action, AC.aShowPageInMem);
					arg.putInt(NV.BookIDX, (Integer)page.get(NV.BookIDX));
					arg.putInt(NV.PageIDX, (Integer)page.get(NV.PageIDX));
					if ( ittAction == AC.aListBookPages) {
						String bookURL = nm.getBookInfo(bookIDX).get(NV.BookURL).toString() ;
						if ( bookURL.contains("zip://") )
							arg.putString(NV.PageFullURL, bookURL + "@" + page.get(NV.PageURL).toString() ); // 1024DB3
					}
					break;
				case AC.aListSitePages:
					arg.putInt(AC.action, AC.aShowPageOnNet);
					arg.putInt(NV.BookIDX, bookIDX);
					arg.putInt(NV.PageIDX, -1);
					arg.putString(NV.PageName, page.get(NV.PageName).toString() );
					arg.putString(NV.PageFullURL, FoxHTTP.getFullURL( nm.getBookInfo(bookIDX).get(NV.BookURL).toString(), page.get(NV.PageURL).toString()) );
					break;
				case AC.aListQDPages:
				case AC.aSearchBookOnQiDian:
					arg.putInt(AC.action, AC.aShowPageOnNet);
					arg.putInt(NV.BookIDX, bookIDX);
					arg.putInt(NV.PageIDX, -1);
					arg.putString(NV.PageName, page.get(NV.PageName).toString() );
					arg.putString(NV.PageFullURL, page.get(NV.PageURL).toString() );
					break;
				case AC.aSearchBookOnSite:
					arg.putInt(AC.action, AC.aShowPageOnNet);
					arg.putInt(NV.BookIDX, bookIDX);
					arg.putInt(NV.PageIDX, -1);
					arg.putString(NV.PageName, page.get(NV.PageName).toString() );
					arg.putString(NV.PageFullURL, FoxHTTP.getFullURL( searchBookURL, page.get(NV.PageURL).toString()) );
					break;
				default:
					break;
				}
				System.out.println("APL: Action=" + arg.getInt(AC.action, 0)
						+ " bookIDX=" + arg.getInt(NV.BookIDX, -1)
						+ " pageIDX=" + arg.getInt(NV.PageIDX, -1));

				startFragment( Fragment_ShowPage4Eink.newInstance(nm, arg) );
			}
		}); // LV item click end
	}

	private void lvItemLongClickDialog() { // 长击LV条目弹出的对话框
		if ( isOnLine ) {
			foxtipL("在线不显示menu的哟");
			return ; // 在线的就不显示menu
		}

		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle("操作:" + data.get(posLongClick).get(NV.PageName).toString());
		final ArrayList<String> menu = new ArrayList<String>();
		menu.add("在线查看");
		menu.add("更新本章");
		menu.add("删除本章");
		menu.add("删除本章并不写入Dellist");
		if ( ittAction != AC.aListLess1KPages && ittAction != AC.aListMore1KPages ) {
			menu.add("删除本章及以上");
			menu.add("删除本章及以上并不写入Dellist");
			menu.add("删除本章及以下");
			menu.add("删除本章及以下并不写入Dellist");
		}
		menu.add("编辑本章");
		menu.add("复制本章URL");
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
						(new Thread() { public void run() {
							nm.updatePage(bookIDX, pageIDX);
							updateLocalData(bookIDX) ; // 更新data数据
							handler.obtainMessage(IS_UPDATEPAGE, lcName).sendToTarget();
						}}).start();
					}
				} else if ( itemName.equalsIgnoreCase("编辑本章") ) {
					startFragment( Fragment_PageInfo.newInstance(nm, bookIDX, pageIDX) );
				} else if ( itemName.equalsIgnoreCase("复制本章URL") ) {
					String fullPageURL = FoxHTTP.getFullURL( nm.getBookInfo(bookIDX).get(NV.BookURL).toString(), page.get(NV.PageURL).toString());
					ToolAndroid.setClipText(fullPageURL, ctx);
					foxtip("已复制到剪贴板:\n" + fullPageURL);
				} else if ( itemName.equalsIgnoreCase("在线查看") ) {
					String fullPageURL = FoxHTTP.getFullURL( nm.getBookInfo(bookIDX).get(NV.BookURL).toString(), page.get(NV.PageURL).toString());
					ToolAndroid.setClipText(fullPageURL, ctx);
					startFragment( Fragment_SearchBook.newInstance(nm) );
				} else {
					foxtip("一脸萌圈，还没实现这个菜单呐:\n" + itemName);
				}

				updateLocalData(bookIDX) ; // 更新data数据
				handler.sendEmptyMessage(IS_RenderListView);
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

	private class onViewClickListener implements View.OnClickListener { // 单击
		@Override
		public void onClick(View v) {
			switch ( v.getId() ) {
			case R.id.testTV:
				back();
				break;
			case R.id.btnAddBook:
				if ( ittAction == AC.aListQDPages |ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite ) {
					if ( "" != searchBookURL && "" != searchBookName ) {
						int nBookIDX = -1 ;
						nBookIDX = nm.addBook(searchBookName, searchBookURL, ""); // 新增，并获取返回bookidx
						if ( nBookIDX < 0 )
							break ;

						startFragment( Fragment_BookInfo.newInstance(nm, nBookIDX) );
						foxtip("已添加: " + searchBookName + " : " + searchBookURL);
						back();
					} else {
						foxtipL("信息不完整@新增 : " + searchBookName + " <" + searchBookURL + ">");
					}
				} else {
					foxtipL("骚年，当前添加功能不可用哟"); // 当是搜索时隐藏添加按钮
				}
				break;
			case R.id.btnCleanBook:
				if ( ittAction == AC.aListQDPages |ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite | ittAction == AC.aListMore1KPages ) {
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
				back();
				break;
			case R.id.btnCleanBookND:
				if ( ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite
				| ittAction == AC.aListAllPages | ittAction == AC.aListLess1KPages | ittAction == AC.aListMore1KPages ) {
					foxtipL("骚年，当前是网络模式，删除功能不可用哟"); // 当是网络时隐藏删除按钮
					break;
				}
				if ( ittAction == AC.aListBookPages )
					nm.clearBook(bookIDX, false);
				foxtip("已删除所有");
				back();
				break;
			case R.id.btnToLVBottom:
				ToolAndroid.jump2ListViewPos(lv, -66) ;
				break;
			case R.id.btnToLVTop:
				ToolAndroid.jump2ListViewPos(lv, -99) ;
				break;
			}
		}
	}

	public class DownTOC implements Runnable { // 后台线程下载网页
		private String tocURL ;
		public DownTOC(String inURL){
			this.tocURL = inURL;
		}
		@Override
		public void run() {
			if ( SiteQiDian.isQidanTOCURL_Touch8(tocURL) ) // 在线查看，站点是起点手机时
				ittAction = AC.aListQDPages;
			if ( ittAction == AC.aListQDPages | ittAction == AC.aSearchBookOnQiDian )
				data = new SiteQiDian().getTOC_Touch8( new FoxHTTP(tocURL).getHTML("UTF-8") );
			if ( ittAction == AC.aListSitePages | ittAction == AC.aSearchBookOnSite )
				data = new NovelSite().getTOC( new FoxHTTP(tocURL).getHTML() ); // PageName PageURL

			handler.sendEmptyMessage(IS_RenderListView);
		}
	}

	private void renderListView() { // 刷新LV
		switch (ittAction) {
		case AC.aListBookPages:
		case AC.aListAllPages:
		case AC.aListLess1KPages:
		case AC.aListMore1KPages:
			adapter = new SimpleAdapter(ctx, data, R.layout.lv_item_pagelist,
					new String[] { NV.PageName, NV.Size }, new int[] { R.id.tvName, R.id.tvCount });
			lv.setAdapter(adapter);
			break;
		case AC.aListSitePages:
		case AC.aListQDPages:
		case AC.aSearchBookOnQiDian:
		case AC.aSearchBookOnSite:
			adapter = new SimpleAdapter(ctx, data, android.R.layout.simple_list_item_1,
					new String[] { NV.PageName }, new int[] { android.R.id.text1 });
			lv.setAdapter(adapter);
			lv.setSelection(adapter.getCount() - 1); // 网络列表跳到尾部
			break;
		default:
			break;
		}
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
		case AC.aListMore1KPages:
			data = nm.getPageList(1001);
			break;
		}
	}

	private OnFinishListener lsn;
	public Fragment setOnFinishListener(OnFinishListener ofl) {
		lsn = ofl;
		return this;
	}
	@Override
	public void onDestroy() {
		if ( lsn != null) {
			lsn.OnFinish();
		}
		super.onDestroy();
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(ctx, sinfo, Toast.LENGTH_SHORT).show();
	}
	private void foxtipL(String sinfo) {
		tv.setText(sinfo);
	}

	private static final int IS_UPDATEPAGE = 88;
	private static final int IS_RenderListView = 5;
	Handler handler = new Handler(new WeakReference<Handler.Callback>(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch ( msg.what ) {
			case IS_UPDATEPAGE: // 更新章节完毕
				foxtipL("更新完毕 : " + (String)msg.obj );
				break;
			case IS_RenderListView: // 下载目录完毕
				if ( data.size() == 0 ) {
					foxtip("列表是空的哟");
					if ( ! isOnLine ) {
						back();
					}
				} else {
					renderListView();
				}
				break;
			}
			return true;
		}
	}).get());
	
	private Context ctx;
}
