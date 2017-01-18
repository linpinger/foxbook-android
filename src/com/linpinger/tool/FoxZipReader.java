package com.linpinger.tool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FoxZipReader {
	protected ZipFile zf ;

	public static String getUtf8TextFromZip(File zFile, String itemFileName) {
		FoxZipReader z = new FoxZipReader(zFile);
		String html = z.getTextFile(itemFileName);
		z.close();
		return html;
	}

	public FoxZipReader(File inZip) {
		try {
			zf = new ZipFile(inZip) ;
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	public ArrayList<Map<String, Object>> getFileList() {
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

	public String getTextFile(String utf8file) {
		return getTextFile(utf8file, "UTF-8");
	}

	public String getTextFile(String filename, String iTextFileEncoding) {
		StringBuilder retStr = new StringBuilder(174080);
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(zf.getInputStream(zf.getEntry(filename)), iTextFileEncoding));
			char[] chars = new char[4096]; // 这个大小不影响读取速度
			int length = 0;
			while ((length = br.read(chars)) > 0) {
				retStr.append(chars, 0, length);
			}
			br.close();
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		return retStr.toString();
	}

	public void getBinFile(String filename, File outFile) {
		try {
			BufferedInputStream bis = new BufferedInputStream(zf.getInputStream(zf.getEntry(filename)));
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile));
			byte[] buf = new byte[1048576];
			int nRead ;
			while ( ( nRead = bis.read(buf) ) != -1 )
				bos.write(buf, 0, nRead);
			bos.close();
			bis.close();
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	public void close() {
		try {
			zf.close();
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}
}
