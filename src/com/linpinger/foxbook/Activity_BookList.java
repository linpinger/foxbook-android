package com.linpinger.foxbook;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.novel.Action_UpdateNovel;
import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.ToolAndroid;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
//import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;

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
public class Activity_BookList extends Activity {

	private float clickX = 0 ; // 用来确定点击横坐标以实现不同LV不同区域点击效果
	private NovelManager nm ;
	File wDir ;			// 工作目录
	File cookiesFile ;	// 保存有cookie的文件名: FoxBook.cookie

	// 设置: 
	SharedPreferences settings;
	private String beforeSwitchShelf = "orderby_count_desc" ; // 和 arrays.xml中的beforeSwitchShelf_Values 对应
	private boolean isUpdateBlankPagesFirst = true; // 更新前先检测是否有空白章节
	private boolean isCompareShelf = true ;		// 更新前比较书架

	private boolean bShelfFileFromIntent = false; // 是否是通过文件关联进来的，会修改不保存退出菜单功能
	private boolean isSaveWhenOpenY = false ; // 非默认路径的选项菜单 不显示且不保存

	ListView lv;
	TextView info;
	List<Map<String, Object>> data;

	private static Handler handler;
	private final int DO_SETTITLE = 1;
	private final int DO_REFRESHLIST = 3;
	private final int DO_REFRESH_TIP = 4;
	private final int IS_NEWVER = 5;
	private final int DO_REFRESH_SETTITLE = 6 ;
	private final int DO_UPDATEFINISH = 7;

	private boolean switchShelfLock = false;
	private long mExitTime;

	private void refresh_BookList() { // 刷新LV中的数据
		data = nm.getBookList(); // 获取书籍列表
		lv.setAdapter(new SimpleAdapter(this, data, R.layout.lv_item_booklist
			, new String[] { NV.BookName, NV.PagesCount }
			, new int[] { R.id.tvName, R.id.tvCount } )); // 设置listview的Adapter, 当data是原data时才能 adapter.notifyDataSetChanged();
	}

	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		this.setTheme(android.R.style.Theme_Holo_Light_NoActionBar); // 无ActionBar
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_booklist);

		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isSaveWhenOpenY = settings.getBoolean("isSaveWhenOpenY", isSaveWhenOpenY);

		lv = (ListView)this.findViewById(R.id.testLV); // 获取LV
		info = (TextView)this.findViewById(R.id.testTV);

		if ( ! ToolAndroid.isEink() ) {
			lv.getRootView().setBackgroundColor(Color.parseColor("#EEFAEE"));
			//lv.setBackgroundColor(Color.parseColor("#EEFAEE"));
		}
		init_LV_Touch() ;
		init_ToolBar_Button_LongClick();

		initQuickButton();

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
			if ( ! isSaveWhenOpenY )
				foxtip("注意:\n退出时不会保存修改哦\n如要保存修改，按菜单键并选择菜单");
		}
		foxtipL(inShelfFile.getName());
		this.nm = new NovelManager(inShelfFile); // Todo: 修改db导入方式
		((FoxApp)this.getApplication()).nm = this.nm ;

		init_handler(); // 初始化一个handler 用于处理后台线程的消息
		refresh_BookList();

		if ( 0 == data.size() ) { // 没有书，跳转到搜索页面
			foxtip("貌似没有书哦，那偶自己打开搜索好了");
			startActivityForResult(new Intent(Activity_BookList.this, Activity_SearchBook.class), 4);
		}
	} // onCreate end

	void init_LV_Touch() {
		lv.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent arg1) {
				clickX = arg1.getX(); // 点击横坐标
				return false;
			}
		});
		lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> av, View v, int position, long id) {
				Map<String, Object> book = (HashMap<String, Object>) av.getItemAtPosition(position);
				foxtipL(book.get(NV.BookName)
					+ " : " + book.get(NV.BookAuthor)
//					+ " @ " + book.get(NV.QDID)
//					+ " = " + book.get(NV.PagesCount)
					+ "\n" + book.get(NV.BookURL));

				if ( settings.getBoolean("isClickItemRightAct", true) ) {
					if ( clickX > lv.getWidth() * 0.8 ) { // 右边1/5处弹出菜单
						lvItemLongClickDialog(book);
						return ;
					}
				}

				if ( (Integer)book.get(NV.PagesCount) > 0) { // 章节数大于0
					Intent itt = new Intent(Activity_BookList.this, Activity_PageList.class);
					itt.putExtra(AC.action, AC.aListBookPages);
					itt.putExtra(NV.BookIDX, (Integer)book.get(NV.BookIDX));
					startActivityForResult(itt, 1);
				}
			}
		}); // LV item click end
		// 初始化 长击 条目 的行为
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> av, View view, int position, long id) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> book = (HashMap<String, Object>) av.getItemAtPosition(position);
				foxtipL(book.get(NV.BookName) + " : " + book.get(NV.PagesCount));
				lvItemLongClickDialog(book);
				return true;
			}
		}); // long click end
	}

	void init_ToolBar_Button_LongClick() {
		info.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		info.setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				if ( bShelfFileFromIntent && ( ! isSaveWhenOpenY ) ) // 保存数据库并退出
					beforeExitApp();
				finish();
				System.exit(0);
				return true;
			}
		});
		this.findViewById(R.id.btnSeeAll).setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				showLessThen1K() ;
				return true;
			}
		});
		this.findViewById(R.id.btnSwitch).setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				refresh_BookList(); // 刷新LV中的数据
				foxtip("ListView已刷新");
				return true;
			}
		});
		this.findViewById(R.id.btnOther).setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				startActivityForResult(new Intent(Activity_BookList.this, Activity_SearchBook.class),4); // 打开搜索书籍
				return true;
			}
		});
	}

	public void onBtnClick(View v) {
		switch ( v.getId() ) {
		case R.id.btnOther:
			createPopupMenu(v);
			foxtipL("其他菜单或按钮");
			break;
		case R.id.btnSeeAll:
			Intent ittall = new Intent(Activity_BookList.this, Activity_PageList.class); // 所有章节
			ittall.putExtra(AC.action, AC.aListAllPages);
			startActivityForResult(ittall, 2);
			break;
		case R.id.btnSwitch:
			if ( bShelfFileFromIntent && ( ! isSaveWhenOpenY ) ) {
				foxtipL("骚年，当前不宜切换书架");
				break;
			}
			foxtipL("切换书架");
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
						String nowPath = nm.switchShelf( ( ! bShelfFileFromIntent ) || isSaveWhenOpenY).getName();
						switchShelfLock = false;
						Message msg = Message.obtain();
						msg.what = DO_REFRESH_SETTITLE;
						msg.obj = nowPath;
						handler.sendMessage(msg);
					}
				}).start();
			}
			break;
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void createPopupMenu(View view) { // 弹出菜单
		PopupMenu popW = new PopupMenu(this, view);
		Menu m = popW.getMenu();

		m.add("设置");
		m.add("更新本软件");
		m.add("全部转为TXT");
		m.add("全部转为EPUB");
		m.add("刷新列表");
		m.add("显示字数少于1K的章节");
		if ( bShelfFileFromIntent && ( ! isSaveWhenOpenY ) ) {
			m.add("保存并退出");
		} else {
			m.add("不保存退出");
		}
		m.add("按页数顺序排列");
		m.add("按页数倒序排列");
		m.add("搜索/添加小说");

		popW.show();

		popW.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem mi) {
				String mt = mi.getTitle().toString();
				if ( mt.equalsIgnoreCase("设置") ) {
					startActivity(new Intent(Activity_BookList.this, Activity_Setting.class));
				} else if ( mt.equalsIgnoreCase("刷新列表") ) {
					refresh_BookList(); // 刷新LV中的数据
					foxtip("ListView已刷新");
				} else if ( mt.equalsIgnoreCase("搜索/添加小说") ) {
					startActivityForResult(new Intent(Activity_BookList.this, Activity_SearchBook.class),4); // 打开搜索书籍
				} else if ( mt.equalsIgnoreCase("显示字数少于1K的章节") ) {
					showLessThen1K() ;
				} else if ( mt.equalsIgnoreCase("更新本软件") ) {
					foxtipL("开始更新版本...");
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
				} else if ( mt.equalsIgnoreCase("不保存退出") || mt.equalsIgnoreCase("保存并退出") ) {
					if ( bShelfFileFromIntent && ( ! isSaveWhenOpenY ) ) // 保存数据库并退出
						beforeExitApp();
					finish();
					System.exit(0);
				} else if ( mt.equalsIgnoreCase("按页数倒序排列") ) {
					foxtipL("倒序排序");
					(new Thread(){ public void run(){
						nm.sortBooks(true);
						nm.simplifyAllDelList();
						Message msg = Message.obtain();
						msg.what = DO_REFRESH_TIP;
						msg.obj = "已按页面页数倒序重排好书籍";
						handler.sendMessage(msg);
					} }).start();
				} else if ( mt.equalsIgnoreCase("按页数顺序排列") ) {
					foxtipL("顺序排序");
					(new Thread(){ public void run(){
							nm.sortBooks(false);
							nm.simplifyAllDelList();
							Message msg = Message.obtain();
							msg.what = DO_REFRESH_TIP;
							msg.obj = "已按页面页数顺序重排好书籍";
							handler.sendMessage(msg);
						} }).start();
				} else if ( mt.equalsIgnoreCase("全部转为EPUB") ) {
					foxtipL("开始转换成EPUB...");
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
				} else if ( mt.equalsIgnoreCase("全部转为TXT") ) {
					foxtipL("开始转换成TXT...");
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
				} else {
					foxtipL(mt);
				}
				return true;
			}
		});
	}

	void showLessThen1K() { // 显示字数少于1K的章节
		Intent ittlok = new Intent(Activity_BookList.this, Activity_PageList.class); // 显示字数少于1K的章节
		ittlok.putExtra(AC.action, AC.aListLess1KPages);
		startActivityForResult(ittlok, 2);
		foxtip("已显示字数少于1K的章节");
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if ( keyCode == KeyEvent.KEYCODE_MENU ) {
			createPopupMenu(this.findViewById(R.id.btnOther));
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	void initQuickButton() {
			// 不要再设置onclick和onLongClick了，会冲突
			this.findViewById(R.id.btnRefreshQuick).setOnTouchListener(new OnTouchListener(){
				FrameLayout.LayoutParams lp;
				int startX;
				int startY;
				int lastX;
				int lastY;
				long sTime ;
				long eTime ;
	
				@TargetApi(Build.VERSION_CODES.JELLY_BEAN) @Override
				public boolean onTouch(View v, MotionEvent event) {
					lp = (FrameLayout.LayoutParams) v.getLayoutParams();
					int x = (int) event.getRawX();
					int y = (int) event.getRawY();
	
					switch(event.getAction()){
					case MotionEvent.ACTION_DOWN:
						sTime = System.currentTimeMillis();
						startX = x;
						startY = y;
						lastX = x;
						lastY = y;
						v.getBackground().setAlpha(99);
						// v.getBackground().setColorFilter(new ColorMatrixColorFilter(BUTTON_PRESSED));
						break;
					case MotionEvent.ACTION_MOVE:
						int offX = x - lastX; //计算移动的距离
						int offY = y - lastY;
						lp.rightMargin = lp.rightMargin - offX ;
						lp.bottomMargin = lp.bottomMargin - offY ;
						v.setLayoutParams(lp);
						lastX = x;
						lastY = y;
						break;
					case MotionEvent.ACTION_UP:
						v.getBackground().setAlpha(255);
						//v.getBackground().setColorFilter(new ColorMatrixColorFilter(BUTTON_RELEASED));
						int ofX = lastX - startX;
						int ofY = lastY - startY;
						if ( ( ( ofX >= 0 && ofX < 10 ) || ( ofX <= 0 && ofX > -10 ) ) && ( ( ofY >= 0 && ofY < 10 ) || ( ofY <= 0 && ofY > -10 ) ) ) { // 非移动
							eTime = System.currentTimeMillis() - sTime;
							if ( eTime > 3000L ) { // 长按 3s
								v.playSoundEffect(0);
								foxtipL("长按3s了");
							} else if ( eTime > 300L ) { // 长按 300ms / 500ms
								v.playSoundEffect(0);
								//foxtipL("已长按更新按钮,操作放在这里");
								createPopupMenu(findViewById(R.id.btnOther));
							} else { // 单击
								v.playSoundEffect(0);
								foxtipL("更新所有");
								isUpdateBlankPagesFirst = settings.getBoolean("isUpdateBlankPagesFirst", isUpdateBlankPagesFirst);
								isCompareShelf = settings.getBoolean("isCompareShelf", isCompareShelf); // 更新前比较书架
								new Thread(new UpdateFMLs().new UpdateAllBook(nm, cookiesFile, isUpdateBlankPagesFirst, isCompareShelf)).start();
							}
						}
						break;
					}
					return true;
				}
			});
	//		private final static float[] BUTTON_PRESSED = new float[] {      
	//		2.0f, 0, 0, 0, -50,      
	//		0, 2.0f, 0, 0, -50,      
	//		0, 0, 2.0f, 0, -50,      
	//		0, 0, 0, 5, 0 };
	//	private final static float[] BUTTON_RELEASED = new float[] {      
	//		1, 0, 0, 0, 0,      
	//		0, 1, 0, 0, 0,      
	//		0, 0, 1, 0, 0,      
	//		0, 0, 0, 1, 0 };
		} // initQuickButton end

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
					new Thread(new UpdateFMLs().new UpdateBook(nm, bookIDX, lcURL, lcName, true)).start();
					foxtip("正在更新: " + lcName);
					break;
				case 1: // 更新本书目录
					new Thread(new UpdateFMLs().new UpdateBook(nm, bookIDX, lcURL, lcName, false)).start();
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
					foxtipL((String)msg.obj);
					int xCount = nm.getLess1KCount();
					if ( xCount > 0 ) {
						foxtipL(xCount + ":" + (String)msg.obj);
						foxtip("有 " + xCount + " 章节短于1K");

						// 显示短章节
						Intent ittlok = new Intent(Activity_BookList.this, Activity_PageList.class);
						ittlok.putExtra(AC.action, AC.aListLess1KPages);
						startActivityForResult(ittlok, 2);
					}
					break;
				case DO_SETTITLE:
					foxtipL((String)msg.obj);
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
					foxtipL((String) msg.obj);
					break;
				case IS_NEWVER:
					foxtipL((String)msg.obj);
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

	public class UpdateFMLs extends Action_UpdateNovel {

		public void onStatuChange(int msgWhat, String msgOBJ){
			if ( msgWhat >= LINEBASE ) { // 多线程更新FMLs
				Message msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = msgOBJ;
				handler.sendMessage(msg);
			} else if ( msgWhat == LINEBASE - 1) { // 刷新LV
				handler.sendEmptyMessage(DO_REFRESHLIST); // 更新完毕，通知刷新
			}
		}
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
	}

	@Override
	public void onBackPressed() { // 返回键被按
		if ((System.currentTimeMillis() - mExitTime) > 2000) { // 两次退出键间隔
			foxtip("再按一次退出程序");
			mExitTime = System.currentTimeMillis();
		} else {
			if ( ( ! bShelfFileFromIntent) || isSaveWhenOpenY ) // 不保存数据库并退出
				beforeExitApp();
			this.finish();
			System.exit(0);
		}
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

	private void foxtipL(String sinfo) {
		info.setText(sinfo);
	}

} // class end

