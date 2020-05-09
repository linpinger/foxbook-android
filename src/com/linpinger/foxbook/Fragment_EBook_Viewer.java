package com.linpinger.foxbook;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.misc.BackHandledFragment;
import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.tool.ToolAndroid;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Fragment_EBook_Viewer extends BackHandledFragment {

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
	private TextView tv;
	SimpleAdapter adapter;
	private NovelManager nm;

	private File eBookFile ;

	SharedPreferences settings;

	public static Fragment_EBook_Viewer newInstance(String inArg) {
		Fragment_EBook_Viewer fc = new Fragment_EBook_Viewer();
		Bundle bd = new Bundle();
		bd.putString("ebookPath", inArg);
		fc.setArguments(bd);
		return fc;
	}

	private void renderListView() { // 刷新LV
		adapter = new SimpleAdapter(ctx, data,
				R.layout.lv_item_pagelist, new String[] { NV.PageName, NV.Size },
				new int[] { R.id.tvName, R.id.tvCount });
		lv.setAdapter(adapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ctx = container.getContext();
		View v = inflater.inflate(R.layout.fragment_ebook_viewer, container, false); // 这个false很重要，不然会崩溃

		settings = PreferenceManager.getDefaultSharedPreferences(ctx);

		tv = (TextView) v.findViewById(R.id.testTV);
		lv = (ListView) v.findViewById(R.id.testLV); // 获取LV
		if ( ! ToolAndroid.isEink() ) {
			lv.setBackgroundColor(Color.parseColor("#EEFAEE"));
		}

		eBookFile = new File( getArguments().getString("ebookPath") ); // 获取txt/zip/epub路径

		if ( eBookFile.getName().toLowerCase().endsWith(".epub"))
			eBookType = EPUB ;
		if ( eBookFile.getName().toLowerCase().endsWith(".zip"))
			eBookType = ZIP ;
		if ( eBookFile.getName().toLowerCase().endsWith(".txt"))
			eBookType = TXT ;

		if ( eBookType == TXT || eBookType == TXTQIDIAN) { // 显示
			v.findViewById(R.id.btnToUTF8).setVisibility(View.VISIBLE);
		}

		this.nm = new NovelManager(eBookFile);

		foxtipL( nm.getBookInfo(0).get(NV.BookName).toString() );
		data = nm.getPageList(0);
		renderListView(); // 处理好data后再刷新列表

		lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> av, View v, int position, long id) { // 初始化 单击 条目 的行为
				Map<String, Object> page = (HashMap<String, Object>) data.get(position);

				// 跳到阅读页
				Bundle itt = new Bundle();
				switch (eBookType) {
				case ZIP:
				case ZIP1024:
					String zipItemName = page.get(NV.PageName).toString();
					foxtipL(zipItemName);
					if ( zipItemName.endsWith(".html") | zipItemName.endsWith(".htm") ) {
						itt.putString(NV.PageFullURL, nm.getBookInfo(0).get(NV.BookURL) + "@" + zipItemName);
						itt.putInt(AC.action, AC.aShowPageInZip1024);
						startFragment( Fragment_ShowPage4Eink.newInstance(nm, itt) );
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
					itt.putInt(AC.action, AC.aShowPageInMem);
					itt.putInt(NV.BookIDX, (Integer)page.get(NV.BookIDX)) ;
					itt.putInt(NV.PageIDX, (Integer)page.get(NV.PageIDX)) ;
					startFragment( Fragment_ShowPage4Eink.newInstance(nm, itt) );
					break;
				default:
					break;
				}
			}
		}); // LV item click end

		onViewClickListener cl = new onViewClickListener();
		tv.setOnClickListener(cl);
		v.findViewById(R.id.btnSaveExit).setOnClickListener(cl);
		v.findViewById(R.id.btnCopyInfo).setOnClickListener(cl);
		v.findViewById(R.id.btnToLVBottom).setOnClickListener(cl);
		v.findViewById(R.id.btnToLVTop).setOnClickListener(cl);
		v.findViewById(R.id.btnToUTF8).setOnClickListener(cl);
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

		return v;
	} // onCreate end

	private class onViewClickListener implements View.OnClickListener { // 单击
		@Override
		public void onClick(View v) {
			switch ( v.getId() ) {
			case R.id.testTV:
				back();
				break;
			case R.id.btnSaveExit:
				if ( ! settings.getBoolean("isSaveAsFML", true) )
					nm.setSaveFormat(NovelManager.SQLITE3);
				nm.close();
				back();
				System.exit(0);
				break;
			case R.id.btnCopyInfo:
				Map<String, Object> bk = nm.getBookInfo(0);
				String fbs = "FoxBook>" + bk.get(NV.BookName).toString() + ">"
						+ bk.get(NV.BookAuthor).toString() + ">"
						+ bk.get(NV.QDID).toString() + ">"
						+ bk.get(NV.BookURL) + ">" ;
				ToolAndroid.setClipText(fbs, ctx);
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
					back();
					System.exit(0);
				} else {
					foxtipL("骚年哟，当前不是txt");
				}
				break;
			}
		}
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(ctx, sinfo, Toast.LENGTH_SHORT).show();
	}
	private void foxtipL(String sinfo) {
		tv.setText(sinfo);
	}
	
	private Context ctx;
}
