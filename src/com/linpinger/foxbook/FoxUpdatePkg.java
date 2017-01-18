package com.linpinger.foxbook;

import android.content.Context;
import android.preference.PreferenceManager;
import java.util.HashMap;
import java.io.File;

import com.linpinger.tool.ToolBookJava;
import com.linpinger.tool.ToolJava;


public class FoxUpdatePkg {
	private Context mContext;
	private String apkPATH = "/sdcard/FoxBook.apk" ;
	private String urlVersion = "http://linpinger.github.io/bin/foxbook-android/version";
	private String urlAPK = "http://linpinger.github.io/bin/foxbook-android/FoxBook.apk";

	public FoxUpdatePkg(Context context) {
		this.mContext = context;

		// 根据设置选择升级线路，默认:github
		String upline = PreferenceManager.getDefaultSharedPreferences(context).getString("upgrade_line", "github");
		if (upline.equalsIgnoreCase("github") ) {
			this.urlVersion = "http://linpinger.github.io/bin/foxbook-android/version" ;
			this.urlAPK = "http://linpinger.github.io/bin/foxbook-android/FoxBook.apk" ;
		}
		if (upline.equalsIgnoreCase("oschina") ) {
			this.urlVersion = "http://linpinger.oschina.io/bin/foxbook-android/version" ;
			this.urlAPK = "http://linpinger.qiniudn.com/prj/FoxBook.apk" ;
		}
	}

	public int FoxCheckUpdate() {
		HashMap<String, Object> remoteVer = getRemoteVersion();
		int newVer = Integer.valueOf((String)remoteVer.get("date")); // 远程日期
		String newURL = (String)remoteVer.get("url");
		String newSHA1 = (String)remoteVer.get("sha1");
		int oldVer = getVersion(mContext); // 本程序日期
		if ( newVer <= oldVer ) { return 0 ; }
		if ( newURL == "" )
			ToolBookJava.saveHTTPFile(urlAPK, apkPATH);
		else
			ToolBookJava.saveHTTPFile(newURL, apkPATH) ;
		String realSHA1 = ToolJava.getFileHash(new File(apkPATH), "SHA1") ;
		if ( newSHA1.compareToIgnoreCase(realSHA1) != 0 ) { return 0 ; } // sha1值不对
		return newVer;
	}

	private HashMap<String, Object> getRemoteVersion() {
		String foxVer = ToolBookJava.downhtml(urlVersion) ;
		// String foxVer = "foxbook-android>20140519>89317>F8639E58AA84A4F7C9E2152E8A6016AAF7EC534D>http://linpinger.qiniudn.com/prj/FoxBook.apk\n" ;
		String xx[] = foxVer.replace("\n", "").replace("\r", "").split(">");
		HashMap<String, Object> item = new HashMap<String, Object>();
		item.put("name", xx[0]);
		item.put("date", xx[1]);
		item.put("size", xx[2]);
		item.put("sha1", xx[3]);
		item.put("url", xx[4]);
		return item;
	}

	private int getVersion(Context context) {
		String versionCode = "";
		try {  // 获取软件版本号，返回类似: 20140101
			versionCode = context.getPackageManager().getPackageInfo("com.linpinger.foxbook", 0).versionName;
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		return Integer.valueOf(versionCode);
	}


}
