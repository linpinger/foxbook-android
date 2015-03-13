package com.linpinger.foxbook;

import java.io.File;
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
import android.content.SharedPreferences;
import android.net.Uri;
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
	
	public FoxMemDB oDB  ; // 默认使用MemDB
	public int downThread = 9 ;  // 页面下载任务线程数
	public int leftThread = downThread ;

	ListView lv_booklist;
	List<Map<String, Object>> data;
	String lcURL, lcName; // long click 的变量
	Integer lcCount, lcID;
	private static Handler handler;
	private final int IS_MSG = 1;
	private final int IS_NEWPAGE = 2;
	private final int IS_REFRESHLIST = 3;
	private final int IS_REGENID = 4;
	private final int IS_NEWVER = 5;
	private final int FROM_DB = 1;
	private final int FROM_NET = 2;
	private long mExitTime;

	private int upchacount;    // 新增章节计数
	
//	private final int SITE_EASOU = 11 ;
	private final int SITE_ZSSQ = 12 ;
	private final int SITE_KUAIDU = 13 ;
	
	// 设置: isMemDB
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	public static final String FOXSETTING = "FOXSETTING";
	private boolean isMemDB = true;  // 是否是内存数据库
	private boolean isIntDB = false;  // 是否是内部存储空间[还是SD卡]中保存数据库


	public class FoxTaskDownPage implements Runnable { // 多线程任务更新页面列表
		List<Map<String, Object>> taskList;
		public FoxTaskDownPage(List<Map<String, Object>> iTaskList) {
			this.taskList = iTaskList ;
		}
		public void run() {
			Message msg;
			String thName = Thread.currentThread().getName();
			Iterator<Map<String, Object>> itr = taskList.iterator();
			HashMap<String, Object> mm ;
			int nowID ;
			String nowURL ;
			int locCount = 0 ;
			int allCount = taskList.size();
			while (itr.hasNext()) {
				++ locCount ;
				mm = (HashMap<String, Object>) itr.next();
				nowID = (Integer) mm.get("id");
				nowURL = (String) mm.get("url");

				FoxBookLib.updatepage(nowID, nowURL, oDB);
				
				msg = Message.obtain();
				msg.what = IS_MSG;
				msg.obj = leftThread + ":" + thName + ":" + locCount + " / " + allCount ;
				handler.sendMessage(msg);
			}
			--leftThread;
			if ( 0 == leftThread ) { // 所有线程更新完毕
				msg = Message.obtain();
				msg.what = IS_MSG;
				msg.obj = "已更新完所有空白章节>25" ;
				handler.sendMessage(msg);
			}
		}
	}

	public class UpdateAllBook implements Runnable {
		public void run() {
			Message msg;
			ArrayList<Thread> threadList = new ArrayList<Thread>(30);
            Thread nowT;
            
        	int anowID, aisEnd; // 全部更新里面使用的变量
        	String anowName, anowURL;
        	
			upchacount = 0 ;
			Iterator<Map<String, Object>> itrl = data.iterator();
			HashMap<String, Object> jj;
			while (itrl.hasNext()) {
				jj = (HashMap<String, Object>) itrl.next();
				anowID = (Integer) jj.get("id");
				anowURL = (String) jj.get("url");
				anowName = (String) jj.get("name");
				aisEnd = (Integer) jj.get("isend");
				if (1 != aisEnd) {
					nowT = new Thread(new UpdateBook(anowID, anowURL, anowName,true));
					threadList.add(nowT);
					nowT.start();
				}
			}
			
            Iterator<Thread> itrT = threadList.iterator();
            while (itrT.hasNext()) {
                nowT = (Thread) itrT.next();
                try {
                    nowT.join();
                } catch (Exception ex) {
                    System.out.println("等待线程错误: " + ex.toString());
                }
            }
            
			msg = Message.obtain();
			msg.what = IS_MSG;
			msg.obj = "共 " + upchacount + " 新章节，全部更新完毕" ;
			handler.sendMessage(msg);
		}
	}

	public class UpdateBook implements Runnable { // 后台线程更新书
		private int bookid;
		private String bookname;
		private String bookurl ;
		private boolean bDownPage = true;

		UpdateBook(int inbookid, String inBookURL, String inbookname, boolean bDownPage) {
			this.bookid = inbookid;
			this.bookurl = inBookURL;
			this.bookname = inbookname;
			this.bDownPage = bDownPage;
		}

		@Override
		public void run() {
			List<Map<String, Object>> xx;
//			String bookurl = FoxDB.getOneCell("select url from book where id=" + String.valueOf(bookid)); // 获取 url
			String existList = FoxMemDBHelper.getPageListStr(bookid, oDB); // 得到旧 list

			Message msg = Message.obtain();
			msg.what = IS_MSG;
			msg.obj = bookname + ": 正在下载目录页";
			handler.sendMessage(msg);

			int site_type = 0 ;
			if ( bookurl.indexOf("zhuishushenqi.com") > -1 ) {
				site_type = SITE_ZSSQ ;
			}
			if ( bookurl.indexOf(".qreader.") > -1 ) {
				site_type = SITE_KUAIDU ;
			}
			
			String html = "";
			switch(site_type) {
			case SITE_KUAIDU:
				if (existList.length() > 3) {
					xx = site_qreader.qreader_GetIndex(bookurl, 55, 1); // 更新模式  最后55章
				} else {
					xx = site_qreader.qreader_GetIndex(bookurl, 0, 1); // 更新模式
				}
				break;
			case SITE_ZSSQ:
				html = FoxBookLib.downhtml(bookurl, "utf-8"); // 下载json
				if (existList.length() > 3) {
					xx = site_zssq.json2PageList(html, 55, 1); // 更新模式  最后55章
				} else {
					xx = site_zssq.json2PageList(html, 0, 1); // 更新模式
				}
				break;
			default:
				html = FoxBookLib.downhtml(bookurl); // 下载url
				if (existList.length() > 3) {
					xx = FoxBookLib.tocHref(html, 55); // 分析获取 list 最后55章
				} else {
					xx = FoxBookLib.tocHref(html, 0); // 分析获取 list 所有章节
				}
			}

			ArrayList<HashMap<String, Object>> newPages = (ArrayList<HashMap<String, Object>>)FoxBookLib.compare2GetNewPages(xx, existList) ;
			int newpagecount = newPages.size(); // 新章节数，便于统计

			if (newpagecount == 0) {
				msg = Message.obtain();
				msg.what = IS_MSG;
				msg.obj = bookname + ": 无新章节";
				handler.sendMessage(msg);
				handler.sendEmptyMessage(IS_REFRESHLIST); // 更新完毕，通知刷新
				if ( ! bDownPage ) { //添加这个主要想在有空白章节时更新一下
					return;
				}
			} else {
				msg = Message.obtain();
				msg.what = IS_NEWPAGE;
				msg.arg1 = newpagecount; // 新章节数
				msg.obj = bookname + ": 新章节数: " + String.valueOf(newpagecount);
				handler.sendMessage(msg);
			}

			if ( newpagecount > 0 ) {
				FoxMemDBHelper.inserNewPages(newPages, bookid, oDB); // 添加到数据库
			}
			

			if (bDownPage) {
			List<Map<String, Object>> nbl = FoxMemDBHelper.getBookNewPages(bookid, oDB);
			int cTask = nbl.size() ; // 总任务数
			
			if ( cTask > 25 ) { // 当新章节数大于 25章就采用多任务下载模式
				int nBaseCount = cTask / downThread ; //每线程基础任务数
				int nLeftCount = cTask % downThread ; //剩余任务数
				int aList[] = new int[downThread] ; // 每个线程中的任务数

				for ( int i = 0; i < downThread; i++ ) {  // 分配任务数
					if ( i < nLeftCount ) {
						aList[i] = nBaseCount + 1 ;
					} else {
						aList[i] = nBaseCount ;
					}
				}

				List<Map<String, Object>> subList ;
				int startPoint = 0 ;
				for ( int i = 0; i < downThread; i++ ) {
					if ( aList[i] == 0 ) { // 这种情况出现在总任务比线程少的情况下
						--leftThread ;
						continue ;
					}
					subList = new ArrayList<Map<String, Object>>(aList[i]);
					for ( int n = startPoint; n < startPoint + aList[i]; n++ ) {
						subList.add((HashMap<String, Object>)nbl.get(n));
					}
					(new Thread(new FoxTaskDownPage(subList), "T" + i)).start() ;

					startPoint += aList[i] ;
				}
			} else {
			// 单线程循环更新页面
			Iterator<Map<String, Object>> itrz = nbl.iterator();
//			String nowURL = "";
			Integer nowpageid = 0;
			int nowCount = 0;
			while (itrz.hasNext()) {
				HashMap<String, Object> nn = (HashMap<String, Object>) itrz.next();
//				nowURL = (String) nn.get("url");
				nowpageid = (Integer) nn.get("id");

				++nowCount;
				msg = Message.obtain();
				msg.what = IS_MSG;
				msg.obj = bookname + ": 下载章节: " + nowCount + " / " + newpagecount ;
				handler.sendMessage(msg);

				FoxBookLib.updatepage(nowpageid, oDB);
			}
			} // 单线程更新
			} // bDownPage

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
					Activity_PageList.oDB = oDB;
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
				builder.setItems(new String[] { "更新本书",
						"更新本书目录",
						"在线查看",
						"搜索:起点",
						"搜索:搜狗",
						"搜索:快读",
						"搜索:追书神器",
						"搜索:宜搜",
						"编辑本书信息",
						"复制书名",
						"删除本书" },
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								case 0:  // 更新本书
									upchacount = 0 ;
									new Thread(new UpdateBook(lcID, lcURL, lcName, true)).start();
									foxtip("正在更新: " + lcName);
									break;
								case 1: // 更新本书目录
									upchacount = 0 ;
									new Thread(new UpdateBook(lcID, lcURL, lcName, false)).start();
									foxtip("正在更新目录: " + lcName);
									break;
								case 2: // 在线查看
									Intent intent = new Intent(
											Activity_BookList.this,
											Activity_PageList.class);
									intent.putExtra("iam", FROM_NET);
									intent.putExtra("bookurl", lcURL);
									intent.putExtra("bookname", lcName);
									if ( lcURL.indexOf("zhuishushenqi.com") > -1 ) {
										intent.putExtra("searchengine", SITE_ZSSQ);
									}
									if ( lcURL.indexOf(".qreader.") > -1 ) {
										intent.putExtra("searchengine", SITE_KUAIDU);
									}
									Activity_PageList.oDB = oDB;
									startActivity(intent);
									break;
								case 3: // 搜索:起点
									String lcQidianID = oDB.getOneCell("select qidianid from book where id=" + lcID);
									String lcQidianURL = "";
									if ( 0 == lcQidianID.length() ) {
						                String json = FoxBookLib.downhtml(site_qidian.qidian_getSearchURL_Mobile(lcName), "utf-8");
						                List<Map<String, Object>> qds = site_qidian.json2BookList(json);
						                if ( qds.get(0).get("name").toString().equalsIgnoreCase(lcName) ) { // 第一个结果就是目标书
											lcQidianURL = qds.get(0).get("url").toString();
						                }
									} else { // 存在起点ID
										lcQidianURL = site_qidian.qidian_getIndexURL_Desk(Integer.valueOf(lcQidianID)) ;
									}
									if ( 0 != lcQidianURL.length() ) {
										Intent intentQD = new Intent(Activity_BookList.this, Activity_PageList.class);
										intentQD.putExtra("iam", FROM_NET);
										intentQD.putExtra("bookurl", lcQidianURL);
										intentQD.putExtra("bookname", lcName);
										intentQD.putExtra("searchengine", 1);
										Activity_PageList.oDB = oDB;
										startActivity(intentQD);
									} else {
										foxtip("在起点上未搜索到该书名");
									}
									break;
								case 4: // 搜索:sougou
									Intent intent7 = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
									intent7.putExtra("bookname", lcName);
									intent7.putExtra("searchengine", 1);
									Activity_QuickSearch.oDB = oDB;
									startActivity(intent7);
									break;
								case 5:  // 搜索:快读
									Intent intent13 = new Intent(
											Activity_BookList.this,
											Activity_PageList.class);
									intent13.putExtra("iam", FROM_NET);
									intent13.putExtra("bookurl", lcURL);
									intent13.putExtra("bookname", lcName);
									intent13.putExtra("searchengine", SITE_KUAIDU);
									Activity_PageList.oDB = oDB;
									startActivity(intent13);
									break;
								case 6: // 搜索:追书神器
									Intent intent9 = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
									intent9.putExtra("bookname", lcName);
									intent9.putExtra("searchengine", 12);
									Activity_QuickSearch.oDB = oDB;
									startActivity(intent9);
									break;
								case 7: // 搜索:easou
									Intent intent8 = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
									intent8.putExtra("bookname", lcName);
									intent8.putExtra("searchengine", 11);
									Activity_QuickSearch.oDB = oDB;
									startActivity(intent8);
									break;
								case 8:  // 编辑本书信息
									Intent itti = new Intent(
											Activity_BookList.this,
											Activity_BookInfo.class);
									itti.putExtra("bookid", lcID);
									Activity_BookInfo.oDB = oDB;
									startActivityForResult(itti, 0);
									break;
								case 9:  // 复制书名
									ClipboardManager cbm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
									cbm.setText(lcName);
									foxtip("已复制到剪贴板:" + lcName);
									break;
								case 10: // 删除本书
									FoxMemDBHelper.deleteBook(lcID, oDB);
									refresh_BookList();
									foxtip("已删除书:" + lcName);
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
		data = FoxMemDBHelper.getBookList(oDB); // 获取书籍列表
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
					refresh_BookList(); // 刷新LV中的数据
					break;
				case IS_NEWVER:
					setTitle((String)msg.obj);

					try {
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setDataAndType(Uri.fromFile(new File("/sdcard/FoxBook.apk")), "application/vnd.android.package-archive"); 
						startActivity(i);
					} catch(Exception e) {
						e.toString();
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

		// 获取设置，是否使用内存数据库
        settings = getSharedPreferences(FOXSETTING, 0);
        editor = settings.edit();
        this.isMemDB = settings.getBoolean("isMemDB", isMemDB);
        this.isIntDB = settings.getBoolean("isIntDB", isIntDB);

		oDB = new FoxMemDB(this.isMemDB, this.isIntDB, this.getApplicationContext()) ; // 默认使用MemDB
		
		init_handler(); // 初始化一个handler 用于处理后台线程的消息

		lv_booklist = getListView(); // 获取LV

		refresh_BookList(); // 刷新LV中的数据

		init_LV_item_click(); // 初始化 单击 条目 的行为
		init_LV_item_Long_click(); // 初始化 长击 条目 的行为
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.booklist, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
				case R.id.action_isMemDB:
					menu.getItem(i).setChecked(this.isMemDB);
					break;
				case R.id.action_isIntDB:
					menu.getItem(i).setChecked(this.isIntDB);
					break;
				case R.id.action_intDB2SD:
					menu.getItem(i).setVisible(this.isIntDB);
					break;
				case R.id.action_SD2intDB:
					menu.getItem(i).setVisible(this.isIntDB);
					break;					
			}
		}
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case R.id.action_updateall: // 更新所有
			new Thread(new UpdateAllBook()).start();
			break;
		case R.id.action_switchdb:
			this.setTitle("切换数据库");
			(new Thread(){
				public void run(){
					String nowPath = oDB.switchMemDB();
					Message msg = Message.obtain();
					msg.what = IS_REGENID;
					msg.obj = "已切换到: " + nowPath;
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_refresh:
			refresh_BookList(); // 刷新LV中的数据
			foxtip("ListView已刷新");
			break;
		case R.id.action_searchbook:  // 打开搜索书籍
			Intent intent = new Intent(Activity_BookList.this,
					Activity_SearchBook.class);
			Activity_SearchBook.oDB = oDB;
			startActivityForResult(intent,0);
			break;
		case R.id.action_allpagelist:  // 所有章节
			Intent ittall = new Intent(Activity_BookList.this, Activity_AllPageList.class);
			ittall.putExtra("howmany", 0); // 显示多少章节
			Activity_AllPageList.oDB = oDB;
			startActivityForResult(ittall, 0);
			break;
		case R.id.action_sortbook_asc: // 顺序排序
			this.setTitle("顺序排序");
			(new Thread(){ public void run(){
					FoxMemDBHelper.regenID(1, oDB); // 顺序bookid
					FoxMemDBHelper.regenID(9, oDB); // 重新生成页面ID
					Message msg = Message.obtain();
					msg.what = IS_REGENID;
					msg.obj = "已按页面页数顺序重排好书籍";
					handler.sendMessage(msg);
				} }).start();
			break;
		case R.id.action_sortbook_desc: // 倒序排序
			this.setTitle("倒序排序");
			(new Thread(){ public void run(){
				FoxMemDBHelper.regenID(2, oDB); // 倒序bookid
				FoxMemDBHelper.regenID(9, oDB); // 重新生成页面ID
				Message msg = Message.obtain();
				msg.what = IS_REGENID;
				msg.obj = "已按页面页数倒序重排好书籍";
				handler.sendMessage(msg);
			} }).start();
			break;
		case R.id.action_isMemDB:
			isMemDB = ! item.isChecked() ;
			item.setChecked(isMemDB);
			editor.putBoolean("isMemDB", isMemDB);
			editor.commit();
			if (isMemDB) {
				foxtip("切换到内存数据库模式，重启程序生效");
			} else {
				foxtip("切换到普通模式，重启程序生效");
			}
			break;
		case R.id.action_all2epub:
			setTitle("开始转换成EPUB...");
			(new Thread(){
				public void run(){
					FoxBookLib.all2epub(oDB);
					Message msg = Message.obtain();
					msg.what = IS_MSG;
					msg.obj = "全部转换完毕: /sdcard/fox.epub";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_all2umd:
			setTitle("开始转换成UMD...");
			(new Thread(){
				public void run(){
					FoxBookLib.all2umd(oDB);
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
					FoxBookLib.all2txt(oDB);
					Message msg = Message.obtain();
					msg.what = IS_MSG;
					msg.obj = "全部转换完毕: /sdcard/fox.txt";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_updatepkg:
			setTitle("开始更新版本...");
			(new Thread(){
				public void run(){
					int newver = new FoxUpdatePkg(getApplicationContext()).FoxCheckUpdate() ;
					Message msg = Message.obtain();
					if ( newver > 0 ) {
						msg.what = IS_NEWVER;
						msg.obj = newver + ":新版本" ;
					} else {
						msg.what = IS_MSG;
						msg.obj = "无新版本" ;
					}
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_exitwithnosave:  // 不保存数据库退出
			this.finish();
			System.exit(0);
			break;
		case R.id.action_isIntDB:   // 是否使用内部存储
			isIntDB = ! item.isChecked() ;
			item.setChecked(isIntDB);
			editor.putBoolean("isIntDB", isIntDB);
			editor.commit();
			if (isIntDB) {
				foxtip("切换到内部存储数据库模式，重启程序生效");
			} else {
				foxtip("切换到SD卡数据库模式，重启程序生效");
			}
			break;
		case R.id.action_intDB2SD:  // 内部存储->SD卡
			setTitle("导出: 内部存储->SD卡...");
			(new Thread(){
				public void run(){
					oDB.SD2Int(false);
					Message msg = Message.obtain();
					msg.what = IS_MSG;
					msg.obj = "完毕导出: 内部存储->SD卡";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_SD2intDB:  // SD卡->内部存储
			foxtip("导入后会自动退出本程序");
			setTitle("导入: SD卡->内部存储...");
			(new Thread(){
				public void run(){
					oDB.SD2Int(true);
					Message msg = Message.obtain();
					msg.what = IS_MSG;
					msg.obj = "完毕导入: SD卡->内部存储";
					handler.sendMessage(msg);
					System.exit(0); // 不保存退出
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
				foxtip("再按一次返回键退出程序");
				mExitTime = System.currentTimeMillis();
			} else {
//				foxtip("退出中，正在保存数据库..."); // 显示不了
				oDB.closeMemDB();
				this.finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}

}
