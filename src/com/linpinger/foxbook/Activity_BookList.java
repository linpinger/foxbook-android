package com.linpinger.foxbook;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.NovelSite;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.Ext_ListActivity_4Eink;
import com.linpinger.tool.ToolAndroid;
import com.linpinger.tool.ToolBookJava;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/*
传递数据: Action, Datas
Activity_PageList : 单击列表 		: aListBookPages, bookIDX
Activity_PageList : 显示所有章		: aListAllPages
Activity_PageList : 显示小于1K章	: aListLess1KPages
Activity_PageList : 在线查看		: aListSitePages, bookIDX
Activity_PageList : 搜索起点		: aListQDPages, bookIDX, TmpString
Activity_SearchBook : 搜索起点		: aListQDPages, bookName
Activity_BookInfo : 编辑信息		: bookIDX

Activity_QuickSearch : 搜索Bing		: atSearch
*/
public class Activity_BookList extends Ext_ListActivity_4Eink {

	private NovelManager nm ;
	File wDir ;			// 工作目录
	File cookiesFile ;	// 保存有cookie的文件名: FoxBook.cookie

	public int downThread = 9 ; // 页面下载任务线程数
	public int leftThread = downThread ;

	// 设置: 
	SharedPreferences settings;
	private String beforeSwitchShelf = "orderby_count_desc" ; // 和 arrays.xml中的beforeSwitchShelf_Values 对应
	private boolean isUpdateBlankPagesFirst = true; // 更新前先检测是否有空白章节
	private boolean isCompareShelf = true ;		// 更新前比较书架
	private boolean isShowIfRoom = false;		// 一直显示菜单图标

	private FoxHTTPD foxHTTPD = null;
	private boolean bShelfFileFromIntent = false; // 是否是通过文件关联进来的，会修改不保存退出菜单功能

	ListView lv_booklist;
	List<Map<String, Object>> data;

	private static Handler handler;
	private final int DO_SETTITLE = 1;
	private final int IS_NEWPAGE = 2;
	private final int DO_REFRESHLIST = 3;
	private final int DO_REFRESH_TIP = 4;
	private final int IS_NEWVER = 5;
	private final int DO_REFRESH_SETTITLE = 6 ;
	private final int DO_UPDATEFINISH = 7;

	private boolean switchShelfLock = false;
	private long mExitTime;

	private int upchacount; // 新增章节计数

	public class UpdateAllBook implements Runnable {
		public void run() {
			Message msg;

			isUpdateBlankPagesFirst = settings.getBoolean("isUpdateBlankPagesFirst", isUpdateBlankPagesFirst);
			if ( isUpdateBlankPagesFirst ) {
				for (Map<String, Object> blankPage : nm.getPageList(99) ) {
					msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = "填空: " + (String)blankPage.get(NV.PageName);
					handler.sendMessage(msg);
					nm.updatePage((Integer)blankPage.get(NV.BookIDX), (Integer)blankPage.get(NV.PageIDX));
				}
			}

			isCompareShelf = settings.getBoolean("isCompareShelf", isCompareShelf); // 更新前比较书架
if ( isCompareShelf ) {
			msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = "下载书架..." ;
			handler.sendMessage(msg);
			List<Map<String, Object>> nn = new NovelSite().compareShelfToGetNew(nm.getBookListForShelf(), cookiesFile);
			if ( nn != null ) {
				int nnSize = nn.size() ;
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = "书架: " + nnSize + " 待更新" ;
				handler.sendMessage(msg);
				if ( 0 == nnSize ) {
					return ;
				} else { 
					int nowBookIDX = -1;
					String nowName, nowURL;
					Thread nowTTT;
					for ( Map<String, Object> mm : nn ) {
						nowBookIDX = (Integer)mm.get(NV.BookIDX);
						nowName = mm.get(NV.BookName).toString();
						nowURL = mm.get(NV.BookURL).toString();
						nowTTT = new Thread(new UpdateBook(nowBookIDX, nowURL, nowName, true));
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
}

			List<Thread> threadList = new ArrayList<Thread>(30);
			Thread nowT;

			// 全部更新里面使用的变量
			int nowBookIDX = -1;
			String anowName, anowURL;
			upchacount = 0 ;
			for ( Map<String, Object> jj : data ) {
				nowBookIDX = (Integer) jj.get(NV.BookIDX);
				anowURL = (String) jj.get(NV.BookURL);
				anowName = (String) jj.get(NV.BookName);
				if ( (Integer)jj.get(NV.BookStatu) != 1 ) {
					nowT = new Thread(new UpdateBook(nowBookIDX, anowURL, anowName,true));
					threadList.add(nowT);
					nowT.start();
				}
			}

			for ( Thread nowThread : threadList ) {
				try {
					nowThread.join();
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
		private int bookIDX = 0 ;
		private String bookname ;
		private String bookurl ;
		private boolean bDownPage = true;

		UpdateBook(int inbookidx, String inBookURL, String inbookname, boolean bDownPage) {
			this.bookIDX = inbookidx;
			this.bookurl = inBookURL;
			this.bookname = inbookname;
			this.bDownPage = bDownPage;
		}

		@Override
		public void run() {
			Message msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = bookname + ": 下载目录页";
			handler.sendMessage(msg);

			String existList = nm.getPageListStr(bookIDX); // 得到旧 list
			List<Map<String, Object>> linkList;
			if ( bookurl.contains(".if.qidian.com") ) {
				linkList = new SiteQiDian().getTOC_Android7(ToolBookJava.downhtml(bookurl, "utf-8"));
			} else {
				linkList = new NovelSite().getTOC(ToolBookJava.downhtml(bookurl)); // 分析获取 list 所有章节
				if ( existList.length() > 3 ) {
					if ( nm.getBookInfo(bookIDX).get(NV.BookAuthor).toString().length() > 1 ) // 无作者名，表示为新书
						linkList = ToolBookJava.getLastNPage(linkList, 55); // 获取 list 最后55章
				}
			}

			List<Map<String, Object>> newPages = ToolBookJava.compare2GetNewPages(linkList, existList) ;
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

			List<Map<String, Object>> nbl = nm.addBookBlankPageList(newPages, bookIDX);
		if (bDownPage) {
			int cTask = nbl.size() ; // 总任务数
			
			if ( cTask > 25 ) { // 当新章节数大于 25章就采用多任务下载模式
				int nBaseCount = cTask / downThread ; //每线程基础任务数
				int nLeftCount = cTask % downThread ; //剩余任务数
				int aList[] = new int[downThread] ; // 每个线程中的任务数

				for ( int i = 0; i < downThread; i++ ) { // 分配任务数
					if ( i < nLeftCount )
						aList[i] = nBaseCount + 1 ;
					else
						aList[i] = nBaseCount ;
				}

				List<Map<String, Object>> subList ;
				int startPoint = 0 ;
				for ( int i = 0; i < downThread; i++ ) {
					if ( aList[i] == 0 ) { // 这种情况出现在总任务比线程少的情况下
						--leftThread ;
						continue ;
					}
					subList = new ArrayList<Map<String, Object>>(aList[i]);
					for ( int n = startPoint; n < startPoint + aList[i]; n++ )
						subList.add(nbl.get(n));
					(new Thread(new FoxTaskDownPage(subList), "T" + i)).start() ;

					startPoint += aList[i] ;
				}
			} else { // 单线程循环更新页面
				int nowCount = 0;
				for (Map<String, Object> blankPage : nbl){
					++nowCount;
					msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = bookname + ": 下载章节: " + nowCount + " / " + newpagecount ;
					handler.sendMessage(msg);

					nm.updatePage(bookIDX, (Integer)blankPage.get(NV.PageIDX));
				}
			} // 单线程更新 end
		} // bDownPage

			msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = bookname + ": 更新完毕";
			handler.sendMessage(msg);

			handler.sendEmptyMessage(DO_REFRESHLIST); // 更新完毕，通知刷新
		}
	}

	public class FoxTaskDownPage implements Runnable { // 多线程任务更新页面列表
		List<Map<String, Object>> taskList;
	
		public FoxTaskDownPage(List<Map<String, Object>> iTaskList) {
			this.taskList = iTaskList ;
		}

		public void run() {
			Message msg;
			String thName = Thread.currentThread().getName();
			int locCount = 0 ;
			int allCount = taskList.size();
			for (Map<String, Object> tsk : taskList) {
				++ locCount ;
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = leftThread + ":" + thName + ":" + locCount + " / " + allCount ;
				handler.sendMessage(msg);

				nm.updatePage((Integer)tsk.get(NV.BookIDX), (Integer)tsk.get(NV.PageIDX));
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

	private void refresh_BookList() { // 刷新LV中的数据
		data = nm.getBookList(); // 获取书籍列表
		lv_booklist.setAdapter(new SimpleAdapter(this, data, R.layout.lv_item_booklist
			, new String[] { NV.BookName, NV.PagesCount }
			, new int[] { R.id.tvName, R.id.tvCount } )); // 设置listview的Adapter, 当data是原data时才能 adapter.notifyDataSetChanged();
		setItemPos4Eink();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(settings.getBoolean("isClickHomeExit", false)); // 标题栏中添加返回图标
		getActionBar().setDisplayShowHomeEnabled(settings.getBoolean("isShowAppIcon", true)); // 隐藏程序图标
	}		// 响应点击事件在onOptionsItemSelected的switch中加入 android.R.id.home this.finish();

	public void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_booklist);

		showHomeUp();
		isShowIfRoom = settings.getBoolean("isShowIfRoom", isShowIfRoom);
		lv_booklist = getListView(); // 获取LV
// GUI 布局显示完毕
		mExitTime = System.currentTimeMillis(); // 当前时间，便于两次退出
		this.wDir = ToolAndroid.getDefaultDir(settings);
		this.cookiesFile = new File(wDir, "FoxBook.cookie");

		File inShelfFile; // 传入的路径(db3/fml文件)
		if ( getIntent().getData() == null ) {
			bShelfFileFromIntent = false;
			inShelfFile = new File(this.wDir, "FoxBook.fml");
			if ( ! inShelfFile.exists() ) {
				inShelfFile = new File(this.wDir, "FoxBook.db3");
				if ( ! inShelfFile.exists() )
					inShelfFile = new File(this.wDir, "FoxBook.fml");
			}
		 } else {
			bShelfFileFromIntent = true;
			inShelfFile = new File(getIntent().getData().getPath());
			foxtip("注意:\n退出时不会保存修改哦\n如要保存修改，按菜单键并选择菜单");
		}
		setTitle(inShelfFile.getName());
		this.nm = new NovelManager(inShelfFile); // Todo: 修改db导入方式
		((FoxApp)this.getApplication()).nm = this.nm ;

		refresh_BookList();
		init_handler(); // 初始化一个handler 用于处理后台线程的消息

		// 初始化 长击 条目 的行为
		lv_booklist.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> book = (HashMap<String, Object>) parent.getItemAtPosition(position);
				setTitle(book.get(NV.BookName) + " : " + book.get(NV.PagesCount));
				lvItemLongClickDialog(book);
				return true;
			}
		}); // long click end

		if ( 0 == data.size() ) { // 没有书，跳转到搜索页面
			foxtip("貌似没有书哦，那偶自己打开搜索好了");
			startActivityForResult(new Intent(Activity_BookList.this, Activity_SearchBook.class),4);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Map<String, Object> book = (HashMap<String, Object>) l.getItemAtPosition(position);
		setTitle(book.get(NV.BookName) + " : " + book.get(NV.BookURL));

		if ( (Integer)book.get(NV.PagesCount) > 0) { // 章节数大于0
			Intent itt = new Intent(Activity_BookList.this, Activity_PageList.class);
			itt.putExtra(AC.action, AC.aListBookPages);
			itt.putExtra(NV.BookIDX, (Integer)book.get(NV.BookIDX));
			startActivityForResult(itt, 1);
		}
		super.onListItemClick(l, v, position, id);
	}

	private void lvItemLongClickDialog(final Map<String, Object> book) { // 长击LV条目弹出的对话框
		final String lcURL = book.get(NV.BookURL).toString();
		final String lcName = book.get(NV.BookName).toString();
		final int bookIDX = (Integer)book.get(NV.BookIDX);

		new AlertDialog.Builder(this) //.setIcon(R.drawable.ic_launcher);
		.setTitle("操作:" + lcName)
		.setItems(new String[] { "更新本书",
				"更新本书目录",
				"在线查看",
				"搜索:起点",
				"搜索:bing",
				"复制书名",
				"编辑本书信息",
				"删除本书" },
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0: // 更新本书
					upchacount = 0 ;
					new Thread(new UpdateBook(bookIDX, lcURL, lcName, true)).start();
					foxtip("正在更新: " + lcName);
					break;
				case 1: // 更新本书目录
					upchacount = 0 ;
					new Thread(new UpdateBook(bookIDX, lcURL, lcName, false)).start();
					foxtip("正在更新目录: " + lcName);
					break;
				case 2: // 在线查看
					Intent itt = new Intent(Activity_BookList.this, Activity_PageList.class);
					itt.putExtra(AC.action, AC.aListSitePages);
					itt.putExtra(NV.BookIDX, bookIDX);
					startActivityForResult(itt, 1);
					break;
				case 3: // 搜索:起点
					String lcQidianID = nm.getBookInfo(bookIDX).get(NV.QDID).toString();

					if ( null == lcQidianID | 0 == lcQidianID.length() | lcQidianID == "0" ) {
						Intent ittQDS = new Intent(Activity_BookList.this, Activity_SearchBook.class);
						ittQDS.putExtra(AC.action, AC.aListQDPages);
						ittQDS.putExtra(NV.BookName, lcName);
						startActivity(ittQDS);
					} else { // 存在起点ID
						String lcQidianURL = new SiteQiDian().getTOCURL_Android7(lcQidianID) ;

						Intent ittQD = new Intent(Activity_BookList.this, Activity_PageList.class);
						ittQD.putExtra(AC.action, AC.aListQDPages);
						ittQD.putExtra(NV.BookIDX, bookIDX);
						ittQD.putExtra(NV.TmpString, lcQidianURL);
						startActivityForResult(ittQD, 1);
					}
					break;
				case 4: // 搜索:bing
					Intent ittSEB = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
					ittSEB.putExtra(NV.BookName, lcName);
					ittSEB.putExtra(AC.searchEngine, AC.SE_BING);
					startActivityForResult(ittSEB, 5);
					break;
				case 5: // 复制书名
					ToolAndroid.setClipText(lcName, getApplicationContext());
					foxtip("已复制到剪贴板: " + lcName);
					break;
				case 6: // 编辑本书信息
					Intent ittBookInfo = new Intent(Activity_BookList.this,Activity_BookInfo.class);
					ittBookInfo.putExtra(NV.BookIDX, bookIDX);
					startActivityForResult(ittBookInfo, 3);
					break;
				case 7: // 删除本书
					nm.deleteBook(bookIDX);
					refresh_BookList();
					foxtip("已删除: " + lcName);
					break;
				}
			}
		})
		.create().show();
	}

	private void init_handler() { // 初始化一个handler 用于处理后台线程的消息
		handler = new Handler(new Handler.Callback() {
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
				case DO_UPDATEFINISH:
					setTitle((String)msg.obj);
					int xCount = nm.getLess1KCount();
					if ( xCount > 0 ) {
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
						i.setDataAndType(Uri.fromFile(new File(wDir, "FoxBook.apk")), "application/vnd.android.package-archive"); 
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.booklist, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
				case R.id.action_exitwithnosave:
					if ( bShelfFileFromIntent )
						menu.getItem(i).setTitle("保存并退出");
					break;
				case R.id.action_switchShelf:
					if ( bShelfFileFromIntent )
						menu.getItem(i).setVisible(false);
					if ( isShowIfRoom )
						setTypeOfShowAsAction(menu.getItem(i));
					break;
				case R.id.action_allpagelist:
					if ( isShowIfRoom )
						setTypeOfShowAsAction(menu.getItem(i));
					break;
				case R.id.action_updateall:
					if ( isShowIfRoom )
						setTypeOfShowAsAction(menu.getItem(i));
					break;
			}
		}
		return true;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setTypeOfShowAsAction(MenuItem mi) {
		mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM); // SHOW_AS_ACTION_NEVER // API > 11
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;
		case R.id.setting:
			startActivity(new Intent(Activity_BookList.this, Activity_Setting.class));
			break;
		case R.id.action_updateall: // 更新所有
			new Thread(new UpdateAllBook()).start();
			break;
		case R.id.action_switchShelf:
			this.setTitle("切换数据库");
			if ( switchShelfLock ) {
				foxtip("还在切换中...");
			} else {
				(new Thread(){
					public void run(){
						switchShelfLock = true;
						beforeSwitchShelf = settings.getString("beforeSwitchShelf", beforeSwitchShelf);
						if ( ! beforeSwitchShelf.equalsIgnoreCase("none") ) { // 切换前先排序
							if ( beforeSwitchShelf.equalsIgnoreCase("orderby_count_desc") )
								nm.sortBooks(true);
							if ( beforeSwitchShelf.equalsIgnoreCase("orderby_count_asc") )
								nm.sortBooks(false);
							nm.simplifyAllDelList();
						}
						String nowPath = nm.switchShelf().getName();
						switchShelfLock = false;
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
		case R.id.action_searchbook: // 打开搜索书籍
			startActivityForResult(new Intent(Activity_BookList.this, Activity_SearchBook.class),4);
			break;

		case R.id.action_allpagelist: // 所有章节
			Intent ittall = new Intent(Activity_BookList.this, Activity_PageList.class);
			ittall.putExtra(AC.action, AC.aListAllPages);
			startActivityForResult(ittall, 2);
			break;
		case R.id.action_showokinapl: // 显示字数少于1K的章节
			Intent ittlok = new Intent(Activity_BookList.this, Activity_PageList.class);
			ittlok.putExtra(AC.action, AC.aListLess1KPages);
			startActivityForResult(ittlok, 2);
			break;

		case R.id.action_sortbook_asc: // 顺序排序
			this.setTitle("顺序排序");
			(new Thread(){ public void run(){
					nm.sortBooks(false);
					nm.simplifyAllDelList();
					Message msg = Message.obtain();
					msg.what = DO_REFRESH_TIP;
					msg.obj = "已按页面页数顺序重排好书籍";
					handler.sendMessage(msg);
				} }).start();
			break;
		case R.id.action_sortbook_desc: // 倒序排序
			this.setTitle("倒序排序");
			(new Thread(){ public void run(){
				nm.sortBooks(true);
				nm.simplifyAllDelList();
				Message msg = Message.obtain();
				msg.what = DO_REFRESH_TIP;
				msg.obj = "已按页面页数倒序重排好书籍";
				handler.sendMessage(msg);
			} }).start();
			break;
		case R.id.action_all2epub:
			setTitle("开始转换成EPUB...");
			(new Thread(){
				public void run(){
					File oFile = new File(wDir, "fox.epub");
					nm.exportAsEpub(oFile);
					Message msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = "全部转换完毕: " + oFile.getPath();
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_all2txt:
			setTitle("开始转换成TXT...");
			(new Thread(){
				public void run(){
					File oFile = new File(wDir, "fox.txt");
					nm.exportAsTxt(oFile);
					Message msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = "全部转换完毕: " + oFile.getPath();
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
		case R.id.action_exitwithnosave: // 不保存数据库退出
			if ( bShelfFileFromIntent ) // 保存数据库并退出
				beforeExitApp();
			this.finish();
			System.exit(0);
			break;
		case R.id.action_foxhttpd: // 启动停止服务器
			int nowListenPort = 8888 ;
			String nowIP = "127.0.0.1" ;
			HashMap<String, String> hmwifi = ToolAndroid.getWifiInfo(getApplicationContext());
			if ( null != hmwifi )
				nowIP = hmwifi.get("ip");
			boolean bStartIt = ! item.isChecked(); // 根据选项是否选中确定是否开启
			if (bStartIt) {
				try {
					foxHTTPD = new FoxHTTPD(nowListenPort, wDir, nm);
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
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent retIntent) {
		if (RESULT_OK == resultCode)
			refresh_BookList(); // 刷新LV中的数据
	}

	private void beforeExitApp() {
		if ( ! settings.getBoolean("isSaveAsFML", true) )
			nm.setSaveFormat(NovelManager.SQLITE3);
		nm.close();
		if (foxHTTPD != null)
			foxHTTPD.stop();
	}

	@Override
	public void onBackPressed() { // 返回键被按
		if ((System.currentTimeMillis() - mExitTime) > 2000) { // 两次退出键间隔
			foxtip("再按一次退出程序");
			mExitTime = System.currentTimeMillis();
		} else {
			if ( ! bShelfFileFromIntent ) // 不保存数据库并退出
				beforeExitApp();
			this.finish();
			System.exit(0);
		}
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}
