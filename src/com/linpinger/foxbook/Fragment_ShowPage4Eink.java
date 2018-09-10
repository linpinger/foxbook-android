package com.linpinger.foxbook;

import java.io.File;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.Site1024;
import com.linpinger.tool.FoxZipReader;
import com.linpinger.tool.ToolAndroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Fragment_ShowPage4Eink extends Fragment {

	public static Fragment_ShowPage4Eink newInstance(NovelManager novelMgr, Bundle iArgs) {
		Fragment_ShowPage4Eink fc = new Fragment_ShowPage4Eink();
		fc.nm = novelMgr;
		fc.setArguments(iArgs);
		return fc;
	}

	public interface OnReadFinishListener {
		public void OnOnReadFinish(String fmlPath, int bookIDX, int pageIDX);
	}
	private OnReadFinishListener offlsner;
	public void setOnReadFinishListener(OnReadFinishListener inListner) { // 如果需要获取返回的阅读进度
		offlsner = inListner;
	}

	@Override
	public void onDestroy() {
		if ( offlsner != null) {
			offlsner.OnOnReadFinish(nm.getShelfFile().getPath(), this.bookIDX, this.pageIDX);
		}
		super.onDestroy();
	}

	private boolean isEink = false ;
	@Override
	public void onAttach(Activity activity) { // 1
		super.onAttach(activity);

		settings = PreferenceManager.getDefaultSharedPreferences(activity);
		editor = settings.edit(); // 获取设置
		if ( settings.getBoolean("isFullScreen", false) )
			activity.getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) { // 3
		ctx = container.getContext();
		this.isEink = ToolAndroid.isEink();

		myBGcolor = settings.getString("myBGcolor", myBGcolor);
		isProcessLongOneLine = settings.getBoolean("isProcessLongOneLine", isProcessLongOneLine);
		isAdd2CNSpaceBeforeLine = settings.getBoolean("isAdd2CNSpaceBeforeLine", isAdd2CNSpaceBeforeLine);

		mv = new FoxTextView(ctx); // 自定义View

		setBGcolor(myBGcolor);
		mv.setBodyBold(settings.getBoolean("isBodyBold", false));

		mv.setOnTouchListener(new OnTouchListener(){ // 触摸事件
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				cX = arg1.getX(); // 获取的坐标给click使用
				cY = arg1.getY(); // getRawX getRawY
				return false;
			}
		});
		mv.setOnClickListener(new OnClickListener(){ // 单击事件
			@Override
			public void onClick(View v) {
				int vy = v.getHeight();
				if ( cY <= vy / 3 ) { // 小于1/3屏高
					int vx = v.getWidth(); // 屏幕宽度
					if ( cX >= vx * 0.6666 ) { // 右上角1/3宽处
						showConfigPopupWindow(v);
					} else if (cX <= vx * 0.333) { // 左上角
						onBackPressed();
					} else {
						mv.clickPrev(); // 上中
					}
				} else {
					mv.clickNext();
				}
			}
		});
		mv.setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				int vy = v.getHeight();
				if ( cY <= vy / 3 ) { // 小于1/3屏高
					int vx = v.getWidth(); // 屏幕宽度
					if ( cX >= vx * 0.6666 ) { // 右上角1/3宽处
						mv.setNextText() ; // 下一章
						mv.postInvalidate();
					} else if (cX <= vx * 0.333) { // 左上角
						onBackPressed();
					} else { // 上中
						mv.setPrevText(); // 上一章
						mv.postInvalidate();
					}
				} else {
					mv.clickPrev(); // 手机显示上一屏
				}
				return true;
			}

		});

		tLastPushEinkButton = System.currentTimeMillis();

		isMapUpKey = settings.getBoolean("isMapUpKey", isMapUpKey);

		fontsize = settings.getFloat("fontsize", (float)ToolAndroid.sp2px(ctx, 18.5f)); // 字体大小
//		fontsize = settings.getFloat("fontsize", fontsize);
		mv.setFontSize(fontsize);

		paddingMultip = settings.getFloat("paddingMultip", paddingMultip);
		mv.setPadding(String.valueOf(paddingMultip) + "f");

		lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
		mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");

		// { Fragment里监听虚拟按键和实体按键的返回事件
		mv.setFocusable(true);
		mv.setFocusableInTouchMode(true);
		mv.requestFocus();
		mv.setOnKeyListener(new OnKeyListener(){
			@Override
			public boolean onKey(View v, int kc, KeyEvent event) {
				// 莫名其妙的按一个按钮出现两个keyCode
				if ( ( event.getAction() == KeyEvent.ACTION_UP ) & ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) ) {
					if ( System.currentTimeMillis() - tLastPushEinkButton < 1000 ) { // 莫名其妙的会多按，也是醉了
						tLastPushEinkButton = System.currentTimeMillis();
						return true ;
					}
					// 2016-8-15: BOOX C67ML Carta 左右翻页健对应: KEYCODE_PAGE_UP = 92, KEYCODE_PAGE_DOWN = 93
					if ( ! isMapUpKey ) {
						if ( KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc ) {
							mv.clickPrev();
							tLastPushEinkButton = System.currentTimeMillis();
							return true;
						}
					}
					mv.clickNext();
					tLastPushEinkButton = System.currentTimeMillis();
					return true;
				}
				if ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) {
					return true;
				}
				if ( KeyEvent.KEYCODE_MENU == kc & event.getAction() == KeyEvent.ACTION_UP ) {
					showConfigPopupWindow(mv);
					return true;
				}
				return false;
			}
		});
		// } Key

		Bundle itt = getArguments(); // 传入参数
		ittAction = itt.getInt(AC.action);
		bookIDX = itt.getInt(NV.BookIDX, -1);
		pageIDX = itt.getInt(NV.PageIDX, -1);
		
		if ( ittAction == AC.aShowPageInMem ) { // 1024DB3
			if ( nm.getBookInfo(bookIDX).get(NV.BookURL).toString().contains("zip://") )
				ittAction = AC.aShowPageInZip1024 ;
		}
		switch ( ittAction ) {
		case AC.aAnotherAppShowContent: // 51用来传递title, content供其他应用直接调用
			mv.setText(itt.getString("title"), "　　" + itt.getString("content").replace("\n", "\n　　"), "从其他应用传来的内容");
			break;
		case AC.aAnotherAppShowOnePageIDX: //"fmlPath": 52需传递bookIDX, pageIDX, 
		case AC.aShowPageInMem:
			if ( AC.aAnotherAppShowOnePageIDX == ittAction ) {
				this.nm = new NovelManager(new File(itt.getString("fmlPath")));
			}
			Map<String, Object> page = nm.getPage(bookIDX, pageIDX);
			String pagetext = page.get(NV.Content).toString();
			if ( null == pagetext | pagetext.length() < 5 )
				pagetext = "本章节内容还没下载，请回到列表，更新本书或本章节" ;
			pagetext = ProcessLongOneLine(pagetext);
			if ( isAdd2CNSpaceBeforeLine ) {
				mv.setText(page.get(NV.PageName).toString()
						, "　　" + pagetext.replace("\n", "\n　　")
						, nm.getBookInfo(bookIDX).get(NV.BookName).toString()
						+ "   " + nm.getPagePosAtShelfPages(bookIDX, pageIDX));
			} else {
				mv.setText(page.get(NV.PageName).toString()
						, pagetext
						, nm.getBookInfo(bookIDX).get(NV.BookName).toString()
						+ "   " + nm.getPagePosAtShelfPages(bookIDX, pageIDX));
			}
			break;
		case AC.aShowPageOnNet: // NET
			final String pageTitle = itt.getString(NV.PageName);
			final String fullPageURL = itt.getString(NV.PageFullURL);
			new Thread( new Runnable() {
				@Override
				public void run() {
					String text = nm.updatePage(fullPageURL) ;

					HashMap<String, String> page = new HashMap<String, String>(2);
					page.put(NV.PageName, pageTitle);
					page.put(NV.Content, text);
					page.put(NV.PageFullURL, fullPageURL);

					Message msg = Message.obtain();
					msg.what = IS_REFRESH;
					msg.obj = page;
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case AC.aShowPageInZip1024: // ZIP文件
			String zipPageFullURL = itt.getString(NV.PageFullURL);
			Matcher mat = Pattern.compile("(?i)^zip://([^@]*?)@([^@]*)$").matcher(zipPageFullURL);
			String zipRelPath = "";
			String zipItemName = "";
			while (mat.find()) {
				zipRelPath = mat.group(1) ;
				zipItemName = mat.group(2) ;
			}

			ZIPFILE = new File(nm.getShelfFile().getParent() + "/" + zipRelPath);
			String html = FoxZipReader.getUtf8TextFromZip(ZIPFILE, zipItemName);
			String content = "";
			String pageName = "";
			if ( html.contains("\"tpc_content\"") ) {
				HashMap<String, Object> xx = new Site1024().getContentTitle(html);
				pageName = xx.get(NV.PageName).toString();
				content = xx.get(NV.Content).toString();
			}

			if ( isAdd2CNSpaceBeforeLine ) {
				mv.setText(pageName , "　　" + content.replace("\n", "\n　　")
						, nm.getBookInfo(bookIDX).get(NV.BookName).toString()
						+ "   " + nm.getPagePosAtShelfPages(bookIDX, pageIDX));
			} else {
				mv.setText(pageName , content
						, nm.getBookInfo(bookIDX).get(NV.BookName).toString()
						+ "   " + nm.getPagePosAtShelfPages(bookIDX, pageIDX));
			}
			break;
		}

		return mv;
	} // oncreate 结束

	private class onViewClickListener implements View.OnClickListener { // 单击
		@Override
		public void onClick(View v) {
			switch ( v.getId() ) {
			case R.id.pc_chap_prev:
				mv.setPrevText(); // 上一章
				mv.postInvalidate();
				break;
			case R.id.pc_chap_next:
				mv.setNextText() ; // 下一章
				mv.postInvalidate();
				break;
			case R.id.pc_size_add:
				changeView(11); // 增大字体
				break;
			case R.id.pc_size_sub:
				changeView(9); // 减小字体
				break;
			case R.id.pc_size_text:
				changeView(10); // 默认 字体
				break;
			case R.id.pc_line_add:
				changeView(21); // 加 行间距
				break;
			case R.id.pc_line_sub:
				changeView(19); // 减 行间距
				break;
			case R.id.pc_line_text:
				changeView(20); // 默认 行间距
				break;
			case R.id.pc_left_add:
				changeView(31); // 增大页边距
				break;
			case R.id.pc_left_sub:
				changeView(29); // 减小页边距
				break;
			case R.id.pc_left_text:
				changeView(30); // 默认 页边距
				break;
			case R.id.pc_bg_a:
				setBGcolor("green");
				break;
			case R.id.pc_bg_b:
				setBGcolor("gray");
				break;
			case R.id.pc_bg_c:
				setBGcolor("white");
				break;
			case R.id.pc_bg_d:
				setBGcolor("default");
				break;
			case R.id.pc_useFont:
				changeView(66);
				break;
			case R.id.pc_setting:
				changeView(99);
				break;
			} // switch end
		}
	}

	private void showConfigPopupWindow(View view) {

		// 一个自定义的布局，作为显示的内容
		View popView = LayoutInflater.from(ctx).inflate(R.layout.popupwin_showpage, null);

		popView.findViewById(R.id.pc_chap_prev).setOnLongClickListener(olcl_exit);

		onViewClickListener cl = new onViewClickListener();
		// 设置按钮的点击事件
		popView.findViewById(R.id.pc_chap_prev).setOnClickListener(cl);
		popView.findViewById(R.id.pc_chap_next).setOnClickListener(cl);

		popView.findViewById(R.id.pc_size_add).setOnClickListener(cl);
		popView.findViewById(R.id.pc_size_sub).setOnClickListener(cl);
		popView.findViewById(R.id.pc_size_text).setOnClickListener(cl);

		popView.findViewById(R.id.pc_line_add).setOnClickListener(cl);
		popView.findViewById(R.id.pc_line_sub).setOnClickListener(cl);
		popView.findViewById(R.id.pc_line_text).setOnClickListener(cl);

		popView.findViewById(R.id.pc_left_sub).setOnClickListener(cl);
		popView.findViewById(R.id.pc_left_add).setOnClickListener(cl);
		popView.findViewById(R.id.pc_left_text).setOnClickListener(cl);

		popView.findViewById(R.id.pc_bg_a).setOnClickListener(cl);
		popView.findViewById(R.id.pc_bg_b).setOnClickListener(cl);
		popView.findViewById(R.id.pc_bg_c).setOnClickListener(cl);
		popView.findViewById(R.id.pc_bg_d).setOnClickListener(cl);

		popView.findViewById(R.id.pc_useFont).setOnClickListener(cl);
		popView.findViewById(R.id.pc_useFont).setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				changeView(67);
				return true;
			}
		});
		popView.findViewById(R.id.pc_setting).setOnClickListener(cl);
		popView.findViewById(R.id.pc_setting).setOnLongClickListener(olcl_exit); // e-ink: 27.5 1.7 0.8

		popView.setFocusable(true);
		popView.setFocusableInTouchMode(true);
		popView.setOnKeyListener(new OnKeyListener() {  
			public boolean onKey(View v, int keyCode, KeyEvent event) {  
				if ( keyCode == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_UP) {
					configPopWin.dismiss();
					isMenuShow = false;
					return true;
				}
				return false;
			}  
		});

		configPopWin = new PopupWindow(popView, 
				LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT,
				true); // MATCH_PARENT

				configPopWin.setTouchable(true);
				configPopWin.setOutsideTouchable(true);

				configPopWin.setTouchInterceptor(new OnTouchListener() {
					@Override
					public boolean onTouch(View arg0, MotionEvent arg1) {
						return false; // 这里如果返回true的话，touch事件将被拦截，拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
					}
				});

				// 如果不设置PopupWindow的背景，无论是点击外部区域还是Back键都无法dismiss弹框
				// 我觉得这里是API的一个bug
				configPopWin.setBackgroundDrawable(new ColorDrawable(0));

				// 设置好参数之后再show
				//configPopWin.showAsDropDown(view);
				configPopWin.showAtLocation(view, Gravity.CENTER, 0, 0); // 中间

				isMenuShow = true;
				// 有个小bug不知道怎么排除: 按menu键: 显示，消失，显示，显示，消失，显显消...
	}

	private class FoxTextView extends View_FoxTextView {
		public FoxTextView(Context context) {
			super(context);
		}

		private int setPrevOrNextText(boolean isNextPage) {
			if ( -1 == pageIDX ) { // 网络，应重定义为-1
				foxtip("亲，ID 为 -1");
				return -1;
			}

			Map<String, Object> mp ;
			String strNoMoreTip ;
			if ( isNextPage ) {
				mp = nm.getNextPage(bookIDX, pageIDX);
				strNoMoreTip = "亲，没有下一页了";
			} else {
				mp = nm.getPrevPage(bookIDX, pageIDX);
				strNoMoreTip = "亲，没有上一页了";
			}
			if ( null == mp ) {
				foxtip(strNoMoreTip);
				return -2;
			}
			if ( ittAction == AC.aShowPageInZip1024 ) {
				String html = FoxZipReader.getUtf8TextFromZip(ZIPFILE, mp.get(NV.PageURL).toString());
				if ( html.contains("\"tpc_content\"") )
					mp.putAll(new Site1024().getContentTitle(html));
			}
			String newBookAddWJX = "★" ;
			if ( (Integer)mp.get(NV.BookIDX) == bookIDX )
				newBookAddWJX = "" ;

			bookIDX = (Integer) mp.get(NV.BookIDX);
			pageIDX = (Integer) mp.get(NV.PageIDX);
			String pagetext = mp.get(NV.Content).toString();
			if ( pagetext.length() == 0 ) {
				foxtip("内容未下载: " + mp.get(NV.PageName));
				return -3;
			}
			pagetext = ProcessLongOneLine(pagetext);
			if ( isAdd2CNSpaceBeforeLine ) {
				mv.setText(newBookAddWJX + mp.get(NV.PageName).toString()
						, "　　" + pagetext.replace("\n", "\n　　")
						, nm.getBookInfo(bookIDX).get(NV.BookName).toString()
						+ "   " + nm.getPagePosAtShelfPages(bookIDX, pageIDX));
			} else {
				mv.setText(newBookAddWJX + mp.get(NV.PageName).toString()
						, pagetext
						, nm.getBookInfo(bookIDX).get(NV.BookName).toString()
						+ "   " + nm.getPagePosAtShelfPages(bookIDX, pageIDX));
			}
			return 0;
		}

		@Override
		public int setPrevText() {
			return setPrevOrNextText(false); // 上一
		}

		@Override
		public int setNextText() {
			return setPrevOrNextText(true); // 下一
		}

		@Override
		public void clickPrev() {
			super.clickPrev();
			if ( isEink ) { ToolAndroid.c67ml_FullRefresh(this); }
		}

		@Override
		public void clickNext() {
			super.clickNext();
			if ( isEink ) { ToolAndroid.c67ml_FullRefresh(this); }
		}
	}

	void changeView(int actionID) {
		switch (actionID) {
		case 9:  // 减小 字体
		case 10: // 默认 字体
		case 11: // 增大 字体
			if ( 9 == actionID ) {
				fontsize -= 0.5f ;
			} else if ( 10 == actionID ) {
				fontsize = 36.5f;
				if ( isEink ) { fontsize = 27.5f;}
			} else if ( 11 == actionID ) {
				fontsize += 0.5f ;
			}
			mv.setFontSize(fontsize).postInvalidate();
			editor.putFloat("fontsize", fontsize).commit();
			((TextView) configPopWin.getContentView().findViewById(R.id.pc_size_text)).setText(new DecimalFormat("#.0").format(fontsize));
			// foxtip("字体大小: " + fontsize);
			break;
		case 19: // 减小 行间距
		case 20: // 默认 行间距
		case 21: // 增大 行间距
			if ( 19 == actionID ) {
				lineSpaceingMultip -= 0.05f ;
			} else if ( 20 == actionID ) {
				lineSpaceingMultip = 1.55f;
				if ( isEink ) { lineSpaceingMultip = 1.7f;}
			} else if ( 21 == actionID ) {
				lineSpaceingMultip += 0.05f ;
			}
			mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f").postInvalidate();
			editor.putFloat("lineSpaceingMultip", lineSpaceingMultip).commit();
			((TextView) configPopWin.getContentView().findViewById(R.id.pc_line_text)).setText(new DecimalFormat("#.00").format(lineSpaceingMultip));
			// foxtip("行间距: " + lineSpaceingMultip);
			break;
		case 29: // 减小 页边距
		case 30: // 默认 页边距
		case 31: // 增大 页边距
			if ( 29 == actionID ) {
				paddingMultip -= 0.1f ;
			} else if ( 30 == actionID ) {
				paddingMultip = 0.5f;
				if ( isEink ) { paddingMultip = 0.8f;}
			} else if ( 31 == actionID ) {
				paddingMultip += 0.1f ;
			}
			mv.setPadding(String.valueOf(paddingMultip) + "f").postInvalidate();
			editor.putFloat("paddingMultip", paddingMultip).commit();
			((TextView) configPopWin.getContentView().findViewById(R.id.pc_left_text)).setText(new DecimalFormat("0.0").format(paddingMultip));
			// foxtip("页边距: " + paddingMultip);
			break;
		case 66:
			String nowFontPath = settings.getString("selectfont", "/sdcard/fonts/foxfont.ttf") ;
			if ( ! mv.setUserFontPath(nowFontPath) ) {
				foxtip("字体不存在:\n\n" + nowFontPath + "\n\n现在选择字体文件");
				selectFile();
			} else {
				mv.postInvalidate();
			}
			break;
		case 67:
			selectFile();
			break;
		case 99:
			startFragment( new Fragment_Setting() );
			break;
		}
	}

//	public void selectFile() { // todo
//		Fragment_FileChooser fc = Fragment_FileChooser.newInstance( android.os.Environment.getExternalStorageDirectory().getPath() );
//		fc.setOnFileChoosenListener(new Fragment_FileChooser.OnFileChoosenListener(){
//			@Override
//			public void OnFileChoosen(File selectedFile) {
//				if ( selectedFile != null ) {
//					String newFont = selectedFile.getPath();
//					String nowPATH = newFont.toLowerCase() ;
//					if ( nowPATH.endsWith(".ttf") | nowPATH.endsWith(".ttc") | nowPATH.endsWith(".otf") ) {
//						editor.putString("selectfont", newFont).commit();
//						foxtip(newFont);
//						mv.setUserFontPath(newFont);
//						mv.postInvalidate();
//					} else {
//						foxtip("要选择后缀为.ttf/.ttc/.otf的字体文件");
//					}
//				}
//			}
//		});
//		startFragment( fc );
//	}
	public void selectFile() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, 9);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 9: // 响应文件选择器的选择
			if (resultCode == Activity.RESULT_OK) {
				Uri uri = data.getData();
				// 判断文件名后缀
				String newFont = new File(uri.getPath()).getAbsolutePath();
				if ( newFont.contains("/document/primary:") ) { // 神奇的路径
					newFont = "/sdcard/" + newFont.split(":")[1];
				}
				String nowPATH = newFont.toLowerCase() ;
				if ( nowPATH.endsWith(".ttf") | nowPATH.endsWith(".ttc") | nowPATH.endsWith(".otf") ) {
					editor.putString("selectfont", newFont);
					editor.commit();
					foxtip("\n" + uri.getPath() + "\n" + newFont);
					mv.setUserFontPath(newFont);
					mv.postInvalidate();
				} else {
					foxtip("要选择后缀为.ttf/.ttc/.otf的字体文件");
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	private void setBGcolor(String bgcolor) {
		myBGcolor = bgcolor ;
		editor.putString("myBGcolor", myBGcolor).commit();

		if ( myBGcolor.equalsIgnoreCase("white") )
			mv.setBackgroundResource(R.color.qd_mapp_bg_white); // 白色背景
		if ( myBGcolor.equalsIgnoreCase("green") )
			mv.setBackgroundResource(R.color.qd_mapp_bg_green);
		if ( myBGcolor.equalsIgnoreCase("default") )
			mv.setBackgroundResource(R.drawable.parchment_paper); // 绘制两次
		if ( myBGcolor.equalsIgnoreCase("gray") )
			mv.setBackgroundResource(R.color.qd_mapp_bg_grey); // 灰色
	}

	private String ProcessLongOneLine(String iText) {
		if ( ! isProcessLongOneLine )
			return iText ;

		//		long sTime = System.currentTimeMillis();

		int sLen = iText.length(); // 字符数
		if ( sLen == 0 )
			return "";
		int lineCount = 0 ; // 行数
		try {
			LineNumberReader lnr = new LineNumberReader(new StringReader(iText));
			while ( null != lnr.readLine() )
				++ lineCount;
		} catch ( Exception e ) {
			e.toString();
		}
		if ( ( sLen / lineCount ) > 200 ) { // 小于200(也许可以定到130)意味着换行符够多，处理的时候不会太卡，大于阈值200就得处理得到足够多的换行符
			//			Log.e("XX", "Cal : " + sLen + " / " +  lineCount + " >= 200 ");
			iText = iText.replace("。", "。\n");
			iText = iText.replace("\n\n", "\n");
		}

		//		Log.e("TT", "Time=" + ( System.currentTimeMillis() - sTime ) );
		return iText;
	}

	OnLongClickListener olcl_exit = new OnLongClickListener() { // 专门用于长按退出
		@Override
		public boolean onLongClick(View arg0) {
			onBackPressed();
			return true;
		}
	};

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(ctx, sinfo, Toast.LENGTH_SHORT).show();
	}
	private void onBackPressed() {
		getActivity().onBackPressed();
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void startFragment(Fragment fragmt) {
		getFragmentManager().beginTransaction().hide(this).add(android.R.id.content, fragmt).addToBackStack(null).commit();
	}

	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if ( msg.what == IS_REFRESH ) {
				HashMap<String, String> page = (HashMap<String, String>)msg.obj;
				String sText = page.get(NV.Content);
				if ( sText.length() < 9 ) {
					mv.setText("错误", "　　啊噢，可能处理的时候出现问题了哦\n\nURL: "
							+ page.get(NV.PageFullURL)
							+ "\nPageName: " + page.get(NV.PageName)
							+ "\nContent:" + sText);
				} else {
					sText = ProcessLongOneLine(sText);
					if ( isAdd2CNSpaceBeforeLine ) {
						mv.setText(page.get(NV.PageName).toString(), "　　" + sText.replace("\n", "\n　　"));
					} else {
						mv.setText(page.get(NV.PageName).toString(), sText);
					}
				}
				mv.postInvalidate();
			}
		}
	};

	Context ctx;

	// { Var Init
	private NovelManager nm;

	private int ittAction = 0; // 传入数据
	private int bookIDX = -1 ; // 翻页时使用
	private int pageIDX = -1 ; // 翻页时使用

	FoxTextView mv;
	private float cX = 0 ; // 点击View的坐标
	private float cY = 0 ; // 点击View的坐标

	SharedPreferences settings;
	SharedPreferences.Editor editor;
	private String myBGcolor = "green"; // 背景:默认羊皮纸
	private boolean isMapUpKey = false;   // 是否映射上翻为下翻键

	// 2018-06 : phone: 36.5 1.55 0.5    e-ink: 27.5 1.7 0.8
	private float fontsize = 36.5f; // 字体大小
	private float lineSpaceingMultip = 1.55f ; // 行间距倍数
	private float paddingMultip = 0.5f ; // 页边距 = 字体大小 * paddingMultip
	private boolean isProcessLongOneLine = true; // 处理超长单行 > 4K
	private boolean isAdd2CNSpaceBeforeLine = true ; // 自动添加两空白

	private long tLastPushEinkButton ;
	private final int IS_REFRESH = 5 ;

	private File ZIPFILE ;

	PopupWindow configPopWin;
	boolean isMenuShow = false;
	// } Var Init

}
