package com.linpinger.foxbook;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class site_easou {
	public static String getUrlSE(String bookname) { //宜搜: in:书名  out: 搜索地址
		try {
			return "http://api.easou.com/api/bookapp/search.m?word=" + URLEncoder.encode(bookname, "UTF-8") + "&type=0&page_id=1&count=20&sort_type=0&cid=eef_easou_book" ;
		} catch (UnsupportedEncodingException e) {
			e.toString();
		}
		return "";
	}
	
	public static String getUrlSL(String ids) { //宜搜: in:书名  out: 网站列表地址
		return "http://api.easou.com/api/bookapp/search_chapterrel.m?" + ids + "&cid=eef_easou_book";
	}

	public static String getUrlToc(String ids) { //宜搜: in:ids  out: 搜索地址
		return "http://api.easou.com/api/bookapp/chapter_list.m?" + ids + "&page_id=1&size=2147483647&cid=eef_easou_book";
	}
	
	public static String getUrlPage(String ids) { //宜搜: in:ids(gid+nid+sort+chaname)   out: 该书页面地址
		return "http://api.easou.com/api/bookapp/batch_chapter.m?a=1&cid=eef_easou_book&gsort=0&sequence=0&" + ids ;
	}
	
	public static String json2IDs(String json, int Type) { //宜搜: in:json  out: gid=nnn&nid=nnn
		String ids ="";
		try {
			JSONObject oJson = new JSONObject(json).getJSONArray("items").getJSONObject(0);
			if ( Type == 0 ) { // 书籍
				ids = "gid=" + oJson.getInt("gid") + "&nid=" +oJson.getInt("nid");
			}
			if ( Type == 1 ) { // 换源
				String lastChaName = oJson.getString("lastChapterName");
				ids = "gid=" + oJson.getInt("gid") + "&nid=" +oJson.getInt("nid") + "&chapter_name=" + URLEncoder.encode(lastChaName, "UTF-8");
			}
		} catch (Exception e) {
			e.toString();
		}
		return ids;
	}
	
	public static List<Map<String, Object>> json2SiteList(String json) { // 宜搜: in:json  out:data
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(55);
		try {
			JSONArray slist = new JSONObject(json).getJSONArray("items");
			int cList = slist.length();
			Map<String, Object> item;
			String ids = "";
			for(int i=0; i<cList; i++) {
				item = new HashMap<String, Object>();
				ids = "gid=" + slist.getJSONObject(i).getInt("gid") + "&nid=" + slist.getJSONObject(i).getInt("nid") ;
				item.put("url", getUrlToc(ids));
				item.put("name", slist.getJSONObject(i).getString("last_chapter_name"));
				data.add(item);
			}
		} catch (Exception e) {
			e.toString();
		}
		return data;
	}
	
	public static List<Map<String, Object>> json2PageList(String json, String sGIDNID, int cLast) { // 宜搜: in:json,gidnid,最后多少页  out:页面列表
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(55);
		try {
			JSONArray slist = new JSONObject(json).getJSONArray("items");
			
			int cList = slist.length();
			int sNum = 0 ;
			if ( cLast < cList ) { //显示最后几条
				sNum = cList - cLast ;
			}
			Map<String, Object> item;
			String chaName = "";
			String hchaName = "";
			int nowSort = 0 ;
			for(int i=sNum; i<cList; i++) {
				item = new HashMap<String, Object>();
				chaName = slist.getJSONObject(i).getString("chapter_name");
				hchaName = URLEncoder.encode(chaName, "UTF-8");
				nowSort = slist.getJSONObject(i).getInt("sort") ;
				item.put("name", chaName);
				item.put("url", getUrlPage(sGIDNID + "&sort=" + nowSort + "&chapter_name=" + hchaName)) ;
				data.add(item);
			}
		} catch (Exception e) {
			e.toString();
		}
		return data;
	}


	
	public static String json2Text(String json){ //宜搜: in:json  out: 正文
		try {
			json = new JSONObject(json).getJSONArray("items").getJSONObject(0).getString("content");
		} catch (JSONException e) {
			e.toString();
		}
		json = json.replace("\\n", "\n");
		json = json.replace("　　", "");
		return json;
	}

}
