package com.linpinger.foxbook;

import java.util.HashMap;

import android.content.Context;


public class FoxUpdatePkg {
	private Context mContext;
	private String urlVersion = "http://linpinger.qiniudn.com/version";
	private String urlAPK = "http://linpinger.qiniudn.com/FoxBook.apk";
	
	public FoxUpdatePkg(Context context) {
		this.mContext = context;
	}

	public int FoxCheckUpdate() {
		HashMap<String, Object> remoteVer = getRemoteVersion();
		int newVer = Integer.valueOf((String)remoteVer.get("date")); // 远程日期
		String newURL = (String)remoteVer.get("url");
		int oldVer = getVersion(mContext); // 本程序日期
		if ( newVer <= oldVer ) { return 0 ; }
		if ( newURL == "" ) {
			FoxBookLib.saveHTTPFile(urlAPK, "/sdcard/FoxBook.apk");
		} else {
			FoxBookLib.saveHTTPFile(newURL, "/sdcard/FoxBook.apk") ;
		}
		return newVer;
	}

	private HashMap<String, Object> getRemoteVersion() {
		String foxVer = FoxBookLib.downhtml(urlVersion) ;
		// String foxVer = "foxbook-android>20140519>89317>F8639E58AA84A4F7C9E2152E8A6016AAF7EC534D>http://foxbook.qiniudn.com/FoxBook.apk\n" ;
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
			e.toString();
		}
	    return Integer.valueOf(versionCode);
	}
	
}
