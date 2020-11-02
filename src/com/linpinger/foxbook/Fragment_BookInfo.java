package com.linpinger.foxbook;

import java.io.File;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.linpinger.misc.BackHandledFragment;
import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.NovelSite;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.ToolAndroid;
import com.linpinger.tool.ToolJava;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu.OnMenuItemClickListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Fragment_BookInfo extends BackHandledFragment {

	public static Fragment_BookInfo newInstance(NovelManager nvMgr, int inBookIDX) {
		Fragment_BookInfo fc = new Fragment_BookInfo();

		fc.nm = nvMgr;
		Bundle bd = new Bundle();
		bd.putInt(NV.BookIDX, inBookIDX);

		fc.setArguments(bd);
		return fc;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ctx = container.getContext();
		View v = inflater.inflate(R.layout.fragment_bookinfo, container, false); // 这个false很重要，不然会崩溃

		if ( ToolAndroid.isEink() ) {
			v.setBackgroundColor(Color.WHITE);
		} else {
			v.setBackgroundResource(R.drawable.background_color);
		}

		init_controls(v) ; // 初始化各控件

		bookIDX = getArguments().getInt(NV.BookIDX); // 获取传入的 bookIDX

		Map<String, Object> info = nm.getBookInfo(bookIDX);
		// 显示数据
		tv_bid.setText(String.valueOf(bookIDX)) ;
		edt_bname.setText(info.get(NV.BookName).toString()) ;
		edt_bauthor.setText(info.get(NV.BookAuthor).toString()) ;
		edt_isend.setText(String.valueOf(info.get(NV.BookStatu))) ;
		edt_qdid.setText(info.get(NV.QDID).toString()) ;
		edt_burl.setText(info.get(NV.BookURL).toString()) ;
		edt_delurl.setText(info.get(NV.DelURL).toString()) ;

		foxtipL("返回，保存，复制，粘贴，网址获取ID");
		return v;
	}

	private class onViewClickListener implements View.OnClickListener { // 单击
		@Override
		public void onClick(View v) {
			switch ( v.getId() ) {
			case R.id.testTV:
				back();
				break;
			case R.id.btnSave:
				Map<String, Object> info = nm.getBlankBookInfo();
				info.put(NV.BookName, edt_bname.getText().toString());
				info.put(NV.BookAuthor, edt_bauthor.getText().toString());
				info.put(NV.BookURL, edt_burl.getText().toString());
				info.put(NV.DelURL, edt_delurl.getText().toString());
				info.put(NV.QDID, edt_qdid.getText().toString());
				info.put(NV.BookStatu, Integer.valueOf(edt_isend.getText().toString()));
				nm.setBookInfo(info, bookIDX);
		
				back();
				break;
			case R.id.btnCopyFocus:
				String toCopy = "";
				if ( edt_bname.isFocused() ) { toCopy = edt_bname.getText().toString() ; }
				if ( edt_bauthor.isFocused() ) { toCopy = edt_bauthor.getText().toString() ; }
				if ( edt_qdid.isFocused() ) { toCopy = edt_qdid.getText().toString() ; }
				if ( edt_burl.isFocused() ) { toCopy = edt_burl.getText().toString() ; }
				if ( edt_delurl.isFocused() ) { toCopy = edt_delurl.getText().toString() ; }
				if ( edt_isend.isFocused() ) {
					toCopy = "FoxBook>" + edt_bname.getText().toString() + ">"
							+ edt_bauthor.getText().toString() + ">"
							+ edt_qdid.getText().toString() + ">"
							+ edt_burl.getText().toString() + ">"
							+ edt_delurl.getText().toString(); // 复制全部
				}
				ToolAndroid.setClipText(toCopy, ctx);
				foxtip("剪贴板: " + toCopy);
				break;
			case R.id.btnPasteFocus:
				String toPaste = ToolAndroid.getClipText(ctx);
				if ( edt_bname.isFocused() ) { edt_bname.setText(toPaste) ; }
				if ( edt_bauthor.isFocused() ) { edt_bauthor.setText(toPaste) ; foxtip("若作者留空，表示是新书，更新时不会只下载最后55章"); }
				if ( edt_qdid.isFocused() ) { edt_qdid.setText(toPaste) ; }
				if ( edt_burl.isFocused() ) { edt_burl.setText(toPaste) ; }
				if ( edt_delurl.isFocused() ) { edt_delurl.setText("") ; }
				if ( edt_isend.isFocused() ) {
					if ( ! toPaste.contains("FoxBook>") ) { // 粘贴全部
						foxtip("剪贴板中的内容格式不对哟");
						break;
					}
					String xx[] = toPaste.split(">");
					edt_bname.setText(xx[1]);
					edt_bauthor.setText(xx[2]);
					edt_qdid.setText(xx[3]);
					if ( edt_burl.getText().toString().contains("http") ) {
						ToolAndroid.setClipText(xx[4], ctx);
						foxtip("剪贴板: " + xx[4]);
					} else {
						edt_burl.setText(xx[4]);
						edt_delurl.setText(xx[5]);
					}
				}
				break;
			case R.id.btnOther:
				createPopupMenu();
				break;
			} // switch end
		} // onClick End
	}

	void createPopupMenu() { // 弹出菜单 btnOther
		PopupMenu popW = new PopupMenu(ctx, btnOther);
		Menu m = popW.getMenu();

		m.add("从URL获取起点ID");
		m.add("导入阅读URL");
		m.add("快速搜索: meegoq");

		m.add("书名转URL: xqqxs"); // meegoq, ymxxs, wutuxs, dajiadu, 13xxs, xqqxs
		m.add("书名转URL: 13xxs");
		m.add("书名转URL: dajiadu");
		m.add("书名转URL: meegoq");
		m.add("书名转URL: wutuxs");
		m.add("书名转URL: ymxxs");

		popW.show();
		popW.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem mi) {
				String mt = mi.getTitle().toString();

				if ( mt.equalsIgnoreCase("从URL获取起点ID") ) {
					String url_now = edt_burl.getText().toString();
					if ( url_now.contains(".qidian.com/") ) {
						edt_qdid.setText(new SiteQiDian().getBookID_FromURL(url_now));
					} else {
						foxtip("URL不包含 .qidian.com/");
					}
				} else if ( mt.equalsIgnoreCase("导入阅读URL") ) {
					String newURL = getYueDuURL(edt_bname.getText().toString());
					if ( ! newURL.equalsIgnoreCase("") ) {
						edt_burl.setText(newURL);
					}
				} else if ( mt.equalsIgnoreCase("快速搜索: meegoq") ) {
					String book_name = edt_bname.getText().toString();
					startFragment( Fragment_QuickSearch.newInstance(nm, AC.SE_MEEGOQ, book_name) );
				} else if ( mt.contains("书名转URL:") ) {
					final String sBookName = edt_bname.getText().toString();
					final String siteType = mt.split(" ")[1];

					(new Thread() { public void run() {
						String sURL = new NovelSite().searchBook(sBookName, siteType);
						Message msg = Message.obtain();
						msg.what = IS_SetBookURL;
						msg.obj = sURL;
						handler.sendMessage(msg);
					}}).start();

				} else {
					foxtipL(mt);
				}
				return true;
			}
		});
	}

	private String getYueDuURL(String iBookName) {
		String json3Path = "/sdcard/bookshelf.json";
		File json3File = new File(json3Path);
		if ( json3File.exists() ) { // 3.x
			String json3Str = ToolJava.readText(json3Path, "utf-8");
			try {
				JSONArray bList = new JSONArray(json3Str);
				JSONObject info;
				for (int i = 0; i < bList.length(); i++) {
					info = bList.getJSONObject(i); // name,author,tocUrl
					if (info.getString("name").equalsIgnoreCase(iBookName)) {
						foxtip("找到: " + iBookName + " By: "
								+ info.getString("author") + "\n"
								+ info.getString("tocUrl"));
						return info.getString("tocUrl");
					}
				}
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		} else { // 2.x
			String json2Path = "/sdcard/YueDu/myBookShelf.json";
			File json2File = new File(json2Path);
			if ( ! json2File.exists() ) {
				foxtip("文件可能不存在:\n" + json2Path);
				return "";
			}
			String json2Str = ToolJava.readText(json2Path, "utf-8");
			try {
				JSONArray bList = new JSONArray(json2Str);
				JSONObject info;
				for (int i = 0; i < bList.length(); i++) {
					info = bList.getJSONObject(i).getJSONObject("bookInfoBean"); // bookInfoBean:name,author,chapterUrl,noteUrl
					if (info.getString("name").equalsIgnoreCase(iBookName)) {
						foxtip("找到: " + iBookName + " By: "
								+ info.getString("author") + "\n"
								+ info.getString("chapterUrl"));
						return info.getString("chapterUrl");
					}
				}
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
		return "";
	}

	private void init_controls(View v) { // 初始化各控件
		tv          = (TextView) v.findViewById(R.id.testTV);
		tv_bid      = (TextView) v.findViewById(R.id.tv_bid);
		edt_bname   = (EditText) v.findViewById(R.id.edt_bname);
		edt_bauthor = (EditText) v.findViewById(R.id.edt_bauthor);
		edt_isend   = (EditText) v.findViewById(R.id.edt_isend);
		edt_qdid    = (EditText) v.findViewById(R.id.edt_qdid);
		edt_burl    = (EditText) v.findViewById(R.id.edt_burl);
		edt_delurl  = (EditText) v.findViewById(R.id.edt_delurl);
		
		btnOther = (Button) v.findViewById(R.id.btnOther);

		onViewClickListener cl = new onViewClickListener();
		tv.setOnClickListener(cl);
		v.findViewById(R.id.btnSave).setOnClickListener(cl);
		v.findViewById(R.id.btnCopyFocus).setOnClickListener(cl);
		v.findViewById(R.id.btnPasteFocus).setOnClickListener(cl);
		btnOther.setOnClickListener(cl);
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(ctx, sinfo, Toast.LENGTH_SHORT).show();
	}
	private void foxtipL(String sinfo) {
		tv.setText(sinfo);
	}

	private TextView tv;
	private NovelManager nm;
	private int bookIDX = -1;
	private TextView tv_bid;
	private EditText edt_bname, edt_bauthor, edt_isend, edt_qdid, edt_burl, edt_delurl;

	Context ctx;
	private Button btnOther;

	private final int IS_SetBookURL = 23;
	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case IS_SetBookURL:
				String bookURL = (String)msg.obj;
				if ( ! bookURL.equalsIgnoreCase("") ) {
					edt_burl.setText( bookURL );
				} else {
					foxtip("木有找到URL");
				}
				break;
			}
			return false;
		}
	});

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

}
