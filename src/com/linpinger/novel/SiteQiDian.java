package com.linpinger.novel;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class SiteQiDian extends NovelSite {

	public String getQiDianBookIDFromURL(String iURL) {
	    String sQDID = "";
	    String RE = "" ;
	    if ( iURL.contains(".if.qidian.com") ) {
	        RE = "(?i)BookId=([0-9]+)&" ;
	    } else if (iURL.contains("m.qidian.com/book/")) { //2017-1-11: m.qidian.com/book/1004179514[/339781806]
	    	RE = "(?i)/book/([0-9]+)" ;
	    } else {
	        RE = "(?i).*/([0-9]+)\\." ; // http://read.qidian.com/BookReader/3059077.aspx
	    }
	    Matcher m = Pattern.compile(RE).matcher(iURL);
	    while (m.find())
	        sQDID = m.group(1);
	    return sQDID;
	}

	// 移动版目录地址: 可以用来获取lastPageID后的更新，为0获取所有
    public String getIndexURL_Mobile(String bookID) {
    	return "http://druid.if.qidian.com/Atom.axd/Api/Book/GetChapterList?BookId=" + bookID + "&timeStamp=0&requestSource=0&md5Signature=" ;
    }

    public String getIndexURL_Desk(String bookID) {
    	return "http://book.qidian.com/info/" + bookID ;
    } // Page: http://read.qidian.com/chapter/k8Ysqe1aqVAVDwQbBL_r1g2/LwKggaUrMLi2uJcMpdsVgA2

    public String getSearchURL_Mobile(String bookName) {
    	// 自动完成: http://druid.if.qidian.com/druid/Api/Search/AutoCompleteWithBookList?key=%E6%9C%AA%E6%9D%A5%E5%A4%A9%E7%8E%8B
    	// http://druid.if.qidian.com/druid/Api/Search/GetBookStoreWithBookList?type=-1&key=%E5%94%90%E6%9C%9D%E5%A5%BD%E5%9C%B0%E4%B8%BB
        String xx = "";
        try {
            xx = "http://druid.if.qidian.com/druid/Api/Search/GetBookStoreWithBookList?type=-1&key=" + URLEncoder.encode(bookName, "UTF-8") ;
        } catch (Exception e) {
			System.err.println(e.toString());
        }
        return xx;
    }

    public List<Map<String, Object>> getJsonBookList(String json) { // book:name,url
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(55);
        try {
            JSONArray slist = new JSONObject(json).getJSONArray("Data");
            int cList = slist.length();
            Map<String, Object> item;
            for (int i = 0; i < cList; i++) {
                item = new HashMap<String, Object>();
                item.put(NV.BookName, slist.getJSONObject(i).getString("BookName"));
                item.put(NV.BookURL, getIndexURL_Mobile(String.valueOf(slist.getJSONObject(i).getInt("BookId"))));
                data.add(item);
            }
        } catch (Exception e) {
			System.err.println(e.toString());
        }
        return data;
    }

    public List<Map<String, Object>> getJsonTOC(String json) { // name,url
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(128);
        try {
            String bookID = String.valueOf(new JSONObject(json).getJSONObject("Data").getInt("BookId"));
            JSONArray slist = new JSONObject(json).getJSONObject("Data").getJSONArray("Chapters");
            int cList = slist.length();
            Map<String, Object> item;
            
            for (int i = 0; i < cList; i++) {
                item = new HashMap<String, Object>(2);
                item.put(NV.PageName, slist.getJSONObject(i).getString("n"));
                item.put(NV.PageURL, getContentURLFromIDs(String.valueOf(slist.getJSONObject(i).getInt("c")), bookID));
                if (1 == slist.getJSONObject(i).getInt("v")) // VIP章节
                    break;
                data.add(item);
            }
        } catch (Exception e) {
			System.err.println(e.toString());
        }
        return data;
    }
    
    public String getJsonContent(String json) { // 2017-1-11:  http://druid.if.qidian.com/Atom.axd/Api/Book/GetContent?BookId=1004936518&ChapterId=344096395
    	String Content = "";
    	try {
    		Content = new JSONObject(json).getString("Data");
    		Content = Content.replace("\\r\\n", "\n");
    		Content = Content.replace("\r\n", "\n");
    		Content = Content.replace("　　", "");
		} catch (Exception e) {
			System.err.println(e.toString());
		}
    	return Content;
    }

    public String getContentURLFromIDs(String pageID, String bookID) {
    	// 2017-1-11: 旧版的暂时可用，就不修改了
    	// return "http://druid.if.qidian.com/Atom.axd/Api/Book/GetContent?BookId=" + bookID + "&ChapterId=" + pageID ;
    	return "http://files.qidian.com/Author" + ( 1 + ( Integer.valueOf(bookID) % 8 ) ) + "/" + bookID + "/" + pageID + ".txt";
    }

    /**
    * 日期: 2015-11-17  , 2016-12-18: 改版后疑似过时
    * @param html 类似 /b7zJ1_AnAJ41,Nw1qx8_dKSIex0RJOkJclQ2.aspx 的网页内容
    * @return http://files.qidian.com/Author7/1939238/53927617.txt
     */
    public String getContentURLFromDeskHtml(String html) {
        Matcher mat = Pattern.compile("(?i)(http://files.qidian.com/.*/[0-9]*/[0-9]*.txt)").matcher(html);
        String txtURL = "";
        while (mat.find())
            txtURL = mat.group(1);
        return txtURL;
    }

    /**
    *
    * @param jsStr http://files.qidian.com/Author7/1939238/53927617.txt 中的内容
    * @return 文本，可直接使用
    */
    public String getContentFromJS(String jsStr) {
        jsStr = jsStr.replace("&lt;", "<");
        jsStr = jsStr.replace("&gt;", ">");
        jsStr = jsStr.replace("document.write('", "");
        jsStr = jsStr.replace("<a>手机用户请到m.qidian.com阅读。</a>", "");
        jsStr = jsStr.replace("<a href=http://www.qidian.com>起点中文网www.qidian.com欢迎广大书友光临阅读，最新、最快、最火的连载作品尽在起点原创！</a>", "");
        jsStr = jsStr.replace("<a href=http://www.qidian.com>起点中文网 www.qidian.com 欢迎广大书友光临阅读，最新、最快、最火的连载作品尽在起点原创！</a>", "");
        jsStr = jsStr.replace("<ahref=http://www.qidian.com>起点中文网www.qidian.com欢迎广大书友光临阅读，最新、最快、最火的连载作品尽在起点原创！</a>", "");
        jsStr = jsStr.replace("');", "");
        jsStr = jsStr.replace("<p>", "\n");
        jsStr = jsStr.replace("　　", "");
        return jsStr;
    }

}

