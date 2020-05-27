package com.linpinger.novel;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.linpinger.tool.FoxHTTP;
import com.linpinger.tool.ToolJava;

public class NovelSite {
	public static final int SiteNobody = 0;
	public static final int Site13xxs = 14;
	public static final int SiteXQqxs = 15;
	public static final int SiteXxBiquge = 24;
	public static final int SiteDajiadu = 41;
	public static final int SiteWutuxs = 42;
	public static final int SiteMeegoq = 43;
	public static final int SiteYmxxs = 44;

	public HashMap<String, Object> getSiteCFG(String bookURL) {
		int siteType = NovelSite.SiteNobody;
		String urlShelf = "";
		String reShelf = "";
		String cookie = "";

		if ( bookURL.contains(".meegoq.com") ) {
			siteType = NovelSite.SiteMeegoq;
			urlShelf = "https://www.meegoq.com/u/";
			reShelf = "(?smi)<li>.*?href=\"([^\"]*/info[0-9]*.html)\"[^>]*>([^<]*)<.*?href=\"([^\"]*/[0-9]*_[0-9]*.html)\"[^>]*>([^<]*)<";
			cookie = "meegoq";
			// 正则分析网页: 书地址, 书名, 新章节地址, 新章节名
		} else if ( bookURL.contains(".ymxxs.com") ) { // 类似meegoq
			siteType = NovelSite.SiteYmxxs;
			urlShelf = "https://www.ymxxs.com/u/";
			reShelf = "(?smi)<li>.*?href=\"([^\"]*/text_[0-9]*.html)\"[^>]*>([^<]*)<.*?\"c\"><a href=\"([^\"]*.html)\"[^>]*>([^<]*)<";
			cookie = "ymxxs";
		} else if ( bookURL.contains(".wutuxs.com") ) {
			siteType = NovelSite.SiteWutuxs;
			urlShelf = "https://www.wutuxs.com/modules/article/bookcase.php";
			reShelf = "(?smi)<tr>.*?(aid=[^\"]*)\"[^>]*>([^<]*)<.*?<td class=\"odd\"><a href=\"[^\"]*cid=([0-9]*)\"[^>]*>([^<]*)<";
			cookie = "wutuxs";
		} else if ( bookURL.contains(".xsbiquge.com") ) {
			siteType = NovelSite.SiteXxBiquge;
			urlShelf = "https://www.xsbiquge.com/bookcase.php" ;
			reShelf = "(?smi)\"s2\"><a href=\"([^\"]+)\"[^>]*>([^<]*)<.*?\"s4\"><a href=\"([^\"]+)\"[^>]*>([^<]*)<";
			cookie = "xsbiquge";
		} else if ( bookURL.contains(".dajiadu8.com") ) {
			siteType = NovelSite.SiteDajiadu;
			urlShelf = "https://www.dajiadu8.com/modules/article/bookcase.php";
			reShelf = "(?smi)<tr>.*?(aid=[^\"]*)&index.*?\"[^>]*>([^<]*)<.*?<td class=\"odd\"><a href=\"[^\"]*cid=([0-9]*)\"[^>]*>([^<]*)<";
			cookie = "dajiadu8";
		} else if ( bookURL.contains(".13xxs.com") ) {
			siteType = NovelSite.Site13xxs;
			urlShelf = "http://www.13xxs.com/modules/article/bookcase.php?classid=0";
			reShelf = "(?smi)<tr>.*?href=\"([^\"]*)\"[^>]*>([^<]*)<.*?href=\"[^\"]*/([0-9]*.html)\"[^>]*>([^<]*)<";
			cookie = "13xxs";
		} else if ( bookURL.contains(".xqqxs.com") ) {
			siteType = NovelSite.SiteXQqxs;
			urlShelf = "https://www.xqqxs.com/modules/article/bookcase.php?delid=604" ;
			reShelf = "(?smi)<tr>.*?&indexflag=(.*?)\"[^>]*>([^<]*)<.*?[^>]<a href=\"[^\"]*cid=([0-9]*)\"[^>]*>([^<]*)<";
			cookie = "xqqxs";
		}

		HashMap<String, Object> ts = new HashMap<String, Object>(9);
		ts.put("SiteType", siteType);
		ts.put("ShelfURL", urlShelf);
		ts.put("ShelfRE", reShelf);
		ts.put("CookieLabel", cookie);
		return ts;
	}

	public HashMap<String, String> getSiteShelf(String bookURL, String cookiePath) { // 返回map: 书名 -> 新章节地址|新章节名
		if ( ! new File(cookiePath).exists() ) { return null; } // cookie文件不存在就滚

		// 根据站点URL获取配置
		HashMap<String, Object> sc = getSiteCFG(bookURL);
		int siteType    = Integer.parseInt(sc.get("SiteType").toString());
		if ( siteType == NovelSite.SiteNobody ) { return null; }
		String urlShelf = sc.get("ShelfURL").toString();
		String reShelf  = sc.get("ShelfRE").toString();
		String xml = ToolJava.readText(cookiePath, "UTF-8");
		String cookie   = Stor.getValue(xml, sc.get("CookieLabel").toString()).replace("\r", "").replace("\n", "");
		if ( cookie.length() < 9 ){ return null ; }

		// 根据配置，下载书架html
		String html = "";
		FoxHTTP cHTTP = new FoxHTTP(urlShelf).setHead("Cookie", cookie);
		if (siteType == NovelSite.SiteXxBiquge) {
			html = cHTTP.getHTML("UTF-8");

			cHTTP = new FoxHTTP(urlShelf + "?page=2").setHead("Cookie", cookie);
			html += cHTTP.getHTML("UTF-8");

			cHTTP = new FoxHTTP(urlShelf + "?page=3").setHead("Cookie", cookie);
			html += cHTTP.getHTML("UTF-8");
		} else if (siteType == NovelSite.SiteMeegoq) {
			html = cHTTP.getHTML("UTF-8");
		} else if (siteType == NovelSite.SiteYmxxs) {
			html = cHTTP.getHTML("UTF-8");
		} else {
			html = cHTTP.getHTML("gbk");
		}
//ToolJava.writeText("# 书架: Len=(" + String.valueOf(html.length()) + ") " + urlShelf + "\n", "/sdcard/99_sync/FoxDebug.log", "utf-8", true);
		if ( html.length() < 5 ) { return null ; }

		// 生成map: 书名 -> 新章节地址|新章节名
		HashMap<String, String> shelfBook = new HashMap<String, String>(28); // 书名 -> 新章节地址
		Matcher mat = Pattern.compile(reShelf).matcher(html);
		while (mat.find()) {
			switch (siteType) {
				case NovelSite.SiteWutuxs:
				case NovelSite.SiteXQqxs:
				case NovelSite.SiteDajiadu:
					shelfBook.put(mat.group(2), mat.group(3) + ".html|" + mat.group(4));
					break;
				case NovelSite.SiteXxBiquge:
				case NovelSite.Site13xxs:
				case NovelSite.SiteMeegoq:
				case NovelSite.SiteYmxxs:
					shelfBook.put(mat.group(2), mat.group(3) + "|" + mat.group(4));
					break;
			}
//ToolJava.writeText("- " + mat.group(2) + " : " + shelfBook.get(mat.group(2)).toString() + "\n", "/sdcard/99_sync/FoxDebug.log", "utf-8", true);
		}

		return shelfBook;
	}

	// 获取有新章的书列表,　返回的数组元素: bookid, bookname, bookurl
	public List<Map<String, Object>> compareShelfToGetNew(List<Map<String, Object>> booksInfo, String cookiePath) {
		// 保存有cookie的文件名: FoxBook.cookie，格式: xml:  <cookies><13xs>cookieStr</13xs><biquge>cookieStr</biquge></cookies>
		// 下载书架网页, 需要 书架地址，cookie
		// 正则分析网页，合成 得到 书地址, 书名, 新章节地址, 新章节名
		// 得到 bookid, bookname, bookurl, pageUrlList
		// 比较书籍的 新章节地址是否在 pageUrlList 中，否就加入返回列表中

//		List<Map<String, Object>> booksInfo = nm.getBookListForShelf(); // 获取需要的信息

		String bookURL = (String) booksInfo.get(0).get(NV.BookURL); // 第一本的BookURL标识为站点

		HashMap<String, String> shelfBook = getSiteShelf(bookURL, cookiePath); // 返回map: 书名 -> 新章节地址|新章节名
		if ( null == shelfBook ) { return null; }

		List<Map<String, Object>> newPages = new ArrayList<Map<String, Object>>(10);
		String nowBookName, nowPageList;
		for(Map<String, Object> mm : booksInfo) {
			nowBookName = mm.get(NV.BookName).toString();
			nowPageList = mm.get(NV.DelURL).toString(); // 这里的DelURL内容: 已删除+未读列表

			if ( shelfBook.containsKey(nowBookName) ) { // 先检查key是否存在，不然会崩
				if ( ! nowPageList.contains( shelfBook.get(nowBookName).split("\\|")[0] + "|") ) {
//ToolJava.writeText("- 新: " + nowBookName + " : " + shelfBook.get(nowBookName)  + "\n", "/sdcard/99_sync/FoxDebug.log", "utf-8", true);
					newPages.add(mm);
				}
			}

		}
		return newPages;
	}

	public String searchBook(String iBookName, String siteType) { // meegoq, ymxxs, wutuxs, dajiadu, 13xxs, xqqxs
		String oURL = "";

		String html = "";
		boolean bMulti = false;
		String reStr = "";
		try {
			if ( siteType.equalsIgnoreCase("meegoq") ) {
				html = new FoxHTTP( "https://www.meegoq.com/search.htm?keyword=" + URLEncoder.encode(iBookName, "UTF-8") ).getHTML();
				reStr = "(?smi)\"n2\"><a href=\"([^\"]+?)\">([^<]+?)<"; // url, name
			} else if ( siteType.equalsIgnoreCase("ymxxs") ) {
				html = new FoxHTTP( "https://www.ymxxs.com/search.htm?keyword=" + URLEncoder.encode(iBookName, "UTF-8") ).getHTML();
				reStr = "(?smi)\"n2\"><a[^>]+?>([^<]+?)<.*?\"c2\"><a href=\"([^\"]+?/)[0-9]+?.html\"[^>]*?>"; // name, url
			} else if ( siteType.equalsIgnoreCase("wutuxs") ) {
				html = new FoxHTTP("http://www.wutuxs.com/modules/article/search.php").getHTML("GBK", "searchtype=articlename&searchkey=" + URLEncoder.encode(iBookName, "GBK"));
				if ( html.contains(">全文阅读<") ) {
					reStr = "(?smi)href=\"([^\"]+?)\" title=\"(.*?)全文阅读\">全文阅读<"; // url, name
				} else {
					bMulti = true;
					reStr = "(?smi)\"odd\"><a href=\"([^\"]+?)\">([^<]+?)</a>"; // url, name
				}
			} else if ( siteType.equalsIgnoreCase("dajiadu") ) {
				html = new FoxHTTP("https://www.dajiadu8.com/modules/article/searchab.php").getHTML("GBK", "searchtype=articlename&searchkey=" + URLEncoder.encode(iBookName, "GBK") + "&Submit=+%CB%D1+%CB%F7+");
				if ( html.contains(">点击阅读<") ) {
					reStr = "(?smi)<h1>([^<]+?)</h1>.*?href=\"([^\"]+?)\">点击阅读<"; // name, url
				} else {
					bMulti = true;
					reStr = "(?smi)\"odd\"><a[^>]+?>([^<]+?)<.*?\"even\"><a href=\"([^\"]+?)\"[^>]+?>"; // name, url
				}
			} else if ( siteType.equalsIgnoreCase("13xxs") ) {
				html = new FoxHTTP("http://www.13xxs.com/modules/article/search.php").getHTML("GBK", "searchtype=articlename&action=login&searchkey=" + URLEncoder.encode(iBookName, "GBK") );
				if ( html.startsWith("http") ) { // 403
					return html;
				}
				if ( html.contains("<div id=\"list\">") ) { // TOC // 403
					reStr = "(?smi)og:novel:read_url\"[^\"]+?\"([^\"]+?)\"";
				} else {
					bMulti = true;
					reStr = "(?smi)\"odd\">[^<]+?<a href=\"([^\"]+?)\">([^<]+?)</a>"; // url, name
				}
			} else if ( siteType.equalsIgnoreCase("xqqxs") ) {
				html = new FoxHTTP("https://www.xqqxs.com/modules/article/search.php").getHTML("GBK", "searchtype=all&searchkey=" + URLEncoder.encode(iBookName, "GBK") + "&action=search&submit=");
				if ( html.startsWith("http") ) { // 403
					html = new FoxHTTP(html).getHTML();
				}
				if ( html.contains(">开始阅读<") ) { // 403
					reStr = "(?smi)href=\"([^\"]+?)\" title=\"([^\"]+?)\"[^>]*?><span>开始阅读<"; // url,name
				} else {
					bMulti = true;
					reStr = "(?smi)\"c_subject\"><a[^>]+?>([^<]*?)<span class=\"hottext\">([^<]*?)</span>([^<]*?)</a>.*?<a href=\"([^\"]+?)\"[^>]+?>目录</a>"; // nameA, nameB, nameC, url
				}
			}
		} catch (Exception e) {
			System.err.println(e.toString());
		}

		Matcher mat = Pattern.compile(reStr).matcher(html); // 结果匹配
		while ( mat.find() ) {
			if ( siteType.equalsIgnoreCase("meegoq") ) {
				if ( iBookName.equalsIgnoreCase( mat.group(2) ) ) {
					oURL = "https:" + mat.group(1).replace("/info", "/book");
				}
			} else if ( siteType.equalsIgnoreCase("ymxxs") ) {
				if ( iBookName.equalsIgnoreCase( mat.group(1) ) ) {
					oURL = "https:" + mat.group(2) + "index.html";
				}
			} else if ( siteType.equalsIgnoreCase("wutuxs") || siteType.equalsIgnoreCase("13xxs") ) {
				if ( ! bMulti ) {
					oURL = mat.group(1);
				} else {
					if ( iBookName.equalsIgnoreCase( mat.group(2) ) ) {
						oURL = mat.group(1);
					}
				}
			} else if ( siteType.equalsIgnoreCase("dajiadu") ) {
				if ( ! bMulti ) {
					oURL = mat.group(2);
				} else {
					if ( iBookName.equalsIgnoreCase( mat.group(1) ) ) {
						oURL = mat.group(2);
					}
				}
			} else if ( siteType.equalsIgnoreCase("xqqxs") ) {
				if ( ! bMulti ) {
					oURL = "https://www.xqqxs.com" + mat.group(1);
				} else {
					if ( iBookName.equalsIgnoreCase( mat.group(1) + mat.group(2) + mat.group(3) ) ) {
						oURL = mat.group(4);
					}
//					System.err.println(mat.group(1) + mat.group(2) + mat.group(3) + "\n" + mat.group(4) + "\n");
				}
			} // else end
// System.err.println(mat.group(1) + "\n" + mat.group(2) + "\n");
		}

		return oURL;
	}

	public List<Map<String, Object>> getTOCLast(String html) { // name,url[,len]
		int lastCount = 80; // 取倒数80个链接

		if (html.length() < 100) { //网页木有下载下来
			return new ArrayList<Map<String, Object>>(1);
		}

		List<Map<String, Object>> lks = new ArrayList<Map<String, Object>>(80);
		List<Map<String, Object>> ldata = new ArrayList<Map<String, Object>>(100);
		Map<String, Object> item;

		// 获取链接 并存入结构中
		Matcher mat = Pattern.compile("(?smi)href *= *[\"']?([^>\"']+)[^>]*> *([^<]+)<").matcher(html);
		while (mat.find()) {
			if (2 == mat.groupCount()) {
				if ( mat.group(1).contains("javascript:")) { continue; } // 过滤js链接
				item = new HashMap<String, Object>(3);
				item.put(NV.PageURL, mat.group(1));
				item.put(NV.PageName, mat.group(2));
				ldata.add(item);
			}
		}

		int lastIDX = ldata.size() - 1 ;
		int firstIDX = lastIDX - lastCount;
		int firstURLLen = ldata.get(firstIDX).get(NV.PageURL).toString().length();
		int firstURLLenB = firstURLLen + 1; // URL长度余量

		int nowLen = 0;
		boolean bAdded = false;
		for (int nowIdx = firstIDX; nowIdx <= lastIDX; nowIdx++) {
			nowLen = ldata.get(nowIdx).get(NV.PageURL).toString().length();
			if ( bAdded ) {
				if ( nowLen != firstURLLenB ) {
					break;
				}
			} else {
				if ( nowLen != firstURLLen ) {
					if ( nowLen != firstURLLenB ) {
						break;
					} else {
						bAdded = true;
					}
				}
			}
			lks.add( ldata.get(nowIdx) );
		}

		return lks;
	}

	public List<Map<String, Object>> getTOC(String html) { // name,url[,len]
		if (html.length() < 100) { //网页木有下载下来
			return new ArrayList<Map<String, Object>>(1);
		}
		List<Map<String, Object>> ldata = new ArrayList<Map<String, Object>>(100);
		Map<String, Object> item;
		int nowurllen = 0;
		HashMap<Integer, Integer> lencount = new HashMap<Integer, Integer>();

		// 有些变态网站没有body标签，而java没找到 body 时， 会遍历整个网页，速度很慢
		if (html.matches("(?smi).*<body.*")) {
			html = html.replaceAll("(?smi).*<body[^>]*>(.*)</body>.*", "$1"); // 获取正文
		} else {
			html = html.replaceAll("(?smi).*?</head>(.*)", "$1"); // 获取正文
		}

		if (html.contains("http://book.qidian.com/info/")) { // 2017-1-11处理起点目录
			html = html.replaceAll("(?smi).*<!--start 目录模块-->(.*)<!--end 目录模块-->.*", "$1"); // 获取列表

			html = html.replace("\n", " ");
			html = html.replace("<a", "\n<a");
			html = html.replaceAll("(?i)<a .*?/BookReader/vol.*?>.*?</a>", ""); // 分卷阅读
			html = html.replaceAll("(?i)<a href=\"//vipreader.qidian.com/chapter/.*?>", ""); // vip
			html = html.replaceAll("(?smi)<span[^>]*>", ""); // 起点<a></a>之间有span标签
			html = html.replace("</span>", "");
		}

		// 获取链接 并存入结构中
		Matcher mat = Pattern.compile("(?smi)href *= *[\"']?([^>\"']+)[^>]*> *([^<]+)<").matcher(html);
		while (mat.find()) {
			if (2 == mat.groupCount()) {
				if ( mat.group(1).contains("javascript:")) { continue; } // 过滤js链接
				item = new HashMap<String, Object>(3);
				item.put(NV.PageURL, mat.group(1));
				item.put(NV.PageName, mat.group(2));
				nowurllen = mat.group(1).length();
				item.put("len", nowurllen);
				ldata.add(item);

				if (null == lencount.get(nowurllen))
					lencount.put(nowurllen, 1);
				else
					lencount.put(nowurllen, 1 + lencount.get(nowurllen));
			}
		}

		// 遍历hashmap lencount 获取最多的url长度
		int maxurllencount = 0;
		int maxurllen = 0;
		Iterator iter = lencount.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Object key = entry.getKey();
			Object val = entry.getValue();
			if (maxurllencount < (Integer) val) {
				maxurllencount = (Integer) val;
				maxurllen = (Integer) key;
			}
		}
//		System.out.println("MaxURLLen:" + maxurllen);

		int minLen = maxurllen - 2; // 最小长度值，这个值可以调节
		int maxLen = maxurllen + 2; // 最大长度值，这个值可以调节

		int ldataSize = ldata.size();
		int halfLink = (int) (ldataSize / 2);

		int startDelRowNum = -9;           // 开始删除的行
		int endDelRowNum = 9 + ldataSize;  // 结束删除的行
		// 只找链接的一半，前半是找开始行，后半是找结束行
		// 找开始
		Integer nowLen = 0;
		Integer nextLen = 0;
		for (int nowIdx = 0; nowIdx < halfLink; nowIdx++) {
			nowLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx))).get("len"));
			if ((nowLen > maxLen) || (nowLen < minLen)) {
				startDelRowNum = nowIdx;
			} else {
				nextLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx + 1))).get("len"));
				if ((nextLen - nowLen > 1) || (nextLen - nowLen < 0)) {
					startDelRowNum = nowIdx;
				}
			}
		}
		// 找结束 nextLen means PrevLen here
		for (int nowIdx = ldataSize - 1; nowIdx > halfLink; nowIdx--) {
			nowLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx))).get("len"));
			if ((nowLen > maxLen) || (nowLen < minLen)) {
				endDelRowNum = nowIdx;
			} else {
				nextLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx - 1))).get("len"));
				if ((nowLen - nextLen > 1) || (nowLen - nextLen < 0)) {
					endDelRowNum = nowIdx;
				}
			}
		}
//		System.out.println("startDelRowNum:" + startDelRowNum + " - endDelRowNum:" + endDelRowNum + " ldataSize:" + ldataSize);

		// 倒着删元素
		if (endDelRowNum < ldataSize) {
			for (int nowIdx = ldataSize - 1; nowIdx >= endDelRowNum; nowIdx--) {
				ldata.remove(nowIdx);
			}
		}
		if (startDelRowNum >= 0) {
			for (int nowIdx = startDelRowNum; nowIdx >= 0; nowIdx--) {
				ldata.remove(nowIdx);
			}
		}

		return ldata;	
	}

	// 将page页的html转换为文本，通用规则
	public String getContent(String html){
		if ( html.equalsIgnoreCase("") ) {
			return "";
		}
		// 规律 novel 应该是由<div>包裹着的最长的行
		// 有些变态网站没有body标签，而java没找到<body时，replaceAll会遍历整个html，速度很慢
		if (html.matches("(?smi).*<body.*")) {
			html = html.replaceAll("(?smi).*<body[^>]*>(.*)</body>.*", "$1"); // 获取正文
		} else {
			html = html.replaceAll("(?smi).*?</head>(.*)", "$1"); // 获取正文
		}

		// MinTag
		html = html.replaceAll("(?smi)<script[^>]*>.*?</script>", ""); // 脚本
		html = html.replaceAll("(?smi)<!--[^>]+-->", ""); // 注释 少见
		html = html.replaceAll("(?smi)<iframe[^>]*>.*?</iframe>", ""); // 框架
		// 相当少见
		html = html.replaceAll("(?smi)<h[1-9]?[^>]*>.*?</h[1-9]?>", ""); // 标题
		// 相当少见
		html = html.replaceAll("(?smi)<meta[^>]*>", ""); // 标题 相当少见

		// 2选1,正文链接有文字，目前没见到这么变态的，所以删吧
		html = html.replaceAll("(?smi)<a [^>]+>.*?</a>", ""); // 删除链接及中间内容
		// html = html.replaceAll("(?smi)<a[^>]*>", "<a>"); // 替换链接为<a>

		// 将html代码缩短,便于区分正文与广告内容，可以按需添加
		html = html.replaceAll("(?smi)<div[^>]*>", "<div>");
		html = html.replaceAll("(?smi)<font[^>]*>", "<font>");
		html = html.replaceAll("(?smi)<table[^>]*>", "<table>");
		html = html.replaceAll("(?smi)<td[^>]*>", "<td>");
		html = html.replaceAll("(?smi)<ul[^>]*>", "<ul>");
		html = html.replaceAll("(?smi)<dl[^>]*>", "<dl>");
		html = html.replaceAll("(?smi)<span[^>]*>", "<span>");

		html = html.toLowerCase();
		html = html.replace("\r", "");
		html = html.replace("\n", "");
		html = html.replace("\t", "");
		html = html.replace("</div>", "</div>\n");
		html = html.replace("<div></div>", "");
		html = html.replace("<li></li>", "");
		html = html.replace("  ", "");

		// getMaxLine -> ll[nMaxLine]
		String[] ll = html.split("\n");
		int nMaxLine = 0;
		int nMaxCount = 0;
		int tmpCount = 0;
		for (int i = 0; i < ll.length; i++) {
			tmpCount = ll[i].length();
			if (tmpCount > nMaxCount) {
				nMaxLine = i;
				nMaxCount = tmpCount;
			}
		}
		html = ll[nMaxLine];

		// html2txt
		html = html.replace("\t", "");
		html = html.replace("\r", "");
		html = html.replace("\n", "");
		html = html.replace("&nbsp;", "");
		html = html.replace("　　", "");
		html = html.replace("<br>", "\n");
		html = html.replace("</br>", "\n");
		html = html.replace("<br/>", "\n");
		html = html.replace("<br />", "\n");
		html = html.replace("<p>", "\n");
		html = html.replace("</p>", "\n");
		html = html.replace("<div>", "\n");
		html = html.replace("</div>", "\n");
		html = html.replace("\n\n", "\n");

		// 处理正文中的<img标签，可以将代码放在这里，典型例子:无错

		// 特殊网站处理可以放在这里
		// 144书院的这个会导致下面正则将正文也删掉了，已使用正则修复
		html = html.replaceAll("(?smi)<span[^>]*>.*?</span>", ""); // 删除<span>里面是混淆字符， 针对 纵横中文混淆字符，以及大家读结尾标签，一般都没有span标签
		html = html.replaceAll("(?smi)<[^<>]+>", ""); // 这是最后一步，调试时可先注释: 删除 html标签,改进型，防止正文有不成对的<
		html = html.replaceAll("(?smi)^\n*", "");

		return html;
	}
}
