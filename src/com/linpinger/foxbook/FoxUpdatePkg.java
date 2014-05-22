package com.linpinger.foxbook;

import android.content.Context;
import java.util.HashMap;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.math.BigInteger;


public class FoxUpdatePkg {
	private Context mContext;
	private String apkPATH = "/sdcard/FoxBook.apk" ;
	private String urlVersion = "http://linpinger.github.io/bin/foxbook-android/version";
	private String urlAPK = "http://linpinger.github.io/bin/foxbook-android/FoxBook.apk";
//	private String urlVersion = "http://linpinger.qiniudn.com/version";
//	private String urlAPK = "http://linpinger.qiniudn.com/FoxBook.apk";
	
	public FoxUpdatePkg(Context context) {
		this.mContext = context;
	}

	public int FoxCheckUpdate() {
		HashMap<String, Object> remoteVer = getRemoteVersion();
		int newVer = Integer.valueOf((String)remoteVer.get("date")); // 远程日期
		String newURL = (String)remoteVer.get("url");
		String newSHA1 = (String)remoteVer.get("sha1");
		int oldVer = getVersion(mContext); // 本程序日期
		if ( newVer <= oldVer ) { return 0 ; }
		if ( newURL == "" ) {
			FoxBookLib.saveHTTPFile(urlAPK, apkPATH);
		} else {
			FoxBookLib.saveHTTPFile(newURL, apkPATH) ;
		}
		String realSHA1 = getFileMD5(new File(apkPATH), "SHA1") ;
		if ( newSHA1.compareToIgnoreCase(realSHA1) != 0 ) { return 0 ; } // sha1值不对
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

	    /**
     * 
     * @param file 
     * @param algorithm 所请求算法的名称  for example: MD5, SHA1, SHA-256, SHA-384, SHA-512 etc. 
     * @return
     */
    public static String getFileMD5(File file,String algorithm) {
        if (!file.isFile()) {
            return null;
        }

        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;

        try {
            digest = MessageDigest.getInstance(algorithm);
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
//            e.printStackTrace();
            return "";
        }

        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }
	
}
