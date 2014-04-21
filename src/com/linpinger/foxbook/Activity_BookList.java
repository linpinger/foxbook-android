package com.linpinger.foxbook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class Activity_BookList extends ListActivity {
	ListView lv_booklist;
	List<Map<String, Object>> data;
	String lcURL, lcName; // long click 的变量
	Integer lcCount, lcID;
	private static Handler handler;
	private final int IS_MSG = 1;
	private final int IS_NEWPAGE = 2;
	private final int IS_REFRESHLIST = 3;
	private final int IS_REGENID = 4;
	private final int FROM_DB = 1;
	private final int FROM_NET = 2;
	private long mExitTime;
	private int anowID, aisEnd; // 全部更新里面使用的变量
	private String anowName;
	private int upthreadcount; // 更新书籍计数
	private int upchacount;    // 新增章节计数

	public class UpdateBook implements Runnable { // 后台线程更新书
		private int bookid;
		private String bookname;
		private boolean bDownPage = true;

		UpdateBook(int inbookid, String inbookname, boolean bDownPage) {
			this.bookid = inbookid;
			this.bookname = inbookname;
			this.bDownPage = bDownPage;
		}

		@Override
		public void run() {
			List<Map<String, Object>> xx;
			String bookurl = FoxDB.getOneCell("select url from book where id="
					+ String.valueOf(bookid)); // 获取 url
			String existList = FoxDB.getPageListStr(bookid); // 得到旧 list
			existList = existList.toLowerCase();

			Message msg = Message.obtain();
			msg.what = IS_MSG;
			msg.obj = bookname + ": 正在下载目录页";
			handler.sendMessage(msg);

			String html = FoxBookLib.downhtml(bookurl); // 下载url

			if (existList.length() > 1024) {
				xx = FoxBookLib.tocHref(html, 55); // 分析获取 list 最后55章
			} else {
				xx = FoxBookLib.tocHref(html, 0); // 分析获取 list 所有章节
			}

			// 比较得到新章节
			String nowURL;
			ArrayList<HashMap<String, Object>> newPages = new ArrayList<HashMap<String, Object>>();
			Iterator<Map<String, Object>> itr = xx.iterator();
			while (itr.hasNext()) {
				HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
				nowURL = (String) mm.get("url");
				if ( ! existList.contains(nowURL.toLowerCase() + "|") ) { // 新章节
					newPages.add(mm);
				}
			}
			int newpagecount = newPages.size(); // 新章节数，便于统计

			if (newpagecount == 0) {
				msg = Message.obtain();
				msg.what = IS_MSG;
				msg.obj = bookname + ": 无新章节";
				handler.sendMessage(msg);
				handler.sendEmptyMessage(IS_REFRESHLIST); // 更新完毕，通知刷新
				return;
			} else {
				msg = Message.obtain();
				msg.what = IS_NEWPAGE;
				msg.arg1 = newpagecount; // 新章节数
				msg.obj = bookname + ": 新章节数: " + String.valueOf(newpagecount);
				handler.sendMessage(msg);
			}

			FoxDB.inserNewPages(newPages, bookid); // 添加到数据库

			if (bDownPage) {
			// 循环更新页面
			List<Map<String, Object>> nbl = FoxDB.getBookNewPages(bookid);
			Iterator<Map<String, Object>> itrz = nbl.iterator();
			Integer nowpageid = 0;
			int nowCount = 0;
			while (itrz.hasNext()) {
				HashMap<String, Object> nn = (HashMap<String, Object>) itrz
						.next();
				nowURL = (String) nn.get("url");
				nowpageid = (Integer) nn.get("id");

				++nowCount;
				msg = Message.obtain();
				msg.what = IS_MSG;
				msg.obj = bookname + ": 下载章节: " + String.valueOf(nowCount)
						+ " / " + String.valueOf(newpagecount);
				handler.sendMessage(msg);

				FoxBookLib.updatepage(nowpageid);
			}
			}

			msg = Message.obtain();
			msg.what = IS_MSG;
			msg.obj = bookname + ": 更新完毕";
			handler.sendMessage(msg);

			handler.sendEmptyMessage(IS_REFRESHLIST); // 更新完毕，通知刷新
		}
	}

	private void init_LV_item_click() { // 初始化 单击 条目 的行为
		OnItemClickListener listener = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				HashMap<String, Object> chapinfo = (HashMap<String, Object>) arg0.getItemAtPosition(arg2);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpcount = Integer.parseInt((String) chapinfo
						.get("count"));
				Integer tmpid = (Integer) chapinfo.get("id");
				setTitle(tmpname + " : " + tmpurl);

				if (tmpcount > 0) {
					Intent intent = new Intent(Activity_BookList.this,
							Activity_PageList.class);
					intent.putExtra("iam", FROM_DB);
					intent.putExtra("bookurl", tmpurl);
					intent.putExtra("bookname", tmpname);
					intent.putExtra("bookid", tmpid);
					startActivityForResult(intent, 0);
				}
			}
		};
		lv_booklist.setOnItemClickListener(listener);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent retIntent) { // 修改书后返回的数据
		if (0 == requestCode && RESULT_OK == resultCode) {
			refresh_BookList(); // 刷新LV中的数据
		}
	}

	private void init_LV_item_Long_click() { // 初始化 长击 条目 的行为
		final Builder builder = new AlertDialog.Builder(this);
		OnItemLongClickListener longlistener = new OnItemLongClickListener() {
			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				HashMap<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				lcURL = (String) chapinfol.get("url");
				lcName = (String) chapinfol.get("name");
				lcCount = Integer.parseInt((String) chapinfol.get("count"));
				lcID = (Integer) chapinfol.get("id");
				setTitle(lcName + " : " + lcURL);

				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("操作:" + lcName);
				builder.setItems(new String[] { "更新本书", "更新本书目录", "在线查看", "编辑本书信息",
						"删除本书", "复制书名", "复制URL" },
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								case 0:
									upchacount = 0 ;
									new Thread(new UpdateBook(lcID, lcName, true)).start();
									foxtip("正在更新: " + lcName);
									break;
								case 1:
									upchacount = 0 ;
									new Thread(new UpdateBook(lcID, lcName, false)).start();
									foxtip("正在更新目录: " + lcName);
									break;
								case 2:
									Intent intent = new Intent(
											Activity_BookList.this,
											Activity_PageList.class);
									intent.putExtra("iam", FROM_NET);
									intent.putExtra("bookurl", lcURL);
									intent.putExtra("bookname", lcName);
									startActivity(intent);
									break;
								case 3:
									Intent itti = new Intent(
											Activity_BookList.this,
											Activity_BookInfo.class);
									itti.putExtra("bookid", lcID);
									startActivityForResult(itti, 0);
									break;
								case 4:
									FoxDB.deleteBook(lcID);
									refresh_BookList();
									foxtip("已删除书:" + lcName);
									break;
								case 5:
									ClipboardManager cbm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
									cbm.setText(lcName);
									foxtip("已复制到剪贴板:" + lcName);
									break;
								case 6:
									ClipboardManager cbm2 = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
									cbm2.setText(lcURL);
									foxtip("已复制到剪贴板:" + lcURL);
									break;
								}
							}
						});
				builder.create().show();

				return true;
			}

		};
		lv_booklist.setOnItemLongClickListener(longlistener);
	}

	private void refresh_BookList() { // 刷新LV中的数据
		data = FoxDB.getBookList(); // 获取书籍列表
		// 设置listview的Adapter
		SimpleAdapter adapter = new SimpleAdapter(this, data,
				R.layout.lv_item_booklist, new String[] { "name", "count" },
				new int[] { R.id.tvName, R.id.tvCount });
		lv_booklist.setAdapter(adapter);
		// adapter.notifyDataSetChanged();
	}

	private void init_handler() { // 初始化一个handler 用于处理后台线程的消息
		handler = new Handler(new Handler.Callback() {
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
				case IS_MSG:
					setTitle((String)msg.obj);
					break;
				case IS_NEWPAGE:
					upchacount += (Integer) msg.arg1;
					setTitle((String) msg.obj);
					break;
				case IS_REGENID :
					refresh_BookList(); // 刷新LV中的数据
					foxtip((String) msg.obj);
					break;
				case IS_REFRESHLIST:
					--upthreadcount;
					refresh_BookList(); // 刷新LV中的数据
					if (upthreadcount <1 ){
						setTitle("新章节数: " + upchacount + "，全部更新完毕");
					} else {
						setTitle("剩余线程: " + upthreadcount);
					}
					break;
				}
				return false;
			}
		});
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_booklist);
		mExitTime = System.currentTimeMillis(); // 当前时间，便于两次退出

		init_handler(); // 初始化一个handler 用于处理后台线程的消息

		lv_booklist = getListView(); // 获取LV

		FoxDB.createDBIfNotExist(); // 如果数据库不存在，创建表结构
		
		refresh_BookList(); // 刷新LV中的数据

		init_LV_item_click(); // 初始化 单击 条目 的行为
		init_LV_item_Long_click(); // 初始化 长击 条目 的行为
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.booklist, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case R.id.action_updateall: // 更新所有
			upthreadcount = 0;
			upchacount = 0 ;
			Iterator<Map<String, Object>> itrl = data.iterator();
			HashMap<String, Object> jj;
			while (itrl.hasNext()) {
				jj = (HashMap<String, Object>) itrl.next();
				anowID = (Integer) jj.get("id");
				anowName = (String) jj.get("name");
				aisEnd = (Integer) jj.get("isend");
				if (1 != aisEnd) {
					++ upthreadcount;
					new Thread(new UpdateBook(anowID, anowName,true)).start();
				}
			}
			break;
		case R.id.action_vacuum:
			FoxDB.vacuumDB();
			foxtip("数据库已缩小");
			break;
		case R.id.action_switchdb:
			FoxDB.switchDB();
			refresh_BookList(); // 刷新LV中的数据
			foxtip("数据库已切换");
			break;
		case R.id.action_refresh:
			refresh_BookList(); // 刷新LV中的数据
			foxtip("ListView已刷新");
			break;
		case R.id.action_searchbook:
			Intent intent = new Intent(Activity_BookList.this,
					Activity_SearchBook.class);
			startActivityForResult(intent,0);
			break;
		case R.id.action_allpagelist:
			Intent ittall = new Intent(Activity_BookList.this, Activity_AllPageList.class);
			ittall.putExtra("howmany", 0); // 显示多少章节
			startActivityForResult(ittall, 0);
			break;
		case R.id.action_sortbook_asc: // 顺序排序
			foxtip("开始生成ID...");
			(new Thread(){ public void run(){
					FoxDB.regenID(1);
					Message msg = Message.obtain();
					msg.what = IS_REGENID;
					msg.obj = "已按页面页数顺序重排好书籍";
					handler.sendMessage(msg);
				} }).start();
			break;
		case R.id.action_sortbook_desc: // 倒序排序
			foxtip("开始生成ID...");
			(new Thread(){ public void run(){
				FoxDB.regenID(2);
				Message msg = Message.obtain();
				msg.what = IS_REGENID;
				msg.obj = "已按页面页数倒序重排好书籍";
				handler.sendMessage(msg);
			} }).start();
			break;
		case R.id.action_regen_pageid:  // 重排pageid
			foxtip("开始生成ID...");
			(new Thread(){ public void run(){
				FoxDB.regenID(9);
				Message msg = Message.obtain();
				msg.what = IS_REGENID;
				msg.obj = "已重排页面ID";
				handler.sendMessage(msg);
			} }).start();
			break;
		case R.id.action_all2umd:
			setTitle("开始转换成UMD...");
			(new Thread(){
				public void run(){
					FoxBookLib.all2umd();
					Message msg = Message.obtain();
					msg.what = IS_MSG;
					msg.obj = "全部转换完毕: /sdcard/fox.umd";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_all2txt:
			setTitle("开始转换成TXT...");
			(new Thread(){
				public void run(){
					FoxBookLib.all2txt();
					Message msg = Message.obtain();
					msg.what = IS_MSG;
					msg.obj = "全部转换完毕: /sdcard/fox.txt";
					handler.sendMessage(msg);
				}
			}).start();
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

	public boolean onKeyDown(int keyCoder, KeyEvent event) { // 按键响应
		if (keyCoder == KeyEvent.KEYCODE_BACK) {
			if ((System.currentTimeMillis() - mExitTime) > 2000) { // 两次退出键间隔
				Toast.makeText(this, "再按一次返回键退出程序", Toast.LENGTH_SHORT).show();
				mExitTime = System.currentTimeMillis();
			} else {
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}

}
