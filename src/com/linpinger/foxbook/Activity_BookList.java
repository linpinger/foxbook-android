package com.linpinger.foxbook;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ClipData;
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

@SuppressLint("SdCardPath")
public class Activity_BookList extends ListActivity {
	
	public FoxMemDB oDB  ; // 默认使用MemDB
	public int downThread = 9 ;  // 页面下载任务线程数
	public int leftThread = downThread ;

	private FoxHTTPD foxHTTPD  = null;
	private boolean bDB3FileFromIntent = false;  // 是否是通过文件关联进来的，会修改不保存数据库退出菜单功能
	
	ListView lv_booklist;
	List<Map<String, Object>> data;
	String lcURL, lcName; // long click 的变量
	Integer lcCount, lcID;
	private static Handler handler;
	private final int DO_SETTITLE = 1;
	private final int IS_NEWPAGE = 2;
	private final int DO_REFRESHLIST = 3;
	private final int DO_REFRESH_TIP = 4;
	private final int IS_NEWVER = 5;
	private final int DO_REFRESH_SETTITLE = 6 ;
	private final int DO_UPDATEFINISH = 7;
	private boolean switchdbLock = false;
	private long mExitTime;

	private int upchacount;    // 新增章节计数
	
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

				FoxMemDBHelper.updatepage(nowID, nowURL, oDB);
				
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = leftThread + ":" + thName + ":" + locCount + " / " + allCount ;
				handler.sendMessage(msg);
			}
			--leftThread;
			if ( 0 == leftThread ) { // 所有线程更新完毕
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = "已更新完所有空白章节>25" ;
				handler.sendMessage(msg);
			}
		}
	}

	public class UpdateAllBook implements Runnable {
		public void run() {
			Message msg;
			
			msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = "下载书架..." ;
			handler.sendMessage(msg);
			ArrayList<HashMap<String, Object>> nn = FoxMemDBHelper.compareShelfToGetNew(oDB);
			if ( nn != null ) {
				int nnSize = nn.size() ;
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = "书架: " + nnSize + " 待更新" ;
				handler.sendMessage(msg);
				if ( 0 == nnSize ) {
					return ;
				} else { 
					Iterator<HashMap<String, Object>> itrXX = nn.iterator();
					HashMap<String, Object> mm;
					int nowBID = 0;
					String nowName, nowURL;
					Thread nowTTT;
					while (itrXX.hasNext()) {
						mm = (HashMap<String, Object>) itrXX.next();
						nowBID =  (Integer)mm.get("id");
						nowName = (String) mm.get("name");
						nowURL = (String) mm.get("url");
//						nowPageList = (String) mm.get("pagelist");
						nowTTT = new Thread(new UpdateBook(nowBID, nowURL, nowName, true));
						nowTTT.start();
						try {
							nowTTT.join();
							msg = Message.obtain();
							msg.what = DO_SETTITLE;
							msg.obj = "更新: " + nowName;
							handler.sendMessage(msg);
						} catch (InterruptedException e) {
							e.toString();
						}
					}
					msg = Message.obtain();
					msg.what = DO_UPDATEFINISH;
					msg.obj = "完毕: " + nnSize + " 已更新" ;
					handler.sendMessage(msg);
					return ;
				}
			}
			
			
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
			msg.what = DO_UPDATEFINISH;
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
			String existList = FoxMemDBHelper.getPageListStr(bookid, oDB); // 得到旧 list

			Message msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = bookname + ": 正在下载目录页";
			handler.sendMessage(msg);

			int site_type = 0 ;
            if ( bookurl.contains("3g.if.qidian.com") ) {
                site_type = SITES.SITE_QIDIAN_MOBILE ;
            }

			
			String html = "";
			switch(site_type) {
			case SITES.SITE_QIDIAN_MOBILE:
				html = FoxBookLib.downhtml(bookurl, "utf-8");
				xx = site_qidian.json2PageList(html);
				break;
			default:
				html = FoxBookLib.downhtml(bookurl); // 下载url
				if (existList.length() > 3) {
					xx = FoxBookLib.tocHref(html, 55); // 分析获取 list 最后55章
				} else {
					xx = FoxBookLib.tocHref(html, 0); // 分析获取 list 所有章节
				}
			}

			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String, Object>> newPages = (ArrayList<HashMap<String, Object>>)FoxBookLib.compare2GetNewPages(xx, existList) ;
			int newpagecount = newPages.size(); // 新章节数，便于统计

			if (newpagecount == 0) {
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = bookname + ": 无新章节";
				handler.sendMessage(msg);
				handler.sendEmptyMessage(DO_REFRESHLIST); // 更新完毕，通知刷新
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
				msg.what = DO_SETTITLE;
				msg.obj = bookname + ": 下载章节: " + nowCount + " / " + newpagecount ;
				handler.sendMessage(msg);

				FoxMemDBHelper.updatepage(nowpageid, oDB);
			}
			} // 单线程更新
			} // bDownPage

			msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = bookname + ": 更新完毕";
			handler.sendMessage(msg);

			handler.sendEmptyMessage(DO_REFRESHLIST); // 更新完毕，通知刷新
		}
	}

	private void init_LV_item_click() { // 初始化 单击 条目 的行为
		OnItemClickListener listener = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> chapinfo = (HashMap<String, Object>) arg0.getItemAtPosition(arg2);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpcount = Integer.parseInt((String) chapinfo.get("count"));
				Integer tmpid = (Integer) chapinfo.get("id");
				setTitle(tmpname + " : " + tmpurl);

				if (tmpcount > 0) {
					Intent intent = new Intent(Activity_BookList.this,
							Activity_PageList.class);
					intent.putExtra("iam", SITES.FROM_DB);
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
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				lcURL = (String) chapinfol.get("url");
				lcName = (String) chapinfol.get("name");
				lcCount = Integer.parseInt((String) chapinfol.get("count"));
				lcID = (Integer) chapinfol.get("id");
				setTitle(lcName + " : " + lcCount);

				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("操作:" + lcName);
				builder.setItems(new String[] { "更新本书",
						"更新本书目录",
						"在线查看",
						"搜索:起点",
						"搜索:bing",
						"复制书名",
						"编辑本书信息",
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
									Intent intent = new Intent(Activity_BookList.this, Activity_PageList.class);
									intent.putExtra("iam", SITES.FROM_NET);
									intent.putExtra("bookurl", lcURL);
									intent.putExtra("bookname", lcName);
									if ( lcURL.contains("3g.if.qidian.com") ) {
										intent.putExtra("searchengine", SITES.SE_QIDIAN_MOBILE);
									}
									Activity_PageList.oDB = oDB;
									startActivity(intent);
									break;
								case 3: // 搜索:起点
									String lcQidianID = oDB.getOneCell("select qidianid from book where id=" + lcID);
									String lcQidianURL = "";
									if ( null == lcQidianID || 0 == lcQidianID.length() ) {
						                String json = FoxBookLib.downhtml(site_qidian.qidian_getSearchURL_Mobile(lcName), "utf-8");
						                List<Map<String, Object>> qds = site_qidian.json2BookList(json);
						                if ( qds.get(0).get("name").toString().equalsIgnoreCase(lcName) ) { // 第一个结果就是目标书
											lcQidianURL = qds.get(0).get("url").toString();
						                }
									} else { // 存在起点ID
										lcQidianURL = site_qidian.qidian_getIndexURL_Mobile(Integer.valueOf(lcQidianID)) ;
									}
									if ( 0 != lcQidianURL.length() ) {
										Intent intentQD = new Intent(Activity_BookList.this, Activity_PageList.class);
										intentQD.putExtra("iam", SITES.FROM_NET);
										intentQD.putExtra("bookurl", lcQidianURL);
										intentQD.putExtra("bookname", lcName);
										intentQD.putExtra("searchengine", SITES.SE_QIDIAN_MOBILE);
										Activity_PageList.oDB = oDB;
										startActivity(intentQD);
									} else {
										foxtip("在起点上未搜索到该书名");
									}
									break;
								case 4: // 搜索:bing
									Intent intent7 = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
									intent7.putExtra("bookname", lcName);
									intent7.putExtra("searchengine", SITES.SE_BING);
									Activity_QuickSearch.oDB = oDB;
									startActivity(intent7);
									break;
								case 5:  // 复制书名
									copyToClipboard(lcName);
									foxtip("已复制到剪贴板: " + lcName);
									break;
								case 6:  // 编辑本书信息
									Intent itti = new Intent(
											Activity_BookList.this,
											Activity_BookInfo.class);
									itti.putExtra("bookid", lcID);
									Activity_BookInfo.oDB = oDB;
									startActivityForResult(itti, 0);
									break;
								case 7: // 删除本书
									FoxMemDBHelper.deleteBook(lcID, oDB);
									refresh_BookList();
									foxtip("已删除: " + lcName);
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
				case DO_UPDATEFINISH:
					setTitle((String)msg.obj);
					String xCount = oDB.getOneCell("select count(id) from page where length(content) < 999");
					if ( Integer.parseInt(xCount) > 0 ) {
						setTitle(xCount + ":" + (String)msg.obj);
						foxtip("有 " + xCount + " 章节短于1K");
					}
					break;
				case DO_SETTITLE:
					setTitle((String)msg.obj);
					break;
				case DO_REFRESHLIST:
					refresh_BookList();
					break;
				case DO_REFRESH_TIP :
					refresh_BookList();
					foxtip((String) msg.obj);
					break;
				case DO_REFRESH_SETTITLE :
					refresh_BookList();
					setTitle((String) msg.obj);
					break;
				case IS_NEWPAGE:
					upchacount += (Integer) msg.arg1;
					setTitle((String) msg.obj);
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

		// 获取传入的路径(关联db3文件)
		String db3PathIn = "none" ;
		try {
			db3PathIn = getIntent().getData().getPath();
		} catch (Exception e) {
			e.toString();
		}

		// 获取设置，是否使用内存数据库
        settings = getSharedPreferences(FOXSETTING, 0);
        editor = settings.edit();
        this.isMemDB = settings.getBoolean("isMemDB", isMemDB);
        this.isIntDB = settings.getBoolean("isIntDB", isIntDB);

        if ( db3PathIn.equalsIgnoreCase("none")) {
        	bDB3FileFromIntent = false;
        	oDB = new FoxMemDB(this.isMemDB, this.isIntDB, this.getApplicationContext()) ; // 默认使用MemDB
        } else {
        	bDB3FileFromIntent = true;
        	File inDB3File = new File(db3PathIn);
        	oDB = new FoxMemDB(inDB3File, this.getApplicationContext()) ; // 打开DB3
        	setTitle(inDB3File.getName());
        	foxtip("注意:\n退出时不会保存数据库的修改哦\n如要保存修改，按菜单键并选择菜单");
        }
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
				case R.id.action_exitwithnosave:
					if ( bDB3FileFromIntent ) {
						menu.getItem(i).setTitle("保存数据库并退出");
					}
					break;
				case R.id.action_switchdb:
					if ( bDB3FileFromIntent ) {
						menu.getItem(i).setVisible(false);
					}
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
			if ( switchdbLock ) {
				foxtip("还在切换中...");
			} else {
				(new Thread(){
					public void run(){
						switchdbLock = true;
						String nowPath = oDB.switchMemDB().getName().replace(".db3", "");
						switchdbLock = false;
						Message msg = Message.obtain();
						msg.what = DO_REFRESH_SETTITLE;
						msg.obj = nowPath;
						handler.sendMessage(msg);
					}
				}).start();
			}
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
			ittall.putExtra("apl_showtype", Activity_AllPageList.SHOW_ALL);
			Activity_AllPageList.oDB = oDB;
			startActivityForResult(ittall, 0);
			break;
		case R.id.action_showokinapl:  // 显示字数少于1K的章节
			Intent ittlok = new Intent(Activity_BookList.this, Activity_AllPageList.class);
			ittlok.putExtra("apl_showtype", Activity_AllPageList.SHOW_LESS1K);
			Activity_AllPageList.oDB = oDB;
			startActivityForResult(ittlok, 0);
			break;
			
		case R.id.action_sortbook_asc: // 顺序排序
			this.setTitle("顺序排序");
			(new Thread(){ public void run(){
					FoxMemDBHelper.regenID(1, oDB); // 顺序bookid
					FoxMemDBHelper.regenID(9, oDB); // 重新生成页面ID
					FoxMemDBHelper.simplifyAllDelList(oDB);
					Message msg = Message.obtain();
					msg.what = DO_REFRESH_TIP;
					msg.obj = "已按页面页数顺序重排好书籍";
					handler.sendMessage(msg);
				} }).start();
			break;
		case R.id.action_sortbook_desc: // 倒序排序
			this.setTitle("倒序排序");
			(new Thread(){ public void run(){
				FoxMemDBHelper.regenID(2, oDB); // 倒序bookid
				FoxMemDBHelper.regenID(9, oDB); // 重新生成页面ID
				FoxMemDBHelper.simplifyAllDelList(oDB);
				Message msg = Message.obtain();
				msg.what = DO_REFRESH_TIP;
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
					FoxMemDBHelper.all2epub(oDB);
					Message msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = "全部转换完毕: /sdcard/fox.epub";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_all2umd:
			setTitle("开始转换成UMD...");
			(new Thread(){
				public void run(){
					FoxMemDBHelper.all2umd(oDB);
					Message msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = "全部转换完毕: /sdcard/fox.umd";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_all2txt:
			setTitle("开始转换成TXT...");
			(new Thread(){
				public void run(){
					FoxMemDBHelper.all2txt(oDB);
					Message msg = Message.obtain();
					msg.what = DO_SETTITLE;
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
						msg.what = DO_SETTITLE;
						msg.obj = "无新版本" ;
					}
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_exitwithnosave:  // 不保存数据库退出
			if ( bDB3FileFromIntent ) { // 保存数据库并退出
				beforeExitApp();
			}
			this.finish();
			System.exit(0);
			break;
		case R.id.action_foxhttpd: // 启动停止服务器
			int nowListenPort = 8888 ;
			String nowIP = FoxUpdatePkg.getLocalIpAddress();
			boolean bStartIt = ! item.isChecked(); // 根据选项是否选中确定是否开启
			if (bStartIt) {
				try {
					foxHTTPD = new FoxHTTPD(nowListenPort, new File("/sdcard/"), oDB);
					item.setChecked(bStartIt);
					item.setTitle(nowIP + ":" + String.valueOf(nowListenPort) + " 已开");
					foxtip(nowIP + ":" + String.valueOf(nowListenPort) + " 已开\n如要关闭，选择同一菜单");
				} catch (Exception e) {
					e.toString();
				}
			} else {
				if (foxHTTPD != null) {
					foxHTTPD.stop();
					item.setChecked(bStartIt);
					item.setTitle(nowIP + ":" + String.valueOf(nowListenPort) + " 已关");
				}
			}
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
					msg.what = DO_SETTITLE;
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
					msg.what = DO_SETTITLE;
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
				if ( ! bDB3FileFromIntent ) { // 不保存数据库并退出
					beforeExitApp();
				}
				this.finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}
	private void beforeExitApp() {
		oDB.closeMemDB();
		if (foxHTTPD != null) {
			foxHTTPD.stop();
		}
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void copyToClipboard(String iText) {
		((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("hello", iText));
	}

}
