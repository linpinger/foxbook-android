package com.linpinger.foxbook;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.Enumeration;
import java.util.HashMap;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import org.apache.http.conn.util.InetAddressUtils;


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
		if ( newURL == "" ) {
			ToolBookJava.saveHTTPFile(urlAPK, apkPATH);
		} else {
			ToolBookJava.saveHTTPFile(newURL, apkPATH) ;
		}
		String realSHA1 = getFileMD5(new File(apkPATH), "SHA1") ;
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
	public static String getFileMD5(File file, String algorithm) {
		if (!file.exists() || !file.isFile()) {
			return "";
		}

		byte[] buffer = new byte[2048];
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			FileInputStream in = new FileInputStream(file);
			while (true) {
				int len = in.read(buffer, 0, 2048);
				if (len != -1) {
					digest.update(buffer, 0, len);
				} else {
					break;
				}
			}
			in.close();

			byte[] md5Bytes = digest.digest();
			StringBuilder hexValue = new StringBuilder();
			for (int i = 0; i < md5Bytes.length; i++) {
				int val = ((int) md5Bytes[i]) & 0xff;
				if (val < 16) {
					hexValue.append("0");
				}
				hexValue.append(Integer.toHexString(val));
			}
			return hexValue.toString();
		} catch (Exception e) {
			// e.printStackTrace();
			return "";
		}
	}

    
    // foxhttpd需要
	public static String getLocalIpAddress() {  
        try {  
            // 遍历网络接口  
            Enumeration<NetworkInterface> infos = NetworkInterface  
                    .getNetworkInterfaces();  
            while (infos.hasMoreElements()) {  
                // 获取网络接口  
                NetworkInterface niFace = infos.nextElement();  
                Enumeration<InetAddress> enumIpAddr = niFace.getInetAddresses();  
                while (enumIpAddr.hasMoreElements()) {  
                    InetAddress mInetAddress = enumIpAddr.nextElement();  
                    // 所获取的网络地址不是127.0.0.1时返回得得到的IP  
                    if (!mInetAddress.isLoopbackAddress()  
                            && InetAddressUtils.isIPv4Address(mInetAddress  
                                    .getHostAddress())) {  
                        return mInetAddress.getHostAddress().toString();  
                    }  
                }  
            }  
        } catch (SocketException e) {  
        	e.toString();
        }  
        return "127.0.0.1";  
    } 
}
