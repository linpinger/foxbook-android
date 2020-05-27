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

	// 将获取的int转为真正的ip地址,参考的网上的，修改了下
	private static String intToIp(int i){
		return ( i & 0xFF) + "." + ((i >> 8 ) & 0xFF) + "." + ((i >> 16 ) & 0xFF) + "." + ((i >> 24 ) & 0xFF ) ;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void download(String iURL, String saveName, String saveDir, Context ctx) { // only: Tool
		DownloadManager downloadManager = (DownloadManager)ctx.getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(iURL));
		request.setDestinationInExternalPublicDir(saveDir, saveName);
		request.setTitle("下载: " + saveName);
		request.setDescription(saveName);
		// request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		// request.setMimeType("application/cn.trinea.download.file");
		downloadManager.enqueue(request);
	}

	public static void startFoxBook(File fmlFile, Context ctx) { // only: Tool
		String fbClassName =  "com.linpinger.foxbook" ;
		Intent foxbook = new Intent();
		foxbook.setComponent(new ComponentName(fbClassName, fbClassName + ".Activity_Main"));
		foxbook.setData( Uri.fromFile(fmlFile) );
		ctx.startActivity(foxbook);
	}

}
