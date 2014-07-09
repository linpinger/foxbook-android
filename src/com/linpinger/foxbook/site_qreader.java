package com.linpinger.foxbook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class site_qreader {
	// { 快读
		
	public static String qreader_Search(String iBookName) { // 搜索书籍，返回 http://m.qreader.me/query_catalog.php?bid=1085238
		String iJson = FoxBookLib.downhtml("http://m.qreader.me/search_books.php", "", "{\"key\":\"" + iBookName + "\"}") ;
		int bookID = 0 ;
		try {
			bookID = new JSONObject(iJson).getJSONArray("books").getJSONObject(0).getInt("id");
			if ( ! ( new JSONObject(iJson).getJSONArray("books").getJSONObject(0).getString("name") ).equalsIgnoreCase(iBookName) )
				bookID = 0 ;
		} catch (Exception e) {
			e.toString() ;
		}
		if ( 0 == bookID )
			return "" ;
		else
			return "http://m.qreader.me/query_catalog.php?bid=" + bookID ;
	}
		
	public static String qreader_GetContent(String PgURL) { // 获取内容页 "http://m.qreader.me/query_catalog.php?bid=1119690#222"
		String bID = "" ;
		String cID = "" ;
		Matcher m = Pattern.compile("bid=([0-9]+)#([0-9]+)").matcher(PgURL);
		while(m.find()) {
			bID = m.group(1) ;
			cID = m.group(2) ;
		}
	
		String iJson = FoxBookLib.downhtml("http://chapter.qreader.me/download_chapter.php", "GBK", "{\"id\":" + bID + ",\"cid\":" + cID + "}") ;
		return iJson.replace("　　", "") ;
	}

	public static List<Map<String, Object>> qreader_GetIndex(String IdxURL, int cLast, int FoxMod) { // 获取目录数组 "http://m.qreader.me/query_catalog.php?bid=1119690"
		String bID = "" ;
		Matcher m = Pattern.compile("bid=([0-9]+)").matcher(IdxURL);
		while(m.find())
			bID = m.group(1) ;

		String iJson = FoxBookLib.downhtml("http://m.qreader.me/query_catalog.php", "", "{\"id\":" + bID + "}") ;
		return json2PageList(iJson, bID, cLast, FoxMod) ;
	}


	public static List<Map<String, Object>> json2PageList(String iJson, String bookID, int cLast, int FoxMod) { // 快读: in:json,最后多少页,是更新模式么  out:页面列表
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(200);
		try {
			JSONArray slist = new JSONObject(iJson).getJSONArray("catalog");
			int cList = slist.length();
			int sNum = 0 ;
	
			if ( cLast > 0 && cLast < cList ) { //显示最后几条
				sNum = cList - cLast ;
			}
	
			Map<String, Object> item;
			String sURL = "";
			for(int i=sNum; i<cList; i++) {
				item = new HashMap<String, Object>();
				item.put("name", slist.getJSONObject(i).getString("n"));
				sURL = "#" + slist.getJSONObject(i).getInt("i") ;
				if ( FoxMod == 0 ){ // 在线查看模式
					item.put("url", getUrlPage(bookID, sURL));
				}
				if ( FoxMod == 1 ){ // 更新模式
					item.put("url", sURL);
				}
				data.add(item);
			}
		} catch (Exception e) {
			e.toString();
		}
		return data;
	}

	public static String getUrlPage(String bookid, String pageid) { // 页面地址
		return "http://chapter.qreader.me/download_chapter.php?bid=" + bookid + "&cid=" + pageid ;
	}



	// } 快读
}
