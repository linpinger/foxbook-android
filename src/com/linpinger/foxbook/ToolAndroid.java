package com.linpinger.foxbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;

public class ToolAndroid {

	public static boolean myConfigImportExPort(Context ctx, boolean isExport) { // 导入导出阅读页配置，这个调整起来还是有点麻烦的
		File cfgFile = new File( Environment.getExternalStorageDirectory().getPath() + "/FoxBook.cfg" );
		SharedPreferences ps = PreferenceManager.getDefaultSharedPreferences(ctx);

		if ( isExport ) { // 导出: DefaultSharedPreferences -> Properties
			StringBuffer oStr = new StringBuffer();
			oStr.append("fontsize=").append(ps.getFloat("fontsize", 36.0f)).append("\n")
				.append("paddingMultip=").append(ps.getFloat("paddingMultip", 0.5f)).append("\n")
				.append("lineSpaceingMultip=").append(ps.getFloat("lineSpaceingMultip", 1.5f)).append("\n")
				.append("myBGcolor=").append(ps.getString("myBGcolor", "green")).append("\n")
				.append("\n");
			if ( cfgFile.exists() )
				cfgFile.renameTo(new File(cfgFile.getPath() + ".old"));
			ToolJava.writeText(oStr.toString(), cfgFile.getPath() );
			return true ;
		} else { // 导入: DefaultSharedPreferences <- Properties
			if ( ! cfgFile.exists() ) {
				System.out.println("错误: FoxBook配置文件不存在，无法导入");
				return false;
			}
			Properties pro = new Properties();
			try {
				FileInputStream inputFile = new FileInputStream(cfgFile);
				pro.load(inputFile);
				inputFile.close();
			} catch ( Exception e ) {
				System.out.println(e.toString());
			}
			Editor ed = ps.edit();
			ed.putFloat("fontsize", Float.valueOf(pro.getProperty("fontsize")));
			ed.putFloat("paddingMultip", Float.valueOf(pro.getProperty("paddingMultip")));
			ed.putFloat("lineSpaceingMultip", Float.valueOf(pro.getProperty("lineSpaceingMultip")));
			ed.putString("myBGcolor", pro.getProperty("myBGcolor"));
			return ed.commit();
		}
	}

    /** 
     * 将sp值转换为px值，保证文字大小不变 
     *  
     * @param spValue 
     * @param fontScale 
     *            （DisplayMetrics类中属性scaledDensity） 
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
     *            （DisplayMetrics类中属性scaledDensity） 
     * @return 
     */  
    public static int px2sp(Context context, float pxValue) {  
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
        return (int) (pxValue / fontScale + 0.5f);  
    }

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void download(String iURL, String saveName, Context ctx) {
		DownloadManager downloadManager = (DownloadManager)ctx.getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(iURL));
		request.setDestinationInExternalPublicDir("99_sync", saveName);
		request.setTitle("下载: " + saveName);
		request.setDescription(saveName);
		// request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		// request.setMimeType("application/cn.trinea.download.file");
		downloadManager.enqueue(request);
	}

	public static void setcliptext(String content, Context ctx){ // 复制文本到剪贴板
		ClipboardManager cmb = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE); 
		cmb.setText(content.trim()); 
	} 

	public static String getcliptext(Context ctx) { // 从剪贴板获得文本
		ClipboardManager cmb = (ClipboardManager)ctx.getSystemService(Context.CLIPBOARD_SERVICE); 
		return cmb.getText().toString().trim(); 
	}

/*
import android.annotation.TargetApi;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.os.Build;
	// 新版Clip
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void copyToClipboard(String iText, Context ctx) {
		((ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("hello", iText));
	}
*/

}
