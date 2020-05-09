package com.linpinger.tool;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Properties;

public class ToolAndroid {

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

	public static int jump2ListViewPos(ListView lv, int position) { // positon有效值: -99=上翻一屏，-66=下翻一屏幕，-1=底部，0=头部，>0
		// http://androiddoc.qiniudn.com/reference/android/widget/AdapterView.html#getCount()
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

	@SuppressLint("SdCardPath")
	public static File getDefaultDir(SharedPreferences settings) {
		File defDir = new File(settings.getString("defaultDir", "/sdcard/FoxBook/"));
		if ( defDir.exists() ) {
			if ( defDir.isFile() )
				System.err.println( "默认存储路径，是文件: " + defDir.getPath() );
			if ( defDir.isDirectory())
				System.out.println( "默认存储路径，是目录: " + defDir.getPath() );
			return defDir ;
		} else { // 文件夹不存在
			if ( ! defDir.mkdir() ) { // 建立失败
				System.err.println( "默认存储路径不存在，新建失败，返回: /sdcard/" );
				return new File("/sdcard/");
			}
		}
		System.out.println( "默认存储绝对路径: " + defDir.getAbsolutePath() );
		return defDir ;
	}

	public static boolean myConfigImportExPort(Context ctx, boolean isExport) { // 导入导出阅读页配置，这个调整起来还是有点麻烦的
		SharedPreferences ps = PreferenceManager.getDefaultSharedPreferences(ctx);
		File cfgFile = new File("/sdcard/FoxBook.cfg" );

		if ( isExport ) { // 导出: DefaultSharedPreferences -> Properties
			StringBuffer oStr = new StringBuffer();
			oStr.append("fontsize=").append(ps.getFloat("fontsize", 36.0f)).append("\n")
				.append("paddingMultip=").append(ps.getFloat("paddingMultip", 0.5f)).append("\n")
				.append("lineSpaceingMultip=").append(ps.getFloat("lineSpaceingMultip", 1.5f)).append("\n")
				.append("myBGcolor=").append(ps.getString("myBGcolor", "green")).append("\n")
				.append("\n");
			ToolJava.renameIfExist(cfgFile);
			ToolJava.writeText(oStr.toString(), cfgFile.getPath() );
			return true ;
		} else { // 导入: DefaultSharedPreferences <- Properties
			if ( ! cfgFile.exists() ) {
				System.out.println("错误: FoxBook配置文件不存在，无法导入");
				return false;
			}
			Properties pro = ToolJava.loadConfig( cfgFile.getPath() );

			Editor ed = ps.edit();
			ed.putFloat("fontsize", Float.valueOf(pro.getProperty("fontsize")));
			ed.putFloat("paddingMultip", Float.valueOf(pro.getProperty("paddingMultip")));
			ed.putFloat("lineSpaceingMultip", Float.valueOf(pro.getProperty("lineSpaceingMultip")));
			ed.putString("myBGcolor", pro.getProperty("myBGcolor"));
			return ed.commit();
		}
	}

	public static String getWifiName(Context context) {
		String ssid = "";
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);// 获取WifiManager
		if ( null != wm ) {
			WifiInfo wi = wm.getConnectionInfo();
			int nid = wi.getNetworkId();

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

	public static String getWifiIP(Context context) {
		String wip = "";
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);// 获取WifiManager
		if ( null != wm ) {
			WifiInfo wi = wm.getConnectionInfo();
			wip = intToIp(wi.getIpAddress());
		}
		if ( null == wip ) { wip = "0.0.0.0"; }
		return wip;
	}

//	// 获取wifi的ip,name,其他信息
//	public static HashMap<String, String> getWifiInfo(Context context) { // ip,name,info | null=无WIFI
//		HashMap<String, String> hm = new HashMap<String, String>();
//		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//		NetworkInfo info = cm.getActiveNetworkInfo();
//		if ( info != null && info.isAvailable() ) {
//			WifiManager wifimanage = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);// 获取WifiManager
////			if (!wifimanage.isWifiEnabled())
////				wifimanage.setWifiEnabled(true);
//			WifiInfo wifiinfo = wifimanage.getConnectionInfo();
//			hm.put("ip", intToIp(wifiinfo.getIpAddress()) );
//			String wifiId = wifiinfo != null ? wifiinfo.getSSID() : null;
//			hm.put("name", wifiId );
//			hm.put("info", wifiinfo.toString());
//			return hm;
//		} else {
//			return null ; //"无WIFI" ;
//		}
//	}

//	public static String getLocalIpAddress() {
//		try { // 遍历网络接口
//			Enumeration<NetworkInterface> infos = NetworkInterface.getNetworkInterfaces();
//			while (infos.hasMoreElements()) { // 获取网络接口
//				NetworkInterface niFace = infos.nextElement();
//				Enumeration<InetAddress> enumIpAddr = niFace.getInetAddresses();
//				while (enumIpAddr.hasMoreElements()) {
//					InetAddress mInetAddress = enumIpAddr.nextElement();
//					// 所获取的网络地址不是127.0.0.1时返回得得到的IP
//					if (!mInetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(mInetAddress.getHostAddress()))
//						return mInetAddress.getHostAddress().toString();
//				}
//			}
//		} catch (Exception e) {
//			System.err.println(e.toString());
//		}
//		return "127.0.0.1";
//	}

	// 将获取的int转为真正的ip地址,参考的网上的，修改了下
	private static String intToIp(int i){  
		return ( i & 0xFF) + "." + ((i >> 8 ) & 0xFF) + "." + ((i >> 16 ) & 0xFF) + "." + ((i >> 24 ) & 0xFF ) ; 
	}

	/** 
	 * 将sp值转换为px值，保证文字大小不变 
	 *  
	 * @param spValue 
	 * @param fontScale 
	 *			（DisplayMetrics类中属性scaledDensity） 
	 * @return 
	 */  
	public static int sp2px(Context context, float spValue) {  
		final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
		return (int) (spValue * fontScale + 0.5f);  
	}

	/** 
	 * 将px值转换为sp值，保证文字大小不变 
	 *  
	 * @param pxValue 
	 * @param fontScale 
	 *			（DisplayMetrics类中属性scaledDensity） 
	 * @return 
	 */  
	public static int px2sp(Context context, float pxValue) {  
		final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
		return (int) (pxValue / fontScale + 0.5f);  
	}

	public static void download(String iURL, String saveName, Context ctx) {
		download(iURL, saveName, "99_sync", ctx);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void download(String iURL, String saveName, String saveDir, Context ctx) {
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

//	public static void setcliptext(String content, Context ctx){ // 复制文本到剪贴板
//		ClipboardManager cmb = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE); 
//		cmb.setText(content.trim()); 
//	} 
//
//	public static String getcliptext(Context ctx) { // 从剪贴板获得文本
//		ClipboardManager cmb = (ClipboardManager)ctx.getSystemService(Context.CLIPBOARD_SERVICE); 
//		return cmb.getText().toString().trim(); 
//	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void setClipText(String iText, Context ctx) {
		((ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("hello", iText));
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static String getClipText(Context ctx) {
		// return ((ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip().getItemAt(0).getText().toString();
		String retS = "";
		CharSequence xx = ((ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip().getItemAt(0).getText();
		if ( xx != null ) {
			retS = xx.toString();
		} else {
			System.err.println("- 剪贴板获取了空对象: " + ctx.getClass().getName() );
		}
		return retS;
	}

}
