package com.linpinger.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FoxEpubReader extends FoxZipReader {

	public FoxEpubReader(File inEpub) {
		super(inEpub);
	}

//	public String getOPFName() { // 木有用到
//		String xml = getTextFile("META-INF/container.xml");
//		Matcher mat = Pattern.compile("(?smi)<rootfile .*?full-path=\"([^\" ]*?)\"").matcher(xml);
//		while (mat.find())
//			return mat.group(1);
//		return "";
//	}

	public HashMap<String, Object> getQiDianEpubInfo() {
		HashMap<String, Object> hm = new HashMap<String, Object>() ;
		String html = getTextFile("title.xhtml");
		Matcher mat = Pattern.compile("(?smi)<li><b>书名</b>：<a href=\"http://([0-9]*).qidian.com[^>]*?>([^<]*?)</a>.*<li><b>作者</b>：<a[^>]*?>([^<]*?)</a>.*<li><b>主题</b>：([^<]*?)<.*<li><b>简介</b>：<pre>(.*)</pre>").matcher(html);
		while (mat.find()) {
			hm.put("qidianid", mat.group(1));
			hm.put("bookname", mat.group(2));
			hm.put("author", mat.group(3));
			hm.put("type", mat.group(4));
			hm.put("info", mat.group(5));
		}
		return hm;
	}

	public ArrayList<HashMap<String, Object>> getQiDianEpubTOC() {
		ArrayList<HashMap<String, Object>> xx = new ArrayList<HashMap<String, Object>>(80);
		String html = getTextFile("catalog.html");
		// <a href="content1004074281_325666373.html">楔子</a>
		Matcher mat = Pattern.compile("(?smi)<a href=\"(content([0-9]*)_([0-9]*).html)\">([^<]*)</a>").matcher(html);
		HashMap<String, Object> hm ;
		while (mat.find()) {
			hm = new HashMap<String, Object>();
			hm.put("title", mat.group(4));
			hm.put("name", mat.group(1));
			hm.put("bookid", mat.group(2));
			hm.put("pageid", mat.group(3));
			xx.add(hm);
		}
		return xx ;
	}

	public String getQiDianEpubPage(String itemName){
		String txt = "";
		String html = getTextFile(itemName);
		Matcher mat = Pattern.compile("(?smi)<div class=\"content\">[\r\n]*(.*?)</div>").matcher(html);
		while (mat.find())
			txt = mat.group(1);
		txt = txt.replace("<p>手机用户请到m.qidian.com阅读。</p>", "")
				.replace("<p>手机阅读器、看书更方便。【<a href=\"http://download.qidian.com/apk/QDReader.apk?k=e\" target=\"_blank\">安卓版</a>】</p>", "");
		txt = txt.replace("\r", "")
				.replace("\n　　", "\n")
				.replace("<br />　　", "\n")
				.replace("<p>", "")
				.replace("</p>", "\n")
				.replace("\n\n", "\n");
		return txt;
	}

//	public static void main(String[] args) {
//		long sTime = System.currentTimeMillis();
//		FoxEpubReader epub = new FoxEpubReader(new File("S:\\1003656831.epub"));
//
//		HashMap<String, Object> hm = epub.getQiDianEpubInfo();
//		System.out.println("qidianid:" + hm.get("qidianid") + "\nbookname:" + hm.get("bookname"));
//
//		for (HashMap<String, Object> hmi : epub.getQiDianEpubTOC())
//			System.out.println(hmi.get("name") + ":" + hmi.get("title") );
//
//		System.out.println(epub.getQiDianEpubPage("content1003656831_317820302.html"));
//
//		epub.close();
//		System.out.println("Time: " + (System.currentTimeMillis() - sTime));
//	}
}
