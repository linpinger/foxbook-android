package com.linpinger.foxbook;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.ray.tools.umd.builder.Umd;
import com.ray.tools.umd.builder.UmdChapters;
import com.ray.tools.umd.builder.UmdHeader;

import android.annotation.SuppressLint;
import android.util.Log;

public class FoxBookLib {
	
	public static void all2txt() { // 所有书籍转为txt
		String txtPath = "/sdcard/fox.txt";
		String sContent = "" ;
		List<Map<String, Object>> data = FoxDB.getUMDArray();
		Iterator<Map<String, Object>> itr = data.iterator();
		while (itr.hasNext()) {
			HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
			sContent = sContent + (String) mm.get("title") + "\n\n" + (String) mm.get("content") + "\n\n\n" ;
		}

		try {
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(txtPath, false));
			bw1.write(sContent);
			bw1.flush();
			bw1.close();
		} catch (IOException e) {
			e.toString();
//			e.printStackTrace();
		}
	}
	

	public static void all2umd() { // 所有书籍转为umd
		String umdPath = "/sdcard/fox.umd";
		Umd umd = new Umd();
		
		UmdHeader uh = umd.getHeader(); // 设置书籍信息
		uh.setTitle("FoxBook所有章节");
		uh.setAuthor("爱尔兰之狐");
		uh.setBookType("小说");
		uh.setYear("2014");
		uh.setMonth("04");
		uh.setDay("01");
		uh.setBookMan("爱尔兰之狐");
		uh.setShopKeeper("爱尔兰之狐");

		
		UmdChapters  cha = umd.getChapters(); // 设置内容
		List<Map<String, Object>> data = FoxDB.getUMDArray();
		Iterator<Map<String, Object>> itr = data.iterator();
		while (itr.hasNext()) {
			HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
			cha.addChapter((String) mm.get("title"), (String) mm.get("content"));
		}

        File file = new File(umdPath); // 生成
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                umd.buildUmd(bos);
                bos.flush();
             } finally {
                fos.close();
            }
        } catch (Exception e) {
        	e.toString();
        }
	}
	
	/*
	public static int updatebook(int bookid) {
		List<Map<String, Object>> xx ;
		String bookurl = FoxDB.getOneCell("select url from book where id=" + String.valueOf(bookid)); // 获取 url
		String existList = FoxDB.getPageListStr(bookid); //得到旧 list
		existList = existList.toLowerCase();
		Log.e("FoxLib1", bookurl);
		String html = downhtml(bookurl); // 下载url
		Log.e("FoxLib2", String.valueOf(html.length()));

		if ( existList.length() > 1024 ) {
			xx = tocHref(html, 55);	// 分析获取 list 最后55章
		} else {
			xx = tocHref(html, 0);	// 分析获取 list 所有章节
		}
		
		// 比较得到新章节
		String nowURL ;
		ArrayList<HashMap<String, Object>> newPages = new ArrayList<HashMap<String, Object>>();
		Iterator<Map<String, Object>> itr = xx.iterator();
		while (itr.hasNext()) {
			HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
			nowURL = (String) mm.get("url");
			if ( ! existList.contains(nowURL.toLowerCase() + "|") ) { // 新章节
				Log.e("FoxLib3", "new : " + nowURL);
				newPages.add(mm);
			}
		}
		FoxDB.inserNewPages(newPages, bookid); // 添加到数据库
		
//		Log.e("FoxLib3.5", "xxx");
		// 循环更新页面
		List<Map<String, Object>> nbl = FoxDB.getBookNewPages(bookid);
//		Log.e("FoxLib3.5", "yyy");
		Iterator<Map<String, Object>> itrz = nbl.iterator();
		Integer nowpageid = 0 ;
		while (itrz.hasNext()) {
			HashMap<String, Object> nn = (HashMap<String, Object>) itrz.next();
			nowURL = (String) nn.get("url");
			nowpageid = (Integer) nn.get("id");
			Log.e("FoxLib4", "updatepage id : " + String.valueOf(nowpageid));
			updatepage(nowpageid);
		}
		Log.e("FoxLib5", "end : newpagecount: " + String.valueOf(newPages.size()));
		return newPages.size() ;
	}
	*/
	
	public static void updatepage(int pageid) {
		Map<String, String> xx = FoxDB.getOneRow("select book.url as bu,page.url as pu from book,page where page.id=" + String.valueOf(pageid) + " and  book.id in (select bookid from page where id=" + String.valueOf(pageid) + ")");
		String fullPageURL = getFullURL(xx.get("bu"),xx.get("pu"));		// 获取bookurl, pageurl 合成得到url
		updatepage(pageid, fullPageURL) ;
	}
	
	public static String updatepage(int pageid, String pageFullURL) {
		String html = downhtml(pageFullURL); // 下载url
		
		// 针对起点文本,特殊处理
		if ( pageFullURL.contains(".qidian.com") ) {
			String scriptURL = "" ;
			// <script src='http://files.qidian.com/Author6/3094349/51689782.txt'  charset='GB2312'></script>
			Matcher mat = Pattern.compile("(?smi)(http://files[^\"']*.txt)").matcher(html);
			while (mat.find()) {
				if (1 == mat.groupCount()) {
					scriptURL = mat.group(1);
				}
			}
			html = downhtml(scriptURL);
			html = html.replace("document.write('", "");
			html = html.replace("<a href=http://www.qidian.com>起点中文网 www.qidian.com 欢迎广大书友光临阅读，最新、最快、最火的连载作品尽在起点原创！</a><a>手机用户请到m.qidian.com阅读。</a>');", "");
		}
		
		String text = pagetext(html);   	// 分析得到text
		if ( pageid > 0 ) { // 当pageid小于0时不写入数据库，主要用于在线查看
			FoxDB.setPageContent(pageid, text); // 写入数据库
			return String.valueOf(0);
		} else {
			return text;
		}
	}
	
	public static List<Map<String, Object>> getSearchEngineHref(String html, String KeyWord) {
//		String KeyWord = "三界血歌" ;
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(64);
		Map<String, Object> item;

		html = html.replace("\t", "");
		html = html.replace("\r", "");
		html = html.replace("\n", "");
		html = html.replaceAll("(?i)<!--[^>]+-->", "") ;
		html = html.replace("<em>", "");
		html = html.replace("</em>", "");
		html = html.replace("<b>", "");
		html = html.replace("</b>", "");
		html = html.replace("<strong>", "");
		html = html.replace("</strong>", "");


		// 获取链接 并存入结构中
		Matcher mat = Pattern.compile("(?smi)href *= *[\"']?([^>\"']+)[\"']?[^>]*> *([^<]+)<").matcher(html);
		while ( mat.find() ) {
			if ( 2 == mat.groupCount() ) {
// 				System.out.println(mat.group(1) + "|" + mat.group(2)) ;
				if ( mat.group(1).length() < 5 ) {
						continue ;
				}
				if ( mat.group(1).indexOf("http") < 0 ) {
						continue ;
				}
				if ( mat.group(2).indexOf(KeyWord) < 0 ) {
						continue ; 
				}

				item = new HashMap<String, Object>();
				item.put("url", mat.group(1));
				item.put("name", mat.group(2));
				data.add(item);
			}
		}

		return data ;
	}
	
	@SuppressLint("UseSparseArrays")
	public static List<Map<String, Object>> tocHref(String html, int lastNpage) {
		List<Map<String, Object>> ldata = new ArrayList<Map<String, Object>>(100);
		Map<String, Object> item;
		int nowurllen = 0;
		Map<Integer, Integer> lencount = new HashMap<Integer, Integer>();

		// 有些变态网站没有body标签，而java没找到 body 时， 会遍历整个网页，速度很慢
		
		if (html.matches("(?smi).*<body.*")) {
			html = html.replaceAll("(?smi).*<body[^>]*>(.*)</body>.*", "$1"); // 获取正文
		} else {
			html = html.replaceAll("(?smi).*?</head>(.*)", "$1"); // 获取正文
		}
		html = html.replaceAll("(?smi)<span[^>]*>", ""); // 起点<a></a>之间有span标签
		html = html.replace("</span>", "");

		// 获取链接 并存入结构中
		Matcher mat = Pattern.compile(
				"(?smi)href *= *[\"']?([^>\"']+)[^>]*> *([^<]+)<")
				.matcher(html);
		while (mat.find()) {
			if (2 == mat.groupCount()) {
				// System.out.println(mat.group(1) + "|" + mat.group(2)) ;
				item = new HashMap<String, Object>();
				item.put("url", mat.group(1));
				item.put("name", mat.group(2));
				nowurllen = mat.group(1).length();
				item.put("len", nowurllen);
				ldata.add(item);

				if (null == lencount.get(nowurllen)) {
					lencount.put(nowurllen, 1);
				} else {
					lencount.put(nowurllen, 1 + lencount.get(nowurllen));
				}
			}
		}

		// 遍历hashmap lencount 获取最多的url长度
		int maxurllencount = 0;
		int maxurllen = 0;
		Iterator<Entry<Integer, Integer>>  iter = lencount.entrySet().iterator();
		while (iter.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry entry = (Map.Entry) iter.next();
			Object key = entry.getKey();
			Object val = entry.getValue();
			if (maxurllencount < (Integer) val) {
				maxurllencount = (Integer) val;
				maxurllen = (Integer) key;
			}
		}
		int maxurllenbig = maxurllen + 1;

		List<Map<String, Object>> od = new ArrayList<Map<String, Object>>(100);
		Map<String, Object> oi;

		// 筛选符合条件的链接
		int nowlen = 0;
		Iterator<Map<String, Object>> itr = ldata.iterator();
		while (itr.hasNext()) {
			HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
			nowlen = (Integer) mm.get("len");
			if (maxurllen == nowlen || maxurllenbig == nowlen) {
				oi = new HashMap<String, Object>();
				oi.put("url", (String) mm.get("url"));
				oi.put("name", (String) mm.get("name"));
				// oi.put("len", mm.get("len"));
				od.add(oi);
			}
		}

		if ( lastNpage > 0 ) { // 显示部分
			int chaptercount = od.size();
			if (chaptercount > lastNpage) {
				int startnum = chaptercount - lastNpage;
				List<Map<String, Object>> odn = new ArrayList<Map<String, Object>>(
						100);
				Map<String, Object> oin;
				// 筛选符合条件的链接
				Iterator<Map<String, Object>> itr1 = od.iterator();
				int ncountx = 0;
				while (itr1.hasNext()) {
					oin = (HashMap<String, Object>) itr1.next();
					++ncountx;
					if (ncountx < startnum) {
						continue;
					} else {
						odn.add(oin);
					}
				}
				return odn;
			}
		}

		return od;
	}

	// 将page页的html转换为文本，通用规则
	public static String pagetext(String html) {
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
		// stringreplace, html, html, <144, 《144, A ;
		// 144书院的这个会导致下面正则将正文也删掉了，已使用正则修复
		html = html.replaceAll("(?smi)<span[^>]*>.*?</span>", ""); // 删除<span>里面是混淆字符，;
																	// 针对
																	// 纵横中文混淆字符，以及大家读结尾标签，一般都没有span标签
		html = html.replaceAll("(?smi)<[^<>]+>", ""); // 这是最后一步，调试时可先注释: 删除
														// html标签,改进型，防止正文有不成对的<

		return html;
	}

	public static String getFullURL(String sbaseurl, String suburl) { // 获取完整路径
		String allURL = "" ;
		try {
			allURL = (new URL(new URL(sbaseurl), suburl)).toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return allURL;
	}


public static String downhtml(String inURL) {
	URL url ;
	HttpURLConnection conn;
	try {
		url = new URL(inURL) ;
		conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
		conn.setConnectTimeout(5000);
		conn.connect();

		
		// 判断返回的是否是gzip数据
		boolean bGZDate = false ;
		Map<String,List<String>> rh = conn.getHeaderFields();
		List<String> ce = rh.get("Content-Encoding") ;
		if ( null == ce ) { // 不是gzip数据
			bGZDate = false ;
		} else {
			bGZDate = true ;
		//	System.out.println(ce.get(0)) ;
		}

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[5120];
		int len = 0;

		if ( bGZDate ) { //Gzip
			InputStream in = conn.getInputStream();
			GZIPInputStream gzin = new GZIPInputStream(in);
			while ((len = gzin.read(buffer)) != -1) {
				outStream.write(buffer, 0, len);
			}
			gzin.close();
			in.close();
		} else {
			InputStream in = conn.getInputStream();
			while ((len = in.read(buffer)) != -1) {
				outStream.write(buffer, 0, len);
			}
			in.close();
		}

		byte[] buf = outStream.toByteArray();
		outStream.close();

		
		// 探测编码
		String html = "";
		html = new String(buf, "gbk");

		if (html.matches("(?smi).*<meta[^>]*charset=[\"]?(utf8|utf-8)[\"]?.*")) {
			// Log.i("Fox", "Guess encoding is UTF8");
			html = new String(buf, "utf-8");
		}
		return html;
	} catch ( Exception e ) { // 错误 是神马
//		e.toString() ;
		return "" ;
	}
}

/*
public static String downhtml(String inURL) {
	// 下载网页 到数组
	byte[] buf = null;
	try {
		buf = getUrlFileData(inURL);
	} catch (Exception e) {
		e.printStackTrace();
	} finally {
		if ( null == buf) {
			return "";
		}
	}

	// 探测编码
	String html = "";
	try {
		html = new String(buf, "gbk");

		if (html.matches("(?smi).*<meta[^>]*charset=(utf8|utf-8).*")) {
			// Log.i("Fox", "Guess encoding is UTF8");
			html = new String(buf, "utf-8");
		}
	} catch (UnsupportedEncodingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return html;

}

// 获取链接地址文件的byte数据
private static byte[] getUrlFileData(String fileUrl) throws Exception {
	URL url = new URL(fileUrl);
	HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
	
//	httpConn.setConnectTimeout(5000);
	httpConn.connect();
	
	InputStream cin = httpConn.getInputStream();
	ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	byte[] buffer = new byte[1024];
	int len = 0;
	while ((len = cin.read(buffer)) != -1) {
		outStream.write(buffer, 0, len);
	}
	cin.close();
	byte[] fileData = outStream.toByteArray();
	outStream.close();
	return fileData;
}
*/

} // 类结束
