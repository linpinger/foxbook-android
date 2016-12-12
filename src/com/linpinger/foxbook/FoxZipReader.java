package com.linpinger.foxbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FoxZipReader {
	private ZipFile zf ;

	public FoxZipReader(File inZip) {
		try {
			zf = new ZipFile(inZip) ;
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
	
	public ArrayList<Map<String, Object>> getList() {
		ArrayList<Map<String, Object>> oList = new ArrayList<Map<String, Object>>(10240);
		HashMap<String, Object> hm ;

		ZipEntry ze ;
		for ( Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
			ze = e.nextElement();
			hm = new HashMap<String, Object>();
			hm.put("name", ze.getName());
			hm.put("count", ze.getSize());
			oList.add(hm);
		}
		return oList;
	}

	public String getHtmlFile(String filename, String inFileEnCoding) {
		StringBuilder retStr = new StringBuilder(174080);
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(zf.getInputStream(zf.getEntry(filename)), inFileEnCoding));
			char[] chars = new char[4096]; // 这个大小不影响读取速度
            int length = 0;
            while ((length = br.read(chars)) > 0) {
                retStr.append(chars, 0, length);
            }
            br.close();
		} catch (Exception e) {
			System.out.println(e.toString());
        }
		return retStr.toString();
	}
	
	
	public void close() {
		try {
			zf.close();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
}
