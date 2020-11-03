package com.linpinger.tool;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.widget.ListView;

import java.io.File;
import java.lang.reflect.Method;

public class ToolAndroid {

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void setClipText(String iText, Context ctx) {
		((ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("hello", iText));
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static String getClipText(Context ctx) {
		String retS = "";
		CharSequence xx = ((ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip().getItemAt(0).getText();
		if ( xx != null ) {
			retS = xx.toString();
		} else {
			System.err.println("- 剪贴板获取了空对象: " + ctx.getClass().getName() );
		}
		return retS;
	}

	public static boolean isEink() {
		return "Onyx".equalsIgnoreCase(android.os.Build.BRAND) ;
	}

	public static void c67ml_FullRefresh(View view) { // 全刷 for Boox C67ML Carta, RK3026Device.class, Mode=4, EpdController.invalidate(this, UpdateMode.GC);
		try {
			Class<Enum> ce = (Class<Enum>) Class.forName("android.view.View$EINK_MODE");
			Method mtd = View.class.getMethod("requestEpdMode", new Class[] { ce });
			mtd.invoke(view, new Object[] { Enum.valueOf(ce, "EPD_FULL") });
			view.invalidate();
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	// http://androiddoc.qiniudn.com/reference/android/widget/AdapterView.html#getCount()
	public static int jump2ListViewPos(ListView lv, int position) { // positon有效值: -99=上翻一屏，-66=下翻一屏幕，-1=底部，0=头部，>0
		int jumpPos = position ; // 目标位置
		int nowFirstPos = lv.getFirstVisiblePosition() ; // 可见的第一个item所处位置
		int nowLastPos  = lv.getLastVisiblePosition() ; // 可见的最后一个item所处位置
		int itemCount = lv.getCount(); // adapter里面元素的数量，不一定等于可见数

		if ( position >= itemCount ) { // 数量太大跳到底部
			jumpPos = itemCount - 1;
		} else if ( position == -99 ) { // 上一屏幕
			jumpPos = nowFirstPos - ( nowLastPos - nowFirstPos );
			if ( jumpPos < 0 ) {
				jumpPos = 0;
			}
		} else if ( position == -66 ) {
			jumpPos = nowLastPos; // 下一屏幕
		} else if ( position == -1 ) {
			jumpPos = itemCount - 1; // 底部
		} else if ( position < 0) {
			return nowFirstPos;
		} // 其他>=0的直接跳了

		lv.setSelection(jumpPos);
		return jumpPos;
	}

	public static int sp2px(Context context, float spValue) { // 将sp值转换为px值，保证文字大小不变
		final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
		return (int) (spValue * fontScale + 0.5f);  
	}

//	public static int px2sp(Context context, float pxValue) { // 将px值转换为sp值，保证文字大小不变
//		final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
//		return (int) (pxValue / fontScale + 0.5f);
//	}


	// 将获取的int转为真正的ip地址,参考的网上的，修改了下
	private static String intToIp(int i){
		return ( i & 0xFF) + "." + ((i >> 8 ) & 0xFF) + "." + ((i >> 16 ) & 0xFF) + "." + ((i >> 24 ) & 0xFF ) ;
	}

	public static String getWifiIP(Context context) { // only: Tool
		String wip = "";
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);// 获取WifiManager
		if ( null != wm ) {
			WifiInfo wi = wm.getConnectionInfo();
			wip = intToIp(wi.getIpAddress());
		}
		if ( null == wip ) { wip = "0.0.0.0"; }
		return wip;
	}

	public static String getWifiName(Context context) {
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);// 获取WifiManager
		if ( null != wm ) { return wm.getConnectionInfo().getSSID(); } else { return ""; }
	}
/*
	public String getWifiName9(Context context) {
		String ssid = "";
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);// 获取WifiManager
		if ( null != wm ) {
			WifiInfo wi = wm.getConnectionInfo();
			int nid = wi.getNetworkId();

			if ( null == wm.getConfiguredNetworks() ) { return "木有获取到"; }
			for( WifiConfiguration wc : wm.getConfiguredNetworks() ) {
				if (wc.networkId == nid) {
					ssid = wc.SSID;
					break;
				}
			}
		}

		if ( null == ssid ) { ssid = "木有获取到"; }
		return ssid;
	}
*/
	public static String getMyVersionName(Context ctx) {
		String oStr = "";
		try {
			oStr = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		return oStr;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static String download(String iURL, String saveName, String saveDir, Context ctx) { // only: Tool
		if( Build.VERSION.SDK_INT >= 26 ){ // Android Q 10.0
			saveDir = Environment.DIRECTORY_DOWNLOADS;
		}
		try {
			DownloadManager downloadManager = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
			DownloadManager.Request request = new DownloadManager.Request(Uri.parse(iURL));
			request.setDestinationInExternalPublicDir(saveDir, saveName);
			request.setTitle("下载: " + saveName);
			request.setDescription(saveName);
			// request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			// request.setMimeType("application/cn.trinea.download.file");
			downloadManager.enqueue(request);
		} catch (Exception e) {
			System.err.println("- download: " + e.toString() );
		}
		return saveDir;
	}

	public static void startAPP(String pkgName, Context ctx) {
		startAPP(pkgName, "", null, null, ctx);
	}
//	public static void startAPP(String pkgName, String className, Context ctx) { // "com.ghisler.android.TotalCommander", "com.ghisler.android.TotalCommander.TotalCommander"
//		startAPP(pkgName, className, null, null, ctx);
//	}
	public static void startAPP(String pkgName, String className, Uri data, String iMIME, Context ctx) {
		Intent itt ;
		if ( "".equalsIgnoreCase(pkgName) ) { // 打开文件
			itt = new Intent(Intent.ACTION_VIEW).setDataAndType(data, iMIME);
		} else { // 打开应用
			if ("".equalsIgnoreCase(className)) { // 后面如果className空着，查询以获取itt
				itt = ctx.getPackageManager().getLaunchIntentForPackage(pkgName);  // 非模式窗口，推荐使用这个
			} else {
				itt = new Intent().setComponent(new ComponentName(pkgName, className)); // 模式窗口
				if ( ! pkgName.contains(".linpinger.") ) {
					itt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 非模式
				}
			}
		}

		if ( data != null ) { itt.setDataAndType(data, iMIME); }

		try {
			ctx.startActivity(itt); // 这货可能崩
		} catch(Exception e) {
			System.err.println( e.toString() );
		}
	}

	public static void startFile(Uri data, String iMIME, Context ctx) {
		startAPP("", "", data, iMIME, ctx);
	}
	public static void startFoxBook(File fmlFile, Context ctx) { // only: Tool
		startAPP("com.linpinger.foxbook", "com.linpinger.foxbook.Activity_Main", Uri.fromFile(fmlFile), null, ctx);
	}

}
